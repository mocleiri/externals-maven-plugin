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
package org.kuali.maven.plugins.externals;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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

public class POMUtils {
	private static final Logger logger = LoggerFactory.getLogger(POMUtils.class);

	private static final String FORMAT_PRETTY_PRINT = "format-pretty-print";
	private static final String XML_DECLARATION = "xml-declaration";
	private static final String DOM_IMPLEMENTATION = "LS";
	private static final String GROUP_ID = "groupId";
	private static final String ARTIFACT_ID = "artifactId";
	private static final String VERSION = "version";
	private static final String PARENT = "parent";
	private static final String PROJECT = "project";
	private static final String SCM = "scm";
	private static final String DEVELOPER_CONNECTION = "developerConnection";
	private static final String CONNECTION = "connection";
	private static final String PROPERTIES = "properties";
	private static final String URL = "url";

	public String format(String xml) {
		try {
			Reader reader = new StringReader(xml);
			InputSource src = new InputSource(reader);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(src);
			return getFormattedXml(document);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public String getFormattedXml(Document document) {
		try {
			Element element = document.getDocumentElement();
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation(DOM_IMPLEMENTATION);
			LSSerializer writer = impl.createLSSerializer();
			DOMConfiguration configuration = writer.getDomConfig();
			configuration.setParameter(FORMAT_PRETTY_PRINT, true);
			configuration.setParameter(XML_DECLARATION, false);
			return writer.writeToString(element);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public GAV getParentGAV(String xml) {
		Document document = getDocument(xml);
		return getParentGav(document);
	}

	public GAV getGAV(String xml) {
		Document document = getDocument(xml);
		NodeList nodeList = document.getChildNodes();
		if (nodeList == null || nodeList.getLength() == 0) {
			throw new IllegalArgumentException("Invalid xml for a Maven pom:\n " + xml);
		}
		if (nodeList.getLength() > 1) {
			throw new IllegalArgumentException("Invalid xml for a Maven pom.  Expected exactly 1 top level node:\n " + xml);
		}
		Node projectNode = nodeList.item(0);
		if (!projectNode.getNodeName().equals(PROJECT)) {
			throw new IllegalArgumentException("Invalid xml for a Maven pom.  Must start with a <project> tag:\n " + xml);
		}
		return getGAV(projectNode.getChildNodes());
	}

	public Document getDocument(String xml) {
		try {
			Reader reader = new StringReader(xml);
			InputSource src = new InputSource(reader);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(src);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public String updateScm(String xml, String prefix, String url) {
		Document document = getDocument(xml);
		NodeList nodeList = document.getElementsByTagName(SCM);
		Node scmNode = nodeList.item(0);
		NodeList scmNodeList = scmNode.getChildNodes();
		int childCount = scmNodeList.getLength();
		for (int i = 0; i < childCount; i++) {
			Node node = scmNodeList.item(i);
			if (node.getNodeName().equals(DEVELOPER_CONNECTION)) {
				node.setTextContent(prefix + url);
			} else if (node.getNodeName().equals(CONNECTION)) {
				node.setTextContent(prefix + url);
			} else if (node.getNodeName().equals(URL)) {
				node.setTextContent(url);
			}
		}
		return getFormattedXml(document);
	}

	public String updateParentVersion(String xml, String version) {
		Document document = getDocument(xml);
		NodeList nodeList = document.getElementsByTagName(PARENT);
		Node parentNode = nodeList.item(0);
		NodeList gavNodeList = parentNode.getChildNodes();
		updateVersion(gavNodeList, version);
		return getFormattedXml(document);
	}

	public String updateVersion(String xml, String version) {
		Document document = getDocument(xml);
		NodeList nodeList = document.getChildNodes();
		Node projectNode = nodeList.item(0);
		updateVersion(projectNode.getChildNodes(), version);
		return getFormattedXml(document);
	}

	public String updateProperties(String xml, Properties properties) {
		Document document = getDocument(xml);
		NodeList nodeList = document.getElementsByTagName(PROPERTIES);
		Node propertiesNode = nodeList.item(0);
		NodeList propertiesNodeList = propertiesNode.getChildNodes();
		List<String> keys = new ArrayList<String>(properties.stringPropertyNames());
		Collections.sort(keys);
		for (String key : keys) {
			String value = properties.getProperty(key);
			Node node = findNode(propertiesNodeList, key);
			node.setTextContent(value);
		}
		return getFormattedXml(document);
	}

	protected Node findNode(NodeList nodeList, String key) {
		int childCount = nodeList.getLength();
		for (int i = 0; i < childCount; i++) {
			Node node = nodeList.item(i);
			String name = node.getNodeName();
			if (name.equals(key)) {
				return node;
			}
		}
		throw new IllegalArgumentException("Unable to locate property '" + key + "'");
	}

	protected void updateVersion(NodeList nodeList, String version) {
		int childCount = nodeList.getLength();
		for (int i = 0; i < childCount; i++) {
			Node node = nodeList.item(i);
			if (node.getNodeName().equals(VERSION)) {
				node.setTextContent(version);
				return;
			}
		}
		throw new IllegalStateException("No <" + VERSION + "> tag was found");
	}

	public GAV getGAV(NodeList nodeList) {
		GAV gav = new GAV();
		int childCount = nodeList.getLength();
		for (int i = 0; i < childCount; i++) {
			Node node = nodeList.item(i);
			if (node.getNodeName().equals(GROUP_ID)) {
				gav.setGroupId(nullSafeTrim(node.getTextContent()));
			}
			if (node.getNodeName().equals(ARTIFACT_ID)) {
				gav.setArtifactId(nullSafeTrim(node.getTextContent()));
			}
			if (node.getNodeName().equals(VERSION)) {
				gav.setVersion(nullSafeTrim(node.getTextContent()));
			}
		}
		return gav;
	}

	public GAV getParentGav(Document document) {
		NodeList nodeList = document.getElementsByTagName(PARENT);
		if (nodeList == null || nodeList.getLength() == 0) {
			return null;
		}
		if (nodeList.getLength() > 1) {
			throw new IllegalStateException("There should only be one <" + PARENT + "> tag in a pom");
		}
		Node parentNode = nodeList.item(0);
		NodeList gavNodeList = parentNode.getChildNodes();
		return getGAV(gavNodeList);
	}

	public void display(NodeList nodeList, int level) {
		String content = getDisplayString(nodeList, level);
		logger.info(content);
	}

	protected String getDisplayString(NodeList nodeList, int level) {
		StringBuilder sb = new StringBuilder();
		int childCount = nodeList.getLength();
		level++;
		for (int i = 0; i < childCount; i++) {
			Node node = nodeList.item(i);
			sb.append(getDisplayString(node, level) + "\n");
			sb.append(getDisplayString(node.getChildNodes(), level));
		}
		return sb.toString();
	}

	protected String getDisplayString(Node node) {
		return getDisplayString(node, -1);
	}

	protected String getDisplayString(Node node, int level) {
		String name = node.getNodeName();
		int type = node.getNodeType();
		String value = node.getNodeValue();
		StringBuilder sb = new StringBuilder();
		if (level != -1) {
			sb.append(level + ":");
			sb.append(StringUtils.repeat(" ", level));
		}
		sb.append("[");
		sb.append(name);
		sb.append(",");
		sb.append(type);
		sb.append(",");
		sb.append(flatten(value));
		sb.append("]");
		return sb.toString();
	}

	protected String nullSafeTrim(String s) {
		if (s == null) {
			return null;
		} else {
			return s.trim();
		}
	}

	protected String flatten(String s) {
		if (s == null) {
			return null;
		} else {
			return s.replace("\n", "LF").replace("\r", "CR");
		}
	}
}
