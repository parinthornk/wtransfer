package org.wso2.carbon.esb.connector;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
public class ZAPIV2 {
	
	private static int getInt(JsonElement e, String fieldName, int defVal) {
		try {
			return Integer.parseInt(e.getAsJsonObject().get(fieldName).getAsString());
		} catch (Exception ex) {
			return defVal;
		}
	}
	
	private static String getFromJsonBodyAsString(byte[] raw, String fieldName) {
		try {
			String jsonBody = new String(raw, StandardCharsets.UTF_8);
			JsonParser parser = new JsonParser();
			JsonObject neoJsonElement = (JsonObject) parser.parse(jsonBody);
			return neoJsonElement.get(fieldName).getAsString();
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static boolean getFromJsonBodyAsBoolean(byte[] raw, String fieldName, boolean defaultValue) {
		try {
			String text = getFromJsonBodyAsString(raw, fieldName);
			return text.equalsIgnoreCase("true") || text.equalsIgnoreCase("1");			
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static int getFromJsonBodyAsInteger(byte[] raw, String fieldName, int defaultValue) {
		try {
			return Integer.parseInt(getFromJsonBodyAsString(raw, fieldName));
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static java.sql.Timestamp getFromJsonBodyAsTimestamp(byte[] raw, String fieldName) {
		try {
			return java.sql.Timestamp.valueOf(getFromJsonBodyAsString(raw, fieldName));
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static long getFromJsonBodyAsLong(byte[] raw, String fieldName, long defaultValue) {
		try {
			return Long.parseLong(getFromJsonBodyAsString(raw, fieldName));
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static String getFromPathParamsAsString(String path, int position) {
		try {
			
			String val = path.split("/")[1 + position];
			try {
				val = java.net.URLDecoder.decode(val, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
			    // not going to happen - value came from JDK's own StandardCharsets
			}
			return val;
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static int getFromPathParamsAsInteger(String path, int position, int defaultValue) {
		try {
			return Integer.parseInt(path.split("/")[1 + position]);
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static long getFromPathParamsAsLong(String path, int position, int defaultValue) {
		try {
			return Long.parseLong(path.split("/")[1 + position]);
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static boolean matchPath(String requested, String template) {
		String[] splitRequested = requested.split("/");
		String[] splitTemplate = template.split("/");
		if (splitRequested.length != splitTemplate.length) {
			return false;
		}
		for (int i=0;i<splitRequested.length;i++) {
			if (!splitTemplate[i].equalsIgnoreCase("*")) {
				if (!splitTemplate[i].equalsIgnoreCase(splitRequested[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean match(String path, String method, String target) {
		String[] sept = target.split(", ");
		String targetPath = sept[0];
		String targetMethod = sept[1];
		if (!matchPath(path, targetPath)) {
			return false;
		}
		if (!method.equalsIgnoreCase(targetMethod)) {
			return false;
		}
		return true;
	}
	
	public static ZConnector.ZResult process(String path, String method, HashMap<String, String> query, HashMap<String, String> header, byte[] bodyRaw) {
		try {
			// TODO: ----------------------------------------------------------------------------------------------------> Workspace
			if (match(path, method, "/workspaces, get")) {
				return Workspace.list();
			}
			if (match(path, method, "/workspaces, post")) {
				return Workspace.create(Workspace.parse(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*, get")) {
				String name = getFromPathParamsAsString(path, 1);
				return Workspace.get(name);
			}
			if (match(path, method, "/workspaces/*, patch")) {
				String name = getFromPathParamsAsString(path, 1);
				return Workspace.update(name, Workspace.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*, delete")) {
				String name = getFromPathParamsAsString(path, 1);
				return Workspace.delete(name);
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Site
			if (match(path, method, "/workspaces/*/sites, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return Site.list(workspace);
			}
			if (match(path, method, "/workspaces/*/sites, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return Site.create(workspace, Site.parse(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/sites/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Site.get(workspace, name);
			}
			if (match(path, method, "/workspaces/*/sites/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Site.update(workspace, name, Site.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/sites/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Site.delete(workspace, name);
			}
			//if (match(path, method, "/workspaces/*/sites/*/items, get")) {
			//	String workspace = getFromPathParamsAsString(path, 1);
			//	String name = getFromPathParamsAsString(path, 3);
			//	return ZWorker.readSiteStatus(workspace, name);
			//}
			if (match(path, method, "/workspaces/*/sites/*/objects, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String siteName = getFromPathParamsAsString(path, 3);
				return ZWorker.listObject(workspace, siteName);
			}
			if (match(path, method, "/sites, get")) {
				return Site.list();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> PGP
			if (match(path, method, "/workspaces/*/pgps, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return PGP.list(workspace);
			}
			if (match(path, method, "/workspaces/*/pgps, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return PGP.create(workspace, PGP.parse(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/pgps/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return PGP.get(workspace, name);
			}
			if (match(path, method, "/workspaces/*/pgps/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return PGP.update(workspace, name, PGP.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/pgps/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return PGP.delete(workspace, name);
			}
			if (match(path, method, "/pgps, get")) {
				return PGP.list();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Config
			if (match(path, method, "/workspaces/*/configs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return Config.list(workspace);
			}
			if (match(path, method, "/workspaces/*/configs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return Config.create(workspace, Config.parse(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/configs/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Config.get(workspace, name);
			}
			if (match(path, method, "/workspaces/*/configs/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Config.update(workspace, name, Config.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/configs/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Config.delete(workspace, name);
			}
			if (match(path, method, "/configs, get")) {
				return Config.list();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Schedule
			if (match(path, method, "/schedules, get")) {
				
				ZConnector.ZResult result = Schedule.listAllEnabled();
				
				// plan JSON parse
				String fieldPlan = "plan";
				JsonElement e = new Gson().fromJson(result.content, JsonElement.class);
				JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
				for (JsonElement je : arr) {
					JsonObject o = je.getAsJsonObject();
					String planText = o.get(fieldPlan).getAsString();
					o.remove(fieldPlan);
					o.add(fieldPlan, new Gson().fromJson(planText, JsonElement.class));
				}
				result.content = e.toString();
				
				return result;
			}
			if (match(path, method, "/workspaces/*/schedules, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				
				ZConnector.ZResult result = Schedule.list(workspace);
				
				// plan JSON parse
				String fieldPlan = "plan";
				JsonElement e = new Gson().fromJson(result.content, JsonElement.class);
				JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
				for (JsonElement je : arr) {
					JsonObject o = je.getAsJsonObject();
					String planText = o.get(fieldPlan).getAsString();
					o.remove(fieldPlan);
					o.add(fieldPlan, new Gson().fromJson(planText, JsonElement.class));
				}
				result.content = e.toString();
				
				return result;
			}
			if (match(path, method, "/workspaces/*/schedules, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				
				// plan stringify
				String fieldPlan = "plan";
				String body = new String(bodyRaw);
				JsonElement e = new Gson().fromJson(body, JsonElement.class);
				JsonObject o = e.getAsJsonObject();
				if (!o.has(fieldPlan)) {
					throw new Exception("Missing field \"plan\".");
				}
				JsonObject plan = o.get(fieldPlan).getAsJsonObject();
				ValidateSchedule.validateV2(plan);
				o.remove(fieldPlan);
				o.addProperty(fieldPlan, plan.toString());
				body = e.toString();
				
				return Schedule.create(workspace, Schedule.parse(body));
			}
			if (match(path, method, "/workspaces/*/schedules/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				
				String fieldPlan = "plan";
				ZConnector.ZResult result = Schedule.get(workspace, name);
				JsonElement e = new Gson().fromJson(result.content, JsonElement.class);
				JsonObject o = e.getAsJsonObject();
				String planText = o.get(fieldPlan).getAsString();
				o.remove(fieldPlan);
				o.add(fieldPlan, new Gson().fromJson(planText, JsonElement.class));
				result.content = e.toString();
				
				return result;
			}
			if (match(path, method, "/workspaces/*/schedules/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				
				// plan stringify
				String fieldPlan = "plan";
				String body = new String(bodyRaw);
				JsonElement e = new Gson().fromJson(body, JsonElement.class);
				JsonObject o = e.getAsJsonObject();
				if (o.has(fieldPlan)) {
					JsonObject plan = o.get(fieldPlan).getAsJsonObject();
					ValidateSchedule.validateV2(plan);
					o.remove(fieldPlan);
					o.addProperty(fieldPlan, plan.toString());
					body = e.toString();
				}
				
				return Schedule.update(workspace, name, Schedule.parsePatch(body));
			}
			if (match(path, method, "/workspaces/*/schedules/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Schedule.delete(workspace, name);
			}
			
			if (match(path, method, "/workspaces/*/schedules/*/patch-checkpoint, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String scheduleId = getFromPathParamsAsString(path, 3);
				return process("/workspaces/" + workspace + "/schedules/" + scheduleId, "patch", query, header, bodyRaw);
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Task
			if (match(path, method, "/workspaces/*/schedules/*/tasks, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				return Task.listTasksBySchedule(workspace, schedule);
			}
			if (match(path, method, "/workspaces/*/schedules/*/tasks/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				String taskId = getFromPathParamsAsString(path, 5);
				return Task.get(workspace, schedule, taskId);
			}
			if (match(path, method, "/workspaces/*/tasks, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				if (query.containsKey("status")) {
					String status = query.get("status");
					return Task.list(workspace, status);
				} else {
					return Task.list(workspace);
				}
			}
			if (match(path, method, "/tasks, get")) {
				if (query.containsKey("status")) {
					String status = query.get("status");
					return Task.listAllByStatus(status);
				} else {
					return Task.listAll();
				}
			}
			if (match(path, method, "/workspaces/*/tasks, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return Task.create(workspace, Task.parse(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/tasks/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Task.get(workspace, name);
			}
			if (match(path, method, "/workspaces/*/tasks/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Task.update(workspace, name, Task.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/tasks/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Task.delete(workspace, name);
			}
			if (match(path, method, "/workspaces/*/tasks/*/patch-status, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return process("/workspaces/" + workspace + "/tasks/" + name, "patch", query, header, bodyRaw);
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Item
			if (match(path, method, "/workspaces/*/tasks/*/items, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long task = getFromPathParamsAsLong(path, 3, -1);
				if (task < 0) { throw new Exception("Failed to parse task ID: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				return Item.list(workspace, task);
			}
			if (match(path, method, "/workspaces/*/tasks/*/items, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long task = getFromPathParamsAsLong(path, 3, -1);
				if (task < 0) { throw new Exception("Failed to parse task ID: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				return Item.create(workspace, task, Item.parse(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/tasks/*/items/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long task = getFromPathParamsAsLong(path, 3, -1);
				if (task < 0) { throw new Exception("Failed to parse task ID: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				long id = getFromPathParamsAsLong(path, 5, -1);
				if (id < 0) { throw new Exception("Failed to parse item ID: \"" + getFromPathParamsAsString(path, 5) + "\"."); }
				return Item.get(workspace, task, id);
			}
			if (match(path, method, "/workspaces/*/tasks/*/items/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long task = getFromPathParamsAsLong(path, 3, -1);
				if (task < 0) { throw new Exception("Failed to parse task ID: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				long id = getFromPathParamsAsLong(path, 5, -1);
				if (id < 0) { throw new Exception("Failed to parse item ID: \"" + getFromPathParamsAsString(path, 5) + "\"."); }
				return Item.update(workspace, task, id, Item.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/workspaces/*/tasks/*/items/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long task = getFromPathParamsAsLong(path, 3, -1);
				if (task < 0) { throw new Exception("Failed to parse task ID: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				long id = getFromPathParamsAsLong(path, 5, -1);
				if (id < 0) { throw new Exception("Failed to parse item ID: \"" + getFromPathParamsAsString(path, 5) + "\"."); }
				return Item.delete(workspace, task, id);
			}
			if (match(path, method, "/workspaces/*/items, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				if (query.containsKey("status")) {
					String status = query.get("status");
					return Item.list(workspace, status);
				} else {
					return Item.list(workspace);
				}
			}
			if (match(path, method, "/workspaces/*/items/*/patch-status, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return process("/workspaces/" + workspace + "/items/" + name, "patch", query, header, bodyRaw);
			}
			if (match(path, method, "/workspaces/*/items/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return Item.update(workspace, name, Item.parsePatch(new String(bodyRaw)));
			}
			if (match(path, method, "/items-to-queue, get")) {
				return Item.listCreatedOrRetrying();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Log_Item
			if (match(path, method, "/workspaces/*/items/*/logs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String itemName = getFromPathParamsAsString(path, 3);
				return Log_Item.listByItem(workspace, itemName);
			}
			if (match(path, method, "/workspaces/*/items/*/logs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String itemName = getFromPathParamsAsString(path, 3);
				return Log_Item.create(workspace, itemName, Log_Item.parse(new String(bodyRaw)));
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Log_Task
			if (match(path, method, "/workspaces/*/tasks/*/logs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long taskId = getFromPathParamsAsLong(path, 3, -1);
				if (taskId < 0) { throw new Exception("Failed to parse taskId: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				return Log_Task.listByTask(workspace, taskId);
			}
			if (match(path, method, "/workspaces/*/tasks/*/logs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				long taskId = getFromPathParamsAsLong(path, 3, -1);
				if (taskId < 0) { throw new Exception("Failed to parse taskId: \"" + getFromPathParamsAsString(path, 3) + "\"."); }
				return Log_Task.create(workspace, taskId, Log_Task.parse(new String(bodyRaw)));
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Log_Schedule
			if (match(path, method, "/workspaces/*/schedules/*/logs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String scheduleName = getFromPathParamsAsString(path, 3);
				return Log_Schedule.listBySchedule(workspace, scheduleName);
			}
			if (match(path, method, "/workspaces/*/schedules/*/logs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String scheduleName = getFromPathParamsAsString(path, 3);
				return Log_Schedule.create(workspace, scheduleName, Log_Schedule.parse(new String(bodyRaw)));
			}
			// TODO: ----------------------------------------------------------------------------------------------------> CAR02
			if (match(path, method, "/12c27e40-ac08-41ce-a8b7-04d3d62e0ccd, get")) {
				return CAR02.getResult();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> TEST EVAL
			/*if (match(path, method, "/solve/*, get")) {
				String output = renamedByLogic(getFromPathParamsAsString(path, 1), "function(x){return x+\".xyz\";}");
				
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put("output", output);
				
				return ZResult.OK_200(ZConnector.ConvertToJsonString(m));
			}*/
			// TODO: ----------------------------------------------------------------------------------------------------> end
		} catch (Exception ex) {
			return ZResult.ERROR_500(ex);
		}
		return ZResult.ERROR_501(method, path);
	}
}