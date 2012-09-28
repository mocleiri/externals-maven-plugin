package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal formatpoms
 * @aggregator
 */
public class FormatPomsMojo extends AbstractMojo {

	MojoHelper helper = MojoHelper.getInstance();
	POMUtils pomUtils = new POMUtils();
	SVNUtils svnUtils = SVNUtils.getInstance();

	/**
	 * @parameter expression="${externals.pom}" default-value="pom.xml"
	 */
	private String pom;

	/**
	 * @parameter expression="${externals.ignoreDirectories}" default-value="src,target,.svn,.git"
	 */
	private String ignoreDirectories;

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
		List<File> poms = helper.getPoms(project.getBasedir(), pom, ignoreDirectories);
		int count = 0;
		for (File pom : poms) {
			String xml = helper.read(pom);
			String formattedXML = pomUtils.format(xml);
			if (!xml.equals(formattedXML)) {
				count++;
				helper.write(pom, formattedXML);
				getLog().info("Formatting applied to " + pom.getAbsolutePath());
			}
		}
		getLog().info("Formatted " + count + " poms");
	}

	public MojoHelper getHelper() {
		return helper;
	}

	public void setHelper(MojoHelper helper) {
		this.helper = helper;
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
