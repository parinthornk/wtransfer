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

	private static HashMap<String, Schedule> mapAllSchedules() throws Exception {
		HashMap<String, Schedule> ret = new HashMap<String, Schedule>();
		JsonElement e1 = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/schedules?available=true", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		for (JsonElement ei1 : arr1) {
			Schedule shedule = (Schedule) DB.parse(Schedule.class, ei1.getAsJsonObject());
			ret.put(shedule.name, shedule);
		}
		
		System.out.println("---> " + ZWorker.WTRANSFER_API_ENDPOINT + "/schedules?available=true");
		return ret;
	}
	
	private static ArrayList<Schedule> getTriggeredSchedules(HashMap<String, Schedule> allSchedules) throws Exception {
		
		// list triggered schedules
		JsonElement e = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/schedules/update-checkpoint", "post", null, null);
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		
		if (allSchedules.size() != arr.size()) { throw new Exception("Unexpected exception. \"allSchedules.size() != arr.size()\". ["+allSchedules.size()+" != "+arr.size()+"]"); }

		ArrayList<Schedule> triggered = new ArrayList<Schedule>();
		for (JsonElement ei : arr) {
			Schedule schedule = (Schedule) DB.parse(Schedule.class, ei.getAsJsonObject());
			
			// find previousCheckpoint
			Timestamp time_prev = null;
			
			
			Set<String> ks = allSchedules.keySet();
			for (String sName : ks) {
				Schedule s1i = allSchedules.get(sName);
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
	
	private static HashMap<Long, Session> mapSessions() throws Exception {
		JsonElement e1 = Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/sessions", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		HashMap<Long, Session> s1 = new HashMap<Long, Session>();
		for (JsonElement ei1 : arr1) {
			Session session = (Session) DB.parse(Session.class, ei1.getAsJsonObject());
			s1.put(session.id, session);
		}
		return s1;
	}
	
	public static ZConnector.ZResult getResult() {
		try {
			loop();
			return ZConnector.ZResult.OK_200("{\"executed\": \"" + System.currentTimeMillis() + "\"}");
		} catch (Exception ex) {
			return ZConnector.ZResult.ERROR_500(ex);
		}
	}
	
	private static Object map_server_locker = new Object();
	private static HashMap<String, IFileServer.FileServer> map_server_map = new HashMap<String, IFileServer.FileServer>();
	
	private static void map_server_clear() {
		synchronized (map_server_locker) {
			map_server_map.clear();
		}
	}
	
	private static void map_server_add(String siteName, IFileServer.FileServer server) {
		synchronized (map_server_locker) {
			map_server_map.put(siteName, server);
		}
	}
	
	private static ArrayList<Item> getNewlyCreatedItems(HashMap<String, Schedule> allSchedules, HashMap<String, Site> allSites) { // TODO

		ArrayList<Item> itemsToCreated = new ArrayList<Item>();
		
		//HashMap<String, IFileServer.FileServer> mapServers = new HashMap<String, IFileServer.FileServer>();
		map_server_clear();
		
		try {
			
			// now
			time_now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			
			// triggered schedules
			ArrayList<Schedule> triggered = getTriggeredSchedules(allSchedules);
			
			// tmp ----------------------- tmp
			triggered.clear(); Set<String> ksrt = allSchedules.keySet(); for (String s : ksrt) { triggered.add(allSchedules.get(s)); }
			
			
			System.out.println("triggeredSchedules: " + triggered.size());
			
			// ------------------------------------------------------------------------------------------------------------------------------------ //
			
			// try open servers concurrently
			long t_open_start = System.currentTimeMillis();
			ArrayList<Thread> arrThreads = new ArrayList<Thread>();
			for (Schedule schedule : triggered) {
				String siteName = allSites.get(schedule.siteSource).name;
				if (!map_server_map.containsKey(siteName)) {
					final Site site = allSites.get(schedule.siteSource);
					arrThreads.add(new Thread() {
						@Override
						public void run() {
							try {
								IFileServer.FileServer server = IFileServer.createServer(site);
								server.open();
								map_server_add(site.name, server);
							} catch (Exception e) {
								e.printStackTrace();
								map_server_add(site.name, null);
							}
						}
					});
				}
			}
			
			for (Thread thread : arrThreads) { thread.start(); }
			for (Thread thread : arrThreads) { thread.join(); }
			long t_open_stop = System.currentTimeMillis();
			Set<String> set_map_server_map = map_server_map.keySet();
			int nNull = 0;
			for (String s : set_map_server_map) { if (map_server_map.get(s) == null) { nNull++; } }
			System.out.println("threads open count: " + arrThreads.size() + ", duration: " + (t_open_stop - t_open_start) + " ms, null = " + nNull + " of " + set_map_server_map.size() + ".");
			
			if ("".length() == 0) {
				throw new Exception("test");
			}
			
			// ------------------------------------------------------------------------------------------------------------------------------------ //
			
			/*for (Schedule schedule : triggered) {
				String siteName = allSites.get(schedule.siteSource).name;
				if (!mapServers.containsKey(siteName)) {
					IFileServer.FileServer server = IFileServer.createServer(allSites.get(schedule.siteSource));
					try {
						server.open();
						mapServers.put(siteName, server);
					} catch (Exception ex) {
						ex.printStackTrace();
						mapServers.put(siteName, null);
					}
				}
			}*/
			
			// list items affected by those schedules
			HashMap<String, ArrayList<Item.Info>> mapScheduleInfo = new HashMap<String, ArrayList<Item.Info>>();
			for (Schedule schedule : triggered) {

				String siteName = allSites.get(schedule.siteSource).name;
				
				IFileServer.FileServer server = map_server_map.get(siteName);
				if (server == null) {
					mapScheduleInfo.put(schedule.name, null);
				} else {

					ArrayList<Item.Info> infos = new ArrayList<Item.Info>();
					JsonArray arr = server.listObjects(schedule.staticDirSource);
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
			}
			
			
			
			HashMap<String, ArrayList<String>> mapServerToArchive = new HashMap<String, ArrayList<String>>();
			System.out.println("--------------------------------------------------------------------");
			for (Schedule schedule : triggered) {
				ArrayList<Item.Info> infos = mapScheduleInfo.get(schedule.name);
				if (infos == null) {

					if (!mapServerToArchive.containsKey(schedule.siteSource)) {
						mapServerToArchive.put(schedule.siteSource, null);
					}
				} else {

					System.out.println("schedule.name: " + schedule.name);
					System.out.println("schedule.siteSource: " + schedule.siteSource);
					System.out.println("total: " + infos.size());
					for (Item.Info info : infos) {
						
						String dirSource_fileName = schedule.staticDirSource + ":" + info.name;
						System.out.println(dirSource_fileName);
						if (!mapServerToArchive.containsKey(schedule.siteSource)) {
							mapServerToArchive.put(schedule.siteSource, new ArrayList<String>());
						}
						
						if (!mapServerToArchive.get(schedule.siteSource).contains(dirSource_fileName)) {
							mapServerToArchive.get(schedule.siteSource).add(dirSource_fileName);
						}
					}
				}
				System.out.println("--------------------------------------------------------------------");
			}

			System.out.println("--------------------------------------------------------------------");
			System.out.println("====================================================================");
			Set<String> ksToArchive = mapServerToArchive.keySet();
			for (String siteName : ksToArchive) {
				IFileServer.FileServer server = map_server_map.get(siteName);
				if (server == null) {
					
				} else {
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
						
						// TODO real archive concurrently
						if (!server.directoryExists(folderArchive)) {
							server.createDirectory(folderArchive);
						}
						server.move(folder + "/" + fileName, folderArchive + "/" + fileNameArchive);
					}
				}
				System.out.println("====================================================================");
			}
			
			// create sessions
			HashMap<Long, Session> sessionsToCreated = new HashMap<Long, Session>();
			long sid = 560000;
			for (Schedule schedule : triggered) {
				
				IFileServer.FileServer server = map_server_map.get(schedule.siteSource);
				String status = server == null ? Session.Status.ERROR.toString() : Session.Status.CREATED.toString();
				
				Session session = new Session();
				session.workspace = schedule.workspace;
				session.id = sid++;
				session.description = "Auto-generated by schedule " + schedule.name + ". [" + UUID.randomUUID().toString().replace("-", "") + "]";
				session.schedule = schedule.name;
				session.status = status;
				session.remark = "Error connecting to site source: \"" + schedule.siteSource + "\", items inside source folder \"" + schedule.staticDirSource + "\" will not be delivered.";
				sessionsToCreated.put(session.id, session);
			}
			
			// TODO: also include non-scheduled sessions
			
			// TODO: also include items in retrymode
			
			// post multiple sessions
			JsonElement sessionsPostResult = null;
			HashMap<String, Session> as = new HashMap<String, Session>();
			if (sessionsToCreated.size() > 0) {
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
				
				JsonArray resSessionsArr = sessionsPostResult.getAsJsonObject().get("list").getAsJsonArray();
				
				for (JsonElement e : resSessionsArr) {
					Session s = (Session) DB.parse(Session.class, e.getAsJsonObject());
					as.put(s.description, s);
				}
				
				
				
				
			}
			
			
			// create items for those sessions
			Timestamp _1970 = Timestamp.valueOf("1970-01-01 00:00:00");
			HashMap<String, Schedule> mapSchedules = new HashMap<String, Schedule>(); for (Schedule schedule : triggered) { mapSchedules.put(schedule.name, schedule); }


			Set<Long> lks = sessionsToCreated.keySet();
			
			for (Long id : lks) {
				Session session = sessionsToCreated.get(id);
				
				Schedule schedule = mapSchedules.get(session.schedule);
				
				ArrayList<Item.Info> infos = mapScheduleInfo.get(session.schedule);
				
				if (infos == null) {
					// TODO: log session: no items discovered for this session, error connecting to source site
				} else {
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
						item.status = Item.Status.CREATED.toString();
						itemsToCreated.add(item);
					}
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
			
			/*// enqueue items
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
			System.out.println("--------------------------------------------------------------------------------------------------------");*/
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		// cleanup servers
		Set<String> ks = map_server_map.keySet();
		for (String siteName : ks) {
			try {
				map_server_map.get(siteName).close();
			} catch (Exception ex) { }
		}
		
		return itemsToCreated;
	}
	
	private static ArrayList<Item> getItemsToRetry() {
		// TODO Auto-generated method stub
		return new ArrayList<Item>();
	}
	
	public static void loop() throws Exception {
		
		System.out.println("------------------------------------------------------------------------------------");
		
		// all schedules
		HashMap<String, Schedule> allSchedules = mapAllSchedules();
		System.out.println("allSchedules: " + allSchedules.size());
		
		// all sites
		HashMap<String, Site> allSites = mapSites();
		System.out.println("allSites: " + allSites.size());
		
		// all existing sessions // TODO, limit only previous 24 hour?
		HashMap<Long, Session> allExistingSessions = mapSessions();
		System.out.println("allExistingSessions: " + allExistingSessions.size());
		
		// list items to be created
		ArrayList<Item> itemsToCreated = getNewlyCreatedItems(allSchedules, allSites);
		System.out.println("itemsToCreated: " + itemsToCreated.size());
		
		// list items in retry mode
		ArrayList<Item> itemsToRetry = getItemsToRetry();
		System.out.println("itemsToRetry: " + itemsToRetry.size());
		
		// construct message for all items
		
		// enqueue

		System.out.println("------------------------------------------------------------------------------------");
	}

	public static void main(String[] arg) throws Exception {
		
		//GenerateClassFromSchema.create();
		
		//for (;;) { loop(); Thread.sleep(2000); }
		
		loop();
	}
}