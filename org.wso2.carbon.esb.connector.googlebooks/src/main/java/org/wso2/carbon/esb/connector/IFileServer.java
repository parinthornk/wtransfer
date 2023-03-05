package org.wso2.carbon.esb.connector;

import java.io.InputStream;
import java.util.Set;

import org.apache.commons.net.ftp.FTPClient;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public interface IFileServer {

	public void open() throws Exception;
	public InputStream getInputStream(String fileName) throws Exception;
	public void receiveFileFromInputStream(InputStream inputStream, String targetFileName, boolean replace) throws Exception;
	public void close();
	
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
	}
}