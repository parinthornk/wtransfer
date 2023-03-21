package com.pttdigital.wtransfer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.pttdigital.wtransfer.Item.Status;
import com.pttdigital.wtransfer.Log.Type;

public class Client {
	public static JsonElement getJsonResponse(String url0, String method, HashMap<String, String> headers, JsonElement jsonRequest) throws Exception {
		JsonElement jsonResponse = null;
		HttpURLConnection conn = null;
        BufferedReader reader = null;
        OutputStream os = null;
        
        try {
        	
        	String url = url0.replace(" ", "%20"); // TODO: ??????????
        	
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
    		
    		if (jsonRequest != null) {
                os = conn.getOutputStream();
                os.write(jsonRequest.toString().getBytes());
                os.flush();
    		}
    		
            int responseCode = conn.getResponseCode();
            if (100 < responseCode && responseCode < 300) {
    			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    			jsonResponse = new Gson().fromJson (reader.lines().collect(Collectors.joining()), JsonElement.class);
            } else {
    			reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = reader.lines().collect(Collectors.joining());
                throw new Exception("Received HTTP error: " + responseCode + ", " + responseBody);
            }
        } catch (Exception ex) {
        	
    		try { reader.close(); } catch (Exception e1) { }
    		try { os.close(); } catch (Exception e1) { }
    		try { conn.disconnect(); } catch (Exception e1) { }
        	throw new Exception("Error calling endpoint: url[" + url0 + "], method[" + method + "]: " + ex);
        }
        
		try { reader.close(); } catch (Exception ex) { }
		try { os.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }

		return jsonResponse;
	}

	public static void setItemStatus(Item item, Status executing) {
		// TODO Auto-generated method stub
		
	}

	public static void addItemLog(Item item, Type info, String string, String string2) {
		// TODO Auto-generated method stub
		
	}

	public static void updateOnExecuteFailed(Item item, Status status, int retryRemaining, Timestamp timeNextRetry, Timestamp timeLatestRetry) {
		// TODO Auto-generated method stub
		
	}
}
