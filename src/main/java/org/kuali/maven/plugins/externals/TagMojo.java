package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * @goal tag
 */
public class TagMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();
	MojoHelper helper = MojoHelper.getInstance();

	/**
	 * @parameter expression="${externals.scmUrlPrefix}" default-value="scm:svn:"
	 */
	private String scmUrlPrefix;

	/**
	 * @parameter expression="${externals.pom}" default-value="pom.xml"
	 */
	private String pom;

	/**
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
	 * @parameter
	 */
	private List<Mapping> mappings;

	/**
	 * @parameter expression="${externals.tagMessage}"
	 */
	private String tagMessage;

	/**
	 * @parameter expression="${externals.externalsMessage}"
	 */
	private String externalsMessage;

	/**
	 * @parameter expression="${externals.buildNumberProperty}" default-value="env.BUILD_NUMBER"
	 */
	private String buildNumberProperty;

	/**
	 * Either <code>BUILDNUMBER</code> or <code>REVISION</code>
	 * 
	 * @parameter expression="${externals.tagStyle}" default-value="REVISION"
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
		BuildTag rootTag = helper.getBuildTag(project.getBasedir(), gav, TagStyle.BUILDNUMBER, buildNumber);
		// Update build info for the root node
		helper.updateBuildInfo(node, rootTag, TagStyle.BUILDNUMBER, buildNumber);
		// Calculate build tags for each module
		List<BuildTag> moduleTags = helper.getBuildTags(project.getProperties(), externals, mappings, tagStyle, buildNumber);
		// Update build information for nodes that represent an svn:external
		helper.updateBuildInfo(nodes, moduleTags, mappings, tagStyle, buildNumber);
		// Recursively update the project gav's and parent gav's
		helper.updateGavs(node);
		// Recursively update the project gav's and parent gav's
		helper.updateXml(node);
		// Update the properties in the root pom that hold version info for the modules
		helper.updateProperties(node, project.getProperties(), mappings);
		// Update the <scm> info in the root pom
		helper.updateScm(node, scmUrlPrefix);
		// Persist the modified poms to the file system
		helper.writePoms(node);
		// Create new svn:externals definitions based on the newly created tags
		List<SVNExternal> newExternals = helper.getExternals(moduleTags, mappings);
		// Create the module tags
		helper.createTags(moduleTags, tagMessage);
		// Create the root tag
		helper.createTag(rootTag, tagMessage);
		// Update svn:externals definitions on the root tag so they point to the new module tags
		SVNCommitInfo info = svnUtils.setExternals(rootTag.getTagUrl(), newExternals, externalsMessage);
		getLog().info("Set " + newExternals.size() + " externals @ " + rootTag.getTagUrl());
		getLog().info("Committed revision " + info.getNewRevision() + ".");
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

	public String getTagMessage() {
		return tagMessage;
	}

	public void setTagMessage(String message) {
		this.tagMessage = message;
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

}
