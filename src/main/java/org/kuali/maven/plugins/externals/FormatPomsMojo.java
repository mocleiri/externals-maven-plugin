package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal formatpoms
 */
public class FormatPomsMojo extends AbstractMojo {

	MojoHelper helper = MojoHelper.getInstance();
	POMUtils pomUtils = new POMUtils();

	/**
	 * @parameter expression="${externals.pom}" default-value="pom.xml"
	 */
	private String pom;

	/**
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
}
