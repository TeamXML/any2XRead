package org.apache.any23.parser;

public class KeyValuePair<K,V> {

	private K _key;
	private V _value;
	
	public KeyValuePair(K key, V value) {
		_key = key;
		_value = value;
	}

	public K getKey() {
		return _key;
	}

	public V getValue() {
		return _value;
	}
	
	
	
}
