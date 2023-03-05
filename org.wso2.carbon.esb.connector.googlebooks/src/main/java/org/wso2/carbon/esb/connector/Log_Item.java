package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Log_Item {
	public static String tableName = "log_item";
	public static String nameOrId = "id";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"id",
		"item",
		"created",
		"modified",
		"logType",
		"title",
		"body",
	};
	
	private static String[] editableFields = new String[] {
		"workspace",
		"id",
		"item",
		"created",
		"modified",
		"logType",
		"title",
		"body",
	};
	
	private static HashMap<String, Object> mapCreate(Log_Item log_item){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", log_item.workspace);
		map.put("id", log_item.id);
		map.put("item", log_item.item);
		map.put("created", now);
		map.put("modified", now);
		map.put("logType", log_item.logType);
		map.put("title", log_item.title);
		map.put("body", log_item.body);
		if (map.containsKey("id")) { map.remove("id"); }
		return map;
	}
	
	public static Log_Item parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Log_Item x = new Log_Item();
		x.workspace = DB.getAsString(o, "workspace");
		x.id = DB.getAsLong(o, "id", 0);
		x.item = DB.getAsString(o, "item");
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		x.logType = DB.getAsString(o, "logType");
		x.title = DB.getAsString(o, "title");
		x.body = DB.getAsString(o, "body");
		return x;
	}
	
	public static Log_Item parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public long id;
	public String item;
	public Timestamp created;
	public Timestamp modified;
	public String logType;
	public String title;
	public String body;
	
	public static ZConnector.ZResult listByWorkspace(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult listByItem(String workspace, String item) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where item = '" + item + "' and workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, String item, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "' and item = '" + item + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, String item, Log_Item ls) {
		if (ls == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(ls);
		m1.put("workspace", workspace);
		m1.put("item", item);
		String sql = "insert into " + Constant.SCHEMA + "." + tableName + " (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return DB.getJsonCreate(sql, nameOrId);
	}
	
	public static ZConnector.ZResult delete(String workspace, String item, Object objNameOrId) {
		String sql = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "' and workspace = '" + workspace + "' and item = '" + item + "';";
		return DB.getJsonDelete(tableName, objNameOrId, sql);
	}
	
	public static HashMap<String, Object> parsePatch(String json) {
		HashMap<String, Object> m1 = new HashMap<String, Object>();
		JsonElement e = new Gson().fromJson(json, JsonElement.class);
		JsonObject o = e.getAsJsonObject();
		try { String field = "workspace"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "id"; m1.put(field, o.get(field).getAsLong()); } catch (Exception ex) { }
		try { String field = "item"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "created"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "modified"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "logType"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "title"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "body"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		return m1;
	}
	
	public static Log_Item[] parse(JsonArray arr) {
		Log_Item[] x;
		x = new Log_Item[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}
}
