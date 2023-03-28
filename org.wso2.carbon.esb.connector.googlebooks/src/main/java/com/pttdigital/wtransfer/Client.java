package com.pttdigital.wtransfer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.wso2.carbon.esb.connector.ZConnector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pttdigital.wtransfer.Item.Status;
import com.pttdigital.wtransfer.Log.Type;

public class Client {
	
	private static ArrayList<String> al_methods = new ArrayList<String>();
	
	public static JsonElement getJsonResponse(String url0, String method, HashMap<String, String> headers, JsonElement jsonRequest) throws Exception {
		
		if (al_methods.size() == 0) {
			al_methods.add("get");
			al_methods.add("post");
			al_methods.add("put");
			al_methods.add("patch");
			al_methods.add("delete");
			al_methods.add("options");
		}
		
		if (method == null) {
			throw new Exception("Unable to construct a HTTP request with method = \"null\".");
		}
		
		if (!al_methods.contains(method.toLowerCase())) {
			throw new Exception("Unable to construct a HTTP request with method = \"" + method + "\", the method is not yet implemented.");
		}
		
		JsonElement jsonResponse = null;
		HttpURLConnection conn = null;
        BufferedReader reader = null;
        OutputStream os = null;
        
        try {
        	
        	String url = url0.replace(" ", "%20"); // TODO: ??????????
        	
            conn = (HttpURLConnection)((new java.net.URL(url)).openConnection());
    		conn.setDoOutput(true);
    		conn.setRequestMethod(method.toUpperCase());

    		HashMap<String, String> hout = new HashMap<String, String>();
    		hout.put("Content-Type", "application/json");
    		if (headers != null) {
    			Set<String> keys = headers.keySet();
    			for (String key : keys) {
    				hout.put(key, headers.get(key));
    			}
    		}
    		Set<String> keys = hout.keySet();
    		for (String key : keys) {
    			conn.setRequestProperty(key, hout.get(key));
    		}
    		
    		if (jsonRequest != null) {
                os = conn.getOutputStream();
                os.write(jsonRequest.toString().getBytes());
                os.flush();
    		}
    		
            int responseCode = conn.getResponseCode();
            if (100 < responseCode && responseCode < 300) {
    			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    			jsonResponse = new Gson().fromJson (reader.lines().collect(Collectors.joining()), JsonElement.class);
            } else {
    			reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = reader.lines().collect(Collectors.joining());
                throw new Exception("Received HTTP error: " + responseCode + ", " + responseBody);
            }
        } catch (Exception ex) {
        	
    		try { reader.close(); } catch (Exception e1) { }
    		try { os.close(); } catch (Exception e1) { }
    		try { conn.disconnect(); } catch (Exception e1) { }
        	throw new Exception("Error calling endpoint: url[" + url0 + "], method[" + method + "]: " + ex);
        }
        
		try { reader.close(); } catch (Exception ex) { }
		try { os.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }

		return jsonResponse;
	}

	public static void setItemStatus(Item item, Status status) {
		try {
			JsonObject o = new JsonObject();
			o.addProperty(Item.wordStatus, status.toString().toUpperCase());
			getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/sessions/" + item.session + "/items/" + item.name + "/patch", "post", null, o);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addItemLog(Item item, Log.Type t, String title, String body) {
		try {
			LogItem log = new LogItem();
			log.id = 0;
			log.item = item.name;
			log.logType = t.toString().toUpperCase();
			log.title = title;
			log.body = body;
			getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/logs", "post", null, new Gson().fromJson(DB.toJsonString(log), JsonObject.class));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateOnExecuteFailed(Item item, Status status, int retryRemaining, Timestamp timeNextRetry, Timestamp timeLatestRetry) {
		// TODO Auto-generated method stub
		
	}
}
