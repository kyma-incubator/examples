const request = require('request');
const traceHeaders = ['x-request-id', 'x-b3-traceid', 'x-b3-spanid', 'x-b3-parentspanid', 'x-b3-sampled', 'x-b3-Flags', 'x-ot-span-context']

module.exports = { main: function (event, context) {
    console.log(event.data);
    var orderId = event.data.orderCode;
    var url = `${process.env.GATEWAY_URL}/electronics/orders/${orderId}`;
    var namespace = 'workshop';
    console.log(namespace)
    console.log(url)
    var traceCtxHeaders = extractTraceHeaders(event.extensions.request.headers)
    console.log(traceCtxHeaders)
    request.get({headers:traceCtxHeaders, url: url, json: true}, function(error, response, body) {
        if(error === null) {
            console.log(response.statusCode)
            if(response.statusCode == '200'){
                var order = {
                    orderId: orderId,
                    total: body.totalPriceWithTax.value,
                    namespace : namespace
                }
                request.post({headers: traceCtxHeaders, url: "http://http-db-service.workshop:8017/orders",json: order}, function(error, response, body){
                    console.log(response.statusCode)
                })
            }else{
                console.log('Call to occ failed with status code ' + response.statusCode)
                console.log(response.body)
            }
        } else {
            console.log(error)
        }
    })
}}

function extractTraceHeaders(headers) {
    var map = {};
    for (var i in traceHeaders) {
        h = traceHeaders[i]
        headerVal = headers[h]
        console.log('header' + h + "-" + headerVal)
        if (headerVal !== undefined) {
            console.log('if not undefined header' + h + "-" + headerVal)
            map[h] = headerVal
        }
    }
    return map;
}
