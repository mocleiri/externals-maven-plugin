package org.kuali.maven.plugins.externals;

import java.io.File;

public class Project {

	GAV parent;
	GAV gav;
	GAV newGav;
	File pom;
	String pomContents;
	BuildTag buildTag;
	String version;

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

	public BuildTag getBuildTag() {
		return buildTag;
	}

	public void setBuildTag(BuildTag buildTag) {
		this.buildTag = buildTag;
	}

	public GAV getNewGav() {
		return newGav;
	}

	public void setNewGav(GAV newGav) {
		this.newGav = newGav;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
