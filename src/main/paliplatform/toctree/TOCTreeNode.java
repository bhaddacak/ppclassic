/*
 * TOCTreeNode.java
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

/** 
 * The TOC tree node, used mainly in CSCD TOC Tree and Tokenizer.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
 
public class TOCTreeNode {
	private String textName = "";
	private String fileName = "";
	private boolean inArchive = false;
	private boolean isExtra = false;
	private boolean isText = false; // Text means the node is a text not a toc 
	private boolean isPlainText = false; // Plain text means the node is in plain text format (extra files)
	private Tokenizer.ProcessStatus processStatus = Tokenizer.ProcessStatus.UNPROCESSED; 
	private Map<String, CSCDTermInfo> termsMap = new HashMap<>();
	private double searchScore = 0.0;
	private int maxQueryFound = 0;

	public TOCTreeNode() {
	}

	public TOCTreeNode(final String t, final String f) {
		textName = t;
		fileName = f==null ? "" : f;
		isPlainText = fileName.toLowerCase().endsWith(".txt");
	}
	
	public TOCTreeNode(final String t, final String f, final boolean inArchive) {
		this(t, f);
		this.inArchive = inArchive;
	}

	public TOCTreeNode(final String t, final String f, final boolean inArchive, final boolean isExtra, final boolean isText) {
		this(t, f);
		this.inArchive = inArchive;
		this.isExtra = isExtra;
		this.isText = isText;
	}

	public String getTextName() {
		return textName;
	}

	public String getFileName() {
		return fileName;
	}

	public boolean isInArchive() {
		return inArchive;
	}

	public boolean isExtra() {
		return isExtra;
	}

	public boolean isText() {
		return isText;
	}

	public boolean isPlainText() {
		return isPlainText;
	}

	public void setTextName(final String t) {
		textName = t;
	}

	public void setFileName(final String f) {
		fileName = f==null ? "" : f;
	}

	public void setInArchive(final boolean b) {
		inArchive = b;
	}

	public void setIsExtra(final boolean b) {
		isExtra = b;
	}

	public void setIsText(final boolean b) {
		isText = b;
	}

	public TOCTreeNode unprocessed() {
		processStatus = Tokenizer.ProcessStatus.UNPROCESSED;
		return this;
	}

	public void setProcessStatus(final Tokenizer.ProcessStatus status) {
		processStatus = status;
	}

	public Tokenizer.ProcessStatus getProcessStatus() {
		return processStatus;
	}

	public PaliDocument toPaliDocument() {
		return new PaliDocument(textName, fileName, inArchive);
	}

	public Map<String, CSCDTermInfo> getTermsMap() {
		return termsMap;
	}

	public void setTermsMap(final Map<String, CSCDTermInfo> map) {
		termsMap = map;
	}

	public CSCDTermInfo getTermInfo(final String term) {
		return termsMap.get(term);
	}

	public void addTerm(final CSCDTermInfo tinfo) {
		termsMap.put(tinfo.getTerm(), tinfo);
	}

	public double getSearchScore() {
		return searchScore;
	}

	public void setSearchScore(final double score) {
		searchScore = score;
	}

	public void addSearchScore(final double score) {
		searchScore += score;
	}

	public int getMaxQueryFound() {
		return maxQueryFound;
	}

	public void setMaxQueryFound(final int num) {
		maxQueryFound = num;
	}
	
	public String toStringFull() {
		String str = textName;
		if(!isExtra && fileName.length() > 0)
			str += " [" + fileName +"]";
		return str;		
	}
	
	@Override
	public String toString() {
		return textName;
	}

	@Override
	public final boolean equals(final Object otherObject) {
        if (this == otherObject) return true;
        if (!(otherObject instanceof TOCTreeNode)) return false;
        final TOCTreeNode other = (TOCTreeNode)otherObject;
        return inArchive == other.inArchive
			&& isExtra == other.isExtra
			&& isText == other.isText
			&& fileName.equals(other.getFileName());
    }

	@Override
	public final int hashCode() {
		return Objects.hash(fileName, Boolean.valueOf(inArchive), Boolean.valueOf(isExtra), Boolean.valueOf(isText));
	}

	@Override
	public final TOCTreeNode clone() {
		final TOCTreeNode ttn = new TOCTreeNode(this.getTextName(), this.getFileName(), this.isInArchive(), this.isExtra(), this.isText());
		return ttn;
	}
}
