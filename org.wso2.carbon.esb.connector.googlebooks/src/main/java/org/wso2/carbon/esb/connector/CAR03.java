package org.wso2.carbon.esb.connector;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CAR03 {

	private static String fnRenameTo(Config config, String sourceFileName) {
		// TODO, rename
		return sourceFileName;
	}

	private static String fnRenameTo(PGP pgp, String sourceFileName) {
		// TODO, pgp rename
		return sourceFileName;
	}
	
	private static IFileServer.FileServer createServer(Site site) throws Exception {
		if (site.protocol.equalsIgnoreCase("sftp")) {
			return new IFileServer.ServerSFTP(site.host, site.port, site.username, site.password, site.rootFolder, site.keyPath);
		}
		if (site.protocol.equalsIgnoreCase("ftp")) {
			return new IFileServer.ServerFTP(site.host, site.port, site.username, site.password, site.rootFolder);
		}
		throw new Exception("Failed to create FileServer: unsupported protocol \"" + site.protocol + "\".");
	}

	private static InputStream applyPGP(PGP pgp, InputStream isource) throws Exception {
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
	}
	
	public static void move(String json) throws Exception {
		
		
		
		IFileServer.FileServer serverSource = null;
		IFileServer.FileServer serverTarget = null;
		
		// for sftp -> ftp (alpine localhost)
		//String json = "{\"target\":{\"workspace\":\"zparinthornk\",\"protocol\":\"ftp\",\"password\":\"alpineftp\",\"port\":21,\"created\":\"2023-02-21 15:58:42\",\"rootFolder\":\"/ftp/alpineftp/fi\",\"name\":\"notebook-parinthorn-ftp\",\"host\":\"localhost\",\"modified\":\"2023-02-21 15:58:42\",\"username\":\"alpineftp\"},\"source\":{\"workspace\":\"zparinthornk\",\"protocol\":\"sftp\",\"password\":\"pass\",\"port\":22,\"created\":\"2023-02-21 16:02:26\",\"rootFolder\":\"/upload\",\"name\":\"notebook-parinthorn\",\"host\":\"localhost\",\"modified\":\"2023-02-21 16:02:26\",\"username\":\"foo\"},\"pgp\":null,\"config\":{\"workspace\":\"zparinthornk\",\"created\":\"2023-02-21 18:31:27\",\"retryCount\":8,\"retryIntervalMs\":60000,\"replace\":1,\"description\":\"description\",\"archiveFolder\":\"/archive\",\"deleteAfterSuccess\":1,\"fnIsFileNameToMove\":\"function(x){return x.endsWith(\\\".csv\\\");}\",\"name\":\"cfg-01\",\"fnArcRenameTo\":\"function(x){return x+\\\".arc\\\";}\",\"modified\":\"2023-02-21 18:34:29\",\"fnRenameTo\":\"function(x){return x;}\"},\"fileName\":\"PTT_ECFC_1_20230121_080104.csv.pgp\",\"item\":{\"workspace\":\"zparinthornk\",\"task\":20,\"retryRemaining\":9,\"timeLatestRetry\":\"1970-01-01 00:00:00\",\"created\":\"2023-03-02 21:05:30\",\"retryIntervalMs\":60000,\"name\":\"zparinthornk:20:Accrued_Dsource_0123_BSA_270223_1139.csv\",\"modified\":\"2023-03-02 21:05:30\",\"retryQuota\":8,\"timeNextRetry\":\"1970-01-01 00:00:00\",\"status\":\"CREATED\"}}";
		
		// for sftp -> ftp
		//String json = "{\"target\":{\"workspace\":\"zparinthornk\",\"protocol\":\"ftp\",\"password\":\"ZaSKHLIUC%\",\"port\":5000,\"created\":\"2023-02-21 15:58:42\",\"rootFolder\":\"/PTT-Debt Portfolio_Test/Archive\",\"name\":\"notebook-parinthorn-ftp\",\"host\":\"10.120.0.243\",\"modified\":\"2023-02-21 15:58:42\",\"username\":\"debttestusr\"},\"source\":{\"workspace\":\"zparinthornk\",\"protocol\":\"sftp\",\"password\":\"pass\",\"port\":22,\"created\":\"2023-02-21 16:02:26\",\"rootFolder\":\"/upload\",\"name\":\"notebook-parinthorn\",\"host\":\"10.236.36.184\",\"modified\":\"2023-02-21 16:02:26\",\"username\":\"foo\"},\"pgp\":null,\"config\":{\"workspace\":\"zparinthornk\",\"created\":\"2023-02-21 18:31:27\",\"retryCount\":8,\"retryIntervalMs\":60000,\"replace\":1,\"description\":\"description\",\"archiveFolder\":\"/archive\",\"deleteAfterSuccess\":1,\"fnIsFileNameToMove\":\"function(x){return x.endsWith(\\\".csv\\\");}\",\"name\":\"cfg-01\",\"fnArcRenameTo\":\"function(x){return x+\\\".arc\\\";}\",\"modified\":\"2023-02-21 18:34:29\",\"fnRenameTo\":\"function(x){return x;}\"},\"fileName\":\"PTT_ECFC_1_20230121_080104.csv.pgp\",\"item\":{\"workspace\":\"zparinthornk\",\"task\":20,\"retryRemaining\":9,\"timeLatestRetry\":\"1970-01-01 00:00:00\",\"created\":\"2023-03-02 21:05:30\",\"retryIntervalMs\":60000,\"name\":\"zparinthornk:20:Accrued_Dsource_0123_BSA_270223_1139.csv\",\"modified\":\"2023-03-02 21:05:30\",\"retryQuota\":8,\"timeNextRetry\":\"1970-01-01 00:00:00\",\"status\":\"CREATED\"}}";
		
		// for ftp -> sftp
		//String json = "{\"source\":{\"workspace\":\"zparinthornk\",\"protocol\":\"ftp\",\"password\":\"##3G4H#HE%\",\"port\":21,\"created\":\"2023-02-21 15:58:42\",\"rootFolder\":\"/FTP-ForUpload/SAP_Smart_Accrued\",\"name\":\"qas-smartaccrued-dsource\",\"host\":\"pds-dsourwb-t01.pttdigital.corp\",\"modified\":\"2023-02-21 15:58:42\",\"username\":\"dsource-qa.pttdigital.com|pds-dsourwb-t01\\\\wso2ftp\"},\"target\":{\"workspace\":\"zparinthornk\",\"protocol\":\"sftp\",\"password\":\"pass\",\"port\":22,\"created\":\"2023-02-21 16:02:26\",\"rootFolder\":\"/upload\",\"name\":\"notebook-parinthorn\",\"host\":\"10.236.36.184\",\"modified\":\"2023-02-21 16:02:26\",\"username\":\"foo\"},\"pgp\":null,\"config\":{\"workspace\":\"zparinthornk\",\"created\":\"2023-02-21 18:31:27\",\"retryCount\":8,\"retryIntervalMs\":60000,\"replace\":1,\"description\":\"description\",\"archiveFolder\":\"/archive\",\"deleteAfterSuccess\":1,\"fnIsFileNameToMove\":\"function(x){return x.endsWith(\\\".csv\\\");}\",\"name\":\"cfg-01\",\"fnArcRenameTo\":\"function(x){return x+\\\".arc\\\";}\",\"modified\":\"2023-02-21 18:34:29\",\"fnRenameTo\":\"function(x){return x;}\"},\"fileName\":\"Accrued_Dsource_0123_BSA_270223_1139.csv\",\"item\":{\"workspace\":\"zparinthornk\",\"task\":20,\"retryRemaining\":9,\"timeLatestRetry\":\"1970-01-01 00:00:00\",\"created\":\"2023-03-02 21:05:30\",\"retryIntervalMs\":60000,\"name\":\"zparinthornk:20:Accrued_Dsource_0123_BSA_270223_1139.csv\",\"modified\":\"2023-03-02 21:05:30\",\"retryQuota\":8,\"timeNextRetry\":\"1970-01-01 00:00:00\",\"status\":\"CREATED\"}}";
		
		// parse request
		JsonObject o = new Gson().fromJson(json, JsonObject.class);
		Site source = Site.parse(o.get("source"));
		Site target = Site.parse(o.get("target"));
		Config config = Config.parse(o.get("config"));
		PGP pgp = null; try { pgp = PGP.parse(o.get("pgp")); } catch (Exception ex) { }
		String fileName = o.get("fileName").getAsString();
		Item item = null; try { item = Item.parse(o.get("item")); } catch (Exception ex) { }
		
		ClientLib.setItemStatus(item, Item.Status.EXECUTING);
		ClientLib.addItemLog(item, Log.Type.INFO, "setItemStatus", "set status to \"EXECUTING\".");
		
		try {
			
			// ---------------------------------------------------------------------------------------------------- //
			
			String connSource = "host["+source.host+"], port["+source.port+"], protocol["+source.protocol+"], username["+source.username+"], password["+source.password+"], keyPath["+source.keyPath+"]";
			try {
				serverSource = createServer(source);
				ClientLib.addItemLog(item, Log.Type.INFO, "connection", "Connecting source: host["+source.host+"], port["+source.port+"], protocol["+source.protocol+"], username["+source.username+"], password["+source.password+"], keyPath["+source.keyPath+"]");
				serverSource.open();
			} catch (Exception ex) {
				throw new Exception("Error while connecting to source: " + connSource + ", " + ex + ". Please make sure connection parameters are correct.");
			}
			
			InputStream isource = null;
			try {
				isource = serverSource.getInputStream(fileName);
			} catch (Exception ex) {
				throw new Exception("Error while initializing source InputStream: " + ex + ". Please make sure that source file \"" + source.rootFolder + "\\" + fileName + "\" is accessible.");
			}
			
			
			ClientLib.addItemLog(item, Log.Type.INFO, "connection", "Applying PGP to InputStream isource: " + isource);
			try {
				isource = applyPGP(pgp, isource);
			} catch (Exception ex) {
				throw new Exception("Error while applying PGP to source InputStream: " + ex);
			}
			
			
			String connTarget = "host["+target.host+"], port["+target.port+"], protocol["+target.protocol+"], username["+target.username+"], password["+target.password+"], keyPath["+target.keyPath+"]";
			try {
				serverTarget = createServer(target);
				ClientLib.addItemLog(item, Log.Type.INFO, "connection", connTarget);
				serverTarget.open();
			} catch (Exception ex) {
				throw new Exception("Error while connecting to target: " + connTarget + ", " + ex + ". Please make sure connection parameters are correct.");
			}
			
			String targetFileName = fileName;
			try {
				targetFileName = fnRenameTo(config, fileName);
			} catch (Exception ex) {
				throw new Exception("Error while calculating targetFileName (via config): " + ex);
			}
			
			try {
				targetFileName = fnRenameTo(pgp, fileName);
			} catch (Exception ex) {
				throw new Exception("Error while calculating targetFileName (via pgp): " + ex);
			}

			ClientLib.addItemLog(item, Log.Type.INFO, "connection", "Uploading file \"" + targetFileName + "\".");
			try {
				serverTarget.receiveFileFromInputStream(isource, targetFileName, true);
			} catch (Exception ex) {
				throw new Exception("Error while uploading file: InputStream[" + isource + "], targetFileName[" + targetFileName + "]: " + ex + ". Please also make sure that target folder \"" + target.rootFolder + "\" is accessible.");
			}
			
			ClientLib.setItemStatus(item, Item.Status.SUCCESS);
			ClientLib.addItemLog(item, Log.Type.INFO, "setItemStatus", "set status to \"SUCCESS\".");

			// ---------------------------------------------------------------------------------------------------- //
			
			if (serverSource != null) { serverSource.close(); }
			if (serverTarget != null) { serverTarget.close(); }
		} catch (Exception ex) {
			if (serverSource != null) { serverSource.close(); }
			if (serverTarget != null) { serverTarget.close(); }
			ClientLib.setItemStatus(item, Item.Status.WAITING_FOR_RETRY);
			ClientLib.addItemLog(item, Log.Type.ERROR, "Exception", ex + "");
			ClientLib.addItemLog(item, Log.Type.INFO, "setItemStatus", "set status to \"WAITING_FOR_RETRY\".");
			
			// prepare for next retry
			if (item != null) {
				try {
					Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
					int retryRemaining = item.retryRemaining - 1;
					if (retryRemaining < 1) {
						Timestamp timeLatestRetry = now;
						Item.Status status = Item.Status.FAILED;
						Timestamp timeNextRetry = now;
						ClientLib.updateOnExecuteFailed(item, status, retryRemaining, timeNextRetry, timeLatestRetry);
					} else {
						Timestamp timeLatestRetry = now;
						Item.Status status = Item.Status.WAITING_FOR_RETRY;
						Timestamp timeNextRetry = new Timestamp(now.getTime() + item.retryIntervalMs);
						ClientLib.updateOnExecuteFailed(item, status, retryRemaining, timeNextRetry, timeLatestRetry);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
			throw ex;
		}
	}

	public static ZConnector.ZResult process(String path, String method, HashMap<String, String> query, HashMap<String, String> header, byte[] bodyRaw) {
		
		if (!ZAPIV2.match(path, method, "/, post")) {
			return ZConnector.ZResult.ERROR_501(method, path);
		}
		
		ZConnector.ZResult ret = new ZConnector.ZResult();
		
		try {
			move(new String(bodyRaw));
			ret.statusCode = 200;
			ret.content = "{\"status\": \"moved\"}";
		} catch (Exception e) {
			e.printStackTrace();
			return ZConnector.ZResult.ERROR_500(e);
		}
		
		return ret;
	}
}