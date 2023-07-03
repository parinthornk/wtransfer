package com.pttdigital.wtransfer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
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
	
	//public OutputStreamWriter getOutputStreamWriter(InputStream inputStream, String filePath, String charset) throws Exception;

	public boolean fileExists(String file) throws Exception;
	public boolean directoryExists(String folder) throws Exception;
	public void createDirectory(String folder) throws Exception;
	public void deleteDirectory(String folder) throws Exception;
	public void deleteFile(String fileName) throws Exception;
	public void move_internal(String fileSource, String fileNameTarget) throws Exception;
	
	public void move_external(String localFilePath, String transferMode, String remoteIp, int remotePort, String remoteProtocol, String remoteUser, String remotePass, String remoteKeyPath, String remoteFileName) throws Exception;
	
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
		
		/*// custom site
		if (site.description.contains("896126e3-c6df-4e9f-bedc-03378f3e41fe")) {
			
		}*/
		
		if (site.protocol.equalsIgnoreCase("sftp")) {
			//OL.sln("type: ServerSFTP");
			return new IFileServer.ServerSFTP(site.host, site.port, site.username, site.password, site.keyPath);
		}
		if (site.protocol.equalsIgnoreCase("ftp")) {
			//OL.sln("type: ServerFTP");
			return new IFileServer.ServerFTP(site.host, site.port, site.username, site.password);
		}
		if (site.protocol.equalsIgnoreCase("ftps")) {
			//OL.sln("type: ServerFTPS");
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
			
			session.connect(25000);
			channel = session.openChannel("sftp");
            channel.connect(25000);
            sftpChannel = (ChannelSftp) channel;
            
            /*// TODO, set encoding
            sftpChannel.setFilenameEncoding("UTF-8");
            
            System.out.println("fisnished.");*/
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
		
		private static ArrayList<Long> ms = new ArrayList<Long>();
		public static void printms() {
			double sum = 0;
			for (long l : ms) {
				sum += l;
			}
			double avg = sum / ms.size();
			OL.sln("avg: " + avg);
		}

		@Override
		public JsonArray listObjects(String folder) throws Exception {
			long t0 = System.currentTimeMillis();
			Vector<?> vFiles = sftpChannel.ls(folder);
			ArrayList<Object> arr = new ArrayList<Object>();
			for (Object o : vFiles) {
				LsEntry entry = (LsEntry) o;
				SftpATTRS attrs = entry.getAttrs();
				Item.Info info = new Item.Info(entry.getFilename(), attrs.getSize(), attrs.isDir(), new Timestamp(Long.parseLong(attrs.getMTime() + "000")));
				arr.add(Item.Info.toDictionary(info));
			}
			JsonArray ret = new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);
			long t1 = System.currentTimeMillis();
			ms.add(t1 - t0);
			return ret;
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
			
			//System.out.println("Creating Directory...");
            String[] complPath = folder.split("/");
            sftpChannel.cd("/");
            for (String dir : complPath) {
                if (dir.length() > 0) {
                    try {
                        //System.out.println("Current Dir : " + sftpChannel.pwd());
                        sftpChannel.cd(dir);
                    } catch (SftpException e2) {
                    	sftpChannel.mkdir(dir);
                    	sftpChannel.cd(dir);
                    }
                }
            }
            sftpChannel.cd("/");
			

			/*String dir = folder;
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
				boolean b = directoryExists(full);
				OL.sln("b: " + b + ", " + full);
				if (!b) {
					sftpChannel.mkdir(full);
					OL.sln("sftpChannel.mkdir("+full+");");
				}
			}*/
			
		}

		@Override
		public void move_internal(String absFileNameSource, String absFileNameTarget) throws SftpException {
			OL.sln("sftpChannel.rename("+absFileNameSource+", "+absFileNameTarget+");");
			sftpChannel.rename(absFileNameSource, absFileNameTarget);
		}

		@Override
		public boolean fileExists(String absFileName) {
			try {
			    return !sftpChannel.stat(absFileName).isDir();
			} catch (Exception e) { }
			return false;
		}

		@Override
		public void deleteDirectory(String folder) throws Exception {
			// TODO Auto-generated method stub

		    // List source directory structure.
		    Collection<ChannelSftp.LsEntry> fileAndFolderList = sftpChannel.ls(folder);

		    // Iterate objects in the list to get file/folder names.
		    for (ChannelSftp.LsEntry item : fileAndFolderList) {
		        if (!item.getAttrs().isDir()) {
		        	sftpChannel.rm(folder + "/" + item.getFilename()); // Remove file.
		        } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) { // If it is a subdir.
		            try {
		                // removing sub directory.
		            	sftpChannel.rmdir(folder + "/" + item.getFilename());
		            } catch (Exception e) { // If subdir is not empty and error occurs.
		                // Do lsFolderRemove on this subdir to enter it and clear its contents.
		            	deleteDirectory(folder + "/" + item.getFilename());
		            }
		        }
		    }
		    sftpChannel.rmdir(folder); // delete the parent directory after empty
		}

		@Override
		public void deleteFile(String fileName) throws Exception {
			// TODO Auto-generated method stub
			sftpChannel.rm(fileName);
		}

		@Override
		public void move_external(String localFilePath, String transferMode, String remoteIp, int remotePort, String remoteProtocol, String remoteUser, String remotePass, String remoteKeyPath, String remoteFileName) throws Exception {
			// TODO Auto-generated method stub
			
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
			
			ftpClient.setDefaultTimeout(25000);
			ftpClient.setConnectTimeout(25000);
			ftpClient.connect(host, port);
			
			
			//ftpClient.setControlEncoding("UTF-8");
			
			//ftpClient.setAutodetectUTF8(true);
			
			
			

			if (ftpClient.login(username, password)) {

				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				
				ftpClient.enterLocalPassiveMode();
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
			
			goToParentDir();
			
			String p = targetFileName;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			
			for (int i=0;i<split.length - 1;i++) {

				ftpClient.changeWorkingDirectory(split[i]);
			}
			
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
			
			for (int i=0;i<10;i++) {
				ftpClient.changeToParentDirectory();
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
			
			String p = folder;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			for (int i=0;i<split.length;i++) {
				String dir = split[i];
				if (!ftpClient.changeWorkingDirectory(dir)) {
					int mkd = ftpClient.mkd(dir);
					OL.sln("\t" + "ftpClient.mkd(\""+dir+"\") ---> " + mkd);
					if (mkd > 499) {
						throw new Exception("Error creating directory \"" + dir + "\", " + ftpClient.getReplyString());
					}
					
					ftpClient.changeWorkingDirectory(dir);
				}
			}
			
			ftpClient.changeToParentDirectory();
		}

		@Override
		public void move_internal(String absFileNameSource, String absFileNameTarget) throws Exception {
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

		@Override
		public void deleteDirectory(String folder) throws Exception {
			ftpClient.dele(folder);
		}

		@Override
		public void deleteFile(String fileName) throws Exception {
			
			goToParentDir();
			
			boolean deleted = ftpClient.deleteFile(fileName);
			if (deleted) {
				String error = ftpClient.getReplyString();
				if (!error.contains("250")) {
					throw new Exception(error);
				}
			}
		}
		
		private void goToParentDir() throws IOException {
			for (int i = 0; i < 3; i++) {
				ftpClient.changeToParentDirectory();
			}
		}

		@Override
		public void move_external(String localFilePath, String transferMode, String remoteIp, int remotePort, String remoteProtocol, String remoteUser, String remotePass, String remoteKeyPath, String remoteFileName) throws Exception {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static class ServerFTPS extends IFileServer.FileServer {
		
		private FTPSClient ftpsClient;

		public ServerFTPS(String _host, int _port, String _username, String _password) {
			super(_host, _port, _username, _password);
		}
		
		@Override
		public void open() throws Exception {
			
			ftpsClient = new FTPSClient();
			
			ftpsClient.setDefaultTimeout(25000);
			ftpsClient.setConnectTimeout(25000);
			ftpsClient.connect(host, port);
			
			if (ftpsClient.login(username, password)) {

				ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
				
				ftpsClient.enterLocalPassiveMode();
			} else {
				
				throw new Exception("Failed to login FTPSClient, host[" + host + "], port[" + port + "], username[" + username + "], password[" + password + "].");
			}
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
			
			for (int i=0;i<10;i++) {
				ftpsClient.changeToParentDirectory();
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
			
			String p = folder;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			for (int i=0;i<split.length;i++) {
				String dir = split[i];
				if (!ftpsClient.changeWorkingDirectory(dir)) {
					int mkd = ftpsClient.mkd(dir);
					OL.sln("\t" + "ftpsClient.mkd(\""+dir+"\") ---> " + mkd);
					if (mkd > 499) {
						throw new Exception("Error creating directory \"" + dir + "\", " + ftpsClient.getReplyString());
					}
					
					ftpsClient.changeWorkingDirectory(dir);
				}
			}
			
			ftpsClient.changeToParentDirectory();
		}

		@Override
		public void move_internal(String absFileNameSource, String absFileNameTarget) throws Exception {
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

		@Override
		public void deleteDirectory(String folder) throws Exception {
			ftpsClient.dele(folder);
		}

		@Override
		public void deleteFile(String fileName) throws Exception {
			
			goToParentDir();
			
			boolean deleted = ftpsClient.deleteFile(fileName);
			if (deleted) {
				String error = ftpsClient.getReplyString();
				if (!error.contains("250")) {
					throw new Exception(error);
				}
			}
		}
		
		private void goToParentDir() throws IOException {
			for (int i = 0; i < 3; i++) {
				ftpsClient.changeToParentDirectory();
			}
		}

		@Override
		public void move_external(String localFilePath, String transferMode, String remoteIp, int remotePort, String remoteProtocol, String remoteUser, String remotePass, String remoteKeyPath, String remoteFileName) throws Exception {
			// TODO Auto-generated method stub
			
		}

		@Override
		public InputStream getInputStream(String absFilePath) throws Exception {
			// TODO Auto-generated method stub
			return ftpsClient.retrieveFileStream(absFilePath);
		}

		@Override
		public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception {

			goToParentDir();
			
			String p = targetFileName;
			while (p.startsWith("/")) { p = p.substring(1, p.length()); }
			while (p.endsWith("/")) { p = p.substring(0, p.length() - 1); }
			while (p.contains("//")) { p = p.replace("//", "/"); }
			String[] split = p.split("/");
			
			for (int i=0;i<split.length - 1;i++) {

				ftpsClient.changeWorkingDirectory(split[i]);
			}
			
			if (!ftpsClient.storeFile(split[split.length - 1], inputStream)) {
				throw new Exception("Failed uploading to \"" + split[split.length - 1] + "\" on \"" + host + "\", " + ftpsClient.getReplyString());
			}
			
			ftpsClient.changeToParentDirectory();
		}
	}
}