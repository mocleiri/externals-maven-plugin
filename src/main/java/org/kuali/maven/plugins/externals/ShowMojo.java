/**
 * Copyright 2011-2014 The Kuali Foundation
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Display svn:externals definitions
 * 
 * @goal show
 * @aggregator
 */
public class ShowMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The directory to examine for svn:externals definitions
	 * 
	 * @parameter expression="${externals.directory}" default-value="${project.basedir}"
	 */
	private File directory;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Examining " + directory.getAbsolutePath());
		// Extract svn:externals info from the root of the checkout
		List<SVNExternal> externals = svnUtils.getExternals(directory);
		getLog().info("Located " + externals.size() + " svn:externals");
		int count = 1;
		for (SVNExternal external : externals) {
			getLog().info(" " + (count++) + "   " + external.getPath() + " " + external.getUrl());
		}
	}

	public MavenProject getProject() {
		return project;
	}

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File file) {
		this.directory = file;
	}
}
