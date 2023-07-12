package com.pttdigital.wtransfer;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wso2.carbon.esb.connector.Queue;
import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZConnector.Constant;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;
import org.wso2.carbon.esb.connector.ZWorker.LogType;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pttdigital.wtransfer.ImportV2.OL;

public class ZAPIV3 {
	

	
    public static void main(String[] args) throws Exception {
    	
    	HashMap<String, String> query = new HashMap<String, String>();
    	query.put("path", "/");
    	ZResult result = process("/workspaces/default/sites/dcloud-sftp/objects", "get", query, null, (byte[])null);
    	OL.sln(result.content);
    	
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
	
	private static boolean match(String path, String method, String target) {
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
	
	private static String getFromPathParamsAsString(String path, int position) throws Exception {
		try {
			
			String val = path.split("/")[1 + position];
			val = java.net.URLDecoder.decode(val, StandardCharsets.UTF_8.name());
			if (val.contains("'") || val.contains("--")) {
				throw new Exception("Error parsing string from path parameter, " + path);
			}
			return val;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
	
	public static ZConnector.ZResult process(String path, String method, HashMap<String, String> query, HashMap<String, String> header, String bodyText) {
		return process(path, method, query, header, bodyText.getBytes());
	}
	
	public static ZConnector.ZResult process(String path, String method, HashMap<String, String> query, HashMap<String, String> header, byte[] bodyRaw) {
		Class<?> c;
		try {
			// TODO: ----------------------------------------------------------------------------------------------------> Common
			if (match(path, method, "/tables, get")) {
				return ZResult.OK_200(GenerateClassFromSchema.exampleObjects().toString());
			}
			
			// TODO: ----------------------------------------------------------------------------------------------------> Workspace
			c = Workspace.class;
			if (match(path, method, "/workspaces, get")) {
				String sql = DB.sqlList(c);
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces, post")) {
				String sql = DB.sqlCreate(c, new String(bodyRaw));
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*, get")) {
				String name = getFromPathParamsAsString(path, 1);
				String sql = DB.sqlGet(c, name);
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*, patch")) {
				String name = getFromPathParamsAsString(path, 1);
				String sql = DB.sqlUpdate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class), name);
				JsonElement e = DB.executeUpdate(sql, c.getSimpleName(), name);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*, delete")) {
				String name = getFromPathParamsAsString(path, 1);
				String sql = DB.sqlDelete(c, name);
				JsonElement e = DB.executeDelete(sql, c.getSimpleName(), name);
				return ZResult.OK_200(e.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Site
			c = Site.class;
			if (match(path, method, "/sites-reachability, get")) {
				return Site.getReachability();
			}
			if (match(path, method, "/sites, get")) {
				//String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\";";
				String sql = "select * from " + ZConnector.Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" order by \"" + Site.getWordName() + "\"";
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sites, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				//String sql = DB.sqlListInWorkspace(c, workspace);
				String sql = "select * from " + ZConnector.Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" where " + Site.getWordWorkspace() + " = '" + workspace + "' order by \"" + Site.getWordName() + "\"";
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sites, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String sql = DB.sqlCreateInWorkspace(c, workspace, new String(bodyRaw));
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sites/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				String sql = DB.sqlGetInWorkspace(c, site, workspace);
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			
			if (match(path, method, "/workspaces/*/sites/*/objects, get")) {
				
				// abs path
				String absolutePath = "/";
				if (query == null) {
				} else if (!query.containsKey("path")) {
				} else {
					absolutePath = query.get("path");
					while (absolutePath.startsWith("/")) { absolutePath = absolutePath.substring(1, absolutePath.length()); }
					absolutePath = "/" + absolutePath;
				}
				while (absolutePath.contains("%20")) { absolutePath = absolutePath.replace("%20", " "); }
				
				String workspace = getFromPathParamsAsString(path, 1);
				
				if (!workspace.equalsIgnoreCase("default")) {
					return ZResult.ERROR_500(new Exception("workspace must be set to \"default\"."));
				}
				
				String site = getFromPathParamsAsString(path, 3);
				String session_id = "";
				
				// open session
				{
					try {
						JsonObject p1 = new JsonObject();
						p1.addProperty("action", "server.open");
						p1.addProperty("site", site);
						session_id = Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1).getAsJsonObject().get("id").getAsString();						
					} catch(Exception ex) { }
				}
				
				if (session_id.length() == 0) {
					throw new Exception("Failed to create CUSTOM_FILE_SERVER_ENDPOINT session.");
				}
				
				// list files
				JsonObject list = null;
				{
					try {
						JsonObject p2 = new JsonObject();
						p2.addProperty("action", "object.list");
						p2.addProperty("id", session_id);
						p2.addProperty("directory", absolutePath);
						list = Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p2).getAsJsonObject();
					} catch(Exception ex) { }
				}
				
				// close session
				{
					try {
						JsonObject p3 = new JsonObject();
						p3.addProperty("action", "server.close");
						p3.addProperty("id", session_id);
						Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p3);
					} catch (Exception ex) { }
				}
				
				if (list == null) {
					throw new Exception("Failed to list objects using CUSTOM_FILE_SERVER_ENDPOINT.");
				}
				
				return ZResult.OK_200(list.toString());
			}
			
			if (match(path, method, "/workspaces/*/sites/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				String sql = DB.sqlUpdateInWorkspace(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class), site, workspace);
				JsonElement e = DB.executeUpdate(sql, c.getSimpleName(), site);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sites/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				String sql = DB.sqlDeleteInWorkspace(c, site, workspace);
				JsonElement e = DB.executeDelete(sql, c.getSimpleName(), site);
				return ZResult.OK_200(e.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Schedule
			c = Schedule.class;
			if (match(path, method, "/schedules, get")) {
				if (query != null) {
					if (query.containsKey("available")) {
						boolean enabled = false;
						String a = query.get("available");
						if (a.equalsIgnoreCase("true") || a.equalsIgnoreCase("false")) {
							enabled = Boolean.parseBoolean(a);
						} else {
							return ZResult.ERROR_400(new Exception("Query parameter \"available\" must be boolean."));
						}
						
						String now = new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(new Timestamp(Calendar.getInstance().getTimeInMillis()));
						
						String sql;
						if (enabled) {
							sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where (\"enabled\" = 'true') and ((\"validFrom\" is null) or (\"validFrom\" <= '" + now + "')) and ((\"validUntil\" is null) or (\"validUntil\" >= '" + now + "'));"; // TODO: direct wording is too risk
						} else {
							sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where (\"enabled\" = 'false') or ((\"validFrom\" is not null) and (\"validFrom\" > '" + now + "')) or ((\"validUntil\" is not null) and (\"validUntil\" < '" + now + "'))"; // TODO: direct wording is too risk
						}
						JsonElement e = DB.executeList(sql);
						return ZResult.OK_200(e.toString());
					}
				}
				JsonElement e = DB.executeList("select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\";");
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				//String sql = DB.sqlListInWorkspace(c, workspace);
				
				
				String schema = Constant.SCHEMA;
				String keyWorkspace = "workspace";
				String tableMother = "Schedule";
				String keyMother = "name";
				String tableChild = "Session";
				String keyChild = "id";
				String keyChildLookupMother = "schedule";
				String countColoumnName = "sessions";
				String sql = "SELECT "+schema+".\""+tableMother+"\".*, COUNT("+schema+".\""+tableChild+"\"."+keyChild+") AS "+countColoumnName+" FROM "+schema+".\""+tableMother+"\" LEFT JOIN "+schema+".\""+tableChild+"\" ON "+schema+".\""+tableMother+"\"."+keyMother+" = "+schema+".\""+tableChild+"\".\""+keyChildLookupMother+"\" and "+schema+".\""+tableMother+"\"."+keyWorkspace+" = "+schema+".\""+tableChild+"\"."+keyWorkspace+" where "+schema+".\""+tableMother+"\"."+keyWorkspace+" = '"+workspace+"' GROUP BY "+schema+".\""+tableMother+"\"."+keyMother+";";
				
				
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/patching, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				return process("/workspaces/" + workspace + "/schedules/" + schedule, "patch", query, header, bodyRaw);
			}
			if (match(path, method, "/schedules/update-checkpoint, post")) {
				//String now = new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(CAR02.time_now); // TODO
				
				//String body = new String(bodyRaw);
				String now = new Gson().fromJson(new String(bodyRaw), JsonObject.class).get("now").getAsString();
				
				String sql = "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set \"previousCheckpoint\" = '" + now + "', \"modified\" = '" + now + "' where (\"enabled\" = 'true') and ((\"validFrom\" is null) or (\"validFrom\" <= '" + now + "')) and ((\"validUntil\" is null) or (\"validUntil\" >= '" + now + "')) returning *;"; // TODO: direct wording is too risk
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules, post")) {
				
				
				//OL.sln("123445");
				
				String workspace = getFromPathParamsAsString(path, 1);
				
				String plan = Schedule.getPlanningArray(bodyRaw);
				if (plan == null) {
					return ZResult.ERROR_400(new Exception("Parameter \"" + Schedule.wordPlan + "\" is required."));
				}
				Schedule.validatePlanningArray(plan);
				
				
				String body = new String(bodyRaw);
				Schedule schedule = (Schedule) DB.parse(c, new Gson().fromJson(body, JsonObject.class));
				
				if (schedule.useDynamicDirSource || schedule.useDynamicDirTarget) {
					return ZResult.ERROR_501(new Exception("Dynamic directory is not yet supported."));
				}
				
				if (!schedule.useDynamicDirSource && schedule.staticDirSource == null) {
					return ZResult.ERROR_400(new Exception("Missing required field \"staticDirSource\"."));
				}
				if (!schedule.useDynamicDirTarget && schedule.staticDirTarget == null) {
					return ZResult.ERROR_400(new Exception("Missing required field \"staticDirTarget\"."));
				}
				
				
				
				String sql = DB.sqlCreateInWorkspace(c, workspace, body);
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				
				
				

				
				String schema = Constant.SCHEMA;
				String keyWorkspace = "workspace";
				String tableMother = "Schedule";
				String keyMother = "name";
				String tableChild = "Session";
				String keyChild = "id";
				String keyChildLookupMother = "schedule";
				String countColoumnName = "sessions";
				String sql = "SELECT "+schema+".\""+tableMother+"\".*, COUNT("+schema+".\""+tableChild+"\"."+keyChild+") AS "+countColoumnName+" FROM "+schema+".\""+tableMother+"\" LEFT JOIN "+schema+".\""+tableChild+"\" ON "+schema+".\""+tableMother+"\"."+keyMother+" = "+schema+".\""+tableChild+"\".\""+keyChildLookupMother+"\" and "+schema+".\""+tableMother+"\"."+keyWorkspace+" = "+schema+".\""+tableChild+"\"."+keyWorkspace+" where "+schema+".\""+tableMother+"\"."+keyWorkspace+" = '"+workspace+"' and "+schema+".\""+tableMother+"\"."+keyMother+" = '"+schedule+"' GROUP BY "+schema+".\""+tableMother+"\"."+keyMother+";";
				
				
				
				
				
				
				
				//String sql = DB.sqlGetInWorkspace(c, schedule, workspace);
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				
				String plan = Schedule.getPlanningArray(bodyRaw);
				if (plan != null) {
					Schedule.validatePlanningArray(plan);
				}
				
				String sql = DB.sqlUpdateInWorkspace(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class), schedule, workspace);
				JsonElement e = DB.executeUpdate(sql, c.getSimpleName(), schedule);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				String sql = DB.sqlDeleteInWorkspace(c, schedule, workspace);
				JsonElement e = DB.executeDelete(sql, c.getSimpleName(), schedule);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/execute, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				
				if (!workspace.equals("default")) {
					throw new Exception("Schedule execution only suppport default workspace.");
				}
				
				String schedule = getFromPathParamsAsString(path, 3);
				HashMap<String, ArrayList<String>> result = CAR02.loop(schedule);
				if (result == null) {
					return ZResult.OK_200("{\"message\":\"Found 0 items.\"}");
				} else {
					if (result.size() == 0) {
						return ZResult.OK_200("{\"message\":\"Found 0 items.\"}");
					} else {
						
						// read status of all queued items
						Set<String> o2 = result.keySet();
						JsonArray arr = new JsonArray();
						for (String schedule_name : o2) {
							ArrayList<String> al = result.get(schedule_name);
							if (al != null) {
								for (String jsonString : al) {
									try {
										JsonObject statusOfItem = getStatusOfItem(jsonString);
										arr.add(statusOfItem);
									} catch (Exception ex) {
										
										// this will response directly to postman
										throw new Exception(result.size() + " items are enqueued but the program failed to monitor their status. " + ex.getMessage());
									}
								}
							}
						}
						
						// success with json array
						JsonObject o = new JsonObject();
						o.add("result", arr);
						return ZResult.OK_200(o.toString());
					}
				}
				
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Session
			c = Session.class;
			//if (match(path, method, "/sessions, get")) {
				
			//	String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\";";
			//	String status = Session.getStatus(query); if (status != null) { if (status.length() != 0) { sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + " where \"" + Session.wordStatus + "\" " + Session.wordStatusOperation + " '" + status + "';"; } }
				
			//	JsonElement e = DB.executeList(sql);
			//	return ZResult.OK_200(e.toString());
			//}
			//if (match(path, method, "/workspaces/*/sessions, get")) {
			//	String workspace = getFromPathParamsAsString(path, 1);
				
			//	String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "';";
			//	String status = Session.getStatus(query); if (status != null) { if (status.length() != 0) { sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + " where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.wordStatus + "\" " + Session.wordStatusOperation + " '" + status + "';"; } }
				
			//	JsonElement e = DB.executeList(sql);
			//	return ZResult.OK_200(e.toString());
			//}
			if (match(path, method, "/workspaces/*/sessions/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				
				

				String schema = Constant.SCHEMA;
				String keyWorkspace = "workspace";
				String tableMother = "Session";
				String keyMother = "id";
				String tableChild = "Item";
				String keyChild = "name";
				String keyChildLookupMother = "session";
				String countColoumnName = "items";
				
				//String keySessionFindSchedule = "schedule";
				
				String sql = "SELECT "+schema+".\""+tableMother+"\".*, COUNT("+schema+".\""+tableChild+"\"."+keyChild+") AS "+countColoumnName+" FROM "+schema+".\""+tableMother+"\" LEFT JOIN "+schema+".\""+tableChild+"\" ON "+schema+".\""+tableMother+"\"."+keyMother+" = "+schema+".\""+tableChild+"\".\""+keyChildLookupMother+"\" and "+schema+".\""+tableMother+"\"."+keyWorkspace+" = "+schema+".\""+tableChild+"\"."+keyWorkspace+" where "+schema+".\""+tableMother+"\"."+keyWorkspace+" = '"+workspace+"' and "+schema+".\""+tableMother+"\".\""+keyMother+"\" = '" + session + "' GROUP BY "+schema+".\""+tableMother+"\"."+keyMother+";";
				
				
				//String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Translate.getNameOrId(c) + "\" = '" + sessions + "';";
				
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/sessions, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				
				

				String schema = Constant.SCHEMA;
				String keyWorkspace = "workspace";
				String tableMother = "Session";
				String keyMother = "id";
				String tableChild = "Item";
				String keyChild = "name";
				String keyChildLookupMother = "session";
				String countColoumnName = "items";
				
				String keySessionFindSchedule = "schedule";
				
				String sql = "SELECT "+schema+".\""+tableMother+"\".*, COUNT("+schema+".\""+tableChild+"\"."+keyChild+") AS "+countColoumnName+" FROM "+schema+".\""+tableMother+"\" LEFT JOIN "+schema+".\""+tableChild+"\" ON "+schema+".\""+tableMother+"\"."+keyMother+" = "+schema+".\""+tableChild+"\".\""+keyChildLookupMother+"\" and "+schema+".\""+tableMother+"\"."+keyWorkspace+" = "+schema+".\""+tableChild+"\"."+keyWorkspace+" where "+schema+".\""+tableMother+"\"."+keyWorkspace+" = '"+workspace+"' and "+schema+".\""+tableMother+"\"."+keySessionFindSchedule+" = '"+schedule+"' GROUP BY "+schema+".\""+tableMother+"\"."+keyMother+" order by "+keyMother+" desc;";
				
				
				//String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Schedule.class.getSimpleName().toLowerCase() + "\" = '" + schedule + "';";
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/sessions, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				HashMap<String, Object> m1 = DB.mapCreate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				m1.put(Workspace.class.getSimpleName().toLowerCase(), workspace);
				m1.put(Schedule.class.getSimpleName().toLowerCase(), schedule);
				String sql = "insert into " + Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ")" + " returning " + Translate.getNameOrId(c) + ";";
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/sessions/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				String session = getFromPathParamsAsString(path, 5);
				
				
				
				
				
				
				
				


				String schema = Constant.SCHEMA;
				String keyWorkspace = "workspace";
				String tableMother = "Session";
				String keyMother = "id";
				String tableChild = "Item";
				String keyChild = "name";
				String keyChildLookupMother = "session";
				String countColoumnName = "items";
				
				String keySessionFindSchedule = "schedule";
				
				String sql = "SELECT "+schema+".\""+tableMother+"\".*, COUNT("+schema+".\""+tableChild+"\"."+keyChild+") AS "+countColoumnName+" FROM "+schema+".\""+tableMother+"\" LEFT JOIN "+schema+".\""+tableChild+"\" ON "+schema+".\""+tableMother+"\"."+keyMother+" = "+schema+".\""+tableChild+"\".\""+keyChildLookupMother+"\" and "+schema+".\""+tableMother+"\"."+keyWorkspace+" = "+schema+".\""+tableChild+"\"."+keyWorkspace+" where "+schema+".\""+tableMother+"\"."+keyWorkspace+" = '"+workspace+"' and "+schema+".\""+tableMother+"\"."+keySessionFindSchedule+" = '"+schedule+"' and "+schema+".\""+tableMother+"\"."+keyMother+" = '"+session+"' GROUP BY "+schema+".\""+tableMother+"\"."+keyMother+";";
				
				
				
				
				
				
				
				
				
				//String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Schedule.class.getSimpleName().toLowerCase() + "\" = '" + schedule + "' and \"" + Translate.getNameOrId(c) + "\" = '" + session + "';";
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/sessions/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				String session = getFromPathParamsAsString(path, 5);
				HashMap<String, Object> m1 = DB.mapUpdate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				String sql = "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set " + DB.concateStmtKeysValues(m1) + " where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Schedule.class.getSimpleName().toLowerCase() + "\" = '" + schedule + "' and \"" + Translate.getNameOrId(c) + "\" = '" + session + "';";
				JsonElement e = DB.executeUpdate(sql, c.getSimpleName(), schedule);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/sessions/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				String session = getFromPathParamsAsString(path, 5);
				String sql = "delete from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Schedule.class.getSimpleName().toLowerCase() + "\" = '" + schedule + "' and \"" + Translate.getNameOrId(c) + "\" = '" + session + "';";
				JsonElement e = DB.executeDelete(sql, c.getSimpleName(), schedule);
				return ZResult.OK_200(e.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> Item
			c = Item.class;
			//if (match(path, method, "/items, get")) {
			//	String status = Item.getStatus(query);
				
			//	if (status != null) {
			//		if (status.length() != 0) {
			//			String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + " where \"" + Item.wordStatus + "\" " + Item.wordStatusOperation + " '" + status + "';";
			//			JsonElement e = DB.executeList(sql);
			//			return ZResult.OK_200(e.toString());
			//		}
			//	}
				
			//	String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + ";";
			//	JsonElement e = DB.executeList(sql);
			//	return ZResult.OK_200(e.toString());
			//}
			if (match(path, method, "/items, get")) {
				
				// where conditions
				ArrayList<String> where = new ArrayList<String>();
				
				// status query
				try { { String field = Item.wordStatus; String x = query.get(field); if (x != null) { if (x.length() > 0) { where.add("\"" + field + "\" = '" + x + "'"); } } } } catch(Exception ex) { }
				
				//String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "' order by \""+Item.getFileNameWord()+"\" asc;";
				
				String sql = "select * from " + Constant.SCHEMA + ".\"" + Item.class.getSimpleName() + "\"";
				if (where.size() > 0) {
					String end = " where";
					for (String x : where) {
						end += " " + x + " AND";
					}
					sql += end;
					sql = sql.substring(0, sql.length() - " AND".length());
				}
				
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*/items, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "' order by \""+Item.getFileNameWord()+"\" asc;";
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*/items, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				HashMap<String, Object> m1 = DB.mapCreate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				m1.put(Workspace.class.getSimpleName().toLowerCase(), workspace);
				m1.put(Session.class.getSimpleName().toLowerCase(), session);
				String sql = "insert into " + Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ")" + " returning " + Translate.getNameOrId(c) + ";";
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*/items/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String item = getFromPathParamsAsString(path, 5);
				
				
				
				
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "' and \"" + Translate.getNameOrId(c) + "\" = '" + item + "';";
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*/items/*/re-execute, post")) {
				
				
				String workspace = getFromPathParamsAsString(path, 1);
				String sessionId = getFromPathParamsAsString(path, 3);
				String itemName = getFromPathParamsAsString(path, 5);

		    	ZResult resultItem = process("/workspaces/" + workspace + "/sessions/" + sessionId + "/items/" + itemName, "get", null, null, (byte[])null);
		    	Item item = (Item) DB.parse(Item.class, new Gson().fromJson(resultItem.content, JsonObject.class));
		    	
		    	// get schedule
		    	ZResult resultSession = process("/workspaces/" + workspace + "/sessions/" + sessionId, "get", null, null, (byte[])null);
		    	
		    	String schedule_name = new Gson().fromJson(resultSession.content, JsonObject.class).get("schedule").getAsString();
		    	ZResult resultSchedule = process("/workspaces/" + workspace + "/schedules/" + schedule_name, "get", null, null, (byte[])null);
		    	//Schedule schedule = (Schedule) DB.parse(Schedule.class, new Gson().fromJson(resultSchedule.content, JsonObject.class));

		    	String site_source_name = new Gson().fromJson(resultSchedule.content, JsonObject.class).get("siteSource").getAsString();
		    	String site_target_name = new Gson().fromJson(resultSchedule.content, JsonObject.class).get("siteTarget").getAsString();
		    	
		    	ZResult site_source = process("/workspaces/" + workspace + "/sites/" + site_source_name, "get", null, null, (byte[])null);
		    	ZResult site_target = process("/workspaces/" + workspace + "/sites/" + site_target_name, "get", null, null, (byte[])null);
		    	//Site siteSource = (Site) DB.parse(Site.class, new Gson().fromJson(site_source.content, JsonObject.class));
		    	//Site siteTarget = (Site) DB.parse(Site.class, new Gson().fromJson(site_target.content, JsonObject.class));
		    	
		    	
		    	//OL.sln(resultSchedule.content);
		    	Client.addItemLog(item, Log.Type.INFO, "RE-EXECUTION", "The item is re-executed manually.");
		    	
		    	// constructed
		    	JsonObject o2 = new JsonObject();
		    	o2.add("item", new Gson().fromJson(resultItem.content, JsonObject.class));
		    	o2.add("session", new Gson().fromJson(resultSession.content, JsonObject.class));
		    	o2.add("schedule", new Gson().fromJson(resultSchedule.content, JsonObject.class));
		    	o2.add("siteSource", new Gson().fromJson(site_source.content, JsonObject.class));
		    	o2.add("siteTarget", new Gson().fromJson(site_target.content, JsonObject.class));
		    	String constructedText = o2.toString();
		    	
		    	// enqueue
		    	Exception error = null;
		    	Queue.Publisher publisher = null;
		    	try {
			    	publisher = new Queue.Publisher();
			    	publisher.open();
			    	publisher.enqueue(schedule_name, constructedText);
		    	} catch (Exception ex) { error = ex; }
		    	
		    	if (publisher != null) { publisher.close(); }
		    	
		    	if (error != null) {
		    		return ZResult.ERROR_500(error);
		    	} else {
			    	return ZResult.OK_200("{\"message\":\"OK\"}");
		    	}
			}
			if (match(path, method, "/workspaces/*/sessions/*/items/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String item = getFromPathParamsAsString(path, 5);
				HashMap<String, Object> m1 = DB.mapUpdate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				String sql = "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set " + DB.concateStmtKeysValues(m1) + " where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "' and \"" + Translate.getNameOrId(c) + "\" = '" + item + "';";
				JsonElement e = DB.executeUpdate(sql, c.getSimpleName(), item);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*/items/*/patch, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String item = getFromPathParamsAsString(path, 5);
				return process("/workspaces/" + workspace + "/sessions/" + session + "/items/" + item, "patch", null, null, bodyRaw);
			}
			if (match(path, method, "/workspaces/*/sessions/*/items/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String item = getFromPathParamsAsString(path, 5);
				String sql = "delete from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "' and \"" + Translate.getNameOrId(c) + "\" = '" + item + "';";
				JsonElement e = DB.executeDelete(sql, c.getSimpleName(), session);
				return ZResult.OK_200(e.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> LogItem
			c = LogItem.class;
			if (match(path, method, "/workspaces/*/items/*/logs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String item = getFromPathParamsAsString(path, 3);
				String statusQuery = ""; String status = Item.getStatus(query); if (status != null) { if (status.length() > 0) { statusQuery = " and \"" + Item.wordStatus + "\" " + Item.wordStatusOperation + " '" + status + "'"; } }
				String sql = String.format("select * from %s where \"%s\" = '%s' and \"%s\" = '%s'%s order by "+Item.getIdWord()+";", Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\"", Workspace.class.getSimpleName().toLowerCase(), workspace, Item.class.getSimpleName().toLowerCase(), item, statusQuery);
				return ZResult.OK_200(DB.executeList(sql).toString());
			}
			if (match(path, method, "/workspaces/*/items/*/logs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String item = getFromPathParamsAsString(path, 3);
				HashMap<String, Object> m1 = DB.mapCreate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				m1.put(Workspace.class.getSimpleName().toLowerCase(), workspace);
				m1.put(Item.class.getSimpleName().toLowerCase(), item);
				String sql = "insert into " + Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ")" + " returning " + Translate.getNameOrId(c) + ";";
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> LogSchedule
			c = LogSchedule.class;
			if (match(path, method, "/workspaces/*/schedules/*/logs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				
				String sql = String.format("select * from %s where \"%s\" = '%s' and \"%s\" = '%s';", Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\"", Workspace.class.getSimpleName().toLowerCase(), workspace, Schedule.class.getSimpleName().toLowerCase(), schedule);
				return ZResult.OK_200(DB.executeList(sql).toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/logs, post")) {
				
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				HashMap<String, Object> m1 = DB.mapCreate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				m1.put(Workspace.class.getSimpleName().toLowerCase(), workspace);
				m1.put(Schedule.class.getSimpleName().toLowerCase(), schedule);
				String sql = "insert into " + Constant.SCHEMA + ".\"" + c.getSimpleName() + "\" (" + DB.concateStmtKeys(m1) + ") values (" + DB.concateStmtValues(m1) + ")" + " returning " + Translate.getNameOrId(c) + ";";
				
				JsonElement e = DB.executeInsert(c, sql);
				return ZResult.OK_200(e.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> CAR02
			if (match(path, method, "/b387cbea-56dc-40ee-a1c1-5492e8b32735, get")) {
				return TestFtpSsl.n0();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> CAR02
			if (match(path, method, "/12c27e40-ac08-41ce-a8b7-04d3d62e0ccd, get")) {
				return CAR02.getResult();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> telnet
			if (match(path, method, "/telnet-check, post")) {
				JsonObject o = new Gson().fromJson(new String(bodyRaw), JsonObject.class);
				boolean telnetStatus = TelnetCheck.getTelnet(o.get("ip").getAsString(), o.get("port").getAsInt());
				if (telnetStatus) {
					return ZResult.OK_200("{\"telnet-status\": \"OK\"}");
				} else {
					return ZResult.ERROR_500(new Exception("telnet failed"));
				}
			}
			// TODO: ----------------------------------------------------------------------------------------------------> site-management
			if (match(path, method, "/site-management, post")) {
				return CAR02.executeSiteAction(new String(bodyRaw));
			}
			// TODO: ----------------------------------------------------------------------------------------------------> sync_transfer
			if (match(path, method, "/workspaces/*/synchronous-operations/transfer, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				JsonObject o = null; try { o = new Gson().fromJson(new String(bodyRaw), JsonObject.class); } catch (Exception ex) { return ZResult.ERROR_400(new Exception("Failed to parse the request into JSON.")); }
		    	String transferMode = null; try { transferMode = o.get("transferMode").getAsString(); } catch (Exception ex) { } if (transferMode == null) { return ZResult.ERROR_400(new Exception("Parameter \"transferMode\" is required.")); }
		    	String sourceServer = null; try { sourceServer = o.get("sourceServer").getAsString(); } catch (Exception ex) { } if (sourceServer == null) { return ZResult.ERROR_400(new Exception("Parameter \"sourceServer\" is required.")); }
		    	String sourceFile = null; try { sourceFile = o.get("sourceFile").getAsString(); } catch (Exception ex) { } if (sourceFile == null) { return ZResult.ERROR_400(new Exception("Parameter \"sourceFile\" is required.")); }
		    	String targetServer = null; try { targetServer = o.get("targetServer").getAsString(); } catch (Exception ex) { } if (targetServer == null) { return ZResult.ERROR_400(new Exception("Parameter \"targetServer\" is required.")); }
		    	String targetFile = null; try { targetFile = o.get("targetFile").getAsString(); } catch (Exception ex) { } if (targetFile == null) { return ZResult.ERROR_400(new Exception("Parameter \"targetFile\" is required.")); }
		    	
		    	// call service http://127.0.0.1:62580/custom-file-server/site-operations
		    	o.addProperty("action", "transfer");
		    	
		    	JsonElement e = Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, o);
		    	
		    	String ret = e.toString();
		    	
		    	while (ret.contains("java.lang.Exception: ")) {ret = ret.replace("java.lang.Exception: ", "Exception: ");}
		    	while (ret.contains("http://127.0.0.1:62580")) {ret = ret.replace("http://127.0.0.1:62580", "");}
		    	while (ret.contains("http://10.224.143.44:8290/wtransfer/workspaces/default")) {ret = ret.replace("http://10.224.143.44:8290/wtransfer/workspaces/default", "");}
		    	
		    	return ZResult.OK_200(ret);
		    	
				//return CAR02.sync_transfer(workspace, transferMode, sourceServer, sourceFile, targetServer, targetFile);
			}
			// TODO: ----------------------------------------------------------------------------------------------------> sync_delete
			if (match(path, method, "/workspaces/*/synchronous-operations/delete, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				JsonObject o = null; try { o = new Gson().fromJson(new String(bodyRaw), JsonObject.class); } catch (Exception ex) { return ZResult.ERROR_400(new Exception("Failed to parse the request into JSON.")); }
				String server = null; try { server = o.get("server").getAsString(); } catch (Exception ex) { } if (server == null) { return ZResult.ERROR_400(new Exception("Parameter \"server\" is required.")); }
		    	String file = null; try { file = o.get("file").getAsString(); } catch (Exception ex) { } if (file == null) { return ZResult.ERROR_400(new Exception("Parameter \"file\" is required.")); }
		    	return CAR02.sync_delete(workspace, server, file);
			}
			// TODO: ----------------------------------------------------------------------------------------------------> test local_file_proxy
			if (match(path, method, "/custom-file-server/site-operations, post")) {
				Object o = Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "post", null, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				return ZResult.OK_200(o.toString());
			}
			// TODO: ----------------------------------------------------------------------------------------------------> end
		} catch (Exception ex) {
			ex.printStackTrace();
			return ZResult.ERROR_500(ex);
		}
		return ZResult.ERROR_501(method, path);
	}

	private static JsonObject getStatusOfItem(String itemQueueString) throws Exception {
		JsonObject jItem = new Gson().fromJson(itemQueueString, JsonObject.class).get("item").getAsJsonObject();
		String itemName = jItem.get("name").getAsString();
		String sessionId = new Gson().fromJson(itemQueueString, JsonObject.class).get("session").getAsJsonObject().get("id").getAsString();
		//long twoMinutes = 2 * 60 * 1000;
		long twoMinutes = 20 * 1000;
		long t_start = System.currentTimeMillis();
		
		String deleteme = "";
		
		for (;;) {
			long elapsed = System.currentTimeMillis() - t_start;
			String trace = "/workspaces/default/sessions/" + sessionId + "/items/" + itemName;
			if (elapsed > twoMinutes) {
				throw new Exception("Transfer took too long. [item: " + trace + "], " + deleteme);
			}
			Object[] objs = readItemStaus(itemName, sessionId);
			String iFileName = objs[1].toString();
			String iStatus = objs[2].toString();
			
			deleteme = "[" + iFileName + ", " + iStatus + "]";
			
			if (iStatus.equalsIgnoreCase(Item.Status.SUCCESS.toString())) {
				JsonObject jtmp = new JsonObject();
				jtmp.addProperty("file", iFileName);
				jtmp.addProperty("success", true);
				jtmp.addProperty("trace", trace);
				return jtmp;
			} else if (iStatus.equalsIgnoreCase(Item.Status.FAILED.toString())) {
				JsonObject jtmp = new JsonObject();
				jtmp.addProperty("file", iFileName);
				jtmp.addProperty("success", false);
				jtmp.addProperty("trace", trace);
				return jtmp;
			} else {
				Thread.sleep(1000);
			}
		}
	}

	private static String[] readItemStaus(String itemName, String sessionId) throws Exception {

		int attempts = 5;
		for (int i=0;i<attempts;i++) {

			try {
				ZResult zr = process("/workspaces/default/sessions/" + sessionId + "/items/" + itemName, "get", null, null, (byte[])null);
				if (zr.statusCode != 200) {
					throw new Exception(zr.content);
				}
				
				Item item = (Item) DB.parse(Item.class, new Gson().fromJson(zr.content, JsonObject.class));
				
				
				
				return new String[] { item.name, item.fileName, item.status };
				
			} catch (Exception ex) {
				if (i < attempts - 1) {
					Thread.sleep(250);
					continue;
				} else {
					throw ex;
				}
			}
		}
		
		throw new Exception("Failed to read item status: " + "/workspaces/default/sessions/" + sessionId + "/items/" + itemName);
	}
}