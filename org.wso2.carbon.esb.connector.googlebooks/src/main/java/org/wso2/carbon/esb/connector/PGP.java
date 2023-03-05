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

public class PGP {
	public static String tableName = "pgp";
	public static String nameOrId = "name";
	
	private static String[] goodCamelCase = new String[] {
		"workspace",
		"name",
		"description",
		"created",
		"modified",
		"direction",
		"keyPath",
		"keyPassword",
		"fnRenameTo",
	};
	
	private static String[] editableFields = new String[] {
		"description",
		"direction",
		"keyPath",
		"keyPassword",
		"fnRenameTo",
	};
	
	private static HashMap<String, Object> mapCreate(PGP pgp){
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("workspace", pgp.workspace);
		map.put("name", pgp.name);
		map.put("description", pgp.description);
		map.put("created", now);
		map.put("modified", now);
		map.put("direction", pgp.direction);
		map.put("keyPath", pgp.keyPath);
		map.put("keyPassword", pgp.keyPassword);
		map.put("fnRenameTo", pgp.fnRenameTo);
		return map;
	}
	
	public static PGP parse(JsonElement e) throws Exception {
		try {
			JsonObject o = e.getAsJsonObject();
			PGP x = new PGP();
			x.workspace = DB.getAsString(o, "workspace");
			x.name = DB.getAsString(o, "name");
			x.description = DB.getAsString(o, "description");
			x.created = DB.getAsTime(o, "created");
			x.modified = DB.getAsTime(o, "modified");
			x.direction = DB.getAsString(o, "direction");
			x.keyPath = DB.getAsString(o, "keyPath");
			x.keyPassword = DB.getAsString(o, "keyPassword");
			x.fnRenameTo = DB.getAsString(o, "fnRenameTo");
			return x;
		} catch (Exception ex) {
			throw new Exception("Failed to parse JsonElement into PGP: " + e);
		}
	}
	
	public static PGP parse(String json) throws JsonSyntaxException, Exception { return parse(new Gson().fromJson(json, JsonElement.class)); }
	
	public String workspace;
	public String name;
	public String description;
	public Timestamp created;
	public Timestamp modified;
	public String direction;
	public String keyPath;
	public String keyPassword;
	public String fnRenameTo;
	
	public static PGP[] parse(JsonArray arr) throws Exception {
		PGP[] x;
		x = new PGP[arr.size()];
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
	
	public static ZConnector.ZResult create(String workspace, PGP pgp) {
		if (pgp == null) { return ZConnector.ZResult.ERROR_400(new Exception(tableName + " cannot be null.")); }
		HashMap<String, Object> m1 = mapCreate(pgp);
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
		try { String field = "direction"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "keyPath"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "keyPassword"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		try { String field = "fnRenameTo"; m1.put(field, o.get(field).getAsString()); } catch (Exception ex) { }
		return m1;
	}
}