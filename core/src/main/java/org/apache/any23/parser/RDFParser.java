package org.apache.any23.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class RDFParser {
	
	private final static String RDF_NAME_SPACE = "rdf";
	private final static String RDF = "RDF";
	private final static String DESCRIPTION = "rdf:Description";
	private final static String ABOUT = RDF_NAME_SPACE + ":" + "about";
	private final static String RDF_XMLNS = "xmlns:rdf";
	private final static String RDF_XMLNS_VALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	
	public final static String LABEL = RDF_NAME_SPACE + ":" + "label";

	private Document _resultDoc;
	
	private ElementExtractor _extractor;
	
	public RDFParser(ElementExtractor extractor) {
		_extractor = extractor;
	}
	
	public String toRDF() throws ParserConfigurationException, TransformerException, DOMException, SAXException, IOException{
		
		DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbfactory.newDocumentBuilder();
		
		_resultDoc = db.newDocument();

		Element root = _resultDoc.createElement(RDF_NAME_SPACE+":"+RDF);
		root.setAttribute(RDF_XMLNS, RDF_XMLNS_VALUE);
		
		for (KeyValuePair<String, String> namespace : _extractor.getNameSpaces()) {
			root.setAttribute(namespace.getKey(), namespace.getValue());
		}
		
		_resultDoc.appendChild(root);
		
		for (Element child : _extractor.getNodes(this)) {
			root.appendChild(child);
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		DOMSource source = new DOMSource(_resultDoc);
		StreamResult result = new StreamResult(new ByteArrayOutputStream());
		transformer.transform(source, result);

		return result.getOutputStream().toString();
	}

	public Element createDescription(String aboutContent) {
		Element description = _resultDoc.createElement(DESCRIPTION);
		description.setAttribute(ABOUT, aboutContent);
		return description;
	}
	
	public Element createEmptyNode(String name){
		return _resultDoc.createElement(name);
	}
	
	public Element createNodeWithText(String name, String content) {
		Element result;
		result = _resultDoc.createElement(name);
		result.appendChild(_resultDoc.createTextNode(content));
		return result;
	}
	
}
