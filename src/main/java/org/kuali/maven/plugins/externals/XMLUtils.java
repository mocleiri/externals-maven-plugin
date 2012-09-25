package org.kuali.maven.plugins.externals;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

public class XMLUtils {
	private static final Logger logger = LoggerFactory.getLogger(XMLUtils.class);

	private static final String FORMAT_PRETTY_PRINT = "format-pretty-print";
	private static final String XML_DECLARATION = "xml-declaration";
	private static final String DOM_IMPLEMENTATION = "LS";

	public String format(String xml) {
		try {
			Reader reader = new StringReader(xml);
			InputSource src = new InputSource(reader);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(src);
			Element element = document.getDocumentElement();
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation(DOM_IMPLEMENTATION);
			LSSerializer writer = impl.createLSSerializer();
			DOMConfiguration configuration = writer.getDomConfig();
			configuration.setParameter(FORMAT_PRETTY_PRINT, true);
			configuration.setParameter(XML_DECLARATION, false);
			return writer.writeToString(element);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getVersion(String xml) {
		try {
			Reader reader = new StringReader(xml);
			InputSource src = new InputSource(reader);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(src);
			NodeList children = document.getChildNodes();
			String s = getDisplayString(children, -1);
			logger.info("XML Structure: \n" + s);
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String getDisplayString(NodeList nodeList, int level) {
		StringBuilder sb = new StringBuilder();
		int childCount = nodeList.getLength();
		level++;
		for (int i = 0; i < childCount; i++) {
			Node node = nodeList.item(i);
			String name = node.getNodeName();
			int type = node.getNodeType();
			String value = node.getNodeValue();
			sb.append(level + ":" + StringUtils.repeat(" ", level) + "[" + name + "," + type + "," + flatten(value) + "]\n");
			sb.append(getDisplayString(node.getChildNodes(), level));
		}
		return sb.toString();
	}

	protected String flatten(String s) {
		if (s == null) {
			return null;
		} else {
			return s.replace("\n", "LF").replace("\r", "CR");
		}
	}
}
