/*
 * TOCTreeSAXHandler.java
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

import javafx.scene.control.TreeItem;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * This handler is used for reading xml data in TOCTree.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
 
class TOCTreeSAXHandler extends DefaultHandler {
	private final TreeItem<TOCTreeNode> baseNode;
	private TreeItem<TOCTreeNode> currNode;

	public TOCTreeSAXHandler(final TreeItem<TOCTreeNode> n) {
		baseNode = n;
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		final TOCTreeNode t = new TOCTreeNode();
		final int attlen = attributes.getLength();
		boolean isLeaf = false;
		int sep;
		String val;
		for (int i = 0; i < attlen; i++) {
			if(attributes.getQName(i).equals("text"))
				t.setTextName(attributes.getValue(i));
			if(attributes.getQName(i).equals("src") || attributes.getQName(i).equals("action")) {
				val = attributes.getValue(i);
				t.setInArchive(val.contains("cscd"));
				sep = val.lastIndexOf('/');
				if(sep != -1)
					val = val.substring(sep+1);
				t.setFileName(val);
				isLeaf = attributes.getQName(i).equals("action");
				t.setIsText(isLeaf);
			}
		}
		if(attlen == 0) {
			currNode = baseNode;
		} else {
			TreeItem<TOCTreeNode> tn = new TreeItem<>(t);
			currNode.getChildren().add(tn);
			currNode = tn;
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		currNode = currNode.getParent();
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		// for test
		//~ String str = (new String(ch, start, length));
		//~ System.out.println(str);
	}

	@Override
	public void error(final SAXParseException e) throws SAXException{
		System.err.println("SAX Error: "+e.getMessage());
	}

	@Override
	public void fatalError(final SAXParseException e) throws SAXException{
		System.err.println("SAX Fatal Error: "+e.getMessage());
	}

	@Override
	public void warning(final SAXParseException e) throws SAXException{
		System.err.println("SAX Warning: "+e.getMessage());
	}
}
