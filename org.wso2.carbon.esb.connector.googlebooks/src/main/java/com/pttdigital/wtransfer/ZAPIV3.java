package com.pttdigital.wtransfer;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZConnector.Constant;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ZAPIV3 {
	
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
			if (match(path, method, "/sites, get")) {
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\";";
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sites, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String sql = DB.sqlListInWorkspace(c, workspace);
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

				String absolutePath = "/";
				if (query == null) {
					//return ZResult.ERROR_400(new Exception("Query parameter: \"filePath\" is required."));
				} else if (!query.containsKey("path")) {
					//return ZResult.ERROR_400(new Exception("Query parameter: \"filePath\" is required."));
				} else {
					absolutePath = query.get("path");
					while (absolutePath.startsWith("/")) {
						absolutePath = absolutePath.substring(1, absolutePath.length());
					}
					absolutePath = "/" + absolutePath;
				}
				
				while (absolutePath.contains("%20")) { absolutePath = absolutePath.replace("%20", " "); }
				
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				String sql = DB.sqlGetInWorkspace(c, site, workspace);
				JsonElement e = DB.executeGet(sql);
				
				IFileServer server = null;
				try {
					Site s = (Site) DB.parse(Site.class, e.getAsJsonObject());
					server = IFileServer.createServer(s);
					server.open();
					
					System.out.println("server.listObjects(" + absolutePath + ");");
					JsonArray array = server.listObjects(absolutePath);
					if (server != null) { server.close(); }
					ZResult result = new ZResult();
					result.statusCode = 200;
					result.content = "{\"count\":" + array.size() + ", \"objects\": " + array + "}";
					return result;
				} catch (Exception ex) {
					if (server != null) { server.close(); }
					throw ex;
				}
				
			}
			
			/*
		IFileServer.FileServer server = null;
		try {
			JsonElement json = ZWorker.getSiteByName(workspace, siteName);
			Site site = Site.parse(json);
			server = IFileServer.createServer(site);
			server.open();
			JsonArray array = server.listObjects();
			if (server != null) { server.close(); }
			ZResult result = new ZResult();
			result.statusCode = 200;
			result.content = "{\"objects\": " + array + "}";
			return result;
		} catch (Exception ex) {
			if (server != null) { server.close(); }
			throw ex;
		}*/
			
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
				String sql = DB.sqlListInWorkspace(c, workspace);
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/schedules/update-checkpoint, post")) {
				String now = new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(new Timestamp(Calendar.getInstance().getTimeInMillis())); // TODO
				String sql = "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set \"previousCheckpoint\" = '" + now + "', \"modified\" = '" + now + "' where (\"enabled\" = 'true') and ((\"validFrom\" is null) or (\"validFrom\" <= '" + now + "')) and ((\"validUntil\" is null) or (\"validUntil\" >= '" + now + "')) returning *;"; // TODO: direct wording is too risk
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules, post")) {
				
				
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
				String sql = DB.sqlGetInWorkspace(c, schedule, workspace);
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
			// TODO: ----------------------------------------------------------------------------------------------------> Session
			c = Session.class;
			if (match(path, method, "/sessions, get")) {
				
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\";";
				String status = Session.getStatus(query); if (status != null) { if (status.length() != 0) { sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + " where \"" + Session.wordStatus + "\" " + Session.wordStatusOperation + " '" + status + "';"; } }
				
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "';";
				String status = Session.getStatus(query); if (status != null) { if (status.length() != 0) { sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + " where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.wordStatus + "\" " + Session.wordStatusOperation + " '" + status + "';"; } }
				
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String sessions = getFromPathParamsAsString(path, 3);
				
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Translate.getNameOrId(c) + "\" = '" + sessions + "';";
				
				JsonElement e = DB.executeGet(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/schedules/*/sessions, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String schedule = getFromPathParamsAsString(path, 3);
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Schedule.class.getSimpleName().toLowerCase() + "\" = '" + schedule + "';";
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
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Schedule.class.getSimpleName().toLowerCase() + "\" = '" + schedule + "' and \"" + Translate.getNameOrId(c) + "\" = '" + session + "';";
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
			if (match(path, method, "/items, get")) {
				String status = Item.getStatus(query);
				
				if (status != null) {
					if (status.length() != 0) {
						String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + " where \"" + Item.wordStatus + "\" " + Item.wordStatusOperation + " '" + status + "';";
						JsonElement e = DB.executeList(sql);
						return ZResult.OK_200(e.toString());
					}
				}
				
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + ";";
				JsonElement e = DB.executeList(sql);
				return ZResult.OK_200(e.toString());
			}
			if (match(path, method, "/workspaces/*/sessions/*/items, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String sql = "select * from " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "';";
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
			if (match(path, method, "/workspaces/*/sessions/*/items/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String session = getFromPathParamsAsString(path, 3);
				String item = getFromPathParamsAsString(path, 5);
				HashMap<String, Object> m1 = DB.mapUpdate(c, new Gson().fromJson(new String(bodyRaw), JsonObject.class));
				String sql = "update " + Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\" set " + DB.concateStmtKeysValues(m1) + " where \"" + Workspace.class.getSimpleName().toLowerCase() + "\" = '" + workspace + "' and \"" + Session.class.getSimpleName().toLowerCase() + "\" = '" + session + "' and \"" + Translate.getNameOrId(c) + "\" = '" + item + "';";
				JsonElement e = DB.executeUpdate(sql, c.getSimpleName(), item);
				return ZResult.OK_200(e.toString());
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
				String sql = String.format("select * from %s where \"%s\" = '%s' and \"%s\" = '%s'%s;", Constant.SCHEMA + ".\"" + c.getSimpleName().toString() + "\"", Workspace.class.getSimpleName().toLowerCase(), workspace, Item.class.getSimpleName().toLowerCase(), item, statusQuery);
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
			// TODO: ----------------------------------------------------------------------------------------------------> CAR02
			if (match(path, method, "/12c27e40-ac08-41ce-a8b7-04d3d62e0ccd, get")) {
				return CAR02.getResult();
			}
			// TODO: ----------------------------------------------------------------------------------------------------> end
		} catch (Exception ex) {
			ex.printStackTrace();
			return ZResult.ERROR_500(ex);
		}
		return ZResult.ERROR_501(method, path);
	}
}