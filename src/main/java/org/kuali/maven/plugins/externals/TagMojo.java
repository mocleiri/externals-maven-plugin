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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Connect svn:externals definitions with a multi-module Maven build in an intelligent manner. This mojo creates a tag from a Subversion
 * checkout containing svn:externals definitions that correspond to Maven modules. The version numbers in the respective poms are modified
 * to reflect the current build. This allows the tag to be used to create reproducible builds. The binaries Maven produces off the tag,
 * correspond exactly to the version numbers in the Maven pom's.
 * 
 * @goal tag
 */
public class TagMojo extends AbstractTagMojo {

	/**
	 * Either <code>BUILDNUMBER</code>, <code>REVISION</code>, or <code>RELEASE</code>
	 * 
	 * @parameter expression="${externals.tagStyle}" default-value="BUILDNUMBER"
	 */
	private TagStyle tagStyle;

	@Override
	public void execute() throws MojoExecutionException {
		helper.createAndUpdateTags(this);
	}

	public TagStyle getTagStyle() {
		return tagStyle;
	}

	public void setTagStyle(TagStyle tagStyle) {
		this.tagStyle = tagStyle;
	}
}
