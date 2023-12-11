/*
 * Utilities.java
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

import paliplatform.toctree.*;
import paliplatform.grammar.*;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import java.sql.*;
import java.security.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.geometry.*;

import javafx.embed.swing.SwingFXUtils;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import static javafx.stage.FileChooser.ExtensionFilter;

/** 
 * The method factory for various uses, including the common constants.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
final public class Utilities {
	public static final String VERSION = "2.1";
	public static String ROOTDIR = "";
	public static String DB_URL = "";
	public static String EXTRAPATH = "";
	public static final String IMGDIR = "/resources/images/";
	public static final String FONTDIR = "/resources/fonts/";
	public static final String CSSDIR = "/resources/styles/";
	public static final String TXTDIR = "/resources/text/";
	public static final String JSDIR = "/resources/js/";
	public static final String DATAPATH = "data" + File.separator;
	public static final String COLLPATH = DATAPATH + "collection" + File.separator;
	public static final String RULESPATH = DATAPATH + "rules" + File.separator;
	public static final String SENTENCESPATH = DATAPATH + "sentences" + File.separator;
	public static final String SENTENCESMAIN = "main" + File.separator;
	public static final String INDEXPATH = DATAPATH + "index" + File.separator;
	public static final String INDEXMAIN = "main" + File.separator;
	public static final String EXFONTPATH = "fonts" + File.separator;
	public static final String CSCD_XSL = CSSDIR + "cscd.xsl";
	public static final String CSCD_CSS = CSSDIR + "cscd.css";
	public static final String DICT_CSS = CSSDIR + "dict.css";
	public static final String CSCD_INFO = "cscd-info.csv";
	public static final String RULES_DICT = "dict.txt";
	public static final String RULES_SANDHI = "sandhi.txt";
	public static final String RULES_STOPWORDS = "stopwords.txt";
	public static final String COMMON_JS = JSDIR + "viewer-common.js";
	public static final String CSCD_JS = JSDIR + "cscd-viewer.js";
	public static final String CSCD_ZIP = "romn_utf8.zip";
	public static final String CSCD_DIR = "romn_utf8/";
	public static final String CSCD_FILES = TXTDIR + "cscdfiles.txt";
	public static final String CPED_TERMS = TXTDIR + "cped-terms.txt";
	public static final String DECLINABLES = TXTDIR + "declinables.txt";
	public static final String PARADIGM_NOUN_LIST = TXTDIR + "paradn.csv";
	public static final String PARADIGM_VERB_LIST = TXTDIR + "paradv.csv";
	public static final String PALI_PRONOUN_LIST = TXTDIR + "pronouns.csv";
	public static final String PALI_NUMERAL_LIST = TXTDIR + "numerals.csv";
	public static final String PALI_ROOT_LIST = TXTDIR + "paliroots.csv";
	public static final String PALI_COMMON_VERB_LIST = TXTDIR + "vcommon.csv";
	public static final String PROSODY = TXTDIR + "prosody.csv";
	public static final String TEXCONV = TXTDIR + "texconv.csv";
	public static final String FONTICON = "PaliPlatformIcons";
	public static final String FONTAWESOME = "Font Awesome 6 Free Solid";
	public static final String FONT_FALLBACK = "sans-serif";
	public static String FONTSERIF = FONT_FALLBACK;
	public static String FONTSANS = FONT_FALLBACK;
	public static String FONTMONO = FONT_FALLBACK;
	public static String FONTMONOBOLD = FONT_FALLBACK;
	public static final String PALI_ALL_CHARS = "ÑĀĪŊŚŪḌḤḶḸṂṄṆṚṜṢṬñāīŋśūḍḥḷḹṃṅṇṛṝṣṭ";
	public static final String REX_NON_PALI = "[^A-Za-z" + PALI_ALL_CHARS + "]+";
	public static final String REX_NON_PALI_NUM = "[^A-Za-z0-9" + PALI_ALL_CHARS + "]+";
	public static final String REX_NON_PALI_PUNC = "[^A-Za-z" + PALI_ALL_CHARS + "?!–-]+";
	public static final String PALI_NOUN_ENDINGS = "aāiīuū";
	public static final String PALI_VOWELS = "aāiīuūeo";
	public static final String PALI_LAHU_VOWELS = "aiu";
	public static final String PALI_LONG_VOWELS = "āīū";
	public static final String PALI_CONSONANTS = "kgṅcjñṭḍṇtdnpbmyrlvshḷ";
	public static final String WITH_H_CHARS = "bcdgjkptḍṭ";
	public static final String DASH = "–";
	public static String csvDelimiter = ";";
	public static final Map<PaliScript, Set<String>> paliFontMap = new EnumMap<>(PaliScript.class); 
	public static final Map<String, DocInfo> docInfoMap = new HashMap<>();
	public static final ObservableList<PaliDocument> bookmarkList = FXCollections.<PaliDocument>observableArrayList();
	public static final Map<PaliTextInput.InputMethod, HashMap<String, String>> paliInputCharMap = new EnumMap<>(PaliTextInput.InputMethod.class);
	public static java.sql.Connection dbConn;
	public static PaliDeclension declension;
	public static final List<String> cscdFiles = new ArrayList<>();
	public static final List<String> cpedTerms = new ArrayList<>();
	public static final List<String> declinables = new ArrayList<>();
	public static final Map<String, PaliWord> paliPronouns = new LinkedHashMap<>(30);
	public static final Map<String, PaliWord> paliNumerals = new LinkedHashMap<>(100);
	public static final Map<String, PaliWord> paliOrdinals = new LinkedHashMap<>(10);
	public static final Map<String, PaliWord> paliIrrNouns = new HashMap<>(160);
	public static final Map<String, List<String>> paliCardinalMap = new LinkedHashMap<>();
	public static final Map<String, List<String>> paliOrdinalMap = new LinkedHashMap<>();	
	public static final Map<Integer, PaliRoot> paliRoots = new HashMap<>();
	public static final Map<String, List<String>> sandhiListMap = new HashMap<>();
	public static final Map<String, DeclinedWord> declPronounsMap = new HashMap<>();
	public static final Map<String, DeclinedWord> declNumbersMap = new HashMap<>();
	public static final Map<String, DeclinedWord> declIrrNounsMap = new HashMap<>();
	public static final Map<Character, String> meterPatternMap = new HashMap<>();
	public static final Map<Character, List<String>> texConvMap = new HashMap<>();
	public static final Set<String> stopwords = new HashSet<>();
	public static File customDictFile;
	public static File sandhiFile;
	public static File stopwordsFile;
	public static double defBaseFontSize;
	public static enum PaliScript {
		UNKNOWN, ROMAN, DEVANAGARI, KHMER, MYANMAR, SINHALA, THAI;
		public String getName() {
			final String name = this.toString();
			return name.charAt(0) + name.substring(1).toLowerCase();
		}
	}

	/**
	 * Calculates the size relative to default base font size.
	 * By this, using absolute sizes can be avoided.
	 * The base size is adjusted automatically according to screen size.
	 */
	public static double getRelativeSize(final double scale) {
		return defBaseFontSize*scale;
	}

	/**
	 * Loads external fonts provided, used for non-Roman scripts.
	 */
	public static void loadExternalFonts() throws Exception {
		final File fontdir = new File(ROOTDIR + EXFONTPATH);
		if(fontdir.exists()) {
			final File[] files = fontdir.listFiles(f -> f.getName().toLowerCase().endsWith(".ttf"));
			for(final File f : files) {
				final java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, f);
				boolean doLoad = false;
				if(-1 == font.canDisplayUpTo(PALI_VOWELS + PALI_CONSONANTS)) {
					paliFontMap.get(PaliScript.ROMAN).add(font.getFamily());
					doLoad = true;
				}
				if(-1 == font.canDisplayUpTo(PaliCharTransformer.thaiConsonants, 0, PaliCharTransformer.thaiConsonants.length)) {
					paliFontMap.get(PaliScript.THAI).add(font.getFamily());
					doLoad = true;
				}
				if(-1 == font.canDisplayUpTo(PaliCharTransformer.khmerConsonants, 0, PaliCharTransformer.khmerConsonants.length)) {
					paliFontMap.get(PaliScript.KHMER).add(font.getFamily());
					doLoad = true;
				}
				if(-1 == font.canDisplayUpTo(PaliCharTransformer.myanmarConsonants, 0, PaliCharTransformer.myanmarConsonants.length)) {
					paliFontMap.get(PaliScript.MYANMAR).add(font.getFamily());
					doLoad = true;
				}
				if(-1 == font.canDisplayUpTo(PaliCharTransformer.sinhalaConsonants, 0, PaliCharTransformer.sinhalaConsonants.length)) {
					paliFontMap.get(PaliScript.SINHALA).add(font.getFamily());
					doLoad = true;
				}
				if(-1 == font.canDisplayUpTo(PaliCharTransformer.devaConsonants, 0, PaliCharTransformer.devaConsonants.length)) {
					paliFontMap.get(PaliScript.DEVANAGARI).add(font.getFamily());
					doLoad = true;
				}
				if(doLoad) {
					javafx.scene.text.Font.loadFont(new FileInputStream(new File(ROOTDIR + EXFONTPATH + f.getName())), 0);
				}
			}
		}
		// if nothing available, set to the fallback font
		for(final PaliScript sc : PaliScript.values()) {
			if(paliFontMap.get(sc).isEmpty())
				paliFontMap.get(sc).add(FONT_FALLBACK);
		}
	}
	
	/**
	 * Checks whether the array has all empty strings.
	 * This funtion is used mainly in conjugation table.
	 */
	public static boolean isArrayEmpty(final String[][] arr) {
		boolean result = true;
		for(final String[] a : arr) {
			for(final String s : a) {
				result = result && s.isEmpty();
			}
		}
		return result;
	}

	/**
	 * Sets up key-character mapping for Pali input.
	 */
	public static void setupPaliInputCharMap() {
		for(PaliTextInput.InputMethod method : PaliTextInput.InputMethod.values()) {
			if(method != PaliTextInput.InputMethod.NORMAL) {
				final HashMap<String, String> inputMap = new HashMap<>();
				final Set<String> keySet = PaliPlatform.settings.stringPropertyNames();
				keySet.stream().filter(k -> k.startsWith(method.abbr+"-"))
								.forEach(k -> {
									final String paliChar = k.substring(3);
									if(paliChar.length()==1)
										inputMap.put(PaliPlatform.settings.getProperty(k), paliChar);
								});
				paliInputCharMap.put(method, inputMap);
			}
		}
	}
	
	/**
	 * Reads a file and determines its script.
	 */
	public static PaliScript getScriptLanguage(final File file) {
		final StringBuilder text = new StringBuilder();
		try(final Scanner in = new Scanner(new FileInputStream(file), "UTF-8")) {
			while(in.hasNextLine() && text.length() < 100) {
				String line = in.nextLine().trim();
				text.append(line);
			}
		} catch(FileNotFoundException e) {
			System.err.println(e);
		}
		return testLanguage(text.toString());
	}
	
	/**
	 * Determines the script language of a given text.
	 */
	public static PaliScript testLanguage(final String input) {
		final String text = input.trim();
		if(text.isEmpty())
			return PaliScript.ROMAN;
		final String specimen = text.length() > 100?text.substring(0, 100):text;
		final int totalLen = specimen.length();
		final PaliScript result;
		int romanCount = 0;
		int thaiCount = 0;
		int khmerCount = 0;
		int myanmarCount = 0;
		int sinhalaCount = 0;
		int devaCount = 0;
		final HashMap<Integer, PaliScript> hitCount = new HashMap<>();
		for(char ch : specimen.toCharArray()) {
			if(ch >= '\u0030' && ch <= '\u007A')
				romanCount++;
			if(ch >= '\u0900' && ch <= '\u097F')
				devaCount++;
			if(ch >= '\u0D80' && ch <= '\u0DFF')
				sinhalaCount++;
			if(ch >= '\u0E00' && ch <= '\u0E7F')
				thaiCount++;
			if(ch >= '\u1000' && ch <= '\u109F')
				myanmarCount++;
			if(ch >= '\u1780' && ch <= '\u17FF')
				khmerCount++;
		}
		hitCount.put(romanCount, PaliScript.ROMAN);
		hitCount.put(thaiCount, PaliScript.THAI);
		hitCount.put(khmerCount, PaliScript.KHMER);
		hitCount.put(myanmarCount, PaliScript.MYANMAR);
		hitCount.put(sinhalaCount, PaliScript.SINHALA);
		hitCount.put(devaCount, PaliScript.DEVANAGARI);
		final List<Integer> max = hitCount.keySet().stream().sorted((x, y)->Integer.compare(y, x)).limit(1).collect(Collectors.toList());
		final int maxScore = max != null && !max.isEmpty() ? max.get(0) : 0;
		if(maxScore > totalLen/2.0)
			result = hitCount.get(maxScore);
		else
			result = PaliScript.UNKNOWN;
		return result;
	}
	
	/**
	 * Replace Ŋ and ŋ with Ṃ and ṃ respectively.
	 * 
	 */
	public static String replaceOldNiggahitaWithNew(final String input) {
		final String output = input.replace("Ŋ", "Ṃ").replace("ŋ", "ṃ");
		return output;
	}
	
	/**
	 * Replace Ṃ and ṃ with Ŋ and ŋ respectively.
	 * 
	 */
	public static String replaceNewNiggahitaWithOld(final String input) {
		final String output = input.replace("Ṃ", "Ŋ").replace("ṃ", "ŋ");
		return output;
	}

	/** 
	 * Loads information of Pali documents in the collection.
	 * The load is done when needed, @see #getDocTitle, #getDocLinks, #getDocUpperLinks.
	 */
	public static void loadDocInfo() {
		try(final Scanner in = new Scanner(new FileInputStream(new File(ROOTDIR + COLLPATH + CSCD_INFO)), "UTF-8")) {
			String[] info;
			DocInfo dInfo;
			List<String> links;
			while(in.hasNextLine()) {
				info = in.nextLine().split(";");
				dInfo = new DocInfo(info[0]);
				dInfo.setTitle(info[1], info[2], info[3]);
				dInfo.setToc(info[4]);
				links = new ArrayList<>();
				for(int i=5; i<info.length; i++) {
					links.add(info[i]);
				}
				dInfo.setLinks(links);
				docInfoMap.put(info[0], dInfo);
			}
		} catch(FileNotFoundException e) {
			System.err.println(e);
		}
	}
	
	public static String getDocTitle(final String filename, final int level) {
		if(docInfoMap.isEmpty())
			loadDocInfo();
		final String id = filename.substring(0, filename.lastIndexOf("."));
		final DocInfo d = docInfoMap.get(id);
		String result;
		if(level < 0) {
			result = d.getFullTitle();
		} else {
			result = d.getTitle(level);
		}
		return result;
	}
	
	public static List<String> getDocLinks(final String filename) {
		if(docInfoMap.isEmpty())
			loadDocInfo();
		final String id = filename.substring(0, filename.lastIndexOf("."));
		final DocInfo d = docInfoMap.get(id);
		return d.getLinks();
	}

	public static List<String> getDocUpperLinks(final String filename) {
		if(docInfoMap.isEmpty())
			loadDocInfo();
		final String id = filename.substring(0, filename.lastIndexOf("."));
		final List<String> links = new ArrayList<>();
		for(DocInfo di : docInfoMap.values()) {
			if(di.getLinks().contains(id))
				links.add(di.getId());
		}
		return links;
	}
	
	public static void createBookmarkList(final String propBookmark) {
		if(!bookmarkList.isEmpty())
			return;
		final String[] mks = propBookmark.split(";");
		for(String m : mks) {
			if(m.length() > 0) {
				final String[] items = m.split(":");
				final String filename = items[0];
				final boolean inZip = items[1].equals("1");
				final String title = inZip?getDocTitle(filename, -1):filename.substring(0, filename.lastIndexOf("."));
				// if not in the archive, check the file existence
				boolean fileExists = true;
				if(!inZip) {
					final File exfile = new File(EXTRAPATH + filename);
					fileExists = exfile.exists();
				}
				if(fileExists) {
					PaliDocument pdoc = new PaliDocument(title, filename, inZip);
					bookmarkList.add(pdoc);
				}
			}
		}
	}
	
	public static void addBookmark(final TOCTreeNode node) {
		final String filename = node.getFileName();
		final boolean inArchive = node.isInArchive();
		// find first whether the filename already existed
		final boolean found = 0 < bookmarkList.stream().filter(p -> p.getFileName().equals(filename) && p.getInArchive()==inArchive).count();
		if(!found) {
			final String title = inArchive?getDocTitle(filename, -1):filename.substring(0, filename.lastIndexOf("."));
			final PaliDocument pdoc = new PaliDocument(title, filename, inArchive);
			bookmarkList.add(pdoc);
		}
	}

	public static void removeObservableItems(final ObservableList<?> list, final List<Integer> selected) {
		// In case of ObservableList, we have to delete from back to front, so inversed sort is needed.
		final Integer[] inds = selected.stream().sorted((x,y) -> y-x).toArray(Integer[]::new);
		for(final Integer i : inds) {
			if(i >= 0)
				list.remove(i.intValue());
		}
	}
	
	/**
	 * Formats the output from XML transformation, @see #readXML.
	 * The output is complete HTML text, including scripts and styles.
	 */
	public static String makeHTML(final String body, final boolean isCSCD) {
		final StringBuilder scriptBody = new StringBuilder();
		try(final Scanner inco = new Scanner(PaliPlatform.class.getResourceAsStream(COMMON_JS), "UTF-8")) {
			while(inco.hasNextLine())
				scriptBody.append(inco.nextLine()).append("\n");
			if(isCSCD) {
				try(final Scanner incs = new Scanner(PaliPlatform.class.getResourceAsStream(CSCD_JS), "UTF-8")) {
					while(incs.hasNextLine())
						scriptBody.append(incs.nextLine()).append("\n");
				}
			}
		}		
		final String jsScript = "<script type='text/javascript'>" + scriptBody.toString() + "</script>";
		final StringBuilder htmlText = new StringBuilder(body);
		htmlText.insert(0, "<!doctype html><html><head><meta charset='utf-8'>"+jsScript+"</head>");
		htmlText.append("</html>");
		return htmlText.toString();
	}
	
	public static void saveHTMLBody(final String body, final String filename) {
		final StringBuilder htmlText = new StringBuilder(body);
		htmlText.insert(0, "<!doctype html><html><head><meta charset='utf-8'></head>"+System.getProperty("line.separator"));
		htmlText.append("</html>");
		final String name = filename.substring(0, filename.lastIndexOf(".")) + ".html";
		saveText(htmlText.toString(), name);
	}
	
	/**
	 * Reads an XML file, either from the collection zip or an individual file,
	 * then, transforms the content into HTML by XSLT. According to the stylesheet given,
	 * the output is wrapped with tag body, hence, not complete HTML.
	 */
	public static String readXML(final String matter, final boolean inArchive) {
		org.w3c.dom.Document domDoc = null;
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final StringWriter writer = new StringWriter();
		try {
			final DocumentBuilder builder = docFactory.newDocumentBuilder();
			if(inArchive) {
				final ZipFile zip = new ZipFile(new File(ROOTDIR + COLLPATH + CSCD_ZIP));
				final ZipEntry entry = zip.getEntry(CSCD_DIR + matter);
				if(entry != null) {
					domDoc = builder.parse(zip.getInputStream(entry));
				} else {
					zip.close();
					return "";
				}
				zip.close();
			} else {
				final File file = new File(EXTRAPATH + matter);
				if(file.exists())
					domDoc = builder.parse(file);		
			}
			if(domDoc == null) return "";
			// read DOM and transform with XSLT
			final InputStream stylesheet = Utilities.class.getResourceAsStream(CSCD_XSL);
			final TransformerFactory tFactory = TransformerFactory.newInstance();
			final StreamSource stylesource = new StreamSource(stylesheet);
			final Transformer transformer = tFactory.newTransformer(stylesource);
			// for test
			//~ transformer.transform(new DOMSource(domDoc), new StreamResult(System.out));
			final DOMSource source = new DOMSource(domDoc);
			final StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			writer.flush();
			writer.close();
		} catch(TransformerConfigurationException tce) {
			// Error generated by the parser
			System.err.println("\n** Transformer Factory error");
			System.err.println("   " + tce.getMessage());
			// Use the contained exception, if any
			if(tce.getException() != null) {
				final Throwable x = tce.getException();
				System.err.println(x);
			}
			System.err.println(tce);
		} catch(TransformerException te) {
			// Error generated by the parser
			System.err.println("\n** Transformation error");
			System.err.println("   " + te.getMessage());
			// Use the contained exception, if any
			if(te.getException() != null) {
				final Throwable x = te.getException();
				System.err.println(x);
			}
			System.err.println(te);
		} catch(SAXException sxe) {
			// Error generated by this application
			// (or a parser-initialization error)
			if(sxe.getException() != null) {
				final Exception x = sxe.getException();
				System.err.println(x);
			}
			System.err.println(sxe);
		} catch(ParserConfigurationException | IOException e) {
			// Parser with specified options can't be built
			System.err.println(e);
		}
		return writer.toString();
	}

	/**
	 * Reads an XML file from the collection zip or extra dir, then strip XML tags and return only the bare text.
	 */
	public static String getTextFromXML(final TOCTreeNode doc, final boolean includeNotes, final boolean indent) {
		final StringBuilder result = new StringBuilder();
		final String filename = doc.getFileName();
		try{
			if(doc.isInArchive()) {
				final ZipFile zip = new ZipFile(new File(ROOTDIR + COLLPATH + CSCD_ZIP));
				final ZipEntry entry = zip.getEntry(CSCD_DIR + filename);
				if(entry != null) {
					final Scanner in = new Scanner(zip.getInputStream(entry), "UTF-8");
					while(in.hasNextLine())
						result.append(xmlToText(in.nextLine(), includeNotes, indent));
					in.close();
				}
				zip.close();
			} else if(doc.isExtra()) {
				final Scanner in = new Scanner(new FileInputStream(new File(EXTRAPATH + filename)), "UTF-8");
				while(in.hasNextLine())
					result.append(xmlToText(in.nextLine(), includeNotes, indent));
				in.close();
			}
		} catch(FileNotFoundException e) {
			System.err.println(e);
		} catch(IOException e) {
			System.err.println(e);
		}
		return result.toString();
	}

	private static String xmlToText(final String xmlStr, final boolean includeNotes, final boolean indent) {
		final StringBuilder result = new StringBuilder();
		final String tmpStr;
		if(includeNotes)
			tmpStr = xmlStr.replace("<note>", " [").replace("</note>", "] ");
		else
			tmpStr = xmlStr.replaceAll("<note>.*?</note>", " ");
		final String patt = "<\\??/?[a-zA-Z](?:[^>\"\']|\"[^\"]*\"|\'[^\']*\'\\?)*>";
		final String text = tmpStr.replaceAll(patt, " ").replaceAll(" {2,}", " ").replace(" .", ".").replace(" ,", ",").trim();
		if(!text.isEmpty()) {
			if(indent)
				result.append("\t");
			result.append(text).append(System.getProperty("line.separator"));
		}
		return result.toString();
	}

	public static void openXMLDocAsText(final TOCTreeNode doc, final boolean withNotes) {
		if(doc == null) return;
		final String text = getTextFromXML(doc, withNotes, true);
		if(!text.isEmpty()) {
			final Object[] args = { "ROMAN", text };
			PaliPlatform.openWindow(PaliPlatform.WindowType.EDITOR, args);
		}
	}

	/**
	 * Check whether a file in the collection contains specific text.
	 */
	public static boolean isCSCDContainsText(final String filename, final String text) {
		boolean result = false;
		try{
			final ZipFile zip = new ZipFile(new File(ROOTDIR + COLLPATH + CSCD_ZIP));
			final ZipEntry entry = zip.getEntry(CSCD_DIR + filename);
			if(entry != null) {
				final Scanner in = new Scanner(zip.getInputStream(entry), "UTF-8");
				while(in.hasNextLine()) {
					final String line = in.nextLine();
					if(line.contains(text)) {
						result = true;
						break;
					}
				}
				in.close();
			}
			zip.close();
		} catch(IOException e) {
			System.err.println(e);
		}
		return result;
	}

	/**
	 * Get a file list of the CSCD collection that contains specific text.
	 */
	public static List<String> getCSCDFileList(final String text) {
		final List<String> result = new ArrayList<>();
		try{
			final ZipFile zip = new ZipFile(new File(ROOTDIR + COLLPATH + CSCD_ZIP));
			for(final Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
				final ZipEntry entry = e.nextElement();
				final String fname = entry.getName();
				if(!fname.contains("toc")) {
					final String name = fname.substring(fname.lastIndexOf(File.separator) + 1);
					final Scanner in = new Scanner(zip.getInputStream(entry), "UTF-8");
					boolean found = false;
					while(in.hasNextLine()) {
						final String line = in.nextLine();
						if(line.contains(text)) {
							found = true;
							break;
						}
					}
					in.close();
					if(found)
						result.add(name);
				}
			}
			zip.close();
		} catch(IOException e) {
			System.err.println(e);
		}
		result.sort(PaliDocument.getFileNameStringComparator());
		return result;
	}

	public static List<String> getCSCDFileList() {
		final List<String> result = new ArrayList<>();
		try{
			final ZipFile zip = new ZipFile(new File(ROOTDIR + COLLPATH + CSCD_ZIP));
			for(final Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
				final ZipEntry entry = e.nextElement();
				final String fname = entry.getName();
				if(!fname.contains("toc")) {
					final String name = fname.substring(fname.lastIndexOf(File.separator) + 1);
					result.add(name);
				}
			}
			zip.close();
		} catch(IOException e) {
			System.err.println(e);
		}
		result.sort(PaliDocument.getFileNameStringComparator());
		return result;
	}
	
	public static void copyText(final String text) {
		final Clipboard cboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(text);
		cboard.setContent(content);
	}
	
	public static File selectDirectory(final String init) {
		return selectDirectory(init, PaliPlatform.stage);
	}

	public static File selectDirectory(final Window owner) {
		return selectDirectory(".", owner);
	}
	
	public static File selectDirectory(final String init, final Window owner) {
		return selectDirectory(init, owner, "Select a directory");
	}

	public static File selectDirectory(final String init, final Window owner, final String title) {
		final File initDir = new File(init);
		final DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle(title);
		dirChooser.setInitialDirectory(initDir);
		return dirChooser.showDialog(owner);
	}
	
	public static File selectFile(final String ext, final String initPath, final Window owner) {
		final File initDir = new File(initPath);
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(initDir);
		final String capExt = ext.toUpperCase();
		fileChooser.setTitle("Select a " + capExt + " file");
		fileChooser.getExtensionFilters().add(new ExtensionFilter(capExt + " Files", "*." + ext));
		return fileChooser.showOpenDialog(owner);
	}
	
	public static File selectTextFile(final Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select a file");
		fileChooser.getExtensionFilters().addAll(
			new ExtensionFilter("Text Files", "*.txt"),
			new ExtensionFilter("All Files", "*.*"));
		return fileChooser.showOpenDialog(owner);
	}
	
	public static List<File> selectMultipleTextFile(final Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select files");
		fileChooser.getExtensionFilters().addAll(
			new ExtensionFilter("Text Files", "*.txt"),
			new ExtensionFilter("All Files", "*.*"));
		return fileChooser.showOpenMultipleDialog(owner);
	}
	
	public static String getTextFileContent(final File theFile) {
		String content = "";
		try {
			content = Files.readString(theFile.toPath());
		} catch(IOException e) {
			System.err.println(e);
		}
		return content == null ? "" : content;
	}
	
	private static File getOutputFile(final String nameAndExt) {
		return getOutputFile(nameAndExt, ".", PaliPlatform.stage);
	}

	public static File getOutputFile(final String nameAndExt, final String initPath, final Window owner) {
		final File initDir = new File(initPath);
		final String[] filename = nameAndExt.split("\\.");
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select a file name");
		fileChooser.setInitialDirectory(initDir);
		fileChooser.setInitialFileName(nameAndExt);
		final String fileExt = filename[filename.length-1];
		final String fileEXT = fileExt.toUpperCase();
		final String fileDesc = fileExt.equals("TXT") ? "Text" : fileEXT;
		final ExtensionFilter extFilter = new ExtensionFilter(fileDesc + " Files", "*." + fileExt);
		fileChooser.getExtensionFilters().add(extFilter);
		return fileChooser.showSaveDialog(owner);
	}

	public static File saveList(final List<String> list, final String filename) {
		final File outfile = getOutputFile(filename);
		if(outfile != null) {
			final String text = list.stream().collect(Collectors.joining("\n"));
			saveText(text, outfile);
		}
		return outfile;
	}
	
	public static File saveText(final String text, final String filename) {
		return saveText(text, filename, PaliPlatform.stage);
	}
	
	public static File saveText(final String text, final String filename, final Window owner) {
		final File outfile = getOutputFile(filename, ".", owner);
		if(outfile != null) {
			saveText(text, outfile);
		}
		return outfile;
	}

	public static void saveText(final String text, final File file) {
		try(final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			out.write(text, 0, text.length());
		} catch(IOException e) {
			System.err.println(e);
		}			
	}
	
	public static void saveSnapshot(final Node node) {
		WritableImage image = node.snapshot(new SnapshotParameters(), null);
		saveImage(image, "image.png");
	}	
	
	private static void saveImage(final WritableImage image, final String filename) {
		final File output = getOutputFile(filename);
		final String imageFormat = filename.substring(filename.lastIndexOf(".")+1);
		if(output != null) {
			// Convert the image to a buffered image.
			final BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
			// Save the image to the file.
			try {
				ImageIO.write(bImage, imageFormat, output);
			} 
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static Set<String> getHeadTermsFromDB(final String query) {
		final Set<String> termSet = new HashSet<>();
		try {
			if(dbConn != null) {
				final Statement stmt = dbConn.createStatement();
				final ResultSet rs = stmt.executeQuery(query);
				while(rs.next()) {
					termSet.add(rs.getString(1));
				}
				rs.close();		
				stmt.close();
			}
		} catch(SQLException e) {
			System.err.println(e);
		}
		return termSet;	
	}
	
	public static List<String> getTermFreqListFromDB(final Set<String> terms) {
		final Map<String, Integer> tfMap = new HashMap<>();
		final String strWhere = terms.stream().map(x -> "TERM='"+x+"'").collect(Collectors.joining(" OR "));
		try {
			if(dbConn != null) {
				final String query = "SELECT TERM,FREQUENCY FROM CSCDTERMS WHERE " + strWhere + ";";
				final Statement stmt = dbConn.createStatement();
				final ResultSet rs = stmt.executeQuery(query);
				while(rs.next())
					tfMap.put(rs.getString(1), rs.getInt(2));
				rs.close();
				stmt.close();
			}
		} catch(SQLException e) {
			System.err.println(e);
		}
		final List<String> result = tfMap.keySet().stream()
											.sorted((x, y) -> Integer.compare(tfMap.get(y), tfMap.get(x)))
											.map(x -> x + " (" + tfMap.get(x) + ")")
											.collect(Collectors.toList());
		return result;	
	}
	
	// count a character in a string
	public static int charCount(final String text, final char ch) {
		int result = 0;
		char[] arr = text.toCharArray();
		for(char c : arr)
			if(c == ch)
				result++;
		return result;
	}

	public static String getUsablePaliWord(final String text) {
		final String[] tokens = text.split(REX_NON_PALI);
		return Arrays.stream(tokens).filter(x -> x.length()>0).findFirst().get().toLowerCase();
	}
	
	public static void loadCSCDFiles() {
		if(!cscdFiles.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(CSCD_FILES), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(!line.isEmpty())
					cscdFiles.add(line);
			}
		}
	}

	public static void loadCPEDTerms() {
		if(!cpedTerms.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(CPED_TERMS), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(!line.isEmpty())
					cpedTerms.add(line);
			}
		}
	}

	public static void loadDeclinables() {
		if(!declinables.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(DECLINABLES), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(!line.isEmpty())
					declinables.add(line);
			}
		}
	}

	public static List<String> lookUpDictFromDB(final DictWin.DictBook dic, final String term) {
		final List<String> meanings = new ArrayList<>();
		final String query = "SELECT MEANING FROM "+dic.toString()+" WHERE TERM='"+term+"';";
		try {
			if(dbConn != null) {
				final Statement stmt = dbConn.createStatement();
				final ResultSet rs = stmt.executeQuery(query);
				while(rs.next()) {
					meanings.add(rs.getString(1));
				}
				rs.close();		
				stmt.close();
			}
		} catch(SQLException e) {
			System.err.println(e);
		}
		return meanings;	
	}
	
	public static PaliWord lookUpCPEDFromDB(final String term) {
		final PaliWord pword = new PaliWord(term);
		final String query = "SELECT POS,PARADIGM,IN_COMPOUNDS,MEANING,SUBMEANING FROM CPED WHERE TERM='"+term+"';";
		try {
			if(dbConn != null) {
				final Statement stmt = dbConn.createStatement();
				final ResultSet rs = stmt.executeQuery(query);
				while(rs.next()) {
					final String para = rs.getString(2);
					final String pos = rs.getString(1);
					if(para == null) {
						if(isGenericParadigmNeeded(pos))
							pword.setParadigm("generic");
					} else {
						pword.setParadigm(para);
					}
					pword.addPosInfo(pos);
					final boolean forCompounds = rs.getBoolean(3);
					pword.addForCompounds(forCompounds);
					final String meaning = rs.getString(4);
					pword.addMeaning(meaning);
					final String submean = rs.getString(5);
					pword.addSubmeaning(submean);
				}
				rs.close();		
				stmt.close();
			}
		} catch(SQLException e) {
			System.err.println(e);
		}
		// dealing with special cases
		final List<String> prdm = pword.getParadigm();
		if(prdm.size() == 1 && prdm.get(0).equals("generic")) {
			final String tm = pword.getTerm();
			if(tm.endsWith("ant") && pword.isAdjective()) {
				pword.clearParadigm();
				pword.setParadigm("guṇavant,himavant,antcommon");
			} else if(tm.endsWith("ar")) {
				pword.clearParadigm();
				pword.setParadigm("kattu");
			}
		}
		return pword;	
	}

	private static boolean isGenericParadigmNeeded(final String pos) {
		boolean result = false;
		if(pos == null)
			return result;
		result = result || pos.equals("3");
		result = result || pos.equals("n.");
		result = result || pos.contains("m.");
		result = result || pos.contains("f.");
		result = result || pos.contains("nt.");
		result = result || pos.contains("adj.");
		return result;
	}
	
	public static String formatCPEDMeaning(final PaliWord pword, final boolean withTag) {
		final StringBuilder text = new StringBuilder();
		final List<String> posInfo = pword.getPosInfo();
		final List<String> meaning = pword.getMeaning();
		final List<String> submeaning = pword.getSubmeaning();
		final List<Boolean> forCompounds = pword.getForCompounds();
		String inCpds;
		String pos;
		String mean;
		String submean;
		final int all = pword.getRecordCount() - 1;
		for(int i = 0; i <= all ; i++) {
			pos = "";
			if(posInfo != null && posInfo.size() > i)
				pos = posInfo.get(i)==null ? "" : "("+posInfo.get(i)+") ";
			if(pos.contains("+") && !pos.contains("of"))
				pos = "(v.)" + pos;
			inCpds = "";
			if(forCompounds.size() > i)
				inCpds = forCompounds.get(i) ? "[In Compounds] " : "";
			mean = "";
			if(meaning.size() > i)
				mean = meaning.get(i)==null ? "" : meaning.get(i);
			submean = "";
			if(submeaning.size() > i)
				submean = submeaning.get(i)==null ? "" :
					withTag ? "<div>"+submeaning.get(i)+"</div>" : submeaning.get(i);
			if(withTag)
				text.append("<p>"+pos+inCpds+mean+submean+"</p>");
			else
				text.append(pos+inCpds+mean+"\n"+submean);
		}
		return text.toString();
	}
	
	public static void loadPronounList() {
		if(!paliPronouns.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(PALI_PRONOUN_LIST), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(line.charAt(0) == '#')
					continue;
				final String[] items = line.split(":");
				final String term = items[0];
				final PaliWord word = new PaliWord(term);
				word.addParadigm(items[1]);
				word.addMeaning(items[2]);
				word.addPosInfo("pron.");
				word.setAllGenders();
				word.setEnding();
				paliPronouns.put(term, word);
			}
		}
	}
	
	public static void loadNumeralList() {
		if(!paliNumerals.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(PALI_NUMERAL_LIST), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(line.charAt(0) == '#')
					continue;
				final String[] items = line.split("\\|");
				final String term = items[0];
				final int value, exp;
				if(items[1].contains("e")) {
					final String[] n = items[1].split("e");
					value = Integer.parseInt(n[0]);
					exp = Integer.parseInt(n[1]);
				} else {
					value = Integer.parseInt(items[1]);
					exp = 0;
				}
				final PaliWord word = createNumeralPaliWord(term, value, exp, false);
				final String expStr = exp > 0 ? "e" + exp : "";
				word.addMeaning(value + expStr);
				paliNumerals.put(term, word);
				final String key = value+"e"+exp;
				final List<String> numTermList;
				if(paliCardinalMap.containsKey(key))
					numTermList = paliCardinalMap.get(key);
				else
					numTermList = new ArrayList<>();
				numTermList.add(term);
				paliCardinalMap.put(key, numTermList);
			} // end while
		} // end try
		// set up ordinal number list, only the distinct terms, the rest use calculation
		if(paliOrdinalMap.isEmpty()) {
			// fill paliOrdinalMap used for list selection
			paliOrdinalMap.put("1e0", Arrays.asList("paṭhama"));
			paliOrdinalMap.put("2e0", Arrays.asList("dutiya"));
			paliOrdinalMap.put("3e0", Arrays.asList("tatiya"));
			paliOrdinalMap.put("4e0", Arrays.asList("catuttha"));
			paliOrdinalMap.put("5e0", Arrays.asList("pañcama"));
			paliOrdinalMap.put("6e0", Arrays.asList("chaṭṭha"));
			for(int i=7; i<=10; i++) {
				final List<String> ordTermList = paliCardinalMap.get(i+"e0");
				for(final String s : ordTermList)
					paliOrdinalMap.put(i+"e0", Arrays.asList(s+"ma"));
			} // end for
			final List<String> ordList = new ArrayList<>(paliOrdinalMap.keySet());
			for(int i=0; i<ordList.size(); i++) {
				final String key = ordList.get(i);
				final String t = paliOrdinalMap.get(key).get(0);
				final int val = Integer.parseInt(key.substring(0, key.length()-2));
				final PaliWord oword = createNumeralPaliWord(t, val, 0, true);
				paliOrdinals.put(t, oword);
			} // end for
		} // end if
	}
	
	public static PaliWord createNumeralPaliWord(final String term, final int num, final int exp, final boolean isOrdinal) {
		final PaliWord pword = new PaliWord(term);
		pword.setNumericValue(num);
		pword.setExpValue(exp);
		pword.addPosInfo("numerals");
		if(term.endsWith("uttara") || term.endsWith("dhika")) {
			pword.setParadigm("generic");
			pword.addPosInfo("nt.");
		} else {
			pword.setNumeralParadigm(isOrdinal);
			pword.setNumeralGender(isOrdinal);
		}
		pword.setEnding();
		return pword;
	}	
	
	public static Map<PaliDeclension.Case, Map<PaliDeclension.Number, List<String>>> computeDeclension(final PaliWord pword, final int genderIndex) {
		final Map<PaliDeclension.Case, Map<PaliDeclension.Number, List<String>>> result = new EnumMap<>(PaliDeclension.Case.class);
		final List<String> paraNames = pword.getParadigm();
		final NounParadigm[] paradigms = new NounParadigm[paraNames.size()];
		final PaliWord.Gender gender = pword.getGender().get(genderIndex);
		for(int i=0; i<paraNames.size(); i++) {
			paradigms[i] = declension.getNounParadigm(paraNames.get(i), pword.getEnding().get(gender), gender);
			if(paradigms[i] == null)
				paradigms[i] = declension.getNounParadigm("generic", pword.getEnding().get(gender), gender);
		}
		// loop for each case
		for(final PaliDeclension.Case cas : PaliDeclension.Case.values()) {
			final Map<PaliDeclension.Number, List<String>> termMap = new EnumMap<>(PaliDeclension.Number.class);
			// loop for singular and plural
			for(final PaliDeclension.Number nu : PaliDeclension.Number.values()) {
				final List<String> terms = new ArrayList<>();
				final Set<String> endingSet = new LinkedHashSet<>();
				for(final NounParadigm np : paradigms) {
					if(np != null)
						endingSet.addAll(np.getEndings(cas, nu));
				}
				final List<String> endings = new ArrayList<>(endingSet);
				if(!endings.isEmpty()) {
					for(int ind=0; ind<endings.size(); ind++) {
						terms.add(pword.withSuffix(endings.get(ind), gender));
					}
				}
				termMap.put(nu, terms);
			} // end for
			result.put(cas, termMap);
		} // end for
		return result;
	}

	public static GridPane createDeclensionGrid(final Map<PaliDeclension.Case, Map<PaliDeclension.Number, List<String>>> decMap) {
		final GridPane resultGrid = new GridPane();
		resultGrid.setHgap(2);
		resultGrid.setVgap(2);
		resultGrid.setPadding(new Insets(2, 2, 2, 2));
		final Label lblCaseHead = new Label("Case");
		lblCaseHead.setStyle("-fx-font-weight:bold");
		final Label lblSingHead = new Label(PaliDeclension.Number.SING.getName());
		lblSingHead.setStyle("-fx-font-weight:bold");
		final Label lblPluHead = new Label(PaliDeclension.Number.PLU.getName());
		lblPluHead.setStyle("-fx-font-weight:bold");
		GridPane.setConstraints(lblCaseHead, 0, 0, 2, 1);
		GridPane.setConstraints(lblSingHead, 2, 0);
		GridPane.setConstraints(lblPluHead, 3, 0);
		resultGrid.getChildren().addAll(lblCaseHead, lblSingHead, lblPluHead);
		decMap.forEach((cas, numMap) -> {
			final Label lblNum = new Label(cas.getNumAbbr());
			final Label lblCase = new Label(cas.getAbbr());
			lblCase.setMinWidth(getRelativeSize(3));
			lblCase.setMaxWidth(getRelativeSize(3));
			final Label lblSing = new Label(numMap.get(PaliDeclension.Number.SING).stream().collect(Collectors.joining(", ")));
			lblSing.setWrapText(true);
			lblSing.setMaxWidth(getRelativeSize(11));
			final Label lblPlu = new Label(numMap.get(PaliDeclension.Number.PLU).stream().collect(Collectors.joining(", ")));
			lblPlu.setWrapText(true);
			lblPlu.setMaxWidth(getRelativeSize(11));
			GridPane.setConstraints(lblNum, 0, cas.ordinal()+1, 1, 1, HPos.LEFT, VPos.TOP);
			GridPane.setConstraints(lblCase, 1, cas.ordinal()+1, 1, 1, HPos.LEFT, VPos.TOP);
			GridPane.setConstraints(lblSing, 2, cas.ordinal()+1, 1, 1, HPos.LEFT, VPos.TOP);
			GridPane.setConstraints(lblPlu, 3, cas.ordinal()+1, 1, 1, HPos.LEFT, VPos.TOP);
			resultGrid.getChildren().addAll(lblNum, lblCase, lblSing, lblPlu);
		});
		return resultGrid;
	}

	public static List<String> getPaliCardinal(final String inNum) {
		final int leng = inNum.length();
		if(leng == 0)
			return Collections.emptyList();
		final int value = Integer.parseInt(inNum);
		if(value == 0)
			return Collections.emptyList();
		// eliminate leading zeroes
		int zNum = 0;
		for(final char ch : inNum.toCharArray()) {
			if(ch == '0')
				zNum++;
			else
				break;
		}
		final String numStr = inNum.substring(zNum);
		final int len = numStr.length();
		final List<String> lowerGroup = new ArrayList<>();
		final List<String> upperGroup = new ArrayList<>();
		List<List<String>> wholeList;
		if(len >= 3) {
			wholeList = get3DigitNumeral(Integer.parseInt(numStr.substring(len-3)), false);
			for(final List<String> l : wholeList)
				lowerGroup.add(l.get(0));
			if(len > 3) {
				wholeList = get3DigitNumeral(Integer.parseInt(numStr.substring(0, len-3)), false);
				for(final List<String> l : wholeList)
					upperGroup.add(l.get(0));
			}
		} else {
			wholeList = get3DigitNumeral(Integer.parseInt(numStr), false);
			for(final List<String> l : wholeList)
				lowerGroup.add(l.get(0));
		}
		final List<String> terms;
		if(!upperGroup.isEmpty()) {
			terms = new ArrayList<>();
			for(final String s : upperGroup) {
				String w = "sahassa";
				if(!lowerGroup.isEmpty()) {
					for(final String f : lowerGroup) {
						if(!s.endsWith("ā") && !s.endsWith("ṃ")) {
							if(!s.equals("eka"))
								w = s + w;
							terms.add(PaliWord.sandhi(f, "adhika") + w);
						}
					}
				} else {
					if(!s.endsWith("ā") && !s.endsWith("ṃ")) {
						if(!s.equals("eka"))
							w = s + w;
						terms.add(w);
					}
				}
			} // end for
		} else {
			terms = lowerGroup;
		} // end if
		return terms;
	}
	
	private static List<List<String>> get3DigitNumeral(final int inNum, final boolean split) {
		final List<List<String>> result = new ArrayList<>();
		final int value;
		if(inNum == 0)
			return result;
		else if(inNum >= 1000)
			value = inNum % 1000;
		else
			value = inNum;
		int lowNum = value;
		int hunNum = 0;
		if(value >= 100) {
			hunNum = value/100;
			lowNum = value%100;
		}
		final List<String> lowerList;
		if(lowNum > 0)
			lowerList = get2DigitNumeral(lowNum);
		else
			lowerList = new ArrayList<>();
		if(hunNum > 0) {
			final List<String> part1;
			final List<String> part2 = paliCardinalMap.get("100e0");
			final List<String> upperList = new ArrayList<>();		
			if(hunNum > 1) {
				part1 = paliCardinalMap.get(hunNum+"e0");
			} else {
				part1 = new ArrayList<>();
			}
			if(!lowerList.isEmpty()) {
				// there are all digits
				for(final String s : part2) {
					if(!part1.isEmpty()) {
						for(final String f : part1) {
							if(!f.endsWith("ā") && !f.endsWith("ṃ"))
								upperList.add(PaliWord.sandhi(f, s));
						}
					} else {
						upperList.add(s);
					}
				} // end for
				for(final String s : upperList) {
					if(!lowerList.isEmpty()) {
						for(final String f : lowerList) {
							if(!f.endsWith("ā") && !f.endsWith("ṃ")) {
								final List<String> composition = new ArrayList<>(2);
								if(split) {
									composition.add(f);
									composition.add(s);
								} else {
									composition.add(PaliWord.sandhi(f, "uttara") + s);
								}
								result.add(composition);
							}
						}
					}
				} // endfor
				// consider x5x the addha
				final List<List<String>> adhhaList = get3DigitAddha(value, false);
				for(final List<String> ls : adhhaList) {
					final List<String> composition = new ArrayList<>(1);
					composition.add(ls.get(0));
					result.add(composition);
				}
			} else {
				// only the highest digit, the rest are zeros
				for(final String s : part2) {
					if(!part1.isEmpty()) {
						for(final String f : part1) {
							final List<String> composition = new ArrayList<>(2);
							if(split) {
								composition.add(f);
								composition.add(s);
								result.add(composition);
							} else {
								if(!f.endsWith("ā") && !f.endsWith("ṃ")) {
									composition.add(PaliWord.sandhi(f, s));
									result.add(composition);
								}
							}
						} // end for
					} else {
						final List<String> composition = new ArrayList<>(2);
						composition.add(s);
						result.add(composition);
					}
				} // end for
			}
		} else {
			// only 2 digits
			for(final String s : lowerList) {
				final List<String> composition = new ArrayList<>(2);
				composition.add(s);
				result.add(composition);
			}
		}
		return result;
	}
	
	private static List<String> get2DigitNumeral(final int value) {
		final List<String> result;
		final List<String> termList = paliCardinalMap.get(value+"e0");
		if(termList == null) {
			// not found in the list, calculate it
			result = new ArrayList<>();
			final List<String> part1;
			final List<String> part2;
			String key;
			if(value % 10 == 9) {
				// end with nine
				part1 = new ArrayList<>();
				part1.add("ekūna");
				part1.add("ūna");
				key = (value+1) + "e0";
				part2 = paliCardinalMap.get(key);
			} else {
				key = (value%10) + "e0";
				part1 = paliCardinalMap.get(key);
				key = ((value/10)*10) + "e0";
				part2 = paliCardinalMap.get(key);
			} // end if
			for(final String f : part1) {
				for(final String s : part2)
					result.add(PaliWord.sandhi(f, s));
			}// end for	
		} else {
			result = termList;
		} // end if
		return result;
	}
	
	private static List<List<String>> get3DigitAddha(final int value, final boolean split) {
		final List<List<String>> result = new ArrayList<>();
		if(value > 860)
			return result;
		final int hunNum = value/100;
		if(hunNum == 0)
			return result;
		final String[] addhas = { "diyaḍḍha", "aḍḍhateyya", "aḍḍhuḍḍha" };
		final int num = value-hunNum*100;
		if(num/10 == 5) {
			final List<String> part1 = paliCardinalMap.get((num%10)+"e0");
			final List<String> part2;		
			if(hunNum >= 1 && hunNum <=3) {
				part2 = Arrays.asList(addhas[hunNum-1]);
			} else {
				part2 = new ArrayList<>();
				for(final String t : paliOrdinalMap.get((hunNum+1)+"e0"))
					part2.add("aḍḍha" + t);
			} // end if
			for(final String s : part2) {
				if(part1 == null) {
					final List<String> tmpList = new ArrayList<>(1);
					tmpList.add(s + "sata");
					result.add(tmpList);
				} else {
					for(final String f : part1) {
						if(f.endsWith("e") || f.endsWith("ā") || f.endsWith("ṃ"))
							continue;
						final List<String> tmpList = new ArrayList<>(2);
						final String ut = PaliWord.sandhi(f, "uttara");
						if(split) {
							tmpList.add(ut);
							tmpList.add(s + "sata");
						} else {
							tmpList.add(PaliWord.sandhi(ut, s) + "sata");
						}
						result.add(tmpList);
					}
				}
			} // end for
		} // end if
		return result;
	}	

	public static void loadRootList() {
		if(!paliRoots.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(PALI_ROOT_LIST), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(line.charAt(0) == '#')
					continue;
				final String[] items = line.split(":");
				String term = items[1];
				final int indTR = term.indexOf("[");
				String termRmk = "";
				if(indTR >= 0) {
					// the root term has a remark
					termRmk = term.substring(indTR+1, term.indexOf("]"));
					term = term.substring(0, indTR);
				}
				String pmean = items[2];
				final int indMR = pmean.indexOf("[");
				String meanRmk = "";
				if(indMR >= 0) {
					// the Pali meaning has a remark
					meanRmk = pmean.substring(indMR+1, pmean.indexOf("]"));
					pmean = pmean.replaceFirst("\\[.*\\]", "");
				}
				final Integer id = Integer.parseInt(items[0]);
				final String group = items[4];
				final PaliRoot root = new PaliRoot(id, term, group);
				root.setRootRemark(termRmk);
				root.setPaliMeaning(pmean);
				root.setMeaningRemark(meanRmk);
				root.setEngMeaning(items[3]);
				paliRoots.put(id, root);
			}
		} // end try
	}

	public static boolean isVowel(final char ch) {
		return (PALI_VOWELS.indexOf(Character.toLowerCase(ch)) >= 0);
	}

	public static boolean isConsonant(final char ch) {
		return (PALI_CONSONANTS.indexOf(Character.toLowerCase(ch)) >= 0);
	}

	public static void createMeterPatternMap() {
		if(!meterPatternMap.isEmpty())
			return;
		// mattāvutti set
		meterPatternMap.put('n', "llll");
		meterPatternMap.put('s', "llg");
		meterPatternMap.put('j', "lgl");
		meterPatternMap.put('b', "gll");
		meterPatternMap.put('m', "gg");
		// vaññavutti set
		meterPatternMap.put('N', "lll");
		meterPatternMap.put('S', "llg");
		meterPatternMap.put('J', "lgl");
		meterPatternMap.put('B', "gll");
		meterPatternMap.put('R', "glg");
		meterPatternMap.put('T', "ggl");
		meterPatternMap.put('M', "ggg");
		meterPatternMap.put('L', "llllllllllllll"); // 14 lahus
	}

	public static String changeToLahuGaru(final String text) {
		final StringBuilder result = new StringBuilder();
		for(final char ch : text.toCharArray()) {
			if(Character.isDigit(ch)) {
				if(ch == '1')
					result.append("l");
				else if(ch == '2')
					result.append("(g|ll)");
				else if(ch == '4')
					result.append("(llll|llg|lgl|gll|gg)");
			} else {
				if(meterPatternMap.containsKey(ch))
					result.append(meterPatternMap.get(ch));
				else
					result.append(ch);
			}
		}
		return result.toString();
	}

	public static int sumMeter(final String pattern) {
		int sum = 0;
		for(int i=0; i<pattern.length(); i++) {
			char ch = pattern.charAt(i);
			if(Character.isDigit(ch)) {
				sum += ch - '0';
			}
		}
		return sum;
	}

	public static String addComputedMeters(final String text) {
		final String[] paragraphs = text.split("\\n");
		final StringBuilder result = new StringBuilder();
		for(final String p : paragraphs) {
			if(!p.trim().isEmpty()) {
				final String[] tokens = p.split(REX_NON_PALI);
				for(final String s : tokens) {
					final String meters = computeMeter(s, true);
					if(!meters.isEmpty())
						result.append(s).append(" (").append(meters).append(") ");
				}
				result.append(System.getProperty("line.separator"));
			} else {
				result.append(System.getProperty("line.separator"));
			}
		}
		return result.toString();
	}
	
	public static String computeMeter(final String text, final boolean useNumber) {
		final char[] munit = useNumber ? new char[] { '1', '2' } : new char[] { 'l', 'g' };
		final String input = text.contains("\n") ? text.toLowerCase().trim().split("\\n")[0] : text.toLowerCase().trim();
		final StringBuilder meterPattern = new StringBuilder();
		final char[] chars = input.toCharArray();
		// consider each character
		for(int i=0; i<chars.length; i++) {
			final char thisCh = chars[i];
			char meter = '0';
			if(PALI_VOWELS.indexOf(thisCh) >= 0) {
				// only consider when it is a vowel
				// check what follows
				if(i < chars.length-1) {
					if(i < chars.length-2) {
						if(chars[i+1] == 'ṃ') {
							// followed by a niggahita, garu is assured
							meter = munit[1];
						} else if(PALI_CONSONANTS.indexOf(chars[i+1]) >= 0 && PALI_CONSONANTS.indexOf(chars[i+2]) >= 0) {
							// followed by a double consonants, garu is assured, with some exceptions
							if(WITH_H_CHARS.indexOf(chars[i+1]) >= 0 && chars[i+2] == 'h') {
								if(PALI_LAHU_VOWELS.indexOf(thisCh) >= 0)
									meter = munit[0];
								else
									meter = munit[1];
							} else {
								meter = munit[1];
							}
						} else {
							if(PALI_LAHU_VOWELS.indexOf(thisCh) >= 0)
								meter = munit[0];
							else
								meter = munit[1];
						}
					} else {
						if(chars[i+1] == 'ṃ') {
							// followed by a niggahita, garu is assured
							meter = munit[1];
						} else {
							if(PALI_LAHU_VOWELS.indexOf(thisCh) >= 0)
								meter = munit[0];
							else
								meter = munit[1];
						}
					}
				} else {
					if(PALI_LAHU_VOWELS.indexOf(thisCh) >= 0)
						meter = munit[0];
					else
						meter = munit[1];
				}
				meterPattern.append(meter);
			}
		} // end for
		return meterPattern.toString();
	}

	public static int getPaliWordLength(final String word) {
		int hfound = 0;
		final char [] chars = word.toCharArray();
		for(int i=0; i <= chars.length - 2; i++){
			if(WITH_H_CHARS.indexOf(chars[i]) >= 0) {
				if(chars[i+1] == 'h') {
					hfound++;
					i++;
				}
			}
		}
		return chars.length - hfound;
	}

	public static char shortenVowel(final char vowel) {
		char result = vowel;
		if(PALI_LONG_VOWELS.indexOf(vowel) > -1) {
			if(vowel == 'ā')
				result = 'a';
			else if(vowel == 'ī')
				result = 'i';
			if(vowel == 'ū')
				result = 'u';
		}
		return result;
	}
	
	public static List<String> getWordFreqList(final Set<String> wordSet) {
		final Tokenizer tokenWin = (Tokenizer)PaliPlatform.getMainWinInstance(PaliPlatform.WindowType.TOKEN)[0];
		final Map<String, TermFreqProp> termsMap = tokenWin.getMergedResultMap();
		final List<TermFreqProp> tfList = new ArrayList<>();
		wordSet.forEach(term -> {
			if(termsMap.containsKey(term))
				tfList.add(termsMap.get(term));
		});
		final List<String> result = tfList.stream()
										.sorted((x, y) -> Integer.compare(y.totalFreqProperty().get(), x.totalFreqProperty().get()))
										.map(x -> x.termProperty().get() + " (" + x.totalFreqProperty().get() + ")")
										.collect(Collectors.toList());
		return result;
	}

	public static void loadSandhiList() {
		if(!sandhiListMap.isEmpty())
			return;
		// load from the sandhi rules file
		try(final Scanner in = new Scanner(new FileInputStream(sandhiFile), "UTF-8")) {
			String[] line;
			while(in.hasNextLine()) {
				line = in.nextLine().split("=");
				final String term = line[0] == null ? "" : line[0].trim();
				if(term.isEmpty() || term.startsWith("#"))
					continue;
				final List<String> partList = new ArrayList<>();
				if(line[1] != null) {
					final String[] detail = line[1].split("\\+");
					for(final String p : detail)
						partList.add(p.trim());
					sandhiListMap.put(term, partList);
				}
			}
		} catch(FileNotFoundException e) {
			System.err.println(e);
		}
	}

	public static void updateSandhiList() {
		sandhiListMap.clear();
		loadSandhiList();
	}

	public static List<String> cutSandhi(final String term) {
		final List<String> result = new ArrayList<>();
		final boolean isCap = Character.isUpperCase(term.charAt(0));
		final String lower = term.toLowerCase();
		if(sandhiListMap.containsKey(lower)) {
			final List<String> sMap = sandhiListMap.get(lower);
			for(int k = 0; k < sMap.size(); k++) {
				final String part = sMap.get(k);
				final String output;
				if(k == 0 && isCap)
					output = Character.toUpperCase(part.charAt(0)) + part.substring(1);
				else
					output = part;
				result.add(output);
			}
		} else {
			result.add(term);
		}
		return result;
	}

	public static void loadStopwords() {
		if(!stopwords.isEmpty())
			return;
		try(final Scanner in = new Scanner(new FileInputStream(Utilities.stopwordsFile), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(line.charAt(0) == '#')
					continue;
				stopwords.add(line);
			}
		} catch(FileNotFoundException e) {
			System.err.println(e);
		}
	}

	public static void loadTexConv() {
		if(!texConvMap.isEmpty())
			return;
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(TEXCONV), "UTF-8")) {
			while(in.hasNextLine()) {
				final String line = in.nextLine().trim();
				if(line.charAt(0) == '#')
					continue;
				final String[] chunks = line.split(":");
				final char ch = chunks[0].charAt(0);
				final String[] texRules = chunks[1].split(",");
				final List<String> ruleList = Arrays.asList(texRules);
				texConvMap.put(ch, ruleList);
			}
		}
	}

	public static void updateStopwords() {
		stopwords.clear();
		loadStopwords();
	}

	private static void computeDeclension(final Map<String, DeclinedWord> outputMap, final Map<String, PaliWord> inputMap) {
		if(inputMap.isEmpty())
			return;
		outputMap.clear();
		if(declension == null)
			declension = new PaliDeclension();
		for(final PaliWord pword : inputMap.values()) {
			final List<PaliWord.Gender> glist = pword.getGender();
			for(int i = 0; i < glist.size(); i++) {
				final Map<PaliDeclension.Case, Map<PaliDeclension.Number, List<String>>> declResult = computeDeclension(pword, i);
				final PaliWord.Gender gen = glist.get(i); 
				declResult.forEach((cas, nmap) -> {
					nmap.forEach((num, lst) -> {
						for(final String t : lst) {
							final DeclinedWord dword;
							if(outputMap.containsKey(t))
								dword = outputMap.get(t);
							else
								dword = new DeclinedWord(t);
							dword.setMeaning(pword.getMeaning().get(0));
							dword.setGender(gen);
							dword.setNumber(num);
							dword.setCase(cas);
							outputMap.put(t, dword);
						}
					});
				});
			}
		}
	}

	public static void createDeclPronounsMap() {
		if(!declPronounsMap.isEmpty())
			return;
		loadPronounList();
		computeDeclension(declPronounsMap, paliPronouns);
	}

	public static void createDeclNumbersMap() {
		if(!declNumbersMap.isEmpty())
			return;
		loadNumeralList();
		computeDeclension(declNumbersMap, paliNumerals);
	}

	public static void createDeclIrrNounsMap() {
		if(!declIrrNounsMap.isEmpty())
			return;
		if(paliIrrNouns.isEmpty()) {
			// load irregular nouns/adj from the database
			final String query = "SELECT TERM,POS,PARADIGM,IN_COMPOUNDS,MEANING,SUBMEANING FROM CPED WHERE PARADIGM!='' " + 
								"AND PARADIGM!='eka' AND PARADIGM!='dvi' AND PARADIGM!='ti' AND PARADIGM!='catu' " +
								"AND PARADIGM!='sabba' AND PARADIGM!='pubba' AND PARADIGM!='asuka' " +
								"AND PARADIGM NOT LIKE 'number%';";
			try {
				if(dbConn != null) {
					final Statement stmt = dbConn.createStatement();
					final ResultSet rs = stmt.executeQuery(query);
					while(rs.next()) {
						final String term = rs.getString(1);
						final String pos = rs.getString(2);
						final String para = rs.getString(3);
						final boolean forCompounds = rs.getBoolean(4);
						final String meaning = rs.getString(5);
						final String submean = rs.getString(6);
						final PaliWord pword = new PaliWord(term);
						pword.setParadigm(para);
						pword.addPosInfo(pos);
						pword.addForCompounds(forCompounds);
						pword.addMeaning(meaning);
						pword.addSubmeaning(submean);
						paliIrrNouns.put(term, pword);
					}
					rs.close();		
					stmt.close();
				}
			} catch(SQLException e) {
				System.err.println(e);
			}
		}
		computeDeclension(declIrrNounsMap, paliIrrNouns);
	}

	/**
	 * Solves the problem of File.separator as delimiter in Windows platform.
	 */
	public static String[] safeSplit(final String input, final String delim) {
		final String safeDelim = delim.contains("\\") ? delim.replace("\\", "\\\\") : delim;
		return input.split(safeDelim);
	}

	public static String getLastPathPart(final File path) {
		if(path == null) return "";
		final String[] strTmp = safeSplit(path.getPath(), File.separator);
		return strTmp[strTmp.length - 1];
	}

	public static String getLastPathPart(final String path) {
		if(path == null || path.isEmpty()) return "";
		final String[] strTmp = safeSplit(path, File.separator);
		return strTmp[strTmp.length - 1];
	}

	public static String MD5Sum(final String text) {
		final StringBuilder result = new StringBuilder();
		try {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			final byte[] dg = md.digest(text.getBytes("UTF-8"));
			for(final byte b : dg)
				result.append(String.format("%x", b));
		} catch(UnsupportedEncodingException | NoSuchAlgorithmException e) {
			System.err.println(e);
		}
		return result.toString();
	}
}
