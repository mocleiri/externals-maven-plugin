package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;

public class MojoHelper {

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
		for (SVNExternal external : externals) {
			File workingCopy = external.getWorkingCopyPath();
			if (!workingCopy.exists()) {
				throw new IllegalArgumentException(workingCopy.getAbsolutePath() + " does not exist");
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

	public boolean isEmpty(Collection<?> c) {
		return c == null || c.isEmpty();
	}

}
