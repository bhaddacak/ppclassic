/*
 * TermFreqProp.java
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

import javafx.beans.property.*;

/** 
 * This class is used in table display of Tokenizer.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
 
public final class TermFreqProp {
	private StringProperty term;
	private IntegerProperty freq;
	private IntegerProperty capFreq; // for capitalized term
	private IntegerProperty totalFreq;
	private DoubleProperty capPercent;
	private final boolean isCap;

	public TermFreqProp(final String term, final int freq, final int capFreq) {
		termProperty().set(term);
		freqProperty().set(freq);
		capFreqProperty().set(capFreq);
		final int tot = freq + capFreq;
		totalFreqProperty().set(tot);
		capPercentProperty().set(capFreq*100.0/tot);
		isCap = Character.isUpperCase(term.charAt(0));
	}

	public StringProperty termProperty() {
		if(term == null)
			term = new SimpleStringProperty(this, "term");
		return term;
	}

	public IntegerProperty freqProperty() {
		if(freq == null)
			freq = new SimpleIntegerProperty(this, "freq");
		return freq;
	}

	public IntegerProperty capFreqProperty() {
		if(capFreq == null)
			capFreq = new SimpleIntegerProperty(this, "capFreq");
		return capFreq;
	}

	public IntegerProperty totalFreqProperty() {
		if(totalFreq == null)
			totalFreq = new SimpleIntegerProperty(this, "totalFreq");
		return totalFreq;
	}

	public DoubleProperty capPercentProperty() {
		if(capPercent == null)
			capPercent = new SimpleDoubleProperty(this, "capPercent");
		return capPercent;
	}

	public boolean isCapitalized() {
		return isCap;
	}

	public void addUpFreq(final TermFreq tf) {
		final int f = freqProperty().get() + tf.getFreq();
		freqProperty().set(f);
		final int capf = capFreqProperty().get() + tf.getCapFreq();
		capFreqProperty().set(capf);
		totalFreqProperty().set(f + capf);
	}
}
