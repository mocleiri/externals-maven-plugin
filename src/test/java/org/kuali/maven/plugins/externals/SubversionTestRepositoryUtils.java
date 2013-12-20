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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * 
 * Creates a structure of /target/repository-name/{repo, main-wc, named-wc} to 
 * allow more than one working copy to be created for a given namespace.
 * 
 * @author ocleirig
 *
 */
public class SubversionTestRepositoryUtils {

	private static final String MAIN_WC = "main-wc";

	private static final String REPO_NAME = "repo";
	
	private static final File BUILD_DIR = new File (System.getProperty("user.dir"), "target");
	
	
	/**
	 * 
	 */
	private SubversionTestRepositoryUtils() {
		
	}
	private static File getRepositoryFile (String repositoryName) {
		File repositoryFile = new File (new File( BUILD_DIR, repositoryName), REPO_NAME);
		
		repositoryFile.mkdirs();
		
		return repositoryFile;
	}
	/**
	 * Create a new repository of the given name
	 * 
	 * @param repositoryName
	 * @return
	 * @throws SVNException
	 */
	public static SVNURL createRepository (String repositoryName) throws SVNException {

	      return SVNRepositoryFactory.createLocalRepository(getRepositoryFile(repositoryName), true , true );
	}
	
	/**
	 * Delete the named repository from the temporary directory
	 * 
	 * @param repositoryName
	 * @throws IOException
	 */
	public static void deleteRepository(String repositoryName) throws IOException {
		FileUtils.deleteDirectory(getRepositoryFile(repositoryName));
	}
	
	public static void deleteRepositoryWorkingCopy(String repositoryName, String workingCopyName) throws IOException {
		FileUtils.deleteDirectory(getWorkingCopyFile(repositoryName, workingCopyName));
	}
	
	public static void deleteRepositoryWorkingCopy(String testRespositoryName) throws IOException {

		deleteRepositoryWorkingCopy(testRespositoryName, testRespositoryName + "-wc");
	}
	
	private static File getWorkingCopyFile(String repositoryName,
			String workingCopyName) {
		
		File workingCopyFile = new File (new File( BUILD_DIR, repositoryName), workingCopyName);
		
		return workingCopyFile;
	}
	private static boolean createModuleStructure (File workingCopy, String moduleName) {
		
		File module = new File (workingCopy, moduleName);
	
		if (!module.mkdir()) {
			return false;
		}
		
		File tags = new File (module, "tags");
		File branches = new File (module, "branches");
		File trunk = new File (module, "trunk");
		
		if (tags.mkdir() && branches.mkdir() && trunk.mkdir())
			return true;
		else
			return false;
		
	}
	public static void createExternalsBaseStructure (String repositoryName) throws IOException {
		
		// checkout at the root to create the initial structure
		File workingCopy = checkOut(repositoryName, null, MAIN_WC, null, null);
		
		// make an aggregate
		createModuleStructure(workingCopy, "aggregate");
		
		// make a module 1
		createModuleStructure(workingCopy, "module1");
		
		// make a module 2
		createModuleStructure(workingCopy, "module2");
		
		
		// create the pom
		createParentPomFile(repositoryName, new File (new File (workingCopy, "aggregate"), "trunk"), new String[] {"module1", "module2"}, "2.0.0-FR1-SNAPSHOT", "2.0.0-FR1-SNAPSHOT", "1.0.0-FR1-SNAPSHOT");
		
		// create the module 1 pom
		createModulePomFile (new File (new File (workingCopy, "module1"), "trunk"), "module1", "2.0.0-FR1-SNAPSHOT", "2.0.0-FR1-SNAPSHOT");
		
		// create the module 2 pom
		createModulePomFile (new File (new File (workingCopy, "module2"), "trunk"), "module2", "2.0.0-FR1-SNAPSHOT", "1.0.0-FR1-SNAPSHOT");
		
		SVNUtils.getInstance().addFiles(new File (workingCopy, "aggregate"), null, null);
		SVNUtils.getInstance().addFiles(new File (workingCopy, "module1"), null, null);
		SVNUtils.getInstance().addFiles(new File (workingCopy, "module2"), null, null);
		
		
		List<SVNExternal> externals = new ArrayList<SVNExternal>();
		
		externals.add(new SVNExternal("file://" + getRepositoryPath(repositoryName, "/module1/trunk"), "module1", null));
		externals.add(new SVNExternal("file://" + getRepositoryPath(repositoryName, "/module2/trunk"), "module2", null));
		
		SVNCommitInfo commitInfo = SVNUtils.getInstance().commit(workingCopy, "initial commit", null, null);
		
		commitInfo = SVNUtils.getInstance().setExternals(getRepositoryPath(repositoryName, "/aggregate/trunk"), externals);
		
		// clean up the working copy
		deleteRepositoryWorkingCopy(repositoryName, MAIN_WC);
		
		
	}

	private static void createModulePomFile(File targetPath, String moduleName, String parentPomVersion, String pomVersion) throws FileNotFoundException {

		PrintWriter pw = new PrintWriter(new File (targetPath, "pom.xml"));
		
		pw.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + 
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" + 
				"    <modelVersion>4.0.0</modelVersion>\r\n" + 
				"    <parent>\r\n" + 
				"    <groupId>org.kuali.maven.plugins</groupId>\r\n" + 
				"    <artifactId>externals-test</artifactId>\r\n" + 
				"        <version>"+parentPomVersion+"</version>\r\n" + 
				"    </parent>\r\n" + 
				"    <artifactId>" + moduleName + "</artifactId>\n" +
				"    <version>"+pomVersion+"</version>\r\n" + 
				"    <packaging>jar</packaging>");
	
