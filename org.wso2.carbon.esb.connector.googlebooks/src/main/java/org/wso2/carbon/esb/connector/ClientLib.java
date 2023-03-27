package org.wso2.carbon.esb.connector;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.Item.Status;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class ClientLib {
	

	public static void setItemStatus(Item item, Item.Status status) {
		if (item != null) {
			try {
				String patch = "{\"status\": \"" + status.toString() + "\"}"; // TODO
				JsonElement e = new Gson().fromJson(patch, JsonElement.class);
				ClientLib.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/patch-status", "post", null, e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void updateOnExecuteFailed(Item item, Status status, int retryRemaining, Timestamp timeNextRetry, Timestamp timeLatestRetry) throws Exception {
		HashMap<String, Object> m = new HashMap<String, Object>(); // TODO
		m.put("status", status);
		m.put("retryRemaining", retryRemaining);
		m.put("timeNextRetry", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(timeNextRetry));
		m.put("timeLatestRetry", new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(timeLatestRetry));
		JsonElement e = new Gson().fromJson(ZConnector.ConvertToJsonString(m), JsonElement.class);
		ClientLib.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/patch-status", "post", null, e);
	}
	
	public static void setTaskStatus(Task task, Task.Status status) throws Exception {
		String patch = "{\"status\": \"" + status.toString() + "\"}"; // TODO
		JsonElement e = new Gson().fromJson(patch, JsonElement.class);
		ClientLib.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + task.workspace + "/tasks/" + task.id + "/patch-status", "post", null, e);
	}
	
	public static void addItemLog(Item item, Log.Type type, String title, String body) {
		if (item != null) {
			try {
				HashMap<String, Object> m = new HashMap<String, Object>(); // TODO
				m.put("logType", type.toString());
				m.put("title", title);
				m.put("body", body);
				JsonElement e = new Gson().fromJson(ZConnector.ConvertToJsonString(m), JsonElement.class);
				ClientLib.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + item.workspace + "/items/" + item.name + "/logs", "post", null, e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static JsonElement getJsonResponse(String url0, String method, HashMap<String, String> headers, JsonElement jsonRequest) throws Exception {
		return com.pttdigital.wtransfer.Client.getJsonResponse(url0, method, headers, jsonRequest);
		
	}

	public static void updateScheduleCheckpoint(Schedule schedule, Timestamp time) throws JsonSyntaxException, Exception {
		ClientLib.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + schedule.workspace + "/schedules/" + schedule.name + "/patch-checkpoint", "post", null, new Gson().fromJson("{\"previousCheckpoint\": \"" + time + "\"}", JsonElement.class));
	}
}