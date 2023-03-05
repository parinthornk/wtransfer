package org.wso2.carbon.esb.connector;

import java.util.ArrayList;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Queue {
	
	public static final String host = "localhost";
	public static final int port = 5672;
	
	public static class Publisher {
		
		private static ConnectionFactory factory;
		private static Connection connection;
		private static Channel channel;
		private static ArrayList<String> queuesName = new ArrayList<String>();
		
		public static void open() throws Exception {
			queuesName.clear();
			factory = new ConnectionFactory();
			factory.setHost(Queue.host);
			factory.setPort(Queue.port);
			connection = factory.newConnection();
			channel = connection.createChannel();
		}
		
		public static void close() {
			try { channel.close(); } catch (Exception e) { }
			try { connection.close(); } catch (Exception e) { }
			queuesName.clear();
		}
		
		public static void enqueue(String queueName, String text) throws Exception {
			
			// initiate new queue if not exists
			if (!queuesName.contains(queueName)) {
				channel.queueDeclare(queueName, true, false, false, null);
				queuesName.add(queueName);
			}
			
			// send to rabbit
			channel.basicPublish("", queueName, null, text.getBytes());
		}
	}
}