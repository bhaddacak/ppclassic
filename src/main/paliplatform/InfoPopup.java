/*
 * InfoPopup.java
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

import javafx.application.Platform;
import javafx.stage.Popup;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.geometry.*;

import java.util.Scanner;

/** 
 * General information popup, normally used for a specific guideline.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class InfoPopup extends Popup {
	public static enum Pos { OVER_LEFT, OVER_CENTER, OVER_RIGHT, BELOW_LEFT, BELOW_CENTER, BELOW_RIGHT }
	final Label title = new Label();
	final Label body = new Label();
	
	public InfoPopup() {
		title.setStyle("-fx-font-size:120%; -fx-font-weight:bold;");
		body.setWrapText(true);
		final VBox space = new VBox();
		space.getStyleClass().add("infopopup");
		space.getChildren().addAll(title, body);
		
		setHideOnEscape(true);
		setAutoHide(true);
		getContent().add(space);
	}
	
	public void setTitle(final String text) {
		title.setText(text);
	}
	
	public void setBody(final String text) {
		body.setText(text);
	}
	
	public void setTextWidth(final double width) {
		body.setPrefWidth(width);
	}
	
	public void setContent(final String fileName) {
		try(final Scanner in = new Scanner(PaliPlatform.class.getResourceAsStream(Utilities.TXTDIR + fileName), "UTF-8")) {
			int num = 0;
			final StringBuilder bodyText = new StringBuilder();
			while(in.hasNextLine()) {
				if(num == 0)
					setTitle(in.nextLine());
				else
					bodyText.append(in.nextLine()).append("\n");
				num++;
			}
			setBody(bodyText.toString());
		}
	}
	
	public void showPopup(final Node anchor, final Pos pos, final boolean withOffet) {
		final double yoOffet = withOffet?Utilities.getRelativeSize(-0.5):0;
		final double ybOffet = withOffet?Utilities.getRelativeSize(2.5):0;
		show(anchor, -1000, -1000);		
		Platform.runLater(() -> {
			final Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
			final double popupWidth = this.getWidth();
			final double popupHight = this.getHeight();
			switch(pos) {
				case OVER_LEFT:
					this.setX(anchorBounds.getMinX());
					this.setY(anchorBounds.getMinY() - popupHight + yoOffet);
					break;
				case OVER_CENTER:
					this.setX(anchorBounds.getMinX() - popupWidth/2.0);
					this.setY(anchorBounds.getMinY() - popupHight + yoOffet);
					break;
				case OVER_RIGHT:
					this.setX(anchorBounds.getMaxX() - popupWidth);
					this.setY(anchorBounds.getMinY() - popupHight + yoOffet);
					break;
				case BELOW_LEFT:
					this.setX(anchorBounds.getMinX());
					this.setY(anchorBounds.getMinY() + ybOffet);
					break;
				case BELOW_CENTER:
					this.setX(anchorBounds.getMinX() - popupWidth/2.0);
					this.setY(anchorBounds.getMinY() + ybOffet);
					break;
				case BELOW_RIGHT:
					this.setX(anchorBounds.getMaxX() - popupWidth);
					this.setY(anchorBounds.getMinY() + ybOffet);
					break;
			}
		});
	}
}
