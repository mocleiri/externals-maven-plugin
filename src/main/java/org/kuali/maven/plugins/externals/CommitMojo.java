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
 * @aggregator
 */
public class CommitMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();

	/**
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

	@Override
	public void execute() throws MojoExecutionException {
		File workingCopy = project.getBasedir();
		List<File> dirs = new ArrayList<File>();
		dirs.add(project.getBasedir());
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
}
