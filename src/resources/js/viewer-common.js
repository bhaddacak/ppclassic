/*
 * viewer-common.js
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

// data definition
const darkThemeObj = {
	"color": "#dfdfdf",
	"background": "#1f1f1f",
	"noteColor": "lightblue",
	"xrefColor": "#E9AA0E",
	"colorBW": "white",
	"backgroundBW": "black",
	"noteColorBW": "white",
	"xrefColorBW": "white"
};
const lightThemeObj = {
	"color": "#1f1f1f",
	"background": "#e0e0e0",
	"noteColor": "blue",
	"xrefColor": "#8B6914",
	"colorBW": "black",
	"backgroundBW": "white",
	"noteColorBW": "black",
	"xrefColorBW": "black"
};
// functions
function setFont(fontname) {
	//~ window.fxHandler.debugPrint(fontname);
	document.body.style.fontFamily = fontname;
}
function setThemeCommon(theme) {
	const themeObj = theme==='DARK'?darkThemeObj:lightThemeObj;
	document.body.style.color = themeObj['color'];
	document.body.style.background = themeObj['background'];
}
function copySelection() {
	const sel = window.getSelection();
	const text = sel.toString();
	if(text.length > 0)
		window.fxHandler.copyText(text);
}
function saveSelection() {
	const sel = window.getSelection();
	const text = sel.toString();
	if(text.length > 0)
		window.fxHandler.saveText(text);
}
function copyBody() {
	window.getSelection().selectAllChildren(document.body);
	const sel = window.getSelection();
	const text = sel.toString();
	window.fxHandler.copyText(text);
}
function saveBody() {
	window.getSelection().selectAllChildren(document.body);
	const sel = window.getSelection();
	const text = sel.toString();
	if(text.length > 0)
		window.fxHandler.saveText(text);
}
function openDeclension(term) {
	if(term.length > 0)
		window.fxHandler.openDeclension(term);
}
