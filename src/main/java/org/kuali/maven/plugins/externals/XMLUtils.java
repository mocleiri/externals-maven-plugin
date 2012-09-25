package org.kuali.maven.plugins.externals;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

public class XMLUtils {
	private static final String FORMAT_PRETTY_PRINT = "format-pretty-print";
	private static final String XML_DECLARATION = "xml-declaration";

	public String format(String xml) {
		try {
			Reader reader = new StringReader(xml);
			InputSource src = new InputSource(reader);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(src);
			Element element = document.getDocumentElement();
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
			LSSerializer writer = impl.createLSSerializer();
			DOMConfiguration configuration = writer.getDomConfig();
			configuration.setParameter(FORMAT_PRETTY_PRINT, true);
			configuration.setParameter(XML_DECLARATION, false);
			return writer.writeToString(element);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
