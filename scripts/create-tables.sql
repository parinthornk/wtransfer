create table "Workspace" (
	"name" text primary key,
	"description" text,
	"created" timestamp,
	"modified" timestamp
);

create table "Site" (
	"workspace" text references "Workspace" (name) not null,
	"name" text primary key,
	"description" text,
	"created" timestamp,
	"modified" timestamp,
	"host" text,
	"port" integer,
	"protocol" text,
	"username" text,
	"password" text,
	"keyPath" text
);

create table "Schedule" (
	"siteSource" text references "Site" (name) not null,
	"useDynamicDirSource" boolean not null,
	"fnDynamicDirSource" text,
	"staticDirSource" text,
	"siteTarget" text references "Site" (name) not null,
	"useDynamicDirTarget" boolean not null,
	"fnDynamicDirTarget" text,
	"staticDirTarget" text,
	"retryCount" integer,
	"retryIntervalMs" integer,
	"fnIsFileToMove" text,
	"fnRenameTo" text,
	"archiveFolder" text,
	"fnArchiveRenameTo" text,
	"workerThreads" integer,
	"workspace" text references "Workspace" (name) not null,
	"name" text primary key,
	"description" text,
	"pgpDirection" text,
	"pgpKeyPath" text,
	"pgpKeyPassword" text,
	"fnPgpRenameTo" text,
	"plan" text,
	"enabled" boolean,
	"previousCheckpoint" timestamp,
	"validFrom" timestamp,
	"validUntil" timestamp,
	"created" timestamp,
	"modified" timestamp
);

create table "Session" (
	"workspace" text references "Workspace" (name) not null,
	"schedule" text references "Schedule" (name) not null,
	"id" serial8 primary key,
	"description" text,
	"created" timestamp,
	"modified" timestamp,
	"status" text
);

create table "Item" (
	"workspace" text references "Workspace" (name),
	"session" bigint references "Session" (id),
	"name" text primary key,
	"description" text,
	"folder" text,
	"folderArchive" text,
	"fileName" text,
	"fileNameArchive" text,
	"created" timestamp,
	"modified" timestamp,
	"retryQuota" integer,
	"retryRemaining" integer,
	"retryIntervalMs" integer,
	"timeNextRetry" timestamp,
	"timeLatestRetry" timestamp,
	"status" text,
	"fnCallback" text
);

create table "LogSchedule" (
	"workspace" text references "Workspace" (name),
	"id" serial8 primary key,
	"schedule" text references "Schedule" (name),
	"created" timestamp,
	"modified" timestamp,
	"logType" text,
	"title" text,
	"body" text
);

create table "LogSession" (
	"workspace" text references "Workspace" (name),
	"id" serial8 primary key,
	"session" integer references "Session" (id),
	"created" timestamp,
	"modified" timestamp,
	"logType" text,
	"title" text,
	"body" text
);

create table "LogItem" (
	"workspace" text references "Workspace" (name),
	"id" serial8 primary key,
	"item" text references "Item" (name),
	"created" timestamp,
	"modified" timestamp,
	"logType" text,
	"title" text,
	"body" text
);