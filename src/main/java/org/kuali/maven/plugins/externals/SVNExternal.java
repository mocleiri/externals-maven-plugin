package org.kuali.maven.plugins.externals;

import java.io.File;

/**
 *
 * 
 * 
 */
public class SVNExternal {

	String url;
	String path;
	File workingCopyPath;

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
