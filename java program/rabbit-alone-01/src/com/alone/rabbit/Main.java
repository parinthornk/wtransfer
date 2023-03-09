package com.alone.rabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

//import java.io.FileWriter;

public class Main {
	
	private static String callApi(String url, String method, HashMap<String, String> headers, String reqBody) throws Exception {
		
		String ret = null;
		
		HttpURLConnection conn = null;
        BufferedReader reader = null;
        OutputStream os = null;
        
        try {


            conn = (HttpURLConnection)((new java.net.URL(url)).openConnection());
    		conn.setDoOutput(true);
    		conn.setRequestMethod(method.toUpperCase());
    		

    		HashMap<String, String> hout = new HashMap<String, String>();
    		hout.put("Content-Type", "application/json");
    		if (headers != null) {
    			Set<String> keys = headers.keySet();
    			for (String key : keys) {
    				hout.put(key, headers.get(key));
    			}
    		}
    		Set<String> keys = hout.keySet();
    		for (String key : keys) {
    			conn.setRequestProperty(key, hout.get(key));
    		}
    		
    		if (reqBody != null) {
                os = conn.getOutputStream();
                os.write(reqBody.getBytes());
                os.flush();
    		}
    		
            int responseCode = conn.getResponseCode();
            if (100 < responseCode && responseCode < 300) {
    			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    			ret = reader.lines().collect(Collectors.joining());
            } else {
    			reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = reader.lines().collect(Collectors.joining());
                throw new Exception("Received HTTP error: " + responseCode + ", " + responseBody);
            }
        } catch (Exception e1) {
    		try { reader.close(); } catch (Exception ex) { }
    		try { os.close(); } catch (Exception ex) { }
    		try { conn.disconnect(); } catch (Exception ex) { }
        	throw new Exception("Error while executing callApi(" + url + ", " + method + ", " + headers + ", " + reqBody + "), " + e1);
        }

		try { reader.close(); } catch (Exception ex) { }
		try { os.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		
		return ret;
	}
	
	private static String host = "localhost";
	private static int port = 5672;
	private static String username = "guest";
	private static String password = "guest";
	private static String endpointMoveFile = "http://localhost:8290/immediate";
	private static String endpointRabbit = "http://localhost:15672/api";
	private static String queueNameDefault = "default";
	private static QueueExecution.Mode defaultThreadsMode = QueueExecution.Mode.MULTIPLE_THREADS_5;
	
	private static QueueListener.Execution getDefaultExecution() {
		return new QueueListener.Execution() {
			@Override
			public void execute(String queueName, String message) {
				if (queueName.equals(queueNameDefault)) {
					try {
						executeDefaultCommand(message);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					return;
				}
				
				try {
					// TODO -> ?
					System.out.println("OK[" + System.currentTimeMillis() + "]: " + message);
					callApi(endpointMoveFile, "post", null, message);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
	}
	
	private static void executeDefaultCommand(String command) throws Exception {
		try {
			String[] sep = command.split(" ");
			String action = sep[0];
			String queueName = sep[1];
			if (queueName.equals(queueNameDefault)) {
				throw new Exception("Default queue cannot be accessed from any commands.");
			}
			if (action.equals("subscribe")) {
				subscribe(queueName, defaultThreadsMode);
			}
			if (action.equals("unsubscribe")) {
				unsubscribe(queueName);
			}
		} catch (Exception ex) {
			throw new Exception("Failed to execute command \"" + command +"\". " + ex);
		}
	}
	
	private static HashMap<String, QueueListener> mapQueueListener = new HashMap<String, QueueListener>();
	
	private static void subscribe(String queueName, QueueExecution.Mode threadsMode) throws Exception {
		if (mapQueueListener.containsKey(queueName)) {
			throw new Exception("Failed to subscribe queue \"" + queueName + "\", the queue was already subscribed.");
		} else {
			QueueListener listener = new QueueListener(host, port, queueName, username, password, getDefaultExecution(), threadsMode);
			listener.listenAsync();
			mapQueueListener.put(queueName, listener);
			System.out.println("SUCCESS,   subscribe: " + queueName);
		}
	}
	
	private static void unsubscribe(String queueName) throws Exception {
		if (mapQueueListener.containsKey(queueName)) {
			QueueListener listener = mapQueueListener.get(queueName);
			listener.close();
			mapQueueListener.remove(queueName);
			System.out.println("SUCCESS, unsubscribe: " + queueName);
		} else {
			throw new Exception("Failed to unsubscribe queue \"" + queueName + "\", the queue has not been subscribed.");
		}
	}
	
	private static ArrayList<String> listQueues() throws Exception {
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "Basic " + new String(Base64.getEncoder().encodeToString((username + ":" + password).getBytes())));
		String json = callApi(endpointRabbit + "/queues", "get", headers, null);
		JsonArray array = new Gson().fromJson(json, JsonArray.class);
		ArrayList<String> ret = new ArrayList<String>();
		for (JsonElement e : array) {
			String queueName = e.getAsJsonObject().get("name").getAsString();
			ret.add(queueName);
		}
		return ret;
	}
	
	private static void readJsonConfig() throws Exception {
		String jsonConfig = new String(Files.readAllBytes(Paths.get("config.json")));
		JsonObject o = new Gson().fromJson(jsonConfig, JsonObject.class);
		
		host = o.get("host").getAsString();
		port = o.get("port").getAsInt();
		username = o.get("username").getAsString();
		password = o.get("password").getAsString();
		endpointMoveFile = o.get("endpointMoveFile").getAsString();
		endpointRabbit = o.get("endpointRabbit").getAsString();
		queueNameDefault = o.get("queueNameDefault").getAsString();
		defaultThreadsMode = QueueExecution.Mode.valueOf(o.get("defaultThreadsMode").getAsString().toUpperCase());
		
		System.out.println("------------------------------------------------------------------------------------------------");
		System.out.println("Initializing variables...");
		System.out.println("host: " + host);
		System.out.println("port: " + port);
		System.out.println("username: " + username);
		System.out.println("password: " + password);
		System.out.println("endpointMoveFile: " + endpointMoveFile);
		System.out.println("endpointRabbit: " + endpointRabbit);
		System.out.println("queueNameDefault: " + queueNameDefault);
		System.out.println("defaultThreadsMode: " + defaultThreadsMode);
		System.out.println("------------------------------------------------------------------------------------------------");
	}
	
	public static void main(String[] args) throws Exception {
		
		// read json config
		try {
			readJsonConfig();
		} catch (Exception ex) {
			throw new Exception("The program was aborted as it failed to read JSON config. " + ex);
		}
		
		for (;;) {
			try {
				
				// list all queues
				ArrayList<String> queues = listQueues();
				
				// always subscribe default queue
				if (!queues.contains(queueNameDefault)) {
					try {
						unsubscribe(queueNameDefault);
					} catch (Exception ex) { }
					queues.add(queueNameDefault);
				}
				
				// subscribe
				for (String queueName : queues) {
					if (!mapQueueListener.containsKey(queueName)) {
						subscribe(queueName, defaultThreadsMode);
					}
				}
				
				// unsubscribe
				Set<String> ks = mapQueueListener.keySet();
				ArrayList<String> toRemove = new ArrayList<String>(); 
				for (String queueName : ks) {
					if (!queues.contains(queueName)) {
						toRemove.add(queueName);
					}
				}
				for (String queueName : toRemove) {
					unsubscribe(queueName);
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			try { Thread.sleep(10000); } catch (Exception e) { }
		}
	}
}