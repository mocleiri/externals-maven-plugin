package org.kuali.maven.plugins.externals;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.util.ArrayList;
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
	 * @parameter expression="${svn.pomFiles}" default-value="pom.xml"
	 */
	private String pomFiles;

	/**
	 * @parameter expression="${svn.ignoreDirectories}" default-value="src,target,.svn,.git"
	 */
	private String ignoreDirectories;

	@Override
	public void execute() throws MojoExecutionException {
		File dir = project.getBasedir();
		IOFileFilter fileFilter = nameFileFilter(pomFiles);
		IOFileFilter dirFilter = getIgnoreDirectoriesFilter(ignoreDirectories);
		List<File> files = new ArrayList<File>(FileUtils.listFiles(dir, fileFilter, dirFilter));
		Collections.sort(files);
		for (File file : files) {
			getLog().info(file.getAbsolutePath());
		}

	}

	protected IOFileFilter getIgnoreDirectoryFilter(String dir) {
		return notFileFilter(and(directoryFileFilter(), nameFileFilter(dir)));
	}

	protected IOFileFilter getIgnoreDirectoriesFilter(String csv) {
		return getIgnoreDirectoriesFilter(csv.split(","));
	}

	protected IOFileFilter getIgnoreDirectoriesFilter(String... directories) {
		IOFileFilter[] filters = new IOFileFilter[directories.length];
		for (int i = 0; i < filters.length; i++) {
			String dir = directories[i].trim();
			filters[i] = getIgnoreDirectoryFilter(dir);
		}
		return FileFilterUtils.and(filters);
	}

	public String getPomFiles() {
		return pomFiles;
	}

	public void setPomFiles(String pomFiles) {
		this.pomFiles = pomFiles;
	}

	public String getIgnoreDirectories() {
		return ignoreDirectories;
	}

	public void setIgnoreDirectories(String ignoreDirectories) {
		this.ignoreDirectories = ignoreDirectories;
	}

}