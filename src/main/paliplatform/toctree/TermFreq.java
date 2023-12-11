/*
 * TermFreq.java
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

/** 
 * This is the result of document processing, mainly used for indexing.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
 
public class TermFreq {
	private final String term;
	private int freq;
	private int capFreq; // for capitalized term
	private int totalFreq;
	private final boolean isCap;
	private double capPercent;
	private final CSCDTermInfo.Field field;

	public TermFreq(final String term, final int freq, final CSCDTermInfo.Field field) {
		this.term = term;
		this.freq = freq;
		totalFreq = freq;
		capFreq = 0;
		capPercent = 0.0;
		isCap = Character.isUpperCase(term.charAt(0));
		this.field = field;
	}

	public String getTerm() {
		return term;
	}

	public int getFreq() {
		return freq;
	}

	public int getTotalFreq() {
		return totalFreq;
	}

	public int getCapFreq() {
		return capFreq;
	}
	public double getCapPercent() {
		return capPercent;
	}

	public void setCapFreq(final int f) {
		capFreq = f;
		totalFreq += f;
		capPercent = capFreq*100.0/totalFreq;
	}

	public void addUpFreq(final int f) {
		freq += f;
	}

	public boolean isCapitalized() {
		return isCap;
	}

	public CSCDTermInfo.Field getField() {
		return field;
	}

	@Override
	public String toString() {
		return term;
	}
}
