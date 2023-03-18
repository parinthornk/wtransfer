package com.pttdigital.wtransfer;

import java.sql.Timestamp;

public class LogItem {
	public String workspace;
	public long id;
	public String item;
	public Timestamp created;
	public Timestamp modified;
	public String logType;
	public String title;
	public String body;
}