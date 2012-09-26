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
		int buildNumber = -1;
		if (tagStyle.equals(TagStyle.BUILDNUMBER)) {
			buildNumber = helper.getBuildNumber(project, buildNumberProperty);
		}

		List<File> files = helper.getPoms(project.getBasedir(), pom, ignoreDirectories);
		List<DefaultMutableTreeNode> nodes = helper.getNodes(files);
		DefaultMutableTreeNode node = helper.getTree(project.getBasedir(), nodes, pom);

		// Extract svn:externals info from the root of the checkout
		List<SVNExternal> externals = svnUtils.getExternals(project.getBasedir());
		// Make sure the modules listed in the pom match the svn:externals definitions and the mappings provided in the plugin config
		helper.validate(project, externals, mappings);
		// Calculate the build tag for the root
		BuildTag rootTag = helper.getBuildTag(project, tagStyle, buildNumber);
		Project rootProject = (Project) node.getUserObject();
		rootProject.setBuildTag(rootTag);
		// Calculate build tags for each module
		List<BuildTag> moduleTags = helper.getBuildTags(project, externals, mappings, tagStyle, buildNumber);
		// Update build information as necessary
		helper.updateBuildInfo(nodes, moduleTags, mappings, tagStyle, buildNumber);
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

}
