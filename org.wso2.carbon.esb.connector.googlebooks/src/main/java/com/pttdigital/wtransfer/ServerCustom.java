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
		// TODO Auto-generated method stub
		
	}

	@Override
	public JsonArray listObjects(String folder) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean fileExists(String file) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean directoryExists(String folder) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createDirectory(String folder) throws Exception {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
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