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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jasypt.util.text.BasicTextEncryptor;

/**
 * @goal decryptall
 */
public class DecryptAllPropertiesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The pattern for matching properties in need of decryption
     *
     * @parameter expression="${properties.endsWith}" default-value=".encrypted"
     * @required
     */
    private String endsWith;

    /**
     * @parameter expression="${properties.show}" default-value="false"
     * @required
     */
    private boolean show;

    /**
     * @parameter expression="${properties.password}"
     * @required
     */
    private String password;

    @Override
    public void execute() throws MojoExecutionException {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(password);
        Properties props = project.getProperties();
        List<String> keys = new ArrayList<String>(props.stringPropertyNames());
        Collections.sort(keys);
        for (String key : keys) {
            boolean decrypt = key.endsWith(endsWith);
            if (!decrypt) {
                continue;
            }
            String value = getProperty(key);
            if (StringUtils.isBlank(value)) {
                getLog().info("Skipping blank property " + key);
                continue;
            }
            String newValue = encryptor.decrypt(value);
            int length = endsWith.length();
            String newKey = key.substring(0, key.length() - length);
            props.setProperty(newKey, newValue);
            if (show) {
                getLog().info("Setting " + newKey);
                getLog().info("Encrypted: " + value);
                getLog().info("Plain Text: " + newValue);
            } else {
                getLog().info("Setting " + newKey);
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

}
