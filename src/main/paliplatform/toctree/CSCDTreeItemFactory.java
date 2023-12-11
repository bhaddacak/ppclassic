/*
 * CSCDTreeItemFactory.java
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

import javafx.scene.control.TreeItem;

import java.io.*;
import java.util.zip.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;

/** 
 * This creates tree nodes of the CSCD collection used in TOCTree.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
class CSCDTreeItemFactory {
	
	public static TreeItem<TOCTreeNode> createCSCDTreeItem() {
		final TreeItem<TOCTreeNode> node = new TreeItem<>(new TOCTreeNode("Chaṭṭha Saṅgāyana CD", "toc0.xml"), new TextIcon("folder", TextIcon.IconSet.AWESOME));
		readXMLAsTreeNode(node);
		for(TreeItem<TOCTreeNode> item : node.getChildren()) {
			readXMLAsTreeNode(item);
		}		
		return node;
	}
	
	public static void readXMLAsTreeNode(final TreeItem<TOCTreeNode> node) {
		try {
			final TOCTreeNode tag = (TOCTreeNode)node.getValue();
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			final SAXParser saxParser = spf.newSAXParser();
			final DefaultHandler handler = new TOCTreeSAXHandler(node);
			if(tag.isInArchive()) {
				final ZipFile zip = new ZipFile(new File(Utilities.ROOTDIR + Utilities.COLLPATH + Utilities.CSCD_ZIP));
				final ZipEntry entry = zip.getEntry(Utilities.CSCD_DIR + tag.getFileName());
				if(entry != null) {
					saxParser.parse(zip.getInputStream(entry), handler);
				}
				zip.close();
			} else {
				saxParser.parse(new File(Utilities.ROOTDIR + Utilities.COLLPATH + tag.getFileName()), handler);
			}
		} catch (SAXException | ParserConfigurationException | IOException e) {
			System.err.println(e);
		}
	}
}
