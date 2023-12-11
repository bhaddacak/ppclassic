/*
 * PaliHtmlViewer.java
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

package paliplatform.viewer;

import paliplatform.*;
import paliplatform.grammar.*;
import paliplatform.toctree.TOCTreeNode;

import java.util.*;
import java.util.stream.*;
import java.util.regex.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.*;
import javafx.event.*;

import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

/** 
 * The viewer of HTML Pali texts. The very source of texts is in fact in XML agreeable to 
 * VRI's CSCD compilation format. XSLT is used to transform XML to HTML.
 * 
 * @author J.R. Bhaddacak
 * @version 2.1
 * @since 2.0
 */
public class PaliHtmlViewer extends HtmlViewer {
	enum LeftListType { HEADING, PARANUM, GATHA }
	static final double DIVIDER_POSITION_LEFT = 0.2;
	static final double DIVIDER_POSITION_RIGHT = 0.8;
	final SplitPane splitPane = new SplitPane();
	final BorderPane textPane = new BorderPane();
	final BorderPane rightPane = new BorderPane();
	final BorderPane leftPane = new BorderPane();
	final ViewerToolBar toolBar;
	String docFilename;
	String pageBody;
	Stage theStage;
	TOCTreeNode thisDoc;
	private final ObservableList<String> docTocList = FXCollections.<String>observableArrayList();
	private final FindReplaceBox findReplaceBox = new FindReplaceBox(this);
	private final TextField findInput = findReplaceBox.getFindTextField();
	private final ComboBox<String> findInputCombo = findReplaceBox.getFindComboBox();
	private final List<RadioMenuItem> scriptRadioMenu = new ArrayList<>();
	private final HashSet<Integer> titleIdSet = new HashSet<>();
	private final HashSet<Integer> subheadIdSet = new HashSet<>();
	private final ToggleButton showNoteButton;
	private final ToggleButton showXRefButton;
	private ToggleButton toggleNumberButton;
	private LeftListType currLeftListType = LeftListType.HEADING;
	private String recentJS = "";
	private String viewerTheme;
	boolean isBW = false;
	private boolean showNotes = true;
	private boolean showXref = false;
	private final SimpleObjectProperty<Utilities.PaliScript> displayScript = new SimpleObjectProperty<>(Utilities.PaliScript.ROMAN);
	private boolean alsoConvertNumber = false;
	private final SimpleBooleanProperty searchTextFound = new SimpleBooleanProperty(false);
	private final SimpleStringProperty clickedText = new SimpleStringProperty("");
	private final InfoPopup infoPopup = new InfoPopup();

