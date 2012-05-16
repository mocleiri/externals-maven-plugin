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
import org.jasypt.util.text.BasicTextEncryptor;

/**
 * Generate encrypted values for the specified system or project properties.
 *
 * @goal encrypt
 */
public class EncryptPropertiesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The list of properties containing values to encrypt
     *
     * @parameter
     * @required
     */
    private String[] properties;

    /**
     * The encrypted value for the properties are stored under their original property key with this text appended to
     * the end.
     *
     * eg "database.password" is stored as "database.password.encrypted"
     *
     * @parameter expression="${properties.suffix}" default-value="encrypted"
     * @required
     */
    private String suffix;

    /**
     * If true, the plain text values being encrypted are displayed to the console.
     *
     * @parameter expression="${properties.show}" default-value="false"
     * @required
     */
    private boolean show;

    /**
     *
     * The password for encrypting property values. This same password can be used to to decrypt the encrypted values.
     *
     * @parameter expression="${properties.password}"
     * @required
     */
    private String password;

    @Override
    public void execute() throws MojoExecutionException {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(password);
        Properties props = project.getProperties();
        for (String key : properties) {
            String value = getProperty(key);
            if (StringUtils.isBlank(value)) {
                getLog().info("Skipping " + key);
                continue;
            }
            String newValue = encryptor.encrypt(value);
            String newKey = key + "." + suffix;
            props.setProperty(newKey, newValue);
            if (show) {
                getLog().info("Setting " + newKey + "=" + newValue + " - " + value);
            } else {
                getLog().info("Setting " + newKey + "=" + newValue);
            }
        }
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
