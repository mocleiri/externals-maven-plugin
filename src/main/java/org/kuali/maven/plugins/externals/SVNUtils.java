package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.tmatesoft.svn.core.SVNCommitInfo;
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
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNUtils {

	private static final String EMPTY_STRING = "";
	private static final String EXTERNALS_PROPERTY_NAME = "svn:externals";
	private static final String EXTERNALS_COMMENT = "#";
	private static final String LINEFEED = "\n";
	private static final String SPACE = " ";

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

	/**
	 * Copy <code>src</code> to <code>dst</code> creating parent directories as needed. An exception is thrown if <code>dst</code> already exists.
	 */
	public SVNCommitInfo copy(String src, String dst, String msg, String username, String password) {
		SVNClientManager manager = SVNClientManager.newInstance(null, username, password);
		SVNCopyClient client = manager.getCopyClient();
		SVNURL dstUrl = getSvnUrl(dst);
		SVNURL srcUrl = getSvnUrl(src);
		SVNCopySource svnCopySource = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, srcUrl);
		SVNCopySource[] sources = new SVNCopySource[] { svnCopySource };
		try {
			return client.doCopy(sources, dstUrl, false, true, true, msg, null);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Return the Subversion url that corresponds with the local working copy
	 */
	public String getUrl(File workingCopyPath) {
		SVNInfo info = getInfo(workingCopyPath);
		return info.getURL().toDecodedString();
	}

	/**
	 * Return any svn:externals associated with the given url. Returns an empty list if there are none. Never returns null.
	 */
	public List<SVNExternal> getExternals(String url) {
		try {
			SVNWCClient client = getSVNWCClient();
			SVNURL svnUrl = getSvnUrl(url);
			SVNPropertyData data = client.doGetProperty(svnUrl, EXTERNALS_PROPERTY_NAME, SVNRevision.HEAD, SVNRevision.HEAD);
			return getExternals(data, null);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Return any svn:externals associated with the working copy. Returns an empty list if there are none. Never returns null.
	 */
	public List<SVNExternal> getExternals(File workingCopyPath) {
		try {
			SVNWCClient client = getSVNWCClient();
			SVNPropertyData data = client.doGetProperty(workingCopyPath, EXTERNALS_PROPERTY_NAME, SVNRevision.WORKING, SVNRevision.WORKING);
			return getExternals(data, workingCopyPath);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Return the revision of the last commit for the working copy.
	 */
	public long getLastRevision(File workingCopyPath) {
		SVNInfo info = getInfo(workingCopyPath);
		return info.getCommittedRevision().getNumber();
	}

	/**
	 * Return the revision of the last commit for the given url
	 */
	public long getLastRevision(String url) {
		SVNRepository repository = getRepository(url);
		return getLastRevision(repository);
	}

	protected List<SVNExternal> getExternals(SVNPropertyData data, File workingCopyPath) {
		if (data == null) {
			return new ArrayList<SVNExternal>();
		}
		SVNPropertyValue value = data.getValue();
		String s = SVNPropertyValue.getPropertyAsString(value);
		String[] tokens = StringUtils.split(s, LINEFEED);
		List<SVNExternal> externals = new ArrayList<SVNExternal>();
		for (String token : tokens) {
			token = token.trim();
			if (token.startsWith(EXTERNALS_COMMENT)) {
				continue;
			}
			if (StringUtils.isBlank(token)) {
				continue;
			}
			String[] values = StringUtils.split(token, SPACE);
			String path = values[0];
			String url = values[1];
			File externalsPath = getExternalWorkingCopyPath(workingCopyPath, path);
			SVNExternal external = new SVNExternal();
			external.setUrl(url);
			external.setPath(path);
			external.setWorkingCopyPath(externalsPath);
			externals.add(external);
		}
		return externals;
	}

	protected File getExternalWorkingCopyPath(File workingCopyPath, String path) {
		if (workingCopyPath == null) {
			return null;
		} else {
			return new File(workingCopyPath.getAbsolutePath() + File.separator + path);
		}
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

	protected long getLastRevision(SVNRepository repository) {
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
			SVNURL svnUrl = getSvnUrl(url);
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

	@SuppressWarnings("deprecation")
	protected SVNURL getSvnUrl(String url) {
		try {
			return SVNURL.parseURIDecoded(url);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}
}
