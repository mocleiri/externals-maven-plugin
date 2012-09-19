package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNUtils {

	private static final String EMPTY_STRING = "";
	private static final String EXTERNALS_PROPERTY_NAME = "svn:externals";

	protected static SVNUtils instance;

	protected SVNUtils() {
		super();
		initialize();
	}

	protected void initialize() {
		SVNRepositoryFactoryImpl.setup();
		DAVRepositoryFactory.setup();
		FSRepositoryFactory.setup();
	}

	public synchronized static SVNUtils getInstance() {
		if (instance == null) {
			instance = new SVNUtils();
		}
		return instance;
	}

	public List<SVNExternal> getExternals(File workingCopyPath) {
		try {
			SVNWCClient client = getSVNWCClient();
			SVNPropertyData data = client.doGetProperty(workingCopyPath, EXTERNALS_PROPERTY_NAME, SVNRevision.WORKING, SVNRevision.WORKING);
			if (data == null) {
				return null;
			}
			SVNPropertyValue value = data.getValue();
			String s = SVNPropertyValue.getPropertyAsString(value);
			String[] tokens = StringUtils.split(s, "\n");
			List<SVNExternal> externals = new ArrayList<SVNExternal>();
			for (String token : tokens) {
				String[] values = StringUtils.split(token, " ");
				String url = values[0];
				String dir = values[1];
				File file = new File(workingCopyPath.getAbsolutePath() + File.separator + dir);
				SVNExternal external = new SVNExternal();
				external.setUrl(url);
				external.setWorkingCopyPath(file);
				externals.add(external);
			}
			return externals;
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	public long getLastRevision(File workingCopyPath) {
		SVNInfo info = getInfo(workingCopyPath);
		return info.getCommittedRevision().getNumber();
	}

	protected SVNWCClient getSVNWCClient() {
		ISVNAuthenticationManager authMgr = SVNWCUtil.createDefaultAuthenticationManager();
		return new SVNWCClient(authMgr, null);
	}

	protected SVNInfo getInfo(File workingCopyPath) {
		try {
			SVNWCClient client = getSVNWCClient();
			SVNRevision revision = SVNRevision.create(-1);
			return client.doInfo(workingCopyPath, revision);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	public long getLastRevision(String url) {
		SVNRepository repository = getRepository(url);
		return getLatestRevision(repository);
	}

	protected long getLatestRevision(SVNRepository repository) {
		try {
			SVNDirEntry entry = repository.info(EMPTY_STRING, -1);
			return entry.getRevision();
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	protected SVNRepository getRepository(String url) {
		return getRepository(url, null, null);
	}

	protected SVNRepository getRepository(String url, String username, String password) {
		try {
			@SuppressWarnings("deprecation")
			SVNURL svnUrl = SVNURL.parseURIDecoded(url);
			SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
			if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
				ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
				repository.setAuthenticationManager(authManager);
			}
			return repository;
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}
}
