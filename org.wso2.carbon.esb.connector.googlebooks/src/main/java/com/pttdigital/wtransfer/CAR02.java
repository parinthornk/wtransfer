package com.pttdigital.wtransfer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.wso2.carbon.esb.connector.Queue;
import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZConnector.Constant;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;
import org.wso2.carbon.esb.connector.ZWorker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.pttdigital.wtransfer.IFileServer.FileServer;
import com.pttdigital.wtransfer.ImportV2.OL;

public class CAR02 {
	
	private static Queue.Publisher queuePublisher = new Queue.Publisher();
	
	public static Timestamp time_now;

	private static String getJsPrint(String script) throws Exception {
		String ret = null;
		StringWriter writer = new StringWriter();
		try {
			ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("JavaScript");
			ScriptContext ctx = engine.getContext();
			ctx.setWriter(writer);
			//System.out.println("engine.eval("+script+");");
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
			LocalLogger.write("error", "Error getJsPrint(" + script + "), " + ex.getMessage());
			try { writer.close(); } catch (Exception e1) { }
			throw ex;
		}
		return ret;
	}

	private static HashMap<String, Schedule> mapSchedules() throws Exception {
		HashMap<String, Schedule> ret = new HashMap<String, Schedule>();
		JsonElement e1 = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules?available=true", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		for (JsonElement ei1 : arr1) {
			Schedule shedule = (Schedule) DB.parse(Schedule.class, ei1.getAsJsonObject());
			ret.put(shedule.name, shedule);
			//OL.sln(ei1);
		}
		
