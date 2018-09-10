const axios = require('axios');     //to invoke REST APIs
const winston = require('winston'); //to support logging


var logLevel = process.env.application_log_level // get Log level from environment variable

if (logLevel === undefined) {
    logLevel = "error";
}

const logger = winston.createLogger({
    level : logLevel,
    transports: [
        new winston.transports.Console()
    ]
  });

//Extract Itro trace information to propagate it further ==> see https://istio.io/docs/tasks/telemetry/distributed-tracing/
const traceHeaders = ['x-request-id', 'x-b3-traceid', 'x-b3-spanid', 'x-b3-parentspanid', 
'x-b3-sampled', 'x-b3-Flags', 'x-ot-span-context'];


module.exports = {
    main: function (event, context) {
        console.log(`Log level '${logLevel}'`);

        var traceCtxHeaders = extractTraceHeaders(event.extensions.request.headers);

        var personId = event.data.personid;

        logger.log('info', `Event received for personid '${personId}'`);       
        
        var url = `${process.env.GATEWAY_URL}/api/v1/person/${personId}`;
        
        logger.log('debug', `Calling GET: ${url}`);

        axios.get(url, {
            headers: traceCtxHeaders,
            responseType: 'json'
        }).then(function (response) {
            
            var personMaintained = response.data;

            // Remove specificts that would harm duplicate search
            delete personMaintained.id;
            delete personMaintained.extensionFields;

            
            handleDuplicates(personMaintained, traceCtxHeaders);

        }).catch(error => handleAxiosError(error)); //Generalized Error Handling    
    }
}

function handleAxiosError(error) {
    if (error.response) { //response.status != 2xx
        logger.log('error', `Error Status returned`,
        { "status": error.status, "statusText": error.statusText, "responseHeaders": error.headers, 
        "requestConfig": error.config});
    } else if (error.request) { //request failed
        logger.log('error', `Request failed`, {"errorDetail" : error.request});
    } else {
        logger.log('error', `Unknown error`, {"errorMessage": error.message});
    }
}

function updateDuplicatePersonExtension(duplicatePersonsArray, traceCtxHeaders) {
    
    for (var counter in duplicatePersonsArray) {      

        var url = `${process.env.GATEWAY_URL}/api/v1/person/${duplicatePersonsArray[counter]}`;

        var data = {
            "id": duplicatePersonsArray[counter],
            "extensionFields": {
              "duplicatePersons": duplicatePersonsArray
                }
            };

        logger.log('debug', `Updating Person ${duplicatePersonsArray[counter]}, calling PATCH: ${url}`, {"data": data});
        

        axios.patch(url, data, {
            headers: traceCtxHeaders,
            responseType: 'json'
            }).then(function (response) {
                logger.log('info', `Person ${response.data.id} successfully updated`);
                logger.log('debug', `Person ${response.data.id} updated, `, {"responseData": response.data});
            }).catch(error => handleAxiosError(error));
    }       

}

function handleDuplicates(personToSearch, traceCtxHeaders) {
    var url = `${process.env.GATEWAY_URL}/api/v1/person/search`;


    logger.log('debug', `Calling POST: ${url}`, {"searchRequest": personToSearch});

    axios.post(url, personToSearch, {
        headers: traceCtxHeaders,
        responseType: 'json'
    }).then(function (response) {
        
        var duplicatePersons = response.data;
        
        logger.log('info', `Number of matching Persons found: ${duplicatePersons.length}`);  
        logger.log('debug', `Number of matching Persons found: ${duplicatePersons.length}`, {"searchResponse": duplicatePersons});    

        // get Array of IDs
        var foundPersonArray = duplicatePersons.map(function(person) {
            return person.id;
        });
        
        logger.log('debug', `Duplicate Person IDs`, {"foundPersonArray": foundPersonArray}); 
     

        // Array must be longer than 1 to represent a duplicate
        if (foundPersonArray.length > 1) {
            updateDuplicatePersonExtension(foundPersonArray, traceCtxHeaders);
        }

    }).catch(error => handleAxiosError(error));
}

function extractTraceHeaders(headers) {
   
    logger.log('debug', `Number of headers found: ${headers.length}`, {"headers": headers});

    var map = {};

    for (var i in traceHeaders) {
        h = traceHeaders[i]
        headerVal = headers[h]

        if (headerVal !== undefined) {
            logger.log('debug', `Header ${h} : ${headerVal}`);
            map[h] = headerVal
        }
    }
    return map;
}

