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
 * Parse version number properties into [major].[minor].[incremental].[qualifier]
 *
 * @goal parse-version-properties
 */
public class ParseVersionPropertiesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The list of properties containing version numbers to parse
     *
     * @parameter
     * @required
     */
    private String[] properties;

    @Override
    public void execute() throws MojoExecutionException {
        Properties props = project.getProperties();
        for (String key : properties) {
            String value = getProperty(key);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            Version version = parseVersion(value);
            setProjectProperty(key, "major", version.getMajor(), props);
            setProjectProperty(key, "minor", version.getMinor(), props);
            setProjectProperty(key, "incremental", version.getIncremental(), props);
            setProjectProperty(key, "qualifier", version.getQualifier(), props);
        }
    }

    protected void setProjectProperty(String key, String suffix, String value, Properties props) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        props.setProperty(key + suffix, value);
        getLog().info("Setting " + key + "." + suffix + "=" + value);
    }

    protected Version parseVersion(String s) {
        Version version = new Version();
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
        if (tokens.length > 3) {
            version.setQualifier(tokens[3]);
        }
        if (StringUtils.isBlank(version.getQualifier())) {
            int pos = s.indexOf("-");
            if (pos != -1 && s.length() > pos) {
                version.setQualifier(s.substring(pos + 1));
            }
        }
        return version;
    }

    protected String getProperty(String key) {
        String sys = System.getProperty(key);
        String proj = project.getProperties().getProperty(key);
        if (!StringUtils.isBlank(sys)) {
            return sys;
        } else {
            return proj;
        }

    }

    public String[] getProperties() {
        return properties;
    }

    public void setProperties(String[] properties) {
        this.properties = properties;
    }
}
