package org.kuali.maven.plugins.externals;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * @goal createtags
 */
public class CreateTagsMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();
	MojoHelper helper = MojoHelper.getInstance();

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
	 * @parameter expression="${svn.tagMessage}"
	 */
	private String tagMessage;

	/**
	 * @parameter expression="${svn.tagStyle}"
	 */
	private TagStyle tagStyle;

	/**
	 * @parameter expression="${svn.externalsMessage}"
	 */
	private String externalsMessage;

	@Override
	public void execute() throws MojoExecutionException {
		// Extract svn:externals info from the root of the checkout
		List<SVNExternal> externals = svnUtils.getExternals(project.getBasedir());
		// Make sure the modules listed in the pom match the svn:externals definitions and the mappings provided in the plugin config
		helper.validate(project, externals, mappings);
		// Calculate the build tag for the root
		BuildTag rootTag = helper.getBuildTag(project, tagStyle);
		// Calculate build tags for each module
		List<BuildTag> moduleTags = helper.getBuildTags(project, externals, mappings, tagStyle);
		// Create new svn:externals definitions based on the newly created tags
		List<SVNExternal> newExternals = helper.getExternals(moduleTags, mappings);
		// Create the module tags
		helper.createTags(moduleTags, tagMessage);
		// Create the root tag
		helper.createTag(rootTag, tagMessage);
		// Update svn:externals definitions on the root tag so they point to the new module tags
		SVNCommitInfo info = svnUtils.setExternals(rootTag.getTagUrl(), newExternals, externalsMessage);
		getLog().info("Set " + newExternals.size() + " externals @ " + rootTag.getTagUrl());
		getLog().info("Committed revision " + info.getNewRevision());
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
