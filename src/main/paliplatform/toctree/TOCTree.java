/*
 * TOCTree.java
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

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.Arrays;

/** 
 * The TOC (Table Of Contents) tree of the Pali collection.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class TOCTree extends BorderPane {
	public static final String EXTRA = "Extra";
	private TreeView<TOCTreeNode> treeView;
	private final TreeItem<TOCTreeNode> treeBase;
	private final TreeItem<TOCTreeNode> extraNode;
	private final ContextMenu popupMenu = new ContextMenu();
	private final Button openButton = new Button("", new TextIcon("file-lines", TextIcon.IconSet.AWESOME));
	private final Button addBookmarkButton = new Button("", new TextIcon("bookmark-plus", TextIcon.IconSet.CUSTOM));
	private final Button toTokenButton = new Button("", new TextIcon("grip", TextIcon.IconSet.AWESOME));
	private final ToggleButton showFileNameButton = new ToggleButton("", new TextIcon("tags", TextIcon.IconSet.AWESOME));
	private final SimpleBooleanProperty isOpenable = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty isPlainText = new SimpleBooleanProperty(false);
	
	public TOCTree() {
		treeBase = new TreeItem<>(new TOCTreeNode("Pāli literature", ""));
		treeBase.setExpanded(true);
		final TreeItem<TOCTreeNode> cscdNode = CSCDTreeItemFactory.createCSCDTreeItem();
		cscdNode.setExpanded(true);
		treeBase.getChildren().add(cscdNode);
		final TOCTreeNode extra = new TOCTreeNode(EXTRA, "");
		extra.setIsExtra(true);
		extraNode = new TreeItem<>(extra);
		updateExtraNode();
		treeView = new TreeView<>(treeBase);
		treeView.setOnDragDetected(mouseEvent -> {
			final Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);
			final ClipboardContent content = new ClipboardContent();
			final TreeItem titem = treeView.getSelectionModel().getSelectedItem();
			final String head = "::" + this.getClass().getName() + "::\n";
			content.putString(head + treeItemToString(titem));
			db.setContent(content);
			mouseEvent.consume();
		});
		treeView.setOnMouseDragged(mouseEvent -> mouseEvent.setDragDetect(true));
		treeView.setShowRoot(false);
		
		// customize tree cell
		treeView.setCellFactory((TreeView<TOCTreeNode> tv) -> {
			return new TreeCell<TOCTreeNode>() {
				@Override
				public void updateItem(TOCTreeNode item, boolean empty) {
					super.updateItem(item, empty);
					if(empty) {
						this.setText(null);
						this.setGraphic(null);
					} else {
						final TOCTreeNode node = this.getTreeItem().getValue();
						if(showFileNameButton.isSelected())
							this.setText(node.toStringFull());
						else
							this.setText(node.toString());
						final TextIcon icon;
						if(node.isText())
							icon = new TextIcon("file-lines", TextIcon.IconSet.AWESOME);
						else
							icon = new TextIcon("folder", TextIcon.IconSet.AWESOME);
						this.setGraphic(icon);
					}
					this.setDisclosureNode(null);
				}
			};
		});
		
		// add context menus
		final MenuItem openMenuItem = new MenuItem("Open");
		openMenuItem.setOnAction(actionEvent -> openDoc());
		final MenuItem openAsTextMenuItem = new MenuItem("Open as text");
		openAsTextMenuItem.disableProperty().bind(isPlainText);
		openAsTextMenuItem.setOnAction(actionEvent -> openDocAsText(true));
		final MenuItem openAsTextNoNotesMenuItem = new MenuItem("Open as text (no notes)");
		openAsTextNoNotesMenuItem.disableProperty().bind(isPlainText);
		openAsTextNoNotesMenuItem.setOnAction(actionEvent -> openDocAsText(false));
		final MenuItem addBookmarkMenuItem = new MenuItem("Add to Bookmarks");
		addBookmarkMenuItem.setOnAction(actionEvent -> addBookmark());
		final MenuItem addToTokenMenuItem = new MenuItem("Add to Tokenizer");
		addToTokenMenuItem.setOnAction(actionEvent -> sendToTokenizer());
		popupMenu.getItems().addAll(openMenuItem, openAsTextMenuItem, openAsTextNoNotesMenuItem, addBookmarkMenuItem, addToTokenMenuItem);
		
		// add mouse listener
		treeView.setOnMouseClicked(mouseEvent -> selectionHandler(mouseEvent));
		treeView.getSelectionModel().selectedItemProperty().addListener((prop, oldValue, newValue) -> prepareButtons(newValue));
        
        // add key listener
        treeView.setOnKeyPressed(keyEvent -> selectionHandler(keyEvent));
        
		setCenter(treeView);
		
		// add toolbar on the top
		final CommonWorkingToolBar toolBar = new CommonWorkingToolBar(treeView);
		// config some buttons
		toolBar.saveTextButton.setOnAction(actionEvent -> saveText());		
		toolBar.copyButton.setOnAction(actionEvent -> copyText());		
		// add new components
		final Button reloadButton = new Button("", new TextIcon("upload", TextIcon.IconSet.AWESOME));
		reloadButton.setTooltip(new Tooltip("Reload extra collection"));
		reloadButton.setOnAction(actionEvent -> updateExtraNode());
		openButton.setTooltip(new Tooltip("Open the selected file"));
		openButton.disableProperty().bind(isOpenable.not());
		openButton.setOnAction(actionEvent -> openDoc());
		addBookmarkButton.setTooltip(new Tooltip("Bookmark the selected file"));
		addBookmarkButton.disableProperty().bind(isOpenable.not());
		addBookmarkButton.setOnAction(actionEvent -> addBookmark());
		toTokenButton.setTooltip(new Tooltip("Add to Tokenizer"));
		toTokenButton.setOnAction(actionEvent -> sendToTokenizer());
		showFileNameButton.setTooltip(new Tooltip("File names on/off"));
		showFileNameButton.setOnAction(actionEvent -> treeView.refresh());
	
		toolBar.getItems().addAll(new Separator(), reloadButton, openButton, addBookmarkButton, toTokenButton,
								new Separator(), showFileNameButton);
		setTop(toolBar);		
	}

	public TreeView<TOCTreeNode> getTreeView() {
		return treeView;
	}
	
	private void selectionHandler(final InputEvent event) {
		final TreeItem<TOCTreeNode> thisItem = treeView.getSelectionModel().getSelectedItem();
		if(thisItem == null) return;
		final TOCTreeNode thisNode = thisItem.getValue();
		if(event instanceof KeyEvent) {
			// action by keyboard
			final KeyEvent keyEvent = (KeyEvent)event;
			if(keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
				popupMenu.hide();
				if(keyEvent.getCode() == KeyCode.ENTER) {
					if(thisNode.isText()) {
						openDoc(thisNode);
					} else {
						if(thisItem.isLeaf()) {
							// not yet loaded, read from the zip archive
							CSCDTreeItemFactory.readXMLAsTreeNode(thisItem);
							thisItem.setExpanded(true);
						} else {
							thisItem.setExpanded(!thisItem.isExpanded());
						}					
					}
				} else if(keyEvent.getCode() == KeyCode.RIGHT) {
					if(!thisNode.isText()) {
						if(thisItem.isLeaf()) {
							// not yet loaded, read from the zip archive
							CSCDTreeItemFactory.readXMLAsTreeNode(thisItem);
						}					
						thisItem.setExpanded(true);
					}
				} else if(keyEvent.getCode() == KeyCode.LEFT) {
					if(!thisNode.isText())			
						thisItem.setExpanded(false);
				}
			}
		} else {
			// action by mouse
			final MouseEvent mouseEvent = (MouseEvent)event;
			if(mouseEvent != null && thisNode.isText()) {
				popupMenu.show(treeView, mouseEvent.getScreenX(), mouseEvent.getScreenY());
			} else {
				popupMenu.hide();
				if(thisItem.isLeaf()) {
					// not yet loaded, read from the zip archive
					CSCDTreeItemFactory.readXMLAsTreeNode(thisItem);
					thisItem.setExpanded(true);
				} else {
					thisItem.setExpanded(!thisItem.isExpanded());
				}
			}			
		}
	}
	
	private void prepareButtons(final TreeItem<TOCTreeNode> titem) {
		if(titem == null) return;
		final TOCTreeNode ttnode = titem.getValue();
		isOpenable.set(ttnode.isText());
		isPlainText.set(ttnode.isPlainText());
	}
	
	private void updateExtraNode() {
		final ObservableList<TreeItem<TOCTreeNode>> nodes = treeBase.getChildren();
		extraNode.getChildren().clear();
		final int extraCount = getExtraFiles(extraNode);
		if(extraCount > 0) {
			if(!nodes.contains(extraNode))
				nodes.add(extraNode);
		} else {
			if(nodes.contains(extraNode))
				nodes.remove(extraNode);
		}
	}

	public static int getExtraFiles(final TreeItem<TOCTreeNode> node) {
		final File extraDir = new File(Utilities.EXTRAPATH);
		if(!extraDir.exists()) return 0;
		final File[] files = extraDir.listFiles(x -> {
				final String fname = x.getName().toLowerCase();
				return fname.endsWith(".xml") || fname.endsWith(".txt");
		});
		if(files.length >= 0) {
			Arrays.sort(files, (x, y) -> x.getName().compareTo(y.getName()));
			for(final File f : files) {
				final String fname = f.getName();
				final TOCTreeNode tag = new TOCTreeNode(fname, fname);
				tag.setIsExtra(true);
				tag.setIsText(true);
				final TreeItem<TOCTreeNode> item = new TreeItem<TOCTreeNode>(tag);
				node.getChildren().add(item);
			}
		}
		return files.length;
	}

	private void openDoc() {
		final TOCTreeNode selected = treeView.getSelectionModel().getSelectedItem().getValue();
		openDoc(selected);
	}

	private void openDoc(final TOCTreeNode node) {
		if(node.isPlainText()) {
			final File nfile = new File(Utilities.EXTRAPATH + node.getFileName());
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, new File[] { nfile });
		} else {
			PaliPlatform.openPaliHtmlViewer(node);
		}
	}
	
	private void openDocAsText(final boolean withNotes) {
		Utilities.openXMLDocAsText(treeView.getSelectionModel().getSelectedItem().getValue(), withNotes);
	}

	private void addBookmark() {
		Utilities.addBookmark(treeView.getSelectionModel().getSelectedItem().getValue());
		Bookmarks.INSTANCE.save();
	}

	private void sendToTokenizer() {
		final TreeItem<TOCTreeNode> titem = treeView.getSelectionModel().getSelectedItem();
		PaliPlatform.sendToTokenizer(titem);
	}

	// Convert TreeItem to String, used in drag & drop action
	private String treeItemToString(final TreeItem titem) {
		if(titem == null) return "";
		final TOCTreeNode ttn = (TOCTreeNode)titem.getValue();
		if(ttn == null) return "";
		final StringBuilder result = new StringBuilder();
		if(ttn.getFileName().isEmpty()) {
			// if head node, read its children
			for(final Object obj : titem.getChildren())
				result.append(treeItemToString((TreeItem)obj));
		} else {
			final String ar = ttn.isInArchive() ? "1" : "0";
			final String ex = ttn.isExtra() ? "1" : "0";
			final String tx = ttn.isText() ? "1" : "0";
			final String tname = ttn.getTextName();
			final String fname = ttn.getFileName();
			result.append(ar).append(":");
			result.append(ex).append(":");
			result.append(tx).append(":");
			result.append(tname).append(":");
			result.append(fname).append("\n");
		}
		return result.toString();
	}
	
	private String makeText() {
		final StringBuilder result = new StringBuilder();
		int row = 0;
		TreeItem<TOCTreeNode> node = null;
		do {
			node = treeView.getTreeItem(row);
			if(node != null) {
				final int tabCount = treeView.getTreeItemLevel(node) - 1;
				final TOCTreeNode ttn = node.getValue();
				final String text = showFileNameButton.isSelected() ? ttn.toStringFull() : ttn.toString();
				result.append("\t".repeat(tabCount)).append(text);
				result.append(System.getProperty("line.separator"));
			}
			row++;
		} while(node != null);
		return result.toString();
	}
	
	private void copyText() {
		Utilities.copyText(makeText());
	}
	
	private void saveText() {
		Utilities.saveText(makeText(), "toctree.txt");
	}
}
