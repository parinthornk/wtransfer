package org.wso2.carbon.esb.connector;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

public class Quarterback extends AbstractConnector {

	@Override
	public void connect(MessageContext messageContext) throws ConnectException {
    	System.out.println("Quarterback, connect");
	}

    @Override
	public boolean mediate(MessageContext context) {
    	System.out.println("Quarterback, mediate");
    	ZWorker.update();
    	return true;
    }
}