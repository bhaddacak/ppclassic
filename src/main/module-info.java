module paliplatform {
	requires jdk.xml.dom;
	requires jdk.jsobject;
	requires java.desktop;
	requires java.sql;
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.swing;
	requires javafx.web;
	requires com.fasterxml.jackson.core;
	requires org.apache.lucene.core;
 	requires org.apache.lucene.analysis.common;
	requires org.apache.lucene.queryparser;
	requires org.apache.lucene.highlighter;
	exports paliplatform;
	opens paliplatform.toctree to javafx.base;
	opens paliplatform.viewer to javafx.web, javafx.base;
	opens paliplatform.grammar to javafx.web, javafx.base;
}
