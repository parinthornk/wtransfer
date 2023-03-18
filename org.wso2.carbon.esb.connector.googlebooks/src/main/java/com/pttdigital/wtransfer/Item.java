package com.pttdigital.wtransfer;

import java.sql.Timestamp;
import java.util.HashMap;

public class Item {
	public String workspace;
	public int session;
	public String name;
	public Timestamp created;
	public Timestamp modified;
	public int retryQuota;
	public int retryRemaining;
	public int retryIntervalMs;
	public Timestamp timeNextRetry;
	public Timestamp timeLatestRetry;
	public String status;
	public String fnCallback;
	
	public static final String wordStatus = "status"; // TODO: modify this after DB field change
	public static final String wordStatusOperation = "ilike"; // TODO: modify this after DB field change
	
	public static String getStatus(HashMap<String, String> query) {
		
		if (query == null) {
			return null;
		}
		
		if (!query.containsKey(wordStatus)) {
			return null;
		}
		
		Item i = new Item();
		i.status = query.get(wordStatus); // TODO: need to modify "wordStatus" accordingly
		return i.status;
	}
}