package org.kuali.maven.plugins.externals;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.io.IOException;
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
import org.kuali.maven.common.Extractor;
import org.kuali.maven.common.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.tmatesoft.svn.core.SVNCommitInfo;

public class MojoHelper {
	private static final Logger logger = LoggerFactory.getLogger(MojoHelper.class);
	private static final String MAVEN_SNAPSHOT_TOKEN = "SNAPSHOT";
	SVNUtils svnUtils = SVNUtils.getInstance();
	XMLUtils xmlUtils = new XMLUtils();
	Extractor extractor = new Extractor();
	PropertiesUtils propertiesUtils = new PropertiesUtils();

	protected static MojoHelper instance;

	protected MojoHelper() {
		super();
	}

	public synchronized static MojoHelper getInstance() {
		if (instance == null) {
			instance = new MojoHelper();
		}
		return instance;
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

	protected List<DefaultMutableTreeNode> getNodes(List<File> files) {
		List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
		for (File file : files) {
			String pomContents = read(file);
			GAV parent = xmlUtils.getParentGAV(pomContents);
			GAV gav = xmlUtils.getGAV(pomContents);
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

	public void writePoms(DefaultMutableTreeNode node) {
		Enumeration<?> e = node.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode) e.nextElement();
			Project project = (Project) element.getUserObject();
			File pom = project.getPom();
			String oldContents = read(pom);
			String newContents = project.getPomContents();
			if (!oldContents.equals(newContents)) {
				logger.info("Updating " + pom.getAbsolutePath());
				write(pom, newContents);
			}
		}
	}

	public void updateScm(DefaultMutableTreeNode root, String scmUrlPrefix) {
		Project project = (Project) root.getUserObject();
		BuildTag buildTag = project.getBuildTag();
		String url = buildTag.getTagUrl();
		String oldXml = project.getPomContents();
		String newXml = xmlUtils.updateScm(oldXml, scmUrlPrefix, url);
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
			childProject.getParent().setVersion(getVersion(node));
			updateGavs(child);
		}
	}

	public void updateXml(DefaultMutableTreeNode node) {
		Project project = (Project) node.getUserObject();
		String version = project.getGav().getVersion();
		if (!StringUtils.isBlank(version)) {
			String oldXml = project.getPomContents();
			String newXml = xmlUtils.updateVersion(oldXml, version);
			project.setPomContents(newXml);
		}
		String parentVersion = project.getParent().getVersion();
		String oldXml = project.getPomContents();
		String newXml = xmlUtils.updateParentVersion(oldXml, parentVersion);
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

	public String getNewVersion(String version, int buildNumber, long revision, TagStyle tagStyle) {
		String trimmed = trimSnapshot(version);
		switch (tagStyle) {
		case REVISION:
			return trimmed + "-r" + revision;
		case BUILDNUMBER:
			return trimmed + "-build-" + buildNumber;
		default:
			throw new IllegalArgumentException(tagStyle + " is unknown");
		}
	}

	public String getTag(String url, String version, String artifactId, int buildNumber, long revision, TagStyle tagStyle) {
		switch (tagStyle) {
		case REVISION:
			return getRevisionTag(url, version, artifactId, revision);
		case BUILDNUMBER:
			return getBuildNumberTag(url, version, artifactId, buildNumber);
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
		sb.append("-");
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
		int count = 0;
		for (Mapping mapping : mappings) {
			String key = mapping.getVersionProperty();
			String value = properties.getProperty(key);
			if (StringUtils.isBlank(value)) {
				if (count++ != 0) {
					sb.append(", ");
				}
				sb.append(key);
			}
		}
		if (sb.length() != 0) {
			throw new IllegalArgumentException("Missing values for [" + sb.toString() + "]");
		}
	}

	public String trimSnapshot(String version) {
		if (version.toUpperCase().endsWith("-" + MAVEN_SNAPSHOT_TOKEN)) {
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
		boolean snapshot = s.toUpperCase().endsWith("-" + MAVEN_SNAPSHOT_TOKEN);
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
				sb.append("-");
			}
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	protected void write(File file, String data) {
		try {
			FileUtils.write(file, data);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String read(File file) {
		try {
			return FileUtils.readFileToString(file);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
