/*
 * MainToolBar.java
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

import javafx.scene.control.*;

/** 
 * The main toolbar. This is a singleton.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
class MainToolBar extends ToolBar {
	static final MainToolBar INSTANCE = new MainToolBar();
	public final Button helpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
	
	private MainToolBar() {
		final Button toctreeButton = new Button("", new TextIcon("folder-tree", TextIcon.IconSet.AWESOME));
		toctreeButton.setTooltip(new Tooltip("Open new TOC Tree"));
		toctreeButton.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.TOCTREE, null));
		
		final Button bookmarksButton = new Button("", new TextIcon("bookmark", TextIcon.IconSet.AWESOME));
		bookmarksButton.setTooltip(new Tooltip("Bookmarks"));
		bookmarksButton.setOnAction(actionEvent -> Bookmarks.INSTANCE.display());
		
		final Button docFinderButton = new Button("", new TextIcon("magnifying-glass", TextIcon.IconSet.AWESOME));
		docFinderButton.setTooltip(new Tooltip("Document Finder"));
		docFinderButton.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.FINDER, null));	
				
		final Button luceneButton = new Button("", new TextIcon("lucene", TextIcon.IconSet.CUSTOM));
		luceneButton.setTooltip(new Tooltip("Lucene Finder"));
		luceneButton.setOnAction(actionEvent -> LuceneFinder.INSTANCE.display());
		
		final Button listerButton = new Button("", new TextIcon("bars", TextIcon.IconSet.AWESOME));
		listerButton.setTooltip(new Tooltip("Simple Lister"));
		listerButton.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.LISTER, null));
		
		final Button tokenButton = new Button("", new TextIcon("grip", TextIcon.IconSet.AWESOME));
		tokenButton.setTooltip(new Tooltip("Tokenizer"));
		tokenButton.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.TOKEN, null));
		
		final Button readerButton = new Button("", new TextIcon("book-open", TextIcon.IconSet.AWESOME));
		readerButton.setTooltip(new Tooltip("Pāli Text Reader"));
		readerButton.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.READER, null));
		
		final Button sentManButton = new Button("", new TextIcon("briefcase", TextIcon.IconSet.AWESOME));
		sentManButton.setTooltip(new Tooltip("Sentence Manager"));
		sentManButton.setOnAction(actionEvent -> SentenceManager.INSTANCE.display());
		
		final Button dictButton = new Button("", new TextIcon("book", TextIcon.IconSet.AWESOME));
		dictButton.setTooltip(new Tooltip("Dictionaries"));
		dictButton.setOnAction(actionEvent -> PaliPlatform.openWindow(PaliPlatform.WindowType.DICT, null));
		
		final Button openTextButton = new Button("", new TextIcon("file-arrow-up", TextIcon.IconSet.AWESOME));
		openTextButton.setTooltip(new Tooltip("Open a text file"));
		openTextButton.setOnAction(actionEvent -> {
			final Object[] args = {""};
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
		});
		
		final Button editorButton = new Button("", new TextIcon("pencil", TextIcon.IconSet.AWESOME));
		editorButton.setTooltip(new Tooltip("Edit a new text file"));
		editorButton.setOnAction(actionEvent -> {
			final Object[] args = {"ROMAN"};
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
		});
		
		final Button settingsButton = new Button("", new TextIcon("gear", TextIcon.IconSet.AWESOME));
		settingsButton.setTooltip(new Tooltip("Settings"));
		settingsButton.setOnAction(actionEvent -> Settings.INSTANCE.display());

		helpButton.setTooltip(new Tooltip("Quick starter guide"));
		helpButton.setOnAction(actionEvent -> PaliPlatform.infoPopup.showPopup(helpButton, InfoPopup.Pos.BELOW_CENTER, true));		
				
		final Button aboutButton = new Button("", new TextIcon("circle-info", TextIcon.IconSet.AWESOME));
		aboutButton.setTooltip(new Tooltip("About the program"));
		aboutButton.setOnAction(actionEvent -> PaliPlatform.about());
		
		final Button exitButton = new Button("", new TextIcon("power-off", TextIcon.IconSet.AWESOME));
		exitButton.setTooltip(new Tooltip("Exit"));
		exitButton.setOnAction(actionEvent -> PaliPlatform.exit(null));
		
		getItems().addAll(toctreeButton, bookmarksButton, docFinderButton, luceneButton, listerButton, tokenButton,
						new Separator(), readerButton, sentManButton,
						new Separator(), dictButton,
						new Separator(), editorButton, openTextButton,
						new Separator(), settingsButton, 
						new Separator(), helpButton, aboutButton, exitButton);
	}
}
