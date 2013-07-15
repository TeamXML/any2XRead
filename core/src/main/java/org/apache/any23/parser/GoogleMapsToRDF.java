package org.apache.any23.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GoogleMapsToRDF implements ElementExtractor{
	
	private final static String GMAPS = "gmaps";
	private final static String GOOGLEMAPS_XMLNS = "xmlns:" + GMAPS;
	private final static String GEO = "geo";
	private final static String GEO_XMLNS = "xmlns:" + GEO;
	private final static String GEO_XMLNS_VALUE = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	private final static String ADDRESS = "address";
	private final static String ADDRESS_XMLNS = "xmlns:"+ADDRESS;
	private final static String ADDRESS_XMLNS_VALUE = "http://linkedgeodata.org/ontology/addr%3";

	// gmap types
	private final static String TYPE = "type";
	private final static String VIEWPORT = "viewport";
	private final static String FORMATTED_ADRESS = "formatted_address";
	private final static String ADRESS_COMPONENT = "address_component";
	private final static String SHORT_NAME = "short_name";
	private final static String GEOMETRY = "geometry";
	private final static String ROUTE = "route";
	private final static String STREET_NUMBER = "street_number";
	private final static String LOCALITY = "locality";
	private final static String COUNTRY = "country";
	private final static String POSTAL_CODE = "postal_code";
	private final static String POLITICAL = "political";
	private final static String LOCATION = "location";
	private final static String LNG = "lng";
	private final static String LAT = "lat";

	// our types
	private final static String A_COUNTRY = ADDRESS + ":" + "Acountry";
	private final static String A_STREET = ADDRESS + ":" + "Astreet";
	private final static String A_HOUSE_NUMBER = ADDRESS + ":" + "Ahousenumber";
	private final static String A_CITY = ADDRESS + ":" + "Acity";
	private final static String POSTCODE = ADDRESS + ":" + "postcode";
	private final static String GEO_LONG = GEO+":long";
	private final static String GEO_LAT = GEO+":lat";
	
	private InputStream _inputStream;
	private String _url;
	private RDFParser _rdfParser;
	private List<KeyValuePair<String, String>> _nameSpaces;

	public GoogleMapsToRDF(InputStream inputStream, String url) {
		_inputStream = inputStream;
		_url = url;
		_nameSpaces = new ArrayList<KeyValuePair<String,String>>();		
		_nameSpaces.add(new KeyValuePair<String, String>(GOOGLEMAPS_XMLNS, url.substring(0, url.indexOf("&"))));
		_nameSpaces.add(new KeyValuePair<String, String>(GEO_XMLNS, GEO_XMLNS_VALUE));
		_nameSpaces.add(new KeyValuePair<String, String>(ADDRESS_XMLNS, ADDRESS_XMLNS_VALUE));
	}
	
	@Override
	public List<Element> getNodes(RDFParser parser) throws ParserConfigurationException, SAXException, IOException {
		
		_rdfParser = parser;
		
		DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbfactory.newDocumentBuilder();
		
		final Document googleMapsDoc = db.parse(new InputSource(
				new InputStreamReader(_inputStream)));

		Node item = googleMapsDoc.getFirstChild().getChildNodes().item(3);

		return processAllChildren(item);
	}

	private List<Element> processAllChildren(Node item) {
		List<Element> result = new ArrayList<Element>();

		for (int i = 0; i < item.getChildNodes().getLength(); i++) {
			Node nextNode = item.getChildNodes().item(i);

			// Handle Element Node and filter first two occurences of type
			if (nextNode.getNodeType() == Node.ELEMENT_NODE
					&& !nextNode.getNodeName().equals(TYPE)) {

				// Handle AdressComponents
				if (nextNode.getNodeName().equals(ADRESS_COMPONENT)) {
					Element addressNode = handleAdressComponent(nextNode);
					if (addressNode != null)
						result.add(addressNode);
					// Handle geometry
				} else if (nextNode.getNodeName().equals(GEOMETRY)) {
					// Look for location Node
					for (int j = 0; j < nextNode.getChildNodes().getLength(); j++) {
						Node geometryNode = nextNode.getChildNodes().item(j);
						if (isValidElementNode(geometryNode) && geometryNode.getNodeName().equals(LOCATION)) {
							result.addAll(handleGeometryComponent(geometryNode));
						}
					}
				} else if (nextNode.getNodeName().equals(FORMATTED_ADRESS)){
					Element labelNode = handleFormattedAdressComponent(nextNode);
					
					if (labelNode != null){
						result.add(labelNode);
					}
				}
			}

		}

		Element descriptionElement = _rdfParser.createDescription(_url);
		
		for (Element element : result) {
			descriptionElement.appendChild(element);
		}
		
		result.clear();
		result.add(descriptionElement);
		
		return result;
	}

	private Element handleFormattedAdressComponent(Node nextNode) {
		String label = getNodeValue(nextNode.getChildNodes());		
		
		if (!label.isEmpty())
			return _rdfParser.createNodeWithText(RDFParser.LABEL, label);
		
		return null;
	}

	private List<Element> handleGeometryComponent(Node item) {

		String latittude = "";
		String longitude = "";
		List<Element> result = new ArrayList<Element>();
		
		for (int i = 0; i < item.getChildNodes().getLength(); i++) {
			Node child = item.getChildNodes().item(i);
			if (isValidElementNode(child)) {
				if (child.getNodeName().equals(LAT)) {
					latittude = getNodeValue(child.getChildNodes());
				} else if (child.getNodeName().equals(LNG))
					longitude = getNodeValue(child.getChildNodes());
			}
		}
		
		if (!latittude.isEmpty() && !longitude.isEmpty()){
			result.add(_rdfParser.createNodeWithText(GEO_LAT, latittude));
			result.add(_rdfParser.createNodeWithText(GEO_LONG, longitude));
		}

		return result;
	}

	private Element handleAdressComponent(Node item) {
		String type = "";
		String shortName = "";
		Element result = null;

		for (int i = 0; i < item.getChildNodes().getLength(); i++) {
			Node child = item.getChildNodes().item(i);
			if (isValidElementNode(child)) {
				if (child.getNodeName().equals(TYPE)) {
					String loc_type = getNodeValue(child.getChildNodes());
					if (!loc_type.equals(POLITICAL))
						type = loc_type;
				} else if (child.getNodeName().equals(SHORT_NAME))
					shortName = getNodeValue(child.getChildNodes());
			}
		}

		if (type.equals(ROUTE)) {
			result = _rdfParser.createNodeWithText(A_STREET, shortName);
		} else if (type.equals(STREET_NUMBER)) {
			result = _rdfParser.createNodeWithText(A_HOUSE_NUMBER, shortName);
		} else if (type.equals(LOCALITY)) {
			result = _rdfParser.createNodeWithText(A_CITY, shortName);
		} else if (type.equals(COUNTRY)) {
			result = _rdfParser.createNodeWithText(A_COUNTRY, shortName);
		} else if (type.equals(POSTAL_CODE)) {
			result = _rdfParser.createNodeWithText(POSTCODE, shortName);
		}

		return result;
	}

	private String getNodeValue(NodeList childNodes) {

		for (int i = 0; i < childNodes.getLength(); i++) {
			Node nextNode = childNodes.item(i);

			if (nextNode.getNodeType() == Node.TEXT_NODE
					&& !nextNode.getNodeValue().trim().isEmpty()) {
				return nextNode.getNodeValue();
			}
		}
		return "";
	}

	// We don't want certain types of nodes like "viewport"
	private boolean isValidElementNode(Node nextNode) {
		return nextNode.getNodeType() == Node.ELEMENT_NODE
				&& !nextNode.getNodeName().equals(VIEWPORT);
	}

	@Override
	public List<KeyValuePair<String, String>> getNameSpaces() {
		return _nameSpaces;
	}

	@Override
	public String getContext() {
		return _url;
	}
}
