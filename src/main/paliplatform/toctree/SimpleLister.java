/*
 * SimpleLister.java
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
import java.util.stream.*;
import java.util.regex.*;
import java.sql.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.util.Callback;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

/** 
 * This window shows simple lists of CSCD documents.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class SimpleLister extends BorderPane {
	private final int DEF_MAX_ROW = 500;
	private static final String[] cscdGroup = { "VM", "SM", "AM", "VA", "SA", "AA", "VT", "ST", "AT", "E" };
	private static final int[] cscdDocs = { 56, 541, 157, 55, 524, 92, 298, 282, 222, 471 };
	private static final int[] cscdTerms = { 32558, 135572, 18267, 61016, 338837, 61994, 195656, 200248, 117967, 320103 };
	private static final int[] vsaSumTerms = { 223584, 502169, 161748 };
	private static final int[] matSumTerms = { 161969, 390916, 421294 };
	private static final int canonSumTerms = 725641;
	private static final int totalSumTerms = 922850;
	private final BorderPane mainPane = new BorderPane();
	private final VBox contentBox = new VBox();
	private final ChoiceBox<String> freqRangeChoice = new ChoiceBox<>();
	private final ChoiceBox<Integer> maxRowChoice = new ChoiceBox<>();
	private final TableView<SimpleTermFreqProp> table = new TableView<>();
	private final ObservableList<SimpleTermFreqProp> shownResultList = FXCollections.<SimpleTermFreqProp>observableArrayList();
	private final PaliTextInput searchTextInput = new PaliTextInput(PaliTextInput.InputType.FIELD);
	private final TextField searchTextField;
	private final Map<Toggle, Tokenizer.FilterMode> filterModeMap = new HashMap<>();
	private final ToggleGroup termFilterGroup = new ToggleGroup();
	private final RadioMenuItem filterSimpleMenuItem = new RadioMenuItem("Simple filter");
	private final ChoiceBox<Integer> firstCharGroupChoice = new ChoiceBox<>();
	private final ChoiceBox<Integer> lastCharGroupChoice = new ChoiceBox<>();
	private final SimpleBooleanProperty[] textGroupProp = new SimpleBooleanProperty[10];
	private final VBox selectionBox = new VBox();
	private final HBox canonGroupBox = new HBox(8);
	private final HBox summaryBox = new HBox();
	private final GridPane summaryGrid;
	private final Label fixedInfoLabel = new Label();
	private final InfoPopup filterHelpPopup = new InfoPopup();
	private final TableLabel[] mulLabels = new TableLabel[3];
	private final TableLabel[] attLabels = new TableLabel[3];
	private final TableLabel[] tikLabels = new TableLabel[3];
	private final TableLabel[] vsaSumLabels = new TableLabel[3];
	private final TableLabel[] matSumLabels = new TableLabel[3];
	private final TableLabel canSumLabel = new TableLabel();
	private final TableLabel annLabel = new TableLabel();
	private final TableLabel totSumLabel = new TableLabel();
	private final RadioMenuItem sumByTermMenuItem = new RadioMenuItem("Term summary");
	private final RadioMenuItem sumByDocMenuItem = new RadioMenuItem("Document summary");
	private Tokenizer.FilterMode currFilterMode = Tokenizer.FilterMode.SIMPLE;
	private PaliTextInput.InputMethod savInputMethod = PaliTextInput.InputMethod.UNUSED_CHARS;
	private String currFreqRange = ">= 1";
	private int maxRowCount = DEF_MAX_ROW;
	private int totalTerms = 922850;
	
	public SimpleLister() {
		// add toolbar on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(table);
		// config some buttons
		toolBar.cameraButton.setOnAction(actionEvent -> Utilities.saveSnapshot(mainPane));
		toolBar.saveTextButton.setOnAction(actionEvent -> saveCSV());		
		toolBar.copyButton.setOnAction(actionEvent -> copyCSV());		
		// add new components
		final List<String> freqRangeList = Arrays.asList("= 1", ">= 1", "> 1", "2 - 10", "> 10", "> 100", "> 1000", "> 10000");
		freqRangeChoice.setTooltip(new Tooltip("Frequency range"));
		freqRangeChoice.getItems().addAll(freqRangeList);
		freqRangeChoice.getSelectionModel().select(1);
		freqRangeChoice.setOnAction(actionEvent -> {
			currFreqRange = freqRangeChoice.getSelectionModel().getSelectedItem();
			updateResult();
		});
		final List<Integer> maxList = Arrays.asList(50, 100, 500, 1000, 5000, 10000, 50000, 100000, 550000, 1000000);
		maxRowChoice.setTooltip(new Tooltip("Maximum rows"));
		maxRowChoice.getItems().addAll(maxList);
		maxRowChoice.getSelectionModel().select(2);
		maxRowChoice.setOnAction(actionEvent -> {
			maxRowCount = maxRowChoice.getSelectionModel().getSelectedItem();
			updateResult();
		});
		final Button summaryButton = new Button("", new TextIcon("Σ", TextIcon.IconSet.SERIF));
		summaryButton.setTooltip(new Tooltip("Show summary on/off"));
		summaryButton.setOnAction(actionEvent -> toggleSummary());
		final MenuButton summaryOptionMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		summaryOptionMenu.setTooltip(new Tooltip("Options"));
		final ToggleGroup summaryGroup = new ToggleGroup();
		sumByTermMenuItem.setSelected(true);
		summaryGroup.getToggles().addAll(sumByTermMenuItem, sumByDocMenuItem);
		summaryGroup.selectedToggleProperty().addListener((observable) -> updateSummary());
		summaryOptionMenu.getItems().addAll(sumByTermMenuItem, sumByDocMenuItem);
		toolBar.getItems().addAll(new Separator(), freqRangeChoice, maxRowChoice, summaryButton, summaryOptionMenu);
		setTop(toolBar);

		// set main pane at the center
		// set up search toolbar
		final ToolBar searchToolBar = new ToolBar();
		searchTextField = (TextField)searchTextInput.getInput();
		searchTextField.setPromptText("Search for...");
		searchTextField.setPrefWidth(Utilities.getRelativeSize(23));
		searchTextField.textProperty().addListener((obs, oldValue, newValue) -> {
			updateResult();
		});
		final MenuButton filterOptionMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		filterOptionMenu.setTooltip(new Tooltip("Options"));
		final RadioMenuItem filterWildcardsMenuItem = new RadioMenuItem("Using ? and *");
		final RadioMenuItem filterRegexMenuItem = new RadioMenuItem("Regular expression");
		final RadioMenuItem filterMeterMenuItem = new RadioMenuItem("Filter by meter");
		filterModeMap.put(filterSimpleMenuItem, Tokenizer.FilterMode.SIMPLE);
		filterModeMap.put(filterWildcardsMenuItem, Tokenizer.FilterMode.WILDCARDS);
		filterModeMap.put(filterRegexMenuItem, Tokenizer.FilterMode.REGEX);
		filterModeMap.put(filterMeterMenuItem, Tokenizer.FilterMode.METER);
		termFilterGroup.getToggles().addAll(filterSimpleMenuItem, filterWildcardsMenuItem, filterRegexMenuItem, filterMeterMenuItem);
		termFilterGroup.selectToggle(filterSimpleMenuItem);
		termFilterGroup.selectedToggleProperty().addListener((observable) -> {
			if(termFilterGroup.getSelectedToggle() != null) {
				final Toggle selected = (Toggle)termFilterGroup.getSelectedToggle();
				currFilterMode = filterModeMap.get(selected);
				if(currFilterMode == Tokenizer.FilterMode.METER) {
					savInputMethod = searchTextInput.getInputMethod();
					searchTextInput.setInputMethod(PaliTextInput.InputMethod.METER_GROUP);
				} else {
					searchTextInput.setInputMethod(savInputMethod);
				}
				updateResult();
			}
		});
		filterOptionMenu.getItems().addAll(filterSimpleMenuItem, filterWildcardsMenuItem, filterRegexMenuItem, filterMeterMenuItem);
		final List<Integer> charGroupList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		firstCharGroupChoice.setTooltip(new Tooltip("# of first characters grouping"));
		firstCharGroupChoice.getItems().addAll(charGroupList);
		firstCharGroupChoice.getSelectionModel().select(0);
		firstCharGroupChoice.setOnAction(actionEvent -> updateResult());
		lastCharGroupChoice.setTooltip(new Tooltip("# of last characters grouping"));
		lastCharGroupChoice.getItems().addAll(charGroupList);
		lastCharGroupChoice.getSelectionModel().select(0);
		lastCharGroupChoice.setOnAction(actionEvent -> updateResult());
		final Button zeroResetButton = new Button("", new TextIcon("0", TextIcon.IconSet.AWESOME));
		zeroResetButton.setTooltip(new Tooltip("Reset all to 0"));
		zeroResetButton.setOnAction(actionEvent -> {
			firstCharGroupChoice.getSelectionModel().select(0);
			lastCharGroupChoice.getSelectionModel().select(0);
		});
		final Button filterHelpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		filterHelpButton.setOnAction(actionEvent -> filterHelpPopup.showPopup(filterHelpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		searchToolBar.getItems().addAll(searchTextField, searchTextInput.getClearButton(),
									searchTextInput.getMethodButton(), filterOptionMenu,
									new Separator(), firstCharGroupChoice, new Label(Utilities.DASH), lastCharGroupChoice, zeroResetButton, 
									new Separator(), filterHelpButton);
		mainPane.setTop(searchToolBar);
		// set up canon group selection at the bottom
		for(int i = 0; i < textGroupProp.length; i++)
			textGroupProp[i] = new SimpleBooleanProperty(true);
		final EventHandler<ActionEvent> textGroupEvent = e -> updateResult();
		final String canonGroupHeadStyle = "-fx-font-size:80%;";
		final Button allButt = new Button("", new TextIcon("asterisk", TextIcon.IconSet.AWESOME));
		allButt.setTooltip(new Tooltip("Toggle all"));
		allButt.setOnAction(actionEvent -> toggleAllTextGroupSelection());
		final VBox mulVBox = new VBox();
		mulVBox.setAlignment(Pos.BOTTOM_CENTER);
		final Label lbMulHead = new Label("Mūla");
		lbMulHead.setTooltip(new Tooltip("Main texts"));
		lbMulHead.setOnMouseClicked(mouseEvent -> selectTextGroup(Arrays.asList(0, 1, 2)));
		lbMulHead.setStyle(canonGroupHeadStyle);
		final HBox mulHBox = new HBox(8);
		final CheckBox cbVM = new CheckBox("V");
		cbVM.setTooltip(new Tooltip("Vinaya"));
		cbVM.selectedProperty().bindBidirectional(textGroupProp[0]);
		cbVM.setOnAction(textGroupEvent);
		final CheckBox cbSM = new CheckBox("S");
		cbSM.setTooltip(new Tooltip("Suttanta"));
		cbSM.selectedProperty().bindBidirectional(textGroupProp[1]);
		cbSM.setOnAction(textGroupEvent);
		final CheckBox cbAM = new CheckBox("A");
		cbAM.setTooltip(new Tooltip("Abhidhamma"));
		cbAM.selectedProperty().bindBidirectional(textGroupProp[2]);
		cbAM.setOnAction(textGroupEvent);
		mulHBox.getChildren().addAll(cbVM, cbSM, cbAM);
		mulVBox.getChildren().addAll(lbMulHead, mulHBox);
		final VBox attVBox = new VBox();
		attVBox.setAlignment(Pos.BOTTOM_CENTER);
		final Label lbAttHead = new Label("Aṭṭhakathā");
		lbAttHead.setTooltip(new Tooltip("Commentaries"));
		lbAttHead.setOnMouseClicked(mouseEvent -> selectTextGroup(Arrays.asList(3, 4, 5)));
		lbAttHead.setStyle(canonGroupHeadStyle);
		final HBox attHBox = new HBox(8);
		final CheckBox cbVA = new CheckBox("V");
		cbVA.setTooltip(new Tooltip("Vinaya"));
		cbVA.selectedProperty().bindBidirectional(textGroupProp[3]);
		cbVA.setOnAction(textGroupEvent);
		final CheckBox cbSA = new CheckBox("S");
		cbSA.setTooltip(new Tooltip("Suttanta"));
		cbSA.selectedProperty().bindBidirectional(textGroupProp[4]);
		cbSA.setOnAction(textGroupEvent);
		final CheckBox cbAA = new CheckBox("A");
		cbAA.setTooltip(new Tooltip("Abhidhamma"));
		cbAA.selectedProperty().bindBidirectional(textGroupProp[5]);
		cbAA.setOnAction(textGroupEvent);
		attHBox.getChildren().addAll(cbVA, cbSA, cbAA);
		attVBox.getChildren().addAll(lbAttHead, attHBox);
		final VBox tikVBox = new VBox();
		tikVBox.setAlignment(Pos.BOTTOM_CENTER);
		final Label lbTikHead = new Label("Ṭīkā");
		lbTikHead.setTooltip(new Tooltip("Subcommentaries"));
		lbTikHead.setOnMouseClicked(mouseEvent -> selectTextGroup(Arrays.asList(6, 7, 8)));
		lbTikHead.setStyle(canonGroupHeadStyle);
		final HBox tikHBox = new HBox(8);
		final CheckBox cbVT = new CheckBox("V");
		cbVT.setTooltip(new Tooltip("Vinaya"));
		cbVT.selectedProperty().bindBidirectional(textGroupProp[6]);
		cbVT.setOnAction(textGroupEvent);
		final CheckBox cbST = new CheckBox("S");
		cbST.setTooltip(new Tooltip("Suttanta"));
		cbST.selectedProperty().bindBidirectional(textGroupProp[7]);
		cbST.setOnAction(textGroupEvent);
		final CheckBox cbAT = new CheckBox("A");
		cbAT.setTooltip(new Tooltip("Abhidhamma"));
		cbAT.selectedProperty().bindBidirectional(textGroupProp[8]);
		cbAT.setOnAction(textGroupEvent);
		tikHBox.getChildren().addAll(cbVT, cbST, cbAT);
		tikVBox.getChildren().addAll(lbTikHead, tikHBox);
		final VBox annVBox = new VBox();
		annVBox.setAlignment(Pos.BOTTOM_CENTER);
		final Label lbAnnHead = new Label("Añña");
		lbAnnHead.setTooltip(new Tooltip("Other texts"));
		lbAnnHead.setOnMouseClicked(mouseEvent -> selectTextGroup(Arrays.asList(9)));
		lbAnnHead.setStyle(canonGroupHeadStyle);
		final CheckBox cbE = new CheckBox("");
		cbE.selectedProperty().bindBidirectional(textGroupProp[9]);
		cbE.setOnAction(textGroupEvent);
		annVBox.getChildren().addAll(lbAnnHead, cbE);
		canonGroupBox.setAlignment(Pos.BOTTOM_CENTER);
		canonGroupBox.getChildren().addAll(allButt, new Separator(Orientation.VERTICAL),
							mulVBox, new Separator(Orientation.VERTICAL),
							attVBox, new Separator(Orientation.VERTICAL),
							tikVBox, new Separator(Orientation.VERTICAL),
							annVBox);

		selectionBox.setAlignment(Pos.BOTTOM_CENTER);
		selectionBox.getChildren().add(canonGroupBox);
		mainPane.setBottom(selectionBox);
		// set up table at the center
		final ContextMenu tablePopupMenu = new ContextMenu();
		final MenuItem sendToDictMenuItem = new MenuItem("Send to Dictionaries");
		sendToDictMenuItem.setOnAction(actionEvent -> sendTermToDict());
		final MenuItem sendToFinderMenuItem = new MenuItem("Send to Document Finder");
		sendToFinderMenuItem.setOnAction(actionEvent -> sendTermToFinder());
		tablePopupMenu.getItems().addAll(sendToDictMenuItem, sendToFinderMenuItem);
		table.setContextMenu(tablePopupMenu);
		table.setOnDragDetected(mouseEvent -> {
			final SimpleTermFreqProp selected = (SimpleTermFreqProp)table.getSelectionModel().getSelectedItem();
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
		updateResult();
		table.setItems(shownResultList);
		setupTable();
		VBox.setVgrow(table, Priority.ALWAYS);
		contentBox.getChildren().add(table);
		mainPane.setCenter(contentBox);
		setCenter(mainPane);

		// set status bar at the bottom
		final AnchorPane statusPane = new AnchorPane();
		AnchorPane.setBottomAnchor(fixedInfoLabel, 0.0);
		AnchorPane.setRightAnchor(fixedInfoLabel, 0.0);
		fixedInfoLabel.setStyle("-fx-font-family:'" + Utilities.FONTMONO +"';-fx-font-size:85%;");
		statusPane.getChildren().add(fixedInfoLabel);
		setBottom(statusPane);

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
					searchTextField.setText(Utilities.getUsablePaliWord(head));
				}
				dragEvent.setDropCompleted(true);
			} else {
				dragEvent.setDropCompleted(false);
			}
			dragEvent.consume();
		});

		// some other initialization
		Utilities.createMeterPatternMap();
		summaryGrid = createSummaryPane();
		summaryBox.getStyleClass().add("stanzabox");
		summaryBox.setAlignment(Pos.BOTTOM_CENTER);
		summaryBox.getChildren().add(summaryGrid);
		filterHelpPopup.setContent("info-lister-filter.txt");
		filterHelpPopup.setTextWidth(Utilities.getRelativeSize(32));
	}

	public void init() {
		currFreqRange = ">= 1";
		maxRowCount = DEF_MAX_ROW;
		freqRangeChoice.getSelectionModel().select(1);
		maxRowChoice.getSelectionModel().select(2);
		firstCharGroupChoice.getSelectionModel().select(0);
		lastCharGroupChoice.getSelectionModel().select(0);
		searchTextField.clear();
		termFilterGroup.selectToggle(filterSimpleMenuItem);
		sumByTermMenuItem.setSelected(true);
		for(final SimpleBooleanProperty tg : textGroupProp)
			tg.set(true);
		updateResult();
	}

	private void toggleAllTextGroupSelection() {
		final boolean value = !isAllTextGroupSelected();
		for(final SimpleBooleanProperty tg : textGroupProp)
			tg.set(value);
		updateResult();
	}

	private boolean isAllTextGroupSelected() {
		boolean result = true;
		for(final SimpleBooleanProperty tg : textGroupProp) {
			if(!tg.get()) {
				result = false;
				break;
			}
		}
		return result;
	}

	private void selectTextGroup(final List<Integer> list) {
		for(int i = 0; i < textGroupProp.length; i++)
			textGroupProp[i].set(list.contains(i));
		updateResult();
	}

	private void updateResult() {
		shownResultList.clear();
		final String searchText = Normalizer.normalize(searchTextField.getText().trim(), Form.NFC);
		final List<SimpleTermFreqProp> result;
		if(currFilterMode == Tokenizer.FilterMode.METER)
			result = filterByMeter(getTermListFromDB(), searchText);
		else
			result = getTermListFromDB(searchText);
		shownResultList.addAll(filterByCharGroup(result));
		updateFixedInfo();
	}

	private void updateFixedInfo() {
		final int shownDocs = getShownDocs();
		final int totalDocs = 2698;
		final int shownTerms = shownResultList.size();
		final String info = String.format("Docs: %,4d of %,4d | Terms: %,9d of %,9d",
							shownDocs, totalDocs, shownTerms, totalTerms);
		fixedInfoLabel.setText(info);
	}

	private int getShownDocs() {
		int sum = 0;
		for(int i = 0; i < cscdDocs.length; i++) {
			if(textGroupProp[i].get())
				sum += cscdDocs[i];
		}
		return sum;
	}

	private List<SimpleTermFreqProp> filterByCharGroup(final Collection<SimpleTermFreqProp> terms) {
		final int nFirst = firstCharGroupChoice.getSelectionModel().getSelectedItem();
		final int nLast = lastCharGroupChoice.getSelectionModel().getSelectedItem();
		final List<SimpleTermFreqProp> result = new ArrayList<>();
		if(nFirst > 0) {
			final Map<String, List<SimpleTermFreqProp>> headingMap = terms.stream()
																		.filter(x -> x.termProperty().get().length() >= nFirst)
																		.collect(Collectors.groupingBy(x -> {
																			final String term = x.termProperty().get();
																			return term.substring(0, nFirst);
																		}));
			if(nLast > 0) {
				headingMap.forEach((tf, lf) -> {
					final Map<String, List<SimpleTermFreqProp>> endingMap = lf.stream()
																				.filter(x -> x.termProperty().get().length() >= nLast)
																				.collect(Collectors.groupingBy(x -> {
																					final String term = x.termProperty().get();
																					final int len = term.length();
																					return term.substring(len - nLast, len);
																				}));
					endingMap.forEach((tl, ll) -> {
						final long totFreq = ll.stream().map(x -> x.freqProperty().get()).reduce(0, Integer::sum);
						final SimpleTermFreqProp tfp = new SimpleTermFreqProp(tf + "-" + tl, (int)totFreq);
						result.add(tfp);
					});
				});
			} else {
				headingMap.forEach((t, lst) -> {
					final long totFreq = lst.stream().map(x -> x.freqProperty().get()).reduce(0, Integer::sum);
					final SimpleTermFreqProp tfp = new SimpleTermFreqProp(t + "-", (int)totFreq);
					result.add(tfp);
				});
			}
		} else {
			if(nLast > 0) {
				final Map<String, List<SimpleTermFreqProp>> endingMap = terms.stream()
																			.filter(x -> x.termProperty().get().length() >= nLast)
																			.collect(Collectors.groupingBy(x -> {
																				final String term = x.termProperty().get();
																				final int len = term.length();
																				return term.substring(len - nLast, len);
																			}));
				endingMap.forEach((t, lst) -> {
					final long totFreq = lst.stream().map(x -> x.freqProperty().get()).reduce(0, Integer::sum);
					final SimpleTermFreqProp tfp = new SimpleTermFreqProp("-" + t, (int)totFreq);
					result.add(tfp);
				});
			} else {
				result.addAll(terms);
				return result;
			}
		}
		result.sort((x, y) -> Integer.compare(y.freqProperty().get(), x.freqProperty().get()));
		return result;
	}

	private List<SimpleTermFreqProp> filterByMeter(final Collection<SimpleTermFreqProp> terms, final String strInput) {
		final String inputPatt = Utilities.changeToLahuGaru(strInput);
		final List<SimpleTermFreqProp> result = inputPatt.length() == 0
												? new ArrayList<>(terms)
												: terms.stream()
													.filter(x -> Utilities.computeMeter(x.termProperty().get(), false).matches(inputPatt))
													.collect(Collectors.toList());
		return result;
	}

	private Callback<TableColumn<SimpleTermFreqProp, Integer>, TableCell<SimpleTermFreqProp, Integer>> getIntegerCellFactory() {
		return col -> {
			TableCell<SimpleTermFreqProp, Integer> cell = new TableCell<SimpleTermFreqProp, Integer>() {
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
		};
	}

	private void setupTable() {
		table.getColumns().clear();
		final TableColumn<SimpleTermFreqProp, String> termCol = new TableColumn<>("Term");
		termCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).termProperty().getName()));
		termCol.setComparator(PaliPlatform.paliComparator);
		termCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(7.5).subtract(Utilities.getRelativeSize(2)));
		table.getColumns().add(termCol);
		final TableColumn<SimpleTermFreqProp, Integer> freqCol = new TableColumn<>("Frequency");
		freqCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).freqProperty().getName()));
		freqCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(1.5));
		freqCol.setStyle("-fx-alignment:center-right");
		freqCol.setCellFactory(getIntegerCellFactory());
		table.getColumns().add(freqCol);
		final TableColumn<SimpleTermFreqProp, Integer> lengthCol = new TableColumn<>("Length");
		lengthCol.setCellValueFactory(new PropertyValueFactory<>(shownResultList.get(0).lengthProperty().getName()));
		lengthCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10));
		lengthCol.setStyle("-fx-alignment:center-right");
		lengthCol.setCellFactory(getIntegerCellFactory());
		table.getColumns().add(lengthCol);
	}

	private List<SimpleTermFreqProp> getTermListFromDB() {
		return getTermListFromDB("");
	}

	private List<SimpleTermFreqProp> getTermListFromDB(final String text) {
		final List<SimpleTermFreqProp> result = new ArrayList<>();
		final String tCondition;
		if(text.isEmpty()) {
			tCondition = "";
		} else {
			if(currFilterMode == Tokenizer.FilterMode.WILDCARDS) {
				final String query = text.replace("*", "%").replace("?", "_");
				tCondition = " AND TERM LIKE '" + query + "' ";
			} else if(currFilterMode == Tokenizer.FilterMode.REGEX) {
				try {
					Pattern.compile(text);
				} catch(PatternSyntaxException e) {
					return Collections.emptyList();
				}
				tCondition = " AND TERM REGEXP '" + text + "' ";
			} else if(currFilterMode == Tokenizer.FilterMode.SIMPLE) {
				tCondition = " AND TERM LIKE '" + text + "%'";
			} else {
				tCondition = "";
			}
		}
		final String query;
		final String totQuery;
		final String fCondition;
		if(isAllTextGroupSelected()) {
			fCondition = "FREQUENCY" + (currFreqRange.equals("2 - 10") ? " >= 2 AND FREQUENCY <= 10 " : currFreqRange);
			query = "SELECT TERM,FREQUENCY FROM CSCDTERMS WHERE " + fCondition + tCondition + " LIMIT " + maxRowCount + ";";
			totQuery = "SELECT COUNT(*) FROM CSCDTERMS WHERE " + fCondition + ";";
		} else {
			final List<String> cscdCols = new ArrayList<>();
			for(int i = 0; i < textGroupProp.length; i++)
				if(textGroupProp[i].get())
					cscdCols.add(cscdGroup[i] + "FREQ");
			if(cscdCols.isEmpty()) {
				query = "";
				totQuery = "";
			} else {
				if(cscdCols.size() == 1) {
					final String col = cscdCols.get(0);
					fCondition = col + (currFreqRange.equals("2 - 10") ? " >= 2 AND " + col + " <= 10 " : currFreqRange);
					query = "SELECT TERM," + col + " FROM CSCDTERMS WHERE " + fCondition + tCondition
							+ " ORDER BY " + col + " DESC LIMIT " + maxRowCount + ";";
					totQuery = "SELECT COUNT(*) FROM CSCDTERMS WHERE " + fCondition + ";";
				} else {
					final String colSum = cscdCols.stream().collect(Collectors.joining("+"));
					final String colCond = " (" + cscdCols.stream().collect(Collectors.joining(" > 0 OR ")) + " > 0) ";
					final String andWhere = " AND " + colCond;
					fCondition = colSum + (currFreqRange.equals("2 - 10") ? " >= 2 AND " + colSum + " <= 10 " : currFreqRange);
					query = "SELECT TERM," + colSum + " FROM CSCDTERMS WHERE " + fCondition + andWhere + tCondition
							+ " ORDER BY " + colSum + " DESC LIMIT " + maxRowCount + ";";
					totQuery = "SELECT COUNT(*) FROM CSCDTERMS WHERE " + fCondition + andWhere + ";";
				}
			}
		}
		if(!query.isEmpty()) {
			try {
				if(Utilities.dbConn != null) {
					final Statement stmt = Utilities.dbConn.createStatement();
					final ResultSet rsData = stmt.executeQuery(query);
					while(rsData.next())
						result.add(new SimpleTermFreqProp(rsData.getString(1), rsData.getInt(2)));
					final ResultSet rsTot = stmt.executeQuery(totQuery);
					if(rsTot.next())
						totalTerms = rsTot.getInt(1);
					rsData.close();
					rsTot.close();
					stmt.close();
				}
			} catch(SQLException e) {
				System.err.println(e);
			}
		} else {
			totalTerms = 0;
		}
		return result;
	}

	private void toggleSummary() {
		final int num = selectionBox.getChildren().size();
		selectionBox.getChildren().clear();
		if(num == 1) {
			selectionBox.getChildren().addAll(summaryBox, canonGroupBox);
			updateSummary();
		} else {
			selectionBox.getChildren().addAll(canonGroupBox);
		}
	}

	private GridPane createSummaryPane() {
		final String boldStyle = "-fx-font-weight:bold;";
		final GridPane resultGrid = new GridPane();
		final TableLabel lbCSCD = new TableLabel("Chaṭṭha Saṅgāyana CD");
		final TableLabel lbVin = new TableLabel("Vinayapiṭaka");
		final TableLabel lbSut = new TableLabel("Suttapiṭaka");
		final TableLabel lbAbh = new TableLabel("Abhidhammapiṭaka");
		final TableLabel lbMul = new TableLabel("        Mūla");
		final TableLabel lbAtt = new TableLabel("  Aṭṭhakathā");
		final TableLabel lbTik = new TableLabel("        Ṭīkā");
		final TableLabel lbAnn = new TableLabel("Añña");
		final TableLabel lbSumVSA = new TableLabel("         Sum");
		final TableLabel lbSumMAT = new TableLabel("Sum");
		final TableLabel lbTot = new TableLabel("Total");
		GridPane.setConstraints(lbCSCD, 0, 0, 1, 1, HPos.LEFT, VPos.TOP);
		GridPane.setConstraints(lbMul, 1, 0, 1, 1, HPos.RIGHT, VPos.TOP);
		GridPane.setConstraints(lbAtt, 2, 0, 1, 1, HPos.RIGHT, VPos.TOP);
		GridPane.setConstraints(lbTik, 3, 0, 1, 1, HPos.RIGHT, VPos.TOP);
		GridPane.setConstraints(lbSumVSA, 4, 0, 1, 1, HPos.RIGHT, VPos.TOP);
		GridPane.setConstraints(lbVin, 0, 1, 1, 1, HPos.LEFT, VPos.TOP);
		GridPane.setConstraints(lbSut, 0, 2, 1, 1, HPos.LEFT, VPos.TOP);
		GridPane.setConstraints(lbAbh, 0, 3, 1, 1, HPos.LEFT, VPos.TOP);
		GridPane.setConstraints(lbSumMAT, 0, 4, 1, 1, HPos.LEFT, VPos.TOP);
		GridPane.setConstraints(lbAnn, 0, 5, 1, 1, HPos.LEFT, VPos.TOP);
		GridPane.setConstraints(lbTot, 0, 6, 1, 1, HPos.LEFT, VPos.TOP);
		resultGrid.getChildren().addAll(lbCSCD, lbVin, lbSut, lbAbh, lbMul, lbAtt, lbTik, lbAnn, lbSumVSA, lbSumMAT, lbTot);
		for(int i = 0; i < 3; i++) {
			mulLabels[i] = new TableLabel();
			GridPane.setConstraints(mulLabels[i], 1, 1+i, 1, 1, HPos.RIGHT, VPos.TOP);
			resultGrid.getChildren().add(mulLabels[i]);
		}
		for(int i = 0; i < 3; i++) {
			attLabels[i] = new TableLabel();
			GridPane.setConstraints(attLabels[i], 2, 1+i, 1, 1, HPos.RIGHT, VPos.TOP);
			resultGrid.getChildren().add(attLabels[i]);
		}
		for(int i = 0; i < 3; i++) {
			tikLabels[i] = new TableLabel();
			GridPane.setConstraints(tikLabels[i], 3, 1+i, 1, 1, HPos.RIGHT, VPos.TOP);
			resultGrid.getChildren().add(tikLabels[i]);
		}
		for(int i = 0; i < 3; i++) {
			vsaSumLabels[i] = new TableLabel();
			vsaSumLabels[i].setStyle(boldStyle);
			GridPane.setConstraints(vsaSumLabels[i], 4, 1+i, 1, 1, HPos.RIGHT, VPos.TOP);
			resultGrid.getChildren().add(vsaSumLabels[i]);
		}
		for(int i = 0; i < 3; i++) {
			matSumLabels[i] = new TableLabel();
			matSumLabels[i].setStyle(boldStyle);
			GridPane.setConstraints(matSumLabels[i], 1+i, 4, 1, 1, HPos.RIGHT, VPos.TOP);
			resultGrid.getChildren().add(matSumLabels[i]);
		}
		canSumLabel.setStyle(boldStyle);
		totSumLabel.setStyle(boldStyle);
		GridPane.setConstraints(canSumLabel, 4, 4, 1, 1, HPos.RIGHT, VPos.TOP);
		GridPane.setConstraints(annLabel, 4, 5, 1, 1, HPos.RIGHT, VPos.TOP);
		GridPane.setConstraints(totSumLabel, 4, 6, 1, 1, HPos.RIGHT, VPos.TOP);
		resultGrid.getChildren().addAll(canSumLabel, annLabel, totSumLabel);
		return resultGrid;
	}

	private void updateSummary() {
		final int[] values = sumByTermMenuItem.isSelected() ? cscdTerms : cscdDocs;
		for(int i = 0; i < 3; i++)
			mulLabels[i].setText(String.format("%,d", values[i]));
		for(int i = 0; i < 3; i++)
			attLabels[i].setText(String.format("%,d", values[i + 3]));
		for(int i = 0; i < 3; i++)
			tikLabels[i].setText(String.format("%,d", values[i + 6]));
		for(int i = 0; i < 3; i++) {
			final int vSum = sumByTermMenuItem.isSelected() ? vsaSumTerms[i] : values[i] + values[i + 3] + values[i + 6];
			vsaSumLabels[i].setText(String.format("%,d", vSum));
		}
		for(int i = 0; i < 3; i++) {
			final int hSum = sumByTermMenuItem.isSelected() ? matSumTerms[i] : values[3*i] + values[3*i + 1] + values[3*i + 2];
			matSumLabels[i].setText(String.format("%,d", hSum));
		}
		annLabel.setText(String.format("%,d", values[9]));
		final int canonSum;
		if(sumByTermMenuItem.isSelected()) {
			canonSum = canonSumTerms;
		} else {
			int canSum = 0;
			for(int i = 0; i < 9; i++)
				canSum += values[i];
			canonSum = canSum;
		}
		canSumLabel.setText(String.format("%,d", canonSum));
		final int totSum = sumByTermMenuItem.isSelected() ? totalSumTerms : canonSum + values[9];
		totSumLabel.setText(String.format("%,d", totSum));
	}

	private void sendTermToDict() {
		final SimpleTermFreqProp tf = table.getSelectionModel().getSelectedItem();
		if(tf != null)
			PaliPlatform.showDict(tf.termProperty().get());
	}

	private void sendTermToFinder() {
		final SimpleTermFreqProp tf = table.getSelectionModel().getSelectedItem();
		if(tf != null)
			PaliPlatform.showFinder(tf.termProperty().get());
	}

	private String makeCSV() {
		final String DELIM = ",";
		final StringBuilder result = new StringBuilder();
		// table columns
		for(int i=0; i<table.getColumns().size(); i++){
			result.append(table.getColumns().get(i).getText()).append(DELIM);
		}
		result.append(System.getProperty("line.separator"));
		// table data
		for(int i=0; i<table.getItems().size(); i++){
			final SimpleTermFreqProp tf = table.getItems().get(i);
			result.append(tf.termProperty().get()).append(DELIM);
			result.append(tf.freqProperty().get()).append(DELIM);
			result.append(tf.lengthProperty().get());
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}
	
	private void copyCSV() {
		Utilities.copyText(makeCSV());
	}
	
	private void saveCSV() {
		Utilities.saveText(makeCSV(), "simplelist.csv");
	}

	// inner classes
	public final class SimpleTermFreqProp {
		private StringProperty term;
		private IntegerProperty freq;
		private IntegerProperty length;

		public SimpleTermFreqProp(final String t, final int f) {
			termProperty().set(t);
			freqProperty().set(f);
			final int len = t.contains("-") ? 0 : Utilities.getPaliWordLength(t);
			lengthProperty().set(len);
		}

		public StringProperty termProperty() {
			if(term == null)
				term = new SimpleStringProperty(this, "term");
			return term;
		}

		public IntegerProperty freqProperty() {
			if(freq == null)
				freq = new SimpleIntegerProperty(this, "freq");
			return freq;
		}

		public IntegerProperty lengthProperty() {
			if(length == null)
				length = new SimpleIntegerProperty(this, "length");
			return length;
		}
	}

	private class TableLabel extends Label {
		private final String globalStyle = "-fx-font-family:'" + Utilities.FONTMONO + "';";

		public TableLabel() {
			super();
			setGlobalStyle();
		}

		public TableLabel(final String text) {
			super(text);
			setGlobalStyle();
		}

		private void setGlobalStyle() {
			setStyle(globalStyle);
		}
	}
}
