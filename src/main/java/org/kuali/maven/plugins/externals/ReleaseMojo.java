/**
 * Copyright 2011-2012 The Kuali Foundation
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
 * @goal release
 */
public class ReleaseMojo extends AbstractTagMojo {

	@Override
	public TagStyle getTagStyle() {
		return TagStyle.RELEASE;
	}

	@Override
	public void execute() throws MojoExecutionException {
		helper.createAndUpdateTag(this);
		getLog().info("Release has been successfully tagged.");
		getLog().info("Incrementing version numbers for the next development iteration");
		helper.incrementVersions(this);
	}

}