	public PaliHtmlViewer(final TOCTreeNode node, final String jumpTargetParaNum) {
		thisDoc = node;
		webEngine.setUserStyleSheetLocation(PaliPlatform.class.getResource(Utilities.CSCD_CSS).toExternalForm());
		// Set the member for the browser's window object after the document loads
		final ViewerFXHandler fxHandler = new ViewerFXHandler(this);
	 	webEngine.getLoadWorker().stateProperty().addListener((prop, oldState, newState) -> {
			if(newState == Worker.State.SUCCEEDED) {
				JSObject jsWindow = (JSObject)webEngine.executeScript("window");
				jsWindow.setMember("fxHandler", fxHandler);
				webEngine.executeScript("init()");
				if(!jumpTargetParaNum.isEmpty()) {
					Platform.runLater(() -> webEngine.executeScript("jumpToParaNum('" + jumpTargetParaNum + "')"));			
				}
				setViewerTheme(PaliPlatform.settings.getProperty("theme"));
				setViewerFont();
			}
		});		
		textPane.setCenter(webView);
		// config Find (& Replace) Box
		findReplaceBox.getFindTextInput().setInputMethod(PaliTextInput.InputMethod.valueOf(PaliPlatform.settings.getProperty("pali-input-method")));
		findReplaceBox.getFindButton().setOnAction(actionEvent -> doRegExFind());
		findReplaceBox.getPrevButton().disableProperty().bind(searchTextFound.not());
		findReplaceBox.getPrevButton().setOnAction(actionEvent -> findNext(-1));
		findReplaceBox.getNextButton().disableProperty().bind(searchTextFound.not());
		findReplaceBox.getNextButton().setOnAction(actionEvent -> findNext(+1));
		findReplaceBox.getCloseButton().setOnAction(actionEvent -> textPane.setBottom(null));
		findInputCombo.setOnShowing(e -> recordQuery(findReplaceBox.getFindTextInput()));
		findReplaceBox.getClearFindButton().setOnAction(actionEvent -> clearFindInput());
		findInputCombo.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				final KeyCode key = keyEvent.getCode();
				if(key == KeyCode.ENTER) {
					if(findReplaceBox.wholeWordProperty().get() || findReplaceBox.regexSearchProperty().get())
						doRegExFind();
					else
						findNext(+1);
				} else if(key == KeyCode.ESCAPE) {
					clearFindInput();
				}
			}
		});
		findInput.textProperty().addListener((obs, oldValue, newValue) -> {
			if(!(findReplaceBox.wholeWordProperty().get() || findReplaceBox.regexSearchProperty().get())) {
				startImmediateSearch(Normalizer.normalize(newValue, Form.NFC));
			}
		});
		// add find & replace box first, and remove it; this makes the focus request possible		
		textPane.setBottom(findReplaceBox);
		
		// set up the right side pane
		// add document information
		final List<String> titles = new ArrayList<>(4);
		docFilename = node.getFileName();
		if(node.isInArchive()) {
			titles.add(Utilities.getDocTitle(docFilename, 2));
			titles.add(Utilities.getDocTitle(docFilename, 1));
			titles.add(Utilities.getDocTitle(docFilename, 0));
			titles.add(docFilename);
		} else {
			titles.add(docFilename);
		}
		final VBox selfInfoBox = new VBox();
		final Label[] selfInfos = new Label[titles.size()];
		for(int i=0; i<titles.size(); i++) {
			switch(i) {
				case 0: selfInfos[i] = new Label(titles.get(i)); break;
				case 1: selfInfos[i] = new Label("┕━ " + titles.get(i)); break;
				case 2: 
					selfInfos[i] = new Label("    ┕━ " + titles.get(i));
					selfInfos[i].getStyleClass().add("emphasized");
					break;
				case 3: selfInfos[i] = new Label("          (" + titles.get(i) + ")"); break;
			}
			selfInfoBox.getChildren().add(selfInfos[i]);
		}
		// add relative links information
		int baseNum = 0;
		final ObservableList<String> relLinkInfoList = FXCollections.<String>observableArrayList();
		final ListView<String> relLinkInfoListView = new ListView<>(relLinkInfoList);
		List<String> upperLinks = new ArrayList<>();
		List<String> lowerLinks = new ArrayList<>();
		if(node.isInArchive()) {
			upperLinks = Utilities.getDocUpperLinks(docFilename);
			lowerLinks = Utilities.getDocLinks(docFilename);
			final List<String> links = new ArrayList<>();
			String title;
			if(!upperLinks.isEmpty()) {
				for(String s : upperLinks) {
					title = Utilities.getDocTitle(s+".xml", 0) +", "+ Utilities.getDocTitle(s+".xml", 1);
					links.add(title + " (" + s + ".xml)");
				}
			}
			baseNum = links.size();
			links.add(titles.get(2));
			for(String s : lowerLinks) {
				title = Utilities.getDocTitle(s+".xml", 0) +", "+ Utilities.getDocTitle(s+".xml", 1);
				links.add(title + " (" + s + ".xml)");
			}
			relLinkInfoList.setAll(links);
		}
		// customize list cells
		final int baseIndex = baseNum;
		final List<String> uLinks = new ArrayList<>(upperLinks);
		final List<String> dLinks = new ArrayList<>(lowerLinks);		
		relLinkInfoListView.setCellFactory((ListView<String> lv) -> {
			return new ListCell<String>() {
				@Override
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					final int index = this.getIndex();
					this.setGraphic(null);
					if (empty) {
						this.setText(null);
						this.setTooltip(null);
					} else {
						ObservableList<String> olist = lv.getItems();
						String value = this.getItem();
						this.setTooltip(new Tooltip(value));
						if(baseIndex == 0) {
							// no upper links
							if(index > baseIndex)
								if(index == olist.size()-1)
									value = "┕━ " + value;
								else
									value = "┝━ " + value;				
						} else {
							if(index == baseIndex)
								value = "┕━ " + value;
							else if(index > baseIndex)
								if(index == olist.size()-1)
									value = "      ┕━ " + value;
								else
									value = "      ┝━ " + value;
						}				
						this.setText(value);
					}
					if(index == baseIndex)
						this.getStyleClass().add("emphasized");
					this.setStyle("-fx-padding: 0px 0px 0px 3px");
				}
			};
		});
		
		// use VBox as the container of the right pane
		final VBox sideBox = new VBox();
		VBox.setVgrow(relLinkInfoListView, Priority.ALWAYS);
		final ObservableList<Node> boxChildren = sideBox.getChildren();
		selfInfoBox.getStyleClass().add("bordered-box");
		boxChildren.add(selfInfoBox);
		if(node.isInArchive()) {
			// add relative links
			final StackPane reldocHead = new StackPane();
			reldocHead.getChildren().add(new Label("Related documents"));
			reldocHead.getStyleClass().add("head-inverted");
			boxChildren.add(reldocHead);
			boxChildren.add(relLinkInfoListView);
			// add context menus to the right pane
			final ContextMenu relLinkPopupMenu = new ContextMenu();
			final MenuItem openMenuItem = new MenuItem("Open");
			openMenuItem.setOnAction(actionEvent -> {
				int ind = relLinkInfoListView.getSelectionModel().getSelectedIndex();
				final String selectedText = relLinkInfoListView.getSelectionModel().getSelectedItem();
				String selectedFile = "";
				if(ind < baseIndex) {
					selectedFile = uLinks.get(ind) + ".xml";
				} else if(ind > baseIndex) {
					selectedFile = dLinks.get(ind - baseIndex - 1) + ".xml";
				}
				if(selectedFile.length() > 0) {
					TOCTreeNode selectedNode = new TOCTreeNode(selectedText, selectedFile, true);
					PaliPlatform.openPaliHtmlViewer(selectedNode);
				}
			});
			relLinkPopupMenu.getItems().addAll(openMenuItem);
			
			// add mouse listener
			relLinkInfoListView.setOnMouseClicked(mouseEvent -> {
				final int ind = relLinkInfoListView.getSelectionModel().getSelectedIndex();
				final String selectedText = relLinkInfoListView.getSelectionModel().getSelectedItem();
				if(selectedText != null && selectedText.length() > 0) {
					if(ind != baseIndex)
						relLinkPopupMenu.show(relLinkInfoListView, mouseEvent.getScreenX(), mouseEvent.getScreenY());
					else
						relLinkPopupMenu.hide();
				} else {
					relLinkPopupMenu.hide();
				}
			});
		}
		// set right components
		rightPane.setCenter(sideBox);
		
		// prepare the left pane's content
		final ListView<String> docTocListView = new ListView<>(docTocList);
		// customize list cells
		docTocListView.setCellFactory((ListView<String> lv) -> {
			return new ListCell<String>() {
				@Override
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					final int index = this.getIndex();
					this.setGraphic(null);
					if (empty) {
						this.setText(null);
						this.setTooltip(null);
					} else {
						final String value = this.getItem();
						this.setTooltip(new Tooltip(value));
						if(currLeftListType==LeftListType.HEADING && subheadIdSet.contains(index) && !titleIdSet.isEmpty())
							this.setText("  - " + value);
						else
							this.setText(value);
					}
					this.setStyle("-fx-padding: 0px 0px 0px 3px");
				}
			};
		});
		// add mouse listener
		docTocListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			final int ind = docTocListView.getSelectionModel().getSelectedIndex();
			if(ind >= 0) {
				final String prefix = "" + currLeftListType.toString().toLowerCase().charAt(0);
				final String command = "jumpTo('" + prefix + ind + "')";
				webEngine.executeScript(command);
				recentJS = command;
			}
		});
		// add left pane's toolbar
		final ToolBar leftpaneToolbar = new ToolBar();
		final ToggleButton leftHeadingButton = new ToggleButton("", new TextIcon("heading", TextIcon.IconSet.AWESOME));
		leftHeadingButton.setTooltip(new Tooltip("Heading list"));
		leftHeadingButton.setOnAction(actionEvent -> setDocToc(LeftListType.HEADING));
		final ToggleButton leftParnumButton = new ToggleButton("", new TextIcon("paragraph", TextIcon.IconSet.AWESOME));
		leftParnumButton.setTooltip(new Tooltip("Paragraph number list"));
		leftParnumButton.setOnAction(actionEvent -> setDocToc(LeftListType.PARANUM));
		final ToggleButton leftGathaButton = new ToggleButton("", new TextIcon("music", TextIcon.IconSet.AWESOME));
		leftGathaButton.setTooltip(new Tooltip("Stanza list"));
		leftGathaButton.setOnAction(actionEvent -> setDocToc(LeftListType.GATHA));
		final ToggleGroup leftListTypeGroup = new ToggleGroup();
		leftListTypeGroup.getToggles().addAll(leftHeadingButton, leftParnumButton, leftGathaButton);
		leftListTypeGroup.selectToggle(
			currLeftListType==LeftListType.HEADING ? leftHeadingButton :
			currLeftListType==LeftListType.PARANUM ? leftParnumButton :
			leftGathaButton);
		leftpaneToolbar.getItems().addAll(leftHeadingButton, leftParnumButton, leftGathaButton);
		// set left components
		leftPane.setTop(leftpaneToolbar);
		leftPane.setCenter(docTocListView);
		
		// compose SplitPane, the left pane is not shown at start
		splitPane.setDividerPositions(DIVIDER_POSITION_RIGHT);
		splitPane.getItems().addAll(textPane, rightPane);	
		
		// add main components
		// add context menus to the main pane
		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem readMenuItem = new MenuItem("Read this portion");
		readMenuItem.disableProperty().bind(clickedText.isEmpty());
		readMenuItem.setOnAction(actionEvent -> openReader());
		final MenuItem openDictMenuItem = new MenuItem("Open Dictionaries");
		openDictMenuItem.setOnAction(actionEvent -> openDict());
		final MenuItem sendToDictMenuItem = new MenuItem("Send to Dictionaries");
		sendToDictMenuItem.setOnAction(actionEvent -> sendToDict());
		final MenuItem calMetersMenuItem = new MenuItem("Calculate meters");
		calMetersMenuItem.disableProperty().bind(clickedText.isEmpty());
		calMetersMenuItem.setOnAction(actionEvent -> calculateMeters());
		final MenuItem analyzeMenuItem = new MenuItem("Analyze this stanza/portion");
		analyzeMenuItem.disableProperty().bind(clickedText.isEmpty());
		analyzeMenuItem.setOnAction(actionEvent -> openAnalyzer());
		final MenuItem openAsTextMenuItem = new MenuItem("Open as text");
		openAsTextMenuItem.setOnAction(actionEvent -> Utilities.openXMLDocAsText(thisDoc, true));
		final MenuItem openAsTextNoNotesMenuItem = new MenuItem("Open as text (no notes)");
		openAsTextNoNotesMenuItem.setOnAction(actionEvent -> Utilities.openXMLDocAsText(thisDoc, false));
		contextMenu.getItems().addAll(readMenuItem, openDictMenuItem, sendToDictMenuItem, calMetersMenuItem, analyzeMenuItem,
										new SeparatorMenuItem(),openAsTextMenuItem, openAsTextNoNotesMenuItem);
		webView.setOnMousePressed(mouseEvent -> {
			if(mouseEvent.getButton() == MouseButton.SECONDARY) {
				if(displayScript.get() == Utilities.PaliScript.ROMAN)
					contextMenu.show(webView, mouseEvent.getScreenX(), mouseEvent.getScreenY());
			} else {
				contextMenu.hide();
			}
		});		
		// add main toolbar
		toolBar = new ViewerToolBar(this, webView);
		// configure some buttons first
		toolBar.saveTextButton.setTooltip(new Tooltip("Save selection as text"));
		toolBar.saveTextButton.setOnAction(actionEvent -> saveSelection());
		toolBar.copyButton.setTooltip(new Tooltip("Copy selection to clipboard"));
		toolBar.copyButton.setOnAction(actionEvent -> copySelection());
		// add new buttons
		final Button toggleRightPaneButton = new Button("", new TextIcon("right-pane", TextIcon.IconSet.CUSTOM));
		toggleRightPaneButton.setTooltip(new Tooltip("Right pane on/off"));
		toggleRightPaneButton.setOnAction(actionEvent -> {
			final ObservableList<Node> ol = splitPane.getItems();
			boolean removed = false;
			if(ol.contains(rightPane)) {
				ol.remove(rightPane);
				removed = true;
			} else {
				ol.add(rightPane);
			}
			final double[] divPos = splitPane.getDividerPositions();
			if(divPos.length == 1) {
				if(removed)
					splitPane.setDividerPositions(DIVIDER_POSITION_LEFT);
				else
					splitPane.setDividerPositions(DIVIDER_POSITION_RIGHT);
			} else if(divPos.length == 2) {
				splitPane.setDividerPositions(DIVIDER_POSITION_LEFT, DIVIDER_POSITION_RIGHT);
			}
		});
		final Button toggleLeftPaneButton = new Button("", new TextIcon("left-pane", TextIcon.IconSet.CUSTOM));
		toggleLeftPaneButton.setTooltip(new Tooltip("Left pane on/off"));
		toggleLeftPaneButton.setOnAction(actionEvent -> {
			final ObservableList<Node> ol = splitPane.getItems();
			boolean removed = false;
			if(ol.contains(leftPane)) {
				ol.remove(leftPane);
				removed = true;
			} else {
				if(ol.size() == 1)
					ol.setAll(leftPane, textPane);
				else if(ol.size() == 2)
					ol.setAll(leftPane, textPane, rightPane);
			}
			final double[] divPos = splitPane.getDividerPositions();
			if(divPos.length == 1) {
				if(removed)
					splitPane.setDividerPositions(DIVIDER_POSITION_RIGHT);
				else
					splitPane.setDividerPositions(DIVIDER_POSITION_LEFT);
			} else if(divPos.length == 2) {
				splitPane.setDividerPositions(DIVIDER_POSITION_LEFT, DIVIDER_POSITION_RIGHT);
			}
			if(!removed) {
				setDocToc(currLeftListType);
			}
		});
		final Button toggleFullViewButton = new Button("", new TextIcon("full-view", TextIcon.IconSet.CUSTOM));
		toggleFullViewButton.setTooltip(new Tooltip("Full view on/off"));
		toggleFullViewButton.setOnAction(actionEvent -> {
			final ObservableList<Node> ol = splitPane.getItems();
			if(ol.size() > 1)
				ol.setAll(textPane);
			else
				ol.setAll(leftPane, textPane, rightPane);
			final double[] divPos = splitPane.getDividerPositions();
			if(divPos.length == 2) {
				splitPane.setDividerPositions(DIVIDER_POSITION_LEFT, DIVIDER_POSITION_RIGHT);
			}
		});
		final Button recentJumpButton = new Button("", new TextIcon("arrow-turn-down", TextIcon.IconSet.AWESOME));
		recentJumpButton.setTooltip(new Tooltip("The last jump"));
		recentJumpButton.setOnAction(actionEvent -> {
			if(recentJS.length() > 0)
				webEngine.executeScript(recentJS);
		});
		showNoteButton = new ToggleButton("", new TextIcon("note-sticky", TextIcon.IconSet.AWESOME));
		showNoteButton.setTooltip(new Tooltip("Show/hide redactional notes"));
		showNoteButton.setSelected(showNotes);
		showNoteButton.setOnAction(actionEvent -> {
			final boolean selected = showNoteButton.isSelected();
			showNotes = selected;
			setShowNotes();
		});
		showXRefButton = new ToggleButton("", new TextIcon("hashtag", TextIcon.IconSet.AWESOME));
		showXRefButton.setTooltip(new Tooltip("Show/hide publication references"));
		showXRefButton.setSelected(showXref);
		showXRefButton.setOnAction(actionEvent -> {
			final boolean selected = showXRefButton.isSelected();
			showXref = selected;
			setShowXref();
		});
		final MenuButton convertMenu = new MenuButton("", new TextIcon("language", TextIcon.IconSet.AWESOME));
		convertMenu.setTooltip(new Tooltip("Convert the script to"));
		final ToggleGroup scriptGroup = new ToggleGroup();
		for(final Utilities.PaliScript sc : Utilities.PaliScript.values()){
			if(sc.ordinal() == 0) continue;
			final String n = sc.toString();
			final RadioMenuItem scriptItem = new RadioMenuItem(n.charAt(0) + n.substring(1).toLowerCase());
			scriptItem.setToggleGroup(scriptGroup);
			scriptItem.setSelected(scriptItem.getText().toUpperCase().equals(displayScript.get().toString()));
			convertMenu.getItems().add(scriptItem);
			scriptRadioMenu.add(scriptItem);
		}
        scriptGroup.selectedToggleProperty().addListener((observable) -> {
			if(scriptGroup.getSelectedToggle() != null) {
				final RadioMenuItem selected = (RadioMenuItem)scriptGroup.getSelectedToggle();
				final Utilities.PaliScript toScript = Utilities.PaliScript.valueOf(selected.getText().toUpperCase());
				if(displayScript.get() != toScript) {
					final Utilities.PaliScript fromScript = displayScript.get();
					displayScript.set(toScript);
					toolBar.setupFontMenu(toScript);
					convertToScript(toScript, fromScript, alsoConvertNumber);
					setShowNotes();
					setShowXref();
				}
			}
        });
		toggleNumberButton = new ToggleButton("", new TextIcon("0-9", TextIcon.IconSet.SANS));
		toggleNumberButton.setTooltip(new Tooltip("Number conversion on/off"));
		toggleNumberButton.setSelected(alsoConvertNumber);
		toggleNumberButton.setOnAction(actionEvent -> {
			alsoConvertNumber = toggleNumberButton.isSelected();
			if(displayScript.get() != Utilities.PaliScript.ROMAN || displayScript.get() != Utilities.PaliScript.SINHALA) {
				convertToScript(displayScript.get(), displayScript.get(), alsoConvertNumber);
				setShowNotes();
				setShowXref();
			}
		});
		final MenuButton findMenu = new MenuButton("", new TextIcon("magnifying-glass", TextIcon.IconSet.AWESOME));
		findMenu.setTooltip(new Tooltip("Find"));
		final MenuItem findMenuItem = new MenuItem("Find");
		findMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));
		findMenuItem.setOnAction(actionEvent -> openFind());
		final MenuItem findNextMenuItem = new MenuItem("Find Next");
		findNextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN));
		findNextMenuItem.disableProperty().bind(searchTextFound.not());		
		findNextMenuItem.setOnAction(actionEvent -> findNext(+1));
		final MenuItem findPrevMenuItem = new MenuItem("Find Previous");
		findPrevMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		findPrevMenuItem.disableProperty().bind(searchTextFound.not());		
		findPrevMenuItem.setOnAction(actionEvent -> findNext(-1));
		findMenu.getItems().addAll(findMenuItem, findNextMenuItem, findPrevMenuItem);
		
		// add components to the toolbar
		toolBar.getItems().addAll(new Separator(), toggleLeftPaneButton, toggleFullViewButton, toggleRightPaneButton
								, new Separator(), recentJumpButton, showNoteButton, showXRefButton
								, new Separator(), convertMenu, toggleNumberButton
								, new Separator(), findMenu
								);

		setTop(toolBar);
		setCenter(splitPane);
		
		// prepare info popup used for dictionary display
		infoPopup.setTextWidth(Utilities.getRelativeSize(25));

		// some other initialization
		Utilities.loadCPEDTerms(); // used in dict look up		
		Utilities.loadSandhiList();
		Platform.runLater(() -> {
			Utilities.createDeclPronounsMap();
			Utilities.createDeclNumbersMap();
			Utilities.createDeclIrrNounsMap();
		});

		init(node, jumpTargetParaNum);
	}
	
	public void init(final TOCTreeNode node, final String jumpTargetParaNum) {
		thisDoc = node;
		docFilename = node.getFileName();
		findInputCombo.getItems().clear();
		if(theStage != null)
			theStage.setTitle(node.getTextName());
		Platform.runLater(() -> textPane.setBottom(null));			
		showNoteButton.setSelected(true);
		showXRefButton.setSelected(false);
		loadContent();
		if(!jumpTargetParaNum.isEmpty())
			Platform.runLater(() -> webEngine.executeScript("jumpToParaNum('" + jumpTargetParaNum + "')"));			
	}

	// for generic viewer, this has to be called
	public void loadContent() {
		pageBody = Utilities.readXML(thisDoc.getFileName(), thisDoc.isInArchive());
		setContent(Utilities.makeHTML(pageBody, true));

	}

	public void clearContent() {
		scriptRadioMenu.get(0).setSelected(true);
		toggleNumberButton.setSelected(false);
		final ObservableList<Node> ol = splitPane.getItems();
		if(!ol.contains(rightPane))
			ol.add(rightPane);
		if(ol.contains(leftPane))
			ol.remove(leftPane);
		splitPane.setDividerPositions(DIVIDER_POSITION_RIGHT);
		setContent(Utilities.makeHTML("", true));
	}

	public void setStage(final Stage stage) {
		theStage = stage;
		theStage.setTitle(thisDoc.getTextName());
		theStage.setOnCloseRequest(new EventHandler<WindowEvent>() {  
			@Override
			public void handle(final WindowEvent event) {
				clearContent();
			}
		});
	}

	public void setViewerTheme(final String theme) {
		viewerTheme = theme;
		setTheme();
	}
	
	public void setViewerTheme(boolean bw) {
		isBW = bw;
		setTheme();
	}
	
	private void setTheme() {
		String command = "setThemeBW('"+viewerTheme+"',"+(isBW?1:0)+");";
		if(showXref)
			command += "setXrefColor('"+viewerTheme+"',"+(isBW?1:0)+");";
		webEngine.executeScript(command);
	}
	
	public void setViewerFont() {
		toolBar.resetFont(displayScript.get());
	}
	
	public void setViewerFont(final String fontname) {
		toolBar.setFontMenu(fontname);
		webEngine.executeScript("setFont('" + fontname + "')");
	}

	private void setViewerFont(final Utilities.PaliScript script) {
		toolBar.resetFont(script);
	}
	
	private void setDocToc(final LeftListType listType) {
		currLeftListType = listType;
		final HTMLDocument hdoc = (HTMLDocument)webEngine.getDocument();
		final HTMLElement body = hdoc.getBody();
		final List<String> docToc = new ArrayList<>();
		org.w3c.dom.NodeList nlist;
		org.w3c.dom.Node nd = null;
		int ind = 0;
		if(listType == LeftListType.HEADING) {
			nlist = body.getElementsByTagName("P");
			for(int i=0; i<nlist.getLength(); i++) {
				nd = nlist.item(i);
				if(nd.hasAttributes()) {
					final org.w3c.dom.NamedNodeMap nnm = nd.getAttributes();
					final String clss = nnm.getNamedItem("class").getTextContent();
					if(clss.equals("title") || clss.equals("subhead")) {
						if(clss.equals("title"))
							titleIdSet.add(ind);
						else if(clss.equals("subhead"))
							subheadIdSet.add(ind);
						docToc.add(nd.getTextContent());
						final org.w3c.dom.Attr attrId = hdoc.createAttribute("id");
						attrId.setValue("jumptarget-h"+ind);
						nnm.setNamedItem(attrId);
						ind++;
					}
				}
			}
		} else if(listType == LeftListType.PARANUM) {
			nlist = body.getElementsByTagName("SPAN");
			for(int i=0; i<nlist.getLength(); i++) {
				nd = nlist.item(i);
				if(nd.hasAttributes()) {
					final org.w3c.dom.NamedNodeMap nnm = nd.getAttributes();
					final String clss = nnm.getNamedItem("class").getTextContent();
					if(clss.equals("paranum")) {
						docToc.add(nd.getTextContent());
						final org.w3c.dom.Attr attrId = hdoc.createAttribute("id");
						attrId.setValue("jumptarget-p"+ind);
						nnm.setNamedItem(attrId);
						ind++;
					}
				}
			}
		} else if(listType == LeftListType.GATHA) {
			nlist = body.getElementsByTagName("P");
			for(int i=0; i<nlist.getLength(); i++) {
				nd = nlist.item(i);
				if(nd.hasAttributes()) {
					final org.w3c.dom.NamedNodeMap nnm = nd.getAttributes();
					final String clss = nnm.getNamedItem("class").getTextContent();
					if(clss.equals("gatha1")) {
						String text = nd.getTextContent().trim();
						text = text.replaceAll("\\[.*?\\]", "");
						int cutPos = text.indexOf(",");
						if(cutPos == -1)
							cutPos = text.indexOf(";");
						if(cutPos == -1)
							text = text.substring(0);
						else
							text = text.substring(0, cutPos);
						text = text.replaceAll("[ ]{2,}", " ");
						text = text.replace("‘", "");
						docToc.add(text);
						final org.w3c.dom.Attr attrId = hdoc.createAttribute("id");
						attrId.setValue("jumptarget-g"+ind);
						nnm.setNamedItem(attrId);
						ind++;
					}
				}
			}
		}
		docTocList.setAll(docToc);
	}
	
	private void openDict() {
		copySelection();
		final Clipboard cboard = Clipboard.getSystemClipboard();
		final String text = cboard.hasString() ? cboard.getString().trim() : "";
		PaliPlatform.openDict(Utilities.getUsablePaliWord(text));
	}
	
	private void sendToDict() {
		copySelection();
		final Clipboard cboard = Clipboard.getSystemClipboard();
		final String text = cboard.hasString() ? cboard.getString().trim() : "";
		PaliPlatform.showDict(Utilities.getUsablePaliWord(text));
	}
	
	public void showDictResult(final String text) {
		final String word = Utilities.getUsablePaliWord(text);
		// if the term is a sandhi word, cut it
		final List<String> parts = Utilities.cutSandhi(word);
		if(parts.size() > 1) {
			final String res = parts.stream().collect(Collectors.joining(" + "));
			infoPopup.setTitle(res);
			infoPopup.setBody("");
			infoPopup.showPopup(webView, InfoPopup.Pos.BELOW_RIGHT, false);
			return;
		}
		// if the term is in declined pronoun list, show it
		if(Utilities.declPronounsMap.containsKey(word)) {
			final DeclinedWord dword = Utilities.declPronounsMap.get(word);
			final String meaning = dword.getMeaning();
			final String caseMeaning = dword.getCaseMeaningString();
			final String head = caseMeaning.isEmpty() ? "" : "(" + caseMeaning + ") ";
			infoPopup.setTitle(word);
			infoPopup.setBody(head + meaning + "\n(" + dword.getCaseString() + ") (" + dword.getNumberString() + ") (" + dword.getGenderString() + ")");
			infoPopup.showPopup(webView, InfoPopup.Pos.BELOW_RIGHT, false);
			return;
		}
		// if the term is in declined numeral list, show it
		if(Utilities.declNumbersMap.containsKey(word)) {
			final DeclinedWord dword = Utilities.declNumbersMap.get(word);
			final String meaning = dword.getMeaning();
			final String caseMeaning = dword.getCaseMeaningString();
			final String head = caseMeaning.isEmpty() ? "" : "(" + caseMeaning + ") ";
			infoPopup.setTitle(word);
			infoPopup.setBody(head + meaning + "\n(" + dword.getCaseString() + ") (" + dword.getNumberString() + ") (" + dword.getGenderString() + ")");
			infoPopup.showPopup(webView, InfoPopup.Pos.BELOW_RIGHT, false);
			return;
		}
		// if the term is in declined noun/adj list, show it
		if(Utilities.declIrrNounsMap.containsKey(word)) {
			final DeclinedWord dword = Utilities.declIrrNounsMap.get(word);
			final String meaning = dword.getMeaning();
			final String caseMeaning = dword.getCaseMeaningString();
			final String head = caseMeaning.isEmpty() ? "" : "(" + caseMeaning + ") ";
			infoPopup.setTitle(word);
			infoPopup.setBody(head + meaning + "\n(" + dword.getCaseString() + ") (" + dword.getNumberString() + ") (" + dword.getGenderString() + ")");
			infoPopup.showPopup(webView, InfoPopup.Pos.BELOW_RIGHT, false);
			return;
		}
		// or else find in the concise dict until something matched
		String term = word;
		PaliWord pword = new PaliWord(term);
		while(pword.getMeaning().isEmpty() && term.length() > 1) {
			final String tfilter = term;
			final Set<String> results = Utilities.cpedTerms.stream().filter(x -> x.startsWith(tfilter)).collect(Collectors.toSet());
			if(!results.isEmpty()) {
				final List<String> rlist = new ArrayList<>(results);
				rlist.sort(PaliPlatform.paliCollator);
				pword = Utilities.lookUpCPEDFromDB(rlist.get(0));
			} else {
				term = term.substring(0, term.length()-1);
			}
		}
		if(!pword.getMeaning().isEmpty()) {
			final String tail = word.equals(term) ? "" : "*";
			infoPopup.setTitle(pword.getTerm()+tail);
			infoPopup.setBody(Utilities.formatCPEDMeaning(pword, false));
			infoPopup.showPopup(webView, InfoPopup.Pos.BELOW_RIGHT, false);
		}
	}

	private void calculateMeters() {
		final String text = clickedText.get();
		if(!text.isEmpty()) {
			final Object[] args = { displayScript.get().toString(), Utilities.addComputedMeters(text) };
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
		}
	}

	private void openAnalyzer() {
		final String text = clickedText.get();
		if(!text.isEmpty()) {
			final Object[] args = { text };
			PaliPlatform.openWindow(PaliPlatform.WindowType.PROSODY, args);
		}
	}

	private void openReader() {
		final String text = clickedText.get();
		if(!text.isEmpty()) {
			final Object[] args = { text };
			PaliPlatform.openWindow(PaliPlatform.WindowType.READER, args);
		}
	}

	public void updateClickedObject(final String text) {
		final String result;
		if(showNotes)
			result = text.trim();
		else
			result = text.trim().replaceAll("\\[.*?\\]", "");
		clickedText.set(result);
	}

	private void copySelection() {
		webEngine.executeScript("copySelection()");
	}
	
	private void saveSelection() {
		webEngine.executeScript("saveSelection()");
	}
	
	private void setShowNotes() {
		webEngine.executeScript("showNotes("+(showNotes?1:0)+")");
	}
	
	private void setShowXref() {
		final String command = "showXRef("+(showXref?1:0)+");setXrefColor('"+viewerTheme+"',"+(isBW?1:0)+");";
		webEngine.executeScript(command);		
	}
	
	private void convertToScript(final Utilities.PaliScript toScript, final Utilities.PaliScript fromScript, final boolean alsoNumber) {
		String command = "";
		final String withNum = alsoNumber ? "1" : "0";
		if(toScript == Utilities.PaliScript.ROMAN) {
			command = "toRoman()";
		} else {
			final boolean useThaiAlt = Boolean.parseBoolean(PaliPlatform.settings.getProperty("thai-alt-chars"));
			command = "toNonRoman('" + toScript.toString() + "'," + withNum + "," + (useThaiAlt?1:0) + ")";
		}
		if(fromScript == Utilities.PaliScript.ROMAN)
			webEngine.executeScript("saveRomanBody()");
		webEngine.executeScript(command);
		setViewerFont(toScript);
	}
	
	public void showMessage(final String text) {
		findReplaceBox.showMessage(text);
	}
	
	public void setSearchTextFound(final boolean yn) {
		searchTextFound.set(yn);
	}
	
	private void recordQuery(final PaliTextInput ptInput) {
		if(searchTextFound.get())
			ptInput.recordQuery();
	}
	
	private void clearFindInput() {
		recordQuery(findReplaceBox.getFindTextInput());
		findInput.clear();
		findInputCombo.setValue(null);
		searchTextFound.set(false);
	}	
	
	private void openFind() {
		findReplaceBox.showReplace(false);
		textPane.setBottom(findReplaceBox);
		findReplaceBox.getFindTextField().requestFocus();		
	}
	
	public void startSearch() {
		if(findReplaceBox.wholeWordProperty().get() || findReplaceBox.regexSearchProperty().get()) {
			doRegExFind();
		} else {
			final String strInput = findInput.getText();
			if(!strInput.isEmpty()) {
				final String strQuery = Normalizer.normalize(strInput, Form.NFC);
				startImmediateSearch(strQuery);
			}
		}
	}
	
	private void startImmediateSearch(final String query) {
		final int caseSensitive = findReplaceBox.caseSensitiveProperty().get() ? 1 : 0;
		try {
			webEngine.executeScript("startSearch('"+query+"',"+caseSensitive+")");
		} catch(netscape.javascript.JSException e) {
			showMessage("Invalid input pattern");
		}

		findInputCombo.commitValue();
	}	
	
	private void findNext(final int direction) {
		final int regexSearch = findReplaceBox.regexSearchProperty().get() ? 1 : 0;
		final int wholeWord = findReplaceBox.wholeWordProperty().get() ? 1 : 0;
		final int regexMode = regexSearch | wholeWord;
		webEngine.executeScript("findNext("+direction+","+regexMode+","+wholeWord+")");
	}
	
	private void doRegExFind() {
		final String strInput = findInput.getText();
		if(!strInput.isEmpty()) {
			final String strQuery = Normalizer.normalize(strInput, Form.NFC);
			boolean error = false;
			try {
				Pattern.compile(strQuery);
			} catch(PatternSyntaxException e) {
				error = true;
			}
			if(error) {
				showMessage("Invalid input pattern");
			} else {
				final int caseSensitive = findReplaceBox.caseSensitiveProperty().get() ? 1 : 0;
				final int wholeWord = findReplaceBox.wholeWordProperty().get() ? 1 : 0;
				try {
					webEngine.executeScript("startRegExSearch('"+strQuery+"',"+wholeWord+","+caseSensitive+")");
				} catch(netscape.javascript.JSException e) {
					showMessage("Invalid input pattern");
				}
			}
		}		
	}
}
