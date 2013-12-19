/**
 * Copyright 2011-2013 The Kuali Foundation
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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class XMLUtilsTest extends AbstractLocalSvnRepositoryTest {
	private static final Logger logger = LoggerFactory.getLogger(XMLUtilsTest.class);

	MojoHelper helper = MojoHelper.getInstance();
	POMUtils xmlUtils = new POMUtils();
	
	private static final String POM = "pom.xml";
	private static final String IGNORE = "src,target,.svn,.git";
	
	private static final String TEST_REPO_NAME = "xml-utils-test-repo";
	
	
	public XMLUtilsTest() {
		super(TEST_REPO_NAME);
	}

	@Test
	public void testModifyPoms() {
		try {
			List<File> poms = helper.getPoms(workingCopy, POM, IGNORE);
			List<DefaultMutableTreeNode> nodes = helper.getNodes(poms);
			DefaultMutableTreeNode node = helper.getTree(workingCopy, nodes, POM);
			helper.updateGavs(node);
		} catch (Exception e) {
			logger.error("testModifyPoms failed", e);
			
			Assert.fail("testModifyPoms failed");
		}
	}

	@Test
	public void testUpdateScm() {
		try {
			List<File> poms = helper.getPoms(workingCopy, POM, IGNORE);
			String rootXml = null;
			for (File pom : poms) {
				String xml = FileUtils.readFileToString(pom);
				int pos = xml.indexOf("<artifactId>kuali-common</artifactId>");
				if (pos == -1) {
					continue;
				} else {
					rootXml = xml;
					break;
				}
			}
			String newXml = xmlUtils.updateScm(rootXml, "scm:svn:", "foo");
			logger.info(newXml);
		} catch (Exception e) {
			logger.error("testUpdateScm failed", e);
			
			Assert.fail("testUpdateScm failed");
		}
	}

	@Test
	public void testUpdateVersion() {
		try {
			List<File> poms = helper.getPoms(workingCopy, POM, IGNORE);
			File pom = poms.get(0);
			String oldXml = FileUtils.readFileToString(pom);
			String newXml = xmlUtils.format(oldXml);
			String foo1Xml = xmlUtils.updateParentVersion(newXml, "foo1");
			logger.info(foo1Xml);
		} catch (Exception e) {
			logger.error("testUpdateVersion failed", e);
			
			Assert.fail("testUpdateVersionFailed");
		}
	}

	@Test
	public void testGetFiles() {
		try {
			List<File> poms = helper.getPoms(workingCopy, POM, IGNORE);
			File pom = poms.get(0);
			String oldXml = FileUtils.readFileToString(pom);
			String newXml = xmlUtils.format(oldXml);
			logger.info(newXml);
			GAV parent = xmlUtils.getParentGAV(newXml);
			GAV gav = xmlUtils.getGAV(newXml);
			logger.info(parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion());
			logger.info(gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion());
		} catch (Exception e) {
			logger.error("testGetFiles failed", e);
			Assert.fail("testGetFiles failed");
		}
	}
}
