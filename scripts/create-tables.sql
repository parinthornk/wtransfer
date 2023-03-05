create table workspace (
	name text primary key,
	description text,
	created timestamp,
	modified timestamp
);
create table site (
	workspace text references workspace (name),
	name text primary key,
	description text,
	created timestamp,
	modified timestamp,
	host text,
	port integer,
	protocol text,
	rootfolder text,
	username text,
	password text,
	keypath text
);
create table pgp (
	workspace text references workspace (name),
	name text primary key,
	description text,
	created timestamp,
	modified timestamp,
	direction text,
	keypath text,
	keypassword text,
	fnrenameto text
);
create table config (
	workspace text references workspace (name),
	name text primary key,
	description text,
	created timestamp,
	modified timestamp,
	replace integer,
	deleteaftersuccess integer,
	retrycount integer,
	retryintervalms integer,
	fnisfilenametomove text,
	fnrenameto text,
	archivefolder text,
	fnarcrenameto text
);
create table schedule (
	workspace text references workspace (name),
	name text primary key,
	description text,
	source text references site (name),
	target text references site (name),
	pgp text references pgp (name),
	config text references config (name),
	plan text,
	enabled integer,
	validfrom timestamp,
	validuntil timestamp,
	created timestamp,
	modified timestamp
);
create table task (
	workspace text references workspace (name),
	source text references site (name),
	target text references site (name),
	pgp text references pgp (name),
	config text references config (name),
	schedule text references schedule (name),
	id serial8 primary key,
	description text,
	created timestamp,
	modified timestamp,
	status text
);
create table item (
	workspace text references workspace (name),
	task integer references task (id),
	name text primary key,
	created timestamp,
	modified timestamp,
	retryQuota integer,
	retryRemaining integer,
	retryIntervalMs integer,
	timeNextRetry timestamp,
	timeLatestRetry timestamp,
	status text
);
create table log_schedule (
	workspace text references workspace (name),
	id serial8 primary key,
	schedule text references schedule (name),
	created timestamp,
	modified timestamp,
	logtype text,
	title text,
	body text
);
create table log_task (
	workspace text references workspace (name),
	id serial8 primary key,
	task integer references task (id),
	created timestamp,
	modified timestamp,
	logtype text,
	title text,
	body text
);
create table log_item (
	workspace text references workspace (name),
	id serial8 primary key,
	item text references item (name),
	created timestamp,
	modified timestamp,
	logtype text,
	title text,
	body text
);