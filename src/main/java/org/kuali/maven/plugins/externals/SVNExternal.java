/**
 * Copyright 2011-2013 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.maven.plugins.externals;

import java.io.File;

/**
 *
 * 
 * 
 */
public class SVNExternal implements Comparable<SVNExternal> {

	String url;
	String path;
	File workingCopyPath;

	
	public SVNExternal(String url, String path, File workingCopyPath) {
		super();
		this.url = url;
		this.path = path;
		this.workingCopyPath = workingCopyPath;
	}

	public SVNExternal() {
	}

	@Override
	public int compareTo(SVNExternal other) {
		return path.compareTo(other.getPath());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public File getWorkingCopyPath() {
		return workingCopyPath;
	}

	public void setWorkingCopyPath(File workingCopyPath) {
		this.workingCopyPath = workingCopyPath;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
