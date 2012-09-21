package org.kuali.maven.plugins.externals;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal createtags
 */
public class CreateTagsMojo extends AbstractMojo {

	/**
	 * 
	 */
	private List<Mapping> mappings;

	@Override
	public void execute() throws MojoExecutionException {
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<Mapping> mappings) {
		this.mappings = mappings;
	}

}
