package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class XMLUtilsTest {
	// private static final Logger logger = LoggerFactory.getLogger(XMLUtilsTest.class);

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
			// logger.info(newXml);
			xmlUtils.getVersion(newXml);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
