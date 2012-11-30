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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * @goal dirsync
 */
public class DirSyncMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();

	/**
	 * The Maven project object
	 *
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The message to associate with the commit
	 *
	 * @parameter expression="${externals.commitMessage}" default-value="Sync directories"
	 */
	private String commitMessage;

	/**
	 * This is the new directory containing the canonical list of files that should be under version control.
	 *
	 * @parameter expression="${externals.newDir}"
	 * @required
	 */
	private File newDir;

	/**
	 * This is the old directory containing files that were previously under version control.
	 *
	 * @parameter expression="${externals.oldDir}"
	 * @required
	 */
	private File oldDir;

	/**
	 * Regex pattern for files in new dir that should be included
	 *
	 * @parameter expression="${externals.include}" default-value="\*\*\/\*"
	 */
	private String include;

	/**
	 * Regex pattern for files in new dir that should be excluded
	 *
	 * @parameter expression="${externals.exclude}"
	 */
	private String exclude;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			getLog().info("Syncing directories");
			getLog().info("New Dir - " + newDir.getCanonicalPath());
			getLog().info("Old Dir - " + oldDir.getCanonicalPath());
			getLog().info("Include - " + include);
			getLog().info("Exclude - " + exclude);
			List<File> oldFiles = getFiles(oldDir, include, exclude);
			List<File> newFiles = getFiles(newDir, include, exclude);
			List<File> deletes = getDeletableFiles(newDir, oldDir, newFiles, oldFiles);
			if (deletes.size() == 0) {
				getLog().info("No files to delete.");
				return;
			}
			getLog().info("Located - " + deletes.size() + " files to delete");
			for (File delete : deletes) {
				getLog().info("Deleting " + delete);
			}
			svnUtils.markForDeletion(deletes);
			SVNCommitInfo info = svnUtils.commit(oldDir, commitMessage, null, null);
			getLog().info("Committed revision " + info.getNewRevision() + ".");
		} catch (Exception e) {
			throw new MojoExecutionException("Unexpected error", e);
		}
	}

	protected List<String> getRelativePaths(File dir, List<File> files) throws IOException {
		String path = dir.getCanonicalPath();
		List<String> paths = new ArrayList<String>();
		for (File file : files) {
			String filePath = file.getCanonicalPath();
			String s = StringUtils.remove(filePath, path);
			paths.add(s);
		}
		return paths;
	}

	protected List<File> getDeletableFiles(File newDir, File oldDir, List<File> newFiles, List<File> oldFiles) throws IOException {
		List<String> newPaths = getRelativePaths(newDir, newFiles);
		List<String> oldPaths = getRelativePaths(oldDir, oldFiles);

		List<File> deletableFiles = new ArrayList<File>();
		for (int i = 0; i < oldPaths.size(); i++) {
			String path = oldPaths.get(i);
			if (!newPaths.contains(path)) {
				deletableFiles.add(oldFiles.get(i));
			}
		}
		return deletableFiles;
	}

	protected List<File> getFiles(File dir, String include, String exclude) {
		SimpleScanner scanner = new SimpleScanner(dir, include, exclude);
		return scanner.getFiles();
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public File getOldDir() {
		return oldDir;
	}

	public void setOldDir(File oldDir) {
		this.oldDir = oldDir;
	}

	public File getNewDir() {
		return newDir;
	}

	public void setNewDir(File newDir) {
		this.newDir = newDir;
	}

	public String getInclude() {
		return include;
	}

	public void setInclude(String include) {
		this.include = include;
	}

	public String getExclude() {
		return exclude;
	}

	public void setExclude(String exclude) {
		this.exclude = exclude;
	}

	public MavenProject getProject() {
		return project;
	}

}
