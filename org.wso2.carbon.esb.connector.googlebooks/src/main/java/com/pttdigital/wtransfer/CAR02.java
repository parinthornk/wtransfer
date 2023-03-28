package com.pttdigital.wtransfer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
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

import org.wso2.carbon.esb.connector.Queue;
import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;
import org.wso2.carbon.esb.connector.ZWorker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.pttdigital.wtransfer.ImportV2.OL;

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

	private static HashMap<String, Schedule> mapSchedules() throws Exception {
		HashMap<String, Schedule> ret = new HashMap<String, Schedule>();
		JsonElement e1 = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules?available=true", "get", null, null);
		JsonArray arr1 = e1.getAsJsonObject().get("list").getAsJsonArray();
		for (JsonElement ei1 : arr1) {
			Schedule shedule = (Schedule) DB.parse(Schedule.class, ei1.getAsJsonObject());
			ret.put(shedule.name, shedule);
		}
		
		System.out.println("---> " + ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules?available=true");
		return ret;
	}
	
	private static ArrayList<Schedule> getTriggeredSchedules(HashMap<String, Schedule> allSchedules) throws Exception {
		
		// list triggered schedules
		JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/schedules/update-checkpoint", "post", null, null);
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
			OL.sln("map_server_map.put("+siteName+", "+server+");");
		}
	}

	private static void openServers(ArrayList<Schedule> triggered, HashMap<String, Site> allSites) throws Exception {
		map_server_clear();
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
	}

	private static void closeServers() {
		Set<String> ks = map_server_map.keySet();
		for (String siteName : ks) {
			try {
				map_server_map.get(siteName).close();
			} catch (Exception ex) { ex.printStackTrace(); }
		}
		map_server_map.clear();
	}
	
	private static ArrayList<Item> getNewlyCreatedItems(HashMap<String, Schedule> allSchedules, HashMap<String, Site> allSites, ArrayList<Schedule> triggered, HashMap<Long, Session> sessionsToCreated) { // TODO

		ArrayList<Item> itemsToCreated = new ArrayList<Item>();
		
		try {
			// ------------------------------------------------------------------------------------------------------------------------------------ //
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
			
			Set<String> ksToArchive = mapServerToArchive.keySet();
			for (String siteName : ksToArchive) {
				IFileServer.FileServer server = map_server_map.get(siteName);
				if (server == null) {
					
				} else {
					ArrayList<String> a = mapServerToArchive.get(siteName);
					for (String text : a) {
						
						String[] sep = text.split(":");
						String folder = sep[0];
						String fileName = sep[1];
						
						String folderArchive = folder + "/archive"; // TODO
						String fileNameArchive = fileName + ".arc";
						
						// TODO real archive concurrently
						if (!server.directoryExists(folderArchive)) {
							server.createDirectory(folderArchive);
							OL.sln("server.createDirectory("+folderArchive+");");
						}
						server.move(folder + "/" + fileName, folderArchive + "/" + fileNameArchive);
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
	


	private static HashMap<String, ArrayList<String>> constructItemsMessage(ArrayList<Item> itemsToCreated, HashMap<String, Schedule> allSchedules, HashMap<String, Site> allSites) throws Exception, IllegalArgumentException, IllegalAccessException {
		HashMap<String, ArrayList<String>> messages = new HashMap<String, ArrayList<String>>();
		for (Item item : itemsToCreated) {
			
			JsonObject o = new JsonObject();
			Session session = mapSessionsDescription.get(item.description);
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
			messages.get(schedule.name).add(o.toString());
			
		}
		return messages;
	}
	
	private static ArrayList<Item> getItemsToRetry() {
		// TODO Auto-generated method stub
		return new ArrayList<Item>();
	}
	
	public static class MyClass {
	    public String id;
	    public String senderchannel;
	    public String receivercomponent;
	    public String channel;
	    public String System;
	    public String scenario;
	    public String direction;
	    public String receivermessageprotocol;
	    public String receiverchannel;
	    public String ServerName;
	    public String ip;
	    public int Port;
	    public String UserName;
	    public String password;
	    public String SourceDir;
	}

	public static void main(String[] arg) throws Exception {
		
		if ("".length() == 0) {

			loop();
			return;
		}
		
		
		
		
		
		
		
		
		
		
		

		ArrayList<Schedule> al_sch = new ArrayList<Schedule>();
		JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/schedules", "get", null, null);
		JsonArray array = e.getAsJsonObject().get("list").getAsJsonArray();
		for (JsonElement ei : array) {
			Schedule schedule = (Schedule) DB.parse(Schedule.class, ei.getAsJsonObject());
			al_sch.add(schedule);
		}
		
		for (Schedule s : al_sch) {
			
			if (!s.name.startsWith("out-")) {
				continue;
			}
			
			JsonObject o = new JsonObject();
			
			String old = s.staticDirSource;
			
			
			if (old.startsWith("/OREO/")) {
				continue;
			}
			
			String newDirName = old;
			
			//newDirName = newDirName.replace("/WSO2_TEST", "");
			
			newDirName = "/OREO/" + newDirName;
			
			while (newDirName.startsWith("/")) { newDirName = newDirName.substring(1, newDirName.length()); }
			while (newDirName.endsWith("/")) { newDirName = newDirName.substring(0, newDirName.length() - 1); }
			while (newDirName.contains("//")) { newDirName = newDirName.replace("//", "/"); }
			newDirName = "/" + newDirName;
			
			o.addProperty("staticDirSource", newDirName);
			//o.addProperty("staticDirTarget", old);
			
			//o.addProperty("enabled", false);
			
			ZResult r = ZAPIV3.process("/workspaces/default/schedules/" + s.name, "patch", null, null, o.toString());
			OL.sln(r.content);
		}
		
		if ("".length() == 0) {
			return;
		}
		
		
		/*if ("".length() == 0) {
			for (int i=0;i<100;i++) {
				long t0 = System.currentTimeMillis();
				ZConnector.ZResult res = ZAPIV3.process("/workspaces/default/sites/dcloud-sftp/objects", "get", null, null, (byte[]) null);
				OL.sln(res.content);
				long t1 = System.currentTimeMillis();
				OL.sln("t1 - t0 = " + (t1 - t0));
			}
			
			
			
			return;
		}*/
		
		/*// create folders on dcloud sftp
		if ("".length() == 0) {
			JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/sites/dcloud-sftp", "get", null, null);
			Site site = (Site) DB.parse(Site.class, e.getAsJsonObject());
			IFileServer.FileServer server = IFileServer.createServer(site);
			server.open();
			JsonElement e2 = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/schedules", "get", null, null);
			JsonArray array = e2.getAsJsonObject().get("list").getAsJsonArray();
			for (JsonElement ei : array) {
				Schedule schedule = (Schedule) DB.parse(Schedule.class, ei.getAsJsonObject());
				if (!schedule.name.startsWith("out-")) {
					continue;
				}
				try {
					server.createDirectory(schedule.staticDirSource);
					OL.sln("xxx["+schedule.name+"], xxx["+true+"]");
				} catch (Exception ex) {
					OL.sln("xxx["+schedule.name+"], xxx["+false+"]");
				}
				
			}
			server.close();
			OL.sln(site.name);
		}*/
		
		
		
		/*if ("".length() == 0) {
			HashMap<String, String> x = new HashMap<String, String>();
			x.put("out-20001", "/Interface/DEV/PTT_FTPS/BankPayment_test/SCB/inbound/Payment_data");
			x.put("out-20002", "/Interface/DEV/AutoFaxOil/SapFax/CNDNDATA");
			x.put("out-20003", "/Interface/DEV/OR_FTPS/pis_testrose");
			x.put("out-20004", "/Interface/DEV/OR_PISMaster/Dev/usr/sap/interface/pttor/pis/outbound");
			x.put("out-20005", "/Interface/DEV/OR_FTPS/BankPayment_test/pttor/SCB/inbound/Payment_data");
			x.put("out-20006", "/Interface/DEV/PTT_FTPS/BankPayment_test/SCB/inbound/ETAXDOC");
			x.put("out-20007", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/ktb/outbound/report");
			x.put("out-20008", "/Interface/DEV/PTT_FTPS/BankStatement_test/ktb/outbound/report");
			x.put("out-20009", "/Interface/DEV/OR_FTPS/Test/usr/sap/interface/bankpayment/pttor/penalty");
			x.put("out-20010", "/Interface/DEV/PTT_FTPS/Test/usr/sap/interface/bankpayment/ptt/penalty");
			x.put("out-20011", "/Interface/DEV/EConfirm/");
			x.put("out-20012", "/Interface/DEV/OR_EConfirm/");
			x.put("out-20013", "/Interface/DEV/OR_SAPFax/data");
			x.put("out-20014", "/Interface/DEV/SAPFax/sapfax_service_pttplc_test");
			x.put("out-20015", "/Interface/DEV/EORDER/Test/usr/sap/interface/e-order/outbound/salesorderinfo");
			x.put("out-20016", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/scb/outbound/reject");
			x.put("out-20017", "/Interface/DEV/PTT_FTPS/BankStatement_test/scb/outbound/reject");
			x.put("out-20018", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20019", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20020", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20021", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/uob/outbound/report");
			x.put("out-20022", "/Interface/DEV/PTT_FTPS/BankStatement_test/uob/outbound/report");
			x.put("out-20023", "/Interface/DEV/EORDER/Test/usr/sap/interface/e-order/outbound/SAP_CONTRACT");
			x.put("out-20024", "/Interface/DEV/LAMS/LAMS_Test/Outbound/");
			x.put("out-20025", "/Interface/DEV/PTTCL_CL16/LAMS_Test/Outbound/");
			x.put("out-20026", "/Interface/DEV/PTTLAO_LA16/Outbound");
			x.put("out-20027", "/Interface/DEV/PTTRM_RM16/Outbound/");
			x.put("out-20028", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/ktb/outbound/backup");
			x.put("out-20029", "/Interface/DEV/PTT_FTPS/BankStatement_test/ktb/outbound/backup");
			x.put("out-20030", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/scb/outbound/backup");
			x.put("out-20031", "/Interface/DEV/PTT_FTPS/BankStatement_test/scb/outbound/backup");
			x.put("out-20032", "/Interface/DEV/BSA_FTP/BankPayment_bsa_test");
			x.put("out-20033", "/Interface/DEV/BSA_KTB/usr/sap/interface/bsa/bankpayment/ktb/outbound/Encrypted");
			x.put("out-20034", "/Interface/DEV/KTB/usr/sap/interface/bankpayment/ktb/outbound/Encrypted");
			x.put("out-20035", "/Interface/DEV/OR_FTP/bankpayment/pttor/ktb/outbound");
			x.put("out-20036", "/Interface/DEV/OR_KTB/usr/sap/interface/pttor/bankpayment/ktb/outbound/Encrypted");
			x.put("out-20037", "/Interface/DEV/PTT_FTP/bankpayment/ktb/outbound");
			x.put("out-20038", "/Interface/DEV/OR_FTPS/BankPayment_test/SCB/inbound/pttor/ETAXDOC/Cancel");
			x.put("out-20039", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/bbl/outbound/report");
			x.put("out-20040", "/Interface/DEV/PTT_FTPS/BankStatement_test/bbl/outbound/report");
			x.put("out-20041", "/Interface/DEV/AmazonWS/amazonwsftp_rolloutrose/INITIAL");
			x.put("out-20042", "/Interface/DEV/AmazonWS/amazonwsftp_rolloutrose/INITIAL");
			x.put("out-20043", "/Interface/DEV/B2BBunker/B2BBunker_test");
			x.put("out-20044", "/Interface/DEV/Chargeback/SSC_Chargeback_test");
			x.put("out-20045", "/Interface/DEV/CreditBureau/CreditBureau_dev");
			x.put("out-20046", "/Interface/DEV/DataLake/PTT-EDP-DEV/Inbound/SAP_FI");
			x.put("out-20047", "/Interface/DEV/DataLake/PTT-EDP-DEV/Inbound/SAP_FI");
			x.put("out-20048", "/Interface/DEV/EBankGuarantee/PTTDP_test/ptt_ebg/outbound");
			x.put("out-20049", "/Interface/DEV/EBilling/PTT-E-Billing_test");
			x.put("out-20050", "/Interface/DEV/GPSC_IntAudit/D:/P2PDataSource/SAP/DEV/");
			x.put("out-20051", "/Interface/DEV/GPSC_MRWeb/gpsc_mr2_test/AutoPrintFiles/SAP_GL_Form");
			x.put("out-20052", "/Interface/DEV/GSP/GSP_DS_TEST/DEV");
			x.put("out-20053", "/Interface/DEV/IFIX/ifix_dev");
			x.put("out-20054", "/Interface/DEV/ITCM/PTT_Contract_Management_test");
			x.put("out-20055", "/Interface/DEV/LIMS/PTTRTILIMS_test");
			x.put("out-20056", "/Interface/DEV/NGRAnywhere/PTT-NGRAnywhere_test");
			x.put("out-20057", "/Interface/DEV/O2C/O2C_Auto_MM_test");
			x.put("out-20058", "/Interface/DEV/OR_AmazonWS/amazonwsftp/INITIAL");
			x.put("out-20059", "/Interface/DEV/OR_AmazonWS/amazonwsftp/INITIAL");
			x.put("out-20060", "/Interface/DEV/OR_EV/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/OUTPUT");
			x.put("out-20061", "/Interface/DEV/OR_KALA/OR-KALA_Test");
			x.put("out-20062", "/Interface/DEV/OR_NewMarine/PTTOR-Marine_E-Order_test");
			x.put("out-20063", "/Interface/DEV/OR_RetailWorkTracking/PTTOR-Retail-Work-Tracking_Test");
			x.put("out-20064", "/Interface/DEV/OrderItem/ZPTT_ORDERITEM_test");
			x.put("out-20065", "/Interface/DEV/PDMS/SSC_Performance_Report_test/SVM");
			x.put("out-20066", "/Interface/DEV/PTTGRP_PISMaster/Dev/usr/sap/interface");
			x.put("out-20067", "/Interface/DEV/PTTRM/");
			x.put("out-20068", "/Interface/DEV/PTTRM/");
			x.put("out-20069", "/Interface/DEV/PTTRM_FTPS_custstmt/");
			x.put("out-20070", "/Interface/DEV/PTTRM_FTPS_custstmt/");
			x.put("out-20071", "/Interface/DEV/PTT_FTPS_ar/Accounting-AR_test");
			x.put("out-20072", "/Interface/DEV/PTT_FTPS_ar/Accounting-AR_test");
			x.put("out-20073", "/Interface/DEV/PTT_FTPS_ar/Accounting-AR_test");
			x.put("out-20074", "/Interface/DEV/PTT_FTPS_etax/PTT-eTax_Web/TransLogFiles");
			x.put("out-20075", "/Interface/DEV/PTT_FTPS_h2os01/Dev/usr/sap/interface");
			x.put("out-20076", "/Interface/DEV/PTT_FTPS_pttngv/excel_report_sap");
			x.put("out-20077", "/Interface/DEV/PTT_FTPS_relateparty/RelatedParty_test");
			x.put("out-20078", "/Interface/DEV/PTT_FTPS_tankis/Tankinspection_test");
			x.put("out-20079", "/Interface/DEV/PTT_FTP_bwflash/PTT_FLASH/HODEV");
			x.put("out-20080", "/Interface/DEV/PTT_FTP_bwflash/PTT_FLASH/HODEV");
			x.put("out-20081", "/Interface/DEV/SSC_CreditBalance/Accounting-AR_test/Credit-Balance/Background");
			x.put("out-20082", "/Interface/DEV/SSC_CreditBalance/Accounting-AR_test/Credit-Balance/Background");
			x.put("out-20083", "/Interface/DEV/SSC_CreditBalance/Accounting-AR_test/Credit-Balance/Background");
			x.put("out-20084", "/Interface/DEV/SSC_ReserveBadDebts/Accounting-AR_test/Reserve-BadDebts/Background");
			x.put("out-20085", "/Interface/DEV/SSC_ReserveBadDebts/Accounting-AR_test/Reserve-BadDebts/Background");
			x.put("out-20086", "/Interface/DEV/SSC_ReserveBadDebts/Accounting-AR_test/Reserve-BadDebts/Background");
			x.put("out-20087", "/Interface/DEV/SmartShelter/outBox");
			x.put("out-20088", "/Interface/DEV/SmartVendor/PTTDP_test/smart_vendor/outbound");
			x.put("out-20089", "/Interface/DEV/TSODataLake/PTT-EDP-DEV/Inbound");
			x.put("out-20090", "/Interface/DEV/TSODataLake/PTT-EDP-DEV/Inbound");
			x.put("out-20091", "/Interface/DEV/PTT_FTPS/BankPayment_test/SCB/inbound/ETAXDOC");
			x.put("out-20092", "/Interface/DEV/EBPP/Dev/usr/sap/interface/ebpp/outbound");
			x.put("out-20093", "/Interface/DEV/MOFI/mofi_dev/outbound/post_result/");
			x.put("out-20094", "/Interface/DEV/EBPP/Dev/usr/sap/interface/ebpp/outbound");
			x.put("out-20095", "/Interface/DEV/PTTDS_TASH505/sapoutboundorsrt2-dev");
			x.put("out-20096", "/Interface/DEV/TAS5441/sapoutbound");
			x.put("out-20097", "/Interface/DEV/TAS8101/QAS/OUTBOUND/SHIPMENT");
			x.put("out-20098", "/Interface/DEV/TAS8441/sapoutboundptt");
			x.put("out-20099", "/Interface/DEV/TASH102/H102-O");
			x.put("out-20100", "/Interface/DEV/TASH103/H103/INBOUND");
			x.put("out-20101", "/Interface/DEV/TASH104/outboundor");
			x.put("out-20102", "/Interface/DEV/TASH105/H105-O");
			x.put("out-20103", "/Interface/DEV/TASH140/sapoutbound5140");
			x.put("out-20104", "/Interface/DEV/TASH201/TEST-SAP_PI/H201");
			x.put("out-20105", "/Interface/DEV/TASH202/TEST-SAP_PI/H202");
			x.put("out-20106", "/Interface/DEV/TASH203/TEST-SAP_PI/H203");
			x.put("out-20107", "/Interface/DEV/TASH204/TEST-SAP_PI/H204");
			x.put("out-20108", "/Interface/DEV/TASH205/sapoutbound5205");
			x.put("out-20109", "/Interface/DEV/TASH240/sapoutbound5240");
			x.put("out-20110", "/Interface/DEV/TASH241/TEST-SAP_PI/H241");
			x.put("out-20111", "/Interface/DEV/TASH301/TEST-SAP_PI/H301");
			x.put("out-20112", "/Interface/DEV/TASH302/TEST-SAP_PI/H302");
			x.put("out-20113", "/Interface/DEV/TASH303/sapoutbound5303");
			x.put("out-20114", "/Interface/DEV/TASH340/TEST-SAP_PI/H340");
			x.put("out-20115", "/Interface/DEV/TASH401/H401-O");
			x.put("out-20116", "/Interface/DEV/TASH441/TEST-SAP_PI/H441");
			x.put("out-20117", "/Interface/DEV/TASH501/sapoutbound5501");
			x.put("out-20118", "/Interface/DEV/TASH502/TEST-SAP_PI/H502");
			x.put("out-20119", "/Interface/DEV/TASH503/TEST-SAP_PI/H503");
			x.put("out-20120", "/Interface/DEV/TASH504/sapoutbound5504");
			x.put("out-20121", "/Interface/DEV/TASH540/sapoutbound5540");
			x.put("out-20122", "/Interface/DEV/TASH541/sapoutbound5541");
			x.put("out-20123", "/Interface/DEV/TASK101/QAS/OUTBOUND/SHIPMENT/K101");
			x.put("out-20124", "/Interface/DEV/TASK305/QAS/OUTBOUND/SHIPMENT/K305");
			x.put("out-20125", "/Interface/DEV/TASK308/TASK308/TEST/outbound/shipment");
			x.put("out-20126", "/Interface/DEV/TASK402/PTTOR/DEV/Outbound/Shipment");
			x.put("out-20127", "/Interface/DEV/TASK441/sapoutboundor");
			x.put("out-20128", "/Interface/DEV/SSC_WorkFlowStatus/");
			x.put("out-20129", "/Interface/DEV/OR_FTPS/BankPayment_test/SCB/inbound/pttor/ETAXDOC");
			x.put("out-20130", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/bbl/outbound/backup");
			x.put("out-20131", "/Interface/DEV/PTT_FTPS/BankStatement_test/bbl/outbound/backup");
			x.put("out-20132", "/Interface/DEV/GPSC/Data_Test");
			x.put("out-20133", "/Interface/DEV/GPSC_PISMaster/GPSCGROUP_PIS_Test/PISData_SAP_Test");
			x.put("out-20134", "/Interface/DEV/GPSC_RPT/D:/SAP/");
			x.put("out-20135", "/Interface/DEV/GPSC_WFMS/GPSC_WFPNBI_dev/inbound");
			x.put("out-20136", "/Interface/DEV/HRAS/HRAIS_test");
			x.put("out-20137", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/BWH");
			x.put("out-20138", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/BWH");
			x.put("out-20139", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/BWH");
			x.put("out-20140", "/Interface/DEV/LNG_EISO/pttlngpis");
			x.put("out-20141", "/Interface/DEV/LNG_PIS/PTTLNG-PIS_Test");
			x.put("out-20142", "/Interface/DEV/NGD/PTTNGD/DEV/AX");
			x.put("out-20143", "/Interface/DEV/NewPISMaster/textfile");
			x.put("out-20144", "/Interface/DEV/OR_SFTP_conicle/upload/inbound_files");
			x.put("out-20145", "/Interface/DEV/PISMaster/Dev/usr/sap/interface");
			x.put("out-20146", "/Interface/DEV/SFTP_Conicle/upload/inbound_files");
			x.put("out-20147", "/Interface/DEV/WFMS/PTT-WorkforceManagement_Test");
			x.put("out-20148", "/Interface/DEV/PIMS/PTTPIMS_dev/inbound");
			x.put("out-20149", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/bay/outbound/backup");
			x.put("out-20150", "/Interface/DEV/PTT_FTPS/BankStatement_test/bay/outbound/backup");
			x.put("out-20151", "/Interface/DEV/PTT_FTPS/BankPayment_test/SCB/inbound/ETAXDOC/Cancel");
			x.put("out-20152", "/Interface/DEV/GSPSMP/PTT_GSP_SMP_dev/inbound");
			x.put("out-20153", "/Interface/DEV/HRAnalytics/LMS");
			x.put("out-20154", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/BWH");
			x.put("out-20155", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/BWH");
			x.put("out-20156", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/BWH");
			x.put("out-20157", "/Interface/DEV/NGR/PTT-NGRAnywhere_test");
			x.put("out-20158", "/Interface/DEV/OR_BPC/home/pod_orbpc/interface/ORBPC-C/outbound");
			x.put("out-20159", "/Interface/DEV/OR_BPC/home/pod_orbpc/interface/ORBPC-C/outbound");
			x.put("out-20160", "/Interface/DEV/OR_EPlanning/prod/OR_Planning_Analytics/Import");
			x.put("out-20161", "/Interface/DEV/OR_LPGDashboard/PTTOR-LPG-inventory-dashboard_Test");
			x.put("out-20162", "/Interface/DEV/OR_SmartAnalytic/PTTOR-APO-SmartAnalytic_Test/APO_Dev");
			x.put("out-20163", "/Interface/DEV/OrgDashboard/PTT-OrgStructureDashboard_test");
			x.put("out-20164", "/Interface/DEV/TSODataLake/PTT-EDP-DEV/Inbound/SAP_BW");
			x.put("out-20165", "/Interface/DEV/TSODataLake/PTT-EDP-DEV/Inbound/SAP_BW");
			x.put("out-20166", "/Interface/DEV/WFMS/PTT-WorkforceManagement_Test");
			x.put("out-20167", "/Interface/DEV/OR_FTPS/BankPayment_test/pttor/SCB/outbound/payment_data");
			x.put("out-20168", "/Interface/DEV/PTT_FTPS/BankPayment_test/SCB/outbound/payment_data");
			x.put("out-20169", "/Interface/DEV/OR_FTPS/BankPayment_test/SCB/inbound/pttor/ETAXDOC");
			x.put("out-20170", "/Interface/DEV/EORDER/Test/usr/sap/interface/e-order/outbound/SAP_CONTRACT");
			x.put("out-20171", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20172", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20173", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20174", "/Interface/DEV/OR_EBPP/Dev/usr/sap/interface/pttor/ebpp/outbound");
			x.put("out-20175", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/scb/outbound/report");
			x.put("out-20176", "/Interface/DEV/PTT_FTPS/BankStatement_test/scb/outbound/report");
			x.put("out-20177", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/uob/outbound/backup");
			x.put("out-20178", "/Interface/DEV/PTT_FTPS/BankStatement_test/uob/outbound/backup");
			x.put("out-20179", "/Interface/DEV/S2O/S2O/DEV/INV");
			x.put("out-20180", "/Interface/DEV/S2O/S2O/DEV/SO");
			x.put("out-20181", "/Interface/DEV/OR_FTPS/BankStatement_test/pttor/bay/outbound/report");
			x.put("out-20182", "/Interface/DEV/PTT_FTPS/BankStatement_test/bay/outbound/report");
			x.put("out-20183", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20184", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20185", "/Interface/DEV/Informatica/PTTHRANA_dev/inbound/HCM");
			x.put("out-20186", "/Interface/DEV/OR_FTPS/BankPayment_test/SCB/inbound/pttor/ETAXDOC");
			x.put("out-20187", "/Interface/DEV/EBPP/Dev/usr/sap/interface/ebpp/outbound");
			x.put("out-20188", "/Interface/DEV/PTT_FTPS/BankPayment_test/SCB/inbound/ETAXDOC");
			x.put("out-20189", "/Interface/DEV/OR_EBPP/Dev/usr/sap/interface/pttor/ebpp/outbound");
			x.put("out-20190", "/Interface/DEV/OR_WMSAMZDC/PTTOR-AMZ-DC_DEV");
			x.put("out-20191", "/Interface/DEV/OR_EBPP/Dev/usr/sap/interface/pttor/ebpp/outbound");
			
			HashMap<String, Schedule> map = new HashMap<String, Schedule>();
			JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/schedules", "get", null, null);
			JsonArray array = e.getAsJsonObject().get("list").getAsJsonArray();
			for (JsonElement ei : array) {
				Schedule schedule = (Schedule) DB.parse(Schedule.class, ei.getAsJsonObject());
				
				if (!schedule.name.startsWith("out-")) {
					continue;
				}
				
				String oldPath = schedule.staticDirSource;
				String newDirName = x.get(schedule.name);
				

				while (newDirName.startsWith("/")) { newDirName = newDirName.substring(1, newDirName.length()); }
				while (newDirName.endsWith("/")) { newDirName = newDirName.substring(0, newDirName.length() - 1); }
				while (newDirName.contains("//")) { newDirName = newDirName.replace("//", "/"); }
				newDirName = "/" + newDirName;
				
				OL.sln("old: " + oldPath);
				OL.sln("new: " + newDirName);
				
				// patch
				JsonObject o = new JsonObject();
				o.addProperty("staticDirSource", newDirName);
				ZResult r = ZAPIV3.process("/workspaces/default/schedules/" + schedule.name, "patch", null, null, o.toString());
				OL.sln(r.content);

				OL.sln("--------------------------------------------------------------------");
			}
		}*/
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*HashMap<String, Site> allSites = mapSites();
		HashMap<String, Schedule> allSchedules = mapSchedules();
		
		
		Set<String> ks = allSchedules.keySet();
		for (String schName : ks) {
			
		}
		
		
		for (int i=10001;i<=10183;i++) {
			//OL.sln(ks.contains("out-" + i));
			
			if (ks.contains("in-" + i)) {
				String sc_name = "in-" + i;
				Schedule sc = allSchedules.get(sc_name);
				OL.sln(sc.siteSource);
			} else {
				OL.sln("");
			}
		}*/
		

		/*Set<String> ks = allSchedules.keySet();
		for (String schName : ks) {
			Schedule schedule = allSchedules.get(schName);
			if (!schedule.name.startsWith("in-")) {
				continue;
			}
			

			OL.sln("name[" + schedule.name + "], siteSource["+schedule.siteSource+"], staticDirSource[" + schedule.staticDirSource + "].");
			
			
			
			IFileServer.FileServer source = IFileServer.createServer(allSites.get(schedule.siteSource));
			
			try {

				source.open();
				
				// /WSO2_TEST
				//source.createDirectory(schedule.staticDirSource);
				//OL.sln("created.");
				
				
				OL.sln("\tsource.directoryExists(): " + source.directoryExists(schedule.staticDirSource));
			} catch (Exception ex) {
				OL.sln("\tError: " + ex);
			}

			OL.sln("");
			OL.sln("");
			
			source.close();
		}*/
		
		
		/*// generate folders on sftp dcloud
		HashMap<String, Site> allSites = mapSites();
		HashMap<String, Schedule> allSchedules = mapSchedules();
		HashMap<String, ArrayList<String>> siteandfolders = new HashMap<String, ArrayList<String>>();
		Set<String> ks = allSchedules.keySet();
		for (String scheduleName : ks) {
			if (!scheduleName.startsWith("out-")) {
				continue;
			}
			Site s = allSites.get(allSchedules.get(scheduleName).siteSource);
			String folder = allSchedules.get(scheduleName).staticDirTarget;
			if (!siteandfolders.containsKey(s.name)) {
				siteandfolders.put(s.name, new ArrayList<String>());
			}
			if (!siteandfolders.get(s.name).contains(folder)) {
				siteandfolders.get(s.name).add(folder);
			}
		}
		Set<String> ks2 = siteandfolders.keySet();
		for (String siteName : ks2) {
			Site site = allSites.get(siteName);
			IFileServer.FileServer server = IFileServer.createServer(site);
			server.open();
			ArrayList<String> arfs = siteandfolders.get(siteName);
			for (String folder : arfs) {
				OL.sln("------------------------------------------------------------");
				boolean created = false;
				try {
					server.createDirectory(folder);
					created = true;
				} catch (Exception ex) { }
				OL.sln("created["+created+"], folder["+folder+"]");
			}
			OL.sln("------------------------------------------------------------");
			server.close();
		}*/
	}
	
	private static HashMap<String, Session> mapSessionsDescription = new HashMap<String, Session>();
	
	private static HashMap<Long, Session> instantiateNewSessions(ArrayList<Schedule> triggered) throws NoSuchFieldException, Exception {

		
		// create sessions
		HashMap<Long, Session> sessionsToCreated = new HashMap<Long, Session>();
		long sid = 560000;
		for (Schedule schedule : triggered) {
			IFileServer.FileServer server = map_server_map.get(schedule.siteSource);
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
				OL.sln("mapSessionsDescription.put("+s.description+", "+s+");");
			}
		}
		
		return sessionsToCreated;
	}

	private static void enqueue(HashMap<String, ArrayList<String>> messages) throws Exception {
		// TODO sort message by what???
		if (messages.size() > 0) {
			try {
				Queue.Publisher.open();
				Set<String> ks = messages.keySet();
				for (String scheduleName : ks) {
					ArrayList<String> list = messages.get(scheduleName);
					for (String text : list) {
						Queue.Publisher.enqueue(scheduleName, text);
					}
				}
			} catch (Exception ex) { }
			Queue.Publisher.close();
		}
	}
	
	public static void loop() throws Exception {
		
		System.out.println("------------------------------------------------------------------------------------");
		
		try {

			// now
			time_now = new Timestamp(Calendar.getInstance().getTimeInMillis());
			
			// all schedules
			HashMap<String, Schedule> allSchedules = mapSchedules();
			System.out.println("allSchedules: " + allSchedules.size());
			
			// all sites
			HashMap<String, Site> allSites = mapSites();
			System.out.println("allSites: " + allSites.size());
			
			/*// all existing sessions for what? // TODO, limit only previous 24 hour?
			HashMap<Long, Session> allExistingSessions = mapSessions();
			System.out.println("allExistingSessions: " + allExistingSessions.size());*/
			
			// triggered schedules
			//ArrayList<Schedule> triggered = getTriggeredSchedules(allSchedules);
			ArrayList<Schedule> triggered = new ArrayList<Schedule>(); triggered.add(allSchedules.get("out-20001"));
			System.out.println("triggered: " + triggered.size());
			
			// open server connections concurrently
			openServers(triggered, allSites);
			
			// new sessions
			HashMap<Long, Session> newCreatedSessions = instantiateNewSessions(triggered);
			System.out.println("sessionsToCreated: " + newCreatedSessions.size());
			
			// list items to be created
			ArrayList<Item> itemsToCreated = getNewlyCreatedItems(allSchedules, allSites, triggered, newCreatedSessions);
			System.out.println("itemsToCreated: " + itemsToCreated.size());
			
			/*// list items in retry mode, TODO
			ArrayList<Item> itemsToRetry = getItemsToRetry();
			System.out.println("itemsToRetry: " + itemsToRetry.size());*/
			
			// construct message for all items
			HashMap<String, ArrayList<String>> messages = constructItemsMessage(itemsToCreated, allSchedules, allSites);
			
			// enqueue
			enqueue(messages);
			
		} catch (Exception ex) { ex.printStackTrace(); }
		
		// cleanup servers
		closeServers();
		
		System.out.println("------------------------------------------------------------------------------------");
	}
}