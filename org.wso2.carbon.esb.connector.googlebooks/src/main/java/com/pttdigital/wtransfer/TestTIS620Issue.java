package com.pttdigital.wtransfer;

import org.apache.commons.net.ftp.FTPSClient;

public class TestTIS620Issue {
	
	public static void start() {
		// TODO Auto-generated method stub
		FTPSClient ftps;
		
		ftps = new FTPSClient("SSL");
		ftps.setConnectTimeout(1000);
		ftps.setAutodetectUTF8(true);
		
		
		
	}
}