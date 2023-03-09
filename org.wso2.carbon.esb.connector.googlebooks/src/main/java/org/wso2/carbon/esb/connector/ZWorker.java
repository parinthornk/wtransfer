package org.wso2.carbon.esb.connector;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.net.ftp.FTPClient;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class ZWorker {
	
	// http://10.224.143.44:8290/services
	public static String WTRANSFER_API_ENDPOINT = "http://localhost:8290/wtransfer";
	
	public enum TaskStatus {
		PENDING,
		INPROGRESS,
		PARTIALSUCCESS,
		SUCCESS,
		FAILED,
	}
	
	public enum LogType {
		INFO,
		WARNING,
		ERROR,
	}
	
	public static boolean isFileNameToMove(String fileName, String logic) {
		boolean ret = false;
		StringWriter writer = new StringWriter();
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			ScriptContext ctx = engine.getContext();
			ctx.setWriter(writer);
			engine.eval("var x = \"" + fileName + "\";var fn = " + logic + ";print(fn(x));");
			String printed = writer.toString();
			ret = Boolean.parseBoolean(printed.substring(0, printed.length() - 2));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try { writer.close(); } catch (Exception ex) { }
		return ret;
	}
	
	private static String renamedByLogic(String fileName, String logic) {
		String ret = fileName;
		StringWriter writer = new StringWriter();
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			ScriptContext ctx = engine.getContext();
			ctx.setWriter(writer);
			engine.eval("var x = \"" + fileName + "\";var fn = " + logic + ";print(fn(x));");
			String printed = writer.toString();
			ret = printed.substring(0, printed.length() - 2);
			return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
			//log(workspace, taskId, LogType.ERROR, "updateInputStreams.isFileNameToMove", "JavaScript error: " + ex);
		}
		try { writer.close(); } catch (Exception ex) { }
		return ret;
	}
	
	/*public static ZConnector.ZResult readSiteStatus(String workspace, String siteName) {
		FTPClient r_ftpClient = null;
		
		ZConnector.ZResult result = new ZConnector.ZResult();
		ChannelSftp r_sftpChannelIn = null;
		Channel r_channelIn = null;
		Session r_sessionIn = null;
		
		JSch jschIn = new JSch();
		try {
			JsonElement source = ZWorker.getSiteByName(workspace, siteName);
			if (source == null) {
				throw new Exception("Site \""+siteName+"\" could not be found on workspace \""+workspace+"\".");
			}
			String host = source.getAsJsonObject().get("host").getAsString();
			String protocol = source.getAsJsonObject().get("protocol").getAsString();
			String rootFolder = source.getAsJsonObject().get("rootFolder").getAsString();
			String password = source.getAsJsonObject().get("password").getAsString();
			int port = source.getAsJsonObject().get("port").getAsInt();
			String username = source.getAsJsonObject().get("username").getAsString();
			if (protocol.equalsIgnoreCase("sftp")) {
				long t0 = System.currentTimeMillis();
				r_sessionIn = jschIn.getSession(username, host, port);
				r_sessionIn.setConfig("StrictHostKeyChecking", "no");
				r_sessionIn.setPassword(password);
				r_sessionIn.connect();
				r_channelIn = r_sessionIn.openChannel("sftp");
				r_channelIn.connect();
				r_sftpChannelIn = (ChannelSftp) r_channelIn;
				r_sftpChannelIn.cd(rootFolder);
				long t1 = System.currentTimeMillis();
				long connectionTimeMs = t1 - t0;
				int serverVersion = r_sftpChannelIn.getServerVersion();
				String home = r_sftpChannelIn.getHome();
				ArrayList<String> items = new ArrayList<String>();
				Vector<?> filelist = r_sftpChannelIn.ls(rootFolder);
				int size = filelist.size();
	            for(int i=0; i<size;i++){
	                LsEntry entry = (LsEntry) filelist.get(i);
	                String item = entry.getFilename();
					items.add(item);
	            }
				Map<String, Object> m1 = new HashMap<String, Object>();
				m1.put("connectionTimeMs", connectionTimeMs);
				m1.put("serverVersion", serverVersion);
				m1.put("currentFolder", rootFolder);
				m1.put("home", home);
				m1.put("itemsCount", items.size());
				m1.put("items", items);
				result.content = ZConnector.ConvertToJsonString(m1);
				result.statusCode = 200;
			} else if (protocol.equalsIgnoreCase("ftp")) {
				long t0 = System.currentTimeMillis();
				r_ftpClient = new FTPClient();
				r_ftpClient.connect(host, port);
				r_ftpClient.enterLocalPassiveMode();
				if (r_ftpClient.login(username, password)) {
					r_ftpClient.cwd(rootFolder);
					long t1 = System.currentTimeMillis();
					long connectionTimeMs = t1 - t0;
					String[] files = r_ftpClient.listNames();

					Map<String, Object> m1 = new HashMap<String, Object>();
					m1.put("connectionTimeMs", connectionTimeMs);
					m1.put("systemName", r_ftpClient.getSystemName());
					m1.put("itemsCount", files.length);
					m1.put("items", files);
					m1.put("currentFolder", rootFolder);
					result.content = ZConnector.ConvertToJsonString(m1);
					result.statusCode = 200;
					
				} else {
					throw new Exception("Invalid credentials: FTPClient.login(\""+username+"\", \""+password+"\").");
				}
			} else {
				throw new Exception("Unsupported protocol for \"" + siteName + "\": \"" + protocol + "\".");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Map<String, Object> m1 = new HashMap<String, Object>();
			m1.put("error", ex + "");
			result.content = ZConnector.ConvertToJsonString(m1);
			result.statusCode = 500;
		}
		
		// cleanup input
		try { if (r_sessionIn != null) { r_sessionIn.disconnect(); } } catch (Exception ex) { }
		try { if (r_sftpChannelIn != null) { r_sftpChannelIn.disconnect(); } } catch (Exception ex) { }
		try { if (r_channelIn != null) { r_channelIn.disconnect(); } } catch (Exception ex) { }
		try { if (r_sessionIn != null) { r_sessionIn.disconnect(); } } catch (Exception ex) { }
		
		// cleanup ftp client
		try { if (r_ftpClient != null) { r_ftpClient.disconnect(); } } catch (Exception ex) { }
		
		return result;
	}*/
	
	private static ArrayList<String> listWorkspaces() {
		ArrayList<String> ret = new ArrayList<String>();
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces");
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        
	        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String responseBody = br.lines().collect(Collectors.joining());
	        
			Gson gson = new Gson();
			JsonElement element = gson.fromJson (responseBody, JsonElement.class);
			JsonObject jsonObj = element.getAsJsonObject();
			JsonArray arr = jsonObj.getAsJsonArray("list");
			
			for (JsonElement je : arr) {
				JsonObject jo = je.getAsJsonObject();
				String workspace = jo.get("name").getAsString();
		        ret.add(workspace);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	private static JsonObject listTasks(String workspace) {
		JsonObject jsonObj = null;
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/tasks");
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String responseBody = br.lines().collect(Collectors.joining());
			JsonElement element = new Gson().fromJson (responseBody, JsonElement.class);
			jsonObj = element.getAsJsonObject();
		} catch (Exception ex) { ex.printStackTrace(); }
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return jsonObj;
	}
	
	private static JsonArray listPendingTasks(ArrayList<String> workspaces, int limit) {
		JsonArray pendingTasks = new JsonArray();
		for (String workspace : workspaces) {
			JsonArray tasks = listTasks(workspace).getAsJsonArray("list");
			for (JsonElement task : tasks) {
				boolean isPendingTask = false;
				try {
					isPendingTask = TaskStatus.PENDING.toString().equalsIgnoreCase(task.getAsJsonObject().get("status").getAsString());
				} catch (Exception ex) { }
				if (isPendingTask) {
					if (pendingTasks.size() < limit) {
						pendingTasks.add(task);
					} else {
						return pendingTasks;
					}
				}
			}
		}
		return pendingTasks;
	}
	
	public static boolean setTaskStatus(String workspace, int taskId, TaskStatus status) {
		boolean ret = false;
		java.net.URL url = null;
		HttpURLConnection conn = null;
		OutputStream os = null;
        BufferedReader br = null;
		try {
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/tasks/" + taskId + "/status");
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("post".toUpperCase());

			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", "application/json");
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				conn.setRequestProperty(key, headers.get(key));
			}
			
			String requestBody = "{\"status\": \"" + status.toString().toLowerCase() + "\"}";
            os = conn.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
	        
	        
	        int responseCode = conn.getResponseCode();
	        if (100 < responseCode && responseCode < 300) {
		        //br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                //String responseBody = br.lines().collect(Collectors.joining());
                System.out.println("Task ["+taskId+"] was set status to \""+status+"\".");
                ret = true;
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = br.lines().collect(Collectors.joining());
                throw new Exception("unable to SetTaskStatus("+workspace+", "+taskId+", "+status+"): " + responseBody);
            }
	        
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { os.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	private static JsonElement getConfig(JsonElement task) {
		JsonElement ret = null;
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			
			String workspace = task.getAsJsonObject().get("workspace").getAsString();
			String config = task.getAsJsonObject().get("config").getAsString();
			
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/configs/" + config);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        
	        int responseCode = conn.getResponseCode();
	        if (100 < responseCode && responseCode < 300) {
		        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String responseBody = br.lines().collect(Collectors.joining());
	        	JsonElement element = new Gson().fromJson (responseBody, JsonElement.class);
	        	//System.out.println(element);
                ret = element;
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = br.lines().collect(Collectors.joining());
                throw new Exception("unable to get config("+workspace+", "+config+"): " + responseBody);
            }
	        
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	private static JsonElement getPgp(JsonElement task) {
		JsonElement ret = null;
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			
			String workspace = task.getAsJsonObject().get("workspace").getAsString();
			String pgp = task.getAsJsonObject().get("pgp").getAsString();
			
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/pgps/" + pgp);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        
	        int responseCode = conn.getResponseCode();
	        if (100 < responseCode && responseCode < 300) {
		        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String responseBody = br.lines().collect(Collectors.joining());
	        	JsonElement element = new Gson().fromJson (responseBody, JsonElement.class);
	        	//System.out.println(element);
                ret = element;
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = br.lines().collect(Collectors.joining());
                throw new Exception("unable to get pgp("+workspace+", "+pgp+"): " + responseBody);
            }
	        
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	public static JsonElement getSiteByName(String workspace, String siteName) {
		JsonElement ret = null;
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/sites/" + siteName);
			//System.out.println(url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        int responseCode = conn.getResponseCode();
	        if (100 < responseCode && responseCode < 300) {
		        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String responseBody = br.lines().collect(Collectors.joining());
	        	JsonElement element = new Gson().fromJson (responseBody, JsonElement.class);
	        	//System.out.println(element);
                ret = element;
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = br.lines().collect(Collectors.joining());
                throw new Exception("unable to get site("+workspace+", "+siteName+"): " + responseBody);
            }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	private static JsonElement getSite(JsonElement task, String scrVsTarget) {
		JsonElement ret = null;
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			
			String workspace = task.getAsJsonObject().get("workspace").getAsString();
			String siteName = task.getAsJsonObject().get(scrVsTarget).getAsString();
			
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/sites/" + siteName);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        
	        int responseCode = conn.getResponseCode();
	        if (100 < responseCode && responseCode < 300) {
		        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String responseBody = br.lines().collect(Collectors.joining());
	        	JsonElement element = new Gson().fromJson (responseBody, JsonElement.class);
	        	//System.out.println(element);
                ret = element;
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = br.lines().collect(Collectors.joining());
                throw new Exception("unable to get site("+workspace+", "+siteName+"): " + responseBody);
            }
	        
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	private static boolean log(String workspace, int taskId, LogType logType, String title, String body) {
		boolean ret = false;
		java.net.URL url = null;
		HttpURLConnection conn = null;
		OutputStream os = null;
        BufferedReader br = null;
		try {
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/logs");
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("post".toUpperCase());

			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", "application/json");
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				conn.setRequestProperty(key, headers.get(key));
			}
			
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("logType", logType.toString().toLowerCase());
			m1.put("title", title);
			m1.put("body", body);
			m1.put("task", taskId);
			String requestBody = ZConnector.ConvertToJsonString(m1);
            os = conn.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
	        
	        int responseCode = conn.getResponseCode();
	        if (100 < responseCode && responseCode < 300) {
		        //br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                //String responseBody = br.lines().collect(Collectors.joining());
                //System.out.println("Log ["+title+"] was added to task \""+taskId+"\".");
                ret = true;
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String responseBody = br.lines().collect(Collectors.joining());
                throw new Exception("unable to log("+workspace+", "+taskId+", "+title+"): " + responseBody);
            }
	        
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { os.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
	}
	
	private static String getString(JsonElement e, String fieldName) {
		try {
			return e.getAsJsonObject().get(fieldName.toLowerCase()).getAsString();
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static int getInt(JsonElement e, String fieldName, int defVal) {
		try {
			return Integer.parseInt(e.getAsJsonObject().get(fieldName).getAsString());
		} catch (Exception ex) {
			return defVal;
		}
	}
	
	private static HashMap<String, InputStream> istreamsIn = null;
	private static ChannelSftp sftpChannelIn = null;
	private static Channel channelIn = null;
	private static Session sessionIn = null;
	private static JSch jschIn = null;
	private static ArrayList<String> listFilesToMove = new ArrayList<String>(); 

	private static ChannelSftp sftpChannelOut = null;
	private static Channel channelOut = null;
	private static Session sessionOut = null;
	private static JSch jschOut = null;
	
	private static FTPClient ftpClientIn = null;
	private static FTPClient ftpClientOut = null;
	private static HashMap<String, ByteArrayOutputStream> ftpossIn = null;
	
	private static String protocolSource = null;
	private static String protocolTarget = null;
	
	private static void updateInputStreams(int taskId, JsonElement site, JsonElement config) throws Exception {

		String workspace = getString(site, "workspace");
		String siteName = getString(site, "name");
		String protocol = getString(site, "protocol");
		String host = getString(site, "host");
		int port = getInt(site, "port", -1);
		String username = getString(site, "username");
		String password = getString(site, "password");
		String rootFolder = getString(site, "rootFolder");

		System.out.println("workspace: " + workspace);
		System.out.println("siteName: " + siteName);
		System.out.println("protocol: " + protocol);
		System.out.println("host: " + host);
		System.out.println("port: " + port);
		System.out.println("username: " + username);
		System.out.println("password: " + password);
		System.out.println("rootFolder: " + rootFolder);
		
		// script for filename validation
		String fnIsFilenameToMove = getString(config, "fnisfilenametomove");
		
		
		log(workspace, taskId, LogType.INFO, "updateInputStreams", "Updating Inputstream[] from "+host+":"+port+" ["+username+":"+password+"] at folder: \""+rootFolder+"\".");

		if (istreamsIn == null) {
			istreamsIn = new HashMap<String, InputStream>();
		}
		
		if (ftpossIn == null) {
			ftpossIn = new HashMap<String, ByteArrayOutputStream>();
		}

		protocolSource = null;
		listFilesToMove.clear();
		if (protocol.equalsIgnoreCase("sftp")) {
			protocolSource = protocol;
			if (rootFolder == null) {rootFolder = "/";}
			if (rootFolder.length() == 0) {rootFolder = "/";}
			log(workspace, taskId, LogType.INFO, "updateInputStreams", "protocol = sftp");
			sftpChannelIn = null;
			channelIn = null;
			sessionIn = null;
			jschIn = new JSch();
			sessionIn = jschIn.getSession(username, host, port);
			sessionIn.setConfig("StrictHostKeyChecking", "no");
			sessionIn.setPassword(password);
			sessionIn.connect();
			channelIn = sessionIn.openChannel("sftp");
			channelIn.connect();
			sftpChannelIn = (ChannelSftp) channelIn;
			sftpChannelIn.cd(rootFolder);
			Vector<?> filelist = sftpChannelIn.ls(rootFolder);
			int size = filelist.size();
            for(int i=0; i<size;i++){
                LsEntry entry = (LsEntry) filelist.get(i);
                String fileName = entry.getFilename();
				boolean moveThisFile = isFileNameToMove(fileName, fnIsFilenameToMove);
				if (moveThisFile) {
					listFilesToMove.add(fileName);
					/*try {
		    			istreamsIn.put(fileName, sftpChannelIn.get(fileName));
					} catch (Exception ex) { }*/
				}
            }
            log(workspace, taskId, LogType.INFO, "updateInputStreams", "found["+size+"], selected["+istreamsIn.size()+"] in folder \""+rootFolder+"\"");
		} else if (protocol.equalsIgnoreCase("ftp")) {
			protocolSource = protocol;
			log(workspace, taskId, LogType.INFO, "updateInputStreams", "protocol = ftp");
			ftpClientIn = new FTPClient();
			ftpClientIn.connect(host, port);
			ftpClientIn.enterLocalPassiveMode();
			if (ftpClientIn.login(username, password)) {
				log(workspace, taskId, LogType.INFO, "updateInputStreams", "success login with [" + username + ":" + password + "].");
				ftpClientIn.cwd(rootFolder);
				String[] files = ftpClientIn.listNames();
				for (String fileName : files) {
					boolean moveThisFile = isFileNameToMove(fileName, fnIsFilenameToMove); 
					if (moveThisFile) {
						listFilesToMove.add(fileName);
						/*try {
							ftpossIn.put(fileName, new ByteArrayOutputStream());
						} catch (Exception ex) { }*/
					}
				}
				log(workspace, taskId, LogType.INFO, "updateInputStreams", "found["+files.length+"], selected["+istreamsIn.size()+"] in folder \""+rootFolder+"\"");
			} else {
				log(workspace, taskId, LogType.ERROR, "updateInputStreams", "failed login with [" + username + ":" + password + "].");
			}
		} else {
			throw new Exception("Unsupported protocol for \"" + siteName + "\": \"" + protocol + "\".");
		}
	}
	
	private static InputStream pgpStream(InputStream x, JsonElement pgp) {
		if (pgp == null) {
			return x;
		}

		String pgpDirection = pgp.getAsJsonObject().get("direction").getAsString();
		String pgpKeyPath = pgp.getAsJsonObject().get("keypath").getAsString();
		try {
			if (pgpDirection.equalsIgnoreCase("encrypt")) {
				byte[] b = PgpHelper.getInstance().encrypt((InputStream)x, pgpKeyPath);
				((InputStream)x).close();
				return new ByteArrayInputStream(b);
			} else if (pgpDirection.equalsIgnoreCase("decrypt")) {
				String keyPassword = pgp.getAsJsonObject().get("keypassword").getAsString();
				byte[] b = PgpHelper.getInstance().decrypt((InputStream)x, pgpKeyPath, keyPassword.toCharArray());
				((InputStream)x).close();
				return new ByteArrayInputStream(b);
			}
		} catch (Exception ex) {
			
		}
		
		return null;
	}
	
	private static InputStream gis = null;
	private static void clear_gis() {
		if (gis != null) {
			try {
				gis.close();
			} catch (Exception ex) {
				
			}
		}
		gis = null;
	}
	
	private static String getSiteFieldByName(JsonElement site, String fieldName) throws Exception {
		try {
			return site.getAsJsonObject().get(fieldName).getAsString();
		} catch (Exception ex) { throw new Exception("Failed reading field \""+fieldName+"\" from site: \"" + site.getAsJsonObject().get("name").getAsString() + "\"."); }
	}
	
	private static TaskStatus executeTask(JsonElement task) {
		TaskStatus ret = TaskStatus.FAILED;
		int taskId = task.getAsJsonObject().get("id").getAsInt();
		String workspace = task.getAsJsonObject().get("workspace").getAsString();
		long l0 = System.currentTimeMillis();
		try {
			
			log(workspace, taskId, LogType.INFO, "Task Start", "Begin execution of task \"" + taskId + "\".");
			//System.out.println("task: " + task);
			
			// get config
			JsonElement config = getConfig(task);
			
			// update source inputstreams
			JsonElement source = getSite(task, "source");
			updateInputStreams(taskId, source, config);
			
			// check for pgp
			JsonElement pgp = null;
			try {
				pgp = getPgp(task);
			} catch (Exception ex) {
				
			}
			
			
			if (pgp == null) {
				log(workspace, taskId, LogType.INFO, "Task Executing", "Transfer without PGP.");
			} else {
				String pgpDirection = pgp.getAsJsonObject().get("direction").getAsString();
				if (!pgpDirection.equalsIgnoreCase("encrypt") && !pgpDirection.equalsIgnoreCase("decrypt")) {
					throw new Exception("Bad PGP direction: " + pgpDirection + ", expected \"encrypt\" or \"decrypt\".");
				}
				log(workspace, taskId, LogType.INFO, "Task Executing", "Transfer with PGP.direction = \"" + pgpDirection + "\".");
			}
			
			
			
			
			// move file
			JsonElement target = getSite(task, "target");
			

			log(workspace, taskId, LogType.INFO, "Task Executing", "Assigning source parameters.");
			
			// TODO -> nullcheck
			String sourceProtocol = getSiteFieldByName(source, "protocol");
			String sourceSiteName = getSiteFieldByName(source, "name");
			
			log(workspace, taskId, LogType.INFO, "Task Executing", "Assigning target parameters.");

			// TODO -> nullcheck
			String targetProtocol = getSiteFieldByName(target, "protocol");//target.getAsJsonObject().get("protocol").getAsString();
			String targetSiteName = getSiteFieldByName(target, "name");//target.getAsJsonObject().get("name").getAsString();
			String targetUsername = getSiteFieldByName(target, "username");//target.getAsJsonObject().get("username").getAsString();
			String targetPassword = getSiteFieldByName(target, "password");//target.getAsJsonObject().get("password").getAsString();
			String targetHost = getSiteFieldByName(target, "host");//target.getAsJsonObject().get("host").getAsString();
			int targetPort = Integer.parseInt(getSiteFieldByName(target, "port"));//target.getAsJsonObject().get("port").getAsInt();
			
			
			
			
			
			
			String targetrootFolder = target.getAsJsonObject().get("rootFolder").getAsString();
			
			
			int totalFiles = listFilesToMove.size();
			int successFiles = 0;
			
			
			
			
			
			if (targetProtocol.equalsIgnoreCase("sftp")) {

				sftpChannelOut = null;
				channelOut = null;
				sessionOut = null;
				jschOut = new JSch();
				sessionOut = jschOut.getSession(targetUsername, targetHost, targetPort);
				sessionOut.setConfig("StrictHostKeyChecking", "no");
				sessionOut.setPassword(targetPassword);
				sessionOut.connect();
				channelOut = sessionOut.openChannel("sftp");
				channelOut.connect();
				sftpChannelOut = (ChannelSftp) channelOut;
				sftpChannelOut.cd(targetrootFolder);
				
				if (sourceProtocol.equalsIgnoreCase("sftp")) {
					for (String fileName : listFilesToMove){
						clear_gis();
						gis = pgpStream(sftpChannelIn.get(fileName), pgp);
						
						String renamed = fileName;
						
						// renamed by pgp
						try {
							String pgpRenameLogic = pgp.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, pgpRenameLogic);
						} catch (Exception ex) { }
						
						// renamed by config
						try {
							String cfgRenameLogic = config.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, cfgRenameLogic);
						} catch (Exception ex) { }

						// transfer from sftp to sftp
						try {
							sftpChannelOut.put(gis, renamed);
							successFiles++;
						} catch (Exception ex) {
							if (successFiles > 0) {
								ret = TaskStatus.PARTIALSUCCESS;								
							}
							log(workspace, taskId, LogType.ERROR, "Task Error", "Failed to move file ["+sourceProtocol+"->"+targetProtocol+"] \"" + fileName + "\", " + ex);
						}
					}
					
				} else if (sourceProtocol.equalsIgnoreCase("ftp")) {
					for (String fileName : listFilesToMove){
						clear_gis();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ftpClientIn.retrieveFile(fileName, bos);
						gis = pgpStream(new ByteArrayInputStream(bos.toByteArray()), pgp);
						
						String renamed = fileName;
						
						// renamed by pgp
						try {
							String pgpRenameLogic = pgp.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, pgpRenameLogic);
						} catch (Exception ex) { }
						
						// renamed by config
						try {
							String cfgRenameLogic = config.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, cfgRenameLogic);
						} catch (Exception ex) { }

						// transfer from ftp to sftp
						try {
							sftpChannelOut.put(gis, renamed);
							successFiles++;
						} catch (Exception ex) {
							if (successFiles > 0) {
								ret = TaskStatus.PARTIALSUCCESS;								
							}
							log(workspace, taskId, LogType.ERROR, "Task Error", "Failed to move file ["+sourceProtocol+"->"+targetProtocol+"] \"" + fileName + "\", " + ex);
						}
					}
					
				} else {
					throw new Exception("Unsupported protocol for \"" + sourceSiteName + "\": \"" + sourceProtocol + "\".");
				}
				
			} else if (targetProtocol.equalsIgnoreCase("ftp")) {
				
				ftpClientOut = new FTPClient();
				ftpClientOut.connect(targetHost, targetPort);
				ftpClientOut.enterLocalPassiveMode();
				if (ftpClientOut.login(targetUsername, targetPassword)) {
					ftpClientOut.cwd(targetrootFolder);
				} else {
					throw new Exception("Failed to login target FTP.");
				}
				
				if (sourceProtocol.equalsIgnoreCase("sftp")) {
					for (String fileName : listFilesToMove) {
						clear_gis();
						gis = pgpStream(sftpChannelIn.get(fileName), pgp);
						
						String renamed = fileName;
						
						// renamed by pgp
						try {
							String pgpRenameLogic = pgp.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, pgpRenameLogic);
						} catch (Exception ex) { }
						
						// renamed by config
						try {
							String cfgRenameLogic = config.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, cfgRenameLogic);
						} catch (Exception ex) { }
						
						// transfer from sftp to ftp
						try {

							if (ftpClientOut.storeFile(renamed, gis)) {
								successFiles++;
							} else {
								throw new Exception(ftpClientOut.getReplyString());
							}
						} catch (Exception ex) {
							if (successFiles > 0) {
								ret = TaskStatus.PARTIALSUCCESS;								
							}
							log(workspace, taskId, LogType.ERROR, "Task Error", "Failed to move file ["+sourceProtocol+"->"+targetProtocol+"] \"" + fileName + "\", " + ex);
						}
					}
					
				} else if (sourceProtocol.equalsIgnoreCase("ftp")) {
					for (String fileName : listFilesToMove) {
						clear_gis();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ftpClientIn.retrieveFile(fileName, bos);
						gis = pgpStream(new ByteArrayInputStream(bos.toByteArray()), pgp);
						
						String renamed = fileName;
						
						// renamed by pgp
						try {
							String pgpRenameLogic = pgp.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, pgpRenameLogic);
						} catch (Exception ex) { }
						
						// renamed by config
						try {
							String cfgRenameLogic = config.getAsJsonObject().get("fnrenameto").getAsString();
							renamed = renamedByLogic(renamed, cfgRenameLogic);
						} catch (Exception ex) { }
						
						// transfer from ftp to ftp
						try {

							if (ftpClientOut.storeFile(renamed, gis)) {
								successFiles++;
							} else {
								throw new Exception(ftpClientOut.getReplyString());
							}
						} catch (Exception ex) {
							if (successFiles > 0) {
								ret = TaskStatus.PARTIALSUCCESS;								
							}
							log(workspace, taskId, LogType.ERROR, "Task Error", "Failed to move file ["+sourceProtocol+"->"+targetProtocol+"] \"" + fileName + "\", " + ex);
						}
					}
				} else {
					throw new Exception("Unsupported protocol for \"" + sourceSiteName + "\": \"" + sourceProtocol + "\".");
				}
			} else {
				throw new Exception("Unsupported protocol for \"" + targetSiteName + "\": \"" + targetProtocol + "\".");
			}
			
			
			
			
			
			
			
			long duration = System.currentTimeMillis() - l0;
			
			
			
			

			if(successFiles == totalFiles) {
				ret = TaskStatus.SUCCESS;
			} else if (successFiles > 0) {
				ret = TaskStatus.PARTIALSUCCESS;
			} else {
				ret = TaskStatus.FAILED;
			}
			

			log(workspace, taskId, LogType.INFO, "Task Finished", "Task \"" + taskId + "\" execution finished in " + duration + " ms, Status = \"" + ret.toString() + "\". Files moved: ["+successFiles+"/"+totalFiles+"]");
			
		} catch (Exception ex) {
			ex.printStackTrace();
			log(workspace, taskId, LogType.ERROR, "Task Error", "Error executing task \"" + taskId + "\", " + ex);
		}
		
		// gis
		clear_gis();

		// cleanup inputstreams
		try {
			if (istreamsIn != null) {
				Set<String> ks = istreamsIn.keySet();
				for (String k : ks) { try { istreamsIn.get(k).close(); } catch (Exception ex) { }}
				istreamsIn.clear();
				istreamsIn = null;
			}
		} catch (Exception ex) { }

		// cleanup ftposs
		try {
			if (ftpossIn != null) {
				Set<String> ks = ftpossIn.keySet();
				for (String k : ks) { try { ftpossIn.get(k).close(); } catch (Exception ex) { }}
				ftpossIn.clear();
				ftpossIn = null;
			}
		} catch (Exception ex) { }
		
		// cleanup ftp client
		try { if (ftpClientIn != null) { ftpClientIn.disconnect(); } } catch (Exception ex) { }
		try { if (ftpClientOut != null) { ftpClientOut.disconnect(); } } catch (Exception ex) { }
		
		// cleanup input
		try { if (sessionIn != null) { sessionIn.disconnect(); } } catch (Exception ex) { }
		try { if (sftpChannelIn != null) { sftpChannelIn.disconnect(); } } catch (Exception ex) { }
		try { if (channelIn != null) { channelIn.disconnect(); } } catch (Exception ex) { }
		try { if (sessionIn != null) { sessionIn.disconnect(); } } catch (Exception ex) { }
		
		// cleanup output
		try { if (sessionOut != null) { sessionOut.disconnect(); } } catch (Exception ex) { }
		try { if (sftpChannelOut != null) { sftpChannelOut.disconnect(); } } catch (Exception ex) { }
		try { if (channelOut != null) { channelOut.disconnect(); } } catch (Exception ex) { }
		try { if (sessionOut != null) { sessionOut.disconnect(); } } catch (Exception ex) { }
		
		return ret;
	}
	
	private static JsonArray listSchedules(ArrayList<String> workspaces) {
		JsonArray ret = new JsonArray();
		
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
        
        for (String workspace : workspaces) {
            try {
    			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace + "/schedules");
    			conn = (HttpURLConnection) url.openConnection();
    			conn.setDoOutput(true);
    			conn.setRequestMethod("get".toUpperCase());
    	        
    	        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    	        String responseBody = br.lines().collect(Collectors.joining());
    	        
    			Gson gson = new Gson();
    			JsonElement element = gson.fromJson (responseBody, JsonElement.class);
    			JsonObject jsonObj = element.getAsJsonObject();
    			JsonArray arr = jsonObj.getAsJsonArray("list");
    			
    			for (JsonElement je : arr) {
    				System.out.println(je);
    			}
            } catch (Exception ex) {
    			ex.printStackTrace();
    			return null;
            }
        }
		
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
		
		/*
		ArrayList<String> ret = new ArrayList<String>();
		java.net.URL url = null;
		HttpURLConnection conn = null;
        BufferedReader br = null;
		try {
			url = new java.net.URL(WTRANSFER_API_ENDPOINT + "/workspaces");
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("get".toUpperCase());
	        
	        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String responseBody = br.lines().collect(Collectors.joining());
	        
			Gson gson = new Gson();
			JsonElement element = gson.fromJson (responseBody, JsonElement.class);
			JsonObject jsonObj = element.getAsJsonObject();
			JsonArray arr = jsonObj.getAsJsonArray("list");
			
			for (JsonElement je : arr) {
				JsonObject jo = je.getAsJsonObject();
				String workspace = jo.get("name").getAsString();
		        ret.add(workspace);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
		try { br.close(); } catch (Exception ex) { }
		try { conn.disconnect(); } catch (Exception ex) { }
		return ret;
		 * */
	}

	public static void update() {
		
		// list all workspaces
		ArrayList<String> workspaces = listWorkspaces();

		// ------------------------------------------------------------------------------------------------------------ // schedule begin
		
		// list schedules
		JsonArray schedules = listSchedules(workspaces);
		System.out.println(schedules);
		
		// ------------------------------------------------------------------------------------------------------------ // schedule end
		
		/*// list current pending tasks (limit?)
		JsonArray pendingTasks = listPendingTasks(workspaces, 100);
		System.out.println("pendingTasks: " + pendingTasks.size());
		
		// set them to inprogress
		JsonArray toExecute = new JsonArray();
		for (JsonElement task : pendingTasks) {
			int taskId = task.getAsJsonObject().get("id").getAsInt();
			String workspace = task.getAsJsonObject().get("workspace").getAsString();
			if (setTaskStatus(workspace, taskId, TaskStatus.INPROGRESS)) {
				toExecute.add(task);
			}
		}
		System.out.println("toExecute: " + toExecute.size());
		
		if (toExecute.size() > 0) { try { Thread.sleep(4000); } catch (Exception ex) { } }
		
		// try execute them
		for (JsonElement task : toExecute) {
			int taskId = task.getAsJsonObject().get("id").getAsInt();
			String workspace = task.getAsJsonObject().get("workspace").getAsString();
			setTaskStatus(workspace, taskId, executeTask(task));
		}*/
	}

	public static ZResult listObject(String workspace, String siteName) throws Exception {
		IFileServer.FileServer server = null;
		try {
			JsonElement json = ZWorker.getSiteByName(workspace, siteName);
			Site site = Site.parse(json);
			server = IFileServer.createServer(site);
			server.open();
			JsonArray array = server.listObjects();
			if (server != null) { server.close(); }
			ZResult result = new ZResult();
			result.statusCode = 200;
			result.content = "{\"objects\": " + array + "}";
			return result;
		} catch (Exception ex) {
			if (server != null) { server.close(); }
			throw ex;
		}
	}
}