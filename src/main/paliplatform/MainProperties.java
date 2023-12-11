/*
 * MainProperties.java
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

import java.util.*;
import java.io.*;
import java.util.Properties;

import javafx.scene.control.Tab;
import javafx.collections.ObservableList;

/** 
 * This manages the program's properties. This class is a singleton.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class MainProperties {
	public static final MainProperties INSTANCE = new MainProperties();
	private static final String PROPERTIES_FILE = "PPClassic.properties";
	private static final double DEFAULT_WIDTH = 870;
	private static final double DEFAULT_HEIGHT = 500;
	private final Properties settings;
	private final File propertiesFile;
	public static enum PaliInputProperties {
		INSTANCE;
		private final Hashtable<String, String> defPaliInputTable = new Hashtable<>();
		private final String[] unusedCharNames = { "a macron (ā)", "i macron (ī)", "u macron (ū)", "n dot above (ṅ)", "n tilde (ñ)",
								"t dot below (ṭ)", "d dot below (ḍ)", "n dot below (ṇ)", "l dot below (ḷ)", "m dot below (ṃ)",
								"uppercase key", "lowercase key" };
		private final String[] unusedCharKeys = { "uc-ā", "uc-ī", "uc-ū", "uc-ṅ", "uc-ñ", "uc-ṭ", "uc-ḍ", "uc-ṇ", "uc-ḷ", "uc-ṃ", "uc-upper", "uc-lower" };
		
		private final String[] compCharNames = { "tilde (o"+'\u0303'+")", "macron (o"+'\u0304'+")", "dot above (o"+'\u0307'+")", "dot below (o"+'\u0323'+")" };
		private final String[] compCharKeys = { "co-" + '\u0303', "co-" + '\u0304', "co-" + '\u0307', "co-" + '\u0323' };
		private PaliInputProperties() {
			String[] unusedCharVals = { "x", "X", "W", "F", "f", "q", "Q", "z", "Z", "w", "<", ">" };
			// for unused-character input method
			for(int i = 0; i<unusedCharKeys.length; i++)
				defPaliInputTable.put(unusedCharKeys[i], unusedCharVals[i]);
			// for composite-character input method
			String[] compCharVals = { "~", "-", "'", "." };
			for(int i = 0; i<compCharKeys.length; i++)
				defPaliInputTable.put(compCharKeys[i], compCharVals[i]);		
		}
		public Hashtable<String, String> getDefaultTable() {
			return defPaliInputTable;
		}
		public String[] getUnusedCharNames() {
			return unusedCharNames;
		}
		public String[] getUnusedCharKeys() {
			return unusedCharKeys;
		}
		public String[] getCompCharNames() {
			return compCharNames;
		}
		public String[] getCompCharKeys() {
			return compCharKeys;
		}
	}
	
	private MainProperties() {
		propertiesFile = new File(".", PROPERTIES_FILE);
		settings = new Properties();
		if (propertiesFile.exists()){
			try (final InputStream in = new FileInputStream(propertiesFile)) {
				settings.load(in);
			} catch (IOException e) {
				System.err.println(e);
			}
		} else {
			//compose the default settings
			settings.setProperty("width", "" + DEFAULT_WIDTH);
			settings.setProperty("height", "" + DEFAULT_HEIGHT);
			settings.setProperty("theme", "LIGHT");
			settings.setProperty("exit-ask", "true");
			settings.setProperty("pali-input-method", PaliTextInput.InputMethod.UNUSED_CHARS.toString());
			settings.setProperty("thai-alt-chars", "false");
			settings.setProperty("bookmarks", "vin04t.nrf0.xml:1;vin04t.nrf1.xml:1");
			settings.setProperty("taborder", "TOCTREE,FINDER,LISTER,TOKEN,DICT");
			settings.setProperty("dictset", "CPED,PTSD,");
			settings.setProperty("extrapath", Utilities.EXTRAPATH);
			// set up default Pali input method properties
			Hashtable<String,String> defTable = PaliInputProperties.INSTANCE.getDefaultTable();
			for(Enumeration<String> k = defTable.keys(); k.hasMoreElements();) {
				final String key = k.nextElement();
				settings.setProperty(key, defTable.get(key));
			}
		}
	}
	
	public Properties getSettings() {
		return settings;
	}
	
	public void saveSettings(final double width, final double height, final ObservableList<Tab> tabList) {
		settings.setProperty("width", "" + width);
		settings.setProperty("height", "" + height);
		String tabOrder = "";
		for(final Tab t : tabList) {
			PaliPlatform.WindowType wt = PaliPlatform.WindowType.from(t.getContent().getClass());
			tabOrder += wt.toString() + ",";
		}
		settings.setProperty("taborder", tabOrder);
		saveSettings();
	}
	
	public void saveSettings() {
		try (final OutputStream out = new FileOutputStream(propertiesFile)) {
			settings.store(out, "Properties of Pali Platform 2");
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}
