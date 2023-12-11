/*
 * CSCDFieldSelectorBox.java
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

import paliplatform.*;

import java.util.*;

import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.*;

/** 
 * This widget is used in Tokenizer and LuceneFinder.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class CSCDFieldSelectorBox extends VBox {
	enum FieldGroup { SIMPLE, FULL }
	private static final List<String> simpleFieldList = Arrays.asList("body", "headings", "gatha", "note", "bold");
	private final Map<CSCDTermInfo.Field, CheckBox> fullFieldMap = new EnumMap<>(CSCDTermInfo.Field.class);
	private final Map<String, CheckBox> simpleFieldMap = new HashMap<>();
	private final Runnable updateFunc;

	public CSCDFieldSelectorBox(final Runnable func) {
		updateFunc = func;
		final EventHandler<ActionEvent> updateSimpleFields = actionEvent -> updateFields(actionEvent.getSource(), FieldGroup.SIMPLE);
		final EventHandler<ActionEvent> updateFullFields = actionEvent -> updateFields(actionEvent.getSource(), FieldGroup.FULL);
		final VBox simpleFieldsBox = new VBox();
		for(int i = 0; i < simpleFieldList.size(); i++) {
			final String text = simpleFieldList.get(i);
			final CheckBox cbSimple = new CheckBox(text);
			cbSimple.setOnAction(updateSimpleFields);
			simpleFieldMap.put(text, cbSimple);
			simpleFieldsBox.getChildren().add(cbSimple);
		}
		final VBox fullFieldsBox = new VBox();
		for(final CSCDTermInfo.Field f : CSCDTermInfo.Field.values()) {
			final CheckBox cbFull = new CheckBox(f.toString().toLowerCase());
			cbFull.setAllowIndeterminate(false);
			cbFull.setOnAction(updateFullFields);
			fullFieldMap.put(f, cbFull);
			fullFieldsBox.getChildren().add(cbFull);
			if(f.ordinal() == 3 || f.ordinal() == 9 || f.ordinal() == 13)
				fullFieldsBox.getChildren().add(new Separator());
		}
		setDefaultFieldSelection();
		final ToolBar fieldOpToolBar = new ToolBar();
		final ToggleButton detailedButton = new ToggleButton("Detailed");
		detailedButton.setSelected(false);
		fieldOpToolBar.getItems().add(detailedButton);
		getChildren().addAll(fieldOpToolBar, simpleFieldsBox);
		detailedButton.setOnAction(actionEvent -> {
			if(detailedButton.isSelected()) {
				getChildren().remove(simpleFieldsBox);
				getChildren().add(fullFieldsBox);
			} else {
				getChildren().remove(fullFieldsBox);
				getChildren().add(simpleFieldsBox);
			}
		});
		setPrefWidth(Utilities.getRelativeSize(8.2));
		setPadding(new Insets(0, 0, 0, 3));
	}

	public void init() {
		setDefaultFieldSelection();
	}

	public boolean isFieldSelected(final CSCDTermInfo.Field fld) {
		return fullFieldMap.get(fld).isSelected();
	}

	public CheckBox getSimpleBoldCheckBox() {
		return simpleFieldMap.get("bold");
	}

	public CheckBox getFullBoldCheckBox() {
		return fullFieldMap.get(CSCDTermInfo.Field.BOLD);
	}

	private void updateFields(final Object source, final FieldGroup group) {
		final CheckBox cbSrc = (CheckBox)source;
		final String text = cbSrc.getText();
		final boolean sel = cbSrc.isSelected();
		if(sel && !text.equals("bold")) {
			simpleFieldMap.get("bold").setSelected(false);
			fullFieldMap.get(CSCDTermInfo.Field.BOLD).setSelected(false);
		}
		if(group == FieldGroup.SIMPLE) {
			switch(text) {
				case "body":
					fullFieldMap.get(CSCDTermInfo.Field.BODYTEXT).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.CENTRE).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.INDENT).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.UNINDENTED).setSelected(sel);
					break;
				case "headings":
					fullFieldMap.get(CSCDTermInfo.Field.NIKAYA).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.BOOK).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.CHAPTER).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.TITLE).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.SUBHEAD).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.SUBSUBHEAD).setSelected(sel);
					break;
				case "gatha":
					fullFieldMap.get(CSCDTermInfo.Field.GATHA1).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.GATHA2).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.GATHA3).setSelected(sel);
					fullFieldMap.get(CSCDTermInfo.Field.GATHALAST).setSelected(sel);
					break;
				case "note":
					fullFieldMap.get(CSCDTermInfo.Field.NOTE).setSelected(sel);
					break;
				case "bold":
					if(sel) {
						setNonBoldFieldCheckBoxes(simpleFieldMap, false);
						setNonBoldFieldCheckBoxes(fullFieldMap, false);
					}
					fullFieldMap.get(CSCDTermInfo.Field.BOLD).setSelected(sel);
					break;
			}
			preventNoFieldSelection(simpleFieldMap);
		} else {
			if(!text.equals("note") && !text.equals("bold")) {
				int trueCount = 0;
				if(fullFieldMap.get(CSCDTermInfo.Field.BODYTEXT).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.CENTRE).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.INDENT).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.UNINDENTED).isSelected()) trueCount++;
				simpleFieldMap.get("body").setIndeterminate(false);
				if(trueCount == 4)
					simpleFieldMap.get("body").setSelected(true);
				else if(trueCount == 0)
					simpleFieldMap.get("body").setSelected(false);
				else
					simpleFieldMap.get("body").setIndeterminate(true);
				trueCount = 0;
				if(fullFieldMap.get(CSCDTermInfo.Field.NIKAYA).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.BOOK).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.CHAPTER).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.TITLE).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.SUBHEAD).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.SUBSUBHEAD).isSelected()) trueCount++;
				simpleFieldMap.get("headings").setIndeterminate(false);
				if(trueCount == 6)
					simpleFieldMap.get("headings").setSelected(true);
				else if(trueCount == 0)
					simpleFieldMap.get("headings").setSelected(false);
				else
					simpleFieldMap.get("headings").setIndeterminate(true);
				trueCount = 0;
				if(fullFieldMap.get(CSCDTermInfo.Field.GATHA1).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.GATHA2).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.GATHA3).isSelected()) trueCount++;
				if(fullFieldMap.get(CSCDTermInfo.Field.GATHALAST).isSelected()) trueCount++;
				simpleFieldMap.get("gatha").setIndeterminate(false);
				if(trueCount == 4)
					simpleFieldMap.get("gatha").setSelected(true);
				else if(trueCount == 0)
					simpleFieldMap.get("gatha").setSelected(false);
				else
					simpleFieldMap.get("gatha").setIndeterminate(true);
			} else {
				simpleFieldMap.get(text).setSelected(sel);
				if(text.equals("bold") && sel) {
					setNonBoldFieldCheckBoxes(simpleFieldMap, false);
					setNonBoldFieldCheckBoxes(fullFieldMap, false);
				}
			}
			preventNoFieldSelection(fullFieldMap);
		}
		updateFunc.run();
	}

	private void setDefaultFieldSelection() {
		setDefaultFieldSelection(simpleFieldMap);
		setDefaultFieldSelection(fullFieldMap);
	}

	private void setDefaultFieldSelection(final Map<? extends Object, CheckBox> map) {
		for(final CheckBox cb : map.values()) {
			final String text = cb.getText();
			final boolean sel = !text.equals("note") && !text.equals("bold");
			cb.setIndeterminate(false);
			cb.setSelected(sel);
		}
	}

	private void setNonBoldFieldCheckBoxes(final Map<? extends Object, CheckBox> map, final boolean val) {
		for(final CheckBox cb : map.values()) {
			if(!cb.getText().equals("bold")) {
				cb.setIndeterminate(false);
				cb.setSelected(val);
			}
		}
	}

	private void preventNoFieldSelection(final Map<? extends Object, CheckBox> map) {
		boolean sel = false;
		for(final CheckBox cb : map.values())
			sel = sel || cb.isSelected();
		if(!sel)
			setDefaultFieldSelection();
	}
}
