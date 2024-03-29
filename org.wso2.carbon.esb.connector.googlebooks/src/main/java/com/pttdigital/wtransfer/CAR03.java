package com.pttdigital.wtransfer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.wso2.carbon.esb.connector.IFileServer.FileServer;
import org.wso2.carbon.esb.connector.PgpHelper;
import org.wso2.carbon.esb.connector.ZAPIV2;
import org.wso2.carbon.esb.connector.ZConnector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CAR03 {

	/*private static String fnRenameTo(Config config, String sourceFileName) throws Exception {
		if (config.fnRenameTo == null) {
			return sourceFileName;
		}
		if (config.fnRenameTo.length() == 0) {
			return sourceFileName;
		}
		return FileNaming.getRenamed(sourceFileName, config.fnRenameTo);
	}

	private static String fnArcRenameTo(Config config, String sourceFileName) throws Exception {
		if (config.fnRenameTo == null) {
			return sourceFileName;
		}
		if (config.fnRenameTo.length() == 0) {
			return sourceFileName;
		}
		return FileNaming.getRenamed(sourceFileName, config.fnArcRenameTo);
	}

	private static String fnRenameTo(PGP pgp, String sourceFileName) throws Exception {
		if (pgp.fnRenameTo == null) {
			return sourceFileName;
		}
		if (pgp.fnRenameTo.length() == 0) {
			return sourceFileName;
		}
		try {
			return FileNaming.getRenamed(sourceFileName, pgp.fnRenameTo);
		} catch (Exception e) {
			e.printStackTrace();
			if (pgp.direction.equalsIgnoreCase("encrypt") && !sourceFileName.toLowerCase().endsWith(".pgp")) {
				return sourceFileName + ".pgp";
			} else if (pgp.direction.equalsIgnoreCase("decrypt") && sourceFileName.toLowerCase().endsWith(".pgp")) {
				return sourceFileName.substring(0, sourceFileName.length() - 4);
			}
			throw new Exception("Error while calculating renamed PGP file, sourceFileName[" + sourceFileName + "], direction[" + pgp.direction + "] + logic[" + pgp.fnRenameTo + "]: " + e);
		}
	}*/

	/*private static InputStream applyPGP(PGP pgp, InputStream isource) throws Exception {
		if (pgp == null) {
			return isource;
		}
		String pgpDirection = pgp.direction;
		String pgpKeyPath = pgp.keyPath;
		if (pgpDirection.equalsIgnoreCase("encrypt")) {
			byte[] b = PgpHelper.getInstance().encrypt(isource, pgpKeyPath);
			isource.close();
			return new ByteArrayInputStream(b);
		} else if (pgpDirection.equalsIgnoreCase("decrypt")) {
			String keyPassword = pgp.keyPassword;
			byte[] b = PgpHelper.getInstance().decrypt(isource, pgpKeyPath, keyPassword.toCharArray());
			isource.close();
			return new ByteArrayInputStream(b);
		}
		throw new Exception("Failed to apply PGP to InputStream, unsupported direction \"" + pgpDirection + "\".");
	}*/
	
	public static void move(String json) throws Exception {
		JsonObject o = new Gson().fromJson(json, JsonObject.class);
		Site source = (Site) DB.parse(Site.class, o.get("siteSource").getAsJsonObject());
		Site target = (Site) DB.parse(Site.class, o.get("siteTarget").getAsJsonObject());
		Schedule schedule = (Schedule) DB.parse(Schedule.class, o.get("schedule").getAsJsonObject());
		Item item = (Item) DB.parse(Item.class, o.get("item").getAsJsonObject());
		do_move(source, target, schedule, item);
	}

	public static void do_move(Site source, Site target, Schedule schedule, Item item) throws Exception {
		
		String fileName = item.fileNameArchive;
		IFileServer.FileServer serverSource = null;
		IFileServer.FileServer serverTarget = null;
		
		Client.setItemStatus(item, Item.Status.EXECUTING);
		Client.addItemLog(item, Log.Type.INFO, "setItemStatus", "set status to \"EXECUTING\".");
		
		try {
			
			// ---------------------------------------------------------------------------------------------------- //
			
			String connSource = "host["+source.host+"], port["+source.port+"], protocol["+source.protocol+"], username["+source.username+"], password["+source.password+"], keyPath["+source.keyPath+"]";
			try {
				serverSource = IFileServer.createServer(source);
				Client.addItemLog(item, Log.Type.INFO, "connection", "Connecting source: host["+source.host+"], port["+source.port+"], protocol["+source.protocol+"], username["+source.username+"], password["+source.password+"], keyPath["+source.keyPath+"]");
				serverSource.open();
			} catch (Exception ex) {
				throw new Exception("Error while connecting to source: " + connSource + ", " + ex + ". Please make sure connection parameters are correct.");
			}
			
			InputStream isource = null;
			try {
				isource = serverSource.getInputStream(item.folderArchive + "/" + fileName);
				if (isource == null) {
					throw new Exception("serverSource.getInputStream(" + item.folderArchive + "/" + fileName + ") returns null.");
				}
			} catch (Exception ex) {
				throw new Exception("Error while initializing source InputStream: " + ex + ". Please make sure that source file \"" + fileName + "\" is accessible in folder \"" + item.folderArchive + "\".");
			}
			
			/*Client.addItemLog(item, Log.Type.INFO, "connection", "Applying PGP to InputStream isource: " + isource);
			try {
				isource = applyPGP(pgp, isource);
			} catch (Exception ex) {
				throw new Exception("Error while applying PGP to source InputStream: " + ex);
			}*/
			
			String connTarget = "host["+target.host+"], port["+target.port+"], protocol["+target.protocol+"], username["+target.username+"], password["+target.password+"], keyPath["+target.keyPath+"]";
			try {
				serverTarget = IFileServer.createServer(target);
				Client.addItemLog(item, Log.Type.INFO, "connection", connTarget);
				serverTarget.open();
			} catch (Exception ex) {
				throw new Exception("Error while connecting to target: " + connTarget + ", " + ex + ". Please make sure connection parameters are correct.");
			}
			
			String targetFolder = schedule.staticDirTarget;
			String targetFileName = targetFolder + "/" + item.fileName;//fileName.substring(0, fileName.length() - ".arc".length());
			
			/*if (pgp != null) {
				try {
					targetFileName = fnRenameTo(pgp, fileName);
				} catch (Exception ex) {
					throw new Exception("Error while calculating targetFileName (via pgp): " + ex);
				}
			}*/

			Client.addItemLog(item, Log.Type.INFO, "connection", "Uploading file \"" + targetFileName + "\".");
			try {
				if (!serverTarget.directoryExists(targetFolder)) {
					try {

						serverTarget.createDirectory(targetFolder);
					} catch (Exception ex) {
						throw new Exception("Error serverTarget.createDirectory(" + targetFolder + "), " + ex);
					}
				}
				
				// attempt to upload the file
				int attempts = 25;
				for (int i = 0; i < attempts; i++) {
					try {
						serverTarget.receiveFileFromInputStream(isource, targetFileName, true);
						break;
					} catch (Exception ex) { 
						if (i == attempts - 1) {
							throw ex;
						} else {
							Thread.sleep(400);
						}
					}
				}
				
				
				
			} catch (Exception ex) {
				throw new Exception("Error while uploading file to \"" + target.name + "\": InputStream[" + isource + "], targetFileName[" + targetFileName + "]: " + ex + ". Please also make sure that target folder \"" + schedule.staticDirTarget + "\" is accessible.");
			}
			
			Client.setItemStatus(item, Item.Status.SUCCESS);
			Client.addItemLog(item, Log.Type.INFO, "setItemStatus", "set status to \"SUCCESS\".");

			// ---------------------------------------------------------------------------------------------------- //
			
			if (serverSource != null) { serverSource.close(); }
			if (serverTarget != null) { serverTarget.close(); }
		} catch (Exception ex) {
			if (serverSource != null) { serverSource.close(); }
			if (serverTarget != null) { serverTarget.close(); }
			Client.setItemStatus(item, Item.Status.WAITING_FOR_RETRY);
			Client.addItemLog(item, Log.Type.ERROR, "Exception", ex + "");
			Client.addItemLog(item, Log.Type.INFO, "setItemStatus", "set status to \"WAITING_FOR_RETRY\".");
			
			// prepare for next retry
			if (item != null) {
				try {
					
					long ms_now = Calendar.getInstance().getTimeInMillis();
					
					Timestamp t_now = new Timestamp(ms_now);
					Timestamp t_nxt = new Timestamp(ms_now + item.retryIntervalMs);
					
					int retryRemaining = item.retryRemaining - 1;
					if (retryRemaining < 1) {
						Item.Status status = Item.Status.FAILED;
						Client.updateOnExecuteFailed(item, status, retryRemaining, t_nxt, t_now);
						
						// TODO: callback notofication
						
					} else {
						Item.Status status = Item.Status.WAITING_FOR_RETRY;
						Client.updateOnExecuteFailed(item, status, retryRemaining, t_nxt, t_now);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
			throw ex;
		}
	}

	public static ZConnector.ZResult process(String path, String method, HashMap<String, String> query, HashMap<String, String> header, byte[] bodyRaw) {
		if (!ZAPIV2.match(path, method, "/, post")) { return ZConnector.ZResult.ERROR_501(method, path); }
		try {
			move_custom(new String(bodyRaw));
			return ZConnector.ZResult.OK_200("{\"status\": \"moved\"}");
		} catch (Exception e) {
			e.printStackTrace();
			return ZConnector.ZResult.ERROR_500(e);
		}
	}
	
	public static void move_custom(String string) throws Exception {
		JsonObject o = new Gson().fromJson(string, JsonObject.class);
		o.addProperty("action", "transfer_car03");
		Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, o);
	}
}