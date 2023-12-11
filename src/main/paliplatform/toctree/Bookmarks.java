/*
 * Bookmarks.java
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

import java.io.File;

import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import javafx.beans.property.*;

/** 
 * This dialog manages bookmarks of Pali texts. This is a singleton.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class Bookmarks extends SingletonWindow {
	public static final Bookmarks INSTANCE = new Bookmarks();
	private final BorderPane mainPane = new BorderPane();
	private final TableView<PaliDocument> table = new TableView<>();
	private final ToggleButton fullTableButton = new ToggleButton("", new TextIcon("table-cells", TextIcon.IconSet.AWESOME));
	private final SimpleBooleanProperty isPlainText = new SimpleBooleanProperty(false);
	
	private Bookmarks() {
		setTitle("Bookmarks");
		getIcons().add(new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "bookmark.png")));
		
		table.setItems(Utilities.bookmarkList);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		boolean isFullTable = false;
		setupTable(isFullTable);
		// add context menu
		final MenuItem openMenuItem = new MenuItem("Open");
		openMenuItem.setOnAction(actionEvent -> openText());		
		final MenuItem openAsTextMenuItem = new MenuItem("Open as text");
		openAsTextMenuItem.disableProperty().bind(isPlainText);
		openAsTextMenuItem.setOnAction(actionEvent -> openAsText(true));		
		final MenuItem openAsTextNoNotesMenuItem = new MenuItem("Open as text (no notes)");
		openAsTextNoNotesMenuItem.disableProperty().bind(isPlainText);
		openAsTextNoNotesMenuItem.setOnAction(actionEvent -> openAsText(false));		
		final MenuItem removeMenuItem = new MenuItem("Remove bookmark(s)");
		removeMenuItem.setOnAction(actionEvent -> removeBookmarks());
		final MenuItem addToTokenMenuItem = new MenuItem("Add to Tokenizer");
		addToTokenMenuItem.setOnAction(actionEvent -> sendToTokenizer());
		final ContextMenu popupMenu = new ContextMenu(openMenuItem, openAsTextMenuItem, openAsTextNoNotesMenuItem, 
														removeMenuItem, addToTokenMenuItem);
		table.setContextMenu(popupMenu);
		table.getSelectionModel().selectedItemProperty().addListener((prop, oldValue, newValue) -> {
			if(newValue != null) {
				final TOCTreeNode node = newValue.toTOCTreeNode();
				isPlainText.set(node.isPlainText());
			}
		});
		table.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				if(keyEvent.getCode() == KeyCode.ENTER)
					openText();
				else if(keyEvent.getCode() == KeyCode.DELETE)
					removeBookmarks();
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
		
		// add options on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(table);
		// use property to bind with disablility of some buttons
		final SimpleListProperty<PaliDocument> bookmarkListProperty = new SimpleListProperty<>(Utilities.bookmarkList);
		// configure some buttons first
		toolBar.saveTextButton.setTooltip(new Tooltip("Save data as CSV"));
		toolBar.saveTextButton.setOnAction(actionEvent -> saveCSV());		
		toolBar.saveTextButton.disableProperty().bind(bookmarkListProperty.sizeProperty().isEqualTo(0));
		toolBar.copyButton.setTooltip(new Tooltip("Copy CSV to clipboard"));
		toolBar.copyButton.setOnAction(actionEvent -> copyCSV());		
		toolBar.copyButton.disableProperty().bind(bookmarkListProperty.sizeProperty().isEqualTo(0));
		// add new buttons
		final Button openButton = new Button("", new TextIcon("file-lines", TextIcon.IconSet.AWESOME));
		openButton.setTooltip(new Tooltip("Open the selected file"));
		openButton.setOnAction(actionEvent -> openText());
		openButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		final Button addToTokenButton = new Button("", new TextIcon("grip", TextIcon.IconSet.AWESOME));
		addToTokenButton.setTooltip(new Tooltip("Add to Tokenizer"));
		addToTokenButton.setOnAction(actionEvent -> sendToTokenizer());
		addToTokenButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		final Button delButton = new Button("", new TextIcon("trash", TextIcon.IconSet.AWESOME));
		delButton.setTooltip(new Tooltip("Remove the selected bookmark(s)"));
		delButton.setOnAction(actionEvent -> removeBookmarks());
		delButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		final Button refreshButton = new Button("", new TextIcon("arrows-rotate", TextIcon.IconSet.AWESOME));
		refreshButton.setTooltip(new Tooltip("Refresh the table"));
		refreshButton.setOnAction(actionEvent -> refreshTable());
		fullTableButton.setTooltip(new Tooltip("Show full table"));
		fullTableButton.setSelected(isFullTable);
		fullTableButton.disableProperty().bind(bookmarkListProperty.sizeProperty().isEqualTo(0));
		fullTableButton.setOnAction(actionEvent -> refreshTable());		
		toolBar.getItems().addAll(new Separator(), openButton, addToTokenButton, delButton, new Separator(), refreshButton, fullTableButton);
		mainPane.setTop(toolBar);
		
		final Scene scene = new Scene(mainPane, windowWidth, windowHeight);
		setScene(scene);

		// set up drop events
		mainPane.setOnDragOver(dragEvent -> {
			if(dragEvent.getGestureSource() != mainPane && dragEvent.getDragboard().hasString()) {
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			dragEvent.consume();
		});
		mainPane.setOnDragDropped(dragEvent -> {
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
						Utilities.addBookmark(ttnode);
					}
				}
				dragEvent.setDropCompleted(true);
			} else {
				dragEvent.setDropCompleted(false);
			}
			dragEvent.consume();
		});
	}
	
	private void refreshTable() {
		ObservableList<TableColumn<PaliDocument, ?>> cols = table.getColumns();
		for(int i=cols.size()-1; i>=0; i--)
			cols.remove(i);
		setupTable(fullTableButton.isSelected());		
	}
	
	private void setupTable(final boolean isFull) {
		if(Utilities.bookmarkList.isEmpty())
			return;
		final Callback<TableColumn<PaliDocument, String>, TableCell<PaliDocument, String>> textNameCellFactory = col -> {
			TableCell<PaliDocument, String> cell = new TableCell<PaliDocument, String>() {
				@Override
				public void updateItem(final String item, final boolean empty) {
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
		textNameCol.setCellValueFactory(new PropertyValueFactory<>(Utilities.bookmarkList.get(0).textNameProperty().getName()));
		textNameCol.setCellFactory(textNameCellFactory);
		textNameCol.setComparator(PaliDocument.getTextNameStringComparator(0));
		final TableColumn<PaliDocument, String> fileNameCol = new TableColumn<>("File");
		fileNameCol.setCellValueFactory(new PropertyValueFactory<>(Utilities.bookmarkList.get(0).fileNameProperty().getName()));
		fileNameCol.setComparator(PaliDocument.getFileNameStringComparator());
		table.getColumns().add(textNameCol);
		if(isFull) {
			textNameCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(3).subtract(7));
			fileNameCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(2));
			final TableColumn<PaliDocument, String> bookCol = new TableColumn<>("Book");
			bookCol.setCellValueFactory(new PropertyValueFactory<>(Utilities.bookmarkList.get(0).textNameProperty().getName()));
			bookCol.setCellFactory(textNameCellFactory);
			bookCol.setComparator(PaliDocument.getTextNameStringComparator(1));
			bookCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(2));
			final TableColumn<PaliDocument, String> groupCol = new TableColumn<>("Group");
			groupCol.setCellValueFactory(new PropertyValueFactory<>(Utilities.bookmarkList.get(0).textNameProperty().getName()));
			groupCol.setCellFactory(textNameCellFactory);
			groupCol.setComparator(PaliDocument.getTextNameStringComparator(2));
			groupCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(2));
			final TableColumn<PaliDocument, String> inArchiveCol = new TableColumn<>("CSCD");
			inArchiveCol.setCellValueFactory(new PropertyValueFactory<>(Utilities.bookmarkList.get(0).inArchiveProperty().getName()));
			inArchiveCol.prefWidthProperty().bind(mainPane.widthProperty().divide(10).multiply(1));
			inArchiveCol.setStyle("-fx-alignment:center");		
			table.getColumns().add(bookCol);
			table.getColumns().add(groupCol);
			table.getColumns().add(fileNameCol);
			table.getColumns().add(inArchiveCol);
		} else {
			textNameCol.prefWidthProperty().bind(mainPane.widthProperty().divide(5).multiply(3).subtract(7));
			fileNameCol.prefWidthProperty().bind(mainPane.widthProperty().divide(5).multiply(2));
			table.getColumns().add(fileNameCol);
		}
		resetWindowSize(isFull);
	}
	
	private void resetWindowSize(final boolean isFull) {
		if(isFull)
			this.setWidth(windowWidth * 1.8);
		else
			this.setWidth(windowWidth);
	}
	
	private void openText() {
		final PaliDocument pd = table.getSelectionModel().getSelectedItem();
		if(pd == null) return;
		final TOCTreeNode node = pd.toTOCTreeNode();
		if(node.isPlainText()) {
			final File nfile = new File(Utilities.EXTRAPATH + node.getFileName());
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, new File[] { nfile });
		} else {
			PaliPlatform.openPaliHtmlViewer(node);
		}
	}

	private void openAsText(final boolean withNotes) {
		final PaliDocument pd = table.getSelectionModel().getSelectedItem();
		if(pd != null)
			Utilities.openXMLDocAsText(pd.toTOCTreeNode(), withNotes);
	}
	
	private void removeBookmarks() {
		Utilities.removeObservableItems(Utilities.bookmarkList, table.getSelectionModel().getSelectedIndices());
		save();
	}
	
	private void sendToTokenizer() {
		for(final PaliDocument pd : table.getSelectionModel().getSelectedItems())
			PaliPlatform.sendToTokenizer(new TreeItem<TOCTreeNode>(pd.toTOCTreeNode()));
	}

	public void save() {
		final String bookmarkStr = Utilities.bookmarkList.stream()
									.map(pd -> pd.getFileName() + ":" + (pd.getInArchive()?"1":"0") + ";")
									.reduce("", String::concat);
		PaliPlatform.settings.setProperty("bookmarks", bookmarkStr);
		MainProperties.INSTANCE.saveSettings();
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
			if(fullTableButton.isSelected()) {
				result.append(Boolean.toString(pd.getInArchive())).append(Utilities.csvDelimiter);
			}
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}
	
	private void copyCSV() {
		Utilities.copyText(makeCSV());
	}
	
	private void saveCSV() {
		Utilities.saveText(makeCSV(), "bookmarks.csv");
	}
}
