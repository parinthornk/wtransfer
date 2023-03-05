package org.wso2.carbon.esb.connector;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Schedule {
	public static String tableName = "schedule";
	public static String nameOrId = "name";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"name",
		"description",
		"source",
		"target",
		"pgp",
		"config",
		"plan",
		"enabled",
		"validFrom",
		"validUntil",
		"created",
		"modified",
		"previousCheckpoint",
	};
	
	private static String[] editableFields = new String[] {
		"description",
		"source",
		"target",
		"pgp",
		"config",
		"plan",
		"enabled",
		"validFrom",
		"validUntil",
		"previousCheckpoint",
	};
	
	private static HashMap<String, Object> mapCreate(Schedule schedule){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", schedule.workspace);
		map.put("name", schedule.name);
		map.put("description", schedule.description);
		map.put("source", schedule.source);
		map.put("target", schedule.target);
		map.put("pgp", schedule.pgp);
		map.put("config", schedule.config);
		map.put("plan", schedule.plan);
		map.put("enabled", schedule.enabled);
		map.put("validFrom", schedule.validFrom);
		map.put("validUntil", schedule.validUntil);
		map.put("created", now);
		map.put("modified", now);
		map.put("previousCheckpoint", now);
		return map;
	}
	
	public static Schedule parse(JsonElement e) {
		JsonObject o = e.getAsJsonObject();
		Schedule x = new Schedule();
		x.workspace = DB.getAsString(o, "workspace");
		x.name = DB.getAsString(o, "name");
		x.description = DB.getAsString(o, "description");
		x.source = DB.getAsString(o, "source");
		x.target = DB.getAsString(o, "target");
		x.pgp = DB.getAsString(o, "pgp");
		x.config = DB.getAsString(o, "config");
		x.plan = DB.getAsString(o, "plan");
		x.enabled = DB.getAsInt(o, "enabled", 0);
		x.validFrom = DB.getAsTime(o, "validFrom");
		x.validUntil = DB.getAsTime(o, "validUntil");
		x.created = DB.getAsTime(o, "created");
		x.modified = DB.getAsTime(o, "modified");
		x.previousCheckpoint = DB.getAsTime(o, "previousCheckpoint");
		return x;
	}
	
	public static Schedule parse(String json) { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public String name;
	public String description;
	public String source;
	public String target;
	public String pgp;
	public String config;
	public String plan;
	public int enabled;
	public Timestamp validFrom;
	public Timestamp validUntil;
	public Timestamp created;
	public Timestamp modified;
	public Timestamp previousCheckpoint;
	
	public static Schedule[] parse(JsonArray arr) {
		Schedule[] x;
		x = new Schedule[arr.size()];
		int n = 0;
		for (JsonElement e : arr) { x[n] = parse(e); n++; }
		return x;
	}
	
	public static ZConnector.ZResult list(String workspace) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "';";
		return DB.getJsonList(tableName, goodCamelCase, sql);
	}
	
	public static ZConnector.ZResult get(String workspace, Object objNameOrId) {
		String sql = "select * from " + Constant.SCHEMA + "." + tableName + " where workspace = '" + workspace + "' and " + nameOrId + " = '" + objNameOrId + "';";
		return DB.getJsonGet(tableName, objNameOrId, goodCamelCase, nameOrId, sql);
	}
	
	public static ZConnector.ZResult create(String workspace, Schedule schedule) {
		if (schedule == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(schedule);
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
		try { String field = "source"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "target"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "pgp"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "config"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "plan"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "enabled"; m1.put(field, o.get(field).getAsInt()); } catch (Exception ex) { }
		try { String field = "validFrom"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "validUntil"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "created"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "modified"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		try { String field = "previousCheckpoint"; m1.put(field, Timestamp.valueOf(o.get(field).getAsString())); } catch (Exception ex) { }
		return m1;
	}
}