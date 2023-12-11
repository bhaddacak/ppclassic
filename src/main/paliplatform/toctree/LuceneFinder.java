/*
 * LuceneFinder.java
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
import java.util.regex.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.image.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

/** 
 * This window utilizes Apache Lucene as an alternative document finder. This is a singleton.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class LuceneFinder extends SingletonWindow {
	public static final LuceneFinder INSTANCE = new LuceneFinder();
	private final String indexRoot = Utilities.ROOTDIR + Utilities.INDEXPATH;
	private String indexPath = indexRoot + Utilities.INDEXMAIN;
	private final BorderPane mainPane = new BorderPane();
	private final BorderPane contentPane = new BorderPane();
	private final PaliTextInput textInput = new PaliTextInput(PaliTextInput.InputType.COMBO);
	private final ComboBox<String> searchComboBox;
	private final TextField searchTextField;
	private final CSCDFieldSelectorBox fieldOptionsBox;
	private final VBox searchResultBox = new VBox();
	private final ContextMenu searchResultPopupMenu = new ContextMenu();
	private final AnchorPane statusPane = new AnchorPane();
	private final HBox progressBox = new HBox(3);
	private final ProgressBar progressBar = new ProgressBar();
	private final Label progressMessage = new Label();
	private final InfoPopup mainHelpPopup = new InfoPopup();
	private final InfoPopup searchHelpPopup = new InfoPopup();
	private final ChoiceBox<Integer> maxResultChoice = new ChoiceBox<>();
	private final CheckMenuItem keepCapMenuItem = new CheckMenuItem("Keep capitalized terms");
	private final CheckMenuItem includeNumberMenuItem = new CheckMenuItem("Include numbers");
	private final CheckMenuItem includeBoldMenuItem = new CheckMenuItem("Include field 'bold'");
	private	final RadioMenuItem oneCharMenuItem = new RadioMenuItem("== 1 char long");
	private final ToggleGroup lengthExclusionGroup = new ToggleGroup();
	private final CheckMenuItem useStopwordsMenuItem = new CheckMenuItem("Use stopwords");
	private final RadioMenuItem allGroupsMenuItem = new RadioMenuItem("Whole collection (CSCD)");
	private final RadioMenuItem allTipitakaMenuItem = new RadioMenuItem("Whole Tipiṭaka (no Añña)");
	private final RadioMenuItem onlyVinMenuItem = new RadioMenuItem("Only Vinaya (M + A + Ṭ)");
	private final RadioMenuItem onlySutMenuItem = new RadioMenuItem("Only Suttanta (M + A + Ṭ)");
	private final RadioMenuItem onlyAbhMenuItem = new RadioMenuItem("Only Abhidhamma (M + A + Ṭ)");
	private final RadioMenuItem onlyMulMenuItem = new RadioMenuItem("Only Mūla (V + S + A)");
	private final RadioMenuItem onlyAttMenuItem = new RadioMenuItem("Only Aṭṭhakathā (V + S + A)");
	private final RadioMenuItem onlyTikMenuItem = new RadioMenuItem("Only Ṭīkā (V + S + A)");
	private final String ONLY_ONE = "Only individual group";
	private final RadioMenuItem onlyOneMenuItem = new RadioMenuItem(ONLY_ONE);
	private final RadioMenuItem onlyAnnMenuItem = new RadioMenuItem("Only Añña");
	private final ToggleGroup textGroupGroup = new ToggleGroup();
	private final CheckMenuItem showWholeLineMenuItem = new CheckMenuItem("Show whole lines");
	private final SimpleBooleanProperty indexAvailable = new SimpleBooleanProperty(false);
	private final ToggleButton showSearchDetailButton = new ToggleButton("", new TextIcon("glasses", TextIcon.IconSet.AWESOME));
	private TOCTreeNode currSelectedDoc = null;
	private String individualTextGroupFilter = "";
	
	private LuceneFinder() {
		getIcons().add(new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "lucene.png")));
		windowWidth = Utilities.getRelativeSize(60);
		
		// prepare field selector
		fieldOptionsBox = new CSCDFieldSelectorBox(() -> search());
		fieldOptionsBox.getSimpleBoldCheckBox().disableProperty().bind(includeBoldMenuItem.selectedProperty().not());
		fieldOptionsBox.getFullBoldCheckBox().disableProperty().bind(includeBoldMenuItem.selectedProperty().not());

		// add toolbar on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(searchResultBox);
		// config some buttons
		toolBar.saveTextButton.setOnAction(actionEvent -> saveText());		
		toolBar.copyButton.setOnAction(actionEvent -> copyText());		
		// add new components
		final Button selectIndexDirButton = new Button("", new TextIcon("folder-open", TextIcon.IconSet.AWESOME));
		selectIndexDirButton.setTooltip(new Tooltip("Select index directory"));
		selectIndexDirButton.setOnAction(actionEvent -> chooseIndexPath(false));
		final Button buildIndexButton = new Button("Build", new TextIcon("screwdriver-wrench", TextIcon.IconSet.AWESOME));
		buildIndexButton.setTooltip(new Tooltip("Build/rebuild Lucene index"));
		buildIndexButton.setOnAction(actionEvent -> buildIndex());
		final MenuButton mainOptionsMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		mainOptionsMenu.setTooltip(new Tooltip("Options for indexing"));
		onlyOneMenuItem.setOnAction(actionEvent -> selectIndividualTextGroup());
		final Menu textGroupMenu = new Menu("Text group");
		textGroupMenu.getItems().addAll(allGroupsMenuItem, allTipitakaMenuItem, onlyVinMenuItem, onlySutMenuItem, onlyAbhMenuItem,
										onlyMulMenuItem, onlyAttMenuItem, onlyTikMenuItem, onlyOneMenuItem, onlyAnnMenuItem);
		textGroupGroup.getToggles().addAll(allGroupsMenuItem, allTipitakaMenuItem, onlyVinMenuItem, onlySutMenuItem, onlyAbhMenuItem,
										onlyMulMenuItem, onlyAttMenuItem, onlyTikMenuItem, onlyOneMenuItem, onlyAnnMenuItem);
		textGroupGroup.selectToggle(allGroupsMenuItem);
		keepCapMenuItem.setSelected(false);
		includeBoldMenuItem.setSelected(false);
		includeBoldMenuItem.setOnAction(actionEvent -> {
			if(!includeBoldMenuItem.isSelected()) {
				fieldOptionsBox.getSimpleBoldCheckBox().setSelected(false);
				fieldOptionsBox.getFullBoldCheckBox().setSelected(false);
			}
		});
		final Menu lengthExcludeMenu = new Menu("Length exclusion");
		final RadioMenuItem noExcMenuItem = new RadioMenuItem("No exclusion");
		final RadioMenuItem twoCharMenuItem = new RadioMenuItem("<= 2 chars long");
		final RadioMenuItem threeCharMenuItem = new RadioMenuItem("<= 3 chars long");
		lengthExcludeMenu.getItems().addAll(noExcMenuItem, oneCharMenuItem, twoCharMenuItem, threeCharMenuItem);
		lengthExclusionGroup.getToggles().addAll(noExcMenuItem, oneCharMenuItem, twoCharMenuItem, threeCharMenuItem);
		lengthExclusionGroup.selectToggle(oneCharMenuItem);
		final MenuItem editStopwordsMenuItem = new MenuItem("Edit stopwords");
		editStopwordsMenuItem.setOnAction(actionEvent -> editStopwords());
		final MenuItem setToDefaultMenuItem = new MenuItem("Set to defaults");
		setToDefaultMenuItem.setOnAction(actionEvent -> setDefaultIndexOptions());
		mainOptionsMenu.getItems().addAll(textGroupMenu, keepCapMenuItem, includeNumberMenuItem, includeBoldMenuItem,
										lengthExcludeMenu, useStopwordsMenuItem, 
										new SeparatorMenuItem(), editStopwordsMenuItem, setToDefaultMenuItem);
		final Button mainHelpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		mainHelpButton.setOnAction(actionEvent -> mainHelpPopup.showPopup(mainHelpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		toolBar.getItems().addAll(new Separator(), selectIndexDirButton, buildIndexButton, mainOptionsMenu, mainHelpButton);
		mainPane.setTop(toolBar);

		// add main content
		// add search toolbar
		final ToolBar searchToolBar = new ToolBar();
		searchComboBox = textInput.getComboBox();
		searchComboBox.setPromptText("Search for...");
		searchComboBox.setPrefWidth(Utilities.getRelativeSize(24));
		searchComboBox.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				if(keyEvent.getCode() == KeyCode.ENTER)
					search();
				else if(keyEvent.getCode() == KeyCode.ESCAPE)
					clearSearch();
			}
		});
		searchTextField = (TextField)textInput.getInput();
		final Button searchButton = new Button("Search");
		searchButton.disableProperty().bind(indexAvailable.not());
		searchButton.setOnAction(actionEvent -> search());
		final List<Integer> maxList = Arrays.asList(10, 20, 30, 50, 100);
		maxResultChoice.setTooltip(new Tooltip("Maximum results"));
		maxResultChoice.getItems().addAll(maxList);
		maxResultChoice.getSelectionModel().select(0);
		maxResultChoice.setOnAction(actionEvent -> search());
		final Button fieldSelButton = new Button("", new TextIcon("list-check", TextIcon.IconSet.AWESOME));
		fieldSelButton.setTooltip(new Tooltip("Field selector on/off"));
		fieldSelButton.setOnAction(actionEvent -> openFieldSelector());
		showSearchDetailButton.setTooltip(new Tooltip("Show text fragments"));
		showSearchDetailButton.setOnAction(actionEvent -> search());
		final Button foldUpAllButton = new Button("", new TextIcon("angles-up", TextIcon.IconSet.AWESOME));
		foldUpAllButton.setTooltip(new Tooltip("Collapse all"));
		foldUpAllButton.disableProperty().bind(showSearchDetailButton.selectedProperty().not());
		foldUpAllButton.setOnAction(actionEvent -> foldSearchResult(false));
		final Button foldDownAllButton = new Button("", new TextIcon("angles-down", TextIcon.IconSet.AWESOME));
		foldDownAllButton.setTooltip(new Tooltip("Expand all"));
		foldDownAllButton.disableProperty().bind(showSearchDetailButton.selectedProperty().not());
		foldDownAllButton.setOnAction(actionEvent -> foldSearchResult(true));
		final MenuButton searchOptionsMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		searchOptionsMenu.setTooltip(new Tooltip("Options for search results"));
		showWholeLineMenuItem.setOnAction(actionEvent -> search());
		searchOptionsMenu.getItems().addAll(showWholeLineMenuItem);
		final Button searchHelpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		searchHelpButton.setOnAction(actionEvent -> searchHelpPopup.showPopup(searchHelpButton, InfoPopup.Pos.BELOW_RIGHT, true));
		searchToolBar.getItems().addAll(searchComboBox, textInput.getClearButton(), textInput.getMethodButton(),
									searchButton, maxResultChoice, fieldSelButton, showSearchDetailButton, 
									foldUpAllButton, foldDownAllButton, searchOptionsMenu, searchHelpButton);
		contentPane.setTop(searchToolBar);

		// add search result box at the center
		searchResultBox.prefWidthProperty().bind(contentPane.widthProperty().subtract(16));
		final ScrollPane searchResultPane = new ScrollPane(searchResultBox);
		contentPane.setCenter(searchResultPane);
		
		mainPane.setCenter(contentPane);

		// add status pane at the bottom
		mainPane.setBottom(statusPane);

		final Scene scene = new Scene(mainPane, windowWidth, windowHeight);
		setScene(scene);

		// set drop action
		mainPane.setOnDragOver(dragEvent -> {
			if(dragEvent.getGestureSource() != mainPane && dragEvent.getDragboard().hasString()) {
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			dragEvent.consume();
		});
		mainPane.setOnDragDropped(dragEvent -> {
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

		// init
		AnchorPane.setBottomAnchor(progressBox, 0.0);
		AnchorPane.setLeftAnchor(progressBox, 0.0);
		progressBox.getChildren().addAll(progressBar, progressMessage);
		mainHelpPopup.setContent("info-lucene.txt");
		mainHelpPopup.setTextWidth(Utilities.getRelativeSize(32));
		searchHelpPopup.setContent("info-lucene-search.txt");
		searchHelpPopup.setTextWidth(Utilities.getRelativeSize(32));
		// prepare search result context menu
		final MenuItem openDocMenuItem = new MenuItem("Open");
		openDocMenuItem.setOnAction(actionEvent -> openCurrentDoc());
		final MenuItem openDocAsTextMenuItem = new MenuItem("Open as text");
		openDocAsTextMenuItem.setOnAction(actionEvent -> Utilities.openXMLDocAsText(currSelectedDoc, true));
		final MenuItem openDocAsTextNoNotesMenuItem = new MenuItem("Open as text (no notes)");
		openDocAsTextNoNotesMenuItem.setOnAction(actionEvent -> Utilities.openXMLDocAsText(currSelectedDoc, false));
		searchResultPopupMenu.getItems().addAll(openDocMenuItem, openDocAsTextMenuItem, openDocAsTextNoNotesMenuItem);
		// check index availibity
		indexAvailable.set(checkIndexAvailable(indexPath));
		updateIndexInfo();
		// prepare doc info
		if(Utilities.docInfoMap.isEmpty())
			Utilities.loadDocInfo();
		if(Utilities.stopwords.isEmpty())
			Utilities.loadStopwords();
	}

	private void selectIndividualTextGroup() {
		final List<String> groupList = Arrays.asList("Vinaya, Mūla", "Vinaya, Aṭṭhakathā", "Vinaya, Ṭīkā",
													"Suttanta, Mūla", "Suttanta, Aṭṭhakathā", "Suttanta, Ṭīkā",
													"Abhidhamma, Mūla", "Abhidhamma, Aṭṭhakathā", "Abhidhamma, Ṭīkā");
		final List<String> pattList = Arrays.asList("v....m.*", "v....a.*", "v....t.*",
													"s....m.*", "s....a.*", "s....t.*",
													"a....m.*", "a....a.*", "a....t.*");
		final ChoiceDialog<String> dialog = new ChoiceDialog<>(groupList.get(0), groupList);
		dialog.initOwner(this);
		dialog.setTitle("Select an individual text group");
		dialog.setHeaderText(null);
		dialog.setContentText("Text group:");
		final Optional<String> result = dialog.showAndWait();
		if(result.isPresent()) {
			final String gname = result.get();
			final int ind = groupList.indexOf(gname);
			final String[] tmps = gname.split(", ");
			final String abbr = "" + tmps[0].charAt(0) + tmps[1].charAt(0);
			individualTextGroupFilter = pattList.get(ind);
			onlyOneMenuItem.setText(ONLY_ONE + " (" + abbr + ")");
		} else {
			textGroupGroup.selectToggle(allGroupsMenuItem);
			onlyOneMenuItem.setText(ONLY_ONE);
		}
	}

	private void buildIndex() {
		final boolean confirm = chooseIndexPath(true);
		if(!confirm) return;
		final String fileFilter;
		final Toggle toggle = textGroupGroup.getSelectedToggle();
		if(toggle == onlyVinMenuItem)
			fileFilter = "v.*";
		else if(toggle == onlySutMenuItem)
			fileFilter = "s.*";
		else if(toggle == onlyAbhMenuItem)
			fileFilter = "a.*";
		else if(toggle == onlyAnnMenuItem)
			fileFilter = "e.*";
		else if(toggle == onlyMulMenuItem)
			fileFilter = "[vsa]....m.*";
		else if(toggle == onlyAttMenuItem)
			fileFilter = "[vsa]....a.*";
		else if(toggle == onlyTikMenuItem)
			fileFilter = "[vsa]....t.*";
		else if(toggle == allTipitakaMenuItem)
			fileFilter = "[^e].*";
		else if(toggle == onlyOneMenuItem)
			fileFilter = individualTextGroupFilter;
		else
			fileFilter = ".*";
		if(fileFilter.isEmpty()) return;
		clearIndexDir();
		progressBar.setProgress(0);
		final Task<Boolean> buildTask = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				updateMessage("Building index (please wait)");
				try{
					final Analyzer analyzer = new PaliIndexAnalyzer();
					final Directory directory = FSDirectory.open(Path.of(indexPath));
					final IndexWriterConfig config = new IndexWriterConfig(analyzer);
					final IndexWriter iwriter = new IndexWriter(directory, config);
					final SAXParserFactory spf = SAXParserFactory.newInstance();
					final SAXParser saxParser = spf.newSAXParser();
					final ZipFile zip = new ZipFile(new File(Utilities.ROOTDIR + Utilities.COLLPATH + Utilities.CSCD_ZIP));
					final int total = zip.size();
					int n = 0;
					for(final Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
						updateProgress(++n, total);
						final ZipEntry entry = e.nextElement();
						final String[] strName = entry.getName().split("/");
						final String fname = strName[strName.length - 1];
						if(!fname.contains("toc")) {
							if(!fname.matches(fileFilter)) continue;
							final Map<CSCDTermInfo.Field, StringBuilder> textMap = new EnumMap<>(CSCDTermInfo.Field.class);
							for(final CSCDTermInfo.Field fld : CSCDTermInfo.Field.values())
								textMap.put(fld, new StringBuilder());
							final DefaultHandler handler = new CSCDTermInfoSAXHandler(textMap);
							saxParser.parse(zip.getInputStream(entry), handler);
							final Document doc = new Document();
							doc.add(new StringField("path", fname, Field.Store.YES));
							textMap.forEach((f, sb) -> {
								boolean doAdd = true;
								if(!includeBoldMenuItem.isSelected() && f == CSCDTermInfo.Field.BOLD)
									doAdd = false;
								if(doAdd) {
									final String text = keepCapMenuItem.isSelected() ? sb.toString() : sb.toString().toLowerCase();
									doc.add(new org.apache.lucene.document.TextField(f.getTag(), tokenize(text), Field.Store.NO));
								}
							});
							iwriter.addDocument(doc);
						}
					}
					iwriter.close();
					zip.close();
				} catch(SAXException | ParserConfigurationException | IOException e) {
					System.err.println(e);
				}
				Platform.runLater(() -> {
					progressBar.progressProperty().unbind();
					statusPane.getChildren().remove(progressBox);
					indexAvailable.set(checkIndexAvailable(indexPath));
					updateIndexInfo();
				});
				return true;
			}
		};
		progressBar.progressProperty().bind(buildTask.progressProperty());
		buildTask.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			progressMessage.setText(newValue);
		});
		PaliPlatform.threadPool.submit(buildTask);
		statusPane.getChildren().add(progressBox);
	}

	private boolean chooseIndexPath(final boolean toWrite) {
		boolean success = true;
		final File indexDir = Utilities.selectDirectory(indexPath, this, "Select an index directory");
		if(indexDir != null) {
			final boolean existed = checkIndexAvailable(indexDir.getPath());
			if(toWrite && existed)
				success = proceedBuildConfirm();
			if(success) {
				indexPath = indexDir.getPath() + File.separator;
				indexAvailable.set(existed);
				updateIndexInfo();
			}
		} else {
			success = false;
		}
		return success;
	}

	private boolean checkIndexAvailable(final String strDir) {
		boolean result = false;
		final File indexDir = new File(strDir);
		final String[] files = indexDir.list((f, s) -> { return s.startsWith("segments"); });
		if(files.length > 0)
			result = true;
		return result;
	}

	private void clearIndexDir() {
		final File indexDir = new File(indexPath);
		for(final File f : indexDir.listFiles()) {
			f.delete();
		}
	}

	private void updateIndexInfo() {
		int docCount = 0;
		if(indexAvailable.get()) {
			try {
				final Directory directory = FSDirectory.open(Path.of(indexPath));
				final DirectoryReader ireader = DirectoryReader.open(directory);
				if(ireader != null) {
					docCount = ireader.numDocs();
					ireader.close();
				}
				directory.close();
			} catch(IOException e) {
				System.err.println(e);
			}
		}
		final String info = " [" + Utilities.getLastPathPart(indexPath) + ": " + docCount + "]";
		setTitle("Lucene Finder" + info);
	}

	private String tokenize(final String text) {
		final String[] tokens = text.split(Utilities.REX_NON_PALI_NUM);
		final RadioMenuItem widExcRadio = (RadioMenuItem)lengthExclusionGroup.getSelectedToggle();
		final Predicate<String> widExcCond;
		if(widExcRadio.getText().contains("1"))
			widExcCond = x -> Utilities.getPaliWordLength(x) == 1; 
		else if(widExcRadio.getText().contains("2"))
			widExcCond = x -> Utilities.getPaliWordLength(x) <= 2; 
		else if(widExcRadio.getText().contains("3"))
			widExcCond = x -> Utilities.getPaliWordLength(x) <= 3; 
		else widExcCond = x -> false;
		final Predicate<String> inclNumCond;
		if(!includeNumberMenuItem.isSelected())
			inclNumCond = x -> x.matches("^\\d+");
		else
			inclNumCond = x -> false;
		final Predicate<String> stopwrdCond;
		if(useStopwordsMenuItem.isSelected())
			stopwrdCond = x -> Utilities.stopwords.contains(x);
		else
			stopwrdCond = x -> false;
		final String result = Arrays.asList(tokens).stream()
										.filter(Predicate.not(stopwrdCond))
										.filter(Predicate.not(inclNumCond))
										.filter(Predicate.not(widExcCond))
										.collect(Collectors.joining(" "));
		return result;
	}

	private void setDefaultIndexOptions() {
		textGroupGroup.selectToggle(allGroupsMenuItem);
		keepCapMenuItem.setSelected(false);
		includeNumberMenuItem.setSelected(false);
		includeBoldMenuItem.setSelected(false);
		lengthExclusionGroup.selectToggle(oneCharMenuItem);
		useStopwordsMenuItem.setSelected(false);
	}

	private void editStopwords() {
		PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, new File[] { Utilities.stopwordsFile });
	}

	private void clearSearch() {
		searchTextField.clear();
	}

	private void search() {
		if(!indexAvailable.get()) return;
		final String strQuery = Normalizer.normalize(searchTextField.getText().trim(), Form.NFC);
		if(strQuery.isEmpty()) return;
		searchComboBox.commitValue();
		final int maxCount = maxResultChoice.getSelectionModel().getSelectedItem();
		try {
			final Analyzer analyzer = new PaliIndexAnalyzer();
			final Directory directory = FSDirectory.open(Path.of(indexPath));
			final DirectoryReader ireader = DirectoryReader.open(directory);
			final IndexSearcher isearcher = new IndexSearcher(ireader);
			final Map<CSCDTermInfo.Field, ScoreDoc[]> scoreDocMap = new EnumMap<>(CSCDTermInfo.Field.class);
			for(final CSCDTermInfo.Field f : CSCDTermInfo.Field.values()) {
				if(fieldOptionsBox.isFieldSelected(f)) {
					final QueryParser parser = new QueryParser(f.getTag(), analyzer);
					parser.setDefaultOperator(QueryParser.Operator.AND);
					final Query query = parser.parse(strQuery);
					scoreDocMap.put(f, isearcher.search(query, maxCount).scoreDocs);
				}
			}
			final List<SearchOutput> outputList = new ArrayList<>();
			for(final CSCDTermInfo.Field f : scoreDocMap.keySet()) {
				final ScoreDoc[] docs = scoreDocMap.get(f);
				final Set<Integer> idSet = new HashSet<>();
				for(final ScoreDoc sd : docs) {
					if(!idSet.contains(sd.doc)) {
						idSet.add(sd.doc);
						outputList.add(new SearchOutput(f, sd));
					}
				}
			}
			outputList.sort((x, y) -> Float.compare(y.getScore(), x.getScore()));
			updateSearchResult(outputList, ireader, strQuery);
			ireader.close();
			directory.close();
		} catch(ParseException | IOException e) {
			System.err.println(e);
		}
	}

	private void updateSearchResult(final List<SearchOutput> outputList, final DirectoryReader ireader, final String strQuery) {
		searchResultBox.getChildren().clear();
		if(!outputList.isEmpty())
			textInput.recordQuery();
		final int maxCount = maxResultChoice.getSelectionModel().getSelectedItem();
		final Map<Integer, Map<CSCDTermInfo.Field, StringBuilder>> resultTextMap = new HashMap<>();
		try {
			if(showSearchDetailButton.isSelected()) {
				// prepare text of each result
				final SAXParserFactory spf = SAXParserFactory.newInstance();
				final SAXParser saxParser = spf.newSAXParser();
				final ZipFile zip = new ZipFile(new File(Utilities.ROOTDIR + Utilities.COLLPATH + Utilities.CSCD_ZIP));
				for(final SearchOutput so : outputList) {
					final int docID = so.getDocID();
					if(!resultTextMap.containsKey(docID)) {
						final Map<CSCDTermInfo.Field, StringBuilder> textMap = new EnumMap<>(CSCDTermInfo.Field.class);
						for(final CSCDTermInfo.Field fld : CSCDTermInfo.Field.values())
							textMap.put(fld, new StringBuilder());
						final DefaultHandler handler = new CSCDTermInfoSAXHandler(textMap);
						final String filename = ireader.document(docID).get("path");
						final ZipEntry entry = zip.getEntry(Utilities.CSCD_DIR + filename);
						if(entry != null)
							saxParser.parse(zip.getInputStream(entry), handler);
						resultTextMap.put(docID, textMap);
					}
				}
			}
			final Analyzer analyzer = new PaliIndexAnalyzer();
			for(int i = 0; i < outputList.size(); i++) {       
				if(i >= maxCount) break;
				final SearchOutput soutput = outputList.get(i);
				final String filename = ireader.document(soutput.getDocID()).get("path");
				final String filecode = filename.substring(0, filename.lastIndexOf(".xml"));
				final DocInfo docInfo = Utilities.docInfoMap.get(filecode);
				if(docInfo == null) continue;
				final String docInfoStr = docInfo.getFullTitleComma() + 
										String.format(" [Score: %.4f] (%s)", soutput.getScore(), soutput.getField().getTag());
				final TitledPane tpane;
				if(showSearchDetailButton.isSelected()) {
					final StringBuilder resultText = new StringBuilder();
					final String text = resultTextMap.get(soutput.getDocID()).get(soutput.getField()).toString()
													.replaceAll(" {2,}", " ").replace(" .", ".").replace(" ,", ",").trim();
					if(showWholeLineMenuItem.isSelected()) {
						// use custom fragmenter
						resultText.append(getFragmentManually(text, strQuery, true));
					} else {
						// use Lucene fragmenter
						final QueryParser parser = new QueryParser(soutput.getField().getTag(), analyzer);
						parser.setDefaultOperator(QueryParser.Operator.AND);
						final Query query = parser.parse(strQuery);
						final SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("{", "}");
						final Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
						final Fragmenter fragmenter = new SimpleFragmenter(100);
						highlighter.setTextFragmenter(fragmenter);
						final TokenStream tokenStream = analyzer.tokenStream(soutput.getField().getTag(), text);
						final String[] frags = highlighter.getBestFragments(tokenStream, text, 10);
						if(frags.length > 0) {
							for(int j = 0; j < frags.length; j++) {
								final String[] tmps = frags[j].split("\n");
								for(final String s : tmps) {
									if(s.contains("{")) {
										resultText.append("» ").append(s).append("...\n");
									}
								}
							}
						} else {
							// highlighter fails, use custom fragmenter instead
							resultText.append(getFragmentManually(text, strQuery, false));
						}
					}
					tpane = new TitledPane(docInfoStr, createTextFlow(resultText.toString()));
					tpane.setExpanded(true);
				} else {
					tpane = new TitledPane(docInfoStr, null); 
					tpane.setCollapsible(false);
				}
				final TOCTreeNode ttn = new TOCTreeNode(docInfo.getTitle(0), filename, true, false, true);
				tpane.setUserData(ttn);
				tpane.setContextMenu(searchResultPopupMenu);
				tpane.setOnContextMenuRequested(cmevent -> {
					final TitledPane tp = (TitledPane)cmevent.getSource();
					currSelectedDoc = (TOCTreeNode)tp.getUserData();
				});
				searchResultBox.getChildren().add(tpane);
			}     
		} catch(SAXException | ParserConfigurationException | ParseException | InvalidTokenOffsetsException | IOException e) {
			System.err.println(e);
		}
	}

	private String getFragmentManually(final String text, final String strQuery, final boolean isWholeLine) {
		// (1) generate query word list (those need highlight)
		final List<String> wlist = new ArrayList<>();
		if(strQuery.charAt(0) == '/' && strQuery.charAt(strQuery.length()-1) == '/') {
			// it is regex query, take it as a whole
			final String rxStr = strQuery.substring(1, strQuery.length()-1);
			wlist.add(rxStr);
		} else {
			String strProcessed;
			strProcessed = strQuery.replaceAll("[&+|/!^~(){}\\[\\]\\-\\\\]", " "); // strip off symbols
			// break query string into words
			if(strProcessed.indexOf('?') >= 0) {
				// replace '?' with a non-whitespace
				strProcessed = strProcessed.replace("?", "\\\\S"); 
			}
			if(strProcessed.indexOf('*') >= 0) {
				// replace '*' with non-whitespace
				strProcessed = strProcessed.replace("*", "\\\\S*");
			}
			final int qInd = strProcessed.indexOf('"');
			if(qInd >= 0) {
				// if '"' is used, take the whole word
				String qStr;
				if(strProcessed.charAt(strProcessed.length()-1) == '"') {
					int lastInd = strProcessed.lastIndexOf('"');
					qStr = strProcessed.substring(qInd+1, lastInd);
				} else {
					qStr = strProcessed.substring(qInd+1);
					// in case of a mixed-up, exclude text after "
					final int qqInd = qStr.indexOf('"');
					if(qqInd >= 0)
						qStr = qStr.substring(0, qqInd).trim();
					
				}
				wlist.add(qStr);
			} else {
				for(final String qs : strProcessed.split("\\s+")) {
					String q = qs;
					final int clInd = qs.indexOf(":"); // field selector, exclude it
					if(clInd >= 0)
						q = qs.substring(clInd + 1);
					if(q.isEmpty()) continue;
					if(q.endsWith("AND") || q.endsWith("OR") || q.endsWith("NOT"))
						continue;
					wlist.add(q);
					if(!keepCapMenuItem.isSelected() && !Character.isUpperCase(q.charAt(0)))
						wlist.add(Character.toUpperCase(q.charAt(0)) + q.substring(1));
				} // end for
			} // end if
		} // end if
		// (2) find the words' position line by line, then generate the string output
		final StringBuilder result = new StringBuilder();
		for(final String line : text.split("\n")) {
			final Map<Integer, String> fragPosMap = new HashMap<>();
			final String wb = "\\b";
			for(final String s : wlist) {
				final Pattern patt = Pattern.compile(wb + s);
				final Matcher matcher = patt.matcher(line);
				while(matcher.find())
					fragPosMap.put(matcher.start(), matcher.group());
			}
			if(!fragPosMap.isEmpty()) {
				final List<Integer> posList = new ArrayList<>(fragPosMap.keySet());
				posList.sort(Integer::compare);
				if(isWholeLine) {
					result.append("» ");
					int ind = 0;
					for(final Integer i : posList) {
						final String before = line.substring(ind, i);
						result.append(before).append("{");
						final String word = fragPosMap.get(i);
						result.append(word).append("}");
						ind = ind + before.length() + word.length();
					}
					result.append(line.substring(ind, line.length())).append("\n");
				} else {
					final int window = 45;
					for(int i = 0; i < posList.size(); i++) {
						int ind = 0;
						int pos = posList.get(i);
						result.append("» ");
						if(pos - ind > window) {
							ind = pos - window;
							result.append("...");
						}
						final String before = line.substring(ind, pos);
						result.append(before).append("{");
						final String word = fragPosMap.get(pos);
						result.append(word).append("}");
						ind = ind + before.length() + word.length();
						int nextPos;
							nextPos = line.length();
						String elip = "";
						if(nextPos - ind > window) {
							nextPos = ind + window;
							elip = "...";
						}
						result.append(line.substring(ind, nextPos)).append(elip).append("\n");
					}
				}
			}
		}
		return result.toString();
	}

	private TextFlow createTextFlow(final String text) {
		final List<Text> tlist = new ArrayList<>();
		final String[] lines = text.split("\n");
		int count = 0;
		for(final String line : lines) {
			final String[] leftB = line.split("\\{");
			for(final String part : leftB) {
				if(part.contains("}")) {
					final String[] rightB = part.split("\\}");
					final Text ht = new Text(rightB[0]);
					ht.getStyleClass().add("search-result-highlight");
					tlist.add(ht);
					if(rightB.length > 1) {
						final Text nt = new Text(rightB[1]);
						nt.getStyleClass().add("search-result-normal");
						tlist.add(nt);
					}
				} else {
					final Text txt = new Text(part);
					txt.getStyleClass().add("search-result-normal");
					tlist.add(txt);
				}
			}
			tlist.add(new Text("\n"));
			count++;
		}
		final TextFlow tflow = new TextFlow();
		tflow.getChildren().addAll(tlist);
		return tflow;
	}

	private void foldSearchResult(final boolean doExpand) {
		if(showSearchDetailButton.isSelected()) {
			for(final Node n : searchResultBox.getChildren()) {
				final TitledPane tp = (TitledPane)n;
				tp.setExpanded(doExpand);
			}
		}
	}

	private void openFieldSelector() {
		if(contentPane.getRight() == null) {
			contentPane.setRight(fieldOptionsBox);
		} else {
			contentPane.setRight(null);
		}
	}

	private void openCurrentDoc() {
		if(currSelectedDoc != null)
			PaliPlatform.openPaliHtmlViewer(currSelectedDoc);
	}

	private void addTermToSearch(final String term) {
		final String existing = searchTextField.getText();
		final String space = existing.isEmpty() ? "" : " ";
		searchTextField.setText(existing + space + term);
	}

	private boolean proceedBuildConfirm() {
		boolean output = false;
		final String message = "The existing index will be replaced, \nproceed to continue.";
		final ConfirmAlert proceedAlert = new ConfirmAlert(this, ConfirmAlert.ConfirmType.PROCEED, message);
		final Optional<ButtonType> result = proceedAlert.showAndWait();
		if(result.isPresent()) {
			if(result.get() == proceedAlert.getConfirmButtonType())
				output = true;
		}
		return output;		
	}

	private String makeText() {
		final StringBuilder result = new StringBuilder();
		result.append("Index path: ").append(indexPath);
		result.append(System.getProperty("line.separator"));
		result.append("Query: ").append(Normalizer.normalize(searchTextField.getText().trim(), Form.NFC));
		result.append(System.getProperty("line.separator"));
		result.append("Results: ").append(System.getProperty("line.separator"));
		for(final Node n : searchResultBox.getChildren()) {
			final TitledPane tp = (TitledPane)n;
			result.append(tp.getText());
			result.append(System.getProperty("line.separator"));
			final Node content = tp.getContent();
			if(content != null) {
				final TextFlow tf = (TextFlow)content;
				for(final Node tn : tf.getChildren()) {
					final Text txt = (Text)tn;
					result.append(txt.getText());
				}
				result.append(System.getProperty("line.separator"));
			}
		}
		return result.toString();
	}
	
	private void copyText() {
		Utilities.copyText(makeText());
	}
	
	private void saveText() {
		Utilities.saveText(makeText(), "luceneout.txt");
	}

	// inner classes
	private class SearchOutput {
		private final CSCDTermInfo.Field field;
		private final int doc;
		private final float score;

		private SearchOutput(final CSCDTermInfo.Field field, final ScoreDoc sdoc) {
			this.field = field;
			doc = sdoc.doc;
			score = sdoc.score;
		}

		private CSCDTermInfo.Field getField() {
			return field;
		}

		private int getDocID() {
			return doc;
		}

		private float getScore() {
			return score;
		}
	}

	public class PaliIndexAnalyzer extends Analyzer {

	public PaliIndexAnalyzer () {
	}

	@Override
	public TokenStreamComponents createComponents(final String fieldName) {
		return new TokenStreamComponents(new PaliTokenizer());
	}

	private class PaliTokenizer extends CharTokenizer {
		public PaliTokenizer() {
		}
		@Override
		protected boolean isTokenChar(final int c) {
			boolean isToken = false;
			isToken |= Character.isDigit(c);
			isToken |= Character.isLetter(c);
			isToken |= Utilities.PALI_ALL_CHARS.indexOf(c) != -1;
			return isToken;
		}
	}
}
}
