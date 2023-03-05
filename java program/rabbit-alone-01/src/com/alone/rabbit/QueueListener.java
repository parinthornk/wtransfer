package com.alone.rabbit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.PriorityQueue;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class QueueListener {
	
	public static class RowsManagement {
		
		public enum Mode {
			SINGLE_THREAD,
			UNLIMIT_THREADS,
			MULTIPLE_THREADS_2,
			MULTIPLE_THREADS_3,
			MULTIPLE_THREADS_4,
			MULTIPLE_THREADS_5,
			MULTIPLE_THREADS_6,
			MULTIPLE_THREADS_8,
			MULTIPLE_THREADS_10,
			MULTIPLE_THREADS_15,
			MULTIPLE_THREADS_20,
			MULTIPLE_THREADS_30,
		}

		private int nThreads = 1;
		
		private String queueName;
		
		public int getThreads() {
			return nThreads;
		}
		
		private RowsManagement[] bs;
		
		private RowsManagement(int threads, String queueName) {
			nThreads = threads;
			
			if (threads > 1) {
				bs = new RowsManagement[nThreads];
				for (int i = 0; i < bs.length; i++) {
					bs[i] = RowsManagement.singleThread(queueName);
				}
			}
		}
		
		public static RowsManagement singleThread(String queueName) {
			return new RowsManagement(+1, queueName);
		}
		
		public static RowsManagement unlimitThreads(String queueName) {
			return new RowsManagement(-1, queueName);
		}
		
		public static RowsManagement multipleThreads(int threads, String queueName) throws Exception {
			if (threads < 2) {
				throw new Exception("Number of threads must be greater than 2.");
			}
			return new RowsManagement(threads, queueName);
		}
		
		public void start() {
			if (nThreads == +1) {
				startQueueMonitor();
			} else if (nThreads == -1) {
				
			} else if (nThreads > 1) {
				for (int i = 0; i < bs.length; i++) {
					bs[i].startQueueMonitor();
				}
			}
		}
		
		public void consume(String text) {
			if (nThreads == +1) {
				
				receive(text);
				
			} else if (nThreads == -1) {
				
				new Thread() {
					@Override
					public void run() {
						executeMessage(queueName, text);
					}
				}.start();
				
			} else if (nThreads > 1) {
				
				int qSizeMin = 999999;
				int mostAvailableIndex = 0;
				for (int i = 0; i < bs.length; i++) {
					int queueSize = bs[i].getSingleQueueSize();
					if (queueSize < qSizeMin) {
						qSizeMin = queueSize;
						mostAvailableIndex = i;
					}
				}
				
				bs[mostAvailableIndex].receive(text);
			}
		}
		
		public static class Queue {
			private Object locker = new Object();
			private PriorityQueue<String> queue = new PriorityQueue<String>();
			public void enqueue(String text) {
				synchronized (locker) {
					try {
						queue.add(text);
					} catch (Exception ex) { }
				}
			}
			public String dequeue() {
				String ret = null;
				synchronized (locker) {
					try {
						if (!queue.isEmpty()) {
							ret = queue.remove();
						}
					} catch (Exception ex) { }
				}
				return ret;
			}
			public int size() {
				int ret = -1;
				synchronized (locker) {
					try {
						ret = queue.size();
					} catch (Exception ex) { }
				}
				return ret;
			}
		}

		public int getSingleQueueSize() {
			return mainQueue.size();
		}
		private Thread mainThread;
		private AutoResetEvent mainWaiter;
		private Queue mainQueue;
		private void receive(String text) {
			mainQueue.enqueue(text);
			mainWaiter.set();
		}
		
		private void startQueueMonitor() {
			mainWaiter = new AutoResetEvent(false);
			mainQueue = new Queue();
			mainThread = new Thread() {
				@Override
				public void run() {
					for (;;) {
						try {
							mainWaiter.waitOne();
						} catch (Exception e) {
							e.printStackTrace();
						}
						for (;;) {
							String text = mainQueue.dequeue();
							if (text == null) {
								break;
							} else {
								executeMessage(queueName, text);
							}
						}
					}
				}
			};
			mainThread.start();
		}
		
		public static class AutoResetEvent
		{
			private final Object _monitor = new Object();
			private volatile boolean _isOpen = false;

			public AutoResetEvent(boolean open)
			{
				_isOpen = open;
			}

			public void waitOne() throws InterruptedException
			{
				synchronized (_monitor) {
					while (!_isOpen) {
						_monitor.wait();
					}
					_isOpen = false;
				}
			}

			public void waitOne(long timeout) throws InterruptedException
			{
				synchronized (_monitor) {
					long t = System.currentTimeMillis();
					while (!_isOpen) {
						_monitor.wait(timeout);
						if (System.currentTimeMillis() - t >= timeout) {
							break;
						}
					}
					_isOpen = false;
				}
			}

			public void set()
			{
				synchronized (_monitor) {
					_isOpen = true;
					_monitor.notify();
				}
			}

			public void reset()
			{
				_isOpen = false;
			}
		}
		
		private void executeMessage(String queueName, String text) {
			execution.execute(queueName, text);
		}

		private Execution execution;
		public void setExecution(Execution _execution) {
			execution = _execution;
		}
	}
	
	public static class Execution {
		public void execute(String queueName, String text) { }
	}
	
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;

	private String host;
	private int port;
	private String username;
	private String password;
	private String queueName;
	private QueueExecution tq;
	
	public QueueListener(String _host, int _port, String _queueName, String _username, String _password, Execution _execution, QueueExecution.Mode _mode) throws NumberFormatException, Exception {
		host = _host;
		port = _port;
		queueName = _queueName;
		username = _username;
		password = _password;
		tq = new QueueExecution(_mode, _execution, _queueName);
	}
	
	public void listenAsync() {
		new Thread() {
			@Override
			public void run() {
				try {
					factory = new ConnectionFactory();
					factory.setHost(host);
					factory.setPort(port);
					factory.setUsername(username);
					factory.setPassword(password);
					connection = factory.newConnection();
					channel = connection.createChannel();
					channel.basicQos(1);
					channel.queueDeclare(queueName, true, false, false, null);
					boolean autoAck = false;
					channel.basicConsume(queueName, autoAck, "a-consumer-tag", new DefaultConsumer(channel) {
						@Override
						public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
							tq.receive(new String(body, StandardCharsets.UTF_8));
							channel.basicAck(envelope.getDeliveryTag(), false);
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}.start();
	}
	
	public void close() {
		try { channel.close(); } catch (Exception ex) { }
		try { connection.close(); } catch (Exception ex) { }
	}
}