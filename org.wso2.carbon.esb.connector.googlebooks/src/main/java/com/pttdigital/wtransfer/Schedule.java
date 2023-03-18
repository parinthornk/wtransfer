package com.pttdigital.wtransfer;

import java.sql.Timestamp;

public class Schedule {
	public String siteSource;
	public boolean useDynamicDirSource;
	public String fnDynamicDirSource;
	public String staticDirSource;
	public String siteTarget;
	public boolean useDynamicDirTarget;
	public String fnDynamicDirTarget;
	public String staticDirTarget;
	public int retryCount;
	public int retryIntervalMs;
	public String fnIsFileToMove;
	public String fnRenameTo;
	public String archiveFolder;
	public String fnArchiveRenameTo;
	public int workerThreads;
	public String workspace;
	public String name;
	public String description;
	public String pgpDirection;
	public String pgpKeyPath;
	public String pgpKeyPassword;
	public String fnPgpRenameTo;
	public String plan;
	public boolean enabled;
	public Timestamp previousCheckpoint;
	public Timestamp validFrom;
	public Timestamp validUntil;
	public Timestamp created;
	public Timestamp modified;
}