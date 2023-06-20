package com.pttdigital.wtransfer;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.Item.Info;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Item {
	public String workspace;
	public long session;
	public String name;
	public String description;
	public String folder;
	public String folderArchive;
	public String fileName;
	public String fileNameArchive;
	public Timestamp created;
	public Timestamp modified;
	public int retryQuota;
	public int retryRemaining;
	public int retryIntervalMs;
	public Timestamp timeNextRetry;
	public Timestamp timeLatestRetry;
	public String status;
	public String fnCallback;
	
	public enum Status {
		CREATED,
		QUEUED,
		ERROR_ENQUEUE,
		DEQUEUED,
		EXECUTING,
		WAITING_FOR_RETRY,
		FAILED,
		SUCCESS,
		DISMISS,
	}
	
	public static final String wordStatus = "status"; // TODO: modify this after DB field change
	public static final String wordStatusOperation = "ilike"; // TODO: modify this after DB field change
	
	public static final String word_timeLatestRetry = "timeLatestRetry"; // TODO: modify this after DB field change
	public static final String word_timeNextRetry = "timeNextRetry"; // TODO: modify this after DB field change
	public static final String word_retryRemaining = "retryRemaining"; // TODO: modify this after DB field change
	
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
	
	public static class Info {
		public String name;
		public long size;
		public boolean isDirectory;
		public Timestamp timestamp;
		
		public Info(String _name, long _size, boolean _isDirectory, Timestamp _timestamp) {
			name = _name;
			size = _size;
			isDirectory = _isDirectory;
			timestamp = _timestamp;
		}
		
		public static HashMap<String, Object> toDictionary(Info x) {
			HashMap<String, Object> ret = new HashMap<String, Object>();
			ret.put("name", x.name);
			ret.put("size", x.size);
			ret.put("isDirectory", x.isDirectory);
			ret.put("timestamp", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(x.timestamp));
			return ret;
		}
		
		public static JsonElement toJson(Info x) {
			return new Gson().fromJson(ZConnector.ConvertToJsonString(toDictionary(x)), JsonElement.class);
		}
		
		public static Info parse(JsonElement e) {
			JsonObject o = e.getAsJsonObject();
			String name = o.get("name").getAsString();
			long size = o.get("size").getAsLong();
			boolean isDirectory = o.get("isDirectory").getAsBoolean();
			Timestamp timestamp = Timestamp.valueOf(o.get("timestamp").getAsString());
			return new Info(name, size, isDirectory, timestamp);
		}
	}

	public static String getSessionWord() {
		return "session";
	}

	public static String getIdWord() {
		// TODO Auto-generated method stub
		return "id";
	}

	public static String getFileNameWord() {
		// TODO Auto-generated method stub
		return "fileName";
	}
	
	public static boolean markedAsRetry(Item i, Timestamp time) {
		
		// must have remaining retry available
		if (i.retryRemaining < 1) {
			return false;
		}
		
		// current time must be after timeNextRetry
		if (time.before(i.timeNextRetry)) {
			return false;
		}
		
		// must be in "WAITING_FOR_RETRY" state
		if (!i.status.equalsIgnoreCase(Item.Status.WAITING_FOR_RETRY.toString())) {
			return false;
		}
		
		return true;
	}
}