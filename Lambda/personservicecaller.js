require('dotenv').config();
const personservice = require('./personservicemodule');

console.log(`GATEWAY_URL = ${process.env.GATEWAY_URL}`);

var event = {
    "data": {
        "personid":process.env.PERSON_ID
    },
    "extensions": {
        "request": {
            "headers": {
                "x-request-id":"hellotracer"
            }
        }
    
    }
}


var result = personservice.main(event,{});





