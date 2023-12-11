/*
 * DocInfo.java
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

/** 
 * This stores information of a document in the Pali collection.
 * @author J.R. Bhaddacak
 * @version 2.0
 * @since 2.0
 */
public class DocInfo {
	private final String id;
	private final String[] title = new String[3];
	private String toc;
	private List<String> links = new ArrayList<>();
	 
	public DocInfo(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setTitle(String s1, String s2, String s3) {
		title[0] = s1;
		title[1] = s2;
		title[2] = s3;
	}
	
	public String getTitle(int level) {
		return title[level];
	}
	
	public String getFullTitle() {
		return title[0] + ":" + title[1] + ":" +  title[2];
	}
	
	public String getFullTitleComma() {
		return title[0] + ", " + title[1] + ", " +  title[2];
	}
	
	public void setToc(String toc) {
		this.toc = toc;
	}
	
	public String getToc() {
		return toc;
	}
	
	public void setLinks(List<String> links) {
		this.links = links;
	}
	
	public List<String> getLinks() {
		return links;
	}
}
