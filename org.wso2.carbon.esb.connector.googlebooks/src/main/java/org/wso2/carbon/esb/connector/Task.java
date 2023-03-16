package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Task {
	
	public enum Status {
		PENDING,
		EXECUTED,
		ERROR,
	}
	
	public static String tableName = "task";
	public static String nameOrId = "id";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"source",
		"target",
		"pgp",
		"config",
		"schedule",
		"id",
		"description",
		"created",
		"modified",
		"status",
	};
	
	private static String[] editableFields = new String[] {
		"source",
		"target",
		"pgp",
		"config",
		"schedule",
		"description",
		"status",
	};
	
	private static HashMap<String, Object> mapCreate(Task task){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", task.workspace);
		map.put("source", task.source);
		map.put("target", task.target);
		map.put("pgp", task.pgp);
		map.put("config", task.config);
		map.put("schedule", task.schedule);
		map.put("id", task.id);
		map.put("description", task.description);
		map.put("created", now);
		map.put("modified", now);
		map.put("status", task.status);
		if (map.containsKey("id")) { map.remove("id"); }
		return map;
	}
	
	public static Task parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Task x = new Task();
		x.workspace = DB.getAsString(o, "workspace");
		x.source = DB.getAsString(o, "source");
		x.target = DB.getAsString(o, "target");
		x.pgp = DB.getAsString(o, "pgp");
		x.config = DB.getAsString(o, "config");
		x.schedule = DB.getAsString(o, "schedule");
		x.id = DB.getAsLong(o, "id", 0);
		x.description = DB.getAsString(o, "description");
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		x.status = DB.getAsString(o, "status");
		return x;
	}
	
	public static Task parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public String source;
	public String target;
	public String pgp;
	public String config;
	public String schedule;
	public long id;
	public String description;
	public Timestamp created;
	public Timestamp modified;
	public String status;
	
	public static Task[] parse(JsonArray arr) {
		Task[] x;
		x = new Task[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}

	public static ZResult listTasksBySchedule(String workspace, String schedule) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and schedule = '" + schedule + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult list(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult list(String workspace, String status) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and status ilike '" + status + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult listAllByStatus(String status) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where status ilike '" + status + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult listAll() {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + ";";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}

	public static ZResult get(String workspace, String schedule, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "' and schedule = '" + schedule + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, Task task) {
		if (task == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(task);
		m1.put("workspace", workspace);
		m1.put("status", Status.PENDING.toString());
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
		try { String field = "source"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "target"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "pgp"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "config"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "schedule"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "id"; m1.put(field, o.get(field).getAsLong()); } catch (Exception ex) { }
		try { String field = "description"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "created"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "modified"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "status"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		return m1;
	}

	public static JsonElement toJsonElement(Task x) {
		return new Gson().fromJson(ZConnector.ConvertToJsonString(mapCreate(x)), JsonElement.class);
	}
}