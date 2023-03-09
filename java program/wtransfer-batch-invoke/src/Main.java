import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

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
	
	private static String car_02_url = "http://localhost:8290/wtransfer/12c27e40-ac08-41ce-a8b7-04d3d62e0ccd";
	
	private static void invoke_car02() throws Exception {
		callApi(car_02_url, "get", null, null);
	}
	
	public static void main(String[] args) {
		while (true) {
			try {
				Thread.sleep(10 * 1000);
			} catch (Exception ex) { }
			try {
				invoke_car02();
			} catch (Exception ex) { ex.printStackTrace(); }
		}
	}
}