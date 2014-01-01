/**
 * Copyright 2011-2014 The Kuali Foundation
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.PathUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.kuali.common.util.Version;
import org.kuali.common.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

public class MojoHelperTest {
	private static final Logger logger = LoggerFactory.getLogger(SVNUtilsTest.class);
	SVNUtils svnUtils = SVNUtils.getInstance();

	MojoHelper helper = MojoHelper.getInstance();
	private static final String POM = "pom.xml";
	private static final String IGNORE = "src,target,.svn,.git";
	private static final File BASEDIR = new File(System.getProperty("user.dir"), "target"+File.separator+"test-aggregate");

	@Test
	public void testIsKnownQualifier() {
		/*
		 * This used to fail but the new multi-part qualifier support doesn't care about ordering.
		 * 
		 * It detects the M6 part and will bump that up by 1.
		 */
		String version = "2.0.0-M6-CM20-SNAPSHOT";
		String nextVersion = "";
		
		boolean exception = false;
		try {
			nextVersion = helper.getNextVersion(version);
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		
		Assert.assertEquals (true, exception);
		
		logger.info(nextVersion);
		Version v = VersionUtils.getVersion(version);
		String qualifier = v.getQualifier();
		logger.info(qualifier);
		Assert.assertFalse(helper.isKnownSubQualifier(qualifier));
	}
	
	private void assertVersionChange (String baseVersion, String expectedNextVersion, boolean assertEquals) {
		assertVersionChange(baseVersion, expectedNextVersion, assertEquals, false);
	}
	
	private void assertVersionChange (String baseVersion, String expectedNextVersion, boolean assertEquals, boolean throwException) {
			
		try {
			String nextVersion = helper.getNextVersion(baseVersion);
			
			if (assertEquals)
				Assert.assertEquals(expectedNextVersion, nextVersion);
			else
				Assert.assertNotEquals(expectedNextVersion, nextVersion);
		} catch (IllegalArgumentException e) {
			
			if (throwException)
				throw e;
			
			if (assertEquals) {
				Assert.fail (e.getMessage());
			}
		}
	}
	
	@Test
	public void testENRFoundersReleaseVersionScheme () {
		/*
		 * Verifies that the ENR founders release module naming scheme will work properly. 
		 */
		
		assertVersionChange("2.0.0-M9-SNAPSHOT", "2.0.0-M10-SNAPSHOT", false);

		assertVersionChange("2.0.0-M6-CM20-SNAPSHOT", "2.0.0-M7-CM20-SNAPSHOT", false);
		
		boolean failed = false;
		try {
			assertVersionChange("2.0.0-FR1-RC-SNAPSHOT", "2.0.0-FR1-RC1-SNAPSHOT", true, true);
		} catch (IllegalArgumentException e) {
			failed = true;
		}
		
		Assert.assertTrue("Should fail if the qualifier is missing a number", failed);
		
		assertVersionChange("2.1.0-FR2-M10-SNAPSHOT", "2.1.0-FR2-M11-SNAPSHOT", true);
		
		assertVersionChange("2.0.0-FR1-SNAPSHOT", "2.0.0-FR2-SNAPSHOT", true);
		
		assertVersionChange("2.0.0-M9-FR1-SNAPSHOT", "2.0.0-M10-FR1-SNAPSHOT", false);
	}

	@Test
	public void testKSAPVersionScheme () {
		
		assertVersionChange("2.0.0-M8-KSAP-SNAPSHOT", "2.0.0-M9-KSAP-SNAPSHOT", false);
		
		assertVersionChange("2.0.0-KSAP-M8-SNAPSHOT", "2.0.0-KSAP-M9-SNAPSHOT", false);
		
	}
	
	@Test
	public void testOtherVersions () {
		
		assertVersionChange("2.0.0-SNAPSHOT", "2.0.1-SNAPSHOT", true);
		
	}
	
	
	@Test
	public void testOnTemporaryRepository () {
		
		try {
			SubversionTestRepositoryUtils.deleteRepository("test-repository");
			SubversionTestRepositoryUtils.deleteRepositoryWorkingCopy("test-repository");
		} catch (IOException e1) {
			// fall through
		}
		SVNURL svnUrl = null;
		try {
			
			svnUrl = SubversionTestRepositoryUtils.createRepository("test-repository");
		} catch (SVNException e) {
			
			Assert.fail("failed to create test-repository");
		}
		
		try {
			SubversionTestRepositoryUtils.createExternalsBaseStructure("test-repository");
		} catch (IOException e) {
			Assert.fail("failed to create the base structure in test-repository.");
		}
		
		File workingCopy = SubversionTestRepositoryUtils.checkOut("test-repository", "aggregate/trunk", null, null);
		
		logger.info ("workingCopy = " + workingCopy.getAbsolutePath());
		
		// check that the modules materialized properly
		
		File module1 = new File (workingCopy, "module1");
		
		Assert.assertEquals (true, module1.exists());
		
		File module2 = new File (workingCopy, "module2");
		
		Assert.assertEquals (true, module2.exists());
		
		
	}
	
	@Test
	public void testIsReleaseCandidate() {
		String s1 = "rc1";
		String s2 = "Rc1";
		String s3 = "RC1";
		String s4 = "rC2";

		Assert.assertTrue(helper.isKnownSubQualifier(s1));
		Assert.assertTrue(helper.isKnownSubQualifier(s2));
		Assert.assertTrue(helper.isKnownSubQualifier(s3));
		Assert.assertTrue(helper.isKnownSubQualifier(s4));

	}

	@Test
	public void testValidate() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			List<DefaultMutableTreeNode> nodes = helper.getNodes(poms);
			DefaultMutableTreeNode tree = helper.getTree(BASEDIR, nodes, POM);
			helper.fillInGavs(tree);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testUpdateVersions() {
		int buildNumber = 100;
		GAV gav = new GAV("org.kuali.student", "student", "2.0.0-SNAPSHOT");
		List<Mapping> mappings = new ArrayList<Mapping>();
		mappings.add(new Mapping("ks-api", "ks.api.version"));
		mappings.add(new Mapping("ks-core", "ks.core.version"));
		mappings.add(new Mapping("ks-enroll", "ks.enroll.version"));
		mappings.add(new Mapping("ks-lum", "ks.lum.version"));
		mappings.add(new Mapping("ks-deployments", "ks.deployments.version"));

		Properties properties = new Properties();
		properties.setProperty("ks.api.version", "2.0.0-M5-SNAPSHOT");
		properties.setProperty("ks.core.version", "2.0.0-SNAPSHOT");
		properties.setProperty("ks.enroll.version", "1.0.0-SNAPSHOT");
		properties.setProperty("ks.lum.version", "2.0.0-SNAPSHOT");
		properties.setProperty("ks.deployments.version", "2.0.0-SNAPSHOT");

		List<File> files = helper.getPoms(BASEDIR, POM, IGNORE);
		List<DefaultMutableTreeNode> nodes = helper.getNodes(files);
		DefaultMutableTreeNode node = helper.getTree(BASEDIR, nodes, POM);
		List<SVNExternal> externals = svnUtils.getExternals(BASEDIR);
		BuildTag rootTag = helper.getBuildTag(BASEDIR, gav, TagStyle.BUILDNUMBER, buildNumber);
		helper.updateBuildInfo(node, rootTag, TagStyle.BUILDNUMBER, buildNumber);
		List<BuildTag> moduleTags = helper.getBuildTags(properties, externals, mappings, TagStyle.REVISION, buildNumber);
		helper.updateBuildInfo(nodes, moduleTags, mappings, TagStyle.REVISION, buildNumber);
		helper.updateGavs(node);
		helper.updateProperties(node, properties, mappings);
		logger.info("\n" + helper.getDisplayString(node));
	}

	// @Test
	public void testGetTree() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			List<DefaultMutableTreeNode> nodes = helper.getNodes(poms);
			DefaultMutableTreeNode tree = helper.getTree(BASEDIR, nodes, POM);
			String s = helper.getDisplayString(tree);
			logger.info("Maven Structure:\n" + s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testGetFiles() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			List<DefaultMutableTreeNode> nodes = helper.getNodes(poms);
			DefaultMutableTreeNode tree = helper.getTree(BASEDIR, nodes, POM);
			String s = helper.getDisplayString(tree, BASEDIR, POM);
			logger.info("Maven Structure:\n" + s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
