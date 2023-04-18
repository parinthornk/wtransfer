package com.pttdigital.wtransfer;

import java.lang.reflect.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class DB {
	
	public static String concateStmtKeysValues(HashMap<String, Object> m1) {
		String tKeys = "";
		Set<String> ks = m1.keySet();
		int n = 0;
		int size = ks.size();
		for (String k : ks) {
			tKeys += "\"" + k + "\"" + " = '" + m1.get(k) + "'";
			if (n < size - 1) {
				tKeys += ", ";
			}
			n++;
		}
		return tKeys;
	}
	
	public static String concateStmtKeys(HashMap<String, Object> m1) {
		String tKeys = "";
		Set<String> ks = m1.keySet();
		int n = 0;
		int size = ks.size();
		for (String k : ks) {
			tKeys += "\"" + k + "\"";
			if (n < size - 1) {
				tKeys += ", ";
			}
			n++;
		}
		return tKeys;
	}
	
	public static String concateStmtValues(HashMap<String, Object> m1) {
		String tKeys = "";
		Set<String> ks = m1.keySet();
		int n = 0;
		int size = ks.size();
		for (String k : ks) {
			Object value = m1.get(k);
			if (value == null) {
				tKeys += value;
			} else {
				tKeys += "'" + value + "'";
			}
			if (n < size - 1) {
				tKeys += ", ";
			}
			n++;
		}
		return tKeys;
	}
	
	public static JsonElement executeList(String sql) throws Exception {
		Connection c = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			rs = stmt.executeQuery(sql);
			JsonArray arr = new JsonArray();
			ResultSetMetaData rsmd = rs.getMetaData();
			while (rs.next()) {
				JsonObject j1 = new JsonObject();
				int numColumns = rsmd.getColumnCount();
				for (int i = 1; i <= numColumns; i++) {
					String fieldName = rsmd.getColumnName(i);
					Object fieldValue = rs.getObject(fieldName);
					if (fieldValue != null) { if (fieldValue.getClass().getName().equalsIgnoreCase("java.sql.Timestamp")) { fieldValue = new SimpleDateFormat(Constant.DATEFORMAT).format((java.sql.Timestamp)(fieldValue)); } }
					if (fieldValue == null) {
						j1.add(fieldName, JsonNull.INSTANCE);
					} else {
						if (fieldValue instanceof String) { j1.addProperty(fieldName, (String) fieldValue); }
						else if (fieldValue instanceof Integer) { j1.addProperty(fieldName, (int) fieldValue); }
						else if (fieldValue instanceof Long) { j1.addProperty(fieldName, (long) fieldValue); }
						else if (fieldValue instanceof Boolean) { j1.addProperty(fieldName, (boolean) fieldValue); }
						else { throw new Exception("Error recieving fieldValue from database, unsupported instanceof \"" + fieldValue.getClass().getName() + "\"."); }
					}
				}
				arr.add(j1);
			}
			JsonObject out = new JsonObject();
			out.addProperty("count", arr.size());
			out.add("list", arr);
			if (rs != null) { try { rs.close(); } catch (Exception e) { } }
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			return out;
		} catch (Exception ex) {
			if (rs != null) { try { rs.close(); } catch (Exception e) { } }
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			ex.printStackTrace();
			throw new Exception("Error executeSqlList, " + ex);
		}
	}
	
	public static JsonElement executeGet(String sql) throws Exception {
		JsonElement e = executeList(sql);
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		if (arr.size() == 0) {
			throw new Exception("The requested resource could not be found.");
		}
		return arr.get(0);
	}
	
	public static JsonElement executeInsert(Class<?> cs, String sql) throws Exception {
		String nameOrId = Translate.getNameOrId(cs);
		Connection c = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			rs = stmt.executeQuery(sql);
			String rName = "";
			while (rs.next()) { String name = rs.getString(nameOrId); rName += name; }
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put(nameOrId, rName);
			if (rs != null) { try { rs.close(); } catch (Exception e) { } }
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			return new Gson().fromJson(ZConnector.ConvertToJsonString(m1), JsonElement.class);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (rs != null) { try { rs.close(); } catch (Exception e) { } }
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			throw new Exception("Error executeInsert, " + ex);
		}
	}
	
	public static JsonElement executeUpdate(String sql, String tableName, String valueNameOrId) throws Exception {
		Connection c = null;
		Statement stmt = null;
		try {
			
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			int updated = stmt.executeUpdate(sql);
			if (updated == 0) {
				throw new Exception(tableName + " \"" + valueNameOrId + "\" does not exist.");
			}

			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("updated", updated);

			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			return new Gson().fromJson(ZConnector.ConvertToJsonString(m1), JsonElement.class);
			
		} catch (Exception ex) {
			ex.printStackTrace();
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			throw new Exception("Error executeUpdate, " + ex);
		}
	}
	
	public static JsonElement executeDelete(String sql, String tableName, String valueNameOrId) throws Exception {
		Connection c = null;
		Statement stmt = null;
		try {
			
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			int deleted = stmt.executeUpdate(sql);
			if (deleted == 0) {
				throw new Exception(tableName + " \"" + valueNameOrId + "\" does not exist.");
			}

			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("deleted", deleted);

			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			return new Gson().fromJson(ZConnector.ConvertToJsonString(m1), JsonElement.class);
			
		} catch (Exception ex) {
			ex.printStackTrace();
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			throw new Exception("Error executeDelete, " + ex);
		}
	}
	
	public static JsonElement executeDeleteAll(String tableName) throws Exception {
		Connection c = null;
		Statement stmt = null;
		try {
			
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			
			String sql = "delete from " + Constant.SCHEMA + ".\"" + tableName + "\";";
			
			System.out.println(sql);
			int deleted = stmt.executeUpdate(sql);

			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("deleted", deleted);

			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			return new Gson().fromJson(ZConnector.ConvertToJsonString(m1), JsonElement.class);
			
		} catch (Exception ex) {
			ex.printStackTrace();
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
			if (c != null) { try { c.close(); } catch (Exception e) { } }
			throw new Exception("Error executeDelete, " + ex);
		}
	}
	
	public static ArrayList<String> listAllFields() throws Exception {

		JsonElement e = executeList("select distinct column_name from information_schema.columns where table_schema = '" + ZConnector.Constant.SCHEMA + "' order by column_name;");
		
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		
		ArrayList<String> ret = new ArrayList<String>();
		for (JsonElement ei : arr) {
			ret.add(ei.getAsJsonObject().get("column_name").getAsString());
		}
		
		return ret;
	}
	
	public static ArrayList<String> listAllTables() throws Exception {
		//select table_name FROM information_schema.tables WHERE table_schema = 'public' order by table_name

		JsonElement e = executeList("select table_name from information_schema.tables where table_schema = '" + ZConnector.Constant.SCHEMA + "' order by table_name;");
		
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		
		ArrayList<String> ret = new ArrayList<String>();
		for (JsonElement ei : arr) {
			ret.add(ei.getAsJsonObject().get("table_name").getAsString());
		}
		
		return ret;
	}

	public static HashMap<String, Object> mapCreate(Class<?> c, JsonObject json) throws Exception {
		if (json == null) {
			throw new Exception("Unable to parse null JSON in " + c.getSimpleName() + ".mapCreate().");
		}
		
		
		
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		HashMap<String, Object> m = new HashMap<String, Object>();
		ArrayList<Field> fields = OU.getFields(c);
		for (Field f : fields) {
			if (f.getModifiers() == 1) {
				String fieldName = f.getName();
				
				if (json.has(fieldName)) {
					m.put(fieldName, Translate.getValueFromJson(json, f.getType(), fieldName));
				}
			}
		}
		m.put("created", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(now));
		m.put("modified", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(now));
		
		if (m.containsKey("id")) { m.remove("id"); } // use auto-increment???
		
		return m;
	}
	
	public static HashMap<String, Object> mapUpdate(Class<?> c, JsonObject json) throws Exception {
		HashMap<String, Object> m = mapCreate(c, json);
		if (m.containsKey("id")) { m.remove("id"); } // TODO: ???
		if (m.containsKey("name")) { m.remove("name"); } // TODO: ???
		if (m.containsKey("created")) { m.remove("created"); } // TODO: ???
		if (m.containsKey(Workspace.class.getSimpleName().toLowerCase())) { m.remove("workspace"); } // TODO: ???
		return m;
	}
	
	public static String sqlCreate(Class<?> c, String body) throws Exception {
		JsonObject json = new Gson().fromJson(body, JsonObject.class);
		HashMap<String, Object> m1 = mapCreate(c, json);
		if (m1.containsKey(Workspace.class.getSimpleName())) { m1.remove(Workspace.class.getSimpleName()); } // TODO: ???
		return "insert into " + Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" (" + concateStmtKeys(m1) + ") values (" + concateStmtValues(m1) + ") returning " + Translate.getNameOrId(c) + ";";
	}
	
	public static String sqlCreateInWorkspace(Class<?> c, String workspace, String body) throws Exception {
		JsonObject json = new Gson().fromJson(body, JsonObject.class);
		HashMap<String, Object> m1 = mapCreate(c, json);
		m1.put(Workspace.class.getSimpleName().toLowerCase(), workspace); // TODO: ???
		return "insert into " + Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" (" + concateStmtKeys(m1) + ") values (" + concateStmtValues(m1) + ") returning " + Translate.getNameOrId(c) + ";";
	}
	
	public static String sqlUpdate(Class<?> c, JsonObject json, Object valueNameOrId) throws Exception {
		HashMap<String, Object> m1 = mapUpdate(c, json);
		return "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set " + concateStmtKeysValues(m1) + " where \"" + Translate.getNameOrId(c) + "\" = '" + valueNameOrId + "';";
	}
	
	public static String sqlUpdateInWorkspace(Class<?> c, JsonObject json, Object valueNameOrId, Object workspaceNameOrId) throws Exception {
		HashMap<String, Object> m1 = mapUpdate(c, json);
		return "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set " + concateStmtKeysValues(m1) + " where \"" + Translate.getNameOrId(c) + "\" = '" + valueNameOrId + "' and \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspaceNameOrId + "';";
	}
	
	public static String sqlList(Class<?> c) {
		return "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\";";
	}
	
	public static String sqlListInWorkspace(Class<?> c, Object workspaceNameOrId) {
		return "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspaceNameOrId + "';";
	}
	
	public static String sqlGetInWorkspace(Class<?> c, Object objectNameOrId, Object workspaceNameOrId) throws Exception {
		return "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspaceNameOrId + "' and \"" + Translate.getNameOrId(c) + "\" = '" + objectNameOrId + "';";
	}
	
	public static String sqlGet(Class<?> c, Object valueNameOrId) throws Exception {
		return "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Translate.getNameOrId(c) + "\" = '" + valueNameOrId + "';";
	}
	
	public static String sqlDelete(Class<?> c, Object valueNameOrId) throws Exception {
		return "delete from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Translate.getNameOrId(c) + "\" = '" + valueNameOrId + "';";
	}
	
	public static String sqlDeleteInWorkspace(Class<?> c, Object valueNameOrId, Object workspaceNameOrId) throws Exception {
		return "delete from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Translate.getNameOrId(c) + "\" = '" + valueNameOrId + "' and " + Workspace.class.getSimpleName().toLowerCase() + " = '" + workspaceNameOrId + "';";
	}
	
	public static Object[] listFromDB(Class<?> css) throws Exception {
		String sql = sqlList(css);
		JsonElement e = executeList(sql);
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		Object[] objs = new Object[arr.size()];
		int n = 0;
		for (JsonElement ei : arr) {
			JsonObject o = ei.getAsJsonObject();
			objs[n] = parse(css, o);
			n++;
		}
		return objs;
	}
	
	public static JsonElement addToDB(Object o) throws IllegalArgumentException, IllegalAccessException, Exception {
		Class<?> css = o.getClass();
		String sql = sqlCreate(css, toJsonString(o));
		return executeInsert(css, sql);
	}
	
	public static JsonElement addToDB(Class<?> css, String json) throws IllegalArgumentException, IllegalAccessException, Exception {
		String sql = sqlCreate(css, json);
		return executeInsert(css, sql);
	}
	
	public static JsonElement addToDB(Class<?> css, JsonElement json) throws IllegalArgumentException, IllegalAccessException, Exception {
		String sql = sqlCreate(css, json.toString());
		return executeInsert(css, sql);
	}
	
	public static Object getFromDB(Class<?> css, Object valueNameOrId) throws Exception {
		String sql = sqlGet(css, valueNameOrId);
		JsonElement e = executeGet(sql);
		return parse(css, e.getAsJsonObject());
	}
	
	public static Object parse(Class<?> css, JsonObject json) throws NoSuchFieldException, Exception {
		Object obj = css.getDeclaredConstructor().newInstance();
		ArrayList<Field> fs = OU.getFields(css);
		for (Field f : fs) { css.getDeclaredField(f.getName()).set(obj, Translate.getValueFromJson(json, f.getType(), f.getName())); }
		return obj;
	}
	
	public static String toJsonString(Object o) throws IllegalArgumentException, IllegalAccessException {
		
		if (o.getClass().toString().toLowerCase().startsWith("class [Ljava.lang.Object".toLowerCase())) {
			Object[] objs = (Object[])o;
			JsonArray arr = new JsonArray();
			for (Object obj : objs) {
				arr.add(new Gson().fromJson(toJsonString(obj), JsonElement.class));
			}
			return "{\"list\": " + arr + "}";
		} else {
			
			Class<?> c = o.getClass();
			ArrayList<Field> fs = OU.getFields(c);
			HashMap<String, Object> m = new HashMap<String, Object>();
			for (Field f : fs) {
				
				String name = f.getName();
				Object value = f.get(o);
				
				String htime = null;
				
				if (value != null) {
					if ((value.getClass() + "").equals("class java.sql.Timestamp")) {
						htime = new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(value);
					}
				}
				
				if (htime == null) {
					m.put(name, value);
				} else {
					m.put(name, htime);
				}
			}
			return ZConnector.ConvertToJsonString(m);
		}
	}

	public static void main(String[] args) throws Exception {
		
	}
}