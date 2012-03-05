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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Writes project properties to a file.
 *
 * @author <a href="mailto:zarars@gmail.com">Zarar Siddiqi</a>
 * @version $Id: WriteProjectProperties.java 9747 2009-05-20 13:27:44Z mark $
 * @goal write-project-properties
 */
public class WriteProjectProperties extends AbstractWritePropertiesMojo {

    /**
     * If true, the plugin will create the properties file formatted the same way Ant formats properties files using the
     * &lt;echoproperties&gt; task. The properties will be sorted by name with the ':', '#' and '=' symbols escaped with
     * a backslash
     *
     * @parameter default-value="false" expression="${properties.antEchoPropertiesMode}"
     */
    private boolean antEchoPropertiesMode;

    /**
     * If true, the plugin will include system properties when writing the properties file
     *
     * @parameter default-value="false" expression="${properties.includeSystemProperties}"
     */
    private boolean includeSystemProperties;

    /**
     * Comma separated set of properties to omit from writing to the properties file
     *
     * @parameter expression="${properties.omit}"
     */
    private String omit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateOutputFile();
        Properties properties = new Properties();
        properties.putAll(project.getProperties());

        Properties systemProperties = System.getProperties();

        // Make sure system properties override Maven project properties
        for (String key : systemProperties.stringPropertyNames()) {
            String mavenValue = properties.getProperty(key);
            String systemValue = systemProperties.getProperty(key);
            // If we are including system properties, always put the system property
            if (includeSystemProperties) {
                properties.put(key, systemValue);
            } else if (mavenValue != null) {
                // Otherwise only update our properties object if the System property overrides a Maven project property
                properties.put(key, systemValue);
            }
        }

        remove(properties, omit);

        getLog().info("Creating " + outputFile);
        if (antEchoPropertiesMode) {
            echoPropertiesMode(outputFile, properties);
        } else {
            writeProperties(properties, outputFile);
        }
    }

    protected void remove(Properties properties, String csv) {
        List<String> keys = ReadPropertiesMojo.getListFromCSV(csv);
        for (String key : keys) {
            properties.remove(key);
        }

    }

    protected void echoPropertiesMode(File file, Properties properties) throws MojoExecutionException {
        // DSTAMP=20120304
        // TODAY=March 4 2012
        // TSTAMP=1651
        SimpleDateFormat dstamp = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat today = new SimpleDateFormat("MMMM d yyyy");
        SimpleDateFormat tstamp = new SimpleDateFormat("HHmm");
        List<String> names = new ArrayList<String>(properties.stringPropertyNames());
        Collections.sort(names);
        Date now = new Date();
        StringBuilder sb = new StringBuilder();
        sb.append("#Ant properties\n");
        sb.append("#" + now + "\n");
        sb.append("DSTAMP=" + dstamp.format(now) + "\n");
        sb.append("TODAY=" + today.format(now) + "\n");
        sb.append("TSTAMP=" + tstamp.format(now) + "\n");
        for (String name : names) {
            String value = properties.getProperty(name);
            value = value.replace("\n", "\\n");
            value = value.replace("\r", "\\r");
            value = value.replace("\t", "\\t");
            value = value.replace(":", "\\:");
            value = value.replace("#", "\\#");
            value = value.replace("=", "\\=");
            sb.append(name + "=" + value + "\n");
        }
        try {
            FileUtils.writeByteArrayToFile(file, sb.toString().getBytes());
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating properties file", e);
        }
    }

    public boolean isAntEchoPropertiesMode() {
        return antEchoPropertiesMode;
    }

    public void setAntEchoPropertiesMode(boolean antEchoPropertiesMode) {
        this.antEchoPropertiesMode = antEchoPropertiesMode;
    }

    public boolean isIncludeSystemProperties() {
        return includeSystemProperties;
    }

    public void setIncludeSystemProperties(boolean includeSystemProperties) {
        this.includeSystemProperties = includeSystemProperties;
    }
}
