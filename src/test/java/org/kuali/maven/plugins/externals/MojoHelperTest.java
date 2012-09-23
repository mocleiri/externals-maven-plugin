package org.kuali.maven.plugins.externals;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MojoHelperTest {
	private static final Logger logger = LoggerFactory.getLogger(SVNUtilsTest.class);

	MojoHelper helper = MojoHelper.getInstance();
	private static final String POM = "pom.xml";
	private static final String IGNORE = "src,target,.svn,.git";
	private static final File BASEDIR = new File("/Users/jeffcaddel/ws/aggregate");

	@Test
	public void testGetFiles() {
		try {
			List<File> poms = helper.getPoms(BASEDIR, POM, IGNORE);
			List<DefaultMutableTreeNode> nodes = helper.getNodes(poms);
			logger.info("poms.size()=" + poms.size());
			Map<String, DefaultMutableTreeNode> map = helper.getMap(nodes);
			logger.info("map.size()=" + map.size());
			DefaultMutableTreeNode tree = helper.getTree(BASEDIR, nodes, POM);
			String s = helper.getDisplayString(tree, BASEDIR, POM);
			logger.info("Maven Structure:\n" + s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
