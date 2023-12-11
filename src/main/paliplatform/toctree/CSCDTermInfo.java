/*
 * CSCDTermInfo.java
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

import java.util.*;

/** 
 * This class manages term's information (frequency, etc.), mainly used by Tokenizer.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
 
public class CSCDTermInfo {
	public static enum Field {
		BODYTEXT, CENTRE, INDENT, UNINDENTED,
		NIKAYA, BOOK, CHAPTER, TITLE, SUBHEAD, SUBSUBHEAD,
		GATHA1, GATHA2, GATHA3, GATHALAST,
		NOTE, BOLD; // excluding PARANUM, HANGNUM, PB, and DOT
		public String getTag() {
			return this.toString().toLowerCase();
		}
		public static boolean isValid(final String tag) {
			boolean result = false;
			final String test = tag.toUpperCase();
			for(final Field f : Field.values()) {
				if(f.toString().equals(test)) {
					result = true;
					break;
				}
			}
			return result;
		}
	}
	private final String term;
	private final Map<Field, List<int[]>> postingMap = new EnumMap<>(Field.class);

	public CSCDTermInfo(final String term) {
		this.term = term;
	}

	public String getTerm() {
		return term;
	}

	public void addPosting(final Field fld, final List<int[]> pst) {
		postingMap.put(fld, pst);
	}

	public Map<Field, List<int[]>> getPostingMap() {
		return postingMap;
	}

	public List<int[]> getPositionList(final Field field) {
		return postingMap.get(field);
	}
}
