/*
 * DictWin.java
 *
 * Copyright (C) 2023 J. R. Bhaddacak 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package paliplatform.grammar;

import paliplatform.*;

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

import java.util.*;
import java.util.stream.Collectors;
import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * The main dictionary window.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class DictWin extends BorderPane {
	public static enum DictBook {
		CPED("Concise Pāli-English Dictionary"), CEPD("Concise English-Pāli Dictionary"),
		PTSD("PTS Pāli-English Dictionary"), DPPN("Dictionary of Pāli Proper Names");
		public final String bookName;
		private DictBook(final String name) {
			bookName = name;
		}
	}
	private final PaliTextInput searchInput = new PaliTextInput(PaliTextInput.InputType.COMBO);
	private final ComboBox<String> searchComboBox;
	private final TextField searchTextField;
	private final EnumMap<DictBook, CheckBox> dictCBMap = new EnumMap<>(DictBook.class);
	private final Set<DictBook> dictSet = EnumSet.noneOf(DictBook.class);
	private final ObservableList<String> resultList = FXCollections.<String>observableArrayList();
	private final Map<String, ArrayList<DictBook>> resultMap = new HashMap<>();
	private final SimpleBooleanProperty useWildcards = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty inMeaning = new SimpleBooleanProperty(false);
	private final HtmlViewer htmlViewer = new HtmlViewer();
	private final InfoPopup infoPopup = new InfoPopup();

	public DictWin(final Object[] args) {
		for(final DictBook db : DictBook.values())
			dictCBMap.put(db, createDictCheckBox(db));

		final VBox mainBox = new VBox();
		// add toolbar on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(this);
		// add a new button
		final Button editorButton = new Button("", new TextIcon("pencil", TextIcon.IconSet.AWESOME));
		editorButton.setTooltip(new Tooltip("Open result in editor"));
		editorButton.setOnAction(actionEvent -> {
			copyText();
			final Clipboard cboard = Clipboard.getSystemClipboard();
			final String text = cboard.hasString() ? cboard.getString() : "";
			if(text.isEmpty()) return;
			final Object[] argsEditor = new Object[2];
			argsEditor[0] = "ROMAN";
			argsEditor[1] = text;
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, argsEditor);
		});
		toolBar.getItems().addAll(editorButton, new Separator());
		// config some buttons
		toolBar.getThemeButton().setOnAction(actionEvent -> {
			final PaliPlatform.Theme theme = toolBar.resetTheme();
			setViewerTheme(theme.toString());
		});
		toolBar.getZoomInButton().setOnAction(actionEvent -> {
			htmlViewer.webView.setFontScale(htmlViewer.webView.getFontScale() + 0.10);
		});
		toolBar.getZoomOutButton().setOnAction(actionEvent -> {
			htmlViewer.webView.setFontScale(htmlViewer.webView.getFontScale() - 0.10);
		});
		toolBar.getResetButton().setOnAction(actionEvent -> {
			htmlViewer.webView.setFontScale(1.0);
		});
		toolBar.saveTextButton.setOnAction(actionEvent -> saveText());
		toolBar.copyButton.setOnAction(actionEvent -> copyText());
		for(final DictBook db : DictBook.values())
			toolBar.getItems().add(dictCBMap.get(db));
		setTop(toolBar);

		// add main content
		// add search toolbar
		final ToolBar searchToolBar = new ToolBar();
		searchComboBox = searchInput.getComboBox();
		searchComboBox.setPromptText("Search...");
		searchComboBox.setOnShowing(e -> recordQuery());
		searchComboBox.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				final KeyCode key = keyEvent.getCode();
				if(key == KeyCode.ENTER) {
					search();
				} else if(key == KeyCode.ESCAPE) {
					searchComboBox.getEditor().clear();
				}
			}
		});
		searchTextField = (TextField)searchInput.getInput();
		searchTextField.textProperty().addListener((obs, oldValue, newValue) -> {
			if(!useWildcards.get() && !inMeaning.get()) {
				// immediate (incremental) search
				final String strQuery = Normalizer.normalize(newValue.trim(), Form.NFC);
				if(!strQuery.isEmpty())
					search(strQuery.toLowerCase());
				searchComboBox.commitValue();
			}
		});
		final Button clearButton = searchInput.getClearButton();
		clearButton.setOnAction(actionEvent -> {
			recordQuery();
			searchTextField.clear();
			searchComboBox.commitValue();
		});
		final Button searchButton = new Button("Search");
		searchButton.disableProperty().bind(useWildcards.not().and(inMeaning.not()));
		searchButton.setOnAction(actionEvent -> search());
		final CheckBox wildcardButton = new CheckBox("Use */?");
		wildcardButton.setTooltip(new Tooltip("Use wildcards (*/?)"));
		wildcardButton.selectedProperty().bindBidirectional(useWildcards);
		wildcardButton.disableProperty().bind(inMeaning);
		final CheckBox inMeaningButton = new CheckBox("In meaning");
		inMeaningButton.setTooltip(new Tooltip("Search in meaning"));
		inMeaningButton.selectedProperty().bindBidirectional(inMeaning);
		searchButton.setOnAction(actionEvent -> search());
		final Button helpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		helpButton.setOnAction(actionEvent -> infoPopup.showPopup(helpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		searchToolBar.getItems().addAll(searchComboBox, clearButton, searchInput.getMethodButton(), searchButton,
										new Separator(), wildcardButton, inMeaningButton, helpButton);

		// add result split pane
		final SplitPane splitPane = new SplitPane();
		splitPane.setDividerPositions(0.22);
		// add result list on the left
		final ListView<String> resultListView = new ListView<>(resultList);
		resultListView.setCellFactory((ListView<String> lv) -> {
			return new ListCell<String>() {
				@Override
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					this.setGraphic(null);
					if (empty) {
						this.setText(null);
						this.setTooltip(null);
					} else {
						final String value = this.getItem();
						final String strBooks = resultMap.get(value).stream().map(x -> x.toString()).collect(Collectors.joining(", "));
						this.setTooltip(new Tooltip(value + " (" + strBooks + ")"));
						this.setText(value);
					}
					this.setStyle("-fx-padding: 0px 0px 0px 3px");
				}
			};
		});
		resultListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			final String term = newValue;
			if(term != null)
				showResult(term);
		});
		resultListView.setOnDragDetected(mouseEvent -> {
			final String selected = resultListView.getSelectionModel().getSelectedItem();
			if(selected != null && resultListView.getSelectionModel().getSelectedIndex() >= 0) {
				final Dragboard db = resultListView.startDragAndDrop(TransferMode.ANY);
				final ClipboardContent content = new ClipboardContent();
				content.putString(selected);
				db.setContent(content);
				mouseEvent.consume();
			}
		});
		resultListView.setOnMouseDragged(mouseEvent -> mouseEvent.setDragDetect(true));
		// add HtmlViewer to show result on the right
		htmlViewer.simpleSetup();
		htmlViewer.webEngine.setUserStyleSheetLocation(PaliPlatform.class.getResource(Utilities.DICT_CSS).toExternalForm());
		// Set the member for the browser's window object after the document loads
		final DictFXHandler fxHandler = new DictFXHandler(htmlViewer);
	 	htmlViewer.webEngine.getLoadWorker().stateProperty().addListener((prop, oldState, newState) -> {
			if(newState == Worker.State.SUCCEEDED) {
				JSObject jsWindow = (JSObject)htmlViewer.webEngine.executeScript("window");
				jsWindow.setMember("fxHandler", fxHandler);
				setViewerTheme(toolBar.getTheme().toString());
				toolBar.resetFont();
			}
		});
		htmlViewer.setContent(Utilities.makeHTML("", false));
		splitPane.getItems().addAll(resultListView, htmlViewer);

		mainBox.getChildren().addAll(searchToolBar, splitPane);
		setCenter(mainBox);
		setPrefWidth(Utilities.getRelativeSize(60));
		setPrefHeight(Utilities.getRelativeSize(45));

		// set up drop event
		this.setOnDragOver(dragEvent -> {
			if(dragEvent.getGestureSource() != this && dragEvent.getDragboard().hasString())
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			dragEvent.consume();
		});
		this.setOnDragDropped(dragEvent -> {
			final Dragboard db = dragEvent.getDragboard();
			if(db.hasString()) {
				final String[] allStr = db.getString().split("\n");
				final String head = allStr[0];
				if(!head.startsWith("::paliplatform")) {
					final String term = head.trim().split("\\s")[0];
					searchTextField.setText(term);
				}
				dragEvent.setDropCompleted(true);
			} else {
				dragEvent.setDropCompleted(false);
			}
			dragEvent.consume();
		});
		
		// set initial values
		init(args);

		// prepare info popup
		infoPopup.setContent("info-dictionaries.txt");
		infoPopup.setTextWidth(Utilities.getRelativeSize(37.5));
	}

	public final void init(final Object[] args) {
		final String strDictSet = PaliPlatform.settings.getProperty("dictset");
		final String[] arrDictSet = strDictSet.split(",");
		for(final String s : arrDictSet)
			if(!s.isEmpty())
				dictSet.add(DictBook.valueOf(s));
		for(final DictBook db : DictBook.values())
			dictCBMap.get(db).setSelected(strDictSet.contains(db.toString()));
		htmlViewer.setContent(Utilities.makeHTML("", false));
		if(args != null) {
			final String term = (String)args[0];
			if(!term.isEmpty())
				searchTextField.setText(term);
		} else {
			searchTextField.clear();
			resultList.clear();
			resultMap.clear();
		}
	}

	private CheckBox createDictCheckBox(final DictWin.DictBook book) {
		final String name = book.toString();
		final CheckBox cb = new CheckBox(name);
		cb.setAllowIndeterminate(false);
		cb.setTooltip(new Tooltip(book.bookName));
		cb.setOnAction(actionEvent -> {
			if(cb.isSelected())
				dictSet.add(book);
			else
				dictSet.remove(book);
			search();
		});
		return cb;
	}

	public void setSearchInput(final String query) {
		htmlViewer.setContent(Utilities.makeHTML("", false));
		searchTextField.setText(query);
	}

	private void search() {
		final String strQuery = Normalizer.normalize(searchTextField.getText().trim(), Form.NFC);
		if(!strQuery.isEmpty())
			search(strQuery);
	}

	private void search(final String query) {
		resultMap.clear();
		// remove single qoute causing SQL error
		String strQuery = query.replace("'", "");
		if(useWildcards.get()) {
			if(strQuery.indexOf('?') >= 0)
				strQuery = strQuery.replace("?", "_");
			if(strQuery.indexOf('*') >= 0)
				strQuery = strQuery.replace("*", "%");
			final int uCount = Utilities.charCount(strQuery, '_');
			final int pCount = Utilities.charCount(strQuery, '%');
			// just * or a sheer combination of ? and * is not allowed
			if(strQuery.length() == 0 || (pCount>0 && pCount+uCount==strQuery.length()))
				return;
		}
		for(Iterator<DictBook> dIt = dictSet.iterator(); dIt.hasNext();) {
			final DictBook dicBook = dIt.next();
			final String tail = useWildcards.get()?"';":"%';";
			String dbQuery = "SELECT TERM FROM " + dicBook.toString() + " WHERE TERM LIKE '" + strQuery + tail;
			if(inMeaning.get()) {
				if(dicBook == DictBook.CPED) {
					dbQuery = "SELECT TERM FROM CPED WHERE MEANING LIKE '%"+query+"%' OR SUBMEANING LIKE '%"+query+"%' OR POS LIKE '%"+query+"%';";
				} else {
					String mQuery = query;
					if(dicBook == DictBook.PTSD) {
						mQuery = Utilities.replaceNewNiggahitaWithOld(query);
					}
					dbQuery = "SELECT TERM FROM "+dicBook.toString()+" WHERE MEANING LIKE '%"+mQuery+"%';";
				}
			}
			final Set<String> results = Utilities.getHeadTermsFromDB(dbQuery);
			for(Iterator<String> tIt = results.iterator(); tIt.hasNext();) {
				final String term = tIt.next();
				final ArrayList<DictBook> dList;
				if(resultMap.containsKey(term))
					dList = resultMap.get(term);
				else
					dList = new ArrayList<>();
				dList.add(dicBook);
				resultMap.put(term, dList);
			}
		}
		final ArrayList<String> resultAll = new ArrayList<>(resultMap.keySet());
		resultAll.sort(PaliPlatform.paliCollator);
		resultList.setAll(resultAll);
		if(!resultList.isEmpty())
			showResult(resultList.get(0));
	}

	private void recordQuery() {
		if(!resultMap.isEmpty())
			searchInput.recordQuery();
	}

	private void showResult(final String term) {
		final StringBuilder result = new StringBuilder();
		final ArrayList<DictBook> dicts = resultMap.get(term);
		if(dicts == null) 	
			return;
		
		// show head word
		result.append("<h1>").append(term).append("</h1>");
		for(int i=0; i<dicts.size(); i++) {
			final DictBook db = dicts.get(i);
			result.append("<p class=bookname>&lt;").append(db.bookName).append("&gt;</p>");
			if(db == DictBook.CPED) {
				result.append(getResultArticle(Utilities.lookUpCPEDFromDB(term)));
			} else {
				final String res = getResultArticle(db, Utilities.lookUpDictFromDB(db, term));
				if(db == DictBook.DPPN)
					result.append(res.replace("<hr>", ""));
				else
					result.append(res);
			}
			if(i < dicts.size()-1)
				result.append("<div class=hrule></div><p></p>");
		}
		htmlViewer.setContent(Utilities.makeHTML(result.toString(), false));
	}

	private String getResultArticle(final PaliWord item) {
		String result = Utilities.formatCPEDMeaning(item, true);
		if(item.isDeclinable()) {
			result = result + "<p><a class=linkbutton onClick=openDeclension('"+item.getTerm()+"')>Show declension</a></p>";
		}
		return result;
	}

	private String getResultArticle(final DictBook dic, final List<String> items) {
		final StringBuilder text = new StringBuilder();
		for(final String s : items) {
			String ss = s.replace("&apos;", "'");
			if(dic == DictBook.PTSD)
				ss = Utilities.replaceOldNiggahitaWithNew(ss);
			text.append(ss);
			text.append("<p></p>");
		}
		return text.toString();
	}

	private void setViewerTheme(final String theme) {
		final String command = "setThemeCommon('"+theme+"');";
		htmlViewer.webEngine.executeScript(command);
	}

	public void setViewerFont(final String fontname) {
		htmlViewer.webEngine.executeScript("setFont('" + fontname + "')");
	}

	private void copyText() {
		htmlViewer.webEngine.executeScript("copyBody()");
	}

	private void saveText() {
		htmlViewer.webEngine.executeScript("saveBody()");
	}
}
