package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal updateparents
 * @aggregator
 */
public class UpdateParents extends AbstractMojo {
	MojoHelper helper = new MojoHelper();

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @parameter expression="${svn.pom}" default-value="pom.xml"
	 */
	private String pom;

	/**
	 * @parameter expression="${svn.ignoreDirectories}" default-value="src,target,.svn,.git"
	 */
	private String ignoreDirectories;

	@Override
	public void execute() throws MojoExecutionException {
		List<File> files = helper.getPoms(project.getBasedir(), pom, ignoreDirectories);
		for (File file : files) {
			getLog().info(file.getAbsolutePath());
		}
		List<DefaultMutableTreeNode> nodes = helper.getNodes(files);
		DefaultMutableTreeNode node = helper.getTree(project.getBasedir(), nodes, pom);
		getLog().info(helper.getDisplayString(node));
	}

	public String getPom() {
		return pom;
	}

	public void setPom(String pomFiles) {
		this.pom = pomFiles;
	}

	public String getIgnoreDirectories() {
		return ignoreDirectories;
	}

	public void setIgnoreDirectories(String ignoreDirectories) {
		this.ignoreDirectories = ignoreDirectories;
	}

}