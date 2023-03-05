package com.alone.rabbit;

public class QueueExecution {
	
	public enum Mode {
		SINGLE_THREAD,
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
	
	private Mode mode;
	private Object locker;
	private QueueListener.Execution execution;
	private String queueName;
	
	public QueueExecution(Mode _mode, QueueListener.Execution _execution, String _queueName) {
		
		mode = _mode;
		execution = _execution;
		queueName = _queueName;
		
		if (mode == Mode.SINGLE_THREAD) {
			
		} else {
			locker = new Object();
			availables = new boolean[Integer.parseInt(mode.toString().split("_")[2])];
			for (int i = 0; i < availables.length; i++) {
				availables[i] = true;
			}
		}
	}
	
	private boolean[] availables;
	
	private int getAvailableIndex() {
		for (;;  ) {
			
			try { Thread.sleep(100); } catch (Exception ex) { }
			int ret = -1;
			synchronized (locker) {
				for (int i = 0; i < availables.length; i++) {
					if (availables[i]) {
						ret = i;
						break;
					}
				}
			}
			
			if (ret > -1) {
				return ret;
			}
		}
	}
	
	private void setAvailable(int index, boolean value) {
		synchronized (locker) {
			availables[index] = value;
		}
	}
	
	public void receive(String text) {
		if (mode == Mode.SINGLE_THREAD) {
			execute(queueName, text);
		} else {
			int index = getAvailableIndex();
			setAvailable(index, false);
			String t = text;
			new Thread() {
				@Override
				public void run() {
					execute(queueName, t);
					setAvailable(index, true);
				}
			}.start();
		}
	}
	
	private void execute(String queueName, String text) {
		try {
			execution.execute(queueName, text);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}