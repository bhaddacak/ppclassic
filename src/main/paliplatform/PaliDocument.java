/*
 * PaliDocument.java
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

import paliplatform.toctree.TOCTreeNode;

import java.util.Comparator;

import javafx.beans.property.*;

/**
 * The class of a Pali document used mainly for TableView in Bookmarks, Document Finder, etc.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */

public final class PaliDocument {
	private StringProperty textName;
	private StringProperty fileName;
	private StringProperty inArchive;
	private IntegerProperty searchResultCount;

	public PaliDocument(final String textName, final String fileName) {
		setTextName(textName);
		setFileName(fileName);
		setInArchive(true);
		searchResultCountProperty().set(0);
	}

	public PaliDocument(final String textName, final String fileName, final boolean inArchive) {
		setTextName(textName);
		setFileName(fileName);
		setInArchive(inArchive);
	}

	public StringProperty textNameProperty() {
		if(textName == null)
			textName = new SimpleStringProperty(this, "textName");
		return textName;
	}
	
	public void setTextName(final String value) {
		textNameProperty().set(value);
	}

	public String getTextName() {
		return textNameProperty().get();
	}

	public StringProperty fileNameProperty() {
		if(fileName == null)
			fileName = new SimpleStringProperty(this, "fileName");
		return fileName;
	}
	
	public void setFileName(final String value) {
		fileNameProperty().set(value);
	}

	public String getFileName() {
		return fileNameProperty().get();
	}

	public StringProperty inArchiveProperty() {
		if(inArchive == null)
			inArchive = new SimpleStringProperty(this, "inArchive");
		return inArchive;
	}
	
	public void setInArchive(final boolean value) {
		inArchiveProperty().set(value?"✔":"");
	}

	public boolean getInArchive() {
		return !inArchiveProperty().get().isEmpty();
	}
	
	public IntegerProperty searchResultCountProperty() {
		if(searchResultCount == null)
			searchResultCount = new SimpleIntegerProperty(this, "searchResultCount");
		return searchResultCount;
	}
	
	public TOCTreeNode toTOCTreeNode() {
		final boolean inAchv = getInArchive();
		final TOCTreeNode ttn = new TOCTreeNode(getTextName(), getFileName(), inAchv);
		ttn.setIsExtra(!inAchv);
		ttn.setIsText(true);
		return ttn;
	}
	
	public boolean equals(final PaliDocument other) {
		return fileName.get().equals(other.getFileName());
	}
	
	public static int compareFileName(final String aName, final String bName) {
		// a text's file name can be divided into 4 chunks, e.g. vin02m|1.mul|10
		final String[] thisName = aName.split("\\."); // thisName[2] = 'xml' (ignored)
		final String thisNameText = thisName[0].substring(0, 6);
		final int thisNameNum = thisName[0].length()>6?Integer.parseInt(thisName[0].substring(6)):0;
		final String thisExtText = thisName[1].substring(0, 3);
		final int thisExtNum = thisName[1].length()>3?Integer.parseInt(thisName[1].substring(3)):0;
		final String[] thatName = bName.split("\\.");
		final String thatNameText = thatName[0].substring(0, 6);
		final int thatNameNum = thatName[0].length()>6?Integer.parseInt(thatName[0].substring(6)):0;
		final String thatExtText = thatName[1].substring(0, 3);
		final int thatExtNum = thatName[1].length()>3?Integer.parseInt(thatName[1].substring(3)):0;
		int result = PaliPlatform.cscdFileNameCollator.compare(thisNameText, thatNameText);
		if(result == 0) {
			result = Integer.compare(thisNameNum, thatNameNum);
			if(result == 0) {
				result = PaliPlatform.cscdFileNameCollator.compare(thisExtText, thatExtText);
				if(result == 0) {
					return Integer.compare(thisExtNum, thatExtNum);
				} else {
					return result;
				}
			} else {
				return result;
			}
		} else {
			return result;
		}
	}

	private static int compareTextName(final String aTName, final String bTName, final int level) {
		// text name can be in full from, e.g. Level0:Level1:Level2
		// in table displays, each level is put into a column, namely, for example, Text, Book, and Group
		final String[] aTextName = aTName.split(":");
		final String[] bTextName = bTName.split(":");
		final String aName = aTextName[level]==null?"":aTextName[level];
		final String bName = bTextName[level]==null?"":bTextName[level];
		// ignore leading numbers
		int ind = 0;
		while(!Character.isLetter(aName.charAt(ind)))
			ind++;
		final String aNameFinal = aName.substring(ind);
		ind = 0;
		while(!Character.isLetter(bName.charAt(ind)))
			ind++;
		final String bNameFinal = bName.substring(ind);
		return PaliPlatform.paliCollator.compare(aNameFinal, bNameFinal);
	}
	
	public static Comparator<PaliDocument> getTextNameComparator() {
		return getTextNameComparator(0);
	}
	
	public static Comparator<PaliDocument> getTextNameComparator(final int level) {
		return (a, b) -> compareTextName(a.getTextName(), b.getTextName(), level);
	}
	
	public static Comparator<String> getTextNameStringComparator(final int level) {
		return (a, b) -> compareTextName(a, b, level);
	}
	
	public static Comparator<PaliDocument> getFileNameComparator() {
		return (a, b) -> compareFileName(a.getFileName(), b.getFileName());
	}
	
	public static Comparator<String> getFileNameStringComparator() {
		return (a, b) -> compareFileName(a, b);
	}
}
