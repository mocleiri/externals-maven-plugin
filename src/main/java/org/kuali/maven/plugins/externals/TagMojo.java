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
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * Connect svn:externals definitions with a multi-module Maven build in an intelligent manner. This mojo creates a tag from a Subversion
 * checkout containing svn:externals definitions that correspond to Maven modules. The version numbers in the respective poms are modified
 * to reflect the current build. This allows the tag to be used to create reproducible builds. The binaries Maven produces off the tag,
 * correspond exactly to the version numbers in the Maven pom's.
 * 
 * @goal tag
 */
public class TagMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();
	MojoHelper helper = MojoHelper.getInstance();

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
	 * Directores to ignore when examining the file system for Maven pom's
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

	/**
	 * Either <code>BUILDNUMBER</code>, <code>REVISION</code>, or <code>RELEASE</code>
	 * 
	 * @parameter expression="${externals.tagStyle}" default-value="BUILDNUMBER"
	 */
	private TagStyle tagStyle;

	@Override
	public void execute() throws MojoExecutionException {
		int buildNumber = helper.getBuildNumber(project, buildNumberProperty);
		GAV gav = helper.getGav(project);

		List<File> files = helper.getPoms(project.getBasedir(), pom, ignoreDirectories);
		List<DefaultMutableTreeNode> nodes = helper.getNodes(files);
		DefaultMutableTreeNode node = helper.getTree(project.getBasedir(), nodes, pom);

		// Extract svn:externals info from the root of the checkout
		List<SVNExternal> externals = svnUtils.getExternals(project.getBasedir());
		// Make sure the modules listed in the pom match the svn:externals definitions and the mappings provided in the plugin config
		helper.validate(project, externals, mappings);
		// Calculate the build tag for the root
		BuildTag rootTag = helper.getBuildTag(project.getBasedir(), gav, tagStyle, buildNumber);
		// Update build info for the root node
		helper.updateBuildInfo(node, rootTag, tagStyle, buildNumber);
		// Calculate build tags for each module
		List<BuildTag> moduleTags = helper.getBuildTags(project.getProperties(), externals, mappings, tagStyle, buildNumber);
		// Update build information for nodes that represent an svn:external
		helper.updateBuildInfo(nodes, moduleTags, mappings, tagStyle, buildNumber);
		// Recursively update the project gav's and parent gav's
		helper.updateGavs(node);
		// Recursively update the corresponding Maven pom's
		helper.updateXml(node);
		// Update the properties in the root pom that hold version info for the modules
		helper.updateProperties(node, project.getProperties(), mappings);
		// Update the <scm> info in the root pom
		helper.updateScm(node, scmUrlPrefix);
		// Create new svn:externals definitions based on the newly created tags
		List<SVNExternal> newExternals = helper.getExternals(moduleTags, mappings);
		// Create the module tags
		helper.createTags(moduleTags, createTagMessage);
		// Create the root tag
		helper.createTag(rootTag, createTagMessage);
		// Update svn:externals definitions on the root tag so they point to the new module tags
		SVNCommitInfo info = svnUtils.setExternals(rootTag.getTagUrl(), newExternals, externalsMessage);
		getLog().info("Set " + newExternals.size() + " externals @ " + rootTag.getTagUrl());
		getLog().info("Committed revision " + info.getNewRevision() + ".");
		getLog().info("Checking out - " + rootTag.getTagUrl());
		getLog().info("Checkout dir - " + checkoutDir.getAbsolutePath());
		if (checkoutDir.exists()) {
			getLog().info("Deleting " + checkoutDir.getAbsolutePath());
			helper.deleteDirectory(checkoutDir);
		}
		long start = System.currentTimeMillis();
		long revision = svnUtils.checkout(rootTag.getTagUrl(), checkoutDir, null, null);
		helper.logTime("Total checkout time: ", System.currentTimeMillis() - start);
		getLog().info("Checked out revision " + revision + ".");
		// Update the poms in the directory where the tag has been checked out
		helper.writePoms(node, project.getBasedir(), checkoutDir);
		// Update the svn.externals file in the tag
		helper.updateExternalsFile(newExternals, file);
		// Commit the changes to the tag
		helper.commitTagChanges(checkoutDir, newExternals, updateTagMessage);
	}

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

	public TagStyle getTagStyle() {
		return tagStyle;
	}

	public void setTagStyle(TagStyle tagStyle) {
		this.tagStyle = tagStyle;
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
