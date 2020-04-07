const axios = require('axios'); 

module.exports = { main: async function (event, context) {
    
    let eventLog = {
        'authorization': event.extensions.request.headers.authorization,
        'content-type': event.extensions.request.headers['content-type'],
        data: event.data
    }
    
    console.log(`Event received: ${JSON.stringify(eventLog)}`)
    
    try {
        var res = await axios.post('http://ory-hydra-public.kyma-system.svc.cluster.local:4444/oauth2/token', "grant_type=client_credentials&scope=api",{
            headers: {
                'authorization': event.extensions.request.headers.authorization,
                'content-type': event.extensions.request.headers['content-type']
            }
        });
        
        console.log(`Response received: ${JSON.stringify(res.data)}`)
        event.extensions.response.status(res.status).json(res.data).send();
    } catch (error) {
        console.log(`Error response received: ${JSON.stringify(error.response.data)}`)
        event.extensions.response.status(error.response.status).json(error.response.data).send();
    }

} }