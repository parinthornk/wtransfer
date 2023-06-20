
const express = require("express");
const app = express();
const port = 3000;

const https = require('http');

app.all('/*', (req, response) => {
	response.writeHead(200, {
		'Content-Type': 'application/json', 
		'custom-header-1': 'v-1',
		'custom-header-2': 'v-2',
		'custom-header-3': 'v-3',
		'custom-header-4': 'v-4'
	});
	response.write(JSON.stringify({
		"message": "hello"
	}));
	response.end();
});

app.listen(port, () => {
	console.log("Example app listening on port: " + port);
});