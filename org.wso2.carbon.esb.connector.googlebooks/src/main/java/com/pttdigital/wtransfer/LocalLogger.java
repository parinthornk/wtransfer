package com.pttdigital.wtransfer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalLogger {
	
	private static Object _locker = new Object();
	
	private static String _log_folder = "FTP-log";
	
	public static void write(String level, String text) {
		synchronized(_locker) {
			try {
				
				// create log folder if not exist
				if (!new File(_log_folder).exists()) {
					new File(_log_folder).mkdir();
				}
				
				// time and fileName
				LocalDateTime now = LocalDateTime.now();
				String timeFormatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
				String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				String fileName = _log_folder + "//" + timeFormatted + "000000.txt";
				
				// append
		        FileWriter fileWriter = null;
		        BufferedWriter writer = null;
		        try {
		        	
		        	fileWriter = new FileWriter(fileName, true);
		        	writer = new BufferedWriter(fileWriter);
		            writer.write(timestamp + "\t" + level.toUpperCase() + "\t" + text);
		            writer.newLine();
		        } catch (Exception e) { e.printStackTrace(); }
		        
		        // cleanup
		        if (writer != null) { try { writer.close(); } catch(Exception ex) { } }
		        if (fileWriter != null) { try { fileWriter.close(); } catch(Exception ex) { } }
				
			} catch (Exception ex) { ex.printStackTrace(); }
		}
	}
}