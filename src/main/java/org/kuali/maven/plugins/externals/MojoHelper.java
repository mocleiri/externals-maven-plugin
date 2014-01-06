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

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.kuali.common.util.Version;
import org.kuali.common.util.VersionUtils;
import org.kuali.maven.common.Extractor;
import org.kuali.maven.common.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.tmatesoft.svn.core.SVNCommitInfo;

public class MojoHelper {
	
	private static final Logger log = LoggerFactory.getLogger(MojoHelper.class);
	
	private static final String QUALIFIER_DELIMETER = "-";
	private static final Logger logger = LoggerFactory.getLogger(MojoHelper.class);
	private static final String MAVEN_SNAPSHOT_TOKEN = "SNAPSHOT";
	private static final char[] DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
	
	private static final String majorQualifierFoundersReleasePrefix = "FR";
	
	private static final String minorQualiferMilestonePrefix  = "M";
	private static final String minorQualiferReleaseCandidatePrefix  = "RC";
	
	SVNUtils svnUtils = SVNUtils.getInstance();
	POMUtils pomUtils = new POMUtils();
	Extractor extractor = new Extractor();
	PropertiesUtils propertiesUtils = new PropertiesUtils();
	NumberFormat nf = NumberFormat.getInstance();

	protected static MojoHelper instance;

	protected MojoHelper() {
		super();
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(3);
		nf.setGroupingUsed(false);
	}

	public synchronized static MojoHelper getInstance() {
		if (instance == null) {
			instance = new MojoHelper();
		}
		return instance;
	}

	public void incrementVersions(AbstractTagMojo mojo) {
		List<File> files = getPoms(mojo.getProject().getBasedir(), mojo.getPom(), mojo.getIgnoreDirectories());
		List<DefaultMutableTreeNode> nodes = getNodes(files);
		DefaultMutableTreeNode node = getTree(mojo.getProject().getBasedir(), nodes, mojo.getPom());
		incrementVersions(node);
		updateGavs(node);
		updateProperties(node, mojo.getProject().getProperties(), mojo.getMappings());
		updateXml(node);
		writePoms(node, mojo.getProject().getBasedir());
		List<SVNExternal> externals = svnUtils.getExternals(mojo.getProject().getBasedir());
		logger.info("Committing pom changes");
		commitChanges(mojo.getProject().getBasedir(), externals, "[externals-maven-plugin] prepare for next development iteration");
	}

