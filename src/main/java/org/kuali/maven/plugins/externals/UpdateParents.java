package org.kuali.maven.plugins.externals;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal updateparents
 * @aggregator
 */
public class UpdateParents extends AbstractMojo {

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @parameter expression="${svn.parentGroupId}"
	 */
	private String parentGroupId;

	/**
	 * @parameter expression="${svn.parentArtifactId}"
	 */
	private String parentArtifactId;

	/**
	 * @parameter expression="${svn.newParentVersion}"
	 */
	private String newParentVersion;

	@Override
	public void execute() throws MojoExecutionException {
		File dir = project.getBasedir();
		IOFileFilter fileFilter = nameFileFilter("pom.xml");
		IOFileFilter dirFilter = makeMvnAware();
		Collection<File> c = FileUtils.listFiles(dir, fileFilter, dirFilter);
		List<File> files = new ArrayList<File>(c);
		Collections.sort(files);
		for (File file : files) {
			getLog().info(file.getAbsolutePath());
		}

	}

	protected IOFileFilter ignoreDirectory(String dir) {
		return notFileFilter(and(directoryFileFilter(), nameFileFilter(dir)));
	}

	protected IOFileFilter makeMvnAware() {
		IOFileFilter sourceFilter = ignoreDirectory("src");
		IOFileFilter targetFilter = ignoreDirectory("target");
		IOFileFilter svnFilter = ignoreDirectory(".svn");
		return FileFilterUtils.and(sourceFilter, targetFilter, svnFilter);
	}

	public String getParentGroupId() {
		return parentGroupId;
	}

	public void setParentGroupId(String parentGroupId) {
		this.parentGroupId = parentGroupId;
	}

	public String getParentArtifactId() {
		return parentArtifactId;
	}

	public void setParentArtifactId(String parentArtifactId) {
		this.parentArtifactId = parentArtifactId;
	}

	public String getNewParentVersion() {
		return newParentVersion;
	}

	public void setNewParentVersion(String newParentVersion) {
		this.newParentVersion = newParentVersion;
	}

}