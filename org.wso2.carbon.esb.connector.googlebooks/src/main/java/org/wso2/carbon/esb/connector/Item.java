package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Item {
	
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
	
	public enum Status {
		CREATED,
		QUEUED,
		EXECUTING,
		WAITING_FOR_RETRY,
		FAILED,
		SUCCESS,
	}
	
	public static String tableName = "item";
	public static String nameOrId = "name";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"task",
		"name",
		"created",
		"modified",
		"retryQuota",
		"retryRemaining",
		"retryIntervalMs",
		"timeNextRetry",
		"timeLatestRetry",
		"status",
	};
	
	private static String[] editableFields = new String[] {
		"workspace",
		"task",
		"name",
		"created",
		"modified",
		"retryQuota",
		"retryRemaining",
		"retryIntervalMs",
		"timeNextRetry",
		"timeLatestRetry",
		"status",
	};
	
	private static HashMap<String, Object> mapCreate(Item item){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", item.workspace);
		map.put("task", item.task);
		map.put("name", item.name);
		map.put("created", now);
		map.put("modified", now);
		map.put("retryQuota", item.retryQuota);
		map.put("retryRemaining", item.retryRemaining);
		map.put("retryIntervalMs", item.retryIntervalMs);
		map.put("timeNextRetry", item.timeNextRetry);
		map.put("timeLatestRetry", item.timeLatestRetry);
		map.put("status", item.status);
		if (map.containsKey("id")) { map.remove("id"); }
		return map;
	}
	
	public static Item parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Item x = new Item();
		x.workspace = DB.getAsString(o, "workspace");
		x.task = DB.getAsInt(o, "task", 0);
		x.name = DB.getAsString(o, "name");
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		x.retryQuota = DB.getAsInt(o, "retryQuota", 0);
		x.retryRemaining = DB.getAsInt(o, "retryRemaining", 0);
		x.retryIntervalMs = DB.getAsInt(o, "retryIntervalMs", 0);
		x.timeNextRetry = DB.getAsTime(o, "timeNextRetry");
		x.timeLatestRetry = DB.getAsTime(o, "timeLatestRetry");
		x.status = DB.getAsString(o, "status");
		return x;
	}
	
	public static Item parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public long task;
	public String name;
	public Timestamp created;
	public Timestamp modified;
	public int retryQuota;
	public int retryRemaining;
	public int retryIntervalMs;
	public Timestamp timeNextRetry;
	public Timestamp timeLatestRetry;
	public String status;
	
	public static ZConnector.ZResult list(String workspace, long task) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and task = '" + task + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult list(String workspace, String status) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and status ilike '" + status + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult list(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult listCreatedOrRetrying() {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where status ilike '" + Item.Status.WAITING_FOR_RETRY.toString() + "' or status ilike '" + Item.Status.CREATED.toString() + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, long task, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and task = '" + task + "' and " + nameOrId + " = '" + objNameOrId + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, long task, Item item) {
		if (item == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(item);
		m1.put("workspace", workspace);
		m1.put("task", task);
		String sql = "insert into " + Constant.SCHEMA + "." + tableName + " (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return DB.getJsonCreate(sql, nameOrId);
	}
	
	public static ZConnector.ZResult update(String workspace, long task, Object objNameOrId, HashMap<String, Object> m1) {
		try {
			HashMap<String, Object> m2 = DB.removeSomeFields(m1, editableFields);
			if (m2.size() < 1) { throw new Exception("No fields to update."); }
			Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			m2.put("modified", now);
			String sql = "update " + Constant.SCHEMA + "." + tableName + " SET " + DB.concateStmtKeysValues(m2) + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "' and task = '" + task + "';";
			return DB.getJsonUpdate(tableName, objNameOrId, sql, nameOrId);
		} catch (Exception e) {
			return ZConnector.ZResult.ERROR_400(e);
		}
	}
	
	public static ZConnector.ZResult update(String workspace, Object objNameOrId, HashMap<String, Object> m1) {
		try {
			HashMap<String, Object> m2 = DB.removeSomeFields(m1, editableFields);
			if (m2.size() < 1) { throw new Exception("No fields to update."); }
			Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			m2.put("modified", now);
			String sql = "update " + Constant.SCHEMA + "." + tableName + " SET " + DB.concateStmtKeysValues(m2) + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "';";
			return DB.getJsonUpdate(tableName, objNameOrId, sql, nameOrId);
		} catch (Exception e) {
			return ZConnector.ZResult.ERROR_400(e);
		}
	}
	
	public static ZConnector.ZResult delete(String workspace, long task, Object objNameOrId) {
		String sql = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "' and task = '" + task + "';";
		return DB.getJsonDelete(tableName, objNameOrId, sql);
	}
	
	public static HashMap<String, Object> parsePatch(String json) {
		HashMap<String, Object> m1 = new HashMap<String, Object>();
		JsonElement e = new Gson().fromJson(json, JsonElement.class);
		JsonObject o = e.getAsJsonObject();
		try { String field = "workspace"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "task"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "name"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "created"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "modified"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "retryQuota"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "retryRemaining"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "retryIntervalMs"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "timeNextRetry"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "timeLatestRetry"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "status"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		return m1;
	}
	
	public static Item[] parse(JsonArray arr) {
		Item[] x;
		x = new Item[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}

	public static JsonElement toJsonElement(Item x) {
		HashMap<String, Object> map = mapCreate(x);
		
		map.put("timeNextRetry", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(x.timeNextRetry));
		map.put("timeLatestRetry", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(x.timeLatestRetry));
		
		return new Gson().fromJson(ZConnector.ConvertToJsonString(map), JsonElement.class);
	}
}