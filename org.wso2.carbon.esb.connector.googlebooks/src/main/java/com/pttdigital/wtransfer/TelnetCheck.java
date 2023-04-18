package com.pttdigital.wtransfer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
public class TelnetCheck {
	public static boolean getTelnet(String ip, int port) {

		Socket socket = null;
		OutputStream os = null;
		InputStream is = null;
		boolean ok = false;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), 3000);
			os = socket.getOutputStream();
			is = socket.getInputStream();
			ok = true;
		} catch (Exception ex) { }
		
		try { if (os != null) { os.close(); } } catch (Exception ex) { }
		try { if (is != null) { is.close(); } } catch (Exception ex) { }
		try { if (socket != null) { socket.close(); } } catch (Exception ex) { }
		
		return ok;
	}
}