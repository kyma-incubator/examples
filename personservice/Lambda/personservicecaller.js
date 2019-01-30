require('dotenv').config();
const personservice = require('./personservicemodule');
var express = require('express');
var app = express();

console.log(`GATEWAY_URL = ${process.env.GATEWAY_URL}`);

app.get("/", async function (req, res) {
    var event = {
        "data": {
            "personid":req.query.personid
        },
        "extensions": {
            "request": req,            
            "response":res        
        }
    };
    await personservice.main(event,{});    
});

app.listen(3000, function () {
    console.log('Example app listening on port 3000!');
  });