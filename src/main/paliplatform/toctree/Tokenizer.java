/*
 * Tokenizer.java
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

import java.util.*;
import java.util.zip.*;
import java.util.stream.*;
import java.util.function.*;
import java.util.regex.*;
import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import static java.lang.Math.abs;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javafx.event.*;
import javafx.geometry.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.scene.text.*;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

/** 
 * This class manipulates terms in Pali documents.
 * It also makes indices and incorporates search function.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class Tokenizer extends BorderPane {
	private static final String MESSAGE_NO_DOC = "Add some documents and process";
	private static final int SEARCH_RESULT_MAX_DOC = 20; // maximum search result including zero-scored docs
	private static final int DEF_ADJ_WORDS = 5; // default number of adjacent words (both sides) in search result
	private final StackPane mainPane = new StackPane();
	private final ListView<TOCTreeNode> docListView = new ListView<>();
	private final ObservableList<TOCTreeNode> docNodeList = FXCollections.<TOCTreeNode>observableArrayList();
	private final Set<TOCTreeNode> unsortedDocSet = new LinkedHashSet<>();
	private final CSCDFieldSelectorBox fieldOptionsBox;
	private final AnchorPane statusPane = new AnchorPane();
	private final Label generalMessage = new Label(MESSAGE_NO_DOC);
	private final Label fixedInfoLabel = new Label();
	private final HBox progressBox = new HBox(3);
	private final ProgressBar progressBar = new ProgressBar();
	private final Label progressMessage = new Label();
	private final CheckBox cbDocListSorted = new CheckBox("Sorted");
	private final SimpleBooleanProperty isProcessing = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty isComputing = new SimpleBooleanProperty(false);
	private final InfoPopup mainHelpPopup = new InfoPopup();
	private final InfoPopup filterHelpPopup = new InfoPopup();
	private final TableView<TermFreqProp> table = new TableView<>();
	private final Map<String, TermFreq> processedResultMap = new HashMap<>();
	private final Map<String, TermWeight> weightedResultMap = new HashMap<>();
	private final ObservableList<TermFreqProp> shownResultList = FXCollections.<TermFreqProp>observableArrayList();
	private final Map<TOCTreeNode, Map<String, Map<CSCDTermInfo.Field, Integer>>> docTermFreqMap = new HashMap<>();
	private final Map<String, TermFreqProp> mergedResultMap = new HashMap<>();
	private final ChoiceBox<Integer> maxRowChoice = new ChoiceBox<>();
	private final CheckMenuItem combineCapMenuItem = new CheckMenuItem("Combine capitalized terms");
	private final CheckMenuItem onlyCapMenuItem = new CheckMenuItem("Only capitalized terms");
	private final Map<Toggle, FilterMode> filterModeMap = new HashMap<>();
	private final ToggleGroup termFilterGroup = new ToggleGroup();
	private final RadioMenuItem filterSimpleMenuItem = new RadioMenuItem("Simple filter");
	private final SplitPane splitPane = new SplitPane();
	private final BorderPane searchPane = new BorderPane();
	private final VBox searchResultBox = new VBox();
	private final PaliTextInput searchTextInput = new PaliTextInput(PaliTextInput.InputType.COMBO);
	private final TextField filterTextField;
	private final TextField searchTextField;
	private final ComboBox<String> searchComboBox;
	private final Spinner<Integer> searchResultWinSizeSpinner = new Spinner<>(1, 20, DEF_ADJ_WORDS); // number of adjacent words to display in both sides
	private final CheckMenuItem autoCapMenuItem = new CheckMenuItem("Auto-include capitalized query");
	private final ContextMenu searchResultPopupMenu = new ContextMenu();
	private final SimpleBooleanProperty isPlainText = new SimpleBooleanProperty(false);
	private Task<Boolean> processTask = null;
	private int maxRowCount = 500;
	private long currIncludedDocs = 0;
	private long currTotalTerms = 0;
	private long currTotalCapTerms = 0;
	private TOCTreeNode currSelectedDoc = null;
	private FilterMode currFilterMode = FilterMode.SIMPLE;
	private PaliTextInput.InputMethod savInputMethod = PaliTextInput.InputMethod.UNUSED_CHARS;
	public static enum ProcessStatus { UNPROCESSED, INCLUDED, EXCLUDED }
	public static enum FilterMode { SIMPLE, WILDCARDS, REGEX, METER }
	
	public Tokenizer() {
		// prepare document list on the left
		docListView.setPrefWidth(Utilities.getRelativeSize(12));
		docListView.setEditable(false);
		docListView.setCellFactory((ListView<TOCTreeNode> lv) -> {
			return new ListCell<TOCTreeNode>() {
				@Override
				public void updateItem(TOCTreeNode item, boolean empty) {
					super.updateItem(item, empty);
					if (empty) {
						this.setText(null);
						this.setGraphic(null);
						this.setTooltip(null);
					} else {
						final TOCTreeNode ttn = this.getItem();
						this.setTooltip(new Tooltip(ttn.getTextName()));
						final String filename = ttn.getFileName();
						if(ttn.isExtra())
							this.setText("x:" + filename);
						else
							this.setText(filename);
						final ProcessStatus status = ttn.getProcessStatus();
						final String icon;
						switch(status) {
							case INCLUDED:
								icon = "✔"; break;
							case EXCLUDED:
								icon = " "; break;
							default: // UNPROCESSED
								icon = "?";
						}
						this.setGraphic(new TextIcon(icon, TextIcon.IconSet.MONO));
					}
				}
			};
		});
		docListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		docListView.setItems(docNodeList);
		// add context menu to the list
		final ContextMenu docListPopupMenu = new ContextMenu();
		final MenuItem includeMenuItem = new MenuItem("Include");
		includeMenuItem.disableProperty().bind(isProcessing);
		includeMenuItem.setOnAction(actionEvent -> includeDoc(true));
		final MenuItem incOnlyMenuItem = new MenuItem("Include only");
		incOnlyMenuItem.disableProperty().bind(isProcessing);
		incOnlyMenuItem.setOnAction(actionEvent -> includeOnlyDoc());
		final MenuItem incAllMenuItem = new MenuItem("Include all");
		incAllMenuItem.disableProperty().bind(isProcessing);
		incAllMenuItem.setOnAction(actionEvent -> includeAllDoc());
		final MenuItem excludeMenuItem = new MenuItem("Exclude");
		excludeMenuItem.disableProperty().bind(isProcessing);
		excludeMenuItem.setOnAction(actionEvent -> includeDoc(false));
		final MenuItem removeMenuItem = new MenuItem("Remove");
		removeMenuItem.disableProperty().bind(isProcessing);
		removeMenuItem.setOnAction(actionEvent -> removeDoc());
		final MenuItem selectAllMenuItem = new MenuItem("Select all");
		selectAllMenuItem.disableProperty().bind(isProcessing);
		selectAllMenuItem.setOnAction(actionEvent -> selectDoc(true));
		final MenuItem invertSelMenuItem = new MenuItem("Invert");
		invertSelMenuItem.disableProperty().bind(isProcessing);
		invertSelMenuItem.setOnAction(actionEvent -> selectDoc(false));
		docListPopupMenu.getItems().addAll(includeMenuItem, incOnlyMenuItem, incAllMenuItem, excludeMenuItem, removeMenuItem, 
										new SeparatorMenuItem(), selectAllMenuItem, invertSelMenuItem);
		docListView.setContextMenu(docListPopupMenu);
		// add toolbar for the list
		final VBox leftBox = new VBox();
		VBox.setVgrow(docListView, Priority.ALWAYS);
		final ToolBar leftToolBar = new ToolBar();
		final Button clearDocButton = new Button("", new TextIcon("trash", TextIcon.IconSet.AWESOME));
		clearDocButton.setTooltip(new Tooltip("Clear document list"));
		clearDocButton.setOnAction(actionEvent -> clearDocList());
		cbDocListSorted.setAllowIndeterminate(false);
		cbDocListSorted.setOnAction(actionEvent -> sortThenUpdateDocList());
		leftToolBar.getItems().addAll(clearDocButton, cbDocListSorted);
		leftBox.getChildren().addAll(leftToolBar, docListView);
		setLeft(leftBox);

		// add toolbar on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(mainPane);
		// config some buttons
		toolBar.saveTextButton.setOnAction(actionEvent -> saveCSV());		
		toolBar.copyButton.setOnAction(actionEvent -> copyCSV());		
		// add new components
		final SimpleListProperty<TOCTreeNode> docListProp = new SimpleListProperty<>(docNodeList);
		final Button processButton = new Button("Process", new TextIcon("person-running", TextIcon.IconSet.AWESOME));
		processButton.setTooltip(new Tooltip("Process documents"));
		processButton.disableProperty().bind(isProcessing.or(isComputing).or(docListProp.sizeProperty().isEqualTo(0)));
		processButton.setOnAction(actionEvent -> processDocument());
		final Button stopButton = new Button("Stop", new TextIcon("hand", TextIcon.IconSet.AWESOME));
		stopButton.setTooltip(new Tooltip("Stop processing"));
		stopButton.disableProperty().bind(isProcessing.not());
		stopButton.setOnAction(actionEvent -> stopProcess());
		final List<Integer> maxList = Arrays.asList(20, 50, 100, 300, 500, 1000, 2000, 3000, 5000, 10000);
		final int maxInd = maxList.indexOf(maxRowCount);
		maxRowChoice.setTooltip(new Tooltip("Maximum rows"));
		maxRowChoice.getItems().addAll(maxList);
		maxRowChoice.getSelectionModel().select(maxInd);
		maxRowChoice.setOnAction(actionEvent -> {
			maxRowCount = maxRowChoice.getSelectionModel().getSelectedItem();
			formatResult(maxRowCount);
		});
		final Button fieldSelButton = new Button("", new TextIcon("list-check", TextIcon.IconSet.AWESOME));
		fieldSelButton.setTooltip(new Tooltip("Field selector on/off"));
		fieldSelButton.setOnAction(actionEvent -> openFieldSelector());
		final MenuButton mainOptionMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		mainOptionMenu.setTooltip(new Tooltip("Options"));
		combineCapMenuItem.setSelected(true);
		final EventHandler<ActionEvent> updateTable = actionEvent -> {
			if(!processedResultMap.isEmpty()) {
				formatResult();
				if(!shownResultList.isEmpty())
					setupTable();
			}
		}; 
		combineCapMenuItem.setOnAction(updateTable);
		onlyCapMenuItem.setOnAction(updateTable);
		mainOptionMenu.getItems().addAll(combineCapMenuItem, onlyCapMenuItem);
		final Button openSearchButton = new Button("", new TextIcon("magnifying-glass", TextIcon.IconSet.AWESOME));
		openSearchButton.setTooltip(new Tooltip("Search pane on/off"));
		openSearchButton.setOnAction(actionEvent -> toggleSearchPane());
		final Button mainHelpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		mainHelpButton.setOnAction(actionEvent -> mainHelpPopup.showPopup(mainHelpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		toolBar.getItems().addAll(new Separator(), processButton, stopButton,
								new Separator(), maxRowChoice, fieldSelButton, mainOptionMenu,
								new Separator(), openSearchButton, mainHelpButton);
		setTop(toolBar);

		// prepare field options on the right
		fieldOptionsBox = new CSCDFieldSelectorBox(() -> updateResult());

		// add main content at the center
		// there are 2 parts, term list and search
		// split pane is used here
		splitPane.setOrientation(Orientation.VERTICAL);
		// 1. set term list module at the top
		final BorderPane filterPane = new BorderPane();
		final ToolBar tableToolBar = new ToolBar();
		final PaliTextInput filterTextInput = new PaliTextInput(PaliTextInput.InputType.FIELD);
		filterTextField = (TextField)filterTextInput.getInput();
		filterTextField.setPrefWidth(Utilities.getRelativeSize(22));
		filterTextField.setPromptText("Filter...");
		filterTextField.textProperty().addListener((obs, oldValue, newValue) -> {
			final String text = Normalizer.normalize(newValue.trim(), Form.NFC);
			formatResult(text);
		});
		final MenuButton filterOptionMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		filterOptionMenu.setTooltip(new Tooltip("Options"));
		final RadioMenuItem filterWildcardsMenuItem = new RadioMenuItem("Using ? and *");
		final RadioMenuItem filterRegexMenuItem = new RadioMenuItem("Regular expression");
		final RadioMenuItem filterMeterMenuItem = new RadioMenuItem("Filter by meter");
		filterModeMap.put(filterSimpleMenuItem, FilterMode.SIMPLE);
		filterModeMap.put(filterWildcardsMenuItem, FilterMode.WILDCARDS);
		filterModeMap.put(filterRegexMenuItem, FilterMode.REGEX);
		filterModeMap.put(filterMeterMenuItem, FilterMode.METER);
		termFilterGroup.getToggles().addAll(filterSimpleMenuItem, filterWildcardsMenuItem, filterRegexMenuItem, filterMeterMenuItem);
		termFilterGroup.selectToggle(filterSimpleMenuItem);
        termFilterGroup.selectedToggleProperty().addListener((observable) -> {
			if(termFilterGroup.getSelectedToggle() != null) {
				final Toggle selected = (Toggle)termFilterGroup.getSelectedToggle();
				currFilterMode = filterModeMap.get(selected);
				if(currFilterMode == FilterMode.METER) {
					savInputMethod = filterTextInput.getInputMethod();
					filterTextInput.setInputMethod(PaliTextInput.InputMethod.METER_GROUP);
				} else {
					filterTextInput.setInputMethod(savInputMethod);
				}
				formatResult();
			}
        });
		filterOptionMenu.getItems().addAll(filterSimpleMenuItem, filterWildcardsMenuItem, filterRegexMenuItem, filterMeterMenuItem);
		final Button filterHelpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		filterHelpButton.setOnAction(actionEvent -> filterHelpPopup.showPopup(filterHelpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		tableToolBar.getItems().addAll(filterTextField, filterTextInput.getClearButton(), filterTextInput.getMethodButton(), filterOptionMenu, filterHelpButton);
		filterPane.setTop(tableToolBar);
		table.setItems(shownResultList);
		final ContextMenu tablePopupMenu = new ContextMenu();
		final MenuItem addToSearchMenuItem = new MenuItem("Add to search");
		addToSearchMenuItem.setOnAction(actionEvent -> addTermToSearch());		
		final MenuItem sendToDictMenuItem = new MenuItem("Send to Dictionaries");
		sendToDictMenuItem.setOnAction(actionEvent -> sendTermToDict());
		tablePopupMenu.getItems().addAll(addToSearchMenuItem, sendToDictMenuItem);
		table.setContextMenu(tablePopupMenu);
		table.setOnDragDetected(mouseEvent -> {
			final TermFreqProp selected = (TermFreqProp)table.getSelectionModel().getSelectedItem();
			if(selected != null && table.getSelectionModel().getSelectedIndex() >= 0) {
				final Dragboard db = table.startDragAndDrop(TransferMode.ANY);
				final ClipboardContent content = new ClipboardContent();
				final String term = selected.termProperty().get();
				content.putString(term);
				db.setContent(content);
				mouseEvent.consume();
			}
		});
		table.setOnMouseDragged(mouseEvent -> mouseEvent.setDragDetect(true));
		filterPane.setCenter(table);
		// 2. set search module at the bottom
		final ToolBar searchToolBar = new ToolBar();
		searchTextField = (TextField)searchTextInput.getInput();
		searchComboBox = searchTextInput.getComboBox();
		searchComboBox.setPromptText("Search for...");
		searchComboBox.setPrefWidth(Utilities.getRelativeSize(24));
		searchComboBox.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				final KeyCode key = keyEvent.getCode();
				if(key == KeyCode.ENTER) {
					if(!searchTextField.getText().trim().isEmpty())
						search();
				} else if(key == KeyCode.ESCAPE) {
					searchComboBox.getEditor().clear();
				}
			}
		});
		final Button searchClearButton = searchTextInput.getClearButton();
		searchClearButton.setOnAction(actionEvent -> {
			searchTextField.clear();
			searchResultBox.getChildren().clear();
		});
		final Button searchButton = new Button("Search");
		searchButton.disableProperty().bind(searchTextField.lengthProperty().isEqualTo(0).or(isComputing));
		searchButton.setOnAction(actionEvent -> search());
		searchResultWinSizeSpinner.setEditable(true);
		searchResultWinSizeSpinner.setPrefWidth(Utilities.getRelativeSize(5));
		searchResultWinSizeSpinner.setTooltip(new Tooltip("Number of adjacent words in search result"));
		searchResultWinSizeSpinner.getEditor().setTextFormatter(new TextFormatter<Integer>(PaliPlatform.integerStringConverter, DEF_ADJ_WORDS));
		searchResultWinSizeSpinner.valueProperty().addListener((ob, oldv, newv) -> search());
		final MenuButton searchOptionMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		searchOptionMenu.setTooltip(new Tooltip("Options"));
		autoCapMenuItem.setSelected(true);
		autoCapMenuItem.setOnAction(actionEvent -> {
			if(!searchTextField.getText().isEmpty())
				search();
		});
		searchOptionMenu.getItems().addAll(autoCapMenuItem);
		final Button foldUpAllButton = new Button("", new TextIcon("angles-up", TextIcon.IconSet.AWESOME));
		foldUpAllButton.setTooltip(new Tooltip("Collapse all"));
		foldUpAllButton.setOnAction(actionEvent -> foldSearchResult(false));
		final Button foldDownAllButton = new Button("", new TextIcon("angles-down", TextIcon.IconSet.AWESOME));
		foldDownAllButton.setTooltip(new Tooltip("Expand all"));
		foldDownAllButton.setOnAction(actionEvent -> foldSearchResult(true));
		searchToolBar.getItems().addAll(searchComboBox, searchClearButton, searchTextInput.getMethodButton(),
										searchButton, searchResultWinSizeSpinner, searchOptionMenu, foldUpAllButton, foldDownAllButton);
		searchPane.setTop(searchToolBar);
		searchResultBox.prefWidthProperty().bind(mainPane.widthProperty().subtract(5));
		final ScrollPane searchResultPane = new ScrollPane(searchResultBox);
		searchPane.setCenter(searchResultPane);
		searchPane.setOnDragOver(dragEvent -> {
			if(dragEvent.getGestureSource() != searchPane && dragEvent.getDragboard().hasString()) {
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			dragEvent.consume();
		});
		searchPane.setOnDragDropped(dragEvent -> {
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
		// 3. add the contents
		splitPane.getItems().add(filterPane);
		mainPane.getChildren().add(splitPane);
		setCenter(mainPane);

		// add status pane at the bottom
		// add fixed information on the far right
		final HBox fixedInfoBox = new HBox();
		AnchorPane.setBottomAnchor(fixedInfoBox, 0.0);
		AnchorPane.setRightAnchor(fixedInfoBox, 0.0);
		fixedInfoLabel.setStyle("-fx-font-family:'" + Utilities.FONTMONO +"';-fx-font-size:85%;");
		fixedInfoBox.getChildren().add(fixedInfoLabel);
		updateFixedInfo();
		AnchorPane.setBottomAnchor(generalMessage, 0.0);
		AnchorPane.setLeftAnchor(generalMessage, 0.0);
		statusPane.getChildren().addAll(generalMessage, fixedInfoBox);
		setBottom(statusPane);

		// set up drop events
		this.setOnDragOver(dragEvent -> {
			if(dragEvent.getGestureSource() != this && dragEvent.getDragboard().hasString()) {
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			dragEvent.consume();
		});
		this.setOnDragDropped(dragEvent -> {
			final Dragboard db = dragEvent.getDragboard();
			if(db.hasString()) {
				final String[] allNodes = db.getString().split("\\n");
				final String head = allNodes[0];
				if(head.startsWith("::paliplatform.toctree")) {
					for(int i = 1; i < allNodes.length; i++) {
						final String item = allNodes[i];
						final String[] nodeStr = item.split(":");
						final boolean ar = nodeStr[0].charAt(0) != '0';
						final boolean ex = nodeStr[1].charAt(0) != '0';
						final boolean tx = nodeStr[2].charAt(0) != '0';
						final String tname = nodeStr[3];
						final String fname = nodeStr.length < 5 ? "" : nodeStr[4];
						final TOCTreeNode ttnode = new TOCTreeNode(tname, fname, ar, ex, tx);
						final TreeItem<TOCTreeNode> titem = new TreeItem<>(ttnode);
						addDoc(titem);
					}
				}
				dragEvent.setDropCompleted(true);
			} else {
				dragEvent.setDropCompleted(false);
			}
			dragEvent.consume();
		});
		// some other initialization
		setPrefWidth(Utilities.getRelativeSize(68));
		Utilities.createMeterPatternMap();

		AnchorPane.setBottomAnchor(progressBox, 0.0);
		AnchorPane.setLeftAnchor(progressBox, 0.0);
		progressBox.getChildren().addAll(progressBar, progressMessage);

		final MenuItem openDocMenuItem = new MenuItem("Open");
		openDocMenuItem.setOnAction(actionEvent -> openCurrentDoc());
		final MenuItem openAsTextMenuItem = new MenuItem("Open as text");
		openAsTextMenuItem.disableProperty().bind(isPlainText);
		openAsTextMenuItem.setOnAction(actionEvent -> openAsText(true));		
		final MenuItem openAsTextNoNotesMenuItem = new MenuItem("Open as text (no notes)");
		openAsTextNoNotesMenuItem.disableProperty().bind(isPlainText);
		openAsTextNoNotesMenuItem.setOnAction(actionEvent -> openAsText(false));		
		searchResultPopupMenu.getItems().addAll(openDocMenuItem, openAsTextMenuItem, openAsTextNoNotesMenuItem);

		mainHelpPopup.setContent("info-tokenizer.txt");
		mainHelpPopup.setTextWidth(Utilities.getRelativeSize(34.5));
		filterHelpPopup.setContent("info-tokenizer-filter.txt");
		filterHelpPopup.setTextWidth(Utilities.getRelativeSize(34.5));
	}

	public void init() {
		maxRowChoice.getSelectionModel().select(4);
		fieldOptionsBox.init();
		combineCapMenuItem.setSelected(true);
		onlyCapMenuItem.setSelected(false);
		termFilterGroup.selectToggle(filterSimpleMenuItem);
		searchTextField.clear();
		autoCapMenuItem.setSelected(true);
		searchResultWinSizeSpinner.getValueFactory().setValue(5);
		if(splitPane.getItems().size() > 1)
			splitPane.getItems().remove(searchPane);
		clearDocList();
		setRight(null);
	}

	private void updateFixedInfo() {
		final int totalDocs = docNodeList.size();
		final int shownTerms = table.getItems().size();
		final String info = String.format("Docs: %,4d of %,4d | Caps: %,8d | Terms: %,9d of %,9d",
							currIncludedDocs, totalDocs, currTotalCapTerms, shownTerms, currTotalTerms);
		fixedInfoLabel.setText(info);
	}

	private void addDocList(final List<TOCTreeNode> ttList) {
		ttList.forEach(x -> {
			if(!unsortedDocSet.contains(x))
				unsortedDocSet.add(x);
		});
		sortThenUpdateDocList();
		updateFixedInfo();
	}

	private void sortThenUpdateDocList() {
		docNodeList.clear();
		if(cbDocListSorted.isSelected()) {
			final List<TOCTreeNode> cscdList = unsortedDocSet.stream()
												.filter(x -> x.isInArchive())
												.sorted((x, y) -> PaliDocument.compareFileName(x.getFileName(), y.getFileName()))
												.collect(Collectors.toList());
			final List<TOCTreeNode> extraList = unsortedDocSet.stream()
												.filter(x -> x.isExtra())
												.sorted((x, y) -> x.getTextName().compareTo(y.getTextName()))
												.collect(Collectors.toList());
			docNodeList.addAll(cscdList);
			docNodeList.addAll(extraList);
		} else {
			docNodeList.addAll(unsortedDocSet);
		}
	}

	private void clearDocList() {
		unsortedDocSet.clear();
		docNodeList.clear();
		unsortedDocSet.clear();
		shownResultList.clear();
		docTermFreqMap.clear();
		searchResultBox.getChildren().clear();
		prepareDocsAndTerms();
	}

	public void addDoc(final TreeItem<TOCTreeNode> titem) {
		addDocList(listTreeNodes(titem));
		final long unprocessedDocs = docNodeList.stream().filter(x -> x.getProcessStatus() == ProcessStatus.UNPROCESSED).count();
		generalMessage.setText(unprocessedDocs + " document(s) unprocessed");
	}

	private List<TOCTreeNode> listTreeNodes(final TreeItem<TOCTreeNode> titem) {
		final List<TOCTreeNode> result = new ArrayList<>();
		final TOCTreeNode tnode = titem.getValue().clone();
		if(tnode.isText()) {
			result.add(tnode);
		} else {
			if(tnode.getFileName().isEmpty()) {
				// empty head node, read its children
				for(final TreeItem<TOCTreeNode> subitem : titem.getChildren()) {
					if(tnode.isExtra())
						result.add(subitem.getValue().clone());
					else
						result.addAll(listTreeNodes(subitem));
				}
			} else {
				// toc node, create new node first and read its children
				final TreeItem<TOCTreeNode> newItem = readTreeNode(tnode);
				for(final TreeItem<TOCTreeNode> subitem : newItem.getChildren())
					result.addAll(listTreeNodes(subitem));
			}
		}
		return result;
	}

	private TreeItem<TOCTreeNode> readTreeNode(final TOCTreeNode node) {
		final TreeItem<TOCTreeNode> newItem = new TreeItem<>(node);
		CSCDTreeItemFactory.readXMLAsTreeNode(newItem);
		return newItem;
	}

	private void includeDoc(final boolean isInclude) {
		final List<TOCTreeNode> selected = new ArrayList<>(docListView.getSelectionModel().getSelectedItems());
		if(!selected.isEmpty() && docListView.getSelectionModel().getSelectedIndex() >= 0) {
			for(final TOCTreeNode ttn : selected) {
				if(ttn.getProcessStatus() != ProcessStatus.UNPROCESSED)
					ttn.setProcessStatus(isInclude ? ProcessStatus.INCLUDED : ProcessStatus.EXCLUDED);
			}
		}
		docListView.refresh();
		prepareDocsAndTerms();
	}

	private void includeOnlyDoc() {
		final List<TOCTreeNode> selected = new ArrayList<>(docListView.getSelectionModel().getSelectedItems());
		if(!selected.isEmpty() && docListView.getSelectionModel().getSelectedIndex() >= 0) {
			docNodeList.forEach(x -> {
				if(x.getProcessStatus() != ProcessStatus.UNPROCESSED)
					x.setProcessStatus(ProcessStatus.EXCLUDED);
			});
			selected.forEach(x -> {
				if(x.getProcessStatus() != ProcessStatus.UNPROCESSED)
					x.setProcessStatus(ProcessStatus.INCLUDED);
			});
		}
		docListView.refresh();
		prepareDocsAndTerms();
	}

	private void includeAllDoc() {
		docNodeList.forEach(x -> {
			if(x.getProcessStatus() != ProcessStatus.UNPROCESSED)
				x.setProcessStatus(ProcessStatus.INCLUDED);
		});
		docListView.refresh();
		prepareDocsAndTerms();
	}

	private void removeDoc() {
		final List<TOCTreeNode> selected = new ArrayList<>(docListView.getSelectionModel().getSelectedItems());
		if(!selected.isEmpty() && docListView.getSelectionModel().getSelectedIndex() >= 0) {
			docNodeList.removeAll(selected);
			unsortedDocSet.removeAll(selected);
			selected.forEach(x -> docTermFreqMap.remove(x));
		}
		// does the rest have included status?
		final long incRest = docNodeList.stream().filter(x -> x.getProcessStatus() == ProcessStatus.INCLUDED).count();
		// if not, clear the result
		if(incRest == 0) {
			shownResultList.clear();
			searchResultBox.getChildren().clear();
		}
		prepareDocsAndTerms();
	}

	private void selectDoc(final boolean isAll) {
		final MultipleSelectionModel<TOCTreeNode> selModel = docListView.getSelectionModel();
		final List<Integer> selected = new ArrayList<>(selModel.getSelectedIndices());
		selModel.selectAll();
		if(!isAll) {
			// invert selection
			for(final Integer i : selected)
				selModel.clearSelection(i);
		}
	}

	private void processDocument() {
		progressBar.setProgress(0);
		processTask = processDocs();
		progressBar.progressProperty().bind(processTask.progressProperty());
		processTask.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			progressMessage.setText(newValue + " processed");
		});
		PaliPlatform.threadPool.submit(processTask);
		statusPane.getChildren().add(progressBox);
		generalMessage.setText("");
		isProcessing.set(true);
	}

	private void stopProcess() {
		processTask.cancel(true);
		isProcessing.set(false);
		progressBar.progressProperty().unbind();
		statusPane.getChildren().remove(progressBox);
		isComputing.set(true);
		prepareDocsAndTerms();
	}

    private Task<Boolean> processDocs() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
				final ObservableList<TOCTreeNode> ttnList = docListView.getItems();
				final int total = ttnList.size();
				for(int i = 0; i < total; i++) {
					if(isCancelled())
						break;
					final TOCTreeNode ttn = ttnList.get(i);
					if(ttn.getProcessStatus() == ProcessStatus.UNPROCESSED) {
						ttn.setProcessStatus(ProcessStatus.INCLUDED);
						ttn.setTermsMap(readTermsMap(ttn));
						final Map<String, Map<CSCDTermInfo.Field, Integer>> termFreqMap = new HashMap<>();
						final Map<String, CSCDTermInfo> termsMap = ttn.getTermsMap();
						termsMap.forEach((term, terminfo) -> {
							final Map<CSCDTermInfo.Field, List<int[]>> postingMap = terminfo.getPostingMap();
							final List<CSCDTermInfo.Field> fields = new ArrayList<>(postingMap.keySet());
							final Map<CSCDTermInfo.Field, Integer> freqMap = new EnumMap<>(CSCDTermInfo.Field.class);
							for(final CSCDTermInfo.Field f : fields) {
								final int freq = postingMap.get(f).size();
								if(freq > 0)
									freqMap.put(f, freq);
							}
							if(!freqMap.isEmpty()) {
								termFreqMap.put(term, freqMap);
							}
						});
						docTermFreqMap.put(ttn, termFreqMap);
						docListView.refresh();
					}
					updateMessage((i+1) + "/" + total);
					updateProgress(i + 1, total);
                }
				Platform.runLater(() -> {
					isProcessing.set(false);
					progressBar.progressProperty().unbind();
					statusPane.getChildren().remove(progressBox);
					isComputing.set(true);
					prepareDocsAndTerms();
				});
                return true;
            }
        };
    }

	private Map<String, CSCDTermInfo> readTermsMap(final TOCTreeNode ttn) {
		final Map<String, CSCDTermInfo> result = new HashMap<>();
		final Map<CSCDTermInfo.Field, StringBuilder> textMap = new EnumMap<>(CSCDTermInfo.Field.class);
		for(final CSCDTermInfo.Field fld : CSCDTermInfo.Field.values()) {
			textMap.put(fld, new StringBuilder());
			if(ttn.isPlainText() && fld == CSCDTermInfo.Field.BODYTEXT)
				break; // plain text needs only field 'bodytext'
		}
		if(ttn.isPlainText()) {
			try{
				final String text = Files.readString(Path.of(Utilities.EXTRAPATH + ttn.getFileName()));
				textMap.get(CSCDTermInfo.Field.BODYTEXT).append(text);
			} catch(IOException e) {
				System.err.println(e);
			}
		} else {
			try {
				final SAXParserFactory spf = SAXParserFactory.newInstance();
				final SAXParser saxParser = spf.newSAXParser();
				final DefaultHandler handler = new CSCDTermInfoSAXHandler(textMap);
				if(ttn.isInArchive()) {
					final ZipFile zip = new ZipFile(new File(Utilities.ROOTDIR + Utilities.COLLPATH + Utilities.CSCD_ZIP));
					final ZipEntry entry = zip.getEntry(Utilities.CSCD_DIR + ttn.getFileName());
					if(entry != null) {
						saxParser.parse(zip.getInputStream(entry), handler);
					} else {
						zip.close();
						return result;
					}
					zip.close();
				} else {
					saxParser.parse(new File(Utilities.EXTRAPATH + ttn.getFileName()), handler);
				}
			} catch(SAXException | ParserConfigurationException | IOException e) {
				System.err.println(e);
			}
		}
		// for debug
		/*
		final StringBuilder debugText = new StringBuilder();
		for(final CSCDTermInfo.Field f : CSCDTermInfo.Field.values()) {
			if(f != CSCDTermInfo.Field.BOLD)
				debugText.append(textMap.get(f).toString()).append("\n");
		}
		final File debugOut = new File("debugout.txt");
		Utilities.saveText(debugText.toString(), debugOut);
		*/
		// tokenize each word in each field
		for(final CSCDTermInfo.Field f : CSCDTermInfo.Field.values()) {
			if(!textMap.containsKey(f)) continue;
			final String[] lines = textMap.get(f).toString().lines()
									.filter(x -> !x.trim().isEmpty())
									.collect(Collectors.toList()).toArray(new String[0]);
			for(int n = 0; n < lines.length; n++) {
				final String[] tokens = lines[n].trim().split(Utilities.REX_NON_PALI);
				for(int i = 0; i < tokens.length; i++) {
					final String term = tokens[i];
					if(!term.isEmpty()) {
						final CSCDTermInfo terminfo;
						if(result.containsKey(term))
							terminfo = result.get(term);
						else
							terminfo = new CSCDTermInfo(term);
						final List<int[]> pstList;
						if(terminfo.getPostingMap().containsKey(f))
							pstList = terminfo.getPostingMap().get(f);
						else
							pstList = new ArrayList<>();
						pstList.add(new int[] { n, i });
						terminfo.addPosting(f, pstList);
						result.put(term, terminfo);
					} // end if
				}
			}
		}
		return result;
	}

	private void prepareDocsAndTerms() {
		progressBar.setProgress(0);
		final Task<Boolean> computeTask = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				updateProgress(0, -1);
				final List<TOCTreeNode> dlist = docTermFreqMap.keySet().stream()
															.filter(x -> x.getProcessStatus() == ProcessStatus.INCLUDED)
															.collect(Collectors.toList());
				final int totDocs = dlist.size();
				// create term list
				updateMessage("Creating term list");
				processedResultMap.clear();
				for(final TOCTreeNode ttn : dlist) {
					final Map<String, Map<CSCDTermInfo.Field, Integer>> tfmap = docTermFreqMap.get(ttn);
					tfmap.forEach((term, fmap) -> {
						fmap.forEach((field, freq) -> {
							final String key = term + ":" + field.ordinal();
							final TermFreq tf;
							if(processedResultMap.containsKey(key)) {
								tf = processedResultMap.get(key);
								if(tf.getField() == field)
									tf.addUpFreq(freq);
							} else {
								tf = new TermFreq(term, freq, field);
							}
							processedResultMap.put(key, tf);
						});
					});
				}
				// process capitalized terms
				updateMessage("Processing capitalized terms");
				final List<String> capKeys = processedResultMap.keySet().stream()
															.filter(x -> Character.isUpperCase(x.charAt(0)))
															.collect(Collectors.toList());
				for(final String cap : capKeys) {
					final TermFreq tfCap = processedResultMap.get(cap);
					final String lower = cap.toLowerCase();
					final TermFreq tfLower;
					if(processedResultMap.containsKey(lower)) {
						tfLower = processedResultMap.get(lower);
					} else {
						final String lowerTerm = lower.split(":")[0];
						tfLower = new TermFreq(lowerTerm, 0, tfCap.getField());
						processedResultMap.put(lower, tfLower);
					}
					tfLower.setCapFreq(tfCap.getFreq());
				}
				// compute doc weighting
				updateMessage("Calculating weights (please wait)");
				weightedResultMap.clear();
				final Set<String> tset = processedResultMap.keySet();
				tset.forEach(t -> {
					final String term = t.split(":")[0];
					if(!weightedResultMap.containsKey(term))
						weightedResultMap.put(term, new TermWeight(term));
				});
				final int total = weightedResultMap.size();
				int count = 0;
				for(final TermWeight tw : weightedResultMap.values()) {
					final String term = tw.getTerm();
					for(final TOCTreeNode ttn : dlist) {
						final Map<String, Map<CSCDTermInfo.Field, Integer>> tfMap = docTermFreqMap.get(ttn);
						if(tfMap.containsKey(term)) {
							final Map<CSCDTermInfo.Field, Integer> fmap = tfMap.get(term);
							fmap.forEach((fld, frq) -> {
								tw.addTF(ttn, fld, frq);
								tw.increaseDocCount(fld);
							});
						}
					}
					tw.computeWeight(dlist, totDocs);
					updateProgress(++count, total);
				}
				Platform.runLater(() -> {
					isComputing.set(false);
					progressBar.progressProperty().unbind();
					statusPane.getChildren().remove(progressBox);
					updateResult();
					if(docNodeList.isEmpty())
						generalMessage.setText(MESSAGE_NO_DOC);
					else if(dlist.isEmpty())
						generalMessage.setText("Please process or include some document(s)");
				});
				return true;
			}
		};
		progressBar.progressProperty().bind(computeTask.progressProperty());
		computeTask.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			progressMessage.setText(newValue);
		});
		PaliPlatform.threadPool.submit(computeTask);
		statusPane.getChildren().add(progressBox);
		generalMessage.setText("");
	}
	
	/*
	// for debugging
	private void saveTermList(final TOCTreeNode ttn) {
		final String linebreak = System.getProperty("line.separator");
		final StringBuilder result = new StringBuilder();
		ttn.getTermsMap().forEach((term, terminfo) -> {
			result.append("== " + term + " ==").append(linebreak);
			final Map<CSCDTermInfo.Field, List<int[]>> postingMap = terminfo.getPostingMap();
			postingMap.forEach((field, posting) -> {
				result.append(field + ": ");
				final String pstStr = posting.stream().map(x -> x[0]+"-"+x[1]).collect(Collectors.joining(", "));
				result.append(pstStr).append(linebreak);
			});
		});
		Utilities.saveText(result.toString(), "termlist.txt");
	}
	*/
	
	private void setupTable() {
		table.getColumns().clear();
		final TableColumn<TermFreqProp, String> termCol = new TableColumn<>("Term");
		termCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).termProperty().getName()));
		termCol.setComparator(PaliPlatform.paliComparator);
		table.getColumns().add(termCol);
		if(combineCapMenuItem.isSelected()) {
			termCol.prefWidthProperty().bind(mainPane.widthProperty().divide(5).multiply(2).subtract(Utilities.getRelativeSize(2)));
			final TableColumn<TermFreqProp, Integer> totFreqCol = new TableColumn<>("Frequency");
			totFreqCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).totalFreqProperty().getName()));
			totFreqCol.prefWidthProperty().bind(mainPane.widthProperty().divide(5));
			totFreqCol.setStyle("-fx-alignment:center-right");
			table.getColumns().add(totFreqCol);
			final TableColumn<TermFreqProp, Integer> capFreqCol = new TableColumn<>("Capitalized");
			capFreqCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).capFreqProperty().getName()));
			capFreqCol.prefWidthProperty().bind(mainPane.widthProperty().divide(5));
			capFreqCol.setStyle("-fx-alignment:center-right");
			table.getColumns().add(capFreqCol);
			final TableColumn<TermFreqProp, Double> capPercentCol = new TableColumn<>("Cap. %");
			capPercentCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).capPercentProperty().getName()));
			capPercentCol.prefWidthProperty().bind(mainPane.widthProperty().divide(5));
			capPercentCol.setStyle("-fx-alignment:center-right");
			capPercentCol.setCellFactory(col -> {
				TableCell<TermFreqProp, Double> cell = new TableCell<TermFreqProp, Double>() {
					@Override
					public void updateItem(final Double item, final boolean empty) {
						super.updateItem(item, empty);
						this.setText(null);
						this.setGraphic(null);
						if(!empty) {
							if(item > 0)
								this.setText(String.format("%.2f", item));
						}
					}
				};
				return cell;
			});
			table.getColumns().add(capPercentCol);
		} else {
			termCol.prefWidthProperty().bind(mainPane.widthProperty().divide(3).multiply(2).subtract(Utilities.getRelativeSize(2)));
			final TableColumn<TermFreqProp, Integer> freqCol = new TableColumn<>("Frequency");
			freqCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).freqProperty().getName()));
			freqCol.prefWidthProperty().bind(mainPane.widthProperty().divide(3));
			freqCol.setStyle("-fx-alignment:center-right");
			table.getColumns().add(freqCol);
		}
	}

	public boolean updateResult() {
		final boolean result;
		formatResult(maxRowCount);
		if(!processedResultMap.isEmpty()) {
			if(!shownResultList.isEmpty()) {
				setupTable();
				if(!searchTextField.getText().isEmpty())
					search();
			}
			result = true;
		} else {
			shownResultList.clear();
			result = false;
		}
		updateFixedInfo();
		return result;
	}

	private void formatResult() {
		final String filter = Normalizer.normalize(filterTextField.getText().trim(), Form.NFC);
		formatResult(maxRowCount, filter);
	}

	private void formatResult(final int maxRow) {
		final String filter = Normalizer.normalize(filterTextField.getText().trim(), Form.NFC);
		formatResult(maxRow, filter);
	}

	private void formatResult(final String filter) {
		formatResult(maxRowCount, filter);
	}

	private void formatResult(final int maxRow, final String strInput) {
		// prepare filters
		final Predicate<String> fieldFilter = x -> {
			final int ind = Integer.parseInt(x.split(":")[1]);
			final CSCDTermInfo.Field fld = CSCDTermInfo.Field.values()[ind];
			return fieldOptionsBox.isFieldSelected(fld);
		};
		final Predicate<TermFreqProp> termFilter;
		String patt = strInput == null ? "" : strInput;
		if(currFilterMode == FilterMode.WILDCARDS) {
			if(patt.isEmpty()) {
				patt = ".*";
			} else {
				if(strInput != null) {
					if(strInput.contains("?"))
						patt = patt.replace('?', '.');
					if(strInput.startsWith("*"))
						patt = patt.replaceFirst("\\*", ".*");
					if(strInput.endsWith("*"))
						patt = patt.substring(0, patt.length()-1) + ".*";
				}
			}
			final String rexPatt = patt;
			termFilter = x -> x.termProperty().get().matches(rexPatt);
		} else if(currFilterMode == FilterMode.REGEX) {
			try {
				Pattern.compile(patt);
			} catch(PatternSyntaxException e) {
				return;
			}
			termFilter = x -> x.termProperty().get().matches(strInput);
		} else { 
			termFilter = x -> x.termProperty().get().startsWith(strInput);
		}
		Predicate<TermFreqProp> caseFilter;
		if(combineCapMenuItem.isSelected()) {
			caseFilter = x -> !x.isCapitalized();
			if(onlyCapMenuItem.isSelected())
				caseFilter = caseFilter.and(x -> x.capFreqProperty().get() > 0);
		} else {
			caseFilter = x -> x.freqProperty().get() > 0;
			if(onlyCapMenuItem.isSelected())
				caseFilter = caseFilter.and(x -> x.isCapitalized());
		}
		// filter by fields first
		final List<String> filteredTerms = processedResultMap.keySet().stream().filter(fieldFilter).collect(Collectors.toList());
		// then merge identical terms in different docs and fields into the merged list
		mergedResultMap.clear();
		for(final String tfld: filteredTerms) {
			final TermFreq tf = processedResultMap.get(tfld);
			final TermFreqProp tfp;
			final String term = tfld.split(":")[0];
			if(mergedResultMap.containsKey(term)) {
				tfp = mergedResultMap.get(term);
				tfp.addUpFreq(tf);
			} else {
				tfp = new TermFreqProp(tf.getTerm(), tf.getFreq(), tf.getCapFreq());
				mergedResultMap.put(term, tfp);
			}
		}
		// filter by search term and cap option, sort and set limit, create the displayed result
		final Comparator<TermFreqProp> fcomp = combineCapMenuItem.isSelected()
											? (x, y) -> Integer.compare(y.totalFreqProperty().get(), x.totalFreqProperty().get())
											: (x, y) -> Integer.compare(y.freqProperty().get(), x.freqProperty().get());
		final List<TermFreqProp> finalResult = currFilterMode == FilterMode.METER 
												? filterByMeter(mergedResultMap.values(), strInput).stream()
																.filter(caseFilter)
																.sorted(fcomp)
																.limit(maxRow)
																.collect(Collectors.toList())
												: mergedResultMap.values().stream()
																.filter(termFilter)
																.filter(caseFilter)
																.sorted(fcomp)
																.limit(maxRow)
																.collect(Collectors.toList());
		shownResultList.setAll(finalResult);
		// prepare terms information
		currIncludedDocs = docNodeList.stream().filter(x -> x.getProcessStatus() == ProcessStatus.INCLUDED).count();
		currTotalTerms = combineCapMenuItem.isSelected()
							? mergedResultMap.values().stream().filter(x -> !x.isCapitalized()).count()
							: mergedResultMap.values().stream().filter(x -> x.freqProperty().get() > 0).count();
		currTotalCapTerms = combineCapMenuItem.isSelected()
							? mergedResultMap.values().stream().filter(x -> x.capFreqProperty().get() > 0).count()
							: mergedResultMap.values().stream().filter(x -> x.isCapitalized()).count();
		updateFixedInfo();
	}

	public Map<String, TermFreqProp> getMergedResultMap() {
		return mergedResultMap;
	}

	private List<TermFreqProp> filterByMeter(final Collection<TermFreqProp> terms, final String strInput) {
		final String inputPatt = Utilities.changeToLahuGaru(strInput);
		final List<TermFreqProp> result = inputPatt.length() == 0
											? new ArrayList<>(terms)
											: terms.stream()
												.filter(x -> Utilities.computeMeter(x.termProperty().get(), false).matches(inputPatt))
												.collect(Collectors.toList());
		return result;
	}

	private void openFieldSelector() {
		if(getRight() == null) {
			setRight(fieldOptionsBox);
		} else {
			setRight(null);
		}
	}

	private void toggleSearchPane() {
		searchTextField.clear();
		searchResultBox.getChildren().clear();
		if(splitPane.getItems().size() == 1)
			splitPane.getItems().add(searchPane);
		else
			splitPane.getItems().remove(searchPane);
	}

	private void openSearchPane() {
		if(splitPane.getItems().size() == 1) {
			splitPane.getItems().add(searchPane);
		}
	}

	private void addTermToSearch() {
		openSearchPane();
		final TermFreqProp tfp = table.getSelectionModel().getSelectedItem();
		if(tfp != null)
			addTermToSearch(tfp.termProperty().get());
	}

	private void addTermToSearch(final String term) {
		final String existing = searchTextField.getText();
		final String space = existing.isEmpty() ? "" : " ";
		searchTextField.setText(existing + space + term);
	}

	private void sendTermToDict() {
		final TermFreqProp tfp = table.getSelectionModel().getSelectedItem();
		if(tfp != null)
			PaliPlatform.showDict(tfp.termProperty().get());
	}

	private void search() {
		final String query = Normalizer.normalize(searchTextField.getText().trim(), Form.NFC);
		if(query.isEmpty())
			return;
		searchComboBox.commitValue();
		final Set<String> qTokens = new HashSet<>();
		final List<TermWeight> qList = new ArrayList<>();
		final List<TOCTreeNode> dlist = docTermFreqMap.keySet().stream()
											.filter(x -> x.getProcessStatus() == ProcessStatus.INCLUDED)
											.collect(Collectors.toList());
		for(final String qstr : query.split(Utilities.REX_NON_PALI)) {
			qTokens.add(qstr);
			if(autoCapMenuItem.isSelected()) {
				// add capitalized terms if needed
				if(Character.isLowerCase(qstr.charAt(0)))
					qTokens.add(Character.toUpperCase(qstr.charAt(0)) + qstr.substring(1));
			}
		}
		// compute query weighting for each term
		qTokens.forEach(qterm -> {
			final TermWeight tw = new TermWeight(qterm);
			for(final TOCTreeNode ttn : dlist) {
				final Map<String, Map<CSCDTermInfo.Field, Integer>> tfMap = docTermFreqMap.get(ttn);
				if(tfMap.containsKey(qterm)) {
					final Set<CSCDTermInfo.Field> fldSet = tfMap.get(qterm).keySet();
					fldSet.forEach(fld -> tw.increaseDocCount(fld));
				}
			}
			tw.computeQueryWeight(dlist.size());
			qList.add(tw);
		});
		// compute similarity between query and documents
		final Set<TOCTreeNode> resultSet = new HashSet<>();
		final Set<CSCDTermInfo.Field> foundFieldSet = EnumSet.noneOf(CSCDTermInfo.Field.class);
		weightedResultMap.values().forEach(x -> x.resetDocScores());
		for(final TermWeight qtw : qList) {
			final String term = qtw.getTerm();
			if(weightedResultMap.containsKey(term)) {
				final TermWeight wtw = weightedResultMap.get(term);
				final Map<TOCTreeNode, Map<CSCDTermInfo.Field, Double>> simMap = wtw.getSimScores(qtw);
				simMap.forEach((doc, map) -> {
					map.forEach((fld, score) -> {
						if(fieldOptionsBox.isFieldSelected(fld) && score > 0.0) {
							doc.addSearchScore(score);
							foundFieldSet.add(fld);
						}
					});
					resultSet.add(doc);
				});
			}
		}
		showSearchResult(resultSet, qTokens, foundFieldSet);
	}

	private void showSearchResult(final Set<TOCTreeNode> docSet, final Set<String> queries, final Set<CSCDTermInfo.Field> fieldSet) {
		searchResultBox.getChildren().clear();
		final List<TitledPane> finalResultList = new ArrayList<>();
		final List<String> qList = new ArrayList<>(queries);
		final List<TOCTreeNode> docList = new ArrayList<>(docSet);
		final int resSize = docSet.size();
		if(resSize < SEARCH_RESULT_MAX_DOC) {
			// if a few or none of result found, add other zero-scored docs
			int count = resSize;
			for(final TOCTreeNode doc : docNodeList) {
				if(doc.getProcessStatus() == ProcessStatus.INCLUDED && !docList.contains(doc)) {
					docList.add(doc);
					if(count++ >= SEARCH_RESULT_MAX_DOC)
						break;
				}
			}
		}
		for(final TOCTreeNode doc : docList) {
			final TextFlow tflow = new TextFlow();
			final String textName = doc.isInArchive() ? " (" + doc.getTextName() + ")" : "";
			final double score = doc.getSearchScore();
			final String docInfo = doc.getFileName() + textName + String.format(" [Score: %.4f]", score);
			final Map<String, CSCDTermInfo> allTermsMap = doc.getTermsMap();
			final Map<String, List<Text>> resultMap = new HashMap<>();
			final Set<Integer> qHashSet = new HashSet<>();
			final Random random = new Random();
			final long[] maxQScore = { 0 };
			for(final String query : qList) {
				if(allTermsMap.containsKey(query)) {
					final CSCDTermInfo tinfo = allTermsMap.get(query);
					final Map<CSCDTermInfo.Field, List<int[]>> postingMap = tinfo.getPostingMap();
					postingMap.forEach((fld, plist) -> {
						if((score > 0.0 && fieldSet.contains(fld)) || (score == 0.0 && fieldOptionsBox.isFieldSelected(fld))) {
							plist.forEach(pt -> {
								final int line = pt[0];
								final int pos = pt[1];
								// find other terms in the same line
								final List<String> portionList = new ArrayList<>();
								final List<Text> contextList = new ArrayList<>();
								allTermsMap.values().forEach(x -> {
									final List<int[]> poslist = x.getPositionList(fld);
									if(poslist != null) {
										for(final int[] p : poslist) {
											if(p[0] == line && (p[1] == pos || abs(p[1] - pos) <= searchResultWinSizeSpinner.getValue()))
												portionList.add(p[1] + ":" + x.getTerm());
										}
									}
								});
								final List<String> finalString = portionList.stream()
												.sorted((x, y) -> Integer.compare(Integer.parseInt(x.split(":")[0]), Integer.parseInt(y.split(":")[0])))
												.map(x -> x.split(":")[1])
												.collect(Collectors.toList());
								final Text bulText = new Text("» ");
								bulText.getStyleClass().add("search-result-normal");
								contextList.add(bulText);
								final Map<String, Boolean> qFoundMap = new HashMap<>();
								for(final String s : finalString) {
									final Text resText = new Text(s + " ");
									boolean isQuery = false;
									for(final String q : qList) {
										if(q.equals(s)) {
											qFoundMap.put(q, true);
											isQuery = true;
											break;
										}
									}
									if(isQuery)
										resText.getStyleClass().add("search-result-highlight");
									else
										resText.getStyleClass().add("search-result-normal");
									contextList.add(resText);
								}
								final Text fldText = new Text(" [" + fld.toString()+ "]");
								fldText.getStyleClass().add("search-result-faded");
								contextList.add(fldText);
								int qhash = random.nextInt();
								while(qHashSet.contains(qhash))
									qhash = random.nextInt();
								qHashSet.add(qhash);
								// find out how many query terms in the line
								final long qscore = qFoundMap.values().stream().filter(x -> x == true).count();
								if(qscore > maxQScore[0])
									maxQScore[0] = qscore;
								resultMap.put(qscore + ":" + qhash, contextList);
							});
						}
					});
				}
			}
			// sort the result by qscore, occurrences of query terms in the lines
			final List<String> keyList = resultMap.keySet().stream()
											.sorted((x, y) -> Integer.compare(Integer.parseInt(y.split(":")[0]), Integer.parseInt(x.split(":")[0])))
											.collect(Collectors.toList());
			for(int i = 0; i < keyList.size(); i++) {
				tflow.getChildren().addAll(resultMap.get(keyList.get(i)));
				if(i < keyList.size() - 1)
					tflow.getChildren().add(new Text("\n"));
			}
			if(!tflow.getChildren().isEmpty()) {
				final TitledPane tpane = new TitledPane(docInfo, tflow);
				doc.setMaxQueryFound((int)maxQScore[0]);
				tpane.setUserData(doc);
				tpane.setContextMenu(searchResultPopupMenu);
				tpane.setOnContextMenuRequested(cmevent -> {
					final TitledPane tp = (TitledPane)cmevent.getSource();
					currSelectedDoc = (TOCTreeNode)tp.getUserData();
					isPlainText.set(currSelectedDoc.isPlainText());
				});
				tpane.setExpanded(false);
				finalResultList.add(tpane);
			}
		}
		if(!finalResultList.isEmpty()) {
			final Map<Integer, List<TitledPane>> finalMap = finalResultList.stream()
															.collect(Collectors.groupingBy(x -> {
																final TOCTreeNode ttn = (TOCTreeNode)x.getUserData();
																return ttn.getMaxQueryFound();
															}));
			final List<Integer> finalKeys = finalMap.keySet().stream().sorted((x, y) -> Integer.compare(y, x)).collect(Collectors.toList());
			finalKeys.forEach(k -> {
				final List<TitledPane> res = finalMap.get(k).stream()
											.sorted((x, y) -> {
												final double xScore = ((TOCTreeNode)x.getUserData()).getSearchScore();
												final double yScore = ((TOCTreeNode)y.getUserData()).getSearchScore();
												return Double.compare(yScore, xScore);
											})
											.collect(Collectors.toList());
				res.forEach(x -> searchResultBox.getChildren().add(x));
			});
			searchTextInput.recordQuery();
		}
	}

	private void foldSearchResult(final boolean doExpand) {
		searchResultBox.getChildren().forEach(x -> {
			if(x instanceof TitledPane)
				((TitledPane)x).setExpanded(doExpand);
		});
	}

	private void openCurrentDoc() {
		if(currSelectedDoc == null) return;
		if(currSelectedDoc.isPlainText()) {
			final File nfile = new File(Utilities.EXTRAPATH + currSelectedDoc.getFileName());
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, new File[] { nfile });
		} else {
			PaliPlatform.openPaliHtmlViewer(currSelectedDoc);
		}
	}

	private void openAsText(final boolean withNotes) {
		if(currSelectedDoc != null)
			Utilities.openXMLDocAsText(currSelectedDoc, withNotes);
	}

	private String makeCSV() {
		final String DELIM = ",";
		// generate csv string
		final StringBuilder result = new StringBuilder();
		// table columns
		for(int i=0; i<table.getColumns().size(); i++){
			result.append(table.getColumns().get(i).getText()).append(DELIM);
		}
		result.append(System.getProperty("line.separator"));
		// table data
		for(int i=0; i<table.getItems().size(); i++){
			final TermFreqProp tfp = table.getItems().get(i);
			result.append(tfp.termProperty().get()).append(DELIM);
			if(combineCapMenuItem.isSelected()) {
				result.append(tfp.totalFreqProperty().get()).append(DELIM);
				result.append(tfp.capFreqProperty().get()).append(DELIM);
				result.append(String.format("%.2f", tfp.capPercentProperty().get())).append(DELIM);
			} else {
				result.append(tfp.freqProperty().get()).append(DELIM);
			}
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}
	
	private void copyCSV() {
		Utilities.copyText(makeCSV());
	}
	
	private void saveCSV() {
		Utilities.saveText(makeCSV(), "termlist.csv");
	}
}
