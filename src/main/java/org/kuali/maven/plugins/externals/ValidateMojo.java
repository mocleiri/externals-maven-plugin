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
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Make sure the aggregate checkout is "self-contained". Make sure the parent versions of the svn:externals modules point back to the correct root pom. Make sure the properties
 * used to figure out which version of the child modules to use, actually match up with the versions declared in the child modules.
 * 
 * @goal validate
 */
public class ValidateMojo extends AbstractMojo {

	SVNUtils svnUtils = SVNUtils.getInstance();
	MojoHelper helper = MojoHelper.getInstance();

	/**
	 * Filename pattern used to discover Maven pom's
	 * 
	 * @parameter expression="${externals.pom}" default-value="pom.xml"
	 */
	private String pom;

	/**
	 * Directories to ignore when examining the file system for Maven pom's
	 * 
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
	 * These mappings connect the svn:externals definitions with a property inside the root pom that controls what version each external is set to
	 * 
	 * @parameter
	 */
	private List<Mapping> mappings;

	@Override
	public void execute() throws MojoExecutionException {
		List<File> files = helper.getPoms(project.getBasedir(), pom, ignoreDirectories);
		List<DefaultMutableTreeNode> nodes = helper.getNodes(files);
		// This tree is based on the file system directory structure
		// It will always have everything correctly connected together
		DefaultMutableTreeNode node = helper.getTree(project.getBasedir(), nodes, pom);
		// Make sure each gav is fully populated
		helper.fillInGavs(node);
		// Populate a map keyed by the gav id
		Map<String, DefaultMutableTreeNode> map = helper.getGavMap(node);
		// Validate that all of the parents are contained in the map
		helper.validateParents(node, map);
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

	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<Mapping> mappings) {
		this.mappings = mappings;
	}
}
