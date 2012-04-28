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

import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Sets system properties.
 *
 * @author <a href="mailto:markh@apache.org">Mark Hobson</a>
 * @version $Id: SetSystemPropertiesMojo.java 9751 2009-05-20 14:53:00Z mark $
 * @goal set-system-properties
 * @phase initialize
 */
public class SetSystemPropertiesMojo extends AbstractMojo {
    // fields -----------------------------------------------------------------

    /**
     * The system properties to set.
     *
     * @parameter
     * @required
     */
    private Properties properties;

    // Mojo methods -----------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (properties.isEmpty()) {
            getLog().debug("No system properties found");

            return;
        }

        getLog().debug("Setting system properties:");

        for (Enumeration<?> propertyNames = properties.propertyNames(); propertyNames.hasMoreElements();) {
            String propertyName = propertyNames.nextElement().toString();
            String propertyValue = properties.getProperty(propertyName);

            getLog().debug("- " + propertyName + " = " + propertyValue);

            System.setProperty(propertyName, propertyValue);
        }

        int count = properties.size();

        getLog().info("Set " + count + " system " + (count > 1 ? "properties" : "property"));
    }
}
