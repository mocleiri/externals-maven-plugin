package org.kuali.maven.plugins.externals;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

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

	@Override
	public void execute() throws MojoExecutionException {
		List<SVNExternal> externals = svnUtils.getExternals(project.getBasedir());
		helper.validate(project, externals, mappings);
		List<BuildTag> buildTags = helper.getBuildTags(project, externals, mappings);
		for (BuildTag buildTag : buildTags) {
			getLog().info(buildTag.getSourceUrl() + "@" + buildTag.getSourceRevision());
		}
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

}