	protected List<DefaultMutableTreeNode> getChildren(DefaultMutableTreeNode node) {
		Enumeration<?> e = node.children();
		List<DefaultMutableTreeNode> children = new ArrayList<DefaultMutableTreeNode>();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) e.nextElement();
			children.add(child);
		}
		return children;
	}

	public void incrementVersions(DefaultMutableTreeNode node) {
		Project project = (Project) node.getUserObject();
		GAV gav = project.getGav();
		String oldVersion = gav.getVersion();
		if (!StringUtils.isBlank(oldVersion)) {
			String newVersion = getNextVersion(oldVersion);
			GAV newGav = new GAV();
			newGav.setGroupId(gav.getGroupId());
			newGav.setArtifactId(gav.getArtifactId());
			newGav.setVersion(newVersion);
			project.setNewGav(newGav);
			logger.info(StringUtils.repeat("  ", node.getLevel()) + gav.getArtifactId() + ":" + gav.getArtifactId() + ":" + oldVersion + "->" + newVersion);
		}
		List<DefaultMutableTreeNode> children = getChildren(node);
		for (DefaultMutableTreeNode child : children) {
			incrementVersions(child);
		}
	}

	public void createAndUpdateTags(AbstractTagMojo mojo) {

		// Extract the Jenkins build number. Defaults to zero if no BUILD_NUMBER is set
		int buildNumber = getBuildNumber(mojo.getProject(), mojo.getBuildNumberProperty());

		// Create a GAV object from the Maven project
		GAV gav = getGav(mojo.getProject());

		// Scan the file system for pom.xml files
		List<File> files = getPoms(mojo.getProject().getBasedir(), mojo.getPom(), mojo.getIgnoreDirectories());

		// Convert the list of files into a list of node objects
		List<DefaultMutableTreeNode> nodes = getNodes(files);

		// Build a tree from the list
		DefaultMutableTreeNode node = getTree(mojo.getProject().getBasedir(), nodes, mojo.getPom());

		// Extract svn:externals info from the root of the checkout
		List<SVNExternal> externals = svnUtils.getExternals(mojo.getProject().getBasedir());

		// Make sure the modules listed in the pom match the svn:externals definitions and the mappings provided in the plugin config
		validate(mojo.getProject(), externals, mojo.getMappings());

		// Calculate the build tag for the root
		BuildTag rootTag = getBuildTag(mojo.getProject().getBasedir(), gav, mojo.getTagStyle(), buildNumber);

		// Update build info for the root node
		updateBuildInfo(node, rootTag, mojo.getTagStyle(), buildNumber);

		// Calculate build tags for each module
		List<BuildTag> moduleTags = getBuildTags(mojo.getProject().getProperties(), externals, mojo.getMappings(), mojo.getTagStyle(), buildNumber);

		// Update build information for nodes that represent an svn:external
		updateBuildInfo(nodes, moduleTags, mojo.getMappings(), mojo.getTagStyle(), buildNumber);

		// Recursively update the mojo.getProject() gav's and parent gav's
		updateGavs(node);

		// Recursively update the corresponding Maven pom's
		updateXml(node);

		// Update the properties in the root pom that hold version info for the modules
		updateProperties(node, mojo.getProject().getProperties(), mojo.getMappings());

		// Update the <scm> info in the root pom
		updateScm(node, mojo.getScmUrlPrefix());

		// Create new svn:externals definitions based on the newly created tags
		List<SVNExternal> newExternals = getExternals(moduleTags, mojo.getMappings());

		// Create the module tags
		createTags(moduleTags, mojo.getCreateTagMessage());

		// Create the root tag
		createTag(rootTag, mojo.getCreateTagMessage());

		// The directory the tag was checked out to
		File checkoutDir = mojo.getCheckoutDir();

		// Update svn:externals definitions on the root tag so they point to the new module tags
		SVNCommitInfo info = svnUtils.setExternals(rootTag.getTagUrl(), newExternals, mojo.getExternalsMessage());
		logger.info("Set " + newExternals.size() + " externals @ " + rootTag.getTagUrl());
		logger.info("Committed revision " + info.getNewRevision() + ".");
		logger.info("Checking out - " + rootTag.getTagUrl());
		logger.info("Checkout dir - " + checkoutDir.getAbsolutePath());
		if (checkoutDir.exists()) {
			logger.info("Deleting " + checkoutDir.getAbsolutePath());
			deleteDirectory(checkoutDir);
		}
		long start = System.currentTimeMillis();
		long revision = svnUtils.checkout(rootTag.getTagUrl(), checkoutDir, null, null);
		logTime("Total checkout time: ", System.currentTimeMillis() - start);
		logger.info("Checked out revision " + revision + ".");

		// Update the poms in the directory where the tag has been checked out
		writePoms(node, mojo.getProject().getBasedir(), checkoutDir);

		// Update the svn.externals file in the tag
		updateExternalsFile(newExternals, mojo.getFile());

		// Commit the changes to the tag
		commitChanges(checkoutDir, newExternals, mojo.getUpdateTagMessage());
	}

	public GAV getGav(MavenProject project) {
		GAV gav = new GAV();
		gav.setGroupId(project.getGroupId());
		gav.setArtifactId(project.getArtifactId());
		gav.setVersion(project.getVersion());
		return gav;
	}

	public String toString(GAV gav) {
		StringBuilder sb = new StringBuilder();
		sb.append(gav.getGroupId());
		sb.append(":");
		sb.append(gav.getArtifactId());
		sb.append(":");
		sb.append(gav.getVersion());
		return sb.toString();
	}

	public String getGroupId(DefaultMutableTreeNode node) {
		List<Project> projects = getProjectPath(node);
		for (Project project : projects) {
			GAV gav = project.getGav();
			if (!StringUtils.isBlank(gav.getGroupId())) {
				return gav.getGroupId();
			}
		}
		throw new IllegalStateException("Unable to determine a version");
	}

	public String getVersion(DefaultMutableTreeNode node) {
		List<Project> projects = getProjectPath(node);
		for (Project project : projects) {
			GAV gav = project.getGav();
			if (!StringUtils.isBlank(gav.getVersion())) {
				return gav.getVersion();
			}
		}
		throw new IllegalStateException("Unable to determine a version");
	}

	protected List<Project> getProjectPath(DefaultMutableTreeNode node) {
		Object[] projectObjects = node.getUserObjectPath();
		List<Project> projects = new ArrayList<Project>();
		for (Object projectObject : projectObjects) {
			projects.add((Project) projectObject);
		}
		Collections.reverse(projects);
		return projects;
	}

	public String getDisplayString(DefaultMutableTreeNode node) {
		Project project = (Project) node.getUserObject();
		GAV gav = project.getGav();
		GAV parent = project.getParent();
		int level = node.getLevel();
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.repeat(" ", level));
		sb.append(toString(parent));
		sb.append(" -> ");
		sb.append(toString(gav));
		sb.append("\n");
		Enumeration<?> children = node.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			sb.append(getDisplayString(child));
		}
		return sb.toString();
	}

	public String getDisplayString(DefaultMutableTreeNode node, File basedir, String pomFile) {
		Project project = (Project) node.getUserObject();
		File pom = project.getPom();
		String pomPath = pom.getAbsolutePath();
		String displayPath = pomPath.replace(basedir.getAbsolutePath(), "");
		displayPath = displayPath.replace(pomFile, "");
		if (!node.isRoot()) {
			displayPath = displayPath.substring(0, displayPath.length() - 1);
			int pos = displayPath.lastIndexOf(File.separator);
			displayPath = displayPath.substring(pos);
			displayPath = displayPath.replace("/", "");
		}
		int level = node.getLevel();
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.repeat(" ", level));
		sb.append(displayPath);
		sb.append("\n");
		Enumeration<?> children = node.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			sb.append(getDisplayString(child, basedir, pomFile));
		}
		return sb.toString();
	}

	/**
	 * Convert each pom.xml into a <code>Project</code> object and then store each <code>Project</code> as the user object in a
	 * <code>DefaultMutableTreeNode</code>
	 */
	protected List<DefaultMutableTreeNode> getNodes(List<File> files) {
		List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
		for (File file : files) {
			String pomContents = read(file);
			GAV parent = pomUtils.getParentGAV(pomContents);
			GAV gav = pomUtils.getGAV(pomContents);
			Project project = new Project();
			project.setPom(file);
			project.setPomContents(pomContents);
			project.setGav(gav);
			project.setParent(parent);
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(project);
			nodes.add(node);
		}
		return nodes;
	}

	public Map<String, DefaultMutableTreeNode> getMap(List<DefaultMutableTreeNode> nodes) {
		Map<String, DefaultMutableTreeNode> map = new HashMap<String, DefaultMutableTreeNode>();
		for (DefaultMutableTreeNode node : nodes) {
			Project project = (Project) node.getUserObject();
			File file = project.getPom();
			map.put(file.getAbsolutePath(), node);
		}
		return map;
	}

	/**
	 * Assemble the list of nodes into a tree, based on the directory structure.
	 */
	public DefaultMutableTreeNode getTree(File basedir, List<DefaultMutableTreeNode> nodes, String pomFile) {
		Map<String, DefaultMutableTreeNode> map = getMap(nodes);
		for (DefaultMutableTreeNode child : nodes) {
			Project project = (Project) child.getUserObject();
			File pom = project.getPom();
			File pomDir = pom.getParentFile();
			File parentPom = new File(pomDir.getParentFile(), pomFile);
			String parentPomPath = parentPom.getAbsolutePath();
			DefaultMutableTreeNode parent = map.get(parentPomPath);
			if (parent != null) {
				parent.add(child);
			}
		}
		String rootPom = basedir + File.separator + pomFile;
		DefaultMutableTreeNode root = map.get(rootPom);
		
		return root;
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

	public List<File> getPoms(File basedir, String pomFile, String ignoreDirectoriesCSV) {
		IOFileFilter fileFilter = nameFileFilter(pomFile);
		IOFileFilter dirFilter = getIgnoreDirectoriesFilter(ignoreDirectoriesCSV);
		List<File> files = new ArrayList<File>(FileUtils.listFiles(basedir, fileFilter, dirFilter));
		Collections.sort(files);
		return files;
	}

	public List<SVNExternal> getExternals(List<BuildTag> moduleTags, List<Mapping> mappings) {
		List<SVNExternal> externals = new ArrayList<SVNExternal>();
		for (int i = 0; i < mappings.size(); i++) {
			Mapping mapping = mappings.get(i);
			BuildTag moduleTag = moduleTags.get(i);
			SVNExternal external = new SVNExternal();
			external.setPath(mapping.getModule());
			external.setUrl(moduleTag.getTagUrl());
			externals.add(external);
		}
		return externals;
	}

	public boolean exists(String url) {
		ResourceLoader loader = new DefaultResourceLoader();
		Resource resource = loader.getResource(url);
		return resource.exists();
	}

	public void createTag(BuildTag buildTag, String message) {
		createTags(Collections.singletonList(buildTag), message);
	}

	public void createTags(List<BuildTag> buildTags, String message) {
		for (BuildTag buildTag : buildTags) {
			String src = buildTag.getSourceUrl();
			long revision = buildTag.getSourceRevision();
			String dst = buildTag.getTagUrl();
			boolean exists = exists(dst);
			if (exists) {
				logger.info("Skip existing tag [" + dst + "]");
				buildTag.setSkipped(true);
			} else {
				SVNCommitInfo info = svnUtils.copy(src, revision, dst, message);
				logger.info("Created [" + dst + "]");
				logger.debug("Comitted revision " + info.getNewRevision());
			}
		}
	}

	public void updateExternalsFile(List<SVNExternal> externals, File externalsFile) {
		StringBuilder sb = new StringBuilder();
		for (SVNExternal external : externals) {
			sb.append(external.getPath());
			sb.append(" ");
			sb.append(external.getUrl());
			sb.append("\n");
		}
		write(externalsFile, sb.toString());
		logger.info("Updated svn:externals control file - " + externalsFile.getAbsolutePath());
	}

	public void commitChanges(File dir, List<SVNExternal> externals, String msg) {
		List<File> workingCopyPaths = new ArrayList<File>();
		workingCopyPaths.add(dir);
		for (SVNExternal external : externals) {
			String path = dir.getAbsolutePath() + File.separator + external.getPath();
			workingCopyPaths.add(new File(path));
		}
		File[] commitDirs = workingCopyPaths.toArray(new File[workingCopyPaths.size()]);
		SVNCommitInfo info = svnUtils.commit(commitDirs, msg, null, null);
		logger.info("Committed revision " + info.getNewRevision() + ".");
	}

	public void logTime(String msg, long elapsed) {
		double millis = elapsed * 1.0D;
		double millisPerSecond = 1000;
		double millisPerMinute = 60 * millisPerSecond;
		double millisPerHour = 60 * millisPerMinute;
		if (millis > millisPerHour) {
			logger.info(msg + nf.format(millis / millisPerHour) + "h");
		} else if (millis > millisPerMinute) {
			logger.info(msg + nf.format(millis / millisPerMinute) + "m");
		} else {
			logger.info(msg + nf.format(millis / millisPerSecond) + "s");
		}
	}

	public void writePoms(DefaultMutableTreeNode node, File baseDir) {
		int count = 0;
		Enumeration<?> e = node.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode) e.nextElement();
			Project project = (Project) element.getUserObject();
			File pom = project.getPom();
			String oldContents = read(pom);
			String newContents = project.getPomContents();
			if (!oldContents.equals(newContents)) {
				logger.debug("Updating " + pom.getAbsolutePath());
				write(pom, newContents);
				count++;
			}
		}
		logger.info("Updated " + count + " Maven pom's");
	}

	public void writePoms(DefaultMutableTreeNode node, File baseDir, File checkoutDir) {
		int count = 0;
		Enumeration<?> e = node.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode) e.nextElement();
			Project project = (Project) element.getUserObject();
			File pom = project.getPom();
			String relativePath = getRelativePath(baseDir, pom);
			File newPom = new File(checkoutDir.getAbsolutePath() + File.separator + relativePath);
			String oldContents = read(pom);
			String newContents = project.getPomContents();
			if (!oldContents.equals(newContents)) {
				logger.debug("Updating " + newPom.getAbsolutePath());
				write(newPom, newContents);
				count++;
			}
		}
		logger.info("Updated " + count + " Maven pom's");
	}

	protected String getRelativePath(File dir, File file) {
		String dirPath = dir.getAbsolutePath();
		String filePath = file.getAbsolutePath();
		return filePath.replace(dirPath, "");
	}

	public void updateScm(DefaultMutableTreeNode root, String scmUrlPrefix) {
		Project project = (Project) root.getUserObject();
		BuildTag buildTag = project.getBuildTag();
		String url = buildTag.getTagUrl();
		String oldXml = project.getPomContents();
		String newXml = pomUtils.updateScm(oldXml, scmUrlPrefix, url);
		project.setPomContents(newXml);
	}

	protected String getGroupId(Project project) {
		GAV gav = project.getGav();
		GAV parent = project.getParent();
		String groupId = gav.getGroupId();
		String parentGroupId = parent.getGroupId();
		if (!StringUtils.isBlank(groupId)) {
			return groupId;
		} else {
			return parentGroupId;
		}
	}

	protected String getVersion(Project project) {
		GAV gav = project.getGav();
		GAV parent = project.getParent();
		String version = gav.getVersion();
		String parentVersion = parent.getVersion();
		if (!StringUtils.isBlank(version)) {
			return version;
		} else {
			return parentVersion;
		}
	}

	public void updateGavs(DefaultMutableTreeNode node) {
		Project project = (Project) node.getUserObject();
		if (project.getNewGav() != null) {
			project.setGav(project.getNewGav());
		}
		Enumeration<?> children = node.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			Project childProject = (Project) child.getUserObject();
			String oldParentVersion = childProject.getParent().getVersion();
			String newParentVersion = getVersion(node);
			if (!oldParentVersion.equals(newParentVersion)) {
				childProject.getParent().setVersion(newParentVersion);
			}
			updateGavs(child);
		}
	}

	public void updateProperties(DefaultMutableTreeNode node, Properties properties, List<Mapping> mappings) {
		Project project = (Project) node.getUserObject();
		Properties versionProperties = getVersionProperties(properties, mappings, node);
		String oldXml = project.getPomContents();
		String newXml = pomUtils.updateProperties(oldXml, versionProperties);
		project.setPomContents(newXml);
	}

	public Properties getVersionProperties(Properties properties, List<Mapping> mappings, DefaultMutableTreeNode node) {
		Properties newProperties = new Properties();
		for (Mapping mapping : mappings) {
			String artifactId = mapping.getModule();
			String key = mapping.getVersionProperty();
			String oldValue = properties.getProperty(key);
			if (StringUtils.isBlank(oldValue)) {
				throw new IllegalStateException("No existing value for '" + key + "'");
			}
			DefaultMutableTreeNode moduleNode = findNode(node, artifactId);
			String newValue = getVersion(moduleNode);
			newProperties.setProperty(key, newValue);
		}
		return newProperties;
	}

	protected DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, String artifactId) {
		Enumeration<?> e = node.breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode) e.nextElement();
			Project project = (Project) element.getUserObject();
			GAV gav = project.getGav();
			if (gav.getArtifactId().equals(artifactId)) {
				return element;
			}
		}
		throw new IllegalStateException("Unable to locate " + artifactId);
	}

	public void updateXml(DefaultMutableTreeNode node) {
		Project project = (Project) node.getUserObject();
		String version = project.getGav().getVersion();
		if (!StringUtils.isBlank(version)) {
			String oldXml = project.getPomContents();
			String newXml = pomUtils.updateVersion(oldXml, version);
			project.setPomContents(newXml);
		}
		String parentVersion = project.getParent().getVersion();
		String oldXml = project.getPomContents();
		String newXml = pomUtils.updateParentVersion(oldXml, parentVersion);
		project.setPomContents(newXml);
		Enumeration<?> children = node.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			updateXml(child);
		}
	}

	protected void log(DefaultMutableTreeNode node) {
		StringBuilder sb = new StringBuilder();
		logger.info(sb.toString());
	}

	public Map<String, DefaultMutableTreeNode> getGavMap(DefaultMutableTreeNode node) {
		Enumeration<?> e = node.breadthFirstEnumeration();
		Map<String, DefaultMutableTreeNode> map = new HashMap<String, DefaultMutableTreeNode>();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode) e.nextElement();
			Project project = (Project) element.getUserObject();
			GAV gav = project.getGav();
			String gavId = toString(gav);
			map.put(gavId, element);
		}
		return map;
	}

	public void validateMappings(Properties properties, List<Mapping> mappings, DefaultMutableTreeNode node) {
		for (Mapping mapping : mappings) {
			boolean valid = isValid(properties, mapping, node);
			if (!valid) {
				throw new IllegalStateException("Version mismatch on " + mapping.getModule());
			}
		}
	}

	public boolean isValid(Properties properties, Mapping mapping, DefaultMutableTreeNode node) {
		DefaultMutableTreeNode match = findNode(node, mapping.getModule());
		Project project = (Project) match.getUserObject();
		GAV gav = project.getGav();
		String propertyVersion = properties.getProperty(mapping.getVersionProperty());
		String gavVersion = gav.getVersion();
		
		if (propertyVersion.equals(gavVersion)) {
			return true;
		}
		else {
			log.warn(String.format("(artifactId, propertyVersion, gavVersion) = (%s, %s, %s)", project.getGav().getArtifactId(), propertyVersion, gavVersion));
			return false;
		}
	}

	public void validateParents(DefaultMutableTreeNode node, Map<String, DefaultMutableTreeNode> map) {
		Enumeration<?> e = node.breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode) e.nextElement();
			if (element.isRoot()) {
				continue;
			}
			Project project = (Project) element.getUserObject();
			GAV parentGav = project.getParent();
			String parentGavId = toString(parentGav);
			DefaultMutableTreeNode parent = map.get(parentGavId);
			if (parent == null) {
				throw new IllegalStateException(parentGavId + " could not be located");
			}
		}
	}

	public void fillInGavs(DefaultMutableTreeNode node) {
		Project project = (Project) node.getUserObject();
		GAV gav = project.getGav();
		String groupId = getGroupId(node);
		String version = getVersion(node);
		if (gav.getGroupId() == null) {
			gav.setGroupId(groupId);
			logger.debug("Update " + gav.getArtifactId() + "->" + groupId);
		}
		if (gav.getVersion() == null) {
			gav.setVersion(version);
			logger.debug("Update " + gav.getArtifactId() + "->" + version);
		}
		Enumeration<?> e = node.children();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) e.nextElement();
			fillInGavs(child);
		}
	}

	public void updateBuildInfo(DefaultMutableTreeNode node, BuildTag buildTag, TagStyle tagStyle, int buildNumber) {
		Project project = (Project) node.getUserObject();
		project.setBuildTag(buildTag);
		GAV oldGav = project.getGav();
		String newVersion = getNewVersion(oldGav.getVersion(), buildNumber, buildTag.getSourceRevision(), tagStyle);
		GAV newGav = new GAV();
		newGav.setGroupId(oldGav.getGroupId());
		newGav.setArtifactId(oldGav.getArtifactId());
		newGav.setVersion(newVersion);
		project.setNewGav(newGav);
		logger.info("GAV Update - [" + toString(oldGav) + "->" + newVersion + "]");
	}

	public void updateBuildInfo(List<DefaultMutableTreeNode> nodes, List<BuildTag> moduleTags, List<Mapping> mappings, TagStyle tagStyle, int buildNumber) {
		for (int i = 0; i < mappings.size(); i++) {
			Mapping mapping = mappings.get(i);
			BuildTag moduleTag = moduleTags.get(i);
			DefaultMutableTreeNode node = findNode(nodes, mapping.getModule());
			updateBuildInfo(node, moduleTag, tagStyle, buildNumber);
		}
	}

	protected DefaultMutableTreeNode findNode(List<DefaultMutableTreeNode> nodes, String artifactId) {
		for (DefaultMutableTreeNode node : nodes) {
			Project project = (Project) node.getUserObject();
			if (project.getGav().getArtifactId().equals(artifactId)) {
				return node;
			}
		}
		throw new IllegalStateException("Unable to locate " + artifactId);
	}

	public List<BuildTag> getBuildTags(Properties properties, List<SVNExternal> externals, List<Mapping> mappings, TagStyle tagStyle, int buildNumber) {
		Collections.sort(externals);
		Collections.sort(mappings);
		List<BuildTag> buildTags = new ArrayList<BuildTag>();
		for (int i = 0; i < externals.size(); i++) {
			SVNExternal external = externals.get(i);
			Mapping mapping = mappings.get(i);
			BuildTag buildTag = getBuildTag(properties, external, mapping, tagStyle, buildNumber);
			buildTags.add(buildTag);
		}
		return buildTags;
	}

	public BuildTag getBuildTag(File workingCopy, GAV gav, TagStyle tagStyle, int buildNumber) {
		String sourceUrl = svnUtils.getUrl(workingCopy);
		long sourceRevision = svnUtils.getLastRevision(workingCopy);
		String version = gav.getVersion();

		String tag = getTag(sourceUrl, version, gav.getArtifactId(), buildNumber, sourceRevision, tagStyle);

		BuildTag buildTag = new BuildTag();
		buildTag.setSourceUrl(sourceUrl);
		buildTag.setSourceRevision(sourceRevision);
		buildTag.setTagUrl(tag);
		return buildTag;
	}

	public BuildTag getBuildTag(Properties properties, SVNExternal external, Mapping mapping, TagStyle tagStyle, int buildNumber) {
		File workingCopy = external.getWorkingCopyPath();
		String sourceUrl = svnUtils.getUrl(workingCopy);
		long sourceRevision = svnUtils.getLastRevision(workingCopy);
		String version = properties.getProperty(mapping.getVersionProperty());
		String tag = getTag(sourceUrl, version, mapping.getModule(), buildNumber, sourceRevision, tagStyle);

		BuildTag buildTag = new BuildTag();
		buildTag.setSourceUrl(sourceUrl);
		buildTag.setSourceRevision(sourceRevision);
		buildTag.setTagUrl(tag);
		return buildTag;
	}

	/**
	 * Assuming version is in the form <code>1.0.0-beta-SNAPSHOT</code>, this method returns <code>1.0.0-beta-r3201</code> when
	 * <code>TagStyle=REVISION</code>, <code>1.0.0-beta-build-187</code> when <code>TagStyle=BUILDNUMBER</code>, and <code>1.0.0-beta</code>
	 * when <code>TagStyle=RELEASE</code>
	 */
	public String getNewVersion(String version, int buildNumber, long revision, TagStyle tagStyle) {
		String trimmed = trimSnapshot(version);
		switch (tagStyle) {
		case REVISION:
			return trimmed + "-r" + revision;
		case BUILDNUMBER:
			return trimmed + "-build-" + buildNumber;
		case RELEASE:
			return trimmed;
		default:
			throw new IllegalArgumentException(tagStyle + " is unknown");
		}
	}

	/**
	 * Assuming version is in the form <code>1.0.0-beta-SNAPSHOT</code>, this method will increment the 3rd digit by 1, and return
	 * <code>1.0.1-beta-SNAPSHOT</code>
	 */
	public String getNextVersion(String version) {
		if (!version.contains(MAVEN_SNAPSHOT_TOKEN)) {
			throw new IllegalArgumentException(version + " is not a " + MAVEN_SNAPSHOT_TOKEN);
		}
		Version v = VersionUtils.getVersion(version);

		String qualifier = v.getQualifier();
		
		if (qualifier != null && !qualifier.isEmpty()) {
			String qualifierParts[] = qualifier.split(QUALIFIER_DELIMETER);

			if (qualifierParts.length > 0) {

				StringBuilder nextQualifer = new StringBuilder();

				if (qualifierContainsVersionedPrefix(
						majorQualifierFoundersReleasePrefix, qualifierParts[0])) {

					// founders release qualifier is present.
					if (qualifierParts.length == 2) {
						// founders release - milestone or release candidate
						nextQualifer.append(qualifierParts[0]);
						nextQualifer.append(QUALIFIER_DELIMETER);

						if (qualifierContainsVersionedPrefix(
								minorQualiferMilestonePrefix, qualifierParts[1])) {
							// milestone

							String nextMilestone = incrementQualifier(
									minorQualiferMilestonePrefix,
									qualifierParts[1]);

							nextQualifer.append(nextMilestone);
						} else if (qualifierContainsVersionedPrefix(
								minorQualiferReleaseCandidatePrefix,
								qualifierParts[1])) {
							// release candidate
							String nextReleaseCandidate = incrementQualifier(
									minorQualiferReleaseCandidatePrefix,
									qualifierParts[1]);

							nextQualifer.append(nextReleaseCandidate);

						} else {
							// invalid minor qualifier
							throw new IllegalArgumentException(
									"invalid minor qualifier: "
											+ qualifierParts[1]);
						}

						v.setQualifier(nextQualifer.toString());
					} else {
						// only contains the main qualifier

						String nextMajorQualifer = incrementQualifier(
								majorQualifierFoundersReleasePrefix,
								qualifierParts[0]);

						v.setQualifier(nextMajorQualifer);
					}

				} else {

					// invalid major qualifier
					throw new IllegalArgumentException(
							"invalid major qualifier: " + qualifierParts[0]);
				}
			} 

		}
		else {
			Integer oldIncremental = new Integer(v.getIncremental());
			Integer newIncremental = oldIncremental + 1;
			v.setIncremental(newIncremental.toString());
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(v.getMajor());
		sb.append(".");
		sb.append(v.getMinor());
		sb.append(".");
		sb.append(v.getIncremental());
		sb.append(QUALIFIER_DELIMETER);
		if (!StringUtils.isBlank(v.getQualifier())) {
			sb.append(v.getQualifier());
			sb.append(QUALIFIER_DELIMETER);
		}
		
		sb.append(MAVEN_SNAPSHOT_TOKEN);
		return sb.toString();
	}

	private String incrementQualifier(String targetPrefix, String qualifier) {
	
		// increment this part
		if (qualifier.toLowerCase().startsWith(targetPrefix.toLowerCase())) {

			String baseQualifier = qualifier.substring(0, targetPrefix.length());
			String token = "";
			try {
				token = qualifier.substring(targetPrefix.length());
				Integer oldVersion = new Integer(token);
				Integer newVersion = oldVersion + 1;

				return baseQualifier + newVersion;
			} catch (NumberFormatException e) {
				
				throw new RuntimeException("failed to convert " + token + " suffix of " + qualifier + "into an Integer", e);
			}

		} else {
			throw new IllegalArgumentException("'" + qualifier
					+ "' does not contain a part starting with '"
					+ targetPrefix);
		}
			
	}

	/**
	 * A main qualifier is a Founders Release FR[0-9]+
	 */
	protected boolean isKnownMainQualifier (String qualifier) {
		return qualifierContainsVersionedPrefix(majorQualifierFoundersReleasePrefix, qualifier);
	}

	/**
	 * A sub qualifier is a milestone M[0-9]+ or release candidate RC[0-9]+
	 */
	protected boolean isKnownSubQualifier(String qualifier) {
		
		if (qualifierContainsVersionedPrefix(minorQualiferMilestonePrefix, qualifier) ||
			qualifierContainsVersionedPrefix(minorQualiferReleaseCandidatePrefix, qualifier)) 
			return true;
		else
			return false;
		
	}


	private boolean qualifierContainsVersionedPrefix(String targetPrefix, String qualifier) {		
		
		if (StringUtils.isBlank(qualifier)) {
			return false;
		} 
		else if (qualifier.length() < targetPrefix.length()) {
			return false;
		}
		else {
			if (qualifier.toLowerCase().startsWith(targetPrefix.toLowerCase())) {
		
				try {
					String suffix = StringUtils.substring(qualifier,
					targetPrefix.length());
					Integer.parseInt(suffix);
				} catch (NumberFormatException e) {
		
					return false;
				}
		
			}
			else {
				return false;
			}
		}
		return true;
	}

	protected boolean isDigit(char c) {
		for (char digit : DIGITS) {
			if (c == digit) {
				return true;
			}
		}
		return false;
	}

	public String getTag(String url, String version, String artifactId, int buildNumber, long revision, TagStyle tagStyle) {
		switch (tagStyle) {
		case REVISION:
			return getRevisionTag(url, version, artifactId, revision);
		case BUILDNUMBER:
			return getBuildNumberTag(url, version, artifactId, buildNumber);
		case RELEASE:
			return getReleaseTag(url, version, artifactId);
		default:
			throw new IllegalArgumentException(tagStyle + " is unknown");
		}
	}

	public int getBuildNumber(MavenProject project, String buildNumberProperty) {
		Properties properties = propertiesUtils.getMavenProperties(project);
		String buildNumber = properties.getProperty(buildNumberProperty);
		if (StringUtils.isBlank(buildNumber)) {
			logger.warn(buildNumberProperty + " is blank");
			return 0;
		} else {
			return new Integer(buildNumber);
		}
	}

	public String getReleaseTag(String url, String version, String artifactId) {
		String tagBase = extractor.getTagBase(url);
		if (StringUtils.isBlank(tagBase)) {
			throw new IllegalArgumentException("Unable to calculate tag base from [" + url + "]");
		}

		String trimmed = trimSnapshot(version);

		StringBuilder sb = new StringBuilder();
		sb.append(tagBase);
		sb.append("/");
		sb.append(artifactId);
		sb.append(QUALIFIER_DELIMETER);
		sb.append(trimmed);
		return sb.toString();
	}

	public String getBuildNumberTag(String url, String version, String artifactId, int buildNumber) {
		StringBuilder sb = new StringBuilder();
		sb.append(getBaseTag(url, version, artifactId));
		sb.append("/");
		sb.append("build-" + buildNumber);
		return sb.toString();
	}

	public String getRevisionTag(String url, String version, String artifactId, long revision) {
		StringBuilder sb = new StringBuilder();
		sb.append(getBaseTag(url, version, artifactId));
		sb.append("/");
		sb.append("r" + revision);
		return sb.toString();
	}

	protected String getBaseTag(String url, String version, String artifactId) {
		String tagBase = extractor.getTagBase(url);
		if (StringUtils.isBlank(tagBase)) {
			throw new IllegalArgumentException("Unable to calculate tag base from [" + url + "]");
		}

		Version v = parseVersion(version);
		String trimmed = trimSnapshot(version);

		StringBuilder sb = new StringBuilder();
		sb.append(tagBase);
		sb.append("/");
		sb.append("builds");
		sb.append("/");
		sb.append(artifactId);
		sb.append(QUALIFIER_DELIMETER);
		sb.append(v.getMajor());
		sb.append(".");
		sb.append(v.getMinor());
		sb.append("/");
		sb.append(trimmed);
		return sb.toString();
	}

	public void validate(MavenProject project, List<SVNExternal> externals, List<Mapping> mappings) {
		validate(externals, mappings);
		validate(project, mappings);
		validateProjectModules(project, externals);
	}

	public void validateProjectModules(MavenProject project, List<SVNExternal> externals) {
		List<String> modules = project.getModules();
		if (isEmpty(modules) && isEmpty(externals)) {
			return;
		} else if (isEmpty(externals) && !isEmpty(modules)) {
			throw new IllegalArgumentException("No externals detected but " + modules.size() + " modules were detected");
		} else if (!isEmpty(externals) && isEmpty(modules)) {
			throw new IllegalArgumentException(externals.size() + " externals were detected but no modules were detected");
		} else if (externals.size() != modules.size()) {
			throw new IllegalArgumentException("Mismatch. " + externals.size() + " externals were detected. " + modules.size() + " modules were detected");
		}
		Collections.sort(modules);
		Collections.sort(externals);
		for (int i = 0; i < modules.size(); i++) {
			String module1 = modules.get(i);
			String module2 = externals.get(i).getPath();
			if (!module1.equals(module2)) {
				throw new IllegalArgumentException("Mismatch. " + module1 + " <> " + module2);
			}
		}
	}

	public void validate(List<SVNExternal> externals, List<Mapping> mappings) {
		if (isEmpty(externals) && isEmpty(mappings)) {
			return;
		} else if (isEmpty(externals) && !isEmpty(mappings)) {
			throw new IllegalArgumentException("No externals detected but " + mappings.size() + " mappings were supplied");
		} else if (!isEmpty(externals) && isEmpty(mappings)) {
			throw new IllegalArgumentException(externals.size() + " externals were detected but no mappings were supplied");
		} else if (externals.size() != mappings.size()) {
			throw new IllegalArgumentException("Mismatch. " + externals.size() + " externals were detected. " + mappings.size() + " mappings were supplied");
		}
		for (SVNExternal external : externals) {
			File workingCopy = external.getWorkingCopyPath();
			if (!workingCopy.exists()) {
				throw new IllegalArgumentException(workingCopy.getAbsolutePath() + " does not exist");
			}
		}
	}

	public void validate(MavenProject project, List<Mapping> mappings) {
		validate(project.getProperties(), mappings);
		validateModules(project.getModules(), mappings);
	}

	public void validateModules(List<String> modules, List<Mapping> mappings) {
		Collections.sort(mappings);
		Collections.sort(modules);
		if (isEmpty(modules) && isEmpty(mappings)) {
			return;
		} else if (isEmpty(modules) && !isEmpty(mappings)) {
			throw new IllegalArgumentException("No modules detected but " + mappings.size() + " mappings were supplied");
		} else if (!isEmpty(modules) && isEmpty(mappings)) {
			throw new IllegalArgumentException(modules.size() + " modules were detected but no mappings were supplied");
		} else if (modules.size() != mappings.size()) {
			throw new IllegalArgumentException("Mismatch. " + modules.size() + " modules were detected. " + mappings.size() + " mappings were supplied");
		}
		for (int i = 0; i < modules.size(); i++) {
			String module1 = modules.get(i);
			String module2 = mappings.get(i).getModule();
			if (!module1.equals(module2)) {
				throw new IllegalArgumentException("Mismatch. " + module1 + " <> " + module2);
			}
		}

	}

	public void validate(Properties properties, List<Mapping> mappings) {
		if (isEmpty(mappings)) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		int missingCount = 0;
		for (Mapping mapping : mappings) {
			String key = mapping.getVersionProperty();
			String value = properties.getProperty(key);
			if (StringUtils.isBlank(value)) {
				if (missingCount++ != 0) {
					sb.append(", ");
				}
				sb.append(key);
			}
		}
		if (missingCount != 0) {
			throw new IllegalArgumentException("Missing values for [" + sb.toString() + "]");
		}
	}

	public String trimSnapshot(String version) {
		if (version.toUpperCase().endsWith(QUALIFIER_DELIMETER + MAVEN_SNAPSHOT_TOKEN)) {
			int length = MAVEN_SNAPSHOT_TOKEN.length() + 1;
			return StringUtils.left(version, version.length() - length);
		} else {
			return version;
		}
	}

	public boolean isEmpty(Collection<?> c) {
		return c == null || c.isEmpty();
	}

	protected Version parseVersion(String s) {
		boolean snapshot = s.toUpperCase().endsWith(QUALIFIER_DELIMETER + MAVEN_SNAPSHOT_TOKEN);
		Version version = new Version();
		version.setSnapshot(snapshot);
		String[] tokens = StringUtils.split(s, ".-");
		if (tokens.length > 0) {
			version.setMajor(tokens[0]);
		}
		if (tokens.length > 1) {
			version.setMinor(tokens[1]);
		}
		if (tokens.length > 2) {
			version.setIncremental(tokens[2]);
		}
		String qualifier = getQualifier(tokens);
		version.setQualifier(qualifier);
		return version;
	}

	protected String getQualifier(String[] tokens) {
		if (tokens.length <= 3) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 3; i < tokens.length; i++) {
			if (tokens[i].toUpperCase().equals(MAVEN_SNAPSHOT_TOKEN)) {
				break;
			}
			if (i != 3) {
				sb.append(QUALIFIER_DELIMETER);
			}
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	public void write(File file, String data) {
		try {
			FileUtils.write(file, data);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void deleteDirectory(File dir) {
		try {
			FileUtils.deleteDirectory(dir);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public String read(File file) {
		try {
			return FileUtils.readFileToString(file);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
