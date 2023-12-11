/*
 * TermWeight.java
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
import static java.lang.Math.log10;

/** 
 * Term weighting is used for search upon the index created.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
 
public class TermWeight {
	private final String term;
	private final Map<CSCDTermInfo.Field, Integer> docCountMap; // the number of docs having this term
	private final Map<TOCTreeNode, Map<CSCDTermInfo.Field, Integer>> tfMap;
	private final Map<TOCTreeNode, Map<CSCDTermInfo.Field, Double>> weightMap;
	private final Map<CSCDTermInfo.Field, Double> queryWeightMap; // in case of this term is query

	public TermWeight(final String term) {
		this.term = term;
		docCountMap = new EnumMap<>(CSCDTermInfo.Field.class);
		tfMap = new HashMap<>();
		weightMap = new HashMap<>();
		queryWeightMap = new EnumMap<>(CSCDTermInfo.Field.class);
	}

	public String getTerm() {
		return term;
	}

	public void increaseDocCount(final CSCDTermInfo.Field field) {
		final int n;
		if(docCountMap.containsKey(field))
			n = docCountMap.get(field) + 1;
		else
			n = 1;
		docCountMap.put(field, n);
	}

	public void addTF(final TOCTreeNode ttn, final CSCDTermInfo.Field field, final int tf) {
		final Map<CSCDTermInfo.Field, Integer> map;
		if(tfMap.containsKey(ttn))
			map = tfMap.get(ttn);
		else
			map = new EnumMap<>(CSCDTermInfo.Field.class);
		map.put(field, tf);
		tfMap.put(ttn, map);
	}

	public void computeWeight(final List<TOCTreeNode> ttnList, final long totDocs) {
		for(final TOCTreeNode ttn : ttnList) {
			if(tfMap.containsKey(ttn)) {
				final Map<CSCDTermInfo.Field, Integer> fmap = tfMap.get(ttn);
				final Map<CSCDTermInfo.Field, Double> wmap = new EnumMap<>(CSCDTermInfo.Field.class);
				docCountMap.forEach((fld, num) -> {
					if(num > 0 && fmap.containsKey(fld)) {
						final double idf = log10(totDocs/num);
						final double w = logTFIDF(fmap.get(fld), idf);
						wmap.put(fld, w);
					}
				});
				weightMap.put(ttn, wmap);
			}
		}
	}

	public void computeQueryWeight(final long totDocs) {
		final int tf = 1; // term in query has frequency = 1
		docCountMap.forEach((fld, num) -> {
			if(num > 0) {
				final double idf = log10(totDocs/num);
				final double w = logTFIDF(tf, idf);
				queryWeightMap.put(fld, w);
			}
		});
	}

	public Map<CSCDTermInfo.Field, Double> getQueryWeightMap() {
		return queryWeightMap;
	}

	public void resetDocScores() {
		weightMap.keySet().forEach(x -> {
			x.setSearchScore(0.0);
			x.setMaxQueryFound(0);
		});
	}

	public Map<TOCTreeNode, Map<CSCDTermInfo.Field, Double>> getSimScores(final TermWeight qterm) {
		final Map<CSCDTermInfo.Field, Double> qwmap = qterm.getQueryWeightMap();
		final Map<TOCTreeNode, Map<CSCDTermInfo.Field, Double>> result = new HashMap<>();
		weightMap.forEach((doc, wmap) -> {
			final Map<CSCDTermInfo.Field, Double> wres = new EnumMap<>(CSCDTermInfo.Field.class);
			wmap.forEach((fld, w) -> {
				final double qw = qwmap.containsKey(fld) ? qwmap.get(fld) : 0.0;
				wres.put(fld, w * qw);
			});
			result.put(doc, wres);
		});
		return result;
	}
	
	/*
	private double simpleTFIDF(final int tf, final double idf) {
		return tf * idf;
	}
	*/
	
	private double logTFIDF(final int tf, final double idf) {
		final double ltf = tf > 0 ? 1 + log10(tf) : 0;
		return ltf * idf;
	}

	@Override
	public String toString() {
		return term;
	}
}
