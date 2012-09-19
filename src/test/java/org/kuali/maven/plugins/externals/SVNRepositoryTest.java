package org.kuali.maven.plugins.externals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNRepositoryTest {

	private static final Logger log = LoggerFactory.getLogger(SVNRepositoryTest.class);

	@Test
	public void test1() {
		try {
			String url = "https://svn.kuali.org/repos/student/enrollment/aggregate/trunk";
			String username = "jcaddel";
			String password = System.getProperty("svn.password");
			SVNRepository repo = getRepository(url, username, password);
			SVNDirEntry entry = repo.info("", -1);
			log.info("Repository Root: " + repo.getRepositoryRoot(true));
			log.info("Repository UUID: " + repo.getRepositoryUUID(true));
			log.info("Latest Repository Revision: " + repo.getLatestRevision());
			log.info("Latest Path revision: " + entry.getRevision());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected SVNRepository getRepository(String url, String username, String password) throws SVNException {
		DAVRepositoryFactory.setup();
		@SuppressWarnings("deprecation")
		SVNURL svnUrl = SVNURL.parseURIDecoded(url);
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
		SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
		repository.setAuthenticationManager(authManager);
		return repository;
	}

}
