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
		
		IFileServer.FileServer serverSource = null;
		IFileServer.FileServer serverTarget = null;
		
		// parse request
		JsonObject o = new Gson().fromJson(json, JsonObject.class);
		Site source = (Site) DB.parse(Site.class, o.get("siteSource").getAsJsonObject());
		Site target = (Site) DB.parse(Site.class, o.get("siteTarget").getAsJsonObject());
		Schedule schedule = (Schedule) DB.parse(Schedule.class, o.get("schedule").getAsJsonObject());
		Item item = (Item) DB.parse(Item.class, o.get("item").getAsJsonObject());
		
		String fileName = item.fileNameArchive;
		
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
			String targetFileName = targetFolder + "/" + fileName.substring(0, fileName.length() - ".arc".length());
			
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
				serverTarget.receiveFileFromInputStream(isource, targetFileName, true);
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
					Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
					int retryRemaining = item.retryRemaining - 1;
					if (retryRemaining < 1) {
						Timestamp timeLatestRetry = now;
						Item.Status status = Item.Status.FAILED;
						Timestamp timeNextRetry = now;
						Client.updateOnExecuteFailed(item, status, retryRemaining, timeNextRetry, timeLatestRetry);
					} else {
						Timestamp timeLatestRetry = now;
						Item.Status status = Item.Status.WAITING_FOR_RETRY;
						Timestamp timeNextRetry = new Timestamp(now.getTime() + item.retryIntervalMs);
						Client.updateOnExecuteFailed(item, status, retryRemaining, timeNextRetry, timeLatestRetry);
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
			move(new String(bodyRaw));
			return ZConnector.ZResult.OK_200("{\"status\": \"moved\"}");
		} catch (Exception e) {
			e.printStackTrace();
			return ZConnector.ZResult.ERROR_500(e);
		}
	}
	
	public static void main(String[] arg) throws Exception {
		String payload = "{\"schedule\":{\"workspace\":\"admin-42\",\"pgpKeyPath\":null,\"description\":null,\"pgpDirection\":null,\"validFrom\":null,\"enabled\":true,\"staticDirSource\":\"/ftp/alpineftp/fi\",\"fnIsFileToMove\":\"function(x){return x.endsWith(\\\".csv\\\");}\",\"fnPgpRenameTo\":null,\"modified\":\"2023-03-20 16:41:33\",\"fnArchiveRenameTo\":\"f(x){return x+\\\".arc\\\";}\",\"plan\":\"*-*-* 23:12:00\",\"useDynamicDirSource\":false,\"fnRenameTo\":\"f(x){return x+\\\".success\\\";}\",\"siteSource\":\"zparin-ftp\",\"retryCount\":16,\"created\":\"2023-03-19 22:12:08\",\"retryIntervalMs\":120000,\"siteTarget\":\"zparin-sftp\",\"fnDynamicDirTarget\":\"string\",\"previousCheckpoint\":\"2023-03-19 23:13:09\",\"staticDirTarget\":\"/upload/test-to\",\"archiveFolder\":\"/arc\",\"fnDynamicDirSource\":\"string\",\"pgpKeyPassword\":null,\"name\":\"schedule-0002\",\"validUntil\":null,\"useDynamicDirTarget\":false,\"workerThreads\":1},\"siteSource\":{\"workspace\":\"admin-42\",\"protocol\":\"ftp\",\"password\":\"alpineftp\",\"port\":21,\"created\":\"2023-03-19 21:49:01\",\"keyPath\":null,\"name\":\"zparin-ftp\",\"host\":\"localhost\",\"description\":\"test local ftp\",\"modified\":\"2023-03-19 21:49:01\",\"username\":\"alpineftp\"},\"siteTarget\":{\"workspace\":\"admin-42\",\"protocol\":\"sftp\",\"password\":\"pass\",\"port\":22,\"created\":\"2023-03-19 21:49:15\",\"keyPath\":null,\"name\":\"zparin-sftp\",\"host\":\"localhost\",\"description\":\"test local sftp\",\"modified\":\"2023-03-19 21:49:15\",\"username\":\"foo\"},\"session\":{\"schedule\":\"schedule-0002\",\"workspace\":\"admin-42\",\"created\":null,\"description\":\"Auto-generated by schedule schedule-0002. [4196d213c6fc418d868f19f15a7fc499]\",\"modified\":null,\"id\":60,\"status\":\"CREATED\"},\"item\":{\"workspace\":\"admin-42\",\"fileName\":\"Accrued_Dsource_0123_BSA_270223 1139.csv\",\"retryRemaining\":17,\"fileNameArchive\":\"Accrued_Dsource_0123_BSA_270223 1139.csv.arc\",\"session\":60,\"created\":null,\"retryIntervalMs\":120000,\"description\":\"Auto-generated by schedule schedule-0002. [4196d213c6fc418d868f19f15a7fc499]\",\"fnCallback\":null,\"folder\":\"/ftp/alpineftp/fi\",\"timeLatestRetry\":\"1970-01-01 00:00:00\",\"name\":\"admin-42:60:Accrued_Dsource_0123_BSA_270223 1139.csv\",\"modified\":null,\"retryQuota\":16,\"folderArchive\":\"/ftp/alpineftp/fi/archive\",\"timeNextRetry\":\"1970-01-01 00:00:00\",\"status\":null}}";
		move(payload);
	}
}