package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLUtilsTest {
	private static final Logger logger = LoggerFactory.getLogger(XMLUtilsTest.class);

	MojoHelper helper = MojoHelper.getInstance();
	XMLUtils xmlUtils = new XMLUtils();
	private static final String POM = "pom.xml";
	private static final String IGNORE = "src,target,.svn,.git";
	private static final File BASEDIR = new File("/Users/jeffcaddel/ws/aggregate");

	@Test
	public void testGetFiles() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			File pom = poms.get(0);
			String oldXml = FileUtils.readFileToString(pom);
			String newXml = xmlUtils.format(oldXml);
			logger.info(newXml);
			GAV parent = xmlUtils.getParentGAV(newXml);
			GAV gav = xmlUtils.getGAV(newXml);
			logger.info(parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion());
			logger.info(gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
