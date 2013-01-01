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
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * 
 * 
 */
public abstract class AbstractTagMojo extends AbstractMojo {

	MojoHelper helper = MojoHelper.getInstance();

	public abstract TagStyle getTagStyle();

	/**
	 * The prefix Maven needs in front of the real SCM url
	 * 
	 * @parameter expression="${externals.scmUrlPrefix}" default-value="scm:svn:"
	 */
	private String scmUrlPrefix;

	/**
	 * The location of a text file that contains the svn:externals definitions
	 * 
	 * @parameter expression="${externals.file}" default-value="${project.build.directory}/checkout/svn.externals"
	 */
	private File file;

	/**
	 * The directory where the new tag is checked out to
	 * 
	 * @parameter expression="${externals.checkoutDir}" default-value="${project.build.directory}/checkout"
	 */
	private File checkoutDir;

	/**
	 * Filename pattern used to discover Maven pom's
	 * 
	 * @parameter expression="${externals.pom}" default-value="pom.xml"
	 */
	private String pom;

	/**
	 * Directories to ignore when examining the file system for Maven pom's
	 * 
	 * @parameter expression="${externals.ignoreDirectories}" default-value="src,target,.svn,.git"
	 */
	private String ignoreDirectories;

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * These mappings connect the svn:externals definitions with a property inside the root pom that controls what version each external is
	 * set to
	 * 
	 * @parameter
	 */
	private List<Mapping> mappings;

	/**
	 * The commit message for when the new tag is first created
	 * 
	 * @parameter expression="${externals.createTagMessage}" default-value="[externals-maven-plugin] Create tag"
	 */
	private String createTagMessage;

	/**
	 * The commit message for when the updated pom's and svn.external file is committed to the tag
	 * 
	 * @parameter expression="${externals.updateTagMessage}" default-value="[externals-maven-plugin] Tag maintenance"
	 */
	private String updateTagMessage;

	/**
	 * The commit message for when the <code>svn propset</code> command is used to set externals on the tag
	 * 
	 * @parameter expression="${externals.externalsMessage}" default-value="[externals-maven-plugin] Set svn:externals"
	 */
	private String externalsMessage;

	/**
	 * The property where the current build number is stored. Jenkins automatically sets an environment variable called
	 * <code>BUILD_NUMBER</code> each time a job is run
	 * 
	 * @parameter expression="${externals.buildNumberProperty}" default-value="env.BUILD_NUMBER"
	 */
	private String buildNumberProperty;

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<Mapping> mappings) {
		this.mappings = mappings;
	}

	public MavenProject getProject() {
		return project;
	}

	public String getCreateTagMessage() {
		return createTagMessage;
	}

	public void setCreateTagMessage(String message) {
		this.createTagMessage = message;
	}

	public String getExternalsMessage() {
		return externalsMessage;
	}

	public void setExternalsMessage(String externalsMessage) {
		this.externalsMessage = externalsMessage;
	}

	public String getScmUrlPrefix() {
		return scmUrlPrefix;
	}

	public void setScmUrlPrefix(String scmUrlPrefix) {
		this.scmUrlPrefix = scmUrlPrefix;
	}

	public String getPom() {
		return pom;
	}

	public void setPom(String pom) {
		this.pom = pom;
	}

	public String getIgnoreDirectories() {
		return ignoreDirectories;
	}

	public void setIgnoreDirectories(String ignoreDirectories) {
		this.ignoreDirectories = ignoreDirectories;
	}

	public String getBuildNumberProperty() {
		return buildNumberProperty;
	}

	public void setBuildNumberProperty(String buildNumberProperty) {
		this.buildNumberProperty = buildNumberProperty;
	}

	public File getCheckoutDir() {
		return checkoutDir;
	}

	public void setCheckoutDir(File checkoutDir) {
		this.checkoutDir = checkoutDir;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getUpdateTagMessage() {
		return updateTagMessage;
	}

	public void setUpdateTagMessage(String updateTagMessage) {
		this.updateTagMessage = updateTagMessage;
	}

}
