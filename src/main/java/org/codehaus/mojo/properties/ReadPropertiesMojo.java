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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * The read-project-properties goal reads property files and stores the properties as project properties. It serves as
 * an alternate to specifying properties in pom.xml.
 *
 * @author <a href="mailto:zarars@gmail.com">Zarar Siddiqi</a>
 * @author <a href="mailto:Krystian.Nowak@gmail.com">Krystian Nowak</a>
 * @version $Id: ReadPropertiesMojo.java 8861 2009-01-21 15:35:38Z pgier $
 * @goal read-project-properties
 */
public class ReadPropertiesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The properties files that will be used when reading properties. Can use both .properties and .xml files
     *
     * @parameter
     * @required
     */
    private File[] files;

    /**
     * If true, the plugin will silently ignore any non-existent properties files, and the build will continue
     *
     * @parameter expression="${properties.quiet}" default-value="false"
     */
    private boolean quiet;

    /**
     * Comma separated list of property values to ignore
     *
     * @parameter expression="${properties.ignore}"
     */
    private String ignore;

    @Override
    public void execute() throws MojoExecutionException {
        List<String> ignoreList = getListFromCSV(ignore);
        Properties projectProperties = project.getProperties();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (validate(file)) {
                getLog().info("Loading " + file);
                if (!StringUtils.isBlank(ignore)) {
                    getLog().info("Ignoring " + ignore);
                }
                Properties p = getProperties(file);
                updateProperties(projectProperties, p, ignoreList);
            }
        }

        Properties env = getEnvironment();
        for (String name : projectProperties.stringPropertyNames()) {
            String value = getPropertyValue(name, projectProperties, env);
            projectProperties.setProperty(name, value);
        }
    }

    protected Properties getEnvironment() throws MojoExecutionException {
        try {
            return CommandLineUtils.getSystemEnvVars();
        } catch (IOException e) {
            throw new MojoExecutionException("Error get environment variables", e);
        }
    }

    protected void updateProperties(Properties p1, Properties p2, List<String> ignore) {
        Set<String> names = p2.stringPropertyNames();
        for (String name : names) {
            if (!ignore.contains(name)) {
                String value = p2.getProperty(name);
                p1.setProperty(name, value);
            }
        }
    }

    protected static final List<String> getListFromCSV(String csv) {
        if (StringUtils.isBlank(csv)) {
            return new ArrayList<String>();
        }
        List<String> list = new ArrayList<String>();
        String[] tokens = StringUtils.split(csv, ",");
        for (String token : tokens) {
            list.add(token.trim());
        }
        return list;
    }

    /**
     * Retrieves a property value, replacing values like ${token} using the Properties to look them up. Shamelessly
     * adapted from:
     * http://maven.apache.org/plugins/maven-war-plugin/xref/org/apache/maven/plugin/war/PropertyUtils.html
     *
     * It will leave unresolved properties alone, trying for System properties, and environment variables and implements
     * reparsing (in the case that the value of a property contains a key), and will not loop endlessly on a pair like
     * test = ${test}
     *
     * @param k
     *            property key
     * @param p
     *            project properties
     * @param environment
     *            environment variables
     * @return resolved property value
     */
    protected String getPropertyValue(String k, Properties p, Properties environment) {
        String v = p.getProperty(k);
        String ret = "";
        int idx, idx2;

        while ((idx = v.indexOf("${")) >= 0) {
            // append prefix to result
            ret += v.substring(0, idx);

            // strip prefix from original
            v = v.substring(idx + 2);

            idx2 = v.indexOf("}");

            // if no matching } then bail
            if (idx2 < 0) {
                break;
            }

            // strip out the key and resolve it
            // resolve the key/value for the ${statement}
            String nk = v.substring(0, idx2);
            v = v.substring(idx2 + 1);
            String nv = p.getProperty(nk);

            // try global environment
            if (nv == null) {
                nv = System.getProperty(nk);
            }

            // try environment variable
            if (nv == null && nk.startsWith("env.") && environment != null) {
                nv = environment.getProperty(nk.substring(4));
            }

            // if the key cannot be resolved,
            // leave it alone ( and don't parse again )
            // else prefix the original string with the
            // resolved property ( so it can be parsed further )
            // taking recursion into account.
            if (nv == null || nv.equals(nk)) {
                ret += "${" + nk + "}";
            } else {
                v = nv + v;
            }
        }
        return ret + v;
    }

    protected boolean validate(File file) throws MojoExecutionException {
        boolean exists = file != null && file.exists();
        if (exists) {
            return true;
        }
        if (quiet) {
            getLog().info("Ignoring non-existent properties file '" + file + "'");
            return false;
        } else {
            throw new MojoExecutionException("Non-existent properties file '" + file + "'");
        }
    }

    protected Properties getProperties(File file) throws MojoExecutionException {
        InputStream in = null;
        try {
            Properties properties = new Properties();
            in = new FileInputStream(file);
            String filename = file.getName().toLowerCase();
            if (filename.endsWith(".xml")) {
                properties.loadFromXML(in);
            } else {
                properties.load(in);
            }
            return properties;
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading properties file " + file.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public File[] getFiles() {
        return files;
    }

    public void setFiles(File[] files) {
        this.files = files;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public String getIgnore() {
        return ignore;
    }

    public void setIgnore(String ignoreProperties) {
        this.ignore = ignoreProperties;
    }

    public MavenProject getProject() {
        return project;
    }

}
