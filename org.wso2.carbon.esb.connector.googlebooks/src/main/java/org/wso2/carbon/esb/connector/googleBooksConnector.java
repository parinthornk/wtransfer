package org.wso2.carbon.esb.connector;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;

public class googleBooksConnector extends AbstractConnector {
	
	public static void main2(String[] args) {
		
		/*// open connection
		try {
			Queue.Publisher.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// send message to queue
		try {
			Queue.Publisher.enqueue("test-queue-xxx", "hello!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// cleanup
		Queue.Publisher.close();*/
		
		try {
			CAR02.car02doWork();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String Operation; public void setOperation(String op) { Operation = op; }

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
    	//System.out.println("googleBooksConnector, connect");
        Object templateParam = getParameter(messageContext, "generated_param");
        try {
            log.info("googleBooks sample connector received message :" + templateParam);
            /**Add your connector code here 
            **/
        } catch (Exception e) {
	    throw new ConnectException(e);	
        }
    }

    public static String decodeUrl(String url) {
    	try {
    	    return java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
    	} catch (Exception e) {
    	    return url;
    	}
    }
    
    @Override
	public boolean mediate(MessageContext context) {
    	//System.out.println("Arg[0] ---> 1234: " + Operation);
    	if (Operation.equalsIgnoreCase("controller")) {
        	ZConnector.ZResult result = new ZConnector.ZResult();
        	try {
    			org.apache.axis2.context.MessageContext axis2MessageContext = null;
    			axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
    			String bodyText = null;
    			byte[] bodyRaw = null;
    			try {
    				bodyText = JsonUtil.jsonPayloadToString(axis2MessageContext);
    				bodyRaw = bodyText.getBytes();
    			} catch (Exception ex) { }
    			String method = context.getProperty("REST_METHOD").toString().toLowerCase();
    			String path = context.getProperty("REST_SUB_REQUEST_PATH").toString();//.toLowerCase();
    			
    			
    			// query parameters
    			HashMap<String, String> query = new HashMap<String, String>();
    			try {
    				String[] sp1 = path.split("\\?");
    				if (sp1.length == 2) {
    					path = sp1[0];
    					String[] sp2 = sp1[1].split("&");
    					for (int n=0;n<sp2.length;n++) {
    						String[] sp3 = sp2[n].split("=");
    						try {
    							String key = sp3[0];
    							String value = sp3[1];
    							query.put(key, value);
    							//System.out.println("key["+key+"], value["+value+"]");
    						} catch (Exception ex) { }
    					}
    				}
    			} catch (Exception ex) {
    				//System.out.println("Error path.split: " + ex);
    			}
    			
    			
    			
    			
    			
    			
    			
    			
    			//System.out.println("================================================================================================================");
    			//System.out.println("======================================================== >>>");
    			//System.out.println("path    : " + path);
    			//System.out.println("method  : " + method);
    			//System.out.println("bodyText: " + bodyText);
    			//System.out.println("======================================================== >>>");
    			result = ZAPIV2.process(path, method, query, null, bodyRaw);
        	} catch (Exception ex) {
        		result = ZConnector.ZResult.ERROR_500(ex);
        	}
    		int statusCode = result.statusCode;
    		String content = result.content;
    		context.setProperty("ftpStatusCode", statusCode);
    		context.setProperty("wresponse", content);
    		//System.out.println("======================================================== <<<");
    		//System.out.println("code    : " + statusCode);
    		//System.out.println("content : " + content);
    		//System.out.println("======================================================== <<<");
    		//System.out.println("================================================================================================================");
    	} else if (Operation.equalsIgnoreCase("quarterback")) {
			//System.out.println("================================================================================================================ quarterback begin");
			//ZWorker.update();
			try {
				CAR02.car02doWork();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("================================================================================================================ quarterback end");
    	} else if (Operation.equalsIgnoreCase("car03")) {
        	ZConnector.ZResult result = new ZConnector.ZResult();
    		try {
    			org.apache.axis2.context.MessageContext axis2MessageContext = null;
    			axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
    			String bodyText = null;
    			byte[] bodyRaw = null;
    			try {
    				bodyText = JsonUtil.jsonPayloadToString(axis2MessageContext);
    				bodyRaw = bodyText.getBytes();
    			} catch (Exception ex) { }
    			String method = context.getProperty("REST_METHOD").toString().toLowerCase();
    			String path = context.getProperty("REST_SUB_REQUEST_PATH").toString();
    			
    			// query parameters
    			HashMap<String, String> query = new HashMap<String, String>();
    			try {
    				String[] sp1 = path.split("\\?");
    				if (sp1.length == 2) {
    					path = sp1[0];
    					String[] sp2 = sp1[1].split("&");
    					for (int n=0;n<sp2.length;n++) {
    						String[] sp3 = sp2[n].split("=");
    						try {
    							String key = sp3[0];
    							String value = sp3[1];
    							query.put(key, value);
    						} catch (Exception ex) { }
    					}
    				}
    			} catch (Exception ex) { }
    			result = CAR03.process(path, method, query, null, bodyRaw);
    		} catch (Exception ex) {
        		result = ZConnector.ZResult.ERROR_500(ex);
    		}
    		int statusCode = result.statusCode;
    		String content = result.content;
    		context.setProperty("ftpStatusCode", statusCode);
    		context.setProperty("wresponse", content);
    	} else {
    		Exception ex = new Exception("Unknown connector operation: " + Operation);
    		ex.printStackTrace();
    	}
		return true;
	}
}