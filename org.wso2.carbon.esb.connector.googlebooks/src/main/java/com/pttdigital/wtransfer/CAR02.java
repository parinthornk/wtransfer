package com.pttdigital.wtransfer;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.wso2.carbon.esb.connector.Queue;
import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZWorker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CAR02 {
	
	public static Timestamp time_now;

	private static String getJsPrint(String script) throws Exception {
		String ret = null;
		StringWriter writer = new StringWriter();
		try {
			ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("JavaScript");
			ScriptContext ctx = engine.getContext();
			ctx.setWriter(writer);
			engine.eval(script);
			String x = writer.toString();
			if (x.endsWith("\r\n")) {
				x = x.substring(0, x.length() - 2);
			} else if (x.endsWith("\n")) {
				x = x.substring(0, x.length() - 1);
			}
			ret = x;
			try { writer.close(); } catch (Exception e1) { }
		} catch (Exception ex) {
			ex.printStackTrace();
			try { writer.close(); } catch (Exception e1) { }
			throw ex;
		}
		return ret;
	}
	
	private static ArrayList<Schedule> getTriggeredSchedules() throws Exception {
		
		// list to record checkpoint
		JsonElement e1 = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/schedules?available=true", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		ArrayList<Schedule> s1 = new ArrayList<Schedule>();
		for (JsonElement ei1 : arr1) { s1.add((Schedule) DB.parse(Schedule.class, ei1.getAsJsonObject())); }
		
		// list triggered schedules
		ArrayList<Schedule> triggered = new ArrayList<Schedule>();
		JsonElement e = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/schedules/update-checkpoint", "post", null, null);
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		
		if (arr1.size() != arr.size()) { throw new Exception("Unexpected exception. \"arr1.size() != arr.size()\". ["+arr1.size()+" != "+arr.size()+"]"); }
		
		for (JsonElement ei : arr) {
			Schedule schedule = (Schedule) DB.parse(Schedule.class, ei.getAsJsonObject());
			
			// find previousCheckpoint
			Timestamp time_prev = null;
			for (Schedule s1i : s1) {
				if (s1i.name.equals(schedule.name)) {
					time_prev = s1i.previousCheckpoint;
					break;
				}
			}
			if (time_prev == null) {
				continue;
			}
			
			String[] sep = schedule.plan.split(",");
			for (String s : sep) {
				if (Schedule.isTriggered(s, time_now, time_prev)) {
					triggered.add(schedule);
				}
			}
		}
		
		return triggered;
	}

	private static String calculateSourceFolder(Schedule schedule) throws Exception {
		if (schedule.useDynamicDirSource) {
			throw new Exception("Dynamic directory is not yet supported."); // TODO: Dynamic directory is not yet supported
		} else {
			return schedule.staticDirSource;
		}
	}
	
	private static ArrayList<Schedule> getAllSchedules() throws NoSuchFieldException, Exception {
		JsonElement e1 = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/schedules?available=true", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		ArrayList<Schedule> s1 = new ArrayList<Schedule>();
		for (JsonElement ei1 : arr1) { s1.add((Schedule) DB.parse(Schedule.class, ei1.getAsJsonObject())); }
		return s1;
	}
	
	private static HashMap<String, Site> mapSites() throws Exception {
		JsonElement e1 = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/sites", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		HashMap<String, Site> s1 = new HashMap<String, Site>();
		for (JsonElement ei1 : arr1) {
			Site site = (Site) DB.parse(Site.class, ei1.getAsJsonObject());
			s1.put(site.name, site);
		}
		return s1;
	}
	
	public static void loop() throws Exception {
		
		System.out.println("" + getJsPrint("print(123);"));
		
		// now
		time_now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		
		// triggered schedules
		ArrayList<Schedule> triggered = getAllSchedules(); // getAllSchedules is just for test
		
		// list sites
		HashMap<String, Site> sites = mapSites();
		
		// list items affected by those schedules
		HashMap<String, IFileServer.FileServer> mapServers = new HashMap<String, IFileServer.FileServer>();
		HashMap<String, ArrayList<Item.Info>> mapScheduleInfo = new HashMap<String, ArrayList<Item.Info>>();
		for (Schedule schedule : triggered) {
			
			String siteName = sites.get(schedule.siteSource).name;
			if (!mapServers.containsKey(siteName)) {
				IFileServer.FileServer server = IFileServer.createServer(sites.get(schedule.siteSource));
				server.open();
				mapServers.put(siteName, server);
			}
			
			ArrayList<Item.Info> infos = new ArrayList<Item.Info>();
			JsonArray arr = mapServers.get(siteName).listObjects(schedule.staticDirSource);
			for (JsonElement e : arr) {
				
				Item.Info info = Item.Info.parse(e);
				if (info.isDirectory) {
					continue;
				}
				
				if (info.size > ZConnector.Constant.MAX_FILE_SIZE_BYTES) {
					continue;
				}
				
				boolean logicOK = false;
				try {
					String fnIsFileToMove = schedule.fnIsFileToMove;
					String logic = "var fn = " + fnIsFileToMove + ";print(fn(\"" + info.name + "\"));";
					String printed = getJsPrint(logic);
					logicOK = printed.equalsIgnoreCase("true");
				} catch (Exception ex) { ex.printStackTrace(); }
				if (!logicOK) {
					continue;
				}
				
				infos.add(info);
			}
			
			mapScheduleInfo.put(schedule.name, infos);
			
			
		}
		
		
		
		HashMap<String, ArrayList<String>> mapServerToArchive = new HashMap<String, ArrayList<String>>();
		System.out.println("--------------------------------------------------------------------");
		for (Schedule schedule : triggered) {
			ArrayList<Item.Info> infos = mapScheduleInfo.get(schedule.name);
			System.out.println("schedule.name: " + schedule.name);
			System.out.println("schedule.siteSource: " + schedule.siteSource);
			System.out.println("total: " + infos.size());
			for (Item.Info info : infos) {
				
				String text = schedule.staticDirSource + ":" + info.name;
				System.out.println(text);
				if (!mapServerToArchive.containsKey(schedule.siteSource)) {
					mapServerToArchive.put(schedule.siteSource, new ArrayList<String>());
				}
				
				if (!mapServerToArchive.get(schedule.siteSource).contains(text)) {
					mapServerToArchive.get(schedule.siteSource).add(text);
				}
			}
			System.out.println("--------------------------------------------------------------------");
		}

		System.out.println("--------------------------------------------------------------------");
		System.out.println("====================================================================");
		Set<String> ksToArchive = mapServerToArchive.keySet();
		for (String siteName : ksToArchive) {
			IFileServer.FileServer server = mapServers.get(siteName);
			ArrayList<String> a = mapServerToArchive.get(siteName);
			System.out.println("siteName: " + siteName);
			System.out.println("total: " + a.size());
			System.out.println("    ----------------------------------------------------------------");
			for (String text : a) {
				System.out.println("\t" + text);
				
				String[] sep = text.split(":");
				String folder = sep[0];
				String fileName = sep[1];
				
				String folderArchive = folder + "/archive"; // TODO
				String fileNameArchive = fileName + ".arc";
				
				System.out.println("\t" + "folder         : " + folder);
				System.out.println("\t" + "fileName       : " + fileName);
				System.out.println("\t" + "folderArchive  : " + folderArchive);
				System.out.println("\t" + "fileNameArchive: " + fileNameArchive);
				System.out.println("    ----------------------------------------------------------------");
				System.out.println("====================================================================");
				
				/*// TODO real archive
				if (!server.directoryExists(folderArchive)) {
					server.createDirectory(folderArchive);
				}
				server.move(folder + "/" + fileName, folderArchive + "/" + fileNameArchive);*/
			}
			System.out.println("====================================================================");
		}
		
		// create sessions
		HashMap<Long, Session> sessionsToCreated = new HashMap<Long, Session>();
		long sid = 560000;
		for (Schedule schedule : triggered) {
			Session session = new Session();
			session.workspace = schedule.workspace;
			session.id = sid++;
			session.description = "Auto-generated by schedule " + schedule.name + ". [" + UUID.randomUUID().toString().replace("-", "") + "]";
			session.schedule = schedule.name;
			session.status = Session.Status.CREATED.toString();
			sessionsToCreated.put(session.id, session);
		}
		
		// TODO: also include non-scheduled sessions
		
		// TODO: also include items in retrymode
		
		// post multiple sessions
		JsonElement sessionsPostResult = null;
		if ("".length() == 0) {
			JsonObject json = new JsonObject();
			JsonArray arrSessions = new JsonArray();
			String fields = "";
			String values = "";
			
			Set<Long> lks = sessionsToCreated.keySet();
			
			for (Long id : lks) {
				Session session = sessionsToCreated.get(id);
				
				JsonObject o = new Gson().fromJson(DB.toJsonString(session), JsonObject.class);
				arrSessions.add(o);
				if (fields.length() == 0) {
					Set<Entry<String, JsonElement>> sd = o.entrySet();
					for (Entry<String, JsonElement> s : sd) {
						if (s.getKey().equalsIgnoreCase("id")) { continue; }
						fields += "\"" + s.getKey() + "\"" + ", ";
					}
					fields = fields.substring(0, fields.length() - 2);
				}
				String textValues = "";
				Set<Entry<String, JsonElement>> sd = o.entrySet();
				for (Entry<String, JsonElement> s : sd) {
					if (s.getKey().equalsIgnoreCase("id")) { continue; }
					String v = s.getValue() + "";
					if (v.startsWith("\"") && v.endsWith("\"")) {
						v = v.substring(1, v.length() - 1);
						v = "'" + v + "'";
					}
					textValues += v + ", ";
				}
				textValues = textValues.substring(0, textValues.length() - 2);
				values += "(" + textValues + "), ";
			}
			values = values.substring(0, values.length() - 2);
			json.add("list", arrSessions);
			System.out.println(json);
			String sql = "insert into " + ZConnector.Constant.SCHEMA + ".\"" + Session.class.getSimpleName() + "\" (" + fields + ") values " + values + " returning *;";
			sessionsPostResult = DB.executeList(sql);
			System.out.println("sessionsPostResult: " + sessionsPostResult);
		}
		
		JsonArray resSessionsArr = sessionsPostResult.getAsJsonObject().get("list").getAsJsonArray();
		HashMap<String, Session> as = new HashMap<String, Session>();
		for (JsonElement e : resSessionsArr) {
			Session s = (Session) DB.parse(Session.class, e.getAsJsonObject());
			as.put(s.description, s);
		}
		
		
		// create items for those sessions
		Timestamp _1970 = Timestamp.valueOf("1970-01-01 00:00:00");
		ArrayList<Item> itemsToCreated = new ArrayList<Item>();
		HashMap<String, Schedule> mapSchedules = new HashMap<String, Schedule>(); for (Schedule schedule : triggered) { mapSchedules.put(schedule.name, schedule); }


		Set<Long> lks = sessionsToCreated.keySet();
		
		for (Long id : lks) {
			Session session = sessionsToCreated.get(id);
			
			Schedule schedule = mapSchedules.get(session.schedule);
			
			ArrayList<Item.Info> infos = mapScheduleInfo.get(session.schedule);
			for (Item.Info info : infos) {
				Item item = new Item();
				
				
				
				item.session = as.get(session.description).id;
				
				item.name = session.workspace + ":" + item.session + ":" + info.name;
				
				
				item.workspace = session.workspace;
				item.retryQuota = schedule.retryCount;
				item.retryRemaining = 1 + schedule.retryCount;
				item.retryIntervalMs = schedule.retryIntervalMs;
				item.timeNextRetry = _1970;
				item.timeLatestRetry = _1970;
				//item.fnCallback = schedule.callback;// TODO
				
				item.description = session.description;
				
				item.fileName = info.name;
				item.folder = schedule.staticDirSource;
				item.fileNameArchive = info.name + ".arc";
				item.folderArchive = schedule.staticDirSource + "/archive"; // TODO
				
				itemsToCreated.add(item);
			}
		}
		
		// post multiple items
		JsonElement resultPostItems = null;
		
		System.out.println("itemsToCreated.size(): " + itemsToCreated.size());
		if (itemsToCreated.size() > 0) {

			JsonObject json = new JsonObject();
			JsonArray arrItems = new JsonArray();
			String fields = "";
			String values = "";
			for (Item item : itemsToCreated) {
				JsonObject o = new Gson().fromJson(DB.toJsonString(item), JsonObject.class);
				arrItems.add(o);
				if (fields.length() == 0) {
					Set<Entry<String, JsonElement>> sd = o.entrySet();
					for (Entry<String, JsonElement> s : sd) {
						if (s.getKey().equalsIgnoreCase("id")) { continue; }
						fields += "\"" + s.getKey() + "\"" + ", ";
					}
					fields = fields.substring(0, fields.length() - 2);
				}
				String textValues = "";
				Set<Entry<String, JsonElement>> sd = o.entrySet();
				for (Entry<String, JsonElement> s : sd) {
					if (s.getKey().equalsIgnoreCase("id")) { continue; }
					String v = s.getValue() + "";
					if (v.startsWith("\"") && v.endsWith("\"")) {
						v = v.substring(1, v.length() - 1);
						v = "'" + v + "'";
					}
					textValues += v + ", ";
				}
				textValues = textValues.substring(0, textValues.length() - 2);
				values += "(" + textValues + "), ";
			}
			values = values.substring(0, values.length() - 2);
			json.add("list", arrItems);
			System.out.println(json);
			String sql = "insert into " + ZConnector.Constant.SCHEMA + ".\"" + Item.class.getSimpleName() + "\" (" + fields + ") values " + values + " returning *;";
			resultPostItems = DB.executeList(sql);
		}
		
		// enqueue items
		System.out.println("--------------------------------------------------------------------------------------------------------");
		if (itemsToCreated.size() > 0) {
			
			Queue.Publisher.open();
			
			JsonArray arrResItems = resultPostItems.getAsJsonObject().get("list").getAsJsonArray();
			for (JsonElement eItem :arrResItems ) {
				Item item = (Item) DB.parse(Item.class, eItem.getAsJsonObject());
				JsonObject message = new JsonObject();
				Session session = as.get(item.description);
				Schedule schedule = mapSchedules.get(session.schedule);
				Site siteSource = sites.get(schedule.siteSource);
				Site siteTarget = sites.get(schedule.siteTarget);
				message.add("schedule", new Gson().fromJson(DB.toJsonString(schedule), JsonObject.class));
				message.add("siteSource", new Gson().fromJson(DB.toJsonString(siteSource), JsonObject.class));
				message.add("siteTarget", new Gson().fromJson(DB.toJsonString(siteTarget), JsonObject.class));
				message.add("session", new Gson().fromJson(DB.toJsonString(session), JsonObject.class));
				message.add("item", new Gson().fromJson(DB.toJsonString(item), JsonObject.class));

				String queueName = siteSource.name + "--to--" + siteTarget.name;
				Queue.Publisher.enqueue(queueName, message.toString());
				
				System.out.println(message);
				System.out.println("--------------------------------");
			}
			
			Queue.Publisher.close();
		}
		System.out.println("--------------------------------------------------------------------------------------------------------");
		
		
		// cleanup servers
		Set<String> ks = mapServers.keySet();
		for (String siteName : ks) {
			mapServers.get(siteName).close();
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		if ("".length() == 0) {
			System.exit(0);
		}
	}
	
	public static void main(String[] arg) throws Exception {
		
		//GenerateClassFromSchema.create();
		
		for (;;) { loop(); Thread.sleep(2000); }
	}
}