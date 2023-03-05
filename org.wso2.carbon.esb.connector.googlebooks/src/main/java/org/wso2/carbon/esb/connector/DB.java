package org.wso2.carbon.esb.connector;

import java.lang.reflect.Field;
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

import org.wso2.carbon.esb.connector.ZConnector.Constant;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DB {
	
	public static String getAsString(JsonObject o, String fieldName) {
		try {
			JsonElement e2 = o.get(fieldName);
			if (e2 == null) {
				return null;
			} else {
				return e2.getAsString();
			}
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static int getAsInt(JsonObject o, String fieldName, int defaultValue) {
		try {
			JsonElement e2 = o.get(fieldName);
			if (e2 == null) {
				return defaultValue;
			} else {
				return e2.getAsInt();
			}
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	public static long getAsLong(JsonObject o, String fieldName, int defaultValue) {
		try {
			JsonElement e2 = o.get(fieldName);
			if (e2 == null) {
				return defaultValue;
			} else {
				return e2.getAsLong();
			}
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	public static Timestamp getAsTime(JsonObject o, String fieldName) {
		try {
			JsonElement e2 = o.get(fieldName);
			if (e2 == null) {
				return null;
			} else {
				return java.sql.Timestamp.valueOf(e2.getAsString());
			}
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static String translate(String fieldName, String[] goodCamelCase) throws Exception {
		for (String s : goodCamelCase) {
			if (fieldName.equalsIgnoreCase(s)) { return s; }
		}
		throw new Exception("Unrecognized field: \"" + fieldName + "\".");
	}
	
	public static void setObjectField(Object obj, String fieldName, Object fieldValue) {
		try {
			Field field;
			field = obj.getClass().getDeclaredField(fieldName);
			Class<?> superClass = obj.getClass().getSuperclass();
		    while (field == null && superClass != null) {
		        try {
		            field = superClass.getDeclaredField(fieldName);
		        } catch (NoSuchFieldException e) {
		            superClass = superClass.getSuperclass();
		        }
		    }
		    field.setAccessible(true);
		    field.set(obj, fieldValue);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static HashMap<String, Object> removeSomeFields(HashMap<String, Object> m1, String[] editableFields) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (m1 != null) {
			Set<String> ks = m1.keySet();
			for (String k : ks) {
				for (String f : editableFields) {
					if (k.equalsIgnoreCase(f)) { map.put(k, m1.get(k)); }
				}
			}
		}
		return map;
	}
	
	public static String constructMessageList(String tableName) {
		String ret = "select * from " + Constant.SCHEMA + "." + tableName;
		return ret;
	}
	
	public static String constructMessageGet(String tableName, String nameOrId, Object objNameOrId) {
		String ret = "select * from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "'";
		return ret;
	}
	
	public static String constructMessageCreate(HashMap<String, Object> m1, String tableName, String nameOrId) {
		String ret = "insert into " + Constant.SCHEMA + "." + tableName + " (" + concateStmtKeys(m1) + ") values (" + concateStmtValues(m1) + ") returning " + nameOrId + ";";
		return ret;
	}
	
	public static String constructMessageUpdate(Object objNameOrId, HashMap<String, Object> m1, String[] editableFields, String tableName, String nameOrId) throws Exception {
		HashMap<String, Object> m2 = removeSomeFields(m1, editableFields);
		if (m2.size() < 1) {
			throw new Exception("No fields to update.");
		}
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		m2.put("modified", now);
		String ret =  "update " + Constant.SCHEMA + "." + tableName + " SET " + concateStmtKeysValues(m2) + " WHERE " + nameOrId + " = '" + objNameOrId + "';";
		System.out.println(ret);
		return ret;
	}
	
	public static String constructMessageDelete(Object objNameOrId, String tableName, String nameOrId) {
		String ret = "delete from " + Constant.SCHEMA + "." + tableName + " where " + nameOrId + " = '" + objNameOrId + "'";
		System.out.println(ret);
		return ret;
	}
	
	public static String concateStmtKeysValues(HashMap<String, Object> m1) {
		String tKeys = "";
		Set<String> ks = m1.keySet();
		int n = 0;
		int size = ks.size();
		for (String k : ks) {
			tKeys += k + " = '" + m1.get(k) + "'";
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
			tKeys += k;
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
				tKeys += "" + value + "";
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
	
	public static ZConnector.ZResult getJsonList(String tableName, String[] goodCamelCase, String sql) {
		ZConnector.ZResult ret = ZConnector.ZResult.ERROR_500(new Exception("Unexpected error."));
		Connection c = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			rs = stmt.executeQuery(sql);
			ArrayList<Object> ls1 = new ArrayList<Object>();
			ResultSetMetaData rsmd = rs.getMetaData();
			while (rs.next()) {
				Map<String, Object> m1 = new HashMap<String, Object>();
				int numColumns = rsmd.getColumnCount();
				for (int i = 1; i <= numColumns; i++) {
					String fieldName = rsmd.getColumnName(i);
					Object fieldValue = rs.getObject(fieldName);
					fieldName = translate(fieldName, goodCamelCase);
					if (fieldValue != null) { if (fieldValue.getClass().getName().equalsIgnoreCase("java.sql.Timestamp")) { fieldValue = new SimpleDateFormat(Constant.DATEFORMAT).format((java.sql.Timestamp)(fieldValue)); } }
					m1.put(fieldName, fieldValue);
				}
				ls1.add(m1);
			}
			Map<String, Object> m2 = new HashMap<String, Object>();
			m2.put("list", ls1);
			ret = ZConnector.ZResult.OK_200(ZConnector.ConvertToJsonString(m2));
		} catch (Exception e) {
			e.printStackTrace();
			ret = ZConnector.ZResult.ERROR_500(e);
		}
		try { rs.close(); } catch (Exception ex) { }
		try { stmt.close(); } catch (Exception ex) { }
		try { c.close(); } catch (Exception ex) { }
		return ret;
	}
	
	public static ZConnector.ZResult getJsonGet(String tableName, Object objNameOrId, String[] goodCamelCase, String nameOrId, String sql) {
		ZConnector.ZResult ret = ZConnector.ZResult.ERROR_500(new Exception("Unexpected error."));
		Connection c = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			rs = stmt.executeQuery(sql);
			ArrayList<Object> ls1 = new ArrayList<Object>();
			ResultSetMetaData rsmd = rs.getMetaData();
			while (rs.next()) {
				Map<String, Object> m1 = new HashMap<String, Object>();
				int numColumns = rsmd.getColumnCount();
				for (int i = 1; i <= numColumns; i++) {
					String fieldName = rsmd.getColumnName(i);
					Object fieldValue = rs.getObject(fieldName);
					fieldName = translate(fieldName, goodCamelCase);
					if (fieldValue != null) { if (fieldValue.getClass().getName().equalsIgnoreCase("java.sql.Timestamp")) { fieldValue = new SimpleDateFormat(Constant.DATEFORMAT).format((java.sql.Timestamp)(fieldValue)); } }
					m1.put(fieldName, fieldValue);
				}
				ls1.add(m1);
			}
			ret = ZConnector.ZResult.OK_200(ZConnector.ConvertToJsonString(ls1.get(0)));
		} catch (Exception e) {
			e.printStackTrace();
			ret = ZConnector.ZResult.ERROR_500(e);
		}
		try { rs.close(); } catch (Exception ex) { }
		try { stmt.close(); } catch (Exception ex) { }
		try { c.close(); } catch (Exception ex) { }
		return ret;
	}
	
	public static ZConnector.ZResult getJsonCreate(String sql, String nameOrId) {
		ZConnector.ZResult ret = ZConnector.ZResult.ERROR_500(new Exception("Unexpected error."));
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
			ret = ZConnector.ZResult.OK_200(ZConnector.ConvertToJsonString(m1));
		} catch (Exception e) {
			e.printStackTrace();
			ret = ZConnector.ZResult.ERROR_500(e);
		}
		try { rs.close(); } catch (Exception ex) { }
		try { stmt.close(); } catch (Exception ex) { }
		try { c.close(); } catch (Exception ex) { }
		return ret;
	}
	
	public static ZConnector.ZResult getJsonUpdate(String tableName, Object objNameOrId, String sql, String nameOrId) {
		ZConnector.ZResult ret = ZConnector.ZResult.ERROR_500(new Exception("Unexpected error."));
		Connection c = null;
		Statement stmt = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			int updated = stmt.executeUpdate(sql);
			if (updated == 0) {
				ret = new ZResult();
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put("error", tableName + " \"" + objNameOrId + "\" does not exist.");
				ret.content = ZConnector.ConvertToJsonString(m1);
				ret.statusCode = 404;
			} else {
				ret = new ZResult();
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put("updated", updated);
				ret.content = ZConnector.ConvertToJsonString(m1);
				ret.statusCode = 200;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = ZConnector.ZResult.ERROR_500(e);
		}
		try { stmt.close(); } catch (Exception ex) { }
		try { c.close(); } catch (Exception ex) { }
		return ret;
	}
	
	public static ZConnector.ZResult getJsonDelete(String tableName, Object objNameOrId, String sql) {
		ZConnector.ZResult ret = ZConnector.ZResult.ERROR_500(new Exception("Unexpected error."));
		Connection c = null;
		Statement stmt = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			System.out.println(sql);
			int deleted = stmt.executeUpdate(sql);
			if (deleted == 0) {
				ret = new ZResult();
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put("error", tableName + " \"" + objNameOrId + "\" does not exist.");
				ret.content = ZConnector.ConvertToJsonString(m1);
				ret.statusCode = 404;
			} else {
				ret = new ZResult();
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put("deleted", deleted);
				ret.content = ZConnector.ConvertToJsonString(m1);
				ret.statusCode = 200;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = ZConnector.ZResult.ERROR_500(e);
		}
		try { stmt.close(); } catch (Exception ex) { }
		try { c.close(); } catch (Exception ex) { }
		return ret;
	}
}