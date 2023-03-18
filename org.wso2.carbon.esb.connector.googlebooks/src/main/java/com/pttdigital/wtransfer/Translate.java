package com.pttdigital.wtransfer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class Translate {
	
	/*private static final String[] dictInit = new String[] {
		"archivefolder", "archiveFolder", 
		"body", "body", 
		"created", "created", 
		"description", "description", 
		"enabled", "enabled", 
		"fn_archiverenameto", "fnArchiveRenameTo", 
		"fn_callback", "fnCallback", 
		"fn_dynamicdirsource", "fnDynamicDirSource", 
		"fn_dynamicdirtarget", "fnDynamicDirTarget", 
		"fn_isfiletomove", "fnIsFileToMove", 
		"fn_renameto", "fnRenameTo", 
		"host", "host", 
		"id", "id", 
		"item", "item", 
		"keypath", "keyPath", 
		"logtype", "logType", 
		"modified", "modified", 
		"name", "name", 
		"password", "password", 
		"pgp_fn_renameto", "fnPgpRenameTo", 
		"pgpdirection", "pgpDirection", 
		"pgpkeypassword", "pgpKeyPassword", 
		"pgpkeypath", "pgpKeyPath", 
		"plan", "plan", 
		"port", "port", 
		"previouscheckpoint", "previousCheckpoint", 
		"protocol", "protocol", 
		"retrycount", "retryCount", 
		"retryintervalms", "retryIntervalMs", 
		"retryquota", "retryQuota", 
		"retryremaining", "retryRemaining", 
		"schedule", "schedule", 
		"session", "session", 
		"sitesource", "siteSource", 
		"sitetarget", "siteTarget", 
		"staticdirsource", "staticDirSource", 
		"staticdirtarget", "staticDirTarget", 
		"status", "status", 
		"timelatestretry", "timeLastRetry", 
		"timenextretry", "timeNextRetry", 
		"title", "title", 
		"usedynamicdirsource", "useDynamicDirSource", 
		"usedynamicdirtarget", "useDynamicDirTarget", 
		"username", "username", 
		"validfrom", "validFrom", 
		"validuntil", "validUntil", 
		"workerthreads", "workerThreads", 
		"workspace", "workspace", 
	};
	
	public static void test() {
	}
	
	public static String translateToInsertDB(String x) throws Exception {
		String ret = getMapToInsertDB().get(x); 
		if (ret == null) {
			throw new Exception("Error translate \"" + x + "\", not found in dictionaly.");
		}
		return ret;
	}
	
	public static String translateToDisplayPostman(String x) throws Exception {
		String ret = getMapToDisplayPostman().get(x); 
		if (ret == null) {
			throw new Exception("Error translate \"" + x + "\", not found in dictionaly.");
		}
		return ret;
	}
	
	private static boolean checkCurrentDatabaseFieldsToDB = true;
	private static HashMap<String, String> mapToDB = null;
	private static HashMap<String, String> getMapToInsertDB() throws Exception {

		if (mapToDB == null) {
			mapToDB = new HashMap<String, String>();
			for (int i=0;i<dictInit.length;i+=2) {
				String database = dictInit[i];
				String postman = dictInit[i + 1];
				mapToDB.put(postman, database);
			}
		}
		
		if (checkCurrentDatabaseFieldsToDB) {
			checkCurrentDatabaseFieldsToDB = false;
			
			ArrayList<String> currentFields = DB.listAllFields();
			
			ArrayList<String> missing = new ArrayList<String>();
			
			for (String field : currentFields) {
				if (!mapToDB.containsValue(field)) {
					missing.add(field);
				}
			}
			
			if (missing.size() > 0) {
				
				String r = "";
				for (String m : missing) {
					r += m + ", ";
				}
				if (r.length() > 2) {
					r = r.substring(0, r.length() - 2);
				}
				
				throw new Exception("Missing fields [" + r + "], please update the dictionary map code.");
			}
		}
		
		return mapToDB;
	}
	
	private static boolean checkCurrentDatabaseFieldsToPostman = true;
	private static HashMap<String, String> mapToPostman = null;
	private static HashMap<String, String> getMapToDisplayPostman() throws Exception {
		
		if (mapToPostman == null) {
			mapToPostman = new HashMap<String, String>();
			for (int i=0;i<dictInit.length;i+=2) {
				String database = dictInit[i];
				String postman = dictInit[i + 1];
				mapToPostman.put(database, postman);
			}
		}
		
		if (checkCurrentDatabaseFieldsToPostman) {
			checkCurrentDatabaseFieldsToPostman = false;
			
			ArrayList<String> currentFields = DB.listAllFields();
			
			ArrayList<String> missing = new ArrayList<String>();
			
			for (String field : currentFields) {
				if (!mapToPostman.containsKey(field)) {
					missing.add(field);
				}
			}
			
			if (missing.size() > 0) {
				
				String r = "";
				for (String m : missing) {
					r += m + ", ";
				}
				if (r.length() > 2) {
					r = r.substring(0, r.length() - 2);
				}
				
				throw new Exception("Missing fields [" + r + "], please update the dictionary map code.");
			}
		}
		
		return mapToPostman;
	}*/
	


	private static boolean checkCurrentDatabaseTables = true;
	private static HashMap<String, String> mttype = null;
	private static HashMap<String, String> mapTables() throws Exception {
		if (mttype == null) {
			mttype = new HashMap<String, String>();
			mttype.put("Item", "name");
			mttype.put("LogItem", "id");
			mttype.put("LogSchedule", "id");
			mttype.put("LogSession", "id");
			mttype.put("Schedule", "name");
			mttype.put("Session", "id");
			mttype.put("Site", "name");
			mttype.put("Workspace", "name");
		}
		
		if (checkCurrentDatabaseTables) {
			checkCurrentDatabaseTables = false;
			
			ArrayList<String> currentTables = DB.listAllTables();
			
			ArrayList<String> missing = new ArrayList<String>();
			
			for (String table : currentTables) {
				if (!mttype.containsKey(table)) {
					missing.add(table);
				}
			}
			
			if (missing.size() > 0) {
				
				String r = "";
				for (String m : missing) {
					r += m + ", ";
				}
				if (r.length() > 2) {
					r = r.substring(0, r.length() - 2);
				}
				
				throw new Exception("Missing table [" + r + "], please update the dictionary mttype code.");
			}
		}
		return mttype;
	}
	
	public static String getNameOrId(Class<?> c) throws Exception {
		return mapTables().get(c.getSimpleName());
	}

	public static Object getValueFromJson(JsonObject o, Class<?> type, String fieldName) throws Exception {
		
		String fieldNameTranslated = fieldName;
		
		if (o.has(fieldNameTranslated)) {
			
			if (o.get(fieldNameTranslated).isJsonNull()) {
				return null;
			}
			
			String dtype = "" + type;
			if (dtype.equals("class java.lang.String")) {
				return o.get(fieldNameTranslated).getAsString();
			} else if (dtype.equals("int")) {
				return o.get(fieldNameTranslated).getAsInt();
			} else if (dtype.equals("long")) {
				return o.get(fieldNameTranslated).getAsLong();
			} else if (dtype.equals("class java.sql.Timestamp")) {
				return Timestamp.valueOf(o.get(fieldNameTranslated).getAsString());
			} else if (dtype.equals("boolean")) {
				return o.get(fieldNameTranslated).getAsBoolean();
			} else {
				throw new Exception("Unsupported datatype \"" + dtype + "\".");
			}
		}
		return null;
	}
}