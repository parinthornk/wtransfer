package com.pttdigital.wtransfer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZWorker;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pttdigital.wtransfer.ImportV2.OL;

public class Site {
	public String workspace;
	public String name;
	public String description;
	public Timestamp created;
	public Timestamp modified;
	public String host;
	public int port;
	public String protocol;
	public String username;
	public String password;
	public String keyPath;
	
	public static ZResult getReachability() throws Exception {

		
		HashMap<String, Site> allSites = CAR02.mapSites();
		Set<String> ks = allSites.keySet();
		ArrayList<Thread> at = new ArrayList<Thread>();
		HashMap<String, Boolean> mapReachable = new HashMap<String, Boolean>();
		Object locker = new Object();
		for (String s : ks) {
			final String name = allSites.get(s).name;
			at.add(new Thread() {
				@Override
				public void run() {
					boolean connection_ok = false;
					
					try {
						JsonElement e = Client.getJsonResponse(ZConnector.Constant.WTRANSFER_API_ENDPOINT + "/workspaces/default/sites/" + name + "/objects", "get", null, null);
						int count = e.getAsJsonObject().get("count").getAsInt();
						connection_ok = count > -1;
					} catch (Exception ex) { }
					
					synchronized (locker) {
						mapReachable.put(name, connection_ok);
					}
				}
			});
		}
		
		for (Thread t : at) { t.start(); }
		for (Thread t : at) { t.join(); }
		
		Set<String> ks2 = allSites.keySet();
		
		String[] d = new String[allSites.size()];
		int dc = 0;
		for (String name : ks2) { d[dc++] = name; }
		Arrays.sort(d);
		
		JsonArray array = new JsonArray();
		for (String name : d) {
			Site site = allSites.get(name);
			JsonObject o = new Gson().fromJson(DB.toJsonString(site), JsonObject.class);
			o.addProperty("reachable", mapReachable.get(site.name));
			array.add(o);
		}
		JsonObject o = new JsonObject();
		o.add("list", array);
		
		return ZResult.OK_200(o.toString());
	}

	public static String getWordWorkspace() {
		// TODO Auto-generated method stub
		return "workspace";
	}

	public static String getWordName() {
		// TODO Auto-generated method stub
		return "name";
	}
}