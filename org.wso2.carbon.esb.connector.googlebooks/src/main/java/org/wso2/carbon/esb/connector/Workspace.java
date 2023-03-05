package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Workspace {
	public static String tableName = "workspace";
	public static String nameOrId = "name";
	
	private static String[] goodCamelCase = new String[] {
		"name",
		"description",
		"created",
		"modified",
	};
	
	private static String[] editableFields = new String[] {
		"description",
	};
	
	private static HashMap<String, Object> mapCreate(Workspace workspace){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", workspace.name);
		map.put("description", workspace.description);
		map.put("created", now);
		map.put("modified", now);
		return map;
	}
	
	public static Workspace parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Workspace x = new Workspace();
		x.name = DB.getAsString(o, "name");
		x.description = DB.getAsString(o, "description");
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		return x;
	}
	
	public static HashMap<String, Object> parsePatch(String json) {
		HashMap<String, Object> m1 = new HashMap<String, Object>();
		JsonElement e = new Gson().fromJson(json, JsonElement.class);
		JsonObject o = e.getAsJsonObject();
		try { String field = "name"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "description"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "created"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "modified"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		return m1;
	}
	
	public static Workspace parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String name;
	public String description;
	public Timestamp created;
	public Timestamp modified;
	
	public static Workspace[] parse(JsonArray arr) {
		Workspace[] x;
		x = new Workspace[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}
	
	public static ZConnector.ZResult list() {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + ";";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}

	public static ZConnector.ZResult get(Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(Workspace workspace) {
		if (workspace == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(workspace);
		String sql = "insert into " + Constant.SCHEMA + "." + tableName + " (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return DB.getJsonCreate(sql, nameOrId);
	}
	
	public static ZConnector.ZResult update(Object objNameOrId, HashMap<String, Object> m1) {
		try {
			HashMap<String, Object> m2 = DB.removeSomeFields(m1, editableFields);
			if (m2.size() < 1) { throw new Exception("No fields to update."); }
			Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			m2.put("modified", now);
			String sql = "update " + Constant.SCHEMA + "." + tableName + " SET " + DB.concateStmtKeysValues(m2) + " WHERE " + nameOrId + " = '" + objNameOrId + "';";
			return DB.getJsonUpdate(tableName, objNameOrId, sql, nameOrId);
		} catch (Exception e) {
			return ZConnector.ZResult.ERROR_400(e);
		}
	}
	
	public static ZConnector.ZResult delete(Object objNameOrId) {
		String sql = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "'";
		return DB.getJsonDelete(tableName, objNameOrId, sql);
	}
}
