package org.kuali.maven.plugins.externals;

public class Mapping implements Comparable<Mapping> {
	String module;
	String versionProperty;

	@Override
	public int compareTo(Mapping mapping) {
		return module.compareTo(mapping.getModule());
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getVersionProperty() {
		return versionProperty;
	}

	public void setVersionProperty(String property) {
		this.versionProperty = property;
	}
}