		//System.out.println("---> " + ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules?available=true");
		return ret;
	}
	
	private static ArrayList<Schedule> getTriggeredSchedules(HashMap<String, Schedule> allSchedules) throws Exception {
		
		
		
		// list triggered schedules
		String now = new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(CAR02.time_now);
		JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules/update-checkpoint", "post", null, new Gson().fromJson("{\"now\":\"" + now + "\"}", JsonObject.class));
		JsonArray arr = e.getAsJsonObject().get("list").getAsJsonArray();
		
		
		
		if (allSchedules.size() != arr.size()) { throw new Exception("Unexpected exception. \"allSchedules.size() != arr.size()\". ["+allSchedules.size()+" != "+arr.size()+"]"); }

		ArrayList<Schedule> triggered = new ArrayList<Schedule>();
		for (JsonElement ei : arr) {
			Schedule schedule = (Schedule) DB.parse(Schedule.class, ei.getAsJsonObject());
			
			if (schedule.isPendingAdHoc) {
				
				
				triggered.add(schedule);
				
				try {
					Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/" + schedule.workspace + "/schedules/" + schedule.name + "/patching", "post", null, new Gson().fromJson("{\"isPendingAdHoc\":false}", JsonElement.class));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				continue;
			}
			
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
					break;
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
		JsonElement e1 = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules?available=true", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		ArrayList<Schedule> s1 = new ArrayList<Schedule>();
		for (JsonElement ei1 : arr1) { s1.add((Schedule) DB.parse(Schedule.class, ei1.getAsJsonObject())); }
		return s1;
	}
	
	public static HashMap<String, Site> mapSites() throws Exception {
		JsonElement e1 = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/sites", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		HashMap<String, Site> s1 = new HashMap<String, Site>();
		for (JsonElement ei1 : arr1) {
			Site site = (Site) DB.parse(Site.class, ei1.getAsJsonObject());
			s1.put(site.name, site);
		}
		return s1;
	}
	
	private static HashMap<Long, Session> mapSessions() throws Exception {
		JsonElement e1 = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/sessions", "get", null, null);
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
			long t0 = System.currentTimeMillis();
			loop(null);
			long t1 = System.currentTimeMillis();
			long duration = t1 - t0;
			return ZConnector.ZResult.OK_200("{\"executed\": " + t1 + ", \"duration\": " + duration + "}");
		} catch (Exception ex) {
			return ZConnector.ZResult.ERROR_500(ex);
		}
	}
	

	private static HashMap<String, IFileServer.FileServer> mserv = new HashMap<String, IFileServer.FileServer>();
	private static HashMap<String, Boolean> mserv_success = new HashMap<String, Boolean>();

	private static void openServers(ArrayList<Schedule> triggered, HashMap<String, Site> allSites) throws Exception {
		
		
		// servers cleanup
		mserv.clear();
		mserv_success.clear();
		
		// list sites
		ArrayList<String> sitesDistinct = new ArrayList<String>();
		for (Schedule schedule : triggered) {
			
			OL.sln("schedule: " + schedule);
			
			if (!sitesDistinct.contains(schedule.siteSource)) {
				sitesDistinct.add(schedule.siteSource);
			}
		}
		
		// create servers from sites
		for (String siteName : sitesDistinct) {
			try {
				mserv.put(siteName, IFileServer.createServer(allSites.get(siteName)));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// threads to open servers
		Object mserv_success_locker = new Object();
		ArrayList<Thread> ats = new ArrayList<Thread>();
		for (String siteName : sitesDistinct) {
			final String sn = siteName;
			ats.add(new Thread() {
				@Override
				public void run() {
					boolean success = false;
					try {
						IFileServer.FileServer sv = null;
						synchronized (mserv_success_locker) {
							try {
								sv = mserv.get(sn);
							} catch (Exception ex) {
								//ex.printStackTrace();
							}
						}
						
						
						
						
						
						
						
						// retry?
						sv.open();
						
						
						
						success = true;
					} catch (Exception ex) {
						//ex.printStackTrace();
						
						LocalLogger.write("error", "Error in openServers(), Failed to connect to site \"" + sn + "\". " + ex.getMessage());
					}
					
					synchronized (mserv_success_locker) {
						mserv_success.put(sn, success);
						//OL.sln("mserv_success.put("+sn+", "+success+");");
					}
				}
			});
		}
		
		// open servers concurrently
		for (Thread th : ats) { th.start(); }
		for (Thread th : ats) { th.join(); }
	}

	private static void closeServers() {
		Set<String> ks = mserv.keySet();
		for (String siteName : ks) {
			try {
				mserv.get(siteName).close();
			} catch (Exception ex) { ex.printStackTrace(); }
		}
		mserv.clear();
		mserv_success.clear();
	}
	
	private static HashMap<String, String> mapFileArcName = new HashMap<String, String>();
	private static ArrayList<Item> getNewlyCreatedItems(HashMap<String, Schedule> allSchedules, HashMap<String, Site> allSites, ArrayList<Schedule> triggered, HashMap<Long, Session> sessionsToCreated) { // TODO

		ArrayList<Item> itemsToCreated = new ArrayList<Item>();
		
		try {
			// ------------------------------------------------------------------------------------------------------------------------------------ //
			// list items affected by those schedules
			HashMap<String, ArrayList<Item.Info>> mapScheduleInfo = new HashMap<String, ArrayList<Item.Info>>();
			for (Schedule schedule : triggered) {
				
				
				LocalLogger.write("info", "getNewlyCreatedItems(" + schedule.name + ") start");
				
				String siteName = allSites.get(schedule.siteSource).name;
				
				//IFileServer.FileServer server = map_server_map.get(siteName);
				
				
				
				boolean server_success = mserv_success.get(siteName);
				if (!server_success) {
					mapScheduleInfo.put(schedule.name, null);
					
					LocalLogger.write("error", "\t" + "getNewlyCreatedItems(" + schedule.name + "). Site \"" + siteName + "\" was marked as unreachable and will be ignored.");
					
				} else {

					ArrayList<Item.Info> infos = new ArrayList<Item.Info>();
					
					String executing = "";
					
					try {
						
						executing = "Executing: mserv.get(" + siteName + ").listObjects(" + schedule.staticDirSource + ");";
						
						JsonArray arr = mserv.get(siteName).listObjects(schedule.staticDirSource);
						
						for (JsonElement e : arr) {
							
							executing = "Executing: Item.Info.parse(" + e + ");";
							
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
						
						LocalLogger.write("info", "\t" + "getNewlyCreatedItems(" + schedule.name + "), Success Retrieving Files: " + infos.size());
						
					} catch (Exception ex) {
						
						LocalLogger.write("error", "\t" + "getNewlyCreatedItems(" + schedule.name + "), Failed Retrieving Files: " + ex.getMessage());
					}
					
					if (infos.size() > 0) {
						//OL.sln("schedule[" + schedule.name + "], infos: " + infos.size());
					}
					
					
					mapScheduleInfo.put(schedule.name, infos);
				}
			}
			
			HashMap<String, ArrayList<String>> mapServerToArchive = new HashMap<String, ArrayList<String>>();
			for (Schedule schedule : triggered) {
				ArrayList<Item.Info> infos = mapScheduleInfo.get(schedule.name);
				if (infos == null) {

					if (!mapServerToArchive.containsKey(schedule.siteSource)) {
						mapServerToArchive.put(schedule.siteSource, null);
					}
				} else {
					for (Item.Info info : infos) {
						
						String dirSource_fileName = schedule.staticDirSource + ":" + info.name;
						if (!mapServerToArchive.containsKey(schedule.siteSource)) {
							mapServerToArchive.put(schedule.siteSource, new ArrayList<String>());
						}
						
						if (!mapServerToArchive.get(schedule.siteSource).contains(dirSource_fileName)) {
							mapServerToArchive.get(schedule.siteSource).add(dirSource_fileName);
						}
					}
				}
			}
			
			mapFileArcName.clear();
			Set<String> ksToArchive = mapServerToArchive.keySet();
			for (String siteName : ksToArchive) {

				boolean server_success = mserv_success.get(siteName);
				if (!server_success) {
					
				} else {
					IFileServer.FileServer server = mserv.get(siteName);
					ArrayList<String> a = mapServerToArchive.get(siteName);
					for (String text : a) {
						
						String[] sep = text.split(":");
						String folder = sep[0];
						String fileName = sep[1];
						
						String folderArchive = folder + "/Archive"; // TODO
						String fileNameArchive = fileName + "." + new SimpleDateFormat("yyyyMMddHHmmss").format(time_now) + ".arc";
						
						
						
						try {

							// TODO real archive concurrently
							LocalLogger.write("info", "real archive concurrently: siteName[" + siteName + "], folder[" + folder + "], fileName[" + fileName + "].");
							long t_start = System.currentTimeMillis();
							int _30_seconds = 5 * 1000;
							while ("" != null) {
								Exception error = null;
								try {
									if (!server.directoryExists(folderArchive)) {
										server.createDirectory(folderArchive);
									}
									server.move_internal(folder + "/" + fileName, folderArchive + "/" + fileNameArchive);
									break;
								} catch (Exception ex) {
									error = ex;
								}
								
								// In case "Permission denied" -> retry is useless, throw immediately
								String error_text = error.getMessage();
								if (error_text.toLowerCase().contains("Permission".toLowerCase()) || error_text.toLowerCase().contains("denied".toLowerCase())) {
									throw error;
								}
								
								// In case "no such file" -> retry is useless, throw immediately
								if (error_text.toLowerCase().contains("no such file".toLowerCase())) {
									throw error;
								}
								
								long elapsed = System.currentTimeMillis() - t_start;
								if (elapsed > _30_seconds) {
									throw error;
								} else {
									Thread.sleep(250);
								}
							}
							
							// -------------------------------------------------------------------------------------------- //
							String arc_name_key = siteName + ":" + folder + ":" + fileName;
							mapFileArcName.put(arc_name_key, fileNameArchive);
							// -------------------------------------------------------------------------------------------- //
						} catch (Exception ex) {
							// TODO, what do you do when it's permission denied?
							//OL.sln("Error archiving in [" + siteName + ", " + folder + "]: " + ex);
							LocalLogger.write("error", "Error archiving in [" + siteName + ", " + folder + "]: " + ex.getMessage());
						}
						
					}
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
						item.session = mapSessionsDescription.get(session.description).id;
						item.name = UUID.randomUUID().toString().replace("-", "").toLowerCase();//session.workspace + ":" + item.session + ":" + info.name;
						item.workspace = session.workspace;
						item.retryQuota = schedule.retryCount;
						item.retryRemaining = 1 + schedule.retryCount;
						item.retryIntervalMs = schedule.retryIntervalMs;
						item.timeNextRetry = _1970;
						item.timeLatestRetry = _1970;
						//item.fnCallback = schedule.callback;// TODO
						item.description = session.description;
						item.fileName = evalTargetFileName(info.name, schedule.fnRenameTo);
						item.folder = schedule.staticDirSource;
						
						
						
						
						// how to track back to archived file name? ---> 1. site, folder, filename
						// String arc_name_key = siteName + ":" + folder + ":" + fileName;
						// mapFileArcName

						String siteName = allSchedules.get(session.schedule).siteSource;
						String folder = allSchedules.get(session.schedule).staticDirSource;
						String fileName = info.name;
						String arc_name_key = siteName + ":" + folder + ":" + fileName;
						if (!mapFileArcName.containsKey(arc_name_key)) {
							
							// TODO, ?????
							continue;
							
							//throw new Exception("mapFileArcName does not containt \"" + arc_name_key + "\".");
						}
						
						String newArcName = mapFileArcName.get(arc_name_key);
						
						item.fileNameArchive = newArcName;//info.name + ".arc";
						item.folderArchive = schedule.staticDirSource + "/Archive"; // TODO
						item.status = Item.Status.CREATED.toString();
						item.created = time_now;
						item.modified = time_now;
						itemsToCreated.add(item);
						
						
						OL.sln("itemsToCreated.add(name["+item.name+"], session["+item.session+"], fileName["+item.fileName+"]);");
					}
				}
			}
			
			// post multiple items
			JsonElement resultPostItems = null;
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
				String sql = "insert into " + ZConnector.Constant.SCHEMA + ".\"" + Item.class.getSimpleName() + "\" (" + fields + ") values " + values + " returning *;";
				resultPostItems = DB.executeList(sql);
				
				// TODO, then what?
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return itemsToCreated;
	}
	


	private static String evalTargetFileName(String originalName, String fnRenameTo) {
		try {
			return getJsPrint("var fn = " + fnRenameTo + ";print(fn(\"" + originalName + "\"));");
		} catch (Exception ex) {
			LocalLogger.write("error", "Failed to evalTargetFileName(\""+originalName+"\", \""+fnRenameTo+"\"): " + ex.getMessage() + ", The file will keep its original name \""+originalName+"\".");
			return originalName;
		}
	}

	private static HashMap<String, ArrayList<String>> constructItemsMessage(ArrayList<Item> itemsToCreated, HashMap<String, Schedule> allSchedules, HashMap<String, Site> allSites, HashMap<String, Session> _mapSessionsDescription, boolean useMapItemDescription) throws Exception, IllegalArgumentException, IllegalAccessException {
		// eyJpdGVtIjp7IndvcmtzcGFjZSI6ImRlZmF1bHQiLCJmaWxlTmFtZSI6ImZpbGVfZXhhbXBsZV9YTFNYXzJNQi0wMDAwLnhsc3giLCJyZXRyeVJlbWFpbmluZyI6NiwiZmlsZU5hbWVBcmNoaXZlIjoiZmlsZV9leGFtcGxlX1hMU1hfMk1CLTAwMDAueGxzeC4yMDIzMDYyMDE3MjQwMS5hcmMiLCJzZXNzaW9uIjo3Mzc2MTU0LCJjcmVhdGVkIjoiMjAyMy0wNi0yMCAxNzoyNDowMSIsInJldHJ5SW50ZXJ2YWxNcyI6NjAwMDAsImRlc2NyaXB0aW9uIjoiQXV0by1nZW5lcmF0ZWQgYnkgc2NoZWR1bGUgdGVzdC16cGFyaW50aG9ybmstMDYyMC4gWzJmYjExMDUyNzA5YzQ5ZTE4NWM5MWQ2ZTllNWEyZWE3XSIsImZuQ2FsbGJhY2siOm51bGwsImZvbGRlciI6Ii9TYXBGYXgvQ05ETkRBVEEiLCJ0aW1lTGF0ZXN0UmV0cnkiOiIxOTcwLTAxLTAxIDAwOjAwOjAwIiwibmFtZSI6ImRjZmQ0YWNhZWMzNjQ3MmM5OGEyYjg4NGU5Zjg3Zjc2IiwibW9kaWZpZWQiOiIyMDIzLTA2LTIwIDE3OjI0OjA0IiwicmV0cnlRdW90YSI6NSwiZm9sZGVyQXJjaGl2ZSI6Ii9TYXBGYXgvQ05ETkRBVEEvQXJjaGl2ZSIsInRpbWVOZXh0UmV0cnkiOiIxOTcwLTAxLTAxIDAwOjAwOjAwIiwic3RhdHVzIjoiV0FJVElOR19GT1JfUkVUUlkifSwic2Vzc2lvbiI6eyJzY2hlZHVsZSI6InRlc3QtenBhcmludGhvcm5rLTA2MjAiLCJ3b3Jrc3BhY2UiOiJkZWZhdWx0IiwiY3JlYXRlZCI6IjIwMjMtMDYtMjAgMTc6MjQ6MDEiLCJkZXNjcmlwdGlvbiI6IkF1dG8tZ2VuZXJhdGVkIGJ5IHNjaGVkdWxlIHRlc3QtenBhcmludGhvcm5rLTA2MjAuIFsyZmIxMTA1MjcwOWM0OWUxODVjOTFkNmU5ZTVhMmVhN10iLCJtb2RpZmllZCI6IjIwMjMtMDYtMjAgMTc6MjQ6MDEiLCJyZW1hcmsiOm51bGwsImlkIjo3Mzc2MTU0LCJzdGF0dXMiOiJDUkVBVEVEIn0sInNjaGVkdWxlIjp7IndvcmtzcGFjZSI6ImRlZmF1bHQiLCJwZ3BLZXlQYXRoIjpudWxsLCJkZXNjcmlwdGlvbiI6IiIsInBncERpcmVjdGlvbiI6bnVsbCwidmFsaWRGcm9tIjpudWxsLCJlbmFibGVkIjp0cnVlLCJzdGF0aWNEaXJTb3VyY2UiOiIvU2FwRmF4L0NORE5EQVRBIiwiZm5Jc0ZpbGVUb01vdmUiOiJmdW5jdGlvbih4KXtyZXR1cm4geC50b0xvd2VyQ2FzZSgpLmVuZHNXaXRoKFwiLmNzdlwiKSB8fCB4LnRvTG93ZXJDYXNlKCkuZW5kc1dpdGgoXCIueGxzeFwiKSB8fCB4LnRvTG93ZXJDYXNlKCkuZW5kc1dpdGgoXCIudHh0XCIpO30iLCJmblBncFJlbmFtZVRvIjpudWxsLCJtb2RpZmllZCI6IjIwMjMtMDYtMjAgMTg6MjA6MjEiLCJmbkFyY2hpdmVSZW5hbWVUbyI6bnVsbCwicGxhbiI6IiotKi0qICo6MDA6MDAsKi0qLSogKjowMTowMCwqLSotKiAqOjAyOjAwLCotKi0qICo6MDM6MDAsKi0qLSogKjowNDowMCwqLSotKiAqOjA1OjAwLCotKi0qICo6MDY6MDAsKi0qLSogKjowNzowMCwqLSotKiAqOjA4OjAwLCotKi0qICo6MDk6MDAsKi0qLSogKjoxMDowMCwqLSotKiAqOjExOjAwLCotKi0qICo6MTI6MDAsKi0qLSogKjoxMzowMCwqLSotKiAqOjE0OjAwLCotKi0qICo6MTU6MDAsKi0qLSogKjoxNjowMCwqLSotKiAqOjE3OjAwLCotKi0qICo6MTg6MDAsKi0qLSogKjoxOTowMCwqLSotKiAqOjIwOjAwLCotKi0qICo6MjE6MDAsKi0qLSogKjoyMjowMCwqLSotKiAqOjIzOjAwLCotKi0qICo6MjQ6MDAsKi0qLSogKjoyNTowMCwqLSotKiAqOjI2OjAwLCotKi0qICo6Mjc6MDAsKi0qLSogKjoyODowMCwqLSotKiAqOjI5OjAwLCotKi0qICo6MzA6MDAsKi0qLSogKjozMTowMCwqLSotKiAqOjMyOjAwLCotKi0qICo6MzM6MDAsKi0qLSogKjozNDowMCwqLSotKiAqOjM1OjAwLCotKi0qICo6MzY6MDAsKi0qLSogKjozNzowMCwqLSotKiAqOjM4OjAwLCotKi0qICo6Mzk6MDAsKi0qLSogKjo0MDowMCwqLSotKiAqOjQxOjAwLCotKi0qICo6NDI6MDAsKi0qLSogKjo0MzowMCwqLSotKiAqOjQ0OjAwLCotKi0qICo6NDU6MDAsKi0qLSogKjo0NjowMCwqLSotKiAqOjQ3OjAwLCotKi0qICo6NDg6MDAsKi0qLSogKjo0OTowMCwqLSotKiAqOjUwOjAwLCotKi0qICo6NTE6MDAsKi0qLSogKjo1MjowMCwqLSotKiAqOjUzOjAwLCotKi0qICo6NTQ6MDAsKi0qLSogKjo1NTowMCwqLSotKiAqOjU2OjAwLCotKi0qICo6NTc6MDAsKi0qLSogKjo1ODowMCwqLSotKiAqOjU5OjAwIiwidXNlRHluYW1pY0RpclNvdXJjZSI6ZmFsc2UsImZuUmVuYW1lVG8iOiJmdW5jdGlvbih4KXtyZXR1cm4geDt9Iiwic2l0ZVNvdXJjZSI6ImRjbG91ZC1zZnRwIiwicmV0cnlDb3VudCI6NSwiY3JlYXRlZCI6IjIwMjMtMDYtMjAgMTc6MTc6MzciLCJpc1BlbmRpbmdBZEhvYyI6ZmFsc2UsInJldHJ5SW50ZXJ2YWxNcyI6NjAwMDAsInNpdGVUYXJnZXQiOiJsZWdhY3ktZmE1MzY3ZWMtYmU2OS00MmZhLTg0NjUtNjNmMTIyYmQ3YmFmIiwiZm5EeW5hbWljRGlyVGFyZ2V0IjpudWxsLCJwcmV2aW91c0NoZWNrcG9pbnQiOiIyMDIzLTA2LTIwIDE4OjIwOjIxIiwic3RhdGljRGlyVGFyZ2V0IjoiL25vdF9leGlzdHMvU2FwRmF4L0NORE5EQVRBL25vdF9leGlzdHMiLCJhcmNoaXZlRm9sZGVyIjoiL2FyY2hpdmUiLCJmbkR5bmFtaWNEaXJTb3VyY2UiOm51bGwsInBncEtleVBhc3N3b3JkIjpudWxsLCJuYW1lIjoidGVzdC16cGFyaW50aG9ybmstMDYyMCIsInZhbGlkVW50aWwiOm51bGwsInVzZUR5bmFtaWNEaXJUYXJnZXQiOmZhbHNlLCJ3b3JrZXJUaHJlYWRzIjo1fSwic2l0ZVNvdXJjZSI6eyJ3b3Jrc3BhY2UiOiJkZWZhdWx0IiwicHJvdG9jb2wiOiJzZnRwIiwicGFzc3dvcmQiOiJXU08yREBzZnRwIzk5IiwicG9ydCI6MjIsImNyZWF0ZWQiOiIyMDIzLTA0LTIwIDEyOjI4OjU5Iiwia2V5UGF0aCI6IiIsIm5hbWUiOiJkY2xvdWQtc2Z0cCIsImhvc3QiOiJvci1zZnRwc2Vydi10MDEucHR0b3IuY29tIiwiZGVzY3JpcHRpb24iOiIiLCJtb2RpZmllZCI6IjIwMjMtMDQtMjUgMTI6MjQ6MzgiLCJ1c2VybmFtZSI6InNmdHBfd3NvMmRfb3JlbyJ9LCJzaXRlVGFyZ2V0Ijp7IndvcmtzcGFjZSI6ImRlZmF1bHQiLCJwcm90b2NvbCI6ImZ0cCIsInBhc3N3b3JkIjoiY2NuZG51c3IiLCJwb3J0IjoyMSwiY3JlYXRlZCI6IjIwMjMtMDQtMjAgMTI6Mjg6NTgiLCJrZXlQYXRoIjpudWxsLCJuYW1lIjoibGVnYWN5LWZhNTM2N2VjLWJlNjktNDJmYS04NDY1LTYzZjEyMmJkN2JhZiIsImhvc3QiOiIxNzIuMjMuMTYuNTUiLCJkZXNjcmlwdGlvbiI6bnVsbCwibW9kaWZpZWQiOiIyMDIzLTA0LTIwIDEyOjI4OjU4IiwidXNlcm5hbWUiOiJjbmRudXNyIn19
		HashMap<String, ArrayList<String>> messages = new HashMap<String, ArrayList<String>>();
		for (Item item : itemsToCreated) {
			
			JsonObject o = new JsonObject();
			Session session;
			if (useMapItemDescription) {
				session = _mapSessionsDescription.get(item.description);
			} else {
				session = _mapSessionsDescription.get(item.session + "");
			}
			//OL.sln("session: " + session);
			Schedule schedule = allSchedules.get(session.schedule);
			Site siteSource = allSites.get(schedule.siteSource);
			Site siteTarget = allSites.get(schedule.siteTarget);

			o.add("item", new Gson().fromJson(DB.toJsonString(item), JsonObject.class));
			o.add("session", new Gson().fromJson(DB.toJsonString(session), JsonObject.class));
			o.add("schedule", new Gson().fromJson(DB.toJsonString(schedule), JsonObject.class));
			o.add("siteSource", new Gson().fromJson(DB.toJsonString(siteSource), JsonObject.class));
			o.add("siteTarget", new Gson().fromJson(DB.toJsonString(siteTarget), JsonObject.class));
			
			if (!messages.containsKey(schedule.name)) {
				messages.put(schedule.name, new ArrayList<String>());
			}
			
			//OL.sln(o);
			messages.get(schedule.name).add(o.toString());
			
		}
		return messages;
	}

	private static ArrayList<Item> getItemsToRetry() {
		// TODO Auto-generated method stub
		return new ArrayList<Item>();
	}


	private static HashMap<String, Session> mapSessionsDescription = new HashMap<String, Session>();
	
	private static HashMap<Long, Session> instantiateNewSessions(ArrayList<Schedule> triggered) throws NoSuchFieldException, Exception {

		
		// create sessions
		HashMap<Long, Session> sessionsToCreated = new HashMap<Long, Session>();
		long sid = 560000;
		for (Schedule schedule : triggered) {
			IFileServer.FileServer server = mserv.get(schedule.siteSource);
			String status = server == null ? Session.Status.ERROR.toString() : Session.Status.CREATED.toString();
			Session session = new Session();
			session.workspace = schedule.workspace;
			session.id = sid++;
			session.description = "Auto-generated by schedule " + schedule.name + ". [" + UUID.randomUUID().toString().replace("-", "").toLowerCase() + "]";
			session.schedule = schedule.name;
			session.status = status;
			session.created = time_now;
			session.modified = time_now;
			session.remark = server == null ? "Error connecting to site source: \"" + schedule.siteSource + "\", items inside source folder \"" + schedule.staticDirSource + "\" will not be delivered." : null;
			sessionsToCreated.put(session.id, session);
		}
		
		// post multiple sessions
		mapSessionsDescription.clear();
		JsonElement sessionsPostResult = null;
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
				mapSessionsDescription.put(s.description, s);
			}
		}
		
		return sessionsToCreated;
	}

	private static void enqueue(HashMap<String, ArrayList<String>> messages) throws Exception {
		// TODO sort message by what???
		if (messages.size() > 0) {
			try {
				
				Set<String> ks = messages.keySet();
				for (String scheduleName : ks) {
					ArrayList<String> list = messages.get(scheduleName);
					for (String text : list) {
						try {
							queuePublisher.enqueue(scheduleName, text);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			} catch (Exception ex) { ex.printStackTrace(); }
			
		}
	}
	
	public static HashMap<String, ArrayList<String>> loop(String specific_schedule) throws Exception {
		
		// ----------------------------------------------------------------------------------------
		LocalLogger.write("info", "---------------------------------------------------------------------------------------- loop begin");
		
		HashMap<String, ArrayList<String>> messages = null;
		
		Exception error = null;
		
		try {
			
			// remove sessions without items
			if (specific_schedule == null) {
				removeSessionsWithoutItems();
			}
			
			// now
			time_now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			LocalLogger.write("info", "time_now: " + time_now);
			
			// all schedules
			HashMap<String, Schedule> allSchedules = mapSchedules();
			LocalLogger.write("info", "allSchedules: " + allSchedules.size());
			
			// triggered schedules
			ArrayList<Schedule> triggered = null;
			if (specific_schedule != null) {
				
				Schedule ts = allSchedules.get(specific_schedule);
				if (ts == null) {
					throw new Exception("\"" + specific_schedule + "\" does not exists.");
				}
				
				triggered = new ArrayList<Schedule>(); triggered.add(ts);
			} else {
				triggered = getTriggeredSchedules(allSchedules);
			}
			LocalLogger.write("info", "triggered: " + triggered.size());
			for (int i = 0; i < triggered.size(); i++) { LocalLogger.write("info", "\t" + "triggered["+i+"]: " + triggered.get(i).name); }
			
			// all sites
			HashMap<String, Site> allSites = mapSites();
			LocalLogger.write("info", "allSites: " + allSites.size());
			
			// list item in retry mode
			HashMap<String, ArrayList<String>> itemsToRetry = new HashMap<String, ArrayList<String>>();
			if (specific_schedule == null) {
				itemsToRetry = getItemsForRetry(allSchedules, allSites);
				LocalLogger.write("info", "itemsToRetry: " + itemsToRetry.size());
			}
			
			// open server connections concurrently
			LocalLogger.write("info", "opening servers...");
			openServers(triggered, allSites);
			
			// new sessions
			HashMap<Long, Session> newCreatedSessions = instantiateNewSessions(triggered);
			LocalLogger.write("info", "sessionsToCreated: " + newCreatedSessions.size());
			
			// list items to be created
			ArrayList<Item> itemsToCreated = getNewlyCreatedItems(allSchedules, allSites, triggered, newCreatedSessions);
			LocalLogger.write("info", "itemsToCreated: " + itemsToCreated.size());
			
			// construct message for all items
			messages = constructItemsMessage(itemsToCreated, allSchedules, allSites, mapSessionsDescription, true);
			LocalLogger.write("info", "messages: " + messages.size());
			
			try {
				
				// queue open
				queuePublisher.open();
				
				// enqueue retry items
				if (specific_schedule == null) {
					enqueue(itemsToRetry);
				}
				
				// enqueue new items
				enqueue(messages);
				
			} catch (Exception ex) { ex.printStackTrace(); }
			
			// queue close
			queuePublisher.close();

			// remove sessions without items
			if (specific_schedule == null) {
				removeSessionsWithoutItems();
			}
			
			LocalLogger.write("info", "---------------------------------------------------------------------------------------- loop end without errors");
			
		} catch (Exception ex) {
			LocalLogger.write("error", "---------------------------------------------------------------------------------------- loop end with error: " + ex.getMessage());
			ex.printStackTrace(); error = ex;
		}
		
		// cleanup servers
		closeServers();
		
		
		
		//IFileServer.ServerSFTP.printms();
		
		if (specific_schedule != null) {
			if (error != null) {
				throw error;
			}
		}
		
		
		
		return messages;
	}

	private static void removeSessionsWithoutItems() {
		Connection c = null;
		Statement stmt = null;
		try {
			c = DriverManager.getConnection("jdbc:postgresql://" + Constant.HOST + ":" + Constant.PORT + "/" + Constant.DATABASE, Constant.USERNAME, Constant.PASSWORD);
			stmt = c.createStatement();
			String sql = "delete from " + ZConnector.Constant.SCHEMA + ".\"" + Session.class.getSimpleName() + "\" s where (select count(*) cnt from " + ZConnector.Constant.SCHEMA + ".\"" + Item.class.getSimpleName() + "\" i where i.\"" + Item.getSessionWord() + "\" = s.\"" + Session.getIdWord() + "\") = 0";
			stmt.executeUpdate(sql);
		} catch (Exception ex) { ex.printStackTrace(); }
		if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
		if (c != null) { try { c.close(); } catch (Exception e) { } }
	}

	private static void clear_db() throws Exception {
		/*DB.executeDeleteAll("LogItem");
		DB.executeDeleteAll("Item");
		DB.executeDeleteAll("Session");
		DB.executeDeleteAll("Schedule");
		DB.executeDeleteAll("Site");*/
		DB.executeDeleteAll("LogSchedule");
	}
	
	public static ZResult executeSiteAction(String json) throws Exception {
		
		ZResult zr = new ZResult();
		JsonObject o = new Gson().fromJson(json, JsonObject.class);
		String site = o.get("site").getAsString();
		String action = o.get("action").getAsString();
		String objectName = o.get("objectName").getAsString();
		
		IFileServer.FileServer server = null;
		try {
			JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/sites/" + site, "get", null, null);
			Site mySite = (Site) DB.parse(Site.class, e.getAsJsonObject());
			server = IFileServer.createServer(mySite);
			server.open();
			
			if (action.equalsIgnoreCase("folder.create")) {
				server.createDirectory(objectName);
				zr = new ZResult();
				zr.statusCode = 201;
				zr.content = "{\"objectName\": \"" + objectName + "\"}";
			} else if (action.equalsIgnoreCase("folder.exists")) {
				zr = ZResult.OK_200("{\"exists\":" + server.directoryExists(objectName) + "}");
			} else if (action.equalsIgnoreCase("folder.delete")) {
				server.deleteDirectory(objectName);
			} else {
				throw new Exception("The action \"" + action + "\" is not recognized.");
			}
			
			if (server != null) { server.close(); }
		} catch (Exception ex) {
			if (server != null) { server.close(); }
			throw ex;
		}
		
		return zr;
	}
	
	private static HashMap<String, ArrayList<String>> getItemsForRetry(HashMap<String, Schedule> allSchedules, HashMap<String, Site> allSites) throws Exception {
    	HashMap<String, String> query = new HashMap<String, String>();
    	query.put("status", "WAITING_FOR_RETRY");
    	ZResult result = ZAPIV3.process("/items", "get", query, null, (byte[])null);
    	JsonArray arr = new Gson().fromJson(result.content, JsonObject.class).get("list").getAsJsonArray();
    	
    	//OL.sln(arr.size());
    	
    	ArrayList<Long> sessionsAssociated = new ArrayList<Long>();
    	ArrayList<Item> items = new ArrayList<Item>();
    	for (JsonElement e : arr) {
    		Item i = (Item) DB.parse(Item.class, e.getAsJsonObject());
    		
    		if (!Item.markedAsRetry(i, time_now)) {
    			continue;
    		}
    		
    		if (!sessionsAssociated.contains(i.session)) {
    			sessionsAssociated.add(i.session);
    		}
    		items.add(i);
    	}
    	
    	if (sessionsAssociated.size() == 0) {
    		return new HashMap<String, ArrayList<String>>();
    	}
    	
    	String where = ""; for (long l : sessionsAssociated) { where += l + ", "; } where = where.substring(0, where.length() - 2);
    	JsonArray jSessions = DB.executeList("select * from " + Constant.SCHEMA + ".\"" + Session.class.getSimpleName() + "\" where " + Session.getIdWord() + " in (" + where + ")").getAsJsonObject().get("list").getAsJsonArray();
    	HashMap<String, Session> mapSessions = new HashMap<String, Session>();
    	for (JsonElement e : jSessions) {
    		Session s = (Session) DB.parse(Session.class, e.getAsJsonObject());
    		mapSessions.put(s.id + "", s);
    	}
    	
    	return constructItemsMessage(items, allSchedules, allSites, mapSessions, false);
	}

	public static ZResult sync_delete(String workspace, String server, String file) {
    	Exception exception = null;
    	int code = 200;
    	
    	FileServer fserv = null;
    	
    	try {
    		
    		// retrieve sites
        	HashMap<String, Site> allSites = null;
        	try {
        		allSites = mapSites();
        		code = 500;
        	} catch (Exception ex) {
        		throw new Exception("Error retrieving servers information from database. " + ex);
        	}
        	
        	// connect, login
        	try {
        		Site site = allSites.get(server);
        		if (site == null) { throw new Exception("Error retrieving servers information from database. The server \"" + server + "\" could not be found on workspace \"" + workspace + "\"."); }
        		if (!site.workspace.equals(workspace)) { throw new Exception("Error retrieving servers information from database. The server \"" + server + "\" could not be found on workspace \"" + workspace + "\"."); }
        		fserv = IFileServer.createServer(site);
        		fserv.open();
        	} catch (Exception ex) {
        		code = 500;
        		throw new Exception("Error connecting to server \"" + server + "\". " + ex);
        	}
        	
        	// delete the file
        	try {
        		fserv.deleteFile(file);
        	} catch (Exception ex) {
        		throw new Exception("Failed to delete file \"" + file + "\". " + ex);
        	}
        	
    	} catch (Exception ex) {
    		exception = ex;
    	}
    	
    	// cleanup
    	if (fserv != null) { try { fserv.close(); } catch (Exception ex) { } }
    	
    	// result
    	ZResult result = new ZResult();
    	if (exception == null) {
    		result = ZResult.OK_200("{\"message\":\"File was deleted.\"}");
    	} else {
    		JsonObject o = new JsonObject();
    		o.addProperty("message", exception + "");
    		result = new ZResult(); result.statusCode = code; result.content = o.toString();
    	}
    	return result;
	}
	
	public static JsonObject analytic_display_item(String[] status, String time_start, String time_stop) throws Exception {
		// select * from "Item" where (created > '2023-07-14 10:57:05' and created < '2023-07-14 14:36:06') and (status ilike 'success' or status ilike 'failed') order by created
		String text_time_0 = "true";
		if (time_start != null) {
			if (time_start.length() > 0) {
				text_time_0 = Item.wordCreated + " > '" + time_start + "'";
			}
		}
		
		String text_time_1 = "true";
		if (time_stop != null) {
			if (time_stop.length() > 0) {
				text_time_1 = Item.wordCreated + " < '" + time_stop + "'";
			}
		}
		
		String text_status = "true";
		if (status != null) {
			if (status.length > 0) {
				text_status = "";
				for (int i = 0; i < status.length; i++) {
					text_status += Item.wordStatus  + " ilike '" + status[i] + "'" + " or ";
				}
				text_status = text_status.substring(0, text_status.length() - " or ".length());
			}
		}
		
		String sql = "select * from " + ZConnector.Constant.SCHEMA + ".\"" + Item.class.getSimpleName() + "\" where (" + text_time_0 + " and " + text_time_1 + ") and (" + text_status + ") order by " + Item.wordCreated;
		
		return DB.executeList(sql).getAsJsonObject();
	}
	
    public static void main(String[] args) throws Exception {

    	/*long _24_hours = 48 * 60 * 60 * 1000;
    	
    	long ms_now = Calendar.getInstance().getTimeInMillis();
    	
    	Timestamp t_start = new Timestamp(ms_now - _24_hours);
    	
    	String text_start = new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(t_start);
    	
    	analytic_display_item(new String[] { "failed" }, text_start, null);*/
    	
    	// /workspaces/default/items?time_start=20230714152700&time_stop=20230715032700&status=queued,executing,failed,success,dismiss

    	/*byte[] bodyRaw = null;
		String method = "get";
		String path = "/workspaces/default/items?time_start=20230712000000";
		
		// query parameters
		HashMap<String, String> query = new HashMap<String, String>();
		try {
			String[] sp1 = path.split("\\?");
			if (sp1.length == 2) {
				path = sp1[0];
				String[] sp2 = sp1[1].split("&");
				for (int n=0;n<sp2.length;n++) {
					String[] sp3 = sp2[n].split("=");
					try {
						String key = sp3[0];
						String value = sp3[1];
						query.put(key, value);
					} catch (Exception ex) { }
				}
			}
		} catch (Exception ex) { ex.printStackTrace(); }
		ZResult result = com.pttdigital.wtransfer.ZAPIV3.process(path, method, query, null, bodyRaw);
    	
		OL.sln("result.content: " + result.content.length());*/
    }
}