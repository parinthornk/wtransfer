package org.wso2.carbon.esb.connector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.wso2.carbon.esb.connector.Item.Status;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class ClientLib {
	public static JsonElement getJsonResponse(String url, String method, HashMap<String, String> headers, JsonElement jsonRequest) throws Exception {
		JsonElement jsonResponse = null;
		HttpURLConnection conn = null;
        BufferedReader reader = null;
        OutputStream os = null;
        
        try {
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
        	throw ex;
        }
        
		try { reader.close(); } catch (Exception ex) { }
		try { os.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }

		return jsonResponse;
	}

	public static void setItemStatus(Item item, Item.Status status) {
		if (item != null) {
			try {
				String patch = "{\"status\": \"" + status.toString() + "\"}"; // TODO
				JsonElement e = new Gson().fromJson(patch, JsonElement.class);
				ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/patch-status", "post", null, e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void updateOnExecuteFailed(Item item, Status status, int retryRemaining, Timestamp timeNextRetry, Timestamp timeLatestRetry) throws Exception {
		HashMap<String, Object> m = new HashMap<String, Object>(); // TODO
		m.put("status", status);
		m.put("retryRemaining", retryRemaining);
		m.put("timeNextRetry", timeNextRetry);
		m.put("timeLatestRetry", timeLatestRetry);
		JsonElement e = new Gson().fromJson(ZConnector.ConvertToJsonString(m), JsonElement.class);
		ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/patch-status", "post", null, e);
	}
	
	public static void setTaskStatus(Task task, Task.Status status) throws Exception {
		String patch = "{\"status\": \"" + status.toString() + "\"}"; // TODO
		JsonElement e = new Gson().fromJson(patch, JsonElement.class);
		ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + task.workspace + "/tasks/" + task.id + "/patch-status", "post", null, e);
	}
	
	public static void addItemLog(Item item, Log.Type type, String title, String body) {
		if (item != null) {
			try {
				HashMap<String, Object> m = new HashMap<String, Object>(); // TODO
				m.put("logType", type.toString());
				m.put("title", title);
				m.put("body", body);
				JsonElement e = new Gson().fromJson(ZConnector.ConvertToJsonString(m), JsonElement.class);
				ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/logs", "post", null, e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void updateScheduleCheckpoint(Schedule schedule, Timestamp time) throws JsonSyntaxException, Exception {
		ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + schedule.workspace + "/schedules/" + schedule.name + "/patch-checkpoint", "post", null, new Gson().fromJson("{\"previousCheckpoint\": \"" + time + "\"}", JsonElement.class));
	}
}