package org.kuali.maven.plugins.externals;

import java.io.File;

public class Project {

	GAV parent;
	GAV gav;
	File pom;
	String pomContents;

	public GAV getParent() {
		return parent;
	}

	public void setParent(GAV parent) {
		this.parent = parent;
	}

	public GAV getGav() {
		return gav;
	}

	public void setGav(GAV gav) {
		this.gav = gav;
	}

	public File getPom() {
		return pom;
	}

	public void setPom(File pom) {
		this.pom = pom;
	}

	public String getPomContents() {
		return pomContents;
	}

	public void setPomContents(String pomContents) {
		this.pomContents = pomContents;
	}
}
