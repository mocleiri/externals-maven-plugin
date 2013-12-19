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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNCommitInfo;

public class SVNUtilsTest extends AbstractLocalSvnRepositoryTest {

	private static final String SVN_UTILS_TEST_REPO = "svn-utils-test-repo";
	private static final Logger log = LoggerFactory.getLogger(SVNUtilsTest.class);
	SVNUtils svnUtils = SVNUtils.getInstance();

	
	public SVNUtilsTest() {
		super(SVN_UTILS_TEST_REPO);
	}

	@Test
	public void testGetExternalsFromWorkingCopyPath2() {
		try {
			List<SVNExternal> externals = svnUtils.getExternals(workingCopy);
			svnUtils.showExternals(externals);
			
			Assert.assertEquals(2, externals.size());
			
		} catch (Exception e) {
			log.error("testGetExternalsFromWorkingCopyPath2 failed", e);
			Assert.fail("testGetExternalsFromWorkingCopyPath2 failed");
		}
	}

	@Test
	public void testDeleteExternals() {
		try {
			
			String repoPath = SubversionTestRepositoryUtils.getRepositoryPath(SVN_UTILS_TEST_REPO, "aggregate/trunk");
			
			List<SVNExternal> externals1 = svnUtils.getExternals(repoPath);
			log.info("externals 1 - " + externals1.size());
			Assert.assertEquals(2, externals1.size());
			
			SVNCommitInfo info = svnUtils.deleteExternals(repoPath);
			long newRevision = info.getNewRevision();
			log.info("Commited revision: " + newRevision);
			
			Assert.assertEquals(3, newRevision);
			
			List<SVNExternal> externals2 = svnUtils.getExternals(repoPath);
			log.info("externals 2 - " + externals2.size());
			Assert.assertEquals(0, externals2.size());
			
		} catch (Exception e) {
			log.error("testDeleteExternals failed", e);
			Assert.fail("testDeleteExternals failed");
		}
	}

	@Test
	public void testServerSideCopy() {
		try {
			long revision = 2;
			
			String srcPath = SubversionTestRepositoryUtils.getRepositoryPath(SVN_UTILS_TEST_REPO, "module1/trunk");
			
			String dstPath = SubversionTestRepositoryUtils.getRepositoryPath(SVN_UTILS_TEST_REPO, "module1/tags/copy-of-trunk");
			
			Copy copy = new Copy();
			copy.setSource(srcPath);
			copy.setRevision(revision);
			copy.setDestination(dstPath);

			SVNCommitInfo info = svnUtils.copy(copy);
			long newRevision = info.getNewRevision();
			log.info("Commited revision: " + newRevision);
			
			SubversionTestRepositoryUtils.checkOut(SVN_UTILS_TEST_REPO, "module1/tags/copy-of-trunk", getNewWorkingCopyName(), null, null);
			
		} catch (Exception e) {
			log.error("testServerSideCopy failed", e);
			Assert.fail("testServerSideCopy failed");
		}
	}
	
	@Test
	public void testGetExternalsFromUrl() {
		try {
			String url = SubversionTestRepositoryUtils.getRepositoryPath(SVN_UTILS_TEST_REPO, "aggregate/trunk");
			List<SVNExternal> externals = svnUtils.getExternals(url);
			svnUtils.showExternals(externals);
		} catch (Exception e) {
			log.error("testGetExternalsFromUrl failed", e);
			Assert.fail("testGetExternalsFromUrl failed");
		}
	}

	@Test
	public void testGetExternalsFromWorkingCopyPath() {
		try {
			List<SVNExternal> externals = svnUtils.getExternals(workingCopy);
			svnUtils.showExternals(externals);
		} catch (Exception e) {
			log.error("testGetExternalsFromWorkingCopyPath failed", e);
			Assert.fail("testGetExternalsFromWorkingCopyPath failed");
		}
	}

	@Test
	public void testGetUrl() {
		try {
			String url = svnUtils.getUrl(workingCopy);
			log.info("url=" + url);
		} catch (Exception e) {
			log.error("testGetUrl failed", e);
			Assert.fail("testGetUrl failed");
		}
	}

	@Test
	public void testGetLastRevision() {
		try {
			String url = SubversionTestRepositoryUtils.getRepositoryPath(SVN_UTILS_TEST_REPO, "aggregate/trunk");
			log.info(url + " - Last revision: " + svnUtils.getLastRevision(url));
			log.info(workingCopy.getAbsolutePath() + " - Last revision: " + svnUtils.getLastRevision(workingCopy));
		} catch (Exception e) {
			log.error("testGetLastRevision failed", e);
			Assert.fail("testGetLastRevision failed");
		}
	}

}
