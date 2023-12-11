/*
 * PaliPlatform.java
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
import paliplatform.viewer.*;
import paliplatform.grammar.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.scene.text.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import javafx.collections.ObservableList;

import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.*;
import java.io.*;
import java.nio.file.*;
import java.text.RuleBasedCollator;

/** 
 * The entry point of the whole program.
 * @author J.R. Bhaddacak
 * @version 2.1
 * @since 2.0
 */
final public class PaliPlatform extends Application {
	public static final String PRODUCT_NAME = "Pāli Platform Classic";
	public static Properties settings;
	static Scene scene;
	static Stage stage;
	static final TabPane tabPane = new TabPane();
	static String releaseNotes = "";
	static final HashSet<Stage> openedWindows = new HashSet<>();
	public static RuleBasedCollator paliCollator = null;
	public static Comparator<String> paliComparator = null;
	public static RuleBasedCollator cscdFileNameCollator = null;
	public static ExecutorService threadPool = null;
	public static StringConverter<Integer> integerStringConverter = null;
	public static final InfoPopup infoPopup = new InfoPopup();
	
	public static enum Theme {
		LIGHT, DARK
	}
	
	public static enum WindowType {
		TOCTREE(TOCTree.class), VIEWER(PaliHtmlViewer.class), EDITOR(PaliTextEditor.class),
		DICT(DictWin.class), DECLENSION(DeclensionWin.class), PROSODY(ProsodyWin.class),
		TOKEN(Tokenizer.class), READER(PaliTextReader.class), LISTER(SimpleLister.class),
		FINDER(DocumentFinder.class);
		private final Class windowClass;
		private WindowType(final Class cls) {
			windowClass = cls;
		}
		public Class getWindowClass() {
			return windowClass;
		}
		public static WindowType from(final Class cls) {
			WindowType result = null;
			for(WindowType wt : WindowType.values()) {
				if(cls.equals(wt.windowClass)) {
					result = wt;
					break;
				}
			}
			return result;
		}
	}
	
    @Override
    public void init() throws Exception {
		// read the application's path from the class location
		final String classPath = URLDecoder.decode(PaliPlatform.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
		final List<String> args = getParameters().getRaw();
		String appPath = "";
		if(args.isEmpty()){
			// if the path is not provided by the command line argument, use the value above
			if(classPath.toLowerCase().endsWith(".jar")) {
				appPath = classPath.substring(0, classPath.lastIndexOf(File.separatorChar)+1);
			} else {
				appPath = classPath;
			}
		} else {
			appPath = args.get(0);
		}
		final File f = new File(appPath);
		Utilities.ROOTDIR = f.isDirectory() ? appPath : "";
		Utilities.EXTRAPATH = Utilities.ROOTDIR + Utilities.COLLPATH + "extra/";
		final Path extraPath = Path.of(Utilities.EXTRAPATH);
		if(Files.notExists(extraPath))
			Files.createDirectories(extraPath);
		final String dbroot = Utilities.ROOTDIR.isEmpty() ? "./" : Utilities.ROOTDIR;
		Utilities.DB_URL = "jdbc:h2:" + dbroot + "data/db/ppclassic;IFEXISTS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
		Utilities.customDictFile = new File(Utilities.ROOTDIR + Utilities.RULESPATH + Utilities.RULES_DICT);
		Utilities.sandhiFile = new File(Utilities.ROOTDIR + Utilities.RULESPATH + Utilities.RULES_SANDHI);
		Utilities.stopwordsFile = new File(Utilities.ROOTDIR + Utilities.RULESPATH + Utilities.RULES_STOPWORDS);
		final Path sentencePath = Path.of(Utilities.ROOTDIR + Utilities.SENTENCESPATH + Utilities.SENTENCESMAIN);
		if(Files.notExists(sentencePath))
			Files.createDirectories(sentencePath);
		final Path indexPath = Path.of(Utilities.ROOTDIR + Utilities.INDEXPATH + Utilities.INDEXMAIN);
		if(Files.notExists(indexPath))
			Files.createDirectories(indexPath);
		// create H2 db persistent connection
		Utilities.dbConn = DriverManager.getConnection(Utilities.DB_URL, "sa", "");
		Utilities.dbConn.setAutoCommit(true);
		
		// load settings
		settings = MainProperties.INSTANCE.getSettings();
		Utilities.setupPaliInputCharMap();
		final String ePath = PaliPlatform.settings.getProperty("extrapath");
		if(ePath != null && !ePath.isEmpty())
			Utilities.EXTRAPATH = ePath;
		// initialize font map
		for(final Utilities.PaliScript sc : Utilities.PaliScript.values())
			Utilities.paliFontMap.put(sc, new HashSet<String>());
		// load embedded fonts
		Utilities.defBaseFontSize = Font.getDefault().getSize();
		Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "PaliPlatformIcons.ttf"), 0); // PaliPlatformIcons
		Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "fa-solid-900.ttf"), 0); // Font Awesome 6 Free Solid
		final Font fontSans = Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "DejaVuSans.ttf"), 0);
		Utilities.FONTSANS = fontSans==null ? Utilities.FONT_FALLBACK : fontSans.getFamily();
		Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "DejaVuSans-Bold.ttf"), 0);
		final Font fontSerif = Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "DejaVuSerif.ttf"), 0);
		Utilities.FONTSERIF = fontSerif==null ? Utilities.FONT_FALLBACK : fontSerif.getFamily();
		Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "DejaVuSerif-Bold.ttf"), 0);
		final Font fontMono = Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "DejaVuSansMono.ttf"), 0);
		Utilities.FONTMONO = fontMono==null ? Utilities.FONT_FALLBACK : fontMono.getFamily();
		final Font fontMonoBold = Font.loadFont(PaliPlatform.class.getResourceAsStream(Utilities.FONTDIR + "DejaVuSansMono-Bold.ttf"), 0);
		Utilities.FONTMONOBOLD = fontMonoBold==null ? Utilities.FONT_FALLBACK : fontMonoBold.getFamily();
		if(fontSans != null)
			Utilities.paliFontMap.get(Utilities.PaliScript.ROMAN).add(fontSans.getFamily());
		if(fontSerif != null)
			Utilities.paliFontMap.get(Utilities.PaliScript.ROMAN).add(fontSerif.getFamily());
		if(fontMono != null)
			Utilities.paliFontMap.get(Utilities.PaliScript.ROMAN).add(fontMono.getFamily());
		// read external fonts
		Utilities.loadExternalFonts();

		// prepare the Pali collator to sort Pali terms, including some Sanskrit characters
		// there is some problem with ḷ (maybe using lh is better)
		final String paliRule = "< A, a < Ā, ā < I, i < Ī, ī < U, u < Ū, ū < E, e < O, o" +		
								"< K, k < KH, Kh, kh < G, g < GH, Gh, gh < Ṅ, ṅ" +
								"< C, c < CH, Ch, ch < J, j < JH, Jh, jh < Ñ, ñ" +
								"< Ṭ, ṭ < ṬH, Ṭh, ṭh < Ḍ, ḍ < ḌH, Ḍh, ḍh < Ṇ, ṇ" +
								"< T, t < TH, Th, th < D, d < DH, Dh, dh < N, n" +
								"< P, p < PH, Ph, ph < B, b < BH, Bh, bh < M, m" +
								"< Y, y < R, r < Ṛ, ṛ < Ṝ, ṝ < L, l < Ḹ, ḹ < V, v" +
								"< Ś, ś < Ṣ, ṣ < S, s < H, h < Ḷ, ḷ < Ṃ, ṃ";
		paliCollator = new RuleBasedCollator(paliRule);
		paliComparator = (x, y) -> paliCollator.compare(x, y);
		
		// prepare the collator to sort CSCD file names, used in PaliDocument
		final String cscdFileNameRule = "< v < s < m < a < e < t < n";
		cscdFileNameCollator = new RuleBasedCollator(cscdFileNameRule);
		
		// prepare StringConverter used in digit-only text input
		integerStringConverter = new StringConverter<Integer>() {
			@Override
			public String toString(final Integer num) {
				if(num == null) return "";
				return num.toString();
			}
			@Override
			public Integer fromString(final String str) {
				if (str != null && str.matches("-?\\d+"))
					return Integer.valueOf(str);
				else
					return 0 ;
			}
		};
		
		// prepare info popup
		infoPopup.setContent("info-quick-starter.txt");
		infoPopup.setTextWidth(Utilities.getRelativeSize(36));

		// prepare executor thread pool for concrrent tasks
		threadPool = Executors.newFixedThreadPool(3);

		// prepare for macOS UI
		final boolean isMacOS = System.getProperty("mrj.version") != null;
		if(isMacOS) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", PRODUCT_NAME);
		}
    }
    
    @Override
    public void start(Stage stage) throws Exception {
		this.stage = stage;
		final double width = Double.parseDouble(settings.getProperty("width"));
		final double height = Double.parseDouble(settings.getProperty("height"));
        final BorderPane root = new BorderPane();
        final VBox topPart = new VBox();
        topPart.getChildren().addAll(MainMenu.INSTANCE, MainToolBar.INSTANCE);
        root.setTop(topPart);
        
        // load preliminary data
        releaseNotes = loadNotesInfo();
        Utilities.createBookmarkList(settings.getProperty("bookmarks"));
        
        // add persistent tabs
        final EnumMap<WindowType, Tab> persisTabs = new EnumMap<>(WindowType.class);
        // 1. TOC Tree of the Pali collection
		final Tab tocTab = new Tab("TOC Tree");
		tocTab.setClosable(false);
		final TextIcon tocIcon = new TextIcon("folder-tree", TextIcon.IconSet.AWESOME);
		tocTab.setGraphic(tocIcon);
		tocTab.setContent(new TOCTree());
		persisTabs.put(WindowType.TOCTREE, tocTab);
		// 2. Document Finder
		final Tab finderTab = new Tab("Document Finder");
		finderTab.setClosable(false);
		final TextIcon finderIcon = new TextIcon("magnifying-glass", TextIcon.IconSet.AWESOME);
		finderTab.setGraphic(finderIcon);
		finderTab.setContent(new DocumentFinder());
		persisTabs.put(WindowType.FINDER, finderTab);
		// 3. Simple Lister
		final Tab listerTab = new Tab("Simple Lister");
		listerTab.setClosable(false);
		final TextIcon listerIcon = new TextIcon("bars", TextIcon.IconSet.AWESOME);
		listerTab.setGraphic(listerIcon);
		listerTab.setContent(new SimpleLister());
		persisTabs.put(WindowType.LISTER, listerTab);
		// 4. Tokenizer
		final Tab tokenTab = new Tab("Tokenizer");
		tokenTab.setClosable(false);
		final TextIcon tokenIcon = new TextIcon("grip", TextIcon.IconSet.AWESOME);
		tokenTab.setGraphic(tokenIcon);
		tokenTab.setContent(new Tokenizer());
		persisTabs.put(WindowType.TOKEN, tokenTab);
		// 5. Dictionaries
		final Tab dictTab = new Tab("Dictionaries");
		dictTab.setClosable(false);
		final TextIcon dictIcon = new TextIcon("book", TextIcon.IconSet.AWESOME);
		dictTab.setGraphic(dictIcon);
		dictTab.setContent(new DictWin(null));
		persisTabs.put(WindowType.DICT, dictTab);
		
		tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
		final String[] tabsOrdered = settings.getProperty("taborder").split(",");
		for(final String t : tabsOrdered) {
			if(!t.isEmpty())
				tabPane.getTabs().add(persisTabs.get(WindowType.valueOf(t)));
		}
        
        root.setCenter(tabPane);
        
        // scene start up
        scene = new Scene(root, width, height);
		stage.getIcons().add(new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "lotusicon.png")));
        stage.setTitle(PRODUCT_NAME);
        stage.setScene(scene);
        setUserAgentStylesheet(STYLESHEET_MODENA);
        refreshTheme();
        
        // Intercepting the close request by pressing the window close button
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {  
			@Override
			public void handle(WindowEvent event) {
				exit(event);
			}
		});
        
        stage.show();
    }
    
    @Override
    public void stop() {
		final Scene s = stage.getScene();
        MainProperties.INSTANCE.saveSettings(s.getWidth(), s.getHeight(), tabPane.getTabs());
		try {
			if(Utilities.dbConn != null)
				Utilities.dbConn.close();
		} catch(SQLException e) {
			System.err.println(e);
		}
		threadPool.shutdown();
    }
    
    static void refreshTheme() {
		scene.getStylesheets().clear();
		final String stylesheet = getCustomStyleSheet();
		if(stylesheet.length() > 0)
			scene.getStylesheets().add(stylesheet);		
	}
	
	public static String getCustomStyleSheet() {
		String style = "";
		switch(settings.getProperty("theme")) {
			case "LIGHT":
				style = PaliPlatform.class.getResource(Utilities.CSSDIR + "custom_light.css").toExternalForm();
				break;
			case "DARK":
				style = PaliPlatform.class.getResource(Utilities.CSSDIR + "custom_dark.css").toExternalForm();
				break;
		}
		return style;
	}
	
	static String getCustomStyleSheet(final Theme theme) {
		final String strTheme = theme.toString().toLowerCase();
		return PaliPlatform.class.getResource(Utilities.CSSDIR + "custom_"+strTheme+".css").toExternalForm();
	}
	
	public static void openPaliHtmlViewer(final TOCTreeNode node) {
		final Object[] args = { node };
		openWindow(WindowType.VIEWER, args);
	}

	public static void openPaliHtmlViewer(final TOCTreeNode node, final String jumpTarget) {
		final Object[] args = { node, jumpTarget };
		openWindow(WindowType.VIEWER, args);
	}

	private static Stage getOpenedWindow(final Class cls) {
		// find an existing closed window
		final Stage stg = openedWindows.stream()
										.filter(s -> s.getScene().getRoot().getClass().equals(cls))
										.filter(s -> !s.isShowing())
										.findFirst()
										.orElse(null);
		return stg;
	}
		
	public static void openWindow(final WindowType win, final Object[] args) {
		final Stage stg = getOpenedWindow(win.getWindowClass());
		switch(win) {
			case TOCTREE:
				if(stg == null) {
					openNewWindow(new TOCTree(), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "folder-tree.png")), "TOC Tree");
				} else {
					showExistingWindow(stg);
				}
				break;
			case FINDER:
				if(stg == null) {
					openNewWindow(new DocumentFinder(), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "magnifying-glass.png")), "Document Finder");
				} else {
					final DocumentFinder finderWin = (DocumentFinder)stg.getScene().getRoot();
					finderWin.init();
					showExistingWindow(stg);
				}
				break;
			case LISTER:
				if(stg == null) {
					openNewWindow(new SimpleLister(), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "bars.png")), "Simple Lister");
				} else {
					final SimpleLister listerWin = (SimpleLister)stg.getScene().getRoot();
					listerWin.init();
					showExistingWindow(stg);
				}
				break;
			case TOKEN:
				if(stg == null) {
					openNewWindow(new Tokenizer(), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "grip.png")), "Tokenizer");
				} else {
					final Tokenizer tokWin = (Tokenizer)stg.getScene().getRoot();
					tokWin.init();
					showExistingWindow(stg);
				}
				break;
			case DICT:
				if(stg == null) {
					openNewWindow(new DictWin(args), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "book.png")), "Dictionaries");
				} else {
					final DictWin dictWin = (DictWin)stg.getScene().getRoot();
					dictWin.init(args);
					showExistingWindow(stg);
				}
				break;
			case DECLENSION:
				if(stg == null) {
					openNewWindow(new DeclensionWin(args), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "table-cells.png")), "Declension Table");
				} else {
					final DeclensionWin declWin = (DeclensionWin)stg.getScene().getRoot();
					declWin.init(DeclensionWin.Mode.NOUN, args);					
					showExistingWindow(stg);
				}
				break;
			case PROSODY:
				if(stg == null) {
					openNewWindow(new ProsodyWin(args), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "music.png")), "Prosody");
				} else {
					final ProsodyWin prosWin = (ProsodyWin)stg.getScene().getRoot();
					final String text = (String)args[0];
					prosWin.analyze(text);
					showExistingWindow(stg);
				}
				break;
			case EDITOR:
				if(stg == null) {
					final PaliTextEditor editor = new PaliTextEditor(args);
					openNewWindow(editor, new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "pencil.png")), "");
				} else {
					final PaliTextEditor editor = (PaliTextEditor)stg.getScene().getRoot();
					if(args == null) {
						// open a file
						if(editor.openFile()) {
							editor.resetFont();
							showExistingWindow(stg);
						}
					} else {
						if(args[0] instanceof File) {
							// open with specified file
							if(editor.openFile((File)args[0])) {
								editor.resetFont();
								showExistingWindow(stg);
							}
						} else if(args[0] instanceof String) {
							// open with specified content
							final String strScript = (String)args[0];
							if(strScript.isEmpty()) {
								// open a file
								if(editor.openFile()) {
									editor.resetFont();
									showExistingWindow(stg);
								}
							} else {
								final Utilities.PaliScript script = Utilities.PaliScript.valueOf(strScript);
								editor.clearEditor(script);
								final String content = args.length<2 ? "" : (String)args[1];
								editor.setContent(content);
								showExistingWindow(stg);
							}
						}
					}
				}
				break;
			case READER:
				if(stg == null) {
					openNewWindow(new PaliTextReader(args), 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "book-open.png")), PaliTextReader.TITLE);
				} else {
					final PaliTextReader reader = (PaliTextReader)stg.getScene().getRoot();
					reader.init(args);
					showExistingWindow(stg);
				}
				break;
			case VIEWER:
				if(args == null) break;
				final TOCTreeNode node = (TOCTreeNode)args[0];
				final String jumpTarget = args.length > 1 ? (String)args[1] : "";
				final PaliHtmlViewer viewer;
				if(stg == null) {
					viewer = new PaliHtmlViewer(node, jumpTarget);
					openNewWindow(viewer, 
						new Image(PaliPlatform.class.getResourceAsStream(Utilities.IMGDIR + "file-lines.png")), node.getTextName());
				} else {
					viewer = (PaliHtmlViewer)stg.getScene().getRoot();
					((PaliHtmlViewer)viewer).init(node, jumpTarget);
					showExistingWindow(stg);
				}
				break;
		}
		// System.out.println(openedWindows.size() + " opened window(s)");
	}
	
	private static void openNewWindow(final Parent p, final Image icon, final String title) {
		final Stage window = new Stage();
		window.getIcons().add(icon);
		window.setTitle(title);
		if(p instanceof PaliTextEditor) {
			Platform.runLater(() -> ((PaliTextEditor)p).setStage(window));
		} else if(p instanceof PaliHtmlViewer) {
			Platform.runLater(() -> ((PaliHtmlViewer)p).setStage(window));
		} else if(p instanceof PaliTextReader) {
			Platform.runLater(() -> ((PaliTextReader)p).setStage(window));
		}
		final Scene sc = new Scene(p);
		final String stylesheet = getCustomStyleSheet();
		if(stylesheet.length() > 0)
			sc.getStylesheets().add(stylesheet);		
		window.setScene(sc);
		openedWindows.add(window);
		window.show();
	}
	
	private static void showExistingWindow(final Stage stg) {
		Scene sc = stg.getScene();
		sc.getStylesheets().clear();
		final String stylesheet = getCustomStyleSheet();
		if(stylesheet.length() > 0)
			sc.getStylesheets().add(stylesheet);
		stg.show();		
	}
	
	public static void openDict(final String term) {
		final Object[] args = { term };
		openWindow(WindowType.DICT, args);
	}
	
	public static void showDict(final String term) {
		final Object[] winObjs = getMainWinInstance(WindowType.DICT);
		if(winObjs[0] != null) {
			final DictWin dictWin = (DictWin)winObjs[0];
			tabPane.getSelectionModel().select((Integer)winObjs[1]);
			dictWin.setSearchInput(term);
		}
	}
	
	public static void showFinder(final String term) {
		final Object[] winObjs = getMainWinInstance(WindowType.FINDER);
		if(winObjs[0] != null) {
			final DocumentFinder finderWin = (DocumentFinder)winObjs[0];
			tabPane.getSelectionModel().select((Integer)winObjs[1]);
			finderWin.setSearchInput(term);
		}
	}
	
	public static void sendToTokenizer(final TreeItem<TOCTreeNode> titem) {
		final Object[] winObjs = getMainWinInstance(WindowType.TOKEN);
		if(winObjs[0] != null) {
			final Tokenizer tokenWin = (Tokenizer)winObjs[0];
			tokenWin.addDoc(titem);
		}
	}

	public static Object[] getMainWinInstance(final WindowType wtype) {
		final Object[] result = new Object[2];
		final ObservableList<Tab> tabList = tabPane.getTabs();
		for(int i = 0; i<tabList.size(); i++) {
			if((wtype == WindowType.DICT && tabList.get(i).getContent() instanceof DictWin)
				|| (wtype == WindowType.FINDER && tabList.get(i).getContent() instanceof DocumentFinder)
				|| (wtype == WindowType.TOKEN && tabList.get(i).getContent() instanceof Tokenizer)) {
				result[0] = tabList.get(i).getContent(); // the window instance
				result[1] = i; // the tab index
				break;
			}
		}
		return result;
	}
	
	public static void setTheme(final Scene scn, final Theme thm) {
		final String stylesheet = getCustomStyleSheet(thm);
		if(stylesheet.length() > 0) {
			scn.getStylesheets().clear();
			scn.getStylesheets().add(stylesheet);		
		}
	}
	
	/** 
	 * Loads the program's release notes from a provided text file. 
	 * The load is done only once when the program starts, 
	 * and the information is retained.
	 * The information is shown in About dialog.
	 */
	private String loadNotesInfo() throws Exception {
		final StringBuilder text = new StringBuilder();
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(Utilities.TXTDIR + "relnotes.txt"), "UTF-8")) {
			while(in.hasNextLine())
				text.append(in.nextLine()).append("\n");
		}
		return text.toString();	
	}	
	
    static void about() {
		About.INSTANCE.setTextInfo(releaseNotes);
		About.INSTANCE.showAndWait();
	}
	
    static void exit(final WindowEvent event) {
		final ConfirmAlert quitAlert = new ConfirmAlert(stage, ConfirmAlert.ConfirmType.QUIT);
		if(Boolean.parseBoolean(settings.getProperty("exit-ask"))) {
			final Optional<ButtonType> result = quitAlert.showAndWait();
			if(result.isPresent()) {
				if(result.get() == quitAlert.getConfirmButtonType()) {
					Platform.exit();
				} else {
					if(event != null)
						event.consume();
				}
			}	
		} else {
			Platform.exit();
		}
	}
}
