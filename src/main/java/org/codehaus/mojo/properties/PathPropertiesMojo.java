/**
 * Copyright 2009-2012 The Kuali Foundation
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
package org.codehaus.mojo.properties;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Translate the indicated properties into classpath friendly form.
 *
 * eg Transform <code>org.kuali.rice</code> into <code>org/kuali/rice</code>
 *
 * A new project property with ".path" added as a suffix gets set with the transformed value
 *
 * @goal path-properties
 */
public class PathPropertiesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter
     * @required
     */
    private String[] properties;

    /**
     * @parameter expression="${properties.suffix} default-value=".path"
     * @required
     */
    private String suffix;

    @Override
    public void execute() throws MojoExecutionException {
        Properties props = project.getProperties();
        for (String property : properties) {
            String value = props.getProperty(property);
            if (StringUtils.isBlank(value)) {
                continue;
            } else {
                String newValue = value.replace(".", "/");
                String newKey = property + suffix;
                props.setProperty(newKey, newValue);
            }
        }
    }

    public String[] getProperties() {
        return properties;
    }

    public void setProperties(String[] properties) {
        this.properties = properties;
    }
}
