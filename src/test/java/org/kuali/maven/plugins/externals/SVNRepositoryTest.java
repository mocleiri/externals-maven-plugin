package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNCommitInfo;

public class SVNRepositoryTest {

	private static final Logger log = LoggerFactory.getLogger(SVNRepositoryTest.class);
	SVNUtils svnUtils = SVNUtils.getInstance();
	String username = "jcaddel";
	String password = System.getProperty("svn.password");

	@Test
	public void testServerSideCopy() {
		try {
			long revision = 36416;
			String src = "https://svn.kuali.org/repos/student/sandbox/enrollment/ks-api/branches/M5";
			String dst = "https://svn.kuali.org/repos/student/sandbox/enrollment/ks-api/tags/builds/2.0/2.0.0-M5/20120919-r" + revision;
			Copy copy = new Copy();
			copy.setSource(src);
			copy.setRevision(revision);
			copy.setDestination(dst);
			copy.setUsername(username);
			copy.setPassword(password);

			SVNCommitInfo info = svnUtils.copy(copy);
			long newRevision = info.getNewRevision();
			log.info("Commited revision: " + newRevision);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testGetExternalsFromUrl() {
		try {
			String url = "https://svn.kuali.org/repos/student/sandbox/enrollment/aggregate/trunk";
			List<SVNExternal> externals = svnUtils.getExternals(url);
			svnUtils.showExternals(externals);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testGetExternalsFromWorkingCopyPath() {
		try {
			File workingCopyPath = new File("/Users/jeffcaddel/ws/aggregate");
			List<SVNExternal> externals = svnUtils.getExternals(workingCopyPath);
			svnUtils.showExternals(externals);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testGetUrl() {
		try {
			File workingCopyPath = new File("/Users/jeffcaddel/ws/aggregate");
			String url = svnUtils.getUrl(workingCopyPath);
			log.info("url=" + url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testGetLastRevision() {
		try {
			String url = "https://svn.kuali.org/repos/student/sandbox/enrollment/aggregate/trunk";
			File workingCopyPath = new File(".");
			log.info(url + " - Last revision: " + svnUtils.getLastRevision(url));
			log.info(workingCopyPath.getAbsolutePath() + " - Last revision: " + svnUtils.getLastRevision(workingCopyPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
