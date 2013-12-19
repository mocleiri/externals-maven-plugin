/**
 * Copyright 2013 The Kuali Foundation
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

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestRetreiveTopLevelPropertiesOnly {

	private static final Logger log = LoggerFactory.getLogger(TestRetreiveTopLevelPropertiesOnly.class);
	
	MojoHelper helper = MojoHelper.getInstance();
	POMUtils pomUtils = new POMUtils();
	
	public TestRetreiveTopLevelPropertiesOnly() {
	}

	@Test
	public void testGetProperties() throws IOException {
		
		URL resource = getClass().getClassLoader().getResource("pom3.xml");
		
		String pomXml = IOUtils.toString(resource.openStream());
		
		Document document = pomUtils.getDocument(pomXml);
		
		NodeList nodeList = document.getElementsByTagName("properties");
	
		int length = nodeList.getLength();
		
		Node propertiesNode = nodeList.item(0);

		NodeList propertiesNodeList = propertiesNode.getChildNodes();
		
		boolean exceptionFound = false;
		try {
			Node node = pomUtils.findNode(propertiesNodeList, "ks.api.version");
		} catch (IllegalArgumentException e) {
			exceptionFound = true;
		}
		
		Assert.assertTrue (exceptionFound);
		
		NodeList topLevelPropertiesNode = pomUtils.getTopLevelProperties(document);
		
		Node node = pomUtils.findNode(topLevelPropertiesNode, "ks.api.version");
		
		Assert.assertNotNull(node);
		
	}
}
