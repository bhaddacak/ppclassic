/*
 * About.java
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

import javafx.scene.*;
import javafx.scene.image.*;
import javafx.scene.text.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Modality;

/** 
 * The about dialog. This is a singleton.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
class About extends Stage {
	static final About INSTANCE = new About();
	private final TextArea otherInfo = new TextArea();
	
	private About() {
        setTitle("About");
		initModality(Modality.APPLICATION_MODAL);
		initOwner(PaliPlatform.stage);
		setResizable(false);
		final VBox aboutBox = new VBox(5);
		final String aboutBoxStyle = "-fx-padding: 10;" +
									"-fx-border-width: 2;" +
									"-fx-border-insets: 5;" +
									"-fx-border-radius: 5;" +
									"-fx-border-color: #909090;";
		aboutBox.setStyle(aboutBoxStyle + "-fx-border-style: solid inside;-fx-alignment: top-center;");
		final HBox logoBox = new HBox(5);
		final VBox nameBox = new VBox();
		nameBox.setStyle(aboutBoxStyle + "-fx-border-style: dotted inside;-fx-alignment: center;");
		final Label progName = new Label("Pāli Platform");
		progName.setFont(Font.font(Utilities.FONTSERIF, FontWeight.BOLD, Utilities.getRelativeSize(1.7)));
		final Label progVersion = new Label("Classic " + Utilities.VERSION);
		progVersion.setFont(Font.font(Utilities.FONTSERIF, FontWeight.MEDIUM, Utilities.getRelativeSize(1.3)));
		final Label progDesc = new Label("\nA classic tool for Pāli studies");
		final VBox versionBox = new VBox();
		progDesc.setFont(Font.font(Utilities.FONTSANS, FontWeight.MEDIUM, Utilities.getRelativeSize(1.2)));
		final String sysInfoStr = 
			"\nOperating System: " + 
			"\n  " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") " + System.getProperty("os.version") +
			"\nJava Virtual Machine: " + 
			"\n  " + System.getProperty("java.vm.name") + 
			"\n  (" +System.getProperty("java.vm.vendor") + ")" +
			"\n  " + System.getProperty("java.vm.version") +
			"\nJavaFX: " + 
			"\n  " + System.getProperty("javafx.version");
		final Label sysInfo = new Label(sysInfoStr);
		sysInfo.setFont(Font.font(Utilities.FONTSANS, FontWeight.MEDIUM, Utilities.getRelativeSize(1)));
		versionBox.getChildren().addAll(sysInfo);
		nameBox.getChildren().addAll(progName, progVersion, progDesc, versionBox);
		final ImageView logo = new ImageView(PaliPlatform.class.getResource(Utilities.IMGDIR + "lotustext-240.png").toExternalForm());
		logoBox.getChildren().addAll(nameBox, logo);
		otherInfo.setFont(Font.font(Utilities.FONTMONO, FontWeight.MEDIUM, Utilities.getRelativeSize(0.85)));
		otherInfo.setStyle("-fx-border-width:2;-fx-border-radius:5;-fx-border-color:#909090;" +
							"-fx-focus-color:transparent;-fx-text-box-border:transparent;");
		otherInfo.setEditable(false);
		otherInfo.setWrapText(true);
		final Button aboutClose = new Button("Close");
		aboutClose.setOnAction(actionEvent -> close());
		aboutBox.getChildren().addAll(logoBox, otherInfo, aboutClose);
		final Scene aboutContent = new Scene(aboutBox, Utilities.getRelativeSize(45), Utilities.getRelativeSize(37));
		setOnShowing(e -> refreshTheme());
		setScene(aboutContent);
	}
	
	public void setTextInfo(String text) {
		if(otherInfo.getText().isEmpty())
			otherInfo.setText(text);
		otherInfo.home();	
	}
	
    public void refreshTheme() {
		final Scene scene = getScene();
		scene.getStylesheets().clear();
		final String stylesheet = PaliPlatform.getCustomStyleSheet();
		if(stylesheet.length() > 0)
			scene.getStylesheets().add(stylesheet);
	}
}
