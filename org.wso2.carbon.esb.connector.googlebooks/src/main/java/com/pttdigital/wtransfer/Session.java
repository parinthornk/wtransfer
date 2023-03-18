package com.pttdigital.wtransfer;

import java.sql.Timestamp;
import java.util.HashMap;

public class Session {
	public String workspace;
	public String schedule;
	public long id;
	public String description;
	public Timestamp created;
	public Timestamp modified;
	public String status;
	
	public static final String wordStatus = "status"; // TODO: modify this after DB field change
	public static final String wordStatusOperation = "ilike"; // TODO: modify this after DB field change
	
	public static String getStatus(HashMap<String, String> query) {
		
		if (query == null) {
			return null;
		}
		
		if (!query.containsKey(wordStatus)) {
			return null;
		}
		
		Session s = new Session();
		s.status = query.get(wordStatus); // TODO: need to modify "wordStatus" accordingly
		return s.status;
	}
}