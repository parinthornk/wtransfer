package com.pttdigital.wtransfer;

import java.util.HashMap;
import java.util.Set;

import org.wso2.carbon.esb.connector.ZConnector;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pttdigital.wtransfer.ImportV2.OL;

public class GenerateClassFromSchema {
	public static void create() throws Exception {
		
		JsonElement e = DB.executeList("select * from information_schema.columns where table_schema = '" + ZConnector.Constant.SCHEMA + "' order by table_name, ordinal_position");
		
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		
		String class0 = "";
		
		for (JsonElement ei : arr) {
			
			JsonObject o = ei.getAsJsonObject();
			String table_name = o.get("table_name").getAsString();
			String column_name = o.get("column_name").getAsString();
			String data_type = o.get("data_type").getAsString();
			
			if (!class0.equals(table_name)) {
				if (class0.length() > 0) {

					System.out.println("}");
					System.out.println();
				}
				class0 = table_name;
				System.out.println("public class " + table_name + " {");
			}

			String dt = "";
			if (data_type.equals("text")) {
				dt = "String";
			} else if (data_type.equals("integer")) {
				dt = "int";
			} else if (data_type.equals("bigint")) {
				dt = "long";
			} else if (data_type.equals("boolean")) {
				dt = "boolean";
			} else if (data_type.equals("timestamp without time zone")) {
				dt = "Timestamp";
			} else {
				throw new Exception("Unsupported data_type \"" + data_type + "\".");
			}
			
			System.out.println("\t" + "public " + dt + " " + column_name + ";");
		}
		System.out.println("}");
		System.out.println();
	}
	
	public static JsonObject exampleObjects() throws Exception {

		JsonArray arr = DB.executeList("select table_name, column_name, data_type from information_schema.columns where table_schema = '" + ZConnector.Constant.SCHEMA + "' order by table_name, ordinal_position").getAsJsonObject().get("list").getAsJsonArray();
		
		HashMap<String, JsonObject> m = new HashMap<String, JsonObject>();
		
		for (JsonElement ei : arr) {
			JsonObject o = ei.getAsJsonObject();
			String table_name = o.get("table_name").getAsString();
			String column_name = o.get("column_name").getAsString();
			String data_type = o.get("data_type").getAsString();
			
			if (!m.containsKey(table_name)) {
				m.put(table_name, new JsonObject());
			}

			if (data_type.equals("text")) {
				m.get(table_name).addProperty(column_name, "string");
			} else if (data_type.equals("integer")) {
				m.get(table_name).addProperty(column_name, 0);
			} else if (data_type.equals("bigint")) {
				m.get(table_name).addProperty(column_name, 0);
			} else if (data_type.equals("boolean")) {
				m.get(table_name).addProperty(column_name, false);
			} else if (data_type.equals("timestamp without time zone")) {
				m.get(table_name).addProperty(column_name, "1970-01-01 00:00:00");
			} else {
				throw new Exception("Unsupported data_type \"" + data_type + "\".");
			}
		}
		
		JsonArray arr2 = new JsonArray();
		Set<String> ks = m.keySet();
		for (String table_name : ks) {
			String tableName = table_name;
			JsonObject o = new JsonObject();
			o.addProperty("name", tableName);
			o.add("data", new Gson().fromJson(ZConnector.ConvertToJsonString(m.get(table_name)), JsonObject.class));
			arr2.add(o);
		}
		
		JsonObject o = new JsonObject();
		o.addProperty("count", arr2.size());
		o.add("tables", arr2);
		return o;
	}
	
	public static void main(String[] args) throws Exception {
		create();
	}
}