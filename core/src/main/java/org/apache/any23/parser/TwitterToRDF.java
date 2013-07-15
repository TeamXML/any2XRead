package org.apache.any23.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

public class TwitterToRDF implements ElementExtractor {

	private static final String TWITTER = "twitter";
	private final static String TWITTER_XMLNS = "xmlns:" + TWITTER;
	private final static String TIMEZONE_XMLNS = "xmlns:timezone";
	private final static String TIMEZONE_XMLNS_CONTENT = "http://www.w3.org/2006/timezone#";

	private String _url;
	private String _twitter;
	private ArrayList<KeyValuePair<String, String>> _nameSpaces;
	private ArrayList<Element> _result;
	private RDFParser _parser;
	private boolean _userAlreadyExtracted;

	public TwitterToRDF(String twitter, String url) {
		_twitter = twitter;
		_url = url.substring(0,url.indexOf(".com")+5);

		_nameSpaces = new ArrayList<KeyValuePair<String, String>>();
		_nameSpaces.add(new KeyValuePair<String, String>(TWITTER_XMLNS, _url));
		_nameSpaces.add(new KeyValuePair<String, String>(TIMEZONE_XMLNS,
				TIMEZONE_XMLNS_CONTENT));
	}

	@SuppressWarnings("unchecked")
	private void extractMessages(Object o) {

		if (o instanceof Map) {
			extractMessageAttributes((Map<String, ?>) o);
		} else if (o instanceof Collection) {
			for (Object value : (Collection<?>) o) {
				extractMessages(value);
			}
		}
	}

	private void extractMessageAttributes(Map<String, ?> map) {
		final Element extractContent = extractContent(map);
		
		if (extractContent != null)
			_result.add(extractContent);
	}

	@SuppressWarnings("unchecked")
	private Element extractContent(Map<String, ?> map) {
		ArrayList<Element> children = new ArrayList<Element>();
		Element result = null;
		for (String key : map.keySet()) {
			if (isValidValue(key)) {
				Object k = map.get(key);
				if (k != null) {
					if (!(k instanceof Map) && !(k instanceof Collection)) {
						String value = getValue(k);

						if (key.equals("id_str")) {
							result = _parser.createDescription(_url + key + "=" + value);
						} else 
							children.add(_parser.createNodeWithText(createTag(key),	value));
					} else if (key.equals("user") && !_userAlreadyExtracted) {
						_userAlreadyExtracted = true;
						extractMessageAttributes((Map<String, ?>) k);
					}
				}
			}
		}

		if (result != null) {
			for (Element element : children) {
				result.appendChild(element);
			}
		}
		return result;
	}

	private String createTag(String key) {
		return TWITTER + ":" + key.toString();
	}

	// We throw out a lot of crap
	private boolean isValidValue(String key) {
		return !key.equals("indices") && !key.equals("id")
				&& !key.equals("created_at") && !key.equals("truncated")
				&& !key.equals("in_reply_to_status_id")
				&& !key.equals("in_reply_to_status_id_str")
				&& !key.equals("in_reply_to_user_id")
				&& !key.equals("in_reply_to_user_id_str")
				&& !key.equals("in_reply_to_screen_name")
				&& !key.equals("favorited") && !key.equals("retweeted")
				&& !key.equals("possibly_sensitive") && !key.equals("lang")
				&& !key.equals("follow_request_sent")
				&& !key.equals("default_profile_image")
				&& !key.equals("utc_offset") && !key.equals("geo_enabled")
				&& !key.equals("verified") && !key.equals("statuses_count")
				&& !key.equals("contributors_enabled")
				&& !key.equals("protected") && !key.equals("is_translator")
				&& !key.equals("listed_count")
				&& !key.equals("profile_background_color")
				&& !key.equals("profile_background_image_url")
				&& !key.equals("profile_background_image_url_https")
				&& !key.equals("profile_background_tile")
				&& !key.equals("profile_image_url_https")
				&& !key.equals("profile_banner_url")
				&& !key.equals("profile_link_color")
				&& !key.equals("profile_sidebar_border_color")
				&& !key.equals("profile_sidebar_fill_color")
				&& !key.equals("profile_text_color")
				&& !key.equals("profile_use_background_image")
				&& !key.equals("default_profile");
	}

	private String getValue(Object k) {
		if ((k instanceof Double)) {
			k = ((Double) k).intValue();
		}
		return k.toString();
	}

	@Override
	public List<KeyValuePair<String, String>> getNameSpaces() {
		return _nameSpaces;
	}

	@Override
	public String getContext() {
		return _url;
	}

	@Override
	public List<Element> getNodes(RDFParser parser)
			throws ParserConfigurationException, SAXException, IOException {

		_result = new ArrayList<Element>();
		_parser = parser;

		Object jsonobjekt = new Gson().fromJson(_twitter, Object.class);
		extractMessages(jsonobjekt);

		return _result;
	}
}
