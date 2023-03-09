package org.wso2.carbon.esb.connector;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public interface IFileServer {

	public void open() throws Exception;
	public InputStream getInputStream(String fileName) throws Exception;
	public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception;
	public void close();
	public JsonArray listObjects() throws Exception;
	
	public static abstract class FileServer implements IFileServer {
		public String host;
		public int port;
		public String username;
		public String password;
		public String rootFolder;
		
		public FileServer(String _host, int _port, String _username, String _password, String _rootFolder) {
			host = _host;
			port = _port;
			username = _username;
			password = _password;
			rootFolder = _rootFolder;
		}
	}
	
	public static IFileServer.FileServer createServer(Site site) throws Exception {
		if (site.protocol.equalsIgnoreCase("sftp")) {
			return new IFileServer.ServerSFTP(site.host, site.port, site.username, site.password, site.rootFolder, site.keyPath);
		}
		if (site.protocol.equalsIgnoreCase("ftp")) {
			return new IFileServer.ServerFTP(site.host, site.port, site.username, site.password, site.rootFolder);
		}
		if (site.protocol.equalsIgnoreCase("ftps")) {
			return new IFileServer.ServerFTPS(site.host, site.port, site.username, site.password, site.rootFolder);
		}
		throw new Exception("Failed to create FileServer: unsupported protocol \"" + site.protocol + "\".");
	}
	
	// TODO: SFTP
	public static class ServerSFTP extends IFileServer.FileServer {
		
		private ChannelSftp sftpChannel;
		private Channel channel;
		private Session session;
		private JSch jsch;
		//private String authenticationKey;
		
		public ServerSFTP(String _host, int _port, String _username, String _password, String _rootFolder, String _authenticationKey) {
			super(_host, _port, _username, _password, _rootFolder);
			//authenticationKey = _authenticationKey;
		}
		
		@Override
		public void open() throws Exception {
			jsch = new JSch();
			session = jsch.getSession(username, host, port);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(password);
			session.connect();
			channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
		}
		
		@Override
		public InputStream getInputStream(String fileName) throws Exception {
			return sftpChannel.get(rootFolder + "/" + fileName);
		}
		
		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception {
			sftpChannel.put(inputStream, rootFolder + "/" + targetFileName);
		}
		
		@Override
		public void close() {
			if (sftpChannel != null) { try { sftpChannel.disconnect(); } catch (Exception ex) { } }
			if (channel != null) { try { channel.disconnect(); } catch (Exception ex) { } }
			if (session != null) { try { session.disconnect(); } catch (Exception ex) { } }
		}

		@Override
		public JsonArray listObjects() throws Exception {
			Vector<?> vFiles = sftpChannel.ls(rootFolder);
			ArrayList<Object> arr = new ArrayList<Object>();
			for (Object o : vFiles) {
				LsEntry entry = (LsEntry) o;
				SftpATTRS attrs = entry.getAttrs();
				Item.Info info = new Item.Info(entry.getFilename(), attrs.getSize(), attrs.isDir(), new Timestamp(Long.parseLong(attrs.getMTime() + "000")));
				arr.add(Item.Info.toDictionary(info));
			}
			return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
		}
	}
	
	// TODO: FTP
	public static class ServerFTP extends IFileServer.FileServer {
		
		private FTPClient ftpClient;

		public ServerFTP(String _host, int _port, String _username, String _password, String _rootFolder) {
			super(_host, _port, _username, _password, _rootFolder);
		}

		@Override
		public void open() throws Exception {
			ftpClient = new FTPClient();
			ftpClient.connect(host, port);
			ftpClient.enterLocalPassiveMode();
			if (ftpClient.login(username, password)) {
				ftpClient.cwd(rootFolder);
			} else {
				throw new Exception("Failed to login FTPClient, host[" + host + "], port[" + port + "], username[" + username + "], password[" + password + "].");
			}
		}

		@Override
		public InputStream getInputStream(String fileName) throws Exception {
			return ftpClient.retrieveFileStream(fileName);
		}

		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception {
			if (!ftpClient.storeFile(targetFileName, inputStream)) {
				throw new Exception("Failed to upload file \"" + targetFileName + "\" to \"" + host + "\".");
			}
		}

		@Override
		public void close() {
			if (ftpClient != null) { try { ftpClient.disconnect(); } catch (Exception ex) { } }
		}

		@Override
		public JsonArray listObjects() throws Exception {
			ArrayList<Object> arr = new ArrayList<Object>();
			FTPFile[] fs = ftpClient.listFiles();
			for (FTPFile f : fs) {
				Item.Info info = new Item.Info(f.getName(), f.getSize(), f.isDirectory(), new Timestamp(f.getTimestamp().getTimeInMillis() + 7 * 3600 * 1000));
				arr.add(Item.Info.toDictionary(info));
			}
			return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
		}
	}
	
	// TODO: FTPS
	public static class ServerFTPS extends IFileServer.FileServer {
		
		private FTPSClient ftpsClient;

		public ServerFTPS(String _host, int _port, String _username, String _password, String _rootFolder) {
			super(_host, _port, _username, _password, _rootFolder);
		}

		@Override
		public void open() throws Exception {
			ftpsClient = new FTPSClient();
			System.out.println("calling ftpsClient.connect("+host+", "+port+") ...");
			ftpsClient.connect(host, port);
			ftpsClient.enterLocalPassiveMode();
			
			System.out.println("calling ftpsClient.login("+username+", "+password+") ...");
			if (ftpsClient.login(username, password)) {
				System.out.println("logged in");
				System.out.println("calling ftpsClient.cwd("+rootFolder+") ...");
				ftpsClient.cwd(rootFolder);
				System.out.println("called  ftpsClient.cwd("+rootFolder+") .");
			} else {
				throw new Exception("Failed to login FTPSClient, host[" + host + "], port[" + port + "], username[" + username + "], password[" + password + "].");
			}
		}

		@Override
		public InputStream getInputStream(String fileName) throws Exception {
			return ftpsClient.retrieveFileStream(fileName);
		}

		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception {
			if (!ftpsClient.storeFile(targetFileName, inputStream)) {
				throw new Exception("Failed to upload file \"" + targetFileName + "\" to \"" + host + "\".");
			}
		}

		@Override
		public void close() {
			if (ftpsClient != null) { try { ftpsClient.disconnect(); } catch (Exception ex) { } }
		}

		@Override
		public JsonArray listObjects() throws Exception {
			ArrayList<Object> arr = new ArrayList<Object>();
			System.out.println("calling ftpsClient.listFiles() ...");
			FTPFile[] fs = ftpsClient.listFiles();
			System.out.println("called  ftpsClient.listFiles() .");
			for (FTPFile f : fs) {
				Item.Info info = new Item.Info(f.getName(), f.getSize(), f.isDirectory(), new Timestamp(f.getTimestamp().getTimeInMillis() + 7 * 3600 * 1000));
				arr.add(Item.Info.toDictionary(info));
			}
			return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
		}
	}
}