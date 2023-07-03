package com.pttdigital.wtransfer;

import java.util.concurrent.Semaphore;

import com.pttdigital.wtransfer.ImportV2.OL;

public class TestExclusive {
	

	
	private static Semaphore semaphore = new Semaphore(1);

	private static void lock() {
        try {
            semaphore.acquire(); // Acquire the semaphore, blocking if it's already acquired by another thread
        } catch (InterruptedException e) {
            // Handle the interruption if required
            e.printStackTrace();
        }
    }

	private static void unlock() {
        semaphore.release(); // Release the semaphore
    }
	
    public static void main(String[] args) throws Exception {
    	for (;;) {
    		lock();
    		OL.sln(System.currentTimeMillis() + ", lock");
    		Thread.sleep(10000);
    		OL.sln(System.currentTimeMillis() + ", unlock");
    		Thread.sleep(10000);
    	}
	}
}