		pw.println("</project>");
		
		pw.close();
	}

	private static void createParentPomFile(String repositoryName, File targetPath, String[]moduleNames, String pomVersion, String module1PomVersion, String module2PomVersion) throws FileNotFoundException {

		PrintWriter pw = new PrintWriter(new File (targetPath, "pom.xml"));
		
		pw.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + 
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" + 
				"    <modelVersion>4.0.0</modelVersion>\r\n" + 
				"    <parent>\r\n" + 
				"        <groupId>org.kuali.pom</groupId>\r\n" + 
				"        <artifactId>kuali-common</artifactId>\r\n" + 
				"        <version>3.3.19</version>\r\n" + 
				"    </parent>\r\n" + 
				"    <groupId>org.kuali.maven.plugins</groupId>\r\n" + 
				"    <artifactId>externals-test</artifactId>\r\n" + 
				"    <version>"+pomVersion+"</version>\r\n" + 
				"    <packaging>pom</packaging>");
		

		// write out the properties
		pw.println("\t<properties>");
		pw.println("\t\t<plugin.externals.version>");
		pw.print("\t\t\t");
		
		
		String pluginVersion = MojoHelper.class.getPackage().getImplementationVersion();
		
		if (pluginVersion == null) {
			// assume running tests locally
			pluginVersion = "1.0.11-SNAPSHOT";
		}
		pw.println(pluginVersion);
		pw.println("\t\t</plugin.externals.version>");
		
		// module1.version
		pw.println("\t\t<module1.version>");
		pw.print("\t\t\t");
		pw.println(module1PomVersion);
		pw.println("\t\t</module1.version>");
		
		// module2.version
		pw.println("\t\t<module2.version>");
		pw.print("\t\t\t");
		pw.println(module2PomVersion);
		pw.println("\t\t</module2.version>");
		
		pw.println("\t</properties>");
		
		// scm section
		
		File repository = new File (BUILD_DIR, repositoryName);
		
		String scmString = "scm:svn:file://localhost/" + repository.getAbsolutePath();
		
		pw.println("\t<scm>");

		pw.print("\t\t<connection>");
		pw.print(scmString);
		pw.println("</connection>");
		
		pw.print("\t\t<developerConnection>");
		pw.print(scmString);
		pw.println("</developerConnection>");
		
		pw.print("\t\t<url>");
		pw.print(scmString);
		pw.println("</url>");
        
        pw.println("\t</scm>");
        
		// write out the linkage to this version of the externals maven plugin
		// build -> plugin management -> plugins
		
		pw.println("\t<build>\n\t\t<pluginManagement>\n\t\t\t<plugins>");
		
		pw.println("<plugin>\r\n" + 
				"                    <groupId>org.kuali.maven.plugins</groupId>\r\n" + 
				"                    <artifactId>externals-maven-plugin</artifactId>\r\n" + 
				"                    <configuration>\r\n" + 
				"                        <commitMessage>[externals-maven-plugin] Automated pom formatting</commitMessage>\r\n" + 
				"                        <mappings>\r\n" + 
				"                            <mapping>\r\n" + 
				"                                <module>module1</module>\r\n" + 
				"                                <versionProperty>module1.version</versionProperty>\r\n" + 
				"                            </mapping>\r\n" + 
				"                            <mapping>\r\n" + 
				"                                <module>module2</module>\r\n" + 
				"                                <versionProperty>module2.version</versionProperty>\r\n" + 
				"                            </mapping>\r\n" + 
				"                        </mappings>\r\n" + 
				"                    </configuration>\r\n" + 
				"                </plugin>");
		
		pw.println("\t\t\t</plugins>\n\t\t</pluginManagement>\n\t</build>");
		
		// write out the modules
		pw.println("\t<modules>");
		
		for (String moduleName : moduleNames) {
			pw.print("\t\t<module>\t\t\t");
			
			pw.println(moduleName);
			
			pw.println("\t\t</module>");
		}
		
		pw.println("\t</modules>");
		
		pw.println("</project>");
		
		pw.close();
	}
	
	public static String getRepositoryPath (String repositoryName, String repositorySubPath) {
		String repo = getRepositoryFile(repositoryName).getAbsolutePath();
		
		String repoPath = repo;
		
		if (repositorySubPath != null && !repositorySubPath.isEmpty()) {
			repoPath = repo + "/" + repositorySubPath;
		}
		
		return repoPath;
		
	}

	public static File checkOut(String repositoryName, String repositorySubPath, String workingCopyName, String userName, String password) {
		
		File workingCopy = getWorkingCopyFile(repositoryName, workingCopyName);
		
		String repoPath = getRepositoryPath(repositoryName, repositorySubPath);
		
		long rev = SVNUtils.getInstance().checkout(repoPath, workingCopy, null, null);
		
		return workingCopy;
	}
	public static File checkOut(String repositoryName, String repositorySubPath, String	userName, String password) {
		return checkOut(repositoryName, repositorySubPath, repositoryName + "-wc", userName, password);
	}

}
