package org.kuali.maven.plugins.externals;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

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

	public boolean isEmpty(Collection<?> c) {
		return c == null || c.isEmpty();
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
	}

	public void validate(Properties properties, List<Mapping> mappings) {
		if (isEmpty(mappings)) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (Mapping mapping : mappings) {
			String key = mapping.getProperty();
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

}
