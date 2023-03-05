package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Log_Task {
	public static String tableName = "log_task";
	public static String nameOrId = "id";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"id",
		"task",
		"created",
		"modified",
		"logType",
		"title",
		"body",
	};
	
	private static HashMap<String, Object> mapCreate(Log_Task log_task){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", log_task.workspace);
		map.put("id", log_task.id);
		map.put("task", log_task.task);
		map.put("created", now);
		map.put("modified", now);
		map.put("logType", log_task.logType);
		map.put("title", log_task.title);
		map.put("body", log_task.body);
		if (map.containsKey("id")) { map.remove("id"); }
		return map;
	}
	
	public static Log_Task parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Log_Task x = new Log_Task();
		x.workspace = DB.getAsString(o, "workspace");
		x.id = DB.getAsLong(o, "id", 0);
		x.task = DB.getAsInt(o, "task", 0);
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		x.logType = DB.getAsString(o, "logType");
		x.title = DB.getAsString(o, "title");
		x.body = DB.getAsString(o, "body");
		return x;
	}
	
	public static Log_Task parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public long id;
	public int task;
	public Timestamp created;
	public Timestamp modified;
	public String logType;
	public String title;
	public String body;
	
	public static Log_Task[] parse(JsonArray arr) {
		Log_Task[] x;
		x = new Log_Task[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}
	
	public static ZConnector.ZResult listByWorkspace(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult listByTask(String workspace, long task) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where task = '" + task + "' and workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, long task, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "' and task = '" + task + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, long task, Log_Task ls) {
		if (ls == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(ls);
		m1.put("workspace", workspace);
		m1.put("task", task);
		String sql = "insert into " + Constant.SCHEMA + "." + tableName + " (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return DB.getJsonCreate(sql, nameOrId);
	}
	
	public static ZConnector.ZResult delete(String workspace, long task, Object objNameOrId) {
		String sql = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "' and task = '" + task + "';";
		return DB.getJsonDelete(tableName, objNameOrId, sql);
	}
}