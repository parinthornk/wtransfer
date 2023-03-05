package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class Config {
	public static String tableName = "config";
	public static String nameOrId = "name";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"name",
		"description",
		"created",
		"modified",
		"replace",
		"deleteAfterSuccess",
		"retryCount",
		"retryIntervalMs",
		"fnIsFileNameToMove",
		"fnRenameTo",
		"archiveFolder",
		"fnArcRenameTo",
	};
	
	private static String[] editableFields = new String[] {
		"description",
		"replace",
		"deleteAfterSuccess",
		"retryCount",
		"retryIntervalMs",
		"fnIsFileNameToMove",
		"fnRenameTo",
		"archiveFolder",
		"fnArcRenameTo",
	};
	
	private static HashMap<String, Object> mapCreate(Config config){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", config.workspace);
		map.put("name", config.name);
		map.put("description", config.description);
		map.put("created", now);
		map.put("modified", now);
		map.put("replace", config.replace);
		map.put("deleteAfterSuccess", config.deleteAfterSuccess);
		map.put("retryCount", config.retryCount);
		map.put("retryIntervalMs", config.retryIntervalMs);
		map.put("fnIsFileNameToMove", config.fnIsFileNameToMove);
		map.put("fnRenameTo", config.fnRenameTo);
		map.put("archiveFolder", config.archiveFolder);
		map.put("fnArcRenameTo", config.fnArcRenameTo);
		return map;
	}
	
	public static Config parse(JsonElement e) throws Exception {
		try {
			JsonObject o = e.getAsJsonObject();
			Config x = new Config();
			x.workspace = DB.getAsString(o, "workspace");
			x.name = DB.getAsString(o, "name");
			x.description = DB.getAsString(o, "description");
			x.created = DB.getAsTime(o, "created");
			x.modified = DB.getAsTime(o, "modified");
			x.replace = DB.getAsInt(o, "replace", 0);
			x.deleteAfterSuccess = DB.getAsInt(o, "deleteAfterSuccess", 0);
			x.retryCount = DB.getAsInt(o, "retryCount", 0);
			x.retryIntervalMs = DB.getAsInt(o, "retryIntervalMs", 0);
			x.fnIsFileNameToMove = DB.getAsString(o, "fnIsFileNameToMove");
			x.fnRenameTo = DB.getAsString(o, "fnRenameTo");
			x.archiveFolder = DB.getAsString(o, "archiveFolder");
			x.fnArcRenameTo = DB.getAsString(o, "fnArcRenameTo");
			return x;
		} catch (Exception ex) {
			throw new Exception("Failed to parse JsonElement into Config: " + e);
		}
	}
	
	public static Config parse(String json) throws JsonSyntaxException, Exception { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public String name;
	public String description;
	public Timestamp created;
	public Timestamp modified;
	public int replace;
	public int deleteAfterSuccess;
	public int retryCount;
	public int retryIntervalMs;
	public String fnIsFileNameToMove;
	public String fnRenameTo;
	public String archiveFolder;
	public String fnArcRenameTo;
	
	public static Config[] parse(JsonArray arr) throws Exception {
		Config[] x;
		x = new Config[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}
	
	public static ZConnector.ZResult list(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult list() {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + ";";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, Config config) {
		if (config == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(config);
		m1.put("workspace", workspace);
		String sql = "insert into " + Constant.SCHEMA + "." + tableName + " (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return DB.getJsonCreate(sql, nameOrId);
	}
	
	public static ZConnector.ZResult update(String workspace, Object objNameOrId, HashMap<String, Object> m1) {
		try {
			HashMap<String, Object> m2 = DB.removeSomeFields(m1, editableFields);
			if (m2.size() < 1) { throw new Exception("No fields to update."); }
			Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			m2.put("modified", now);
			String sql = "update " + Constant.SCHEMA + "." + tableName + " SET " + DB.concateStmtKeysValues(m2) + " WHERE " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "';";
			return DB.getJsonUpdate(tableName, objNameOrId, sql, nameOrId);
		} catch (Exception e) {
			return ZConnector.ZResult.ERROR_400(e);
		}
	}
	
	public static ZConnector.ZResult delete(String workspace, Object objNameOrId) {
		String sql = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "'";
		return DB.getJsonDelete(tableName, objNameOrId, sql);
	}
	
	public static HashMap<String, Object> parsePatch(String json) {
		HashMap<String, Object> m1 = new HashMap<String, Object>();
		JsonElement e = new Gson().fromJson(json, JsonElement.class);
		JsonObject o = e.getAsJsonObject();
		try { String field = "workspace"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "name"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "description"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "created"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "modified"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "replace"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "deleteAfterSuccess"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "retryCount"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "retryIntervalMs"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "fnIsFileNameToMove"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "fnRenameTo"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "archiveFolder"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "fnArcRenameTo"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		return m1;
	}
}