package org.kuali.maven.plugins.externals;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

public class SVNRepositoryTest {

	private static final String UTF8 = "UTF-8";
	private static final Logger log = LoggerFactory.getLogger(SVNRepositoryTest.class);

	@Test
	public void test1() {
		try {
			String url = "https://svn.kuali.org/repos/student/enrollment/aggregate/trunk";
			String username = "jcaddel";
			String password = System.getProperty("svn.password");
			SVNRepository repository = getRepository(url, username, password);
			log.info("Repository Root: " + repository.getRepositoryRoot(true));
			log.info("Repository UUID: " + repository.getRepositoryUUID(true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected SVNRepository getRepository(String url, String username, String password) throws SVNException {
		DAVRepositoryFactory.setup();
		SVNURL svnUrl = SVNURL.parseURIEncoded(url);
		ISVNAuthenticationManager authManager = getAuthenticationManager(username, password);
		SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
		repository.setAuthenticationManager(authManager);
		return repository;
	}

	protected String encodeUTF8(String s) {
		try {
			return URLEncoder.encode(s, UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(UTF8 + " encoding is not supported.", e);
		}
	}

	protected ISVNAuthenticationManager getAuthenticationManager(String username, String password) {
		SVNAuthentication auth = new SVNPasswordAuthentication("jcaddel", "gw570229", false, null, false);
		SVNAuthentication[] array = new SVNAuthentication[] { auth };
		return new BasicAuthenticationManager(array);
	}

}
