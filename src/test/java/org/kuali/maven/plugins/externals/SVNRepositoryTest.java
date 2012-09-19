package org.kuali.maven.plugins.externals;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SVNRepositoryTest {

	private static final Logger log = LoggerFactory.getLogger(SVNRepositoryTest.class);
	SVNUtils svnUtils = SVNUtils.getInstance();
	String username = "jcaddel";
	String password = System.getProperty("svn.password");

	@Test
	public void testGetExternals() {
		try {
			File workingCopyPath = new File("/Users/jeffcaddel/ws/aggregate");
			svnUtils.getExternals(workingCopyPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetLastRevision() {
		try {
			String url = "https://svn.kuali.org/repos/student/enrollment/aggregate/trunk";
			File workingCopyPath = new File(".");
			log.info(url + " - Last revision: " + svnUtils.getLastRevision(url));
			log.info(workingCopyPath.getAbsolutePath() + " - Last revision: " + svnUtils.getLastRevision(workingCopyPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
