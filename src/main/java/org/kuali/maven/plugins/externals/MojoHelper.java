package org.kuali.maven.plugins.externals;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.kuali.maven.common.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.tmatesoft.svn.core.SVNCommitInfo;

public class MojoHelper {
	private static final Logger LOG = LoggerFactory.getLogger(MojoHelper.class);
	private static final String MAVEN_SNAPSHOT_TOKEN = "SNAPSHOT";

	SVNUtils svnUtils = SVNUtils.getInstance();
	Extractor extractor = new Extractor();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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

	public boolean exists(String url) {
		ResourceLoader loader = new DefaultResourceLoader();
		Resource resource = loader.getResource(url);
		return resource.exists();
	}

	public void createTags(List<BuildTag> buildTags, String message) {
		for (BuildTag buildTag : buildTags) {
			String src = buildTag.getSourceUrl();
			long revision = buildTag.getSourceRevision();
			String dst = buildTag.getTagUrl();
			boolean exists = exists(dst);
			if (exists) {
				LOG.info("Skip existing tag [" + dst + "]");
				buildTag.setSkipped(true);
			} else {
				SVNCommitInfo info = svnUtils.copy(src, revision, dst, message);
				LOG.info("Created [" + dst + "] Revision " + info.getNewRevision());
			}
		}
	}

	public List<BuildTag> getBuildTags(MavenProject project, List<SVNExternal> externals, List<Mapping> mappings) {
		Collections.sort(externals);
		Collections.sort(mappings);
		List<BuildTag> buildTags = new ArrayList<BuildTag>();
		for (int i = 0; i < externals.size(); i++) {
			SVNExternal external = externals.get(i);
			Mapping mapping = mappings.get(i);
			BuildTag buildTag = getBuildTag(project, external, mapping);
			buildTags.add(buildTag);
		}
		return buildTags;
	}

	public BuildTag getBuildTag(MavenProject project, SVNExternal external, Mapping mapping) {
		File workingCopy = external.getWorkingCopyPath();
		String sourceUrl = svnUtils.getUrl(workingCopy);
		long sourceRevision = svnUtils.getLastRevision(workingCopy);

		String tagBase = extractor.getTagBase(sourceUrl);
		if (StringUtils.isBlank(tagBase)) {
			throw new IllegalArgumentException("Unable to calculate tag base from [" + sourceUrl + "]");
		}

		String version = project.getProperties().getProperty(mapping.getVersionProperty());
		Version v = parseVersion(version);
		String trimmed = trimSnapshot(version);

		StringBuilder sb = new StringBuilder();
		sb.append(tagBase);
		sb.append("/");
		sb.append("builds");
		sb.append("/");
		sb.append(mapping.getModule());
		sb.append("-");
		sb.append(v.getMajor());
		sb.append(".");
		sb.append(v.getMinor());
		sb.append("/");
		sb.append(trimmed);
		sb.append("/");
		sb.append("r" + sourceRevision);

		BuildTag buildTag = new BuildTag();
		buildTag.setSourceUrl(sourceUrl);
		buildTag.setSourceRevision(sourceRevision);
		buildTag.setTagUrl(sb.toString());
		return buildTag;
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
}
