package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Log_Schedule {
	public static String tableName = "log_schedule";
	public static String nameOrId = "id";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"id",
		"schedule",
		"created",
		"modified",
		"logType",
		"title",
		"body",
	};
	
	private static HashMap<String, Object> mapCreate(Log_Schedule log_schedule){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", log_schedule.workspace);
		map.put("id", log_schedule.id);
		map.put("schedule", log_schedule.schedule);
		map.put("created", now);
		map.put("modified", now);
		map.put("logType", log_schedule.logType);
		map.put("title", log_schedule.title);
		map.put("body", log_schedule.body);
		if (map.containsKey("id")) { map.remove("id"); }
		return map;
	}
	
	public static Log_Schedule parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Log_Schedule x = new Log_Schedule();
		x.workspace = DB.getAsString(o, "workspace");
		x.id = DB.getAsLong(o, "id", 0);
		x.schedule = DB.getAsString(o, "schedule");
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		x.logType = DB.getAsString(o, "logType");
		x.title = DB.getAsString(o, "title");
		x.body = DB.getAsString(o, "body");
		return x;
	}
	
	public static Log_Schedule parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public long id;
	public String schedule;
	public Timestamp created;
	public Timestamp modified;
	public String logType;
	public String title;
	public String body;
	
	public static Log_Schedule[] parse(JsonArray arr) {
		Log_Schedule[] x;
		x = new Log_Schedule[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}
	
	public static ZConnector.ZResult listByWorkspace(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult listBySchedule(String workspace, String schedule) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where schedule = '" + schedule + "' and workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, String schedule, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "' and schedule = '" + schedule + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, String schedule, Log_Schedule ls) {
		if (ls == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(ls);
		m1.put("workspace", workspace);
		m1.put("schedule", schedule);
		String sql = "insert into " + Constant.SCHEMA + "." + tableName + " (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return DB.getJsonCreate(sql, nameOrId);
	}
	
	public static ZConnector.ZResult delete(String workspace, String schedule, Object objNameOrId) {
		String sql = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "' and schedule = '" + schedule + "';";
		return DB.getJsonDelete(tableName, objNameOrId, sql);
	}
}