/*
 * FindReplaceBox.java
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

import paliplatform.viewer.*;

import javafx.application.Platform;
import javafx.stage.Popup;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.*;
import javafx.collections.*;
import javafx.beans.property.SimpleBooleanProperty;

/** 
 * The Find and Replace box used mainly in Editor and Viewer.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class FindReplaceBox extends VBox {
	private final Node hostNode;
	private final PaliTextInput findTextInput = new PaliTextInput(PaliTextInput.InputType.COMBO);
	private final PaliTextInput replaceTextInput = new PaliTextInput(PaliTextInput.InputType.COMBO);
	private final TextField findTextField;
	private final ComboBox<String> findComboBox;
	private final TextField replaceTextField;
	private final ComboBox<String> replaceComboBox;
	private final HBox replaceBox = new HBox();
	private final Button findButton = new Button("Find");
	private final Button prevButton = new Button("", new TextIcon("angle-left", TextIcon.IconSet.AWESOME));
	private final Button nextButton = new Button("", new TextIcon("angle-right", TextIcon.IconSet.AWESOME));
	private final Button closeButton = new Button("", new TextIcon("xmark", TextIcon.IconSet.AWESOME));
	private final Button clearFindButton;
	private final Button clearReplaceButton;
	private final Popup messagePopup = new Popup();
	private final Label messageText = new Label("");
	private final InfoPopup infoPopup = new InfoPopup();
	private final SimpleBooleanProperty caseSensitive = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty wholeWord = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty regexSearch = new SimpleBooleanProperty(false);
	
	public FindReplaceBox(final Node node) {
		hostNode = node;
		// find part
		findTextInput.setInputMethod(PaliTextInput.InputMethod.NORMAL);
		findComboBox = findTextInput.getComboBox();
		findComboBox.setPromptText("Search for...");
		findTextField = (TextField)findTextInput.getInput();
		clearFindButton = findTextInput.getClearButton();
		findButton.disableProperty().bind(wholeWord.or(regexSearch).not());
		prevButton.setTooltip(new Tooltip("Find backward"));
		nextButton.setTooltip(new Tooltip("Find forward"));
		closeButton.setTooltip(new Tooltip("Close"));
		final Button helpButton = new Button("", new TextIcon("circle-question", TextIcon.IconSet.AWESOME));
		helpButton.setOnAction(actionEvent -> infoPopup.showPopup(helpButton, InfoPopup.Pos.OVER_RIGHT, true));
		final MenuButton findOptionsMenu = new MenuButton("", new TextIcon("check-double", TextIcon.IconSet.AWESOME));		
		findOptionsMenu.setTooltip(new Tooltip("Options"));
		final CheckMenuItem caseSensitivityMenuItem = new CheckMenuItem("Case sensitive");
		caseSensitivityMenuItem.disableProperty().bind(regexSearch);
		caseSensitivityMenuItem.selectedProperty().bindBidirectional(caseSensitive);
		caseSensitivityMenuItem.setOnAction(actionEvent -> startSearch());
		final CheckMenuItem wholeWordMenuItem = new CheckMenuItem("Whole word");
		wholeWordMenuItem.disableProperty().bind(regexSearch);
		wholeWordMenuItem.selectedProperty().bindBidirectional(wholeWord);
		wholeWordMenuItem.setOnAction(actionEvent -> startSearch());
		final CheckMenuItem regexMenuItem = new CheckMenuItem("Regular expression");
		regexMenuItem.selectedProperty().bindBidirectional(regexSearch);
		regexMenuItem.setOnAction(actionEvent -> startSearch());
		findOptionsMenu.getItems().addAll(caseSensitivityMenuItem, wholeWordMenuItem, regexMenuItem);
		final HBox findBox = new HBox();
		findBox.getChildren().addAll(findComboBox, clearFindButton, findTextInput.getMethodButton(),
									new Separator(), findButton, prevButton, nextButton, 
									new Separator(), findOptionsMenu, 
									new Separator(), helpButton, closeButton);
		// replace part
		replaceTextInput.setInputMethod(PaliTextInput.InputMethod.NORMAL);
		replaceComboBox = replaceTextInput.getComboBox();
		replaceComboBox.setPromptText("Replace with...");
		replaceTextField = (TextField)replaceTextInput.getInput();
		clearReplaceButton = replaceTextInput.getClearButton();
		final Button replaceButton = new Button("Replace");
		replaceButton.setOnAction(actionEvent -> replace(false));
		final Button replaceAllButton = new Button("All at once");
		replaceAllButton.setOnAction(actionEvent -> replace(true));
		Platform.runLater(() -> {
			if(node instanceof PaliTextEditor) {
				replaceButton.disableProperty().bind(((PaliTextEditor)node).searchTextFoundProperty().not());
				replaceAllButton.disableProperty().bind(((PaliTextEditor)node).searchTextFoundProperty().not());
			}
		});
		replaceBox.getChildren().addAll(replaceComboBox, clearReplaceButton, replaceTextInput.getMethodButton(), 
										new Separator(), replaceButton, replaceAllButton);
				
		// add components
		getChildren().add(findBox);
		setPadding(new Insets(3));
		
		// prepare popups
		messagePopup.setHideOnEscape(true);
		messagePopup.setAutoHide(true);
		final StackPane messageBox = new StackPane();
		messageBox.getStyleClass().add("infopopup");
		messageBox.getChildren().add(messageText);
		messagePopup.getContent().add(messageBox);
				
		infoPopup.setContent("info-find-replace.txt");
		infoPopup.setTextWidth(Utilities.getRelativeSize(32));
	}
	
	public PaliTextInput getFindTextInput() {
		return findTextInput;
	}
	
	public TextField getFindTextField() {
		return findTextField;
	}
	
	public ComboBox<String> getFindComboBox() {
		return findComboBox;
	}
	
	public PaliTextInput getReplaceTextInput() {
		return replaceTextInput;
	}
		
	public TextField getReplaceTextField() {
		return replaceTextField;
	}
	
	public ComboBox<String> getReplaceComboBox() {
		return replaceComboBox;
	}
	
	public Button getFindButton() {
		return findButton;
	}
	
	public Button getPrevButton() {
		return prevButton;
	}
	
	public Button getNextButton() {
		return nextButton;
	}
	
	public Button getCloseButton() {
		return closeButton;
	}
	
	public Button getClearFindButton() {
		return clearFindButton;
	}
	
	public Button getClearReplaceButton() {
		return clearReplaceButton;
	}
	
	public SimpleBooleanProperty caseSensitiveProperty() {
		return caseSensitive;
	}
	
	public SimpleBooleanProperty wholeWordProperty() {
		return wholeWord;
	}
	
	public SimpleBooleanProperty regexSearchProperty() {
		return regexSearch;
	}
	
	public void clearInputs() {
		findTextInput.clearRecord();
		replaceTextInput.clearRecord();
		findTextField.clear();
		replaceTextField.clear();
	}
	
	public void clearOptions() {
		caseSensitive.set(false);
		wholeWord.set(false);
		regexSearch.set(false);
	}
	
	public void showReplace(final boolean isShown) {
		final ObservableList<Node> obsList = getChildren();
		if(isShown) {
			if(!obsList.contains(replaceBox)) {
				obsList.add(replaceBox);
			}
		} else {
			obsList.remove(replaceBox);
		}
	}
	
	public void showMessage(final String text) {
		messageText.setText(text);
		final Bounds buttonBounds = findComboBox.localToScreen(findComboBox.getBoundsInLocal());
		final Runnable task = () -> {
			try {
				Platform.runLater(() -> messagePopup.show(findComboBox, buttonBounds.getMinX(), buttonBounds.getMinY() - Utilities.getRelativeSize(3.2)));
				Thread.sleep(1000);
				Platform.runLater(() -> hideMessage());
			} catch (InterruptedException e) {
				System.err.println(e);
			}
		};
		final Thread t = new Thread(task); 
		t.start();
	}
	
	private void hideMessage() {
		messagePopup.hide();
	}
	
	private void startSearch() {
		if(hostNode instanceof PaliTextEditor) {
			((PaliTextEditor)hostNode).startSearch();
		} else if(hostNode instanceof PaliHtmlViewer) {
			((PaliHtmlViewer)hostNode).startSearch();
		}
	}
	
	private void replace(final boolean isAll) {
		if(hostNode instanceof PaliTextEditor) {
			if(isAll)
				((PaliTextEditor)hostNode).replaceAll();
			else
				((PaliTextEditor)hostNode).replaceFirst();
		}
	}
}
