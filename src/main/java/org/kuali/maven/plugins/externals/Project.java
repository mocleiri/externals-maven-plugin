package org.kuali.maven.plugins.externals;

import java.io.File;

public class Project {
	String groupId;
	String artifactId;
	String version;
	File pom;
	String pomContents;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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
