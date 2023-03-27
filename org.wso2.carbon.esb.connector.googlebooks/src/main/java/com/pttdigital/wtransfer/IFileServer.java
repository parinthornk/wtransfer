package com.pttdigital.wtransfer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.wso2.carbon.esb.connector.ZConnector;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.pttdigital.wtransfer.ImportV2.OL;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public interface IFileServer {

	public void open() throws Exception;
	public InputStream getInputStream(String absFilePath) throws Exception;
	public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception;
	public void close();
	public JsonArray listObjects(String folder) throws Exception;

	public boolean fileExists(String file) throws Exception;
	public boolean directoryExists(String folder) throws Exception;
	public void createDirectory(String folder) throws Exception;
	public void move(String fileSource, String fileNameTarget) throws Exception;
	
	public static abstract class FileServer implements IFileServer {
		public String host;
		public int port;
		public String username;
		public String password;
		
		public FileServer(String _host, int _port, String _username, String _password) {
			host = _host;
			port = _port;
			username = _username;
			password = _password;
		}
	}
	
	public static IFileServer.FileServer createServer(Site site) throws Exception {
		if (site.protocol.equalsIgnoreCase("sftp")) {
			return new IFileServer.ServerSFTP(site.host, site.port, site.username, site.password, site.keyPath);
		}
		if (site.protocol.equalsIgnoreCase("ftp")) {
			return new IFileServer.ServerFTP(site.host, site.port, site.username, site.password);
		}
		if (site.protocol.equalsIgnoreCase("ftps")) {
			return new IFileServer.ServerFTPS(site.host, site.port, site.username, site.password);
		}
		throw new Exception("Failed to create FileServer: unsupported protocol \"" + site.protocol + "\".");
	}
	
	// TODO: SFTP
	public static class ServerSFTP extends IFileServer.FileServer {
		
		private ChannelSftp sftpChannel;
		private Channel channel;
		private Session session;
		private JSch jsch;
		private String authenticationKey;
		
		public ServerSFTP(String _host, int _port, String _username, String _password, String _authenticationKey) {
			super(_host, _port, _username, _password);
			authenticationKey = _authenticationKey;
		}
		
		@Override
		public void open() throws Exception {
			
			boolean useAuthKey = false;
			if (authenticationKey != null) {
				useAuthKey = authenticationKey.length() > 0;
			}
			
			jsch = new JSch();
			if (useAuthKey) {
				jsch.addIdentity("sftpIdentityKey", Files.readAllBytes(Paths.get(authenticationKey)), (byte[]) null, (byte[]) null);
			}
			
			session = jsch.getSession(username, host, port);
			session.setConfig("StrictHostKeyChecking", "no");
			
			if (!useAuthKey) {
				session.setPassword(password);
			}
			
			session.connect(3000);
			channel = session.openChannel("sftp");
            channel.connect(3000);
            sftpChannel = (ChannelSftp) channel;
		}
		
		@Override
		public InputStream getInputStream(String absFilePath) throws Exception {
			return sftpChannel.get(absFilePath);
		}
		
		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String absTargetFilePath, boolean replace) throws Exception {
			sftpChannel.put(inputStream, absTargetFilePath);
		}
		
		@Override
		public void close() {
			if (sftpChannel != null) { try { sftpChannel.disconnect(); } catch (Exception ex) { } }
			if (channel != null) { try { channel.disconnect(); } catch (Exception ex) { } }
			if (session != null) { try { session.disconnect(); } catch (Exception ex) { } }
		}

		@Override
		public JsonArray listObjects(String folder) throws Exception {
			OL.sln("doing  listObjects("+folder+")");
			Vector<?> vFiles = sftpChannel.ls(folder);
			OL.sln("vFiles: " + vFiles.size());
			ArrayList<Object> arr = new ArrayList<Object>();
			for (Object o : vFiles) {
				LsEntry entry = (LsEntry) o;
				SftpATTRS attrs = entry.getAttrs();
				Item.Info info = new Item.Info(entry.getFilename(), attrs.getSize(), attrs.isDir(), new Timestamp(Long.parseLong(attrs.getMTime() + "000")));
				arr.add(Item.Info.toDictionary(info));
			}
			return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
		}

		@Override
		public boolean directoryExists(String folder) {
			try {
			    return sftpChannel.stat(folder).isDir();
			} catch (Exception e) { }
			return false;
		}

		@Override
		public void createDirectory(String folder) throws SftpException {
			
			

			String dir = folder;
			while (dir.startsWith("/")) { dir = dir.substring(1, dir.length()); }
			while (dir.endsWith("/")) { dir = dir.substring(0, dir.length() - 1); }
			while (dir.contains("//")) { dir = dir.replace("//", "/"); }
			String[] split = dir.split("/");
			for (int i=0;i<split.length;i++) {
				String full = "";
				for (int j=0;j<=i;j++) {
					full += "/" + split[j];
				}
				while (full.startsWith("/")) { full = full.substring(1, full.length()); }
				OL.sln(full);
				
				if (!directoryExists(full)) {
					sftpChannel.mkdir(full);
				}
			}
			
			/*for (int i=0;i<20;i++) {
				sftpChannel.cd("..");
			}*/
			
		}

		@Override
		public void move(String absFileNameSource, String absFileNameTarget) throws SftpException {
			sftpChannel.rename(absFileNameSource, absFileNameTarget);
		}

		@Override
		public boolean fileExists(String absFileName) {
			try {
			    return !sftpChannel.stat(absFileName).isDir();
			} catch (Exception e) { }
			return false;
		}
	}
	
	// TODO: FTP
	public static class ServerFTP extends IFileServer.FileServer {
		
		private FTPClient ftpClient;

		public ServerFTP(String _host, int _port, String _username, String _password) {
			super(_host, _port, _username, _password);
		}
		
		@Override
		public void open() throws Exception {
			
			// zero bytes // TODO
			
			// .lock
			
			//timeout();// TODO
			
			// maximum file size
			
			// enqueue items order by ???
			
			// 5 threads configurable -> config in schedule
			
			ftpClient = new FTPClient();
			
			ftpClient.setDefaultTimeout(3000);
			ftpClient.setConnectTimeout(3000);
			ftpClient.connect(host, port);
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();
			if (ftpClient.login(username, password)) {
				
			} else {
				throw new Exception("Failed to login FTPClient, host[" + host + "], port[" + port + "], username[" + username + "], password[" + password + "].");
			}
		}

		@Override
		public InputStream getInputStream(String absFilePath) throws Exception {
			return ftpClient.retrieveFileStream(absFilePath);
		}

		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception {
			
			ftpClient.changeToParentDirectory();
			ftpClient.changeToParentDirectory();
			ftpClient.changeToParentDirectory();

			String p = targetFileName;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			OL.sln("split: " + split.length);
			for (int i=0;i<split.length - 1;i++) {

				ftpClient.changeWorkingDirectory(split[i]);
				OL.sln("ftpClient.changeWorkingDirectory("+split[i]+")");
				
				OL.sln(ftpClient.getReplyString());
				
				OL.sln("ftpClient.printWorkingDirectory(): " + ftpClient.printWorkingDirectory());
			}

			OL.sln("ftpClient.printWorkingDirectory(): " + ftpClient.printWorkingDirectory());
			if (!ftpClient.storeFile(split[split.length - 1], inputStream)) {
				throw new Exception("Failed uploading to \"" + split[split.length - 1] + "\" on \"" + host + "\", " + ftpClient.getReplyString());
			}
			
			ftpClient.changeToParentDirectory();
		}

		@Override
		public void close() {
			if (ftpClient != null) { try { ftpClient.disconnect(); } catch (Exception ex) { } }
		}

		@Override
		public JsonArray listObjects(String folder) throws Exception {
			ftpClient.cwd(folder);
			ArrayList<Object> arr = new ArrayList<Object>();
			FTPFile[] fs = ftpClient.listFiles();
			for (FTPFile f : fs) {
				Item.Info info = new Item.Info(f.getName(), f.getSize(), f.isDirectory(), new Timestamp(f.getTimestamp().getTimeInMillis() + 0 * 3600 * 1000));
				arr.add(Item.Info.toDictionary(info));
			}
			return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
		}

		@Override
		public boolean directoryExists(String folder) throws Exception {
			FTPFile[] fs = ftpClient.listFiles();
			for (FTPFile f : fs) {
				if (f.getName().equals(folder)) {
					return f.isDirectory();
				}
			}
			return false;
		}

		@Override
		public void createDirectory(String folder) throws Exception {

			//ftpClient.changeToParentDirectory();
			//OL.sln(ftpClient.getReplyString());
			
			String p = folder;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			for (int i=0;i<split.length;i++) {
				String dir = split[i];
				if (!ftpClient.changeWorkingDirectory(dir)) {
					OL.sln(ftpClient.getReplyString());
					int mkd = ftpClient.mkd(dir);
					
					if (mkd > 499) {
						throw new Exception("Error creating directory \"" + dir + "\", " + ftpClient.getReplyString());
					}
					
					ftpClient.changeWorkingDirectory(dir);
					OL.sln(ftpClient.getReplyString());
				}
			}
			
			ftpClient.changeToParentDirectory();
			OL.sln(ftpClient.getReplyString());
		}

		@Override
		public void move(String absFileNameSource, String absFileNameTarget) throws Exception {
			ftpClient.rename(absFileNameSource, absFileNameTarget);
		}

		@Override
		public boolean fileExists(String file) throws Exception {
			FTPFile[] fs = ftpClient.listFiles();
			for (FTPFile f : fs) {
				if (f.getName().equals(file)) {
					return f.isFile();
				}
			}
			return false;
		}
	}
	
	// TODO: FTPS
	public static class ServerFTPS extends IFileServer.FileServer {
		
		private FTPSClient ftpsClient;

		public ServerFTPS(String _host, int _port, String _username, String _password) {
			super(_host, _port, _username, _password);
		}
		
		@Override
		public void open() throws Exception {
			
			// zero bytes // TODO
			
			// .lock
			
			//timeout();// TODO
			
			// maximum file size
			
			// enqueue items order by ???
			
			// 5 threads configurable -> config in schedule
			
			ftpsClient = new FTPSClient(true);
			//ftpsClient.setAuthValue("TLS");
			ftpsClient.setDefaultTimeout(3000);
			ftpsClient.setConnectTimeout(3000);
			ftpsClient.connect(host, port);
			ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpsClient.enterLocalPassiveMode();
			if (ftpsClient.login(username, password)) {
				
			} else {
				throw new Exception("Failed to login ftpsClient, host[" + host + "], port[" + port + "], username[" + username + "], password[" + password + "].");
			}
		}

		@Override
		public InputStream getInputStream(String absFilePath) throws Exception {
			return ftpsClient.retrieveFileStream(absFilePath);
		}

		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception {
			
			ftpsClient.changeToParentDirectory();
			ftpsClient.changeToParentDirectory();
			ftpsClient.changeToParentDirectory();

			String p = targetFileName;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			OL.sln("split: " + split.length);
			for (int i=0;i<split.length - 1;i++) {

				ftpsClient.changeWorkingDirectory(split[i]);
				OL.sln("ftpsClient.changeWorkingDirectory("+split[i]+")");
				
				OL.sln(ftpsClient.getReplyString());
				
				OL.sln("ftpsClient.printWorkingDirectory(): " + ftpsClient.printWorkingDirectory());
			}

			OL.sln("ftpsClient.printWorkingDirectory(): " + ftpsClient.printWorkingDirectory());
			if (!ftpsClient.storeFile(split[split.length - 1], inputStream)) {
				throw new Exception("Failed uploading to \"" + split[split.length - 1] + "\" on \"" + host + "\", " + ftpsClient.getReplyString());
			}
			
			ftpsClient.changeToParentDirectory();
		}

		@Override
		public void close() {
			if (ftpsClient != null) { try { ftpsClient.disconnect(); } catch (Exception ex) { } }
		}

		@Override
		public JsonArray listObjects(String folder) throws Exception {
			ftpsClient.cwd(folder);
			ArrayList<Object> arr = new ArrayList<Object>();
			FTPFile[] fs = ftpsClient.listFiles();
			for (FTPFile f : fs) {
				Item.Info info = new Item.Info(f.getName(), f.getSize(), f.isDirectory(), new Timestamp(f.getTimestamp().getTimeInMillis() + 0 * 3600 * 1000));
				arr.add(Item.Info.toDictionary(info));
			}
			return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
		}

		@Override
		public boolean directoryExists(String folder) throws Exception {
			FTPFile[] fs = ftpsClient.listFiles();
			for (FTPFile f : fs) {
				if (f.getName().equals(folder)) {
					return f.isDirectory();
				}
			}
			return false;
		}

		@Override
		public void createDirectory(String folder) throws Exception {

			//ftpsClient.changeToParentDirectory();
			//OL.sln(ftpsClient.getReplyString());
			
			String p = folder;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			for (int i=0;i<split.length;i++) {
				String dir = split[i];
				if (!ftpsClient.changeWorkingDirectory(dir)) {
					OL.sln(ftpsClient.getReplyString());
					int mkd = ftpsClient.mkd(dir);
					
					if (mkd > 499) {
						throw new Exception("Error creating directory \"" + dir + "\", " + ftpsClient.getReplyString());
					}
					
					ftpsClient.changeWorkingDirectory(dir);
					OL.sln(ftpsClient.getReplyString());
				}
			}
			
			ftpsClient.changeToParentDirectory();
			OL.sln(ftpsClient.getReplyString());
		}

		@Override
		public void move(String absFileNameSource, String absFileNameTarget) throws Exception {
			ftpsClient.rename(absFileNameSource, absFileNameTarget);
		}

		@Override
		public boolean fileExists(String file) throws Exception {
			FTPFile[] fs = ftpsClient.listFiles();
			for (FTPFile f : fs) {
				if (f.getName().equals(file)) {
					return f.isFile();
				}
			}
			return false;
		}
	}
}