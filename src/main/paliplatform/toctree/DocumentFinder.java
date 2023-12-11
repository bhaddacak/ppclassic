/*
 * DocumentFinder.java
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

package paliplatform.toctree;

import paliplatform.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import javafx.geometry.*;
import javafx.collections.*;
import javafx.scene.input.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.application.Platform;

/** 
 * This can find and open a specific Pali document from the collection.
 * @author J.R. Bhaddacak
 * @version 2.1
 * @since 2.0
 */
public class DocumentFinder extends BorderPane {
	private enum TextGroup {
		MULA("Mūla:Main"), ATTHA("Aṭṭhakathā:Commentaries"), TIKA("Ṭīkā:Subcommentaries"), ANYA("Añña:Other texts");
		private final String pname;
		private final String ename;
		private TextGroup(final String name) {
			final String[] names = name.split(":");
			pname = names[0];
			ename = names[1];
		}
		public String getPaliName() {
			return pname;
		}
		public String getEngName() {
			return ename;
		}
	}
	private enum SearchField { TEXT, BOOK, GROUP, CONTENT }
	private enum WildCardPos { START, MIDDLE, END, NONE }
	private final BorderPane mainPane = new BorderPane();
	private final PaliTextInput textInput = new PaliTextInput(PaliTextInput.InputType.COMBO);
	private final EnumMap<TextGroup, CheckBox> textCBMap = new EnumMap<>(TextGroup.class);
	private final Set<TextGroup> textSet = EnumSet.allOf(TextGroup.class);
	private final ObservableList<PaliDocument> resultList = FXCollections.<PaliDocument>observableArrayList();
	private final TableView<PaliDocument> table = new TableView<>();
	private final RadioMenuItem inTextMenuItem = new RadioMenuItem("Find in text name");
	private final ToggleGroup searchFieldGroup = new ToggleGroup();
	private final HBox statusBox = new HBox(3);
	private final Label statusMessage = new Label();
	private final HBox progressBox = new HBox(3);
	private final ProgressBar progressBar = new ProgressBar();
	private final Label progressMessage = new Label();
	private final InfoPopup helpPopup = new InfoPopup();
	private final TextField searchTextField;
	private final ComboBox<String> searchComboBox;
	private SearchField searchIn = SearchField.TEXT;
	private Task<Boolean> searchTask = null;
	
	public DocumentFinder() {
		for(final TextGroup tg : TextGroup.values())
			textCBMap.put(tg, createTextGroupCheckBox(tg));

		resultList.add(new PaliDocument("",""));
		table.setItems(resultList);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setupTable();
		// prepare doc info
		if(Utilities.docInfoMap.isEmpty())
			Utilities.loadDocInfo();
		// add context menu
		final ContextMenu popupMenu = new ContextMenu();
		final MenuItem openMenuItem = new MenuItem("Open");
		openMenuItem.setOnAction(actionEvent -> openText());		
		final MenuItem openAsTextMenuItem = new MenuItem("Open as text");
		openAsTextMenuItem.setOnAction(actionEvent -> openAsText(true));		
		final MenuItem openAsTextNoNotesMenuItem = new MenuItem("Open as text (no notes)");
		openAsTextNoNotesMenuItem.setOnAction(actionEvent -> openAsText(false));		
		final MenuItem addBookmarkMenuItem = new MenuItem("Add to Bookmarks");
		addBookmarkMenuItem.setOnAction(actionEvent -> addBookmark());		
		final MenuItem addToTokenMenuItem = new MenuItem("Add to Tokenizer");
		addToTokenMenuItem.setOnAction(actionEvent -> sendToTokenizer());
		popupMenu.getItems().addAll(openMenuItem, openAsTextMenuItem, openAsTextNoNotesMenuItem, addBookmarkMenuItem, addToTokenMenuItem);
		table.setContextMenu(popupMenu);
		table.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				if(keyEvent.getCode() == KeyCode.ENTER)
					openText();
			}
		});
		table.setOnDragDetected(mouseEvent -> {
			final Dragboard db = table.startDragAndDrop(TransferMode.ANY);
			final ClipboardContent content = new ClipboardContent();
			final String head = "::" + this.getClass().getName() + "::\n";
			final StringBuilder dragText = new StringBuilder(head);
			for(final PaliDocument pd : table.getSelectionModel().getSelectedItems()) {
				dragText.append("1:0:1::").append(pd.getFileName()).append("\n");
			}
			content.putString(dragText.toString());
			db.setContent(content);
			mouseEvent.consume();
		});
		table.setOnMouseDragged(mouseEvent -> mouseEvent.setDragDetect(true));
		mainPane.setCenter(table);
		setCenter(mainPane);
		
		// add options on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(table);
		// use property to bind with disablility of some buttons
		final SimpleListProperty<PaliDocument> resultListProperty = new SimpleListProperty<>(resultList);
		// configure some buttons first
		toolBar.saveTextButton.setTooltip(new Tooltip("Save data as CSV"));
		toolBar.saveTextButton.setOnAction(actionEvent -> saveCSV());		
		toolBar.saveTextButton.disableProperty().bind(resultListProperty.sizeProperty().isEqualTo(0));
		toolBar.copyButton.setTooltip(new Tooltip("Copy CSV to clipboard"));
		toolBar.copyButton.setOnAction(actionEvent -> copyCSV());		
		toolBar.copyButton.disableProperty().bind(resultListProperty.sizeProperty().isEqualTo(0));
		// add new buttons
		final Button openButton = new Button("", new TextIcon("file-lines", TextIcon.IconSet.AWESOME));
		openButton.setTooltip(new Tooltip("Open the selected file"));
		openButton.setOnAction(actionEvent -> openText());
		openButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		final Button addBookmarkButton = new Button("", new TextIcon("bookmark-plus", TextIcon.IconSet.CUSTOM));
		addBookmarkButton.setTooltip(new Tooltip("Bookmark the selected file"));
		addBookmarkButton.setOnAction(actionEvent -> addBookmark());		
		addBookmarkButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		toolBar.getItems().addAll(new Separator(), openButton, addBookmarkButton, new Separator());
		for(final TextGroup tg : TextGroup.values())
			toolBar.getItems().add(textCBMap.get(tg));
		final Button helpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		helpButton.setOnAction(actionEvent -> helpPopup.showPopup(helpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		toolBar.getItems().addAll(new Separator(), helpButton);
		// add second toolbar
		final ToolBar secondToolBar = new ToolBar();
		searchComboBox = textInput.getComboBox();
		searchComboBox.setPromptText("Filter...");
		searchComboBox.setPrefWidth(Utilities.getRelativeSize(22));
		searchComboBox.setOnShowing(e -> recordQuery());
		searchTextField = (TextField)textInput.getInput();
		searchTextField.textProperty().addListener((obs, oldValue, newValue) -> {
			if(searchIn != SearchField.CONTENT) {
				final String strQuery = Normalizer.normalize(newValue.trim(), Form.NFC);
				if(isValidQuery(strQuery))
					search(strQuery);
				else
					clearResult();
				searchComboBox.commitValue();
			}
		});
		final Button clearButton = textInput.getClearButton();
		clearButton.setOnAction(actionEvent -> {
			recordQuery();
			searchTextField.clear();
			searchComboBox.commitValue();
		});
		searchComboBox.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				if(keyEvent.getCode() == KeyCode.ENTER) {
					search();
				} else if(keyEvent.getCode() == KeyCode.ESCAPE) {
					searchTextField.clear();
				}
			}
		});
		final ToggleGroup searchFieldGroup = new ToggleGroup();
		final RadioButton inTextRadio = new RadioButton("Text");
		final RadioButton inBookRadio = new RadioButton("Book");
		final RadioButton inGroupRadio = new RadioButton("Group");
		final RadioButton inContentRadio = new RadioButton("Content");
		searchFieldGroup.getToggles().addAll(inTextRadio, inBookRadio, inGroupRadio, inContentRadio);
		inTextRadio.setSelected(true);
		inTextRadio.setOnAction(actionEvent -> search(SearchField.TEXT));
		inBookRadio.setOnAction(actionEvent -> search(SearchField.BOOK));
		inGroupRadio.setOnAction(actionEvent -> search(SearchField.GROUP));
		inContentRadio.setOnAction(actionEvent -> search(SearchField.CONTENT));
		secondToolBar.getItems().addAll(new Separator(), searchComboBox, clearButton, textInput.getMethodButton(),
							new Separator(), new Label("Find in: "), inTextRadio, inBookRadio, inGroupRadio, inContentRadio);
		final VBox toolBox = new VBox();
		toolBox.getChildren().addAll(toolBar, secondToolBar);
		setTop(toolBox);
		
		// set up drop action
		this.setOnDragOver(dragEvent -> {
			if(dragEvent.getGestureSource() != this && dragEvent.getDragboard().hasString()) {
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			dragEvent.consume();
		});
		this.setOnDragDropped(dragEvent -> {
			final Dragboard db = dragEvent.getDragboard();
			if(db.hasString()) {
				final String[] allLines = db.getString().split("\\n");
				final String head = allLines[0].trim();
				if(!head.startsWith("::paliplatform")) {
					addTermToSearch(head);	
				}
				dragEvent.setDropCompleted(true);
			} else {
				dragEvent.setDropCompleted(false);
			}
			dragEvent.consume();
		});
	
		// set up status bar
		statusBox.getChildren().addAll(statusMessage);
		statusBox.setPadding(new Insets(2, 2, 0, 2));
		setBottom(statusBox);

		// some init
		helpPopup.setContent("info-finder.txt");
		helpPopup.setTextWidth(Utilities.getRelativeSize(38));
		progressBox.getChildren().addAll(progressBar, progressMessage);
		progressBox.setPadding(new Insets(2, 2, 0, 2));
		setPrefWidth(Utilities.getRelativeSize(67));
		Utilities.loadCSCDFiles();
		search();
	}

	public void init() {
		clearResult();
		searchTextField.clear();
		searchFieldGroup.selectToggle(inTextMenuItem);
		searchIn = SearchField.TEXT;
		search();
	}

	private CheckBox createTextGroupCheckBox(final TextGroup tgroup) {
		final String name = tgroup.getPaliName();
		final CheckBox cb = new CheckBox(name);
		cb.setAllowIndeterminate(false);
		cb.setSelected(true);
		cb.setTooltip(new Tooltip(tgroup.getEngName()));
		cb.setOnAction(actionEvent -> {
			if(cb.isSelected())
				textSet.add(tgroup);
			else
				textSet.remove(tgroup);
			search();
		});
		return cb;
	}
	
	private void setupTable() {
		if(resultList.isEmpty())
			return;
		final Callback<TableColumn<PaliDocument, String>, TableCell<PaliDocument, String>> textNameCellFactory = col -> {
			TableCell<PaliDocument, String> cell = new TableCell<PaliDocument, String>() {
				@Override
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					this.setText(null);
					this.setGraphic(null);
					if(!empty) {
						final String head = this.getTableColumn().getText();
						final String[] textnames = item.split(":");
						String display = "";
						if(head.equals("Text")) {
							display = textnames[0];
						}
						if(textnames.length > 1) {
							// from the archive
							if(head.equals("Book"))
								display = textnames[1];
							else if(head.equals("Group"))
								display = textnames[2];
						}
						this.setText(display);
						this.setTooltip(new Tooltip(display));
					}
				}
			};
			return cell;
		};
		final TableColumn<PaliDocument, String> textNameCol = new TableColumn<>("Text");
		textNameCol.setCellValueFactory(new PropertyValueFactory<>(resultList.get(0).textNameProperty().getName()));
		textNameCol.setCellFactory(textNameCellFactory);
		textNameCol.setComparator(PaliDocument.getTextNameStringComparator(0));
		textNameCol.prefWidthProperty().bind(mainPane.widthProperty().divide(17).multiply(5).subtract(20));
		final TableColumn<PaliDocument, String> bookCol = new TableColumn<>("Book");
		bookCol.setCellValueFactory(new PropertyValueFactory<>(resultList.get(0).textNameProperty().getName()));
		bookCol.setCellFactory(textNameCellFactory);
		bookCol.setComparator(PaliDocument.getTextNameStringComparator(1));
		bookCol.prefWidthProperty().bind(mainPane.widthProperty().divide(17).multiply(4));
		final TableColumn<PaliDocument, String> groupCol = new TableColumn<>("Group");
		groupCol.setCellValueFactory(new PropertyValueFactory<>(resultList.get(0).textNameProperty().getName()));
		groupCol.setCellFactory(textNameCellFactory);
		groupCol.setComparator(PaliDocument.getTextNameStringComparator(2));
		groupCol.prefWidthProperty().bind(mainPane.widthProperty().divide(17).multiply(4));
		final TableColumn<PaliDocument, String> fileNameCol = new TableColumn<>("File");
		fileNameCol.setCellValueFactory(new PropertyValueFactory<>(resultList.get(0).fileNameProperty().getName()));
		fileNameCol.setComparator(PaliDocument.getFileNameStringComparator());
		fileNameCol.prefWidthProperty().bind(mainPane.widthProperty().divide(17).multiply(3));
		final TableColumn<PaliDocument, Integer> searchResultCol = new TableColumn<>("#");
		searchResultCol.setCellValueFactory(new PropertyValueFactory<>(resultList.get(0).searchResultCountProperty().getName()));
		searchResultCol.prefWidthProperty().bind(mainPane.widthProperty().divide(17));
		searchResultCol.setStyle("-fx-alignment:center-right");
		searchResultCol.setCellFactory(col -> {
			TableCell<PaliDocument, Integer> cell = new TableCell<PaliDocument, Integer>() {
				@Override
				public void updateItem(final Integer item, final boolean empty) {
					super.updateItem(item, empty);
					this.setText(null);
					this.setGraphic(null);
					if(!empty) {
						if(item > 0)
							this.setText(String.format("%,d", item));
					}
				}
			};
			return cell;
		});
		table.getColumns().add(textNameCol);
		table.getColumns().add(bookCol);
		table.getColumns().add(groupCol);
		table.getColumns().add(fileNameCol);
		table.getColumns().add(searchResultCol);
	}

	private void clearResult() {
		resultList.clear();
		updateStatus();
	}

	private void addTermToSearch(final String term) {
		searchTextField.setText(term);
	}

	private boolean isValidQuery(final String text) {
		boolean result = true;
		final int threshold;
		if(searchIn == SearchField.CONTENT) {
			threshold = 3;
		} else {
			if(text.startsWith("*"))
				threshold = 2;
			else
				threshold = 0;
		}
		result = text.length() >= threshold;
		return result;
	}
	
	public void setSearchInput(final String text) {
		searchTextField.setText(text);
	}

	private void search() {
		final String text = Normalizer.normalize(searchTextField.getText().trim(), Form.NFC);
		if(isValidQuery(text))
			search(text);
		else
			clearResult();
	}

	private void search(final SearchField field) {
		searchIn = field;
		final String text = Normalizer.normalize(searchTextField.getText().trim(), Form.NFC);
		if(isValidQuery(text))
			search(text);
		else
			clearResult();
	}
	
	private void search(final String inputStr) {
		clearResult();
		if(searchIn != SearchField.CONTENT) {
			// search from file information
			final String strToFind = inputStr.toLowerCase();
			final String[] strQuery = new String[2];
			final WildCardPos wildcardPos;
			if(strToFind.isEmpty()) {
				wildcardPos = WildCardPos.NONE;
			} else {
				if(strToFind.startsWith("*")) {
					wildcardPos = WildCardPos.START;
					strQuery[0] = strToFind.substring(strToFind.indexOf("*") + 1).replaceAll("\\*.*?$", "");
				} else {
					if(strToFind.contains("*") && !strToFind.endsWith("*")) {
						wildcardPos = WildCardPos.MIDDLE;
						strQuery[0] = strToFind.substring(0, strToFind.indexOf("*"));
						strQuery[1] = strToFind.substring(strToFind.lastIndexOf("*") + 1);
					} else {
						wildcardPos = WildCardPos.END;
						strQuery[0] = strToFind.replaceAll("\\*.*?$", "");
					}
				}
			}
			Utilities.docInfoMap.forEach((id, doc) -> {
				final boolean cond;
				if(strToFind.isEmpty()) {
					cond = true;
				} else {
					final String title = doc.getTitle(searchIn.ordinal()).toLowerCase();
					int ind = 0;
					while(!Character.isLetter(title.charAt(ind))) {
						ind++;
					}
					if(wildcardPos == WildCardPos.START)
						cond = title.substring(ind).contains(strQuery[0]);
					else if(wildcardPos == WildCardPos.MIDDLE)
						cond = title.substring(ind).startsWith(strQuery[0]) && title.substring(ind).contains(strQuery[1]);
					else
						cond = title.substring(ind).startsWith(strQuery[0]);
				}
				if(cond){
					if(filterByTextGroup(id)) {
						final PaliDocument pd = new PaliDocument(doc.getFullTitle(), id+".xml");
						resultList.add(pd);
					}
				}
			});
			FXCollections.sort(resultList, PaliDocument.getFileNameComparator());
			updateStatus();
		} else {
			// brute full text search in the collection
			searchContent(inputStr);
		}
	}

	private boolean filterByTextGroup(final String id) {
		boolean result = false;
		if(textSet.contains(TextGroup.ANYA) && id.startsWith("e")) {
			result = true;
		} else if(textSet.contains(TextGroup.MULA) && id.matches(".*m\\d*\\..*")) {
			result = true;
		} else if(textSet.contains(TextGroup.ATTHA) && id.matches(".*a\\d*\\..*")) {
			result = true;
		} else if(textSet.contains(TextGroup.TIKA) && id.matches(".*t\\d*\\..*")) {
			result = true;
		}
		return result;
	}
	
	private void searchContent(final String text) {
		setBottom(null);
		if(searchTask != null) {
			searchTask.cancel(true);
			progressBar.progressProperty().unbind();
		}
		progressBar.setProgress(0);
		Pattern searchPatt;
		try {
			searchPatt = Pattern.compile(text);
		} catch(PatternSyntaxException e) {
			return;
		}
		searchTask = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				final int total = Utilities.cscdFiles.size();
				try{
					final ZipFile zip = new ZipFile(new File(Utilities.ROOTDIR + Utilities.COLLPATH + Utilities.CSCD_ZIP));
					int count = 0;
					updateMessage("Searching... (please wait)");
					for(final String filename : Utilities.cscdFiles) {
						final String id = filename.substring(0, filename.indexOf(".xml"));
						if(!filterByTextGroup(id)) continue;
						final ZipEntry entry = zip.getEntry(Utilities.CSCD_DIR + filename);
						final Scanner in = new Scanner(zip.getInputStream(entry), "UTF-8");
						final long foundCount = in.findAll(searchPatt).count();
						if(foundCount > 0) {
							final DocInfo doc = Utilities.docInfoMap.get(id);
							final PaliDocument pd = new PaliDocument(doc.getFullTitle(), filename);
							pd.searchResultCountProperty().set((int)foundCount);
							resultList.add(pd);
						}
						in.close();
						updateProgress(++count, total);
					}
					zip.close();
				} catch(IOException e) {
					System.err.println(e);
				}
				Platform.runLater(() -> {
					progressBar.progressProperty().unbind();
					mainPane.setBottom(statusBox);
					updateStatus();
				});
				return true;
			}
		};
		progressBar.progressProperty().bind(searchTask.progressProperty());
		searchTask.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			progressMessage.setText(newValue);
		});
		PaliPlatform.threadPool.submit(searchTask);
		mainPane.setBottom(progressBox);
	}

	private void updateStatus() {
		final int num = resultList.size();
		final String s = num > 1 ? "s" : "";
		final String text = num == 0 ? "No item found" : num + " item" + s + " found";
		statusMessage.setText(text);
	}

	private void recordQuery() {
		if(!resultList.isEmpty())
			textInput.recordQuery();
	}

	private void openText() {
		final PaliDocument pd = table.getSelectionModel().getSelectedItem();
		if(pd != null && pd.getFileName().length() > 0) {
			PaliPlatform.openPaliHtmlViewer(pd.toTOCTreeNode());	
		}
	}

	private void openAsText(final boolean withNotes) {
		final PaliDocument pd = table.getSelectionModel().getSelectedItem();
		if(pd != null && pd.getFileName().length() > 0)
			Utilities.openXMLDocAsText(pd.toTOCTreeNode(), withNotes);
	}
	
	private void addBookmark() {
		for(final PaliDocument pd : table.getSelectionModel().getSelectedItems()) {
			if(pd != null && pd.getFileName().length() > 0)
				Utilities.addBookmark(pd.toTOCTreeNode());
		}
		Bookmarks.INSTANCE.save();			
	}

	private void sendToTokenizer() {
		for(final PaliDocument pd : table.getSelectionModel().getSelectedItems())
			PaliPlatform.sendToTokenizer(new TreeItem<TOCTreeNode>(pd.toTOCTreeNode()));
	}

	private String makeCSV() {
		// generate csv string
		final StringBuilder result = new StringBuilder();
		// table columns
		for(int i=0; i<table.getColumns().size(); i++){
			result.append(table.getColumns().get(i).getText()).append(Utilities.csvDelimiter);
		}
		result.append(System.getProperty("line.separator"));
		// table data
		for(int i=0; i<table.getItems().size(); i++){
			final PaliDocument pd = table.getItems().get(i);
			final String[] textNames = pd.getTextName().split(":");
			result.append(textNames[0]).append(Utilities.csvDelimiter);
			if(textNames.length > 1) {
				result.append(textNames[1]).append(Utilities.csvDelimiter);
				result.append(textNames[2]).append(Utilities.csvDelimiter);
			}
			result.append(pd.getFileName()).append(Utilities.csvDelimiter);
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}
	
	private void copyCSV() {
		Utilities.copyText(makeCSV());
	}
	
	private void saveCSV() {
		Utilities.saveText(makeCSV(), "search-result.csv");
	}
}
