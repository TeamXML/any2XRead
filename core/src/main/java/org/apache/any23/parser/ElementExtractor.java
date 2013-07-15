package org.apache.any23.parser;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public interface ElementExtractor {

	List<KeyValuePair<String, String>> getNameSpaces();
	String getContext();
	List<Element> getNodes(RDFParser parser) throws ParserConfigurationException, SAXException, IOException;

}
