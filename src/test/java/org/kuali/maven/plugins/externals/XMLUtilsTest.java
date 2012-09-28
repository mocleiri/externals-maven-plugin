package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLUtilsTest {
	private static final Logger logger = LoggerFactory.getLogger(XMLUtilsTest.class);

	MojoHelper helper = MojoHelper.getInstance();
	POMUtils xmlUtils = new POMUtils();
	private static final String POM = "pom.xml";
	private static final String IGNORE = "src,target,.svn,.git";
	private static final File BASEDIR = new File("/Users/jeffcaddel/ws/aggregate");

	// @Test
	public void testModifyPoms() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			List<DefaultMutableTreeNode> nodes = helper.getNodes(poms);
			DefaultMutableTreeNode node = helper.getTree(BASEDIR, nodes, POM);
			helper.updateGavs(node);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testUpdateScm() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			String rootXml = null;
			for (File pom : poms) {
				String xml = FileUtils.readFileToString(pom);
				int pos = xml.indexOf("<artifactId>kuali-common</artifactId>");
				if (pos == -1) {
					continue;
				} else {
					rootXml = xml;
					break;
				}
			}
			String newXml = xmlUtils.updateScm(rootXml, "scm:svn:", "foo");
			logger.info(newXml);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testUpdateVersion() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			File pom = poms.get(0);
			String oldXml = FileUtils.readFileToString(pom);
			String newXml = xmlUtils.format(oldXml);
			String foo1Xml = xmlUtils.updateParentVersion(newXml, "foo1");
			logger.info(foo1Xml);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
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
