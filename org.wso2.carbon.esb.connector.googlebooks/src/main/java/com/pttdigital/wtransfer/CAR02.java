package com.pttdigital.wtransfer;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZWorker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

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
	
	public static void main(String[] arg) throws Exception {
		for (;;) { loop(); Thread.sleep(2000); }
	}
	
	public static void loop() throws Exception {
		
		
		
		// now
		time_now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		
		// triggered schedules
		ArrayList<Schedule> triggered = getAllSchedules(); // getAllSchedules is just for test
		
		// list sites
		HashMap<String, Site> sites = mapSites();
		
		int sid_tmp = 580000;
		HashMap<Long, Session> mapSessions = new HashMap<Long, Session>(); 
		ArrayList<String> text_siteSource_sourceFolder = new ArrayList<String>();
		ArrayList<String> text_session_siteSource_sourceFolder = new ArrayList<String>();
		for (Schedule schedule : triggered) {
			System.out.println("triggered schedule: " + schedule.name + ", " + schedule.previousCheckpoint);
			
			Session session = new Session();
			session.id = sid_tmp++;
			session.schedule = schedule.name;
			session.description = "Auto-generated by " + schedule.name + ".";
			session.status = Session.Status.CREATED.toString();
			
			String sourceFolder = calculateSourceFolder(schedule);
			if (!text_siteSource_sourceFolder.contains(schedule.siteSource + ":" + sourceFolder)) {
				text_siteSource_sourceFolder.add(schedule.siteSource + ":" + sourceFolder);
			}
			
			text_session_siteSource_sourceFolder.add(session.id + ":" + schedule.siteSource + ":" + sourceFolder);
			
			mapSessions.put(session.id, session);
		}
		
		// post multiple sessions TODO
		
		HashMap<String, IFileServer.FileServer> mapServers = new HashMap<String, IFileServer.FileServer>();
		for (String s : text_siteSource_sourceFolder) {
			
			try {

				String[] sep = s.split(":");
				String siteName = sep[0];
				Site site = sites.get(siteName);
				if (!mapServers.containsKey(siteName)) {
					IFileServer.FileServer server = IFileServer.createServer(site);
					server.open();
					
					String folder = sep[1];
					JsonArray arr = server.listObjects(folder);
					
					ArrayList<Item.Info> infos = new ArrayList<Item.Info>();
					for (JsonElement e : arr) {
						Item.Info info = Item.Info.parse(e);
						if (info.isDirectory) {
							continue;
						}
						if (info.size > ZConnector.Constant.MAX_FILE_SIZE_BYTES) {
							continue;
						}
						
						// TODO eval filename to move
						
						
						infos.add(info);
					}
					
					
					mapServers.put(siteName, server);
				}
				
				System.out.println("arc: " + s);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		Set<String> ks = mapServers.keySet();
		for (String siteName : ks) {
			mapServers.get(siteName).close();
		}
		
		System.exit(0);
	}
}