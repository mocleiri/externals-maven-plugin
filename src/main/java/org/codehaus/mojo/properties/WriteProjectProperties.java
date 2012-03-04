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
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
     * If true, the plugin will create a second properties file with a ".sorted" extension that contains the properties
     * sorted by name
     *
     * @parameter default-value="false" expression="${properties.writeSorted}"
     */
    private boolean writeSorted;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateOutputFile();
        Properties properties = new Properties();
        properties.putAll(project.getProperties());

        Properties systemProperties = System.getProperties();

        // allow system properties to over write key/value found in maven properties
        Enumeration<?> enumeration = systemProperties.keys();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String value = systemProperties.getProperty(key);
            if (properties.get(key) != null) {
                properties.put(key, value);
            }

        }

        getLog().info("Creating " + outputFile);
        writeProperties(properties, outputFile);
        if (writeSorted) {
            createSorted(outputFile, properties);
        }
    }

    protected void createSorted(File file, Properties properties) throws MojoExecutionException {
        List<String> names = new ArrayList<String>(properties.stringPropertyNames());
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            String value = properties.getProperty(name);
            sb.append(name + "=" + value + "\n");
        }
        try {
            String filename = outputFile.getAbsolutePath() + ".sorted";
            getLog().info("Creating " + filename);
            FileUtils.writeByteArrayToFile(new File(filename), sb.toString().getBytes());
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating sorted file", e);
        }
    }

    public boolean isWriteSorted() {
        return writeSorted;
    }

    public void setWriteSorted(boolean writeSorted) {
        this.writeSorted = writeSorted;
    }
}
