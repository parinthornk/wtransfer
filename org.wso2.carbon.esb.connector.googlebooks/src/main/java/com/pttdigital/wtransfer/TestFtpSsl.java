package com.pttdigital.wtransfer;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.wso2.carbon.esb.connector.ZConnector;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pttdigital.wtransfer.ImportV2.OL;

public class TestFtpSsl {
	public static void main(String[] arg) {
		
		

        String server = "hq-h2o-s01.ptt.corp";
        int port = 21;
        String username = "Dispatcher";
        String password = "cD1sp@tcher";
        

        String targetFile = "file_example_XLSX_2MB-0000.xlsx";
        String localFilePath = "C:\\Users\\parin\\Documents\\filezilla\\template\\TEST\\" + targetFile;

        String remoteDirectory = "/Dev/usr/sap/interface/pttor/ebpp/outbound";

        FTPSClient ftpClient = new FTPSClient();
        //ftpClient.setEnabledProtocols("SSL");

        try {
            // Connect to the FTP server
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("FTP server refused connection.");
                return;
            }

            // Authenticate with the server
            OL.sln("ftpClient.login("+username+", "+password+")");
            if (!ftpClient.login(username, password)) {
                System.out.println("FTP login failed.");
                return;
            }

            // Set binary file transfer mode
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            ftpClient.enterLocalPassiveMode();
            

			/*ftpClient.cwd(remoteDirectory);
			ArrayList<Object> arr = new ArrayList<Object>();
			FTPFile[] fs = ftpClient.listFiles();
			for (FTPFile f : fs) {
				Item.Info info = new Item.Info(f.getName(), f.getSize(), f.isDirectory(), new Timestamp(f.getTimestamp().getTimeInMillis() + 0 * 3600 * 1000));
				arr.add(Item.Info.toDictionary(info));
				OL.sln(f.getName());
			}
			//return new Gson().fromJson(ZConnector.ConvertToJsonString(arr), JsonArray.class);*/
            
            
            // Upload the file
            File localFile = new File(localFilePath);
            try (FileInputStream fis = new FileInputStream(localFile)) {
            	
            	String remoteFilePath = remoteDirectory + "/" + targetFile;
            	
                boolean uploaded = ftpClient.storeFile(remoteFilePath, fis);
                if (uploaded) {
                    System.out.println("File uploaded successfully.");
                } else {
                    System.out.println("Failed to upload file.");
                    OL.sln(ftpClient.getReplyString());
                }
            }

            // Logout and disconnect
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	public static ZResult n0() {
		JsonObject o = new JsonObject();
		JsonArray arro = new JsonArray();
		
		ZResult ret = ZResult.ERROR_501(new Exception("not implemented"));
		


        String server = "hq-h2o-s01.ptt.corp";
        int port = 21;
        String username = "Dispatcher";
        String password = "cD1sp@tcher";
        

        String remoteDirectory = "/Dev/usr/sap/interface/pttor/ebpp/outbound";

        FTPSClient ftpClient = new FTPSClient();
        //ftpClient.setEnabledProtocols("SSL");

        try {
            // Connect to the FTP server
            ftpClient.connect(server, port);
            
            
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                throw new Exception("FTP server refused connection.");
            }

			o.addProperty("connected", true);

            // Authenticate with the server
            OL.sln("ftpClient.login("+username+", "+password+")");
            if (!ftpClient.login(username, password)) {
            	throw new Exception("FTP login failed.");
            }

			o.addProperty("login", true);

            // Set binary file transfer mode
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			o.addProperty("setFileType", true);
            
            ftpClient.enterLocalPassiveMode();

			o.addProperty("enterLocalPassiveMode", true);
            

			ftpClient.cwd(remoteDirectory);

			o.addProperty("remoteDirectory", remoteDirectory);
			ArrayList<Object> arr = new ArrayList<Object>();
			FTPFile[] fs = ftpClient.listFiles();
			for (FTPFile f : fs) {
				Item.Info info = new Item.Info(f.getName(), f.getSize(), f.isDirectory(), new Timestamp(f.getTimestamp().getTimeInMillis() + 0 * 3600 * 1000));
				arr.add(Item.Info.toDictionary(info));
				OL.sln(f.getName());
				
				arro.add(f.getName());
			}
			
			o.addProperty("total", arr.size());
			o.add("items", arro);

            // Logout and disconnect
            ftpClient.logout();
            ftpClient.disconnect();
            ret.statusCode = 200;
        } catch (Exception e) {
            e.printStackTrace();
            
            o.addProperty("error", "" + e);
            ret.statusCode = 500;
        }
		
        ret.content = o.toString();
		return ret;
	}
}