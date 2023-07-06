package com.pttdigital.wtransfer;
import java.io.InputStream;

import org.wso2.carbon.esb.connector.ZConnector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ServerCustom extends IFileServer.FileServer {
	
	private String siteName = null;
	private String sessionId = null;

	public ServerCustom(Site site) {
		super(site.host, site.port, site.username, site.password);
		siteName = site.name;
	}

	@Override
	public void open() throws Exception {
		JsonObject p1 = new JsonObject();
		p1.addProperty("action", "server.open");
		p1.addProperty("site", siteName);
		sessionId = Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1).getAsJsonObject().get("id").getAsString();
	}

	@Override
	public void close() {
		try {
			JsonObject p1 = new JsonObject();
			p1.addProperty("action", "server.close");
			p1.addProperty("id", sessionId);
			Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1);
		} catch (Exception e) { }
	}

	@Override
	public JsonArray listObjects(String folder) throws Exception {
		JsonObject p1 = new JsonObject();
		p1.addProperty("action", "object.list");
		p1.addProperty("id", sessionId);
		p1.addProperty("directory", folder);
		return Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1).getAsJsonObject().get("objects").getAsJsonArray();
	}

	@Override
	public boolean fileExists(String file) throws Exception {
		JsonObject p1 = new JsonObject();
		p1.addProperty("action", "file.exists");
		p1.addProperty("id", sessionId);
		p1.addProperty("file", file);
		return Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1).getAsJsonObject().get("exists").getAsBoolean();
	}

	@Override
	public boolean directoryExists(String folder) throws Exception {
		try {
			JsonObject p1 = new JsonObject();
			p1.addProperty("action", "directory.exists");
			p1.addProperty("id", sessionId);
			p1.addProperty("directory", folder);
			return Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1).getAsJsonObject().get("exists").getAsBoolean();			
		} catch (Exception ex) {
			if (ex.getMessage().contains("Received HTTP error: 404")) {
				return false;
			}
			throw ex;
		}
	}

	@Override
	public void createDirectory(String folder) throws Exception {
		JsonObject p1 = new JsonObject();
		p1.addProperty("action", "directory.create");
		p1.addProperty("id", sessionId);
		p1.addProperty("directory", folder);
		Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1);
	}

	@Override
	public void deleteDirectory(String folder) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteFile(String fileName) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move_internal(String fileSource, String fileNameTarget) throws Exception {
		JsonObject p1 = new JsonObject();
		p1.addProperty("action", "file.move_internal");
		p1.addProperty("id", sessionId);
		p1.addProperty("sourceFile", fileSource);
		p1.addProperty("targetFile", fileNameTarget);
		Client.getJsonResponse(ZConnector.Constant.CUSTOM_FILE_SERVER_ENDPOINT, "POST", null, p1);
	}

	@Override
	public void move_external(String localFilePath, String transferMode, String remoteIp, int remotePort,
			String remoteProtocol, String remoteUser, String remotePass, String remoteKeyPath, String remoteFileName)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InputStream getInputStream(String absFilePath) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
}