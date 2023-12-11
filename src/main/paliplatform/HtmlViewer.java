/*
 * HtmlViewer.java
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

import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Window;
import javafx.print.*;

/** 
 * A generic HTML viewer is the common ancestor of all HTML text viewers.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class HtmlViewer extends BorderPane {
	public final WebView webView = new WebView();
	public final WebEngine webEngine = webView.getEngine();
	
	public HtmlViewer() {
		webView.setContextMenuEnabled(false);
		webView.setMinWidth(Utilities.getRelativeSize(25));
	}
	
	public final void simpleSetup() {
		setCenter(webView);
	}
	
	public void setContent(String text) {
		webEngine.loadContent(text);
	}
	
	public static void print(final WebView wview) {
		final WebEngine engine = wview.getEngine();
		final Window window = wview.getScene().getWindow();
		PrinterJob job = PrinterJob.createPrinterJob();
		if(job != null) {
			final boolean doPrint = job.showPrintDialog(window);
			if(doPrint) {
				engine.print(job);
				job.endJob();
			}			
		}
	}
}
