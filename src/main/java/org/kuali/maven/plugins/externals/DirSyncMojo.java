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
 * This mojo examines <code>oldDir</code> for any files that are not also in <code>newDir</code>. If it finds any, it deletes them from
 * Subversion.
 *
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
	 * This is the new directory containing files that should be under version control.
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
	 * CSV list of regex patterns for files that should be included
	 *
	 * @parameter expression="${externals.include}" default-value="\*\*\/*"
	 */
	private String includes;

	/**
	 * CSV list of regex patterns for files that should be excluded
	 *
	 * @parameter expression="${externals.exclude}" default-value="\*\*\/.svn/\*\*,\*\*\/.git/\*\*"
	 */
	private String excludes;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			getLog().info("Syncing directories");
			getLog().info("New Dir - " + newDir.getCanonicalPath());
			getLog().info("Old Dir - " + oldDir.getCanonicalPath());
			getLog().info("Include - " + includes);
			getLog().info("Exclude - " + excludes);
			List<File> oldFiles = getFiles(oldDir, includes, excludes);
			List<File> newFiles = getFiles(newDir, includes, excludes);
			List<File> deletes = getDeletableFiles(newDir, oldDir, newFiles, oldFiles);
			if (deletes.size() == 0) {
				getLog().info("No files to delete.");
				return;
			}
			getLog().info("Located - " + deletes.size() + " files to delete");
			for (File delete : deletes) {
				getLog().info("Deleting " + delete.getCanonicalPath());
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

	protected List<File> getFiles(File dir, String includes, String excludes) {

		SimpleScanner scanner = new SimpleScanner(dir, includes, excludes);
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

	public String getIncludes() {
		return includes;
	}

	public void setIncludes(String include) {
		this.includes = include;
	}

	public String getExcludes() {
		return excludes;
	}

	public void setExcludes(String exclude) {
		this.excludes = exclude;
	}

	public MavenProject getProject() {
		return project;
	}

}
