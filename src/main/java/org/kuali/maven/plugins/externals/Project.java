/**
 * Copyright 2011-2012 The Kuali Foundation
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
