/*
 * PaliTextEditor.java
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

package paliplatform;

import paliplatform.viewer.PaliTextReader;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.event.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.util.regex.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import javax.print.*;
import javax.print.attribute.*;

/** 
 * A general text editor for Pali.
 * @author J.R. Bhaddacak
 * @version 2.1
 * @since 2.0
 */
public class PaliTextEditor extends BorderPane {
	private static final String DEFAULT_FILENAME = "untitled.txt";
	private final PaliTextInput textInput;
	private final TextArea area;
	private String fileName = DEFAULT_FILENAME;
	private Stage theStage;
	private final SimpleObjectProperty<File> theFile = new SimpleObjectProperty<>(null);
	private final SimpleBooleanProperty saveable = new SimpleBooleanProperty(false);
	private final CommonWorkingToolBar toolBar;
	private final CheckMenuItem saveOnCloseMenuItem = new CheckMenuItem("Autosave on close");
	final MenuItem findNextMenuItem = new MenuItem("Find _Next");
	final MenuItem findPrevMenuItem = new MenuItem("Find Pre_v");	
	private final FindReplaceBox findReplaceBox = new FindReplaceBox(this);
	private final TextField findInput = findReplaceBox.getFindTextField();
	private final TextField replaceInput = findReplaceBox.getReplaceTextField();
	private final ComboBox<String> findInputCombo = findReplaceBox.getFindComboBox();
	private final ComboBox<String> replaceInputCombo = findReplaceBox.getReplaceComboBox();
	private final List<int[]> regexFindResult = new ArrayList<>();
	private boolean caseSensitive = false;
	private boolean wholeWord = false;
	private boolean regexSearch = false;
	private boolean regexMode = false;
	private SimpleBooleanProperty searchTextFound = new SimpleBooleanProperty(false);
	private int currFoundIndex = 0;
	private SimpleObjectProperty<Utilities.PaliScript> currScript = new SimpleObjectProperty<>(Utilities.PaliScript.ROMAN);
	enum TexConvertMode { SIMPLE, BRACES, WITH_A }
	
	public PaliTextEditor(final Object[] args) {
		// add text area
		textInput = new PaliTextInput(PaliTextInput.InputType.AREA);
		textInput.setInputMethod(PaliTextInput.InputMethod.NORMAL);
		saveable.bindBidirectional(textInput.changeProperty());
		area = (TextArea) textInput.getInput();
		area.setWrapText(true);
		area.setPrefWidth(Utilities.getRelativeSize(44));
		area.setPrefHeight(Utilities.getRelativeSize(27));
		setCenter(area);
		area.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				final Dragboard db = event.getDragboard();
				if(event.getGestureSource() != area && (db.hasFiles() || db.hasString()))
					event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				event.consume();
			}
		});
		area.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				final Dragboard db = event.getDragboard();
				if(db.hasFiles()) {
					final File file = db.getFiles().get(0);
					if(theFile.get() == null && area.getText().isEmpty())
						Platform.runLater(() ->	openFile(file));
					else
						PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, new Object[] { file });
				} else if(db.hasString()) {
					final String text = db.getString();
					if(!text.startsWith("::paliplatform")) {
						if(theFile.get() == null)
							area.appendText(text + "\n");
						else
							PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, new Object[] { "ROMAN", text });
					}
				}
				event.consume();
			}
		});
		area.setOnDragDetected(mouseEvent -> {
			final int start = area.getSelection().getStart();
			final String text = area.getText();
			if(Character.isLetterOrDigit(text.charAt(start))) {
				final Dragboard db = area.startDragAndDrop(TransferMode.ANY);
				final ClipboardContent content = new ClipboardContent();
				// get the word under cursor
				int s = start;
				while(--s > 0)
					if(!Character.isLetterOrDigit(text.charAt(s))) break;
				int e = start;
				final int len = text.length();
				while(++e < len)
					if(!Character.isLetterOrDigit(text.charAt(e))) break;
				content.putString(text.substring(s+1, e));
				db.setContent(content);
				mouseEvent.consume();
			}
		});
		area.setOnMouseDragged(mouseEvent -> mouseEvent.setDragDetect(true));
		toolBar = new CommonWorkingToolBar(area);
		if(args[0] instanceof File) {
			Platform.runLater(() ->	openFile((File)args[0]));
		} else {
			final String strScript = (String)args[0];
			if(strScript.isEmpty()) {
				// open an existing file
				Platform.runLater(() ->	openFile());
			} else {
				currScript.set(Utilities.PaliScript.valueOf(strScript));
				final String content = args.length<2 ? "" : (String)args[1];
				setContent(content);
				toolBar.setupFontMenu(currScript.get());
			}
		}
		// add menu and toolbar on the top
		final MenuBar menuBar = new MenuBar();
		// file menu
		final Menu fileMenu = new Menu("_File");
		fileMenu.setMnemonicParsing(true);
		final MenuItem newMenuItem = new MenuItem("_New");
		newMenuItem.setMnemonicParsing(true);
		newMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
		newMenuItem.setOnAction(actionEvent -> newFile());
		final MenuItem openMenuItem = new MenuItem("_Open");
		openMenuItem.setMnemonicParsing(true);
		openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
		openMenuItem.setOnAction(actionEvent -> openAFile());
		final MenuItem saveMenuItem = new MenuItem("_Save");
		saveMenuItem.setMnemonicParsing(true);
		saveMenuItem.disableProperty().bind(saveable.not());
		saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
		saveMenuItem.setOnAction(actionEvent -> saveText());
		final MenuItem saveAsMenuItem = new MenuItem("Save _As...");
		saveAsMenuItem.setMnemonicParsing(true);
		saveAsMenuItem.disableProperty().bind(theFile.isNull());
		saveAsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		saveAsMenuItem.setOnAction(actionEvent -> saveTextAs(fileName.substring(0, fileName.lastIndexOf(".")) + "_copy.txt"));
		final MenuItem printMenuItem = new MenuItem("_Print");
		printMenuItem.setMnemonicParsing(true);
		printMenuItem.disableProperty().bind(theFile.isNull());
		printMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));
		printMenuItem.setOnAction(actionEvent -> print());
		final MenuItem closeMenuItem = new MenuItem("_Close");
		closeMenuItem.setMnemonicParsing(true);
		closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
		closeMenuItem.setOnAction(actionEvent -> closeWindow(null));
		fileMenu.getItems().addAll(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, 
									new SeparatorMenuItem(), printMenuItem, 
									new SeparatorMenuItem(), closeMenuItem);
		// edit menu
		final Menu editMenu = new Menu("_Edit");
		editMenu.setMnemonicParsing(true);
		final MenuItem undoMenuItem = new MenuItem("_Undo");
		undoMenuItem.setMnemonicParsing(true);
		undoMenuItem.disableProperty().bind(area.undoableProperty().not());
		undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
		undoMenuItem.setOnAction(actionEvent -> {
			area.undo();
			saveable.set(true);
		});	
		final MenuItem redoMenuItem = new MenuItem("_Redo");
		redoMenuItem.setMnemonicParsing(true);
		redoMenuItem.disableProperty().bind(area.redoableProperty().not());
		redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN));
		redoMenuItem.setOnAction(actionEvent -> {
			area.redo();
			saveable.set(true);
		});	
		final MenuItem cutMenuItem = new MenuItem("Cu_t");
		cutMenuItem.setMnemonicParsing(true);
		cutMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
		cutMenuItem.setOnAction(actionEvent -> area.cut());	
		final MenuItem copyMenuItem = new MenuItem("_Copy");
		copyMenuItem.setMnemonicParsing(true);
		copyMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
		copyMenuItem.setOnAction(actionEvent -> area.copy());	
		final MenuItem pasteMenuItem = new MenuItem("_Paste");
		pasteMenuItem.setMnemonicParsing(true);
		pasteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
		pasteMenuItem.setOnAction(actionEvent -> area.paste());
		final MenuItem findMenuItem = new MenuItem("_Find");
		findMenuItem.setMnemonicParsing(true);
		findMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));
		findMenuItem.setOnAction(actionEvent -> openFind(false));
		findNextMenuItem.setMnemonicParsing(true);
		findNextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN));
		findNextMenuItem.setOnAction(actionEvent -> findNext(+1));
		findPrevMenuItem.setMnemonicParsing(true);
		findPrevMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		findPrevMenuItem.setOnAction(actionEvent -> findNext(-1));
		final MenuItem replaceMenuItem = new MenuItem("Rep_lace");
		replaceMenuItem.setMnemonicParsing(true);
		replaceMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		replaceMenuItem.setOnAction(actionEvent -> openFind(true));
		
		editMenu.getItems().addAll(undoMenuItem, redoMenuItem, 
									new SeparatorMenuItem(), cutMenuItem, copyMenuItem, pasteMenuItem,
									new SeparatorMenuItem(), findMenuItem, findNextMenuItem, findPrevMenuItem, replaceMenuItem);
		// tools menu
		final Menu toolsMenu = new Menu("_Tools");
		toolsMenu.setMnemonicParsing(true);
		final Menu convertToMenu = new Menu("Con_vert to");
		convertToMenu.setMnemonicParsing(true);
		for(Utilities.PaliScript sc : Utilities.PaliScript.values()) {
			if(sc.ordinal() == 0) continue;
			final String sname = sc.toString();
			final MenuItem mitem = new MenuItem(sname.charAt(0) + sname.substring(1).toLowerCase());
			if(sc == Utilities.PaliScript.ROMAN)
				mitem.disableProperty().bind(currScript.isEqualTo(sc));
			else
				mitem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
			mitem.setOnAction(actionEvent -> convertTo(sc));
			convertToMenu.getItems().add(mitem);
		}
		final MenuItem composeMenuItem = new MenuItem("_Compose characters");
		composeMenuItem.setMnemonicParsing(true);
		composeMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		composeMenuItem.setOnAction(actionEvent -> composeChars(true));	
		final MenuItem decomposeMenuItem = new MenuItem("_Decompose characters");
		decomposeMenuItem.setMnemonicParsing(true);
		decomposeMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		decomposeMenuItem.setOnAction(actionEvent -> composeChars(false));	
		final MenuItem removeAccentsMenuItem = new MenuItem("_Remove diacritics");
		removeAccentsMenuItem.setMnemonicParsing(true);
		removeAccentsMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		removeAccentsMenuItem.setOnAction(actionEvent -> removeAccents());	
		final MenuItem calMetersMenuItem = new MenuItem("Calculate _meters");
		calMetersMenuItem.setMnemonicParsing(true);
		calMetersMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		calMetersMenuItem.setOnAction(actionEvent -> calculateMeters());
		final MenuItem analyzeMenuItem = new MenuItem("_Analyze the stanza/text");
		analyzeMenuItem.setMnemonicParsing(true);
		analyzeMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		analyzeMenuItem.setOnAction(actionEvent -> openAnalyzer());
		final MenuItem readTextMenuItem = new MenuItem("Read _text");
		readTextMenuItem.setMnemonicParsing(true);
		readTextMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		readTextMenuItem.setOnAction(actionEvent -> openReader());
		final MenuItem changeMdotAboveMenuItem = new MenuItem("Change ṃ to ṁ");
		changeMdotAboveMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		changeMdotAboveMenuItem.setOnAction(actionEvent -> replaceChar('ṃ', 'ṁ'));
		final MenuItem changeMdotBelowMenuItem = new MenuItem("Change ṁ to ṃ");
		changeMdotBelowMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		changeMdotBelowMenuItem.setOnAction(actionEvent -> replaceChar('ṁ', 'ṃ'));
		final MenuItem toUpperCaseMenuItem = new MenuItem("Change to _uppercase");
		toUpperCaseMenuItem.setMnemonicParsing(true);
		toUpperCaseMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		toUpperCaseMenuItem.setOnAction(actionEvent -> changeCase(true));
		final MenuItem toLowerCaseMenuItem = new MenuItem("Change to _lowercase");
		toLowerCaseMenuItem.setMnemonicParsing(true);
		toLowerCaseMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		toLowerCaseMenuItem.setOnAction(actionEvent -> changeCase(false));
		final MenuItem sortAscMenuItem = new MenuItem("Sort ascendingly");
		sortAscMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		sortAscMenuItem.setOnAction(actionEvent -> paliSort(true));
		final MenuItem sortDesMenuItem = new MenuItem("Sort descendingly");
		sortDesMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		sortDesMenuItem.setOnAction(actionEvent -> paliSort(false));
		final Menu paliToTexMenu = new Menu("Pāli to TeX");
		paliToTexMenu.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		final MenuItem simpleP2TMenuItem = new MenuItem("Style 1: \\~n");
		simpleP2TMenuItem.setOnAction(actionEvent -> paliToTex(TexConvertMode.SIMPLE));
		final MenuItem bracesP2TMenuItem = new MenuItem("Style 2: \\~{n}");
		bracesP2TMenuItem.setOnAction(actionEvent -> paliToTex(TexConvertMode.BRACES));
		final MenuItem withaP2TMenuItem = new MenuItem("Style 3: \\a{~}n");
		withaP2TMenuItem.setOnAction(actionEvent -> paliToTex(TexConvertMode.WITH_A));
		paliToTexMenu.getItems().addAll(simpleP2TMenuItem, bracesP2TMenuItem, withaP2TMenuItem);
		final MenuItem texToPaliMenuItem = new MenuItem("TeX to Pāli");
		texToPaliMenuItem.disableProperty().bind(currScript.isNotEqualTo(Utilities.PaliScript.ROMAN));
		texToPaliMenuItem.setOnAction(actionEvent -> texToPali());
		toolsMenu.getItems().addAll(convertToMenu, composeMenuItem, decomposeMenuItem, removeAccentsMenuItem,
									new SeparatorMenuItem(), calMetersMenuItem, analyzeMenuItem, readTextMenuItem,
									new SeparatorMenuItem(), changeMdotAboveMenuItem, changeMdotBelowMenuItem,
											toUpperCaseMenuItem, toLowerCaseMenuItem, sortAscMenuItem, sortDesMenuItem,
									new SeparatorMenuItem(), paliToTexMenu, texToPaliMenuItem);
		// options menu
		final Menu optionsMenu = new Menu("_Options");
		saveOnCloseMenuItem.setSelected(false);
		final CheckMenuItem wrapTextMenuItem = new CheckMenuItem("Wrap text");
		wrapTextMenuItem.selectedProperty().bindBidirectional(area.wrapTextProperty());
		optionsMenu.getItems().addAll(wrapTextMenuItem, saveOnCloseMenuItem);
		
		menuBar.getMenus().addAll(fileMenu, editMenu, toolsMenu, optionsMenu);
		
		// tool bar
		Platform.runLater(() -> toolBar.resetFont(currScript.get()));
		// config some buttons and add new ones
		toolBar.saveTextButton.setTooltip(new Tooltip("Save"));
		toolBar.saveTextButton.disableProperty().bind(saveable.not());
		toolBar.saveTextButton.setOnAction(actionEvent -> saveText());		
		toolBar.copyButton.setOnAction(actionEvent -> copyText());
		
		toolBar.getItems().addAll(new Separator(), textInput.getMethodButton());
		
		// config Find & Replace Box
		findReplaceBox.getFindButton().setOnAction(actionEvent -> doRegExFind());
		findReplaceBox.getPrevButton().setOnAction(actionEvent -> findNext(-1));
		findReplaceBox.getNextButton().setOnAction(actionEvent -> findNext(+1));
		findReplaceBox.getCloseButton().setOnAction(actionEvent -> setBottom(null));
		findInputCombo.setOnShowing(e -> recordQuery(findReplaceBox.getFindTextInput()));
		findInput.textProperty().addListener((obs, oldValue, newValue) -> {
			if(!(wholeWord || regexSearch))
				startImmediateSearch(Normalizer.normalize(newValue, Form.NFC));
		});
		findInputCombo.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				final KeyCode key = keyEvent.getCode();
				if(key == KeyCode.ENTER) {
					if(wholeWord || regexSearch)
						doRegExFind();
					else
						findNext(+1);
				} else if(key == KeyCode.ESCAPE) {
					clearFindInput();
				}
			}
		});
		replaceInputCombo.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				final KeyCode key = keyEvent.getCode();
				if(key == KeyCode.ESCAPE) {
					clearReplaceInput();
				}
			}
		});
		findReplaceBox.getClearFindButton().setOnAction(actionEvent -> clearFindInput());
		findReplaceBox.getClearReplaceButton().setOnAction(actionEvent -> clearReplaceInput());
		// add components
		final VBox topBox = new VBox();
		topBox.getChildren().addAll(menuBar, toolBar);
		setTop(topBox);
		// add find & replace box first, and remove it; this makes the focus request possible
		setBottom(findReplaceBox);
		Platform.runLater(() -> setBottom(null));
		// one-time init
		if(Utilities.texConvMap.isEmpty())
			Utilities.loadTexConv();
	}
	
	public void setStage(final Stage stage) {
		theStage = stage;
		theStage.setTitle(fileName);
		theStage.setOnCloseRequest(new EventHandler<WindowEvent>() {  
			@Override
			public void handle(final WindowEvent event) {
				closeWindow(event);
			}
		});
	}
	
	public final void setContent(final String text) {
		area.setText(text);
	}

	private void openAFile() {
		if(saveable.get()) {
			if(saveOnCloseMenuItem.isSelected()) {
				saveText();
				openFile();
			} else {
				final ConfirmAlert saveAlert = new ConfirmAlert(theStage, ConfirmAlert.ConfirmType.SAVE);
				final Optional<ButtonType> result = saveAlert.showAndWait();
				if(result.isPresent()) {
					if(result.get() == saveAlert.getConfirmButtonType()) {
						saveText();
						openFile();
					} else {
						if(result.get() == saveAlert.getDiscardButtonType())
							openFile();
					}
				}
			}
		} else {
			openFile();
		}
		toolBar.resetFont(currScript.get());
	}
		
	public final boolean openFile() {
		final File file = Utilities.selectTextFile(theStage);
		return openFile(file);
	}

	public final boolean openFile(final File file) {
		boolean success = false;
		if(file != null) {
			fileName = file.getName();
			if(theStage != null)
				theStage.setTitle(fileName);
			final PaliTextInput.InputMethod saveInputMethod = textInput.getInputMethod();
			textInput.changeInputMethod(PaliTextInput.InputMethod.NONE);
			final String content = Utilities.getTextFileContent(file);
			currScript.set(Utilities.testLanguage(content));
			toolBar.setupFontMenu(currScript.get());
			area.setText(content);
			textInput.changeInputMethod(saveInputMethod);
			success = true;
			theFile.set(file);
			setInitialValues();
		}
		return success;
	}
	
	private void setInitialValues() {
		saveable.set(false);
		area.setWrapText(true);
		saveOnCloseMenuItem.setSelected(false);
		searchTextFound.set(false);
		regexMode = false;
		findReplaceBox.clearInputs();
		findReplaceBox.clearOptions();
		setBottom(null);
	}
		
	private void newFile() {
		if(saveable.get()) {
			if(saveOnCloseMenuItem.isSelected()) {
				saveText();
				clearEditor();
			} else {
				final ConfirmAlert saveAlert = new ConfirmAlert(theStage, ConfirmAlert.ConfirmType.SAVE);
				final Optional<ButtonType> result = saveAlert.showAndWait();
				if(result.isPresent()) {
					if(result.get() == saveAlert.getConfirmButtonType()) {
						saveText();
						clearEditor();
					} else {
						if(result.get() == saveAlert.getDiscardButtonType())
							clearEditor();
					}
				}
			}
		} else {
			clearEditor();
		}
	}
	
	public void clearEditor(final Utilities.PaliScript script) {
		currScript.set(script);
		clearEditor();
	}
	
	private void clearEditor() {
		fileName = DEFAULT_FILENAME;
		theStage.setTitle(fileName);
		theFile.set(null);
		area.clear();
		setInitialValues();
		resetFont(currScript.get());
	}
	
	public void resetFont(final Utilities.PaliScript script) {
		toolBar.resetFont(script);
	}

	public void resetFont(final String fontname) {
		toolBar.resetFont(fontname);
	}
	
	public void resetFont() {
		toolBar.resetFont(currScript.get());
	}
	
	private void copyText() {
		if(area.getSelection().getLength() > 0)
			area.copy();
		else
			Utilities.copyText(area.getText());
	}
	
	private void saveText() {
		if(theFile.get() == null) {
			saveTextAs(fileName);
		} else {
			final File file = theFile.get();
			Utilities.saveText(area.getText(), file);
			if(file.equals(Utilities.customDictFile))
				PaliTextReader.updateCustomDict();
			else if(file.equals(Utilities.sandhiFile))
				Utilities.updateSandhiList();
			else if(file.equals(Utilities.stopwordsFile))
				Utilities.updateStopwords();
			saveable.set(false);
		}
	}
	
	private void saveTextAs(final String newname) {
		final File file = Utilities.saveText(area.getText(), newname, theStage);
		if(file != null) {
			fileName = file.getName();
			theStage.setTitle(fileName);
			theFile.set(file);
			saveable.set(false);
		}
	}
	
	private void print() {
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        PrintService printService[] = PrintServiceLookup.lookupPrintServices(flavor, pras);
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        PrintService service = ServiceUI.printDialog(null, 200, 200, printService, defaultService, flavor, pras);
        if (service != null) {
			try {
				DocPrintJob job = service.createPrintJob();
				FileInputStream fis = new FileInputStream(theFile.get());
				DocAttributeSet das = new HashDocAttributeSet();
				Doc doc = new SimpleDoc(fis, flavor, das);
				job.print(doc, pras);
			} catch (FileNotFoundException | PrintException e) {
				System.err.println(e);
			}
        }
	}
	
	public SimpleBooleanProperty searchTextFoundProperty() {
		return searchTextFound;
	}
	
	private void clearFindInput() {
		recordQuery(findReplaceBox.getFindTextInput());
		findInput.clear();
		findInputCombo.setValue(null);
		searchTextFound.set(false);
		regexFindResult.clear();
		setDisableNextPrev(wholeWord || regexSearch);
		area.deselect();		
	}
	
	private void clearReplaceInput() {
		recordQuery(findReplaceBox.getReplaceTextInput());
		replaceInput.clear();		
	}
	
	private void openFind(final boolean withReplace) {
		findReplaceBox.showReplace(withReplace);
		setBottom(findReplaceBox);
		findReplaceBox.getFindTextField().requestFocus();
	}
	
	public void startSearch() {
		caseSensitive = findReplaceBox.caseSensitiveProperty().get();
		wholeWord = findReplaceBox.wholeWordProperty().get();
		regexSearch = findReplaceBox.regexSearchProperty().get();
		setDisableNextPrev(wholeWord || regexSearch);
		if(wholeWord || regexSearch) {
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
		regexMode = false;
		searchTextFound.set(false);
		currFoundIndex = 0;
		search(query, +1);
		findInputCombo.commitValue();		
	}
	
	private void search(final String strToFind, final int direction) {
		int foundPos = -1;
		if(caseSensitive) {
			// case sensitive search
			foundPos = direction>0 ? area.getText().indexOf(strToFind, currFoundIndex) : area.getText().lastIndexOf(strToFind, currFoundIndex);
		} else {
			// case insentitive search
			final String query = strToFind.toLowerCase();
			final String lowerCaseText = area.getText().toLowerCase();
			foundPos = direction>0 ? lowerCaseText.indexOf(query, currFoundIndex) : lowerCaseText.lastIndexOf(query, currFoundIndex);
		}
		if(foundPos > -1) {
			searchTextFound.set(true);
			area.selectRange(foundPos, foundPos+strToFind.length());
			currFoundIndex = foundPos;
		} else {
			if(searchTextFound.get()) {
				// circle the search
				currFoundIndex = direction>0 ? 0 : area.getLength();
				search(strToFind, direction);
			} else {
				area.deselect();
				showMessage("Not found");
			}
		}
	}
	
	private void doRegExFind() {
		currFoundIndex = 0;
		regexMode = true;
		regexFindResult.clear();
		final String strInput = findInput.getText();
		if(strInput.isEmpty())
			return;
		final String strToFind;
		final String textBody;
		if(regexSearch || caseSensitive) {
			strToFind = Normalizer.normalize(strInput, Form.NFC);
			textBody = area.getText();
		} else {
			strToFind = Normalizer.normalize(strInput.toLowerCase(), Form.NFC);
			textBody = area.getText().toLowerCase();
		}
		String patt = "";
		if(wholeWord) {
			patt = "\\b" + strToFind + "\\b";
		} else {
			patt = strToFind;
		}
		try {
			final Pattern regPat = Pattern.compile(patt);
			final Matcher matcher = regPat.matcher(textBody);
			while(matcher.find()) {
				final int[] res = new int[2];
				res[0] = matcher.start(0);
				res[1] = matcher.end(0);
				regexFindResult.add(res);
			}
			if(regexFindResult.isEmpty()) {
				searchTextFound.set(false);
				area.deselect();
				showMessage("Not found");
			} else {
				searchTextFound.set(true);
				findFirst();
			}
		} catch(PatternSyntaxException e) {
			searchTextFound.set(false);
			showMessage("Invalid input pattern");
			setDisableNextPrev(true);
		}
	}
	
	private void findFirst() {
		final int[] pos = regexFindResult.get(currFoundIndex);
		area.selectRange(pos[0], pos[1]);
		setDisableNextPrev(false);		
		showMessage((currFoundIndex+1) + " of " + regexFindResult.size());
	}
	
	private void findNext(final int direction) {
		currFoundIndex += direction;		
		if(regexMode) {
			if(regexFindResult.isEmpty())
				return;
			if(currFoundIndex >= regexFindResult.size())
				currFoundIndex = 0;
			else if(currFoundIndex < 0)
				currFoundIndex = regexFindResult.size() - 1;
			final int[] pos = regexFindResult.get(currFoundIndex);
			area.selectRange(pos[0], pos[1]);
			showMessage((currFoundIndex+1) + " of " + regexFindResult.size());
		} else {
			search(Normalizer.normalize(findInput.getText(), Form.NFC), direction);
		}
	}
	
	public void replaceFirst() {
		if(!area.getSelectedText().isEmpty()) {
			final String replacement = Normalizer.normalize(replaceInput.getText(), Form.NFC);
			area.replaceSelection(replacement);
			if(regexMode)
				doRegExFind();
			else
				findNext(+1);
			recordQuery(findReplaceBox.getFindTextInput());
			recordQuery(findReplaceBox.getReplaceTextInput());
		}
	}
	
	public void replaceAll() {
		if(!area.getSelectedText().isEmpty()) {
			if(proceedConfirm()) {
				final String replacement = Normalizer.normalize(replaceInput.getText(), Form.NFC);
				if(regexMode) {
						final String text = area.getText();
						final StringBuilder sb = new StringBuilder();
						int start = 0;
						for(int[] range : regexFindResult) {
							sb.append(text.substring(start, range[0]));
							sb.append(replacement);
							start = range[1];
						}
						sb.append(text.substring(start, text.length()));
						area.setText(sb.toString());
						regexFindResult.clear();
				} else {
					final String newText = area.getText().replace(Normalizer.normalize(findInput.getText(), Form.NFC), replacement);
					area.setText(newText);
				}
				recordQuery(findReplaceBox.getFindTextInput());
				recordQuery(findReplaceBox.getReplaceTextInput());				
				searchTextFound.set(false);
				setDisableNextPrev(true);
			}
		}
	}
	
	private void recordQuery(final PaliTextInput ptInput) {
		if(searchTextFound.get())
			ptInput.recordQuery();
	}
	
	private void showMessage(final String text) {
		findReplaceBox.showMessage(text);
	}
	
	private void setDisableNextPrev(final boolean yn) {
		findNextMenuItem.setDisable(yn);
		findPrevMenuItem.setDisable(yn);
		findReplaceBox.getPrevButton().setDisable(yn);
		findReplaceBox.getNextButton().setDisable(yn);			
	}

	private void openNewEditor(final Utilities.PaliScript script, final String content) {
		final Object[] args = { script.toString(), content };
		PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
	}
	
	private void convertTo(final Utilities.PaliScript script) {
		final String result;
		final String selText = area.getSelectedText();
		final String inputText = selText.isEmpty() ? area.getText() : selText;
		switch(script) {
			case ROMAN:
				switch(currScript.get()) {
					case DEVANAGARI: result = PaliCharTransformer.devanagariToRoman(inputText); break;
					case KHMER: result = PaliCharTransformer.khmerToRoman(inputText); break;
					case MYANMAR: result = PaliCharTransformer.myanmarToRoman(inputText); break;
					case SINHALA: result = PaliCharTransformer.sinhalaToRoman(inputText); break;
					case THAI: result = PaliCharTransformer.thaiToRoman(inputText); break;
					default: result = "";
				}
				break;
			case DEVANAGARI:
				result = PaliCharTransformer.romanToDevanagari(Normalizer.normalize(inputText, Form.NFC));
				break;
			case KHMER:
				result = PaliCharTransformer.romanToKhmer(Normalizer.normalize(inputText, Form.NFC));
				break;
			case MYANMAR:
				result = PaliCharTransformer.romanToMyanmar(Normalizer.normalize(inputText, Form.NFC));
				break;
			case SINHALA:
				result = PaliCharTransformer.romanToSinhala(Normalizer.normalize(inputText, Form.NFC));
				break;
			case THAI:
				PaliCharTransformer.setUsingAltThaiChars(Boolean.parseBoolean(PaliPlatform.settings.getProperty("thai-alt-chars")));
				result = PaliCharTransformer.romanToThai(Normalizer.normalize(inputText, Form.NFC));
				break;
			default:
				result = "";
		}
		openNewEditor(script, result);
	}
	
	private void composeChars(final boolean isCompose) {
		final String selText = area.getSelectedText();
		final String inputText = selText.isEmpty() ? area.getText() : selText;
		final String result = isCompose ? Normalizer.normalize(inputText, Form.NFC) : Normalizer.normalize(inputText, Form.NFD);
		openNewEditor(currScript.get(), result);
	}
	
	private void removeAccents() {
		final String selText = area.getSelectedText();
		final String inputText = selText.isEmpty() ? area.getText() : selText;
		final String result = removeAccents(inputText);
		openNewEditor(currScript.get(), result);
	}
	
	private String removeAccents(final String input) {
		final String text = Normalizer.normalize(input, Form.NFD);
		final int len = text.length();
		final StringBuilder output = new StringBuilder(len);
		for(int i = 0; i < len; i++) {
			final char ch = text.charAt(i);
			if(ch < '\u0300' || ch > '\u036F')
				output.append(ch);
		}
		return output.toString();
	}

	private void calculateMeters() {
		final String selText = area.getSelectedText();
		final String inputText = selText.isEmpty() ? area.getText() : selText;
		openNewEditor(currScript.get(), Utilities.addComputedMeters(inputText));
	}

	private void openAnalyzer() {
		final String selText = area.getSelectedText();
		final String inputText = selText.isEmpty() ? area.getText() : selText;
		final Object[] args = { inputText };
		PaliPlatform.openWindow(PaliPlatform.WindowType.PROSODY, args);
	}

	private void openReader() {
		final String selText = area.getSelectedText();
		final String inputText = selText.isEmpty() ? area.getText() : selText;
		final Object[] args = { inputText };
		PaliPlatform.openWindow(PaliPlatform.WindowType.READER, args);
	}

	private void changeCase(final boolean isUpper) {
		final String selText = area.getSelectedText();
		final boolean isAll = selText.isEmpty();
		final String inputText = isAll ? area.getText() : selText;
		if(isAll) {
			if(proceedConfirm())
				area.setText(toCase(inputText, isUpper));
		} else {
			final IndexRange indRange = area.getSelection();
			final int start = indRange.getStart();
			final StringBuilder text = new StringBuilder(area.getText());
			text.delete(start, indRange.getEnd());
			text.insert(start, toCase(inputText, isUpper));
			area.setText(text.toString());
		}
	}

	private String toCase(final String input, final boolean isUpper) {
		final String result = isUpper ? input.toUpperCase() : input.toLowerCase();
		return result;
	}

	private void paliSort(final boolean isAsc) {
		if(proceedConfirm()) {
			final Stream<String> allLines = area.getText().lines().map(s -> s.trim());
			final Stream<String> result;
			if(isAsc)
				result = allLines.sorted((x, y) -> PaliPlatform.paliCollator.compare(x, y));
			else
				result = allLines.sorted((x, y) -> PaliPlatform.paliCollator.compare(y, x));
			area.setText(result.collect(Collectors.joining((System.getProperty("line.separator")))));
		}
	}

	private void replaceChar(final char fromCh, final char toCh) {
		final String oldText = area.getText();
		String newText = oldText.replace(fromCh, toCh);
		newText = newText.replace(Character.toUpperCase(fromCh), Character.toUpperCase(toCh));
		area.setText(newText);
	}

	private void paliToTex(final TexConvertMode mode) {
		final String selText = Normalizer.normalize(area.getSelectedText(), Form.NFC);
		final boolean isAll = selText.isEmpty();
		final String inputText = isAll ? Normalizer.normalize(area.getText(), Form.NFC) : selText;
		if(isAll) {
			if(proceedConfirm())
				area.setText(toTex(inputText, mode));
		} else {
			final IndexRange indRange = area.getSelection();
			final int start = indRange.getStart();
			final StringBuilder text = new StringBuilder(area.getText());
			text.delete(start, indRange.getEnd());
			text.insert(start, toTex(inputText, mode));
			area.setText(text.toString());
		}
	}

	private void texToPali() {
		final String selText = area.getSelectedText();
		final boolean isAll = selText.isEmpty();
		final String inputText = isAll ? area.getText() : selText;
		if(isAll) {
			if(proceedConfirm())
				area.setText(fromTex(inputText));
		} else {
			final IndexRange indRange = area.getSelection();
			final int start = indRange.getStart();
			final StringBuilder text = new StringBuilder(area.getText());
			text.delete(start, indRange.getEnd());
			text.insert(start, fromTex(inputText));
			area.setText(text.toString());
		}
	}

	private String toTex(final String text, final TexConvertMode mode) {
		final StringBuilder result = new StringBuilder();
		for(final char ch : text.toCharArray()) {
			if(Utilities.texConvMap.containsKey(ch))
				result.append(Utilities.texConvMap.get(ch).get(mode.ordinal()));
			else
				result.append(ch);
		}
		return result.toString();
	}

	private String fromTex(final String text) {
		String result = text;
		for(final char ch : Utilities.texConvMap.keySet()) {
			final List<String> pattList = Utilities.texConvMap.get(ch);
			result = result.replace(pattList.get(0), "" + ch)
							.replace(pattList.get(1), "" + ch)
							.replace(pattList.get(2), "" + ch);
		}
		return result;
	}

	private boolean proceedConfirm() {
		boolean output = false;
		final String message = "This whole-file operation cannot be undone.\nMake sure the file is well saved."
								+ "\nIf you are aware what you are doing, then proceed.";
		final ConfirmAlert proceedAlert = new ConfirmAlert(theStage, ConfirmAlert.ConfirmType.PROCEED, message);
		final Optional<ButtonType> result = proceedAlert.showAndWait();
		if(result.isPresent()) {
			if(result.get() == proceedAlert.getConfirmButtonType())
				output = true;
		}
		return output;		
	}
	
	private void closeWindow(final WindowEvent event) {
		if(saveable.get()) {
			if(saveOnCloseMenuItem.isSelected()) {
				saveText();
				theStage.hide();
			} else {
				final ConfirmAlert saveAlert = new ConfirmAlert(theStage, ConfirmAlert.ConfirmType.SAVE);
				final Optional<ButtonType> result = saveAlert.showAndWait();
				if(result.isPresent()) {
					if(result.get() == saveAlert.getConfirmButtonType()) {
						saveText();
						theStage.hide();
					} else {
						if(result.get() != saveAlert.getDiscardButtonType()) {
							if(event != null) {
								event.consume();
							}
						} else {
							theStage.hide();
						}
					}
				}
			}
		} else {
			theStage.hide();
		}
	}
}
