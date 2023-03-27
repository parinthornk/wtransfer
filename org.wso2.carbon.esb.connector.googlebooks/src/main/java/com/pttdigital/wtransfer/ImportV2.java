package com.pttdigital.wtransfer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZWorker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class ImportV2 {
	
	public static class ExportedObject {
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
	
	public static class OL {
		
		public static void sln(Object o) {
			System.out.println(o);
		}
		
		public static boolean isNullOrEmpty(String s) {
			if (s == null) {
				return true;
			}
			return s.length() == 0;
		}
		
		private static HashMap<String, String> tmpo = new HashMap<String, String>();
		public static void printAllTmpo() {
			// cx.put("xxxxx", "xxxxx");
			Set<String> ks = tmpo.keySet();
			for (String s : ks) {
				OL.sln("cx.put(\"" + s + "\", \"" + tmpo.get(s) + "\");");
			}
		}
		
		public static String getIp(String hostName) throws UnknownHostException {
			if (OL.isNullOrEmpty(hostName)) {
				return null;
			}
			
			
			/*HashMap<String, String> cx = new HashMap<String, String>();
			cx.put("172.25.72.11", "172.25.72.11");
			cx.put("172.25.2.21", "172.25.2.21");
			cx.put("10.195.2.19", "10.195.2.19");
			cx.put("10.237.103.52", "10.237.103.52");
			cx.put("10.237.103.53", "10.237.103.53");
			cx.put("10.237.103.50", "10.237.103.50");
			cx.put("10.237.103.51", "10.237.103.51");
			cx.put("172.25.198.21", "172.25.198.21");
			cx.put("172.25.68.21", "172.25.68.21");
			cx.put("10.109.100.15", "10.109.100.15");
			cx.put("172.25.204.121", "172.25.204.121");
			cx.put("172.25.70.21", "172.25.70.21");
			cx.put("10.120.17.25", "10.120.17.25");
			cx.put("10.197.248.25", "10.197.248.25");
			cx.put("172.25.202.21", "172.25.202.21");
			cx.put("10.120.20.75", "10.120.20.75");
			cx.put("172.17.15.39", "172.17.15.39");
			cx.put("172.25.74.21", "172.25.74.21");
			cx.put("172.25.8.35", "172.25.8.35");
			cx.put("10.237.103.54", "10.237.103.54");
			cx.put("172.21.4.21", "172.21.4.21");
			cx.put("172.25.130.21", "172.25.130.21");
			cx.put("172.25.12.35", "172.25.12.35");
			cx.put("172.25.66.11", "172.25.66.11");
			cx.put("172.25.132.21", "172.25.132.21");
			cx.put("172.21.2.21", "172.21.2.21");
			cx.put("172.25.136.21", "172.25.136.21");
			cx.put("172.25.134.21", "172.25.134.21");
			if ("".length() == 0) {// TODO, change 0 to 1
				return cx.get(hostName);
			}*/
			
			
			
			
			
			
			
			
			
			
			
			String ret = InetAddress.getByName(hostName).getHostAddress();
			if (!tmpo.containsKey(ret)) {
				tmpo.put(hostName, ret);
			}
			
			return ret;
		}
		
		public static String base64Encode(String text) {
			return Base64.getEncoder().encodeToString(text.getBytes());
		}
		
		public static String base64Decode(String base64) {
			return new String(Base64.getDecoder().decode(base64));
		}

		public static void fileWrite(String path, String content) throws Exception {
			Files.write(Paths.get(path), content.getBytes());
		}
	}
	
	
	private static HashMap<String, ArrayList<ImportV2.ExportedObject>> mapUniqueSites = new HashMap<String, ArrayList<ImportV2.ExportedObject>>();
	private static HashMap<String, String> mapUniqueSitesId = new HashMap<String, String>();
	
	private static ArrayList<ImportV2.ExportedObject> jsonArrayToExportedObjects(JsonArray array) throws NoSuchFieldException, Exception {

		ArrayList<ImportV2.ExportedObject> a = new ArrayList<ImportV2.ExportedObject>();
		for (JsonElement element : array) {
			ImportV2.ExportedObject m = (ImportV2.ExportedObject) DB.parse(ImportV2.ExportedObject.class, element.getAsJsonObject());
			
			// try resolve ip if empty
			if (OL.isNullOrEmpty(m.ip)) { try { m.ip = OL.getIp(m.ServerName); } catch (Exception ex) { } }
			
			if (m.Port < 0) {
				continue;
			}
			
			if (OL.isNullOrEmpty(m.UserName)) {
				continue;
			}
			
			if (OL.isNullOrEmpty(m.password)) {
				continue;
			}
			
			a.add(m);
			
		}
		return a;
	}
	
	public static void loadUniqueSitesFromJson() throws NoSuchFieldException, Exception {
		
		mapUniqueSites.clear();
		mapUniqueSitesId.clear();
		
		JsonArray arrayIn = new Gson().fromJson(Files.readString(Path.of(filePathInbound)), JsonArray.class);
		JsonArray arrayOut = new Gson().fromJson(Files.readString(Path.of(filePathOutbound)), JsonArray.class);
		
		
		JsonArray array = new JsonArray();
		for (JsonElement element : arrayIn) { array.add(element); }
		for (JsonElement element : arrayOut) { array.add(element); }
		
		
		
		ArrayList<ImportV2.ExportedObject> a = jsonArrayToExportedObjects(array);
		
		
		mapUniqueSites = new HashMap<String, ArrayList<ImportV2.ExportedObject>>();
		for (ImportV2.ExportedObject x : a) {
			String unique = OL.base64Encode(x.ip) + ":" + x.Port + ":" + OL.base64Encode(x.UserName) + ":" + OL.base64Encode(x.password);
			if (!mapUniqueSites.containsKey(unique)) {
				mapUniqueSites.put(unique, new ArrayList<ImportV2.ExportedObject>());
			}
			mapUniqueSites.get(unique).add(x);
		}

		OL.sln("------------------------------------------------------------------------------------");
		// site-30001
		int site_id = 30001;
		Set<String> ks = mapUniqueSites.keySet();
		for (String unique : ks) {
			
			String[] split = unique.split(":");
			String ip = OL.base64Decode(split[0]);
			int port = Integer.parseInt(split[1]);
			String username = OL.base64Decode(split[2]);
			String password = OL.base64Decode(split[3]);
			
			ArrayList<ImportV2.ExportedObject> ar = mapUniqueSites.get(unique);
			
			
			//mapUniqueSitesId
			
			int sid = site_id;
			mapUniqueSitesId.put(unique, "site-" + sid);
			site_id++;
			
			
			/*OL.sln("unique  : " + unique);
			OL.sln("site_id : " + sid);
			OL.sln("ip      : " + ip);
			OL.sln("port    : " + port);
			OL.sln("username: " + username);
			OL.sln("password: " + password);
			OL.sln("total   : " + mapUniqueSites.get(unique).size());
			
			for (ImportV2.ExportedObject ob : ar) {
				OL.sln("\t" + ob.id);
			}
			
			OL.sln("------------------------------------------------------------------------------------");*/
		}
		OL.sln("unique sites: " + mapUniqueSites.size());
	}

	private static void listSchedules() throws NoSuchFieldException, Exception {
		//OL.sln("list inbound schedules (legacy ---> WSO2 ---> dcloud)");

		if ("".length() == 0) {
			JsonArray array = new Gson().fromJson(Files.readString(Path.of(filePathInbound)), JsonArray.class);

			ArrayList<ImportV2.ExportedObject> a = jsonArrayToExportedObjects(array);
			
			
			JsonArray p = new JsonArray();
			for (ImportV2.ExportedObject schedule : a) {
				
				String site = getSiteFromSchedule(schedule);
				
				//what_should_be_target_folder_in_dcloud?___should_it_name_after_schedule?(); // TODO
				
				//OL.sln("schedule[" + schedule.id + "]: [" + mapUniqueSitesId.get(site) + " -> wso2 -> aws-or-sftp], from legacy source folder[" + schedule.SourceDir + "] to aws-or-sftp folder[" + schedule.SourceDir + "].");
				
				String sourceSite = mapUniqueSitesId.get(site);
				String staticDirSource = schedule.SourceDir;
				String targetSite = "aws-or-sftp";
				String staticDirTarget = schedule.SourceDir;
				
				Schedule sg = new Schedule();
				sg.name = schedule.id;
				sg.siteSource = sourceSite;
				sg.staticDirSource = staticDirSource;
				sg.siteTarget = targetSite;
				sg.staticDirTarget = staticDirTarget;
				sg.enabled = true;
				sg.useDynamicDirSource = false;
				sg.useDynamicDirTarget = false;
				sg.retryCount = 10;
				sg.retryIntervalMs = 300 * 1000;
				sg.workerThreads = 5;
				
				p.add(new Gson().fromJson(DB.toJsonString(sg), JsonObject.class));
			}
			OL.sln(p);
			OL.fileWrite("t1.json", p.toString());
		}
		
		if ("".length() == 0) {
			JsonArray array = new Gson().fromJson(Files.readString(Path.of(filePathOutbound)), JsonArray.class);

			ArrayList<ImportV2.ExportedObject> a = jsonArrayToExportedObjects(array);

			JsonArray p = new JsonArray();
			for (ImportV2.ExportedObject schedule : a) {
				
				String site = getSiteFromSchedule(schedule);
				
				//OL.sln("schedule[" + schedule.id + "]: [dcloud -> wso2 -> " + mapUniqueSitesId.get(site) + "], from dcloud source folder[" + schedule.SourceDir + "] to legacy target folder[" + schedule.SourceDir + "].");
				

				String sourceSite = "dcloud-sftp";
				String staticDirSource = schedule.SourceDir;
				String targetSite = mapUniqueSitesId.get(site);
				String staticDirTarget = schedule.SourceDir;
				
				Schedule sg = new Schedule();
				sg.name = schedule.id;
				sg.siteSource = sourceSite;
				sg.staticDirSource = staticDirSource;
				sg.siteTarget = targetSite;
				sg.staticDirTarget = staticDirTarget;
				sg.enabled = true;
				sg.useDynamicDirSource = false;
				sg.useDynamicDirTarget = false;
				sg.retryCount = 10;
				sg.retryIntervalMs = 300 * 1000;
				sg.workerThreads = 5;
				
				p.add(new Gson().fromJson(DB.toJsonString(sg), JsonObject.class));
			}
			OL.sln(p);
			OL.fileWrite("t2.json", p.toString());
		}
		
	}

	private static String getSiteFromSchedule(ExportedObject schedule) throws Exception {
		Set<String> ks = mapUniqueSites.keySet();
		for (String unique : ks) {
			ArrayList<ExportedObject> a = mapUniqueSites.get(unique);
			for (ExportedObject o : a) {
				if (o.id.equals(schedule.id)) {
					return unique;
				}
			}
		}
		throw new Exception("Cannot find site for schedule: " + schedule.id);
	}

	private static void post_sites_to_db() throws Exception {
		
		/*// dcloud
		Site siteDcloud = new Site();
		siteDcloud.name = "dcloud-sftp";
		siteDcloud.port = 22;
		siteDcloud.host = "10.120.23.80";
		siteDcloud.username = "sftp_wso2t_oreo";
		siteDcloud.password = "OReoWSO2Dev#23";
		siteDcloud.protocol = "sftp";
		Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/default/sites", "post", null, new Gson().fromJson(DB.toJsonString(siteDcloud), JsonElement.class));
		System.out.println("siteDcloud.name: " + siteDcloud.name);*/
		
		/*// aws-sftp
		Site siteDcloud = new Site();
		siteDcloud.name = "aws-or-sftp";
		siteDcloud.protocol = "sftp";
		Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/default/sites", "post", null, new Gson().fromJson(DB.toJsonString(siteDcloud), JsonElement.class));
		System.out.println("siteDcloud.name: " + siteDcloud.name);*/
		
		/*// TODO Auto-generated method stub
		Set<String> ks = mapUniqueSitesId.keySet();
		for (String k : ks) {
			String siteName = mapUniqueSitesId.get(k);
			
			String[] split = k.split(":");
			String ip = OL.base64Decode(split[0]);
			int port = Integer.parseInt(split[1]);
			String username = OL.base64Decode(split[2]);
			String password = OL.base64Decode(split[3]);
			
			Site site = new Site();
			site.name = siteName;
			site.port = port;
			site.host = ip;
			site.username = username;
			site.password = password;
			
			if (site.port == 22) {
				site.protocol = "sftp";
			} else {
				site.protocol = "ftp";
			}
			
			Client.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/default/sites", "post", null, new Gson().fromJson(DB.toJsonString(site), JsonElement.class));
			System.out.println("site.name: " + site.name);
		}*/
	}

	private static final String filePathInbound = "C:\\Users\\parin\\Documents\\oreo-ftp\\version 2\\simp-ListPOD-Inbound.json";
	private static final String filePathOutbound = "C:\\Users\\parin\\Documents\\oreo-ftp\\version 2\\simp-ListPOD-Outbound.json";
	
	public static void main(String[] args) throws NoSuchFieldException, Exception {
		
		/*String h = "{\"name\":\"in-10004\",\"enabled\":true,\"useDynamicDirSource\":false,\"useDynamicDirTarget\":false,\"retryCount\":10,\"retryIntervalMs\":300000,\"workerThreads\":5,\"sourceSite\":\"site-30004\",\"staticDirSource\":\"/home/pod_orbpc/interface/ORBPC-C/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/home/pod_orbpc/interface/ORBPC-C/inbound\"}";
		

		Schedule sc2 = (Schedule) DB.parse(Schedule.class, new Gson().fromJson(h, JsonObject.class));
		
		
		OL.sln(sc2);
		
		if ("".length() == 0) {
			return;
		}*/
		
		//loadUniqueSitesFromJson();
		
		
		
		
		//post_sites_to_db();
		
		
		//OL.sln("====================================================================================");
		
		//listSchedules();
		
		
		
		/*String all = "[{\"name\":\"in-10004\",\"enabled\": true,\"useDynamicDirSource\": false,\"useDynamicDirTarget\": false,\"sourceSite\":\"site-30004\",\"staticDirSource\":\"/home/pod_orbpc/interface/ORBPC-C/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/home/pod_orbpc/interface/ORBPC-C/inbound\"},{\"name\":\"in-10005\",\"sourceSite\":\"site-30004\",\"staticDirSource\":\"/home/pod_orbpc/interface/ORBPC-C/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/home/pod_orbpc/interface/ORBPC-C/inbound\"},{\"name\":\"in-10014\",\"sourceSite\":\"site-30010\",\"staticDirSource\":\"/pricing/Data\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/pricing/Data\"},{\"name\":\"in-10018\",\"sourceSite\":\"site-30061\",\"staticDirSource\":\"/BW/PTT_SM/DataDEV/MAIN/CSV\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/BW/PTT_SM/DataDEV/MAIN/CSV\"},{\"name\":\"in-10019\",\"sourceSite\":\"site-30061\",\"staticDirSource\":\"/BW/PTT_SM/DataDEV/MAIN/CSV\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/BW/PTT_SM/DataDEV/MAIN/CSV\"},{\"name\":\"in-10022\",\"sourceSite\":\"site-30055\",\"staticDirSource\":\"/Inbound/0083\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Inbound/0083\"},{\"name\":\"in-10023\",\"sourceSite\":\"site-30055\",\"staticDirSource\":\"/Inbound/0083\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Inbound/0083\"},{\"name\":\"in-10030\",\"sourceSite\":\"site-30009\",\"staticDirSource\":\"/LAMS_Test/Inbound/0396\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/LAMS_Test/Inbound/0396\"},{\"name\":\"in-10031\",\"sourceSite\":\"site-30009\",\"staticDirSource\":\"/LAMS_Test/Inbound/0396\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/LAMS_Test/Inbound/0396\"},{\"name\":\"in-10032\",\"sourceSite\":\"site-30037\",\"staticDirSource\":\"/Inbound/0088\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Inbound/0088\"},{\"name\":\"in-10033\",\"sourceSite\":\"site-30037\",\"staticDirSource\":\"/Inbound/0088\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Inbound/0088\"},{\"name\":\"in-10039\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/bankstatement/uob/inbound/Decrypted\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/bankstatement/uob/inbound/Decrypted\"},{\"name\":\"in-10040\",\"sourceSite\":\"site-30013\",\"staticDirSource\":\"/localcollect/ManualDownload/lcc571format\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/localcollect/ManualDownload/lcc571format\"},{\"name\":\"in-10046\",\"sourceSite\":\"site-30063\",\"staticDirSource\":\"/202/Outbound/ZS4PIFII010\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/202/Outbound/ZS4PIFII010\"},{\"name\":\"in-10047\",\"sourceSite\":\"site-30063\",\"staticDirSource\":\"/202/Outbound/ZS4PIFII010\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/202/Outbound/ZS4PIFII010\"},{\"name\":\"in-10050\",\"sourceSite\":\"site-30040\",\"staticDirSource\":\"/amazonwsftp/GOOD_RECEIPT\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/amazonwsftp/GOOD_RECEIPT\"},{\"name\":\"in-10051\",\"sourceSite\":\"site-30040\",\"staticDirSource\":\"/amazonwsftp/GOOD_RECEIPT\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/amazonwsftp/GOOD_RECEIPT\"},{\"name\":\"in-10052\",\"sourceSite\":\"site-30028\",\"staticDirSource\":\"/etax/DEV/sap/FIAR/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/etax/DEV/sap/FIAR/inbound\"},{\"name\":\"in-10053\",\"sourceSite\":\"site-30028\",\"staticDirSource\":\"/etax/DEV/sap/FIAP/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/etax/DEV/sap/FIAP/inbound\"},{\"name\":\"in-10056\",\"sourceSite\":\"site-30056\",\"staticDirSource\":\"/EVChargingStation/DEVELOPMENT/STATUS_BILLING/INPUT\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/EVChargingStation/DEVELOPMENT/STATUS_BILLING/INPUT\"},{\"name\":\"in-10059\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/pttor/bankstatement/bbl/inbound/Decrypted\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/pttor/bankstatement/bbl/inbound/Decrypted\"},{\"name\":\"in-10068\",\"sourceSite\":\"site-30015\",\"staticDirSource\":\"/Dev/usr/sap/interface/ebpp/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Dev/usr/sap/interface/ebpp/inbound\"},{\"name\":\"in-10069\",\"sourceSite\":\"site-30015\",\"staticDirSource\":\"/Dev/usr/sap/interface/ebpp/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Dev/usr/sap/interface/ebpp/inbound\"},{\"name\":\"in-10080\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/bankstatement/bay/inbound/Decrypted\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/bankstatement/bay/inbound/Decrypted\"},{\"name\":\"in-10081\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/bankstatement/bbl/inbound/Encrypted\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/bankstatement/bbl/inbound/Encrypted\"},{\"name\":\"in-10086\",\"sourceSite\":\"site-30025\",\"staticDirSource\":\"/mofi_dev/inbound/post/0010\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/mofi_dev/inbound/post/0010\"},{\"name\":\"in-10087\",\"sourceSite\":\"site-30025\",\"staticDirSource\":\"/mofi_dev/inbound/post/0010\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/mofi_dev/inbound/post/0010\"},{\"name\":\"in-10090\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/pttor/bankstatement/uob/inbound/Decrypted\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/pttor/bankstatement/uob/inbound/Decrypted\"},{\"name\":\"in-10093\",\"sourceSite\":\"site-30032\",\"staticDirSource\":\"/MR_Web_Report_testrose\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/MR_Web_Report_testrose\"},{\"name\":\"in-10104\",\"sourceSite\":\"site-30015\",\"staticDirSource\":\"/Dev/usr/sap/interface/bi/apo/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Dev/usr/sap/interface/bi/apo/inbound\"},{\"name\":\"in-10109\",\"sourceSite\":\"site-30015\",\"staticDirSource\":\"/Dev/usr/sap/interface/pttor/ebpp/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Dev/usr/sap/interface/pttor/ebpp/inbound\"},{\"name\":\"in-10110\",\"sourceSite\":\"site-30015\",\"staticDirSource\":\"/Dev/usr/sap/interface/pttor/ebpp/inbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/Dev/usr/sap/interface/pttor/ebpp/inbound\"},{\"name\":\"in-10117\",\"sourceSite\":\"site-30011\",\"staticDirSource\":\"/HealthCheckData/Development/Corporate/Upload CSV\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/HealthCheckData/Development/Corporate/Upload CSV\"},{\"name\":\"in-10118\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/pcard/bbl/inbound/Decrypted/\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/pcard/bbl/inbound/Decrypted/\"},{\"name\":\"in-10121\",\"sourceSite\":\"site-30033\",\"staticDirSource\":\"/PTT-OrgStructureDashboard_test/JE-InboundSAP\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTT-OrgStructureDashboard_test/JE-InboundSAP\"},{\"name\":\"in-10126\",\"sourceSite\":\"site-30054\",\"staticDirSource\":\"/usr/sap/interface/pttor/bankstatement/bay/inbound/Decrypted\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/usr/sap/interface/pttor/bankstatement/bay/inbound/Decrypted\"},{\"name\":\"in-10127\",\"sourceSite\":\"site-30056\",\"staticDirSource\":\"/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/INPUT\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/INPUT\"},{\"name\":\"in-10132\",\"sourceSite\":\"site-30047\",\"staticDirSource\":\"/SSC_Performance_Report_test/SSC/CSV\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/SSC_Performance_Report_test/SSC/CSV\"},{\"name\":\"in-10133\",\"sourceSite\":\"site-30059\",\"staticDirSource\":\"/PTT_MIDServer_Dev01/export/all_inactive_cases\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTT_MIDServer_Dev01/export/all_inactive_cases\"},{\"name\":\"in-10142\",\"sourceSite\":\"site-30064\",\"staticDirSource\":\"/PTT-WorkforceManagement_Test/Outbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTT-WorkforceManagement_Test/Outbound\"},{\"name\":\"in-10149\",\"sourceSite\":\"site-30061\",\"staticDirSource\":\"/PTT_FLASH/FlashDataDEV\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTT_FLASH/FlashDataDEV\"},{\"name\":\"in-10150\",\"sourceSite\":\"site-30061\",\"staticDirSource\":\"/PTT_FLASH/FlashDataDEV\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTT_FLASH/FlashDataDEV\"},{\"name\":\"in-10151\",\"sourceSite\":\"site-30005\",\"staticDirSource\":\"/inBox\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/inBox\"},{\"name\":\"in-10158\",\"sourceSite\":\"site-30040\",\"staticDirSource\":\"/amazonwsftp_rolloutrose/GOOD_RECEIPT\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/amazonwsftp_rolloutrose/GOOD_RECEIPT\"},{\"name\":\"in-10159\",\"sourceSite\":\"site-30040\",\"staticDirSource\":\"/amazonwsftp_rolloutrose/GOOD_RECEIPT\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/amazonwsftp_rolloutrose/GOOD_RECEIPT\"},{\"name\":\"in-10162\",\"sourceSite\":\"site-30013\",\"staticDirSource\":\"/localcollect/AutoFileFromSCB\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/localcollect/AutoFileFromSCB\"},{\"name\":\"in-10163\",\"sourceSite\":\"site-30013\",\"staticDirSource\":\"/localcollect/AutoFileFromSCB\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/localcollect/AutoFileFromSCB\"},{\"name\":\"in-10164\",\"sourceSite\":\"site-30013\",\"staticDirSource\":\"/localcollect/ManualDownload/lcc256format\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/localcollect/ManualDownload/lcc256format\"},{\"name\":\"in-10171\",\"sourceSite\":\"site-30045\",\"staticDirSource\":\"/PTTOR-Excise_Tax_Lube_test/upload\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTTOR-Excise_Tax_Lube_test/upload\"},{\"name\":\"in-10172\",\"sourceSite\":\"site-30045\",\"staticDirSource\":\"/PTTOR-Excise_Tax_Lube_test/upload\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTTOR-Excise_Tax_Lube_test/upload\"},{\"name\":\"in-10173\",\"sourceSite\":\"site-30045\",\"staticDirSource\":\"/PTTOR-Excise_Tax_Lube_test/upload\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTTOR-Excise_Tax_Lube_test/upload\"},{\"name\":\"in-10176\",\"sourceSite\":\"site-30013\",\"staticDirSource\":\"/localcollect/pttor/AutoFileFromSCB\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/localcollect/pttor/AutoFileFromSCB\"},{\"name\":\"in-10177\",\"sourceSite\":\"site-30013\",\"staticDirSource\":\"/localcollect/pttor/AutoFileFromSCB\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/localcollect/pttor/AutoFileFromSCB\"},{\"name\":\"in-10183\",\"sourceSite\":\"site-30042\",\"staticDirSource\":\"/PTTRMIS_TEST/KM/Outbound\",\"targetSite\":\"aws-or-sftp\",\"staticDirTarget\":\"/PTTRMIS_TEST/KM/Outbound\"},{\"name\":\"out-20001\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/Payment_data\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/Payment_data\"},{\"name\":\"out-20002\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/SapFax/CNDNDATA\",\"targetSite\":\"site-30017\",\"staticDirTarget\":\"/SapFax/CNDNDATA\"},{\"name\":\"out-20003\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/pis_testrose\",\"targetSite\":\"site-30039\",\"staticDirTarget\":\"/pis_testrose\"},{\"name\":\"out-20004\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/pttor/pis/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/pttor/pis/outbound\"},{\"name\":\"out-20005\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/pttor/SCB/inbound/Payment_data\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/pttor/SCB/inbound/Payment_data\"},{\"name\":\"out-20006\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/ETAXDOC\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/ETAXDOC\"},{\"name\":\"out-20007\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/ktb/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/ktb/outbound/report\"},{\"name\":\"out-20008\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/ktb/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/ktb/outbound/report\"},{\"name\":\"out-20011\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30027\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20012\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30027\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20015\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Test/usr/sap/interface/e-order/outbound/salesorderinfo\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Test/usr/sap/interface/e-order/outbound/salesorderinfo\"},{\"name\":\"out-20016\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/scb/outbound/reject\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/scb/outbound/reject\"},{\"name\":\"out-20017\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/scb/outbound/reject\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/scb/outbound/reject\"},{\"name\":\"out-20018\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20019\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20020\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20021\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/uob/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/uob/outbound/report\"},{\"name\":\"out-20022\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/uob/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/uob/outbound/report\"},{\"name\":\"out-20023\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Test/usr/sap/interface/e-order/outbound/SAP_CONTRACT\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Test/usr/sap/interface/e-order/outbound/SAP_CONTRACT\"},{\"name\":\"out-20024\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/LAMS_Test/Outbound/\",\"targetSite\":\"site-30009\",\"staticDirTarget\":\"/LAMS_Test/Outbound/\"},{\"name\":\"out-20026\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Outbound\",\"targetSite\":\"site-30055\",\"staticDirTarget\":\"/Outbound\"},{\"name\":\"out-20027\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Outbound/\",\"targetSite\":\"site-30037\",\"staticDirTarget\":\"/Outbound/\"},{\"name\":\"out-20028\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/ktb/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/ktb/outbound/backup\"},{\"name\":\"out-20029\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/ktb/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/ktb/outbound/backup\"},{\"name\":\"out-20030\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/scb/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/scb/outbound/backup\"},{\"name\":\"out-20031\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/scb/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/scb/outbound/backup\"},{\"name\":\"out-20033\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/usr/sap/interface/bsa/bankpayment/ktb/outbound/Encrypted\",\"targetSite\":\"site-30054\",\"staticDirTarget\":\"/usr/sap/interface/bsa/bankpayment/ktb/outbound/Encrypted\"},{\"name\":\"out-20034\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/usr/sap/interface/bankpayment/ktb/outbound/Encrypted\",\"targetSite\":\"site-30054\",\"staticDirTarget\":\"/usr/sap/interface/bankpayment/ktb/outbound/Encrypted\"},{\"name\":\"out-20036\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/usr/sap/interface/pttor/bankpayment/ktb/outbound/Encrypted\",\"targetSite\":\"site-30054\",\"staticDirTarget\":\"/usr/sap/interface/pttor/bankpayment/ktb/outbound/Encrypted\"},{\"name\":\"out-20038\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC/Cancel\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC/Cancel\"},{\"name\":\"out-20039\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/bbl/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/bbl/outbound/report\"},{\"name\":\"out-20040\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/bbl/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/bbl/outbound/report\"},{\"name\":\"out-20041\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/amazonwsftp_rolloutrose/INITIAL\",\"targetSite\":\"site-30040\",\"staticDirTarget\":\"/amazonwsftp_rolloutrose/INITIAL\"},{\"name\":\"out-20042\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/amazonwsftp_rolloutrose/INITIAL\",\"targetSite\":\"site-30040\",\"staticDirTarget\":\"/amazonwsftp_rolloutrose/INITIAL\"},{\"name\":\"out-20043\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/B2BBunker_test\",\"targetSite\":\"site-30035\",\"staticDirTarget\":\"/B2BBunker_test\"},{\"name\":\"out-20045\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/CreditBureau_dev\",\"targetSite\":\"site-30006\",\"staticDirTarget\":\"/CreditBureau_dev\"},{\"name\":\"out-20046\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-EDP-DEV/Inbound/SAP_FI\",\"targetSite\":\"site-30020\",\"staticDirTarget\":\"/PTT-EDP-DEV/Inbound/SAP_FI\"},{\"name\":\"out-20047\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-EDP-DEV/Inbound/SAP_FI\",\"targetSite\":\"site-30020\",\"staticDirTarget\":\"/PTT-EDP-DEV/Inbound/SAP_FI\"},{\"name\":\"out-20048\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTDP_test/ptt_ebg/outbound\",\"targetSite\":\"site-30038\",\"staticDirTarget\":\"/PTTDP_test/ptt_ebg/outbound\"},{\"name\":\"out-20050\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/D:/P2PDataSource/SAP/DEV/\",\"targetSite\":\"site-30065\",\"staticDirTarget\":\"/D:/P2PDataSource/SAP/DEV/\"},{\"name\":\"out-20052\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/GSP_DS_TEST/DEV\",\"targetSite\":\"site-30060\",\"staticDirTarget\":\"/GSP_DS_TEST/DEV\"},{\"name\":\"out-20053\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/ifix_dev\",\"targetSite\":\"site-30007\",\"staticDirTarget\":\"/ifix_dev\"},{\"name\":\"out-20054\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT_Contract_Management_test\",\"targetSite\":\"site-30049\",\"staticDirTarget\":\"/PTT_Contract_Management_test\"},{\"name\":\"out-20055\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTRTILIMS_test\",\"targetSite\":\"site-30021\",\"staticDirTarget\":\"/PTTRTILIMS_test\"},{\"name\":\"out-20057\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/O2C_Auto_MM_test\",\"targetSite\":\"site-30062\",\"staticDirTarget\":\"/O2C_Auto_MM_test\"},{\"name\":\"out-20058\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/amazonwsftp/INITIAL\",\"targetSite\":\"site-30040\",\"staticDirTarget\":\"/amazonwsftp/INITIAL\"},{\"name\":\"out-20059\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/amazonwsftp/INITIAL\",\"targetSite\":\"site-30040\",\"staticDirTarget\":\"/amazonwsftp/INITIAL\"},{\"name\":\"out-20060\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/OUTPUT\",\"targetSite\":\"site-30056\",\"staticDirTarget\":\"/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/OUTPUT\"},{\"name\":\"out-20061\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/OR-KALA_Test\",\"targetSite\":\"site-30053\",\"staticDirTarget\":\"/OR-KALA_Test\"},{\"name\":\"out-20062\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTOR-Marine_E-Order_test\",\"targetSite\":\"site-30041\",\"staticDirTarget\":\"/PTTOR-Marine_E-Order_test\"},{\"name\":\"out-20063\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTOR-Retail-Work-Tracking_Test\",\"targetSite\":\"site-30018\",\"staticDirTarget\":\"/PTTOR-Retail-Work-Tracking_Test\"},{\"name\":\"out-20064\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/ZPTT_ORDERITEM_test\",\"targetSite\":\"site-30016\",\"staticDirTarget\":\"/ZPTT_ORDERITEM_test\"},{\"name\":\"out-20065\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/SSC_Performance_Report_test/SVM\",\"targetSite\":\"site-30047\",\"staticDirTarget\":\"/SSC_Performance_Report_test/SVM\"},{\"name\":\"out-20066\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface\"},{\"name\":\"out-20067\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30046\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20068\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30046\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20069\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30019\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20070\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30019\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20071\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test\"},{\"name\":\"out-20072\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test\"},{\"name\":\"out-20073\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test\"},{\"name\":\"out-20074\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-eTax_Web/TransLogFiles\",\"targetSite\":\"site-30023\",\"staticDirTarget\":\"/PTT-eTax_Web/TransLogFiles\"},{\"name\":\"out-20075\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface\"},{\"name\":\"out-20076\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/excel_report_sap\",\"targetSite\":\"site-30052\",\"staticDirTarget\":\"/excel_report_sap\"},{\"name\":\"out-20077\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/RelatedParty_test\",\"targetSite\":\"site-30029\",\"staticDirTarget\":\"/RelatedParty_test\"},{\"name\":\"out-20078\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Tankinspection_test\",\"targetSite\":\"site-30012\",\"staticDirTarget\":\"/Tankinspection_test\"},{\"name\":\"out-20079\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT_FLASH/HODEV\",\"targetSite\":\"site-30061\",\"staticDirTarget\":\"/PTT_FLASH/HODEV\"},{\"name\":\"out-20080\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT_FLASH/HODEV\",\"targetSite\":\"site-30061\",\"staticDirTarget\":\"/PTT_FLASH/HODEV\"},{\"name\":\"out-20081\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test/Credit-Balance/Background\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test/Credit-Balance/Background\"},{\"name\":\"out-20082\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test/Credit-Balance/Background\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test/Credit-Balance/Background\"},{\"name\":\"out-20083\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test/Credit-Balance/Background\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test/Credit-Balance/Background\"},{\"name\":\"out-20084\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test/Reserve-BadDebts/Background\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test/Reserve-BadDebts/Background\"},{\"name\":\"out-20085\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test/Reserve-BadDebts/Background\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test/Reserve-BadDebts/Background\"},{\"name\":\"out-20086\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Accounting-AR_test/Reserve-BadDebts/Background\",\"targetSite\":\"site-30008\",\"staticDirTarget\":\"/Accounting-AR_test/Reserve-BadDebts/Background\"},{\"name\":\"out-20087\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/outBox\",\"targetSite\":\"site-30005\",\"staticDirTarget\":\"/outBox\"},{\"name\":\"out-20088\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTDP_test/smart_vendor/outbound\",\"targetSite\":\"site-30001\",\"staticDirTarget\":\"/PTTDP_test/smart_vendor/outbound\"},{\"name\":\"out-20089\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-EDP-DEV/Inbound\",\"targetSite\":\"site-30020\",\"staticDirTarget\":\"/PTT-EDP-DEV/Inbound\"},{\"name\":\"out-20090\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-EDP-DEV/Inbound\",\"targetSite\":\"site-30020\",\"staticDirTarget\":\"/PTT-EDP-DEV/Inbound\"},{\"name\":\"out-20091\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/ETAXDOC\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/ETAXDOC\"},{\"name\":\"out-20092\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/ebpp/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/ebpp/outbound\"},{\"name\":\"out-20093\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/mofi_dev/outbound/post_result/\",\"targetSite\":\"site-30025\",\"staticDirTarget\":\"/mofi_dev/outbound/post_result/\"},{\"name\":\"out-20094\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/ebpp/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/ebpp/outbound\"},{\"name\":\"out-20128\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/\",\"targetSite\":\"site-30050\",\"staticDirTarget\":\"/\"},{\"name\":\"out-20129\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC\"},{\"name\":\"out-20130\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/bbl/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/bbl/outbound/backup\"},{\"name\":\"out-20131\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/bbl/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/bbl/outbound/backup\"},{\"name\":\"out-20132\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Data_Test\",\"targetSite\":\"site-30034\",\"staticDirTarget\":\"/Data_Test\"},{\"name\":\"out-20133\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/GPSCGROUP_PIS_Test/PISData_SAP_Test\",\"targetSite\":\"site-30044\",\"staticDirTarget\":\"/GPSCGROUP_PIS_Test/PISData_SAP_Test\"},{\"name\":\"out-20134\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/D:/SAP/\",\"targetSite\":\"site-30048\",\"staticDirTarget\":\"/D:/SAP/\"},{\"name\":\"out-20135\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/GPSC_WFPNBI_dev/inbound\",\"targetSite\":\"site-30066\",\"staticDirTarget\":\"/GPSC_WFPNBI_dev/inbound\"},{\"name\":\"out-20136\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/HRAIS_test\",\"targetSite\":\"site-30057\",\"staticDirTarget\":\"/HRAIS_test\"},{\"name\":\"out-20137\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/BWH\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/BWH\"},{\"name\":\"out-20138\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/BWH\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/BWH\"},{\"name\":\"out-20139\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/BWH\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/BWH\"},{\"name\":\"out-20140\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/pttlngpis\",\"targetSite\":\"site-30022\",\"staticDirTarget\":\"/pttlngpis\"},{\"name\":\"out-20141\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTLNG-PIS_Test\",\"targetSite\":\"site-30058\",\"staticDirTarget\":\"/PTTLNG-PIS_Test\"},{\"name\":\"out-20142\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTNGD/DEV/AX\",\"targetSite\":\"site-30026\",\"staticDirTarget\":\"/PTTNGD/DEV/AX\"},{\"name\":\"out-20143\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/textfile\",\"targetSite\":\"site-30036\",\"staticDirTarget\":\"/textfile\"},{\"name\":\"out-20144\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/upload/inbound_files\",\"targetSite\":\"site-30024\",\"staticDirTarget\":\"/upload/inbound_files\"},{\"name\":\"out-20145\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface\"},{\"name\":\"out-20146\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/upload/inbound_files\",\"targetSite\":\"site-30043\",\"staticDirTarget\":\"/upload/inbound_files\"},{\"name\":\"out-20147\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-WorkforceManagement_Test\",\"targetSite\":\"site-30064\",\"staticDirTarget\":\"/PTT-WorkforceManagement_Test\"},{\"name\":\"out-20148\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTPIMS_dev/inbound\",\"targetSite\":\"site-30067\",\"staticDirTarget\":\"/PTTPIMS_dev/inbound\"},{\"name\":\"out-20149\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/bay/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/bay/outbound/backup\"},{\"name\":\"out-20150\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/bay/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/bay/outbound/backup\"},{\"name\":\"out-20151\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/ETAXDOC/Cancel\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/ETAXDOC/Cancel\"},{\"name\":\"out-20154\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/BWH\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/BWH\"},{\"name\":\"out-20155\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/BWH\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/BWH\"},{\"name\":\"out-20156\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/BWH\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/BWH\"},{\"name\":\"out-20158\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/home/pod_orbpc/interface/ORBPC-C/outbound\",\"targetSite\":\"site-30004\",\"staticDirTarget\":\"/home/pod_orbpc/interface/ORBPC-C/outbound\"},{\"name\":\"out-20159\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/home/pod_orbpc/interface/ORBPC-C/outbound\",\"targetSite\":\"site-30004\",\"staticDirTarget\":\"/home/pod_orbpc/interface/ORBPC-C/outbound\"},{\"name\":\"out-20161\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTOR-LPG-inventory-dashboard_Test\",\"targetSite\":\"site-30003\",\"staticDirTarget\":\"/PTTOR-LPG-inventory-dashboard_Test\"},{\"name\":\"out-20162\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTOR-APO-SmartAnalytic_Test/APO_Dev\",\"targetSite\":\"site-30031\",\"staticDirTarget\":\"/PTTOR-APO-SmartAnalytic_Test/APO_Dev\"},{\"name\":\"out-20163\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-OrgStructureDashboard_test\",\"targetSite\":\"site-30033\",\"staticDirTarget\":\"/PTT-OrgStructureDashboard_test\"},{\"name\":\"out-20164\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-EDP-DEV/Inbound/SAP_BW\",\"targetSite\":\"site-30020\",\"staticDirTarget\":\"/PTT-EDP-DEV/Inbound/SAP_BW\"},{\"name\":\"out-20165\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-EDP-DEV/Inbound/SAP_BW\",\"targetSite\":\"site-30020\",\"staticDirTarget\":\"/PTT-EDP-DEV/Inbound/SAP_BW\"},{\"name\":\"out-20166\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTT-WorkforceManagement_Test\",\"targetSite\":\"site-30064\",\"staticDirTarget\":\"/PTT-WorkforceManagement_Test\"},{\"name\":\"out-20167\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/pttor/SCB/outbound/payment_data\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/pttor/SCB/outbound/payment_data\"},{\"name\":\"out-20168\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/outbound/payment_data\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/outbound/payment_data\"},{\"name\":\"out-20169\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC\"},{\"name\":\"out-20170\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Test/usr/sap/interface/e-order/outbound/SAP_CONTRACT\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Test/usr/sap/interface/e-order/outbound/SAP_CONTRACT\"},{\"name\":\"out-20171\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20172\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20173\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20174\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/pttor/ebpp/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/pttor/ebpp/outbound\"},{\"name\":\"out-20175\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/scb/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/scb/outbound/report\"},{\"name\":\"out-20176\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/scb/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/scb/outbound/report\"},{\"name\":\"out-20177\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/uob/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/uob/outbound/backup\"},{\"name\":\"out-20178\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/uob/outbound/backup\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/uob/outbound/backup\"},{\"name\":\"out-20181\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/pttor/bay/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/pttor/bay/outbound/report\"},{\"name\":\"out-20182\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankStatement_test/bay/outbound/report\",\"targetSite\":\"site-30051\",\"staticDirTarget\":\"/BankStatement_test/bay/outbound/report\"},{\"name\":\"out-20183\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20184\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20185\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTHRANA_dev/inbound/HCM\",\"targetSite\":\"site-30002\",\"staticDirTarget\":\"/PTTHRANA_dev/inbound/HCM\"},{\"name\":\"out-20186\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/pttor/ETAXDOC\"},{\"name\":\"out-20187\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/ebpp/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/ebpp/outbound\"},{\"name\":\"out-20188\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/BankPayment_test/SCB/inbound/ETAXDOC\",\"targetSite\":\"site-30030\",\"staticDirTarget\":\"/BankPayment_test/SCB/inbound/ETAXDOC\"},{\"name\":\"out-20189\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/pttor/ebpp/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/pttor/ebpp/outbound\"},{\"name\":\"out-20190\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/PTTOR-AMZ-DC_DEV\",\"targetSite\":\"site-30014\",\"staticDirTarget\":\"/PTTOR-AMZ-DC_DEV\"},{\"name\":\"out-20191\",\"sourceSite\":\"dcloud-sftp\",\"staticDirSource\":\"/Dev/usr/sap/interface/pttor/ebpp/outbound\",\"targetSite\":\"site-30015\",\"staticDirTarget\":\"/Dev/usr/sap/interface/pttor/ebpp/outbound\"}]";
		JsonArray arr = new Gson().fromJson(all, JsonArray.class);
		for (JsonElement e : arr) {
			OL.sln(e);
			Schedule sc = (Schedule) DB.parse(Schedule.class, e.getAsJsonObject());
			String iden = OL.base64Encode(sc.siteSource) + ":" + OL.base64Decode(sc.staticDirSource) + ":" + OL.base64Encode(sc.siteTarget) + ":" + OL.base64Encode(sc.staticDirTarget);
			
			System.out.println(iden);
		}*/
		
		HashMap<String, ArrayList<Schedule>> mapS = new HashMap<String, ArrayList<Schedule>>();
		JsonArray arr1 = new Gson().fromJson(Files.readString(Path.of("t1.json")), JsonArray.class);
		JsonArray arr2 = new Gson().fromJson(Files.readString(Path.of("t2.json")), JsonArray.class);
		JsonArray arr = new JsonArray();
		for (JsonElement e : arr1) { arr.add(e); }
		for (JsonElement e : arr2) { arr.add(e); }
		for (JsonElement e : arr) {
			Schedule sc = (Schedule) DB.parse(Schedule.class, e.getAsJsonObject());
			String key = OL.base64Encode(sc.siteSource) + ":" + OL.base64Encode(sc.staticDirSource) + ":" + OL.base64Encode(sc.siteTarget) + ":" + OL.base64Encode(sc.staticDirTarget);
			if (!mapS.containsKey(key)) {
				mapS.put(key, new ArrayList<Schedule>());
			}
			mapS.get(key).add(sc);
		}

		OL.sln("------------------------------------------------");
		Set<String> ks = mapS.keySet();
		for (String key : ks) {
			ArrayList<Schedule> ss = mapS.get(key);
			
			Schedule example = ss.get(0);
			
			example.plan = "*-*-* *:00:00";
			
			String json = DB.toJsonString(example);
			
			
			JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/schedules", "post", null, new Gson().fromJson(json, JsonElement.class));
			OL.sln(e);
			
			//OL.sln("key: " + key);
			//OL.sln("size(): " + ss.size());
			OL.sln("------------------------------------------------");
		}
		OL.sln("total schedules: " + mapS.size());
	}
}