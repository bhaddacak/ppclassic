/*
 * MainMenu.java
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

import paliplatform.toctree.*;
import paliplatform.viewer.*;
import paliplatform.grammar.*;

import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCode;

/** 
 * The main menu bar including some action controllers. This is a singleton.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
class MainMenu extends MenuBar {
	static final MainMenu INSTANCE = new MainMenu();
	
	private MainMenu() {
		// File
		final Menu fileMenu = new Menu("_File");
		fileMenu.setMnemonicParsing(true);
		final MenuItem openTextMenuItem = new MenuItem("_Open a text file", new TextIcon("file-arrow-up", TextIcon.IconSet.AWESOME));
		openTextMenuItem.setMnemonicParsing(true);
		openTextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
		openTextMenuItem.setOnAction(actionEvent -> {
			final Object[] args = {""};
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
		});
		final MenuItem editorMenuItem = new MenuItem("Edit a _new text file", new TextIcon("pencil", TextIcon.IconSet.AWESOME));
		editorMenuItem.setMnemonicParsing(true);
		editorMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
		editorMenuItem.setOnAction(actionEvent -> {
			final Object[] args = {"ROMAN"};
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
		});
		final MenuItem batchMenuItem = new MenuItem("_Batch Script Transformer", new TextIcon("gears", TextIcon.IconSet.AWESOME));
		batchMenuItem.setMnemonicParsing(true);
		batchMenuItem.setOnAction(actionEvent -> BatchScriptTransformer.INSTANCE.display());
		final MenuItem exitMenuItem = new MenuItem("E_xit", new TextIcon("power-off", TextIcon.IconSet.AWESOME));
		exitMenuItem.setMnemonicParsing(true);
		exitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
		exitMenuItem.setOnAction(actionEvent -> PaliPlatform.exit(null));
		fileMenu.getItems().addAll(editorMenuItem, openTextMenuItem, 
								new SeparatorMenuItem(), batchMenuItem, new SeparatorMenuItem(), exitMenuItem);
		
		// Collection
		final Menu collectionMenu = new Menu("_Collection");
		collectionMenu.setMnemonicParsing(true);
		final MenuItem tocMenuItem = new MenuItem("_TOC Tree", new TextIcon("folder-tree", TextIcon.IconSet.AWESOME));
		tocMenuItem.setMnemonicParsing(true);
		tocMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.TOCTREE, null));
		final MenuItem bookmarksMenuItem = new MenuItem("_Bookmarks", new TextIcon("bookmark", TextIcon.IconSet.AWESOME));
		bookmarksMenuItem.setMnemonicParsing(true);
		bookmarksMenuItem.setOnAction(actionEvent -> Bookmarks.INSTANCE.display());
		final MenuItem docFinderMenuItem = new MenuItem("Document _Finder", new TextIcon("magnifying-glass", TextIcon.IconSet.AWESOME));
		docFinderMenuItem.setMnemonicParsing(true);
		docFinderMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.FINDER, null));
		final MenuItem luceneMenuItem = new MenuItem("Lu_cene Finder", new TextIcon("lucene", TextIcon.IconSet.CUSTOM));
		luceneMenuItem.setMnemonicParsing(true);
		luceneMenuItem.setOnAction(actionEvent -> LuceneFinder.INSTANCE.display());
		final MenuItem listerMenuItem = new MenuItem("Simple _Lister", new TextIcon("bars", TextIcon.IconSet.AWESOME));
		listerMenuItem.setMnemonicParsing(true);
		listerMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.LISTER, null));
		final MenuItem tokenMenuItem = new MenuItem("To_kenizer", new TextIcon("grip", TextIcon.IconSet.AWESOME));
		tokenMenuItem.setMnemonicParsing(true);
		tokenMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.TOKEN, null));
		final MenuItem readerMenuItem = new MenuItem("Text _Reader", new TextIcon("book-open", TextIcon.IconSet.AWESOME));
		readerMenuItem.setMnemonicParsing(true);
		readerMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.READER, null));
		final MenuItem sentManMenuItem = new MenuItem("Sentence _Manager", new TextIcon("briefcase", TextIcon.IconSet.AWESOME));
		sentManMenuItem.setMnemonicParsing(true);
		sentManMenuItem.setOnAction(actionEvent -> SentenceManager.INSTANCE.display());
		collectionMenu.getItems().addAll(tocMenuItem, bookmarksMenuItem, docFinderMenuItem, luceneMenuItem, listerMenuItem, tokenMenuItem, 
										new SeparatorMenuItem(), readerMenuItem, sentManMenuItem);
				
		// Grammar
		final Menu grammarMenu = new Menu("_Grammar");
		grammarMenu.setMnemonicParsing(true);
		final MenuItem dictMenuItem = new MenuItem("_Dictionaries", new TextIcon("book", TextIcon.IconSet.AWESOME));
		dictMenuItem.setMnemonicParsing(true);
		dictMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.DICT, null));
		final MenuItem lettersMenuItem = new MenuItem("Letters", new TextIcon("font", TextIcon.IconSet.AWESOME));
		lettersMenuItem.setOnAction(actionEvent -> LetterWin.INSTANCE.display());
		final MenuItem declMenuItem = new MenuItem("Declension table", new TextIcon("table-cells", TextIcon.IconSet.AWESOME));
		declMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.DECLENSION, null));
		final MenuItem verbsMenuItem = new MenuItem("Verbs", new TextIcon("person-walking", TextIcon.IconSet.AWESOME));
		verbsMenuItem.setOnAction(actionEvent -> VerbWin.INSTANCE.display());
		final MenuItem conjugMenuItem = new MenuItem("Conjugation table", new TextIcon("table-cells", TextIcon.IconSet.AWESOME));
		conjugMenuItem.setOnAction(actionEvent -> ConjugationWin.INSTANCE.display());
		final MenuItem rootsMenuItem = new MenuItem("Roots", new TextIcon("seedling", TextIcon.IconSet.AWESOME));
		rootsMenuItem.setOnAction(actionEvent -> RootWin.INSTANCE.display());
		final MenuItem prosodyMenuItem = new MenuItem("Prosody", new TextIcon("music", TextIcon.IconSet.AWESOME));
		prosodyMenuItem.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.PROSODY, null));
		grammarMenu.getItems().addAll(dictMenuItem, lettersMenuItem, declMenuItem, verbsMenuItem, conjugMenuItem, rootsMenuItem, prosodyMenuItem);
				
		// Option
		Menu optionsMenu = new Menu("_Options");
		final Menu themeMenu = new Menu("Global _theme");
		themeMenu.setMnemonicParsing(true);
		final ToggleGroup themeGroup = new ToggleGroup();
		for(PaliPlatform.Theme t : PaliPlatform.Theme.values()){
			final String tName = t.toString();
			final RadioMenuItem themeItem = new RadioMenuItem(tName.charAt(0) + tName.substring(1).toLowerCase());
			themeItem.setToggleGroup(themeGroup);
			themeItem.setSelected(themeItem.getText().toUpperCase().equals(PaliPlatform.settings.getProperty("theme")));
			themeMenu.getItems().add(themeItem);
		}
        themeGroup.selectedToggleProperty().addListener((observable) -> {
			if(themeGroup.getSelectedToggle() != null) {
				final RadioMenuItem selected = (RadioMenuItem)themeGroup.getSelectedToggle();
				final String t = selected.getText().toUpperCase();
				PaliPlatform.settings.setProperty("theme", "" + t);
				PaliPlatform.refreshTheme();
			}
        });		
		optionsMenu.setMnemonicParsing(true);
		MenuItem settingsMenuItem = new MenuItem("Settings", new TextIcon("gear", TextIcon.IconSet.AWESOME));
		settingsMenuItem.setOnAction(actionEvent -> Settings.INSTANCE.display());
		optionsMenu.getItems().addAll(themeMenu, settingsMenuItem);
		
		// Help
		final Menu helpMenu = new Menu("_Help");
		helpMenu.setMnemonicParsing(true);
		final MenuItem helpMenuItem = new MenuItem("Quick starter", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		helpMenuItem.setOnAction(actionEvent -> PaliPlatform.infoPopup.showPopup(MainToolBar.INSTANCE.helpButton, InfoPopup.Pos.BELOW_CENTER, true));				
		final MenuItem aboutMenuItem = new MenuItem("About", new TextIcon("circle-info", TextIcon.IconSet.AWESOME));
		aboutMenuItem.setOnAction(actionEvent -> PaliPlatform.about());
		helpMenu.getItems().addAll(helpMenuItem, aboutMenuItem);
						
		getMenus().addAll(fileMenu, collectionMenu, grammarMenu, optionsMenu, helpMenu);
	}
}
