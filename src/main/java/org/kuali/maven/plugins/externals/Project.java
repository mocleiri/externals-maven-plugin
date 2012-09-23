package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

public class Project {
	Project parent;
	List<Project> modules;
	String groupId;
	String artifactId;
	String version;
	File pom;
	String pomContents;

	public Project getParent() {
		return parent;
	}

	public void setParent(Project parent) {
		this.parent = parent;
	}

	public List<Project> getModules() {
		return modules;
	}

	public void setModules(List<Project> modules) {
		this.modules = modules;
	}

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
