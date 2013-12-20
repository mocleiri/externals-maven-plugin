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
/**
 * 
 */
package org.kuali.maven.plugins.externals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

/**
 * @author ocleirig
 *
 */
public abstract class AbstractLocalSvnRepositoryTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractLocalSvnRepositoryTest.class);
	
	protected final String testRespositoryName;
	protected File workingCopy = null;

	protected SVNURL svnUrl;

	private AtomicInteger workingCopyCounter = new AtomicInteger();
	
	private Set<String>additionalWorkingCopyNames = new HashSet<String>();

	/**
	 * 
	 */
	public AbstractLocalSvnRepositoryTest(String testRespositoryName) {
		this.testRespositoryName = testRespositoryName;
	}
	
	
	/**
	 * Create the local svn repository and populate it with the base configuration.
	 * 
	 * then checkout a working copy pointed at the aggregate/trunk which has svn:externals to two modules.
	 * 
	 * @throws IOException
	 * @throws SVNException
	 */
	@Before
	public void resetTestRespository() throws IOException, SVNException {
		
		SubversionTestRepositoryUtils.deleteRepository(testRespositoryName);
			
		for (String name : additionalWorkingCopyNames) {
			SubversionTestRepositoryUtils.deleteRepositoryWorkingCopy(testRespositoryName, name);
		}
		
		SubversionTestRepositoryUtils.deleteRepositoryWorkingCopy(testRespositoryName);
			
		svnUrl = SubversionTestRepositoryUtils.createRepository(testRespositoryName);
		
		SubversionTestRepositoryUtils.createExternalsBaseStructure(testRespositoryName);
		
		workingCopy = SubversionTestRepositoryUtils.checkOut(testRespositoryName, "aggregate/trunk", null, null);
		
		logger.info ("workingCopy = " + workingCopy.getAbsolutePath());
		
		
	}
	
	protected String getNewWorkingCopyName() {
		
		StringBuilder workingCopyName = new StringBuilder(testRespositoryName);
		
		workingCopyName.append("-wc-");
		workingCopyName.append(workingCopyCounter.incrementAndGet());
		
		String name = workingCopyName.toString();
		
		additionalWorkingCopyNames.add(name);
		
		return name;
	}


}
