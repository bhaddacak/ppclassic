/*
 * Sentence.java
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

package paliplatform.viewer;

import paliplatform.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/** 
 * The representation of a Pali sentence, this is used in SentenceManager.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class Sentence {
	private String hash;
	private String text;
	private final Map<String, String> transMap;
	private String edit;
	private String sentDir;
	private File sentFile;


	public Sentence(final String hash) {
		this(hash, "");
	}

	public Sentence(final File file) {
		sentFile = file;
		final String fname = file.getName();
		hash = fname.substring(0, fname.lastIndexOf(".json"));
		transMap = new HashMap<>();
		sentDir = file.getParent() + File.separator;
		load();
	}

	public Sentence(final String hash, final String text) {
		this(hash, text, text);
	}

	public Sentence(final String strHash, final String bareText, final String editText) {
		if(strHash.isEmpty())
			hash = Utilities.MD5Sum(bareText);
		else
			hash = strHash;
		text = bareText;
		edit = editText.replace("--", Utilities.DASH);
		transMap = new HashMap<>();
		sentDir = Utilities.ROOTDIR + Utilities.SENTENCESPATH + Utilities.SENTENCESMAIN;
		sentFile = new File(sentDir + hash + ".json");
	}

	public void setHash(final String hash) {
		this.hash = hash;
	}

	public String getHash() {
		return hash;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public List<String> getTextTokens() {
		final String[] tokens = text.split(Utilities.REX_NON_PALI_PUNC);
		return Arrays.asList(tokens);
	}

	public void setEditText(final String edit) {
		this.edit = edit.replace("--", Utilities.DASH);
	}

	public void restoreEdit() {
		edit = text;
	}

	public String getEditText() {
		return edit;
	}

	public List<String> getEditTokens() {
		final List<String> result = new ArrayList<>();
		final String[] tokens = edit.split(Utilities.REX_NON_PALI_PUNC);
		for(final String token : tokens) {
			// find non '-' start
			int start = 0;
			for(int i = start; i < token.length(); i++) {
				if(token.charAt(i) != '-') {
					start = i;
					break;
				}
			}
			// find non '-' end
			int end = token.length() - 1;
			for(int i = end; i > 0; i--) {
				if(token.charAt(i) != '-') {
					end = i;
					break;
				}
			}
			final String strTmp = token.substring(start, end + 1);
			// separate dash, ? and !
			final String[] strTokens = strTmp.replace(Utilities.DASH, " " + Utilities.DASH + " ")
										.replaceAll("(\\?+)", " ? ")
										.replaceAll("(\\!+)", " ! ")
										.split(" +");
			for(final String s : strTokens)
				if(!s.isEmpty()) result.add(s);
		}
		return result;
	}

	public void addTranslation(final String variant, final String text) {
		transMap.put(variant, text);
	}

	public void addAllTranslations(final Map<String, String> trmap) {
		trmap.forEach((k, v) -> addTranslation(k, v));
	}

	public void setTranslation(final String variant, final String text) {
		transMap.put(variant, text);
	}

	public void removeTranslation(final String variant) {
		if(transMap.containsKey(variant))
			transMap.remove(variant);
	}

	public Map<String, String> getTranslationMap() {
		return transMap;
	}

	public Set<String> getVariantSet() {
		return transMap.keySet();
	}

	public String getFirstVariant() {
		return transMap.keySet().stream().sorted((x, y) -> x.compareTo(y)).findFirst().orElse("");
	}

	public boolean hasVariant(final String variant) {
		final boolean result;
		if(isTranslationEmpty())
			result = false;
		else
			result = transMap.containsKey(variant);
		return result;
	}

	public void renameVariant(final String oldName, final String newName) {
		if(hasVariant(newName)) return;
		final String savText = transMap.get(oldName);
		transMap.remove(oldName);
		transMap.put(newName, savText);
	}

	public void removeVariant(final String varName) {
		transMap.remove(varName);
	}

	public List<String> getTranslationList() {
		return new ArrayList<>(transMap.values());
	}

	public String getTranslation(final String variant) {
	return transMap.get(variant);
	}

	public String getAllTranslations() {
		return transMap.values().stream().collect(Collectors.joining(" "));
	}

	public boolean isTranslationEmpty() {
		return transMap.isEmpty();
	}

	public boolean hasTranslation() {
		return !transMap.isEmpty();
	}

	public boolean isValid() {
		return hash.equals(Utilities.MD5Sum(text));
	}

	public File getFile() {
		return sentFile;
	}

	public String getSentenceDir() {
		return sentDir;
	}

	public void setSentenceDir(final String dir) {
		sentDir = dir;
		sentFile = new File(sentDir + this.hash + ".json");
	}

	public boolean setSentenceDirAndLoad(final String dir) {
		setSentenceDir(dir);
		return load();
	}

	public final boolean load() {
		if(!sentFile.exists())
			return false;
		try {
			final JsonFactory factory = new JsonFactory();
			final JsonParser parser = factory.createParser(sentFile);
			String strVar = "";
			while(!parser.isClosed()) {
				final JsonToken ftoken = parser.nextToken();
				if(ftoken == null) break;
				if(ftoken == JsonToken.FIELD_NAME) {
					final String fldName = parser.getValueAsString();
					if(fldName.equals("text")) {
						final JsonToken vtoken = parser.nextToken();
						if(vtoken != null && vtoken == JsonToken.VALUE_STRING)
							text = parser.getValueAsString();
					} else if(fldName.equals("edit")) {
						final JsonToken vtoken = parser.nextToken();
						if(vtoken != null && vtoken == JsonToken.VALUE_STRING)
							edit = parser.getValueAsString();
					} else if(fldName.equals("variant")) {
						final JsonToken vtoken = parser.nextToken();
						if(vtoken != null && vtoken == JsonToken.VALUE_STRING)
							strVar = parser.getValueAsString();
					} else if(fldName.equals("transtext")) {
						final JsonToken vtoken = parser.nextToken();
						if(vtoken != null && vtoken == JsonToken.VALUE_STRING)
							addTranslation(strVar, parser.getValueAsString());
					}
				}
			} // end while
		} catch(IOException e) {
			System.err.println(e);
		}
		return true;
	}

	public void save(final boolean forceOverWrite) {
		save(sentFile, forceOverWrite);
	}

	public void saveAs(final String newPath, final boolean forceOverWrite) {
		sentDir = newPath;
		sentFile = new File(sentDir + this.hash + ".json");
		save(forceOverWrite);
	}

	public void save(final File file, final boolean forceOverWrite) {
		if(!forceOverWrite && sentFile.exists())
			return;
		try {
			final JsonFactory factory = new JsonFactory();
			final JsonGenerator generator = factory.createGenerator(sentFile, JsonEncoding.UTF8);
			generator.useDefaultPrettyPrinter();
			generator.writeStartObject();
			generator.writeStringField("text", text);
			generator.writeStringField("edit", edit);
			generator.writeFieldName("translations");
			generator.writeStartArray();
			for(final String v : transMap.keySet()) {
				final String t = transMap.get(v);
				generator.writeStartObject();
				generator.writeStringField("variant", v);
				generator.writeStringField("transtext", t);
				generator.writeEndObject();
			}
			generator.writeEndArray();
			generator.writeEndObject();
			generator.close();
		} catch(IOException e) {
			System.err.println(e);
		}
	}

	public boolean equals(final Sentence other) {
		return hash.equals(other.getHash()); 
	}
	
	@Override
	public String toString() {
		final String result;
		final int maxlen = 180;
		if(text.length() > maxlen)
			result = text.substring(0, maxlen) + "...";
		else
			result = text;
		return result;
	}
}
