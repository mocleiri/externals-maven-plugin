package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * Examine the local working copy of a project for any svn:externals definitions. Commit any local changes including any changes under the svn:externals directories.
 * 
 * @goal commit
 * @aggregator
 */
public class CommitMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();

	/**
	 * The message to associate with the commit
	 * 
	 * @parameter expression="${externals.commitMessage}" default-value="[externals-maven-plugin] Commit changes"
	 */
	private String commitMessage;

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The directory where a local working copy of the project is checked out
	 * 
	 * @parameter expression="${externals.workingCopy}" default-value="${project.basedir}"
	 */
	private File workingCopy;

	@Override
	public void execute() throws MojoExecutionException {
		List<File> dirs = new ArrayList<File>();
		dirs.add(workingCopy);
		List<SVNExternal> externals = svnUtils.getExternals(workingCopy);
		for (SVNExternal external : externals) {
			File dir = new File(workingCopy.getAbsolutePath() + File.separator + external.getPath());
			if (!dir.exists()) {
				getLog().warn(dir.getAbsolutePath() + " does not exist");
			} else {
				dirs.add(dir);
			}
		}
		getLog().info("Committing changes in:");
		for (File dir : dirs) {
			getLog().info(dir.getAbsolutePath());
		}
		SVNCommitInfo info = svnUtils.commit(dirs, commitMessage, null, null);
		long newRevision = info.getNewRevision();
		if (newRevision == -1) {
			getLog().info("No changes detected.");
		} else {
			getLog().info("Committed revision " + info.getNewRevision() + ".");
		}
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public MavenProject getProject() {
		return project;
	}

	public File getWorkingCopy() {
		return workingCopy;
	}

	public void setWorkingCopy(File workingCopy) {
		this.workingCopy = workingCopy;
	}
}
