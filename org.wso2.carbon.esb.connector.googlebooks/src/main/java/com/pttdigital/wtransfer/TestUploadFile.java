package com.pttdigital.wtransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class TestUploadFile {
public static void main(String[] args) {
    	
        String server = "10.195.2.19";
        int port = 20;
        String username = "WSO2_Dev";
        String password = "initial0$";
        String remoteDirectory = "/DEV/H102/SAP_OUTBOUND_DEV";
        
        // "ISO-8859-1"
        //String charset = "UTF-8";

        
        // ชื่อภาษาไทย.txt
        String targetFile = "ชื่อภาษาไทย.txt";
        //String targetFile = "ชื่อภาษาไทย.txt";
        String localFilePath = "C:\\Users\\parin\\Documents\\filezilla\\template\\TEST\\" + targetFile;
        
        
        System.out.println("localFilePath: " + localFilePath);

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            //ftpClient.setAutodetectUTF8(true);
            

            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            ftpClient.setControlEncoding(StandardCharsets.UTF_8.name());

            File localFile = new File(localFilePath);
            //System.out.println(localFile);
            
            InputStream inputStream = new FileInputStream(localFile);
            //InputStream inputStream = new FileInputStream(localFile, StandardCharsets.UTF_8);
            
            System.out.println(remoteDirectory + "/" + localFile.getName());
            
            
            
            
            
            
            
            
            
            
            
            String remotePath = new String((remoteDirectory + "/" + targetFile).getBytes("ASCII"), "ASCII");
            System.out.println("remotePath: " + remotePath);
            
            boolean uploaded = ftpClient.storeFile(remotePath, inputStream);
            
            //boolean uploaded = ftpClient.storeFile("/DEV/H102/SAP_OUTBOUND_DEV/ทดสอบ.txt", inputStream);
            
            //boolean uploaded = ftpClient.storeFile(remoteDirectory + "/" + localFile.getName(), inputStream);
            
            
            
            
            
            //boolean uploaded = ftpClient.storeFile(remoteDirectory + "/" + targetFile, inputStream);
            inputStream.close();

            if (uploaded) {
                System.out.println("File uploaded successfully.");
            } else {
                System.out.println("File upload failed.");
            }
            
            System.out.println(ftpClient.getReplyString());

            ftpClient.logout();
            ftpClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
