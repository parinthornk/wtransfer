create table workspace (
	name text primary key,
	description text,
	created timestamp,
	modified timestamp
);
create table site (
	workspace text references workspace (name) not null,
	name text primary key,
	description text,
	created timestamp,
	modified timestamp,
	host text,
	port integer,
	protocol text,
	username text,
	password text,
	keypath text
);


create table schedule (
	
	sitesource text references site (name) not null,
	usedynamicdirsource boolean not null,
	fn_dynamicdirsource text,
	staticdirsource text references folder (name) not null,
	
	sitetarget text references site (name) not null,
	usedynamicdirtarget boolean not null,
	fn_dynamicdirtarget text,
	staticdirtarget text references folder (name) not null,
	
	
	
	
	
	replacefile boolean,
	retrycount integer,
	retryintervalms integer,
	fn_iffiletomove text,
	fn_renameto text,
	archivefolder text,
	fn_archiverenameto text,
	
	workspace text references workspace (name) not null,
	name text primary key,
	description text,
	
	pgpdirection text,
	pgpkeypath text,
	pgpkeypassword text,
	pgp_fn_renameto text,
	
	plan text,
	enabled integer,
	
	previouscheckpoint timestamp,
	validfrom timestamp,
	validuntil timestamp,
	created timestamp,
	modified timestamp
);
create table session (
	workspace text references workspace (name) not null,
	schedule text references schedule (name) not null,
	id serial8 primary key,
	description text,
	created timestamp,
	modified timestamp,
	status text
);
create table item (
	workspace text references workspace (name),
	session integer references session (id),
	name text primary key,
	created timestamp,
	modified timestamp,
	retryquota integer,
	retryremaining integer,
	retryintervalms integer,
	timenextretry timestamp,
	timelatestretry timestamp,
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
	session integer references session (id),
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