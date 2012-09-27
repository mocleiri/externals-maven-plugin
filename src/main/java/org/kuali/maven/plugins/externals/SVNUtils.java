package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
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
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SVNUtils.class);
	private static final String EMPTY_STRING = "";
	private static final String EXTERNALS_PROPERTY_NAME = "svn:externals";
	private static final String EXTERNALS_COMMENT = "#";
	private static final String DELETE_EXTERNALS_COMMIT_MESSAGE = "Delete externals";
	private static final String CREATE_EXTERNALS_COMMIT_MESSAGE = "Create externals";
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

	public void showExternals(List<SVNExternal> externals) {
		for (SVNExternal e : externals) {
			StringBuilder sb = new StringBuilder();
			sb.append("[" + e.getPath());
			sb.append(", " + e.getWorkingCopyPath());
			sb.append(", " + e.getUrl());
			sb.append("]");
			LOGGER.info(sb.toString());
		}
	}

	/**
	 * Copy <code>src</code> to <code>dst</code> creating parent directories as needed. An exception is thrown if <code>dst</code> already exists.
	 */
	public SVNCommitInfo copy(String src, String dst) {
		return copy(src, null, dst, null);
	}

	/**
	 * Copy <code>src</code> to <code>dst</code> creating parent directories as needed. An exception is thrown if <code>dst</code> already exists.
	 */
	public SVNCommitInfo copy(String src, String dst, String msg) {
		return copy(src, null, dst, msg);
	}

	/**
	 * Copy <code>src</code> at the indicated revision to <code>dst</code> creating parent directories as needed. An exception is thrown if <code>dst</code> already exists.
	 */
	public SVNCommitInfo copy(String src, Long revision, String dst) {
		return copy(src, revision, dst, null);
	}

	/**
	 * Copy <code>src</code> at the indicated revision to <code>dst</code> creating parent directories as needed. An exception is thrown if <code>dst</code> already exists.
	 */
	public SVNCommitInfo copy(String src, Long revision, String dst, String msg) {
		Copy copy = new Copy();
		copy.setSource(src);
		copy.setRevision(revision);
		copy.setDestination(dst);
		copy.setMessage(msg);
		return copy(copy);
	}

	public SVNCommitInfo setExternals(String url, List<SVNExternal> externals) {
		return setExternals(url, externals, null);
	}

	public SVNCommitInfo setExternals(String url, List<SVNExternal> externals, String message) {
		return setExternals(url, externals, message, null, null);
	}

	public SVNCommitInfo setExternals(String url, List<SVNExternal> externals, String message, String username, String password) {
		SVNClientManager manager = SVNClientManager.newInstance(null, username, password);
		SVNWCClient client = manager.getWCClient();
		String commitMessage = StringUtils.isBlank(message) ? CREATE_EXTERNALS_COMMIT_MESSAGE : message;
		SVNURL svnUrl = getSvnUrl(url);
		StringBuilder sb = new StringBuilder();
		for (SVNExternal external : externals) {
			sb.append(external.getPath() + " " + external.getUrl() + "\n");
		}
		SVNPropertyValue value = SVNPropertyValue.create(sb.toString());
		try {
			return client.doSetProperty(svnUrl, EXTERNALS_PROPERTY_NAME, value, SVNRevision.HEAD, commitMessage, null, true, null);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	public SVNCommitInfo deleteExternals(String url) {
		return deleteExternals(url, null);
	}

	public SVNCommitInfo deleteExternals(String url, String message) {
		return deleteExternals(url, message, null, null);
	}

	public SVNCommitInfo deleteExternals(String url, String message, String username, String password) {
		SVNClientManager manager = SVNClientManager.newInstance(null, username, password);
		SVNWCClient client = manager.getWCClient();
		String commitMessage = StringUtils.isBlank(message) ? DELETE_EXTERNALS_COMMIT_MESSAGE : message;
		SVNURL svnUrl = getSvnUrl(url);
		try {
			return client.doSetProperty(svnUrl, EXTERNALS_PROPERTY_NAME, null, SVNRevision.HEAD, commitMessage, null, true, null);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	public SVNCommitInfo copy(Copy copy) {
		SVNClientManager manager = SVNClientManager.newInstance(null, copy.getUsername(), copy.getPassword());
		SVNCopyClient client = manager.getCopyClient();
		SVNURL dstUrl = getSvnUrl(copy.getDestination());
		SVNURL srcUrl = getSvnUrl(copy.getSource());
		SVNRevision revision = SVNRevision.HEAD;
		if (copy.getRevision() != null) {
			revision = SVNRevision.create(copy.getRevision());
		}
		String msg = copy.getMessage();
		if (StringUtils.isBlank(msg)) {
			String r = (copy.getRevision() != null) ? "@" + revision : "";
			msg = "Copy " + copy.getSource() + r + " to " + copy.getDestination();
		}
		SVNCopySource svnCopySource = new SVNCopySource(SVNRevision.HEAD, revision, srcUrl);
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

	public long checkout(String url, File dstPath, String username, String password) {
		try {
			SVNClientManager manager = SVNClientManager.newInstance(null, username, password);
			SVNUpdateClient client = manager.getUpdateClient();
			client.setIgnoreExternals(false);
			SVNURL svnUrl = getSvnUrl(url);
			return client.doCheckout(svnUrl, dstPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
		} catch (SVNException e) {
			throw new IllegalStateException(e);
		}
	}

	public void commit(File workingCopyPath, String message, String username, String password) {
		try {
			SVNClientManager manager = SVNClientManager.newInstance(null, username, password);
			SVNCommitClient client = manager.getCommitClient();
			client.doCommit(new File[] { workingCopyPath }, true, message, null, null, true, true, SVNDepth.INFINITY);
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
