package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * @goal commit
 */
public class CommitMojo extends AbstractMojo {

	MojoHelper helper = MojoHelper.getInstance();
	POMUtils pomUtils = new POMUtils();
	SVNUtils svnUtils = SVNUtils.getInstance();

	/**
	 * @parameter expression="${externals.commitMessage}" default-value="[externals-maven-plugin] Commit changes"
	 */
	private String commitMessage;

	/**
	 * @parameter
	 */
	private List<Mapping> mappings;

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException {
		List<File> dirs = new ArrayList<File>();
		dirs.add(project.getBasedir());
		for (Mapping mapping : mappings) {
			File dir = new File(project.getBasedir().getAbsolutePath() + File.separator + mapping.getModule());
			if (!dir.exists()) {
				getLog().warn(dir.getAbsolutePath() + " does not exist");
			} else {
				dirs.add(dir);
			}
		}
		SVNCommitInfo info = svnUtils.commit(dirs, commitMessage, null, null);
		getLog().info("Committed revision " + info.getNewRevision() + ".");
	}

	public MojoHelper getHelper() {
		return helper;
	}

	public void setHelper(MojoHelper helper) {
		this.helper = helper;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
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
