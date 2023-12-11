/*
 * ViewerFXHandler.java
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

/** 
 * The handler class used to communicate with JavaScript in WebView.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */

public class ViewerFXHandler extends FXHandler {
	public ViewerFXHandler(final PaliHtmlViewer viewer) {
		super((PaliHtmlViewer)viewer);
		textOutput = "palidoc.txt";
	}
	
	public void setSearchTextFound(final boolean yn) {
		((PaliHtmlViewer)viewer).setSearchTextFound(yn);
	}
	
	public void showMessage(final String text) {
		((PaliHtmlViewer)viewer).showMessage(text);
	}
	
	public void showDictResult(final String text) {
		((PaliHtmlViewer)viewer).showDictResult(text);
	}

	public void updateClickedObject(final String className) {
		((PaliHtmlViewer)viewer).updateClickedObject(className);
	}
}
