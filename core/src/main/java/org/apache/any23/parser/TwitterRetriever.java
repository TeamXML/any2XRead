package org.apache.any23.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

public class TwitterRetriever {

	private static final String TWITTER = "https://api.twitter.com/1.1/statuses/user_timeline.json";
	private String _completeURL;

	public String getCompleteURL() {
		return _completeURL;
	}

	public String retrieveTimeLine(String name, String count) throws Exception {

		String method = "GET";
		List<NameValuePair> urlParams = new ArrayList<NameValuePair>();
		urlParams.add(new NameValuePair("screen_name", name));
		urlParams.add(new NameValuePair("count", count));

		String oAuthConsumerKey = "0c50rX10hjE8xtuhqo1kKw";
		String oAuthConsumerSecret = "CfdA6mYnDBLERvjTcaHmQcy4bvkgav2ZbWJX3BX9SvY";

		String oAuthAccessToken = "1562954982-t7s7bOa0zliNGp0PF7ILuwtI0iYeJ1KOUo1wd0e";
		String oAuthAccessTokenSecret = "OGPfh7rncLAoyfFMmawnqWEKMMCUzUxugHhz17Dbw";

		String oAuthNonce = String.valueOf(System.currentTimeMillis());
		String oAuthSignatureMethod = "HMAC-SHA1";
		String oAuthTimestamp = time();
		String oAuthVersion = "1.0";

		String signatureBaseString1 = method;
		String signatureBaseString2 = TWITTER;

		List<NameValuePair> allParams = new ArrayList<NameValuePair>();
		allParams
				.add(new NameValuePair("oauth_consumer_key", oAuthConsumerKey));
		allParams.add(new NameValuePair("oauth_nonce", oAuthNonce));
		allParams.add(new NameValuePair("oauth_signature_method",
				oAuthSignatureMethod));
		allParams.add(new NameValuePair("oauth_timestamp", oAuthTimestamp));
		allParams.add(new NameValuePair("oauth_token", oAuthAccessToken));
		allParams.add(new NameValuePair("oauth_version", oAuthVersion));
		allParams.addAll(urlParams);

		Collections.sort(allParams, new NvpComparator());

		StringBuffer signatureBaseString3 = new StringBuffer();
		for (int i = 0; i < allParams.size(); i++) {
			NameValuePair nvp = allParams.get(i);
			if (i > 0) {
				signatureBaseString3.append("&");
			}
			signatureBaseString3.append(nvp.getName() + "=" + nvp.getValue());
		}

		String signatureBaseStringTemplate = "%s&%s&%s";
		String signatureBaseString = String.format(signatureBaseStringTemplate,
				URLEncoder.encode(signatureBaseString1, "UTF-8"),
				URLEncoder.encode(signatureBaseString2, "UTF-8"),
				URLEncoder.encode(signatureBaseString3.toString(), "UTF-8"));

		String compositeKey = URLEncoder.encode(oAuthConsumerSecret, "UTF-8")
				+ "&" + URLEncoder.encode(oAuthAccessTokenSecret, "UTF-8");

		String oAuthSignature = computeSignature(signatureBaseString,compositeKey);
		String oAuthSignatureEncoded = URLEncoder.encode(oAuthSignature,"UTF-8");

		String authorizationHeaderValueTempl = "OAuth oauth_consumer_key=\"%s\", oauth_nonce=\"%s\", oauth_signature=\"%s\", oauth_signature_method=\"%s\", oauth_timestamp=\"%s\", oauth_token=\"%s\", oauth_version=\"%s\"";

		String authorizationHeaderValue = String.format(
				authorizationHeaderValueTempl, oAuthConsumerKey, oAuthNonce,
				oAuthSignatureEncoded, oAuthSignatureMethod, oAuthTimestamp,
				oAuthAccessToken, oAuthVersion);

		StringBuffer urlWithParams = new StringBuffer(TWITTER);
		for (int i = 0; i < urlParams.size(); i++) {
			if (i == 0) {
				urlWithParams.append("?");
			} else {
				urlWithParams.append("&");
			}
			NameValuePair urlParam = urlParams.get(i);
			urlWithParams.append(urlParam.getName() + "=" + urlParam.getValue());
		}

		final String completeURLString = urlWithParams.toString();
		_completeURL = completeURLString.substring(0, completeURLString.indexOf("&"));

		GetMethod getMethod = new GetMethod(completeURLString);
		getMethod.addRequestHeader("Authorization", authorizationHeaderValue);

		HttpClient cli = new HttpClient();
		cli.executeMethod(getMethod);

		String response = getMethod.getResponseBodyAsString();
		return response;
	}

	private static String computeSignature(String baseString, String keyString)
			throws GeneralSecurityException, UnsupportedEncodingException,
			Exception {
		SecretKey secretKey = null;

		byte[] keyBytes = keyString.getBytes();
		secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

		Mac mac = Mac.getInstance("HmacSHA1");

		mac.init(secretKey);

		byte[] text = baseString.getBytes();

		return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
	}

	private String time() {
		long millis = System.currentTimeMillis();
		long secs = millis / 1000;
		return String.valueOf(secs);
	}

	public class NvpComparator implements Comparator<NameValuePair> {

		public int compare(NameValuePair arg0, NameValuePair arg1) {
			String name0 = arg0.getName();
			String name1 = arg1.getName();
			return name0.compareTo(name1);
		}
	}
}