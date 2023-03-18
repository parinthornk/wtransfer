package org.wso2.carbon.esb.connector;

import java.io.StringWriter;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class FileNaming {

	private static String getJsPrint(String script) throws Exception {
		String ret = null;
		StringWriter writer = new StringWriter();
		try {
			ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("JavaScript");
			ScriptContext ctx = engine.getContext();
			ctx.setWriter(writer);
			engine.eval(script);
			String x = writer.toString();
			if (x.endsWith("\r\n")) {
				x = x.substring(0, x.length() - 2);
			} else if (x.endsWith("\n")) {
				x = x.substring(0, x.length() - 1);
			}
			ret = x;
			try { writer.close(); } catch (Exception e1) { }
		} catch (Exception ex) {
			ex.printStackTrace();
			try { writer.close(); } catch (Exception e1) { }
			throw ex;
		}
		return ret;
	}
	
	public static boolean isFileNameToMove(String fileName, String logic) throws Exception {
		String res = getJsPrint("var fn = " + logic + ";print(fn(\"" + fileName + "\"));");
		return Boolean.parseBoolean(res);
	}
	
	public static String getRenamed(String fileName, String logic) throws Exception {
		return getJsPrint("var fn = " + logic + ";print(fn(\"" + fileName + "\"));");
	}
}