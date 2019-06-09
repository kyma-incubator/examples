# Overview

Cloud Native application development is now the hot topic in the industry. Developers want to use modern languages, write micro-services or even serverless functions. They want easy scalability with modern monitoring tools like Prometheus and Grafana. Kubernetes and CNCF landscape projects are no longer perceived as a hype for early adopters. This is the mainstream now.

If you start a new, green field project you are lucky - you can dive into the great variety of tools and frameworks and use them. But how to pick the right tools? At the moment of writing this post, there is 686 projects registered in the [CNCF landscape](https://landscape.cncf.io/).

We have also less lucky developers, that still has to deal with the applications designed when monoliths where cool. What about them? Can they benefit from Cloud Native patterns? Yes, they can!

# Imagine your legacy application
You probably have some applications you have to integrate with or extend it features, but you are not happy with it.You can have different reasons:
- it requires writing code in the language you don't know, and you want to use only Golang or JavaScript
- it is possible to add new feature to the application but requires complex redeployment process which is risky
- you just don't want to touch it because it is fragile and adding anything can make it unstable
- you want to write extension which can be scaled independently of the application

# Wordpress as an example
I prepared some example to help your imagination. The simple scenario with Wordpress as a legacy application. Imagine you are running some commerce site and you created a blog with product reviews and tests based on Wordpress. Now you want to engage your customers and you enabled comments in your blog posts. Users should see their comments immediately published, but you don't have time to moderate the content. The idea is to publish only positive comments automatically, and send send other comments to some channel where customer service can react (slack channel will be used for that).

You could use Wordpress hook `comment_post` and implement a plugin in PHP. But it won't work for me. I don't know PHP, and my team mates don't either. I would like to use external systems (text analytics, slack, maybe more int he future), and I don't want to deal with secrets and authorization flows in Wordpress side. Additionally, I want to utilize all modern DevOps practices and patterns, like [12 Factor App](https://12factor.net). In other words: me and my team want to do cool, cloud native stuff on top of Kubernetes, instead of be wordpress maintainers.

Of course in this simple scenario, microservices, Kubernetes, service mesh, and other tools would be overkill, but real world use cases are more complex, and you can imagine how this initial flow can grow in the future.

# Implementation plan

Let's implement and deploy our example. I will use:
- Kubernetes cluster from Google Kubernetes Engine to deploy both Wordpress and my code.
- KNative eventing and NATS as a messaging middleware to decouple Wordpress from my extension
- Istio service mesh together with Prometheus, Grafana and Jaeger to get monitoring and tracing
- Kubeless as serverless engine for my code
- Service Catalog and Azure Service Broker to enable Azure Text Analytics services in my Kubernetes cluster
- Grafana Loki for keeping logs
- Wordpress Connector for Kyma and Kyma Application Broker to bind Wordpress API and Events to my code

# Installation
From the list above you can expect long installation process, but I will use Kyma operator from GCP Marketplace that starts GKE cluster and installs all the tools with one click. All you need before is Google Account and GCP Project. If you don't have one you can create it and additionally Google will give you $300 credit for running your cluster.
Go to Marketplace, create the cluster and deploy Kyma on it. Piece of cake!

When it is ready (about 15-20 min) you should see Kyma info and Next Steps panel. Default Kyma installation generates self-signed TLS certificate, and your browser will complain if you try to hit console URL. The base64 decoded certificate is displayed in Kyma info panel. You need to make it trusted in your system. To do this in OSX, copy the certificate string, run:
```
echo '<certificate_string>' | base64 -D > kyma.cer && sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain kyma.cer
``` 
Remember to replace `certificate_string` with the value copied from Kyma info panel.

Then you can open the URL from Address field in your browser. In my case it is: `https://console.35.193.85.37.xip.io`. You can log in to Kyma console using user and password provided in Kyma application details.

## Wordpress installation
If you already have wordpress installed you can go to the next step. If not you can easily deploy wordpress with few clicks in Kyma console.
First, create namespace for wordpress.  Then download the wordpress deployment file: [wordpress-deployment.yaml](wordpress-deployment.yaml) (it is recommended to change the `mysql-pass` secret). Then go to the namespace wordpress and click `+ Deploy new resource to the namespace` link, and select wordpress-deployment.yaml file from your disk.

If you prefer you can do the same from command line (assuming that your current Kubernetes context is set to the Kyma cluster):
```
kubectl create namespace wordpress
kubectl -n wordpress apply -f https://raw.githubusercontent.com/kyma-incubator/examples/master/wordpress/wordpress-deployment.yaml
```
Wait few seconds until wordpress starts. You can check the status in Deployments section:

When all deployments are in the running state navigate to https://wordpress.35.193.85.37.xip.io (replace IP with the one for your cluster), and finish installation wizard. 

## Kyma plugin for Wordpress

Before you install plugins ensure that you have proper configuration of Permalinks. Log into Wordpress as admin, go to Settings -> Permalinks, select `Post name` option and save changes.
Now download and install (Plugins -> Add new -> Upload plugin), and activate these 2 plugins:
- [Basic Auth](https://github.com/WP-API/Basic-Auth/archive/master.zip) - for more details go for [GitHub repository](https://github.com/WP-API/Basic-Auth)
- [Kyma Connector](https://github.com/kyma-incubator/wordpress-connector/archive/master.zip) - for more details go for [GitHub repository](https://github.com/kyma-incubator/wordpress-connector)

Go to Settings -> Kyma Connector, uncheck Verify SSL option (you need it because default Kyma installation uses self-signed certificates), and provide username and password you created during installation, and save changes. 


# Connect Wordpress to Kyma

In this step you will establish trusted connection between Wordpress instance and your Kubernetes cluster. You will also register Wordpress API and Wordpress Events in Service Catalog, and enable both in selected namespace.

In Kyma console navigate back to home and go to Applications, and create new one named `wordpress`.

Open it and press Connect Application link. Copy connection token URL to the clipboard. Go to wordpress Kyma Connector Settings, and paste token URL in Kyma Connection field, and press Connect button. You should see the success message in wordpress and you should see new entry inside Provided Services & Events section of worpdress application in Kyma.

# Diasable SSL for Kyma->Wordpress

Wordpress installed in cluster also uses self-signed SSL certificate. Kyma default settings will not allow for such connection. You need to turn it on by:

  1. Edit the `wordpress-application-gateway` Deployment in the `kyma-integration` Namespace. Run:
    ```
    kubectl -n kyma-integration edit deployment wordpress-application-gateway
    ```
  2. Edit the Deployment in Vim. Select `i` to start editing.
  3. Find the **skipVerify** parameter and change its value to `true`.
  4. Select `esc`, type `:wq`, and select `enter` to write and quit.

# Enable Wordpress Events and APIs in selected namespace

Application connector can expose APIs and Events (Async API) of the application in Service Catalog. To show Wordpress in the Service Catalog you need to first bind application to selected namespace. In the wordpress application create binding and select namespace stage. Now you can go to namespace stage and open Catalog - you should see Wordpress API there. Open it and have a look at API console and Events specification. We will react on `comment.post.v1` event and will interact with `/wp/v2/comments/{id}` API. To make them available in the stage namespace click Add button and create instance of wordpress Service Class. Application Connector behind the scenes creates application gateway (kind of proxy) that is forwarding requests from bounded services or functions to the Wordpress instance. 

# Write your code
You did the wiring, so lets write some code. In the namespace stage create new Lambda named local-review and paste in the editor following code:
``` javascript
const Sentiment = require('sentiment');
const sentiment = new Sentiment();
const axios = require("axios");

function isPositive(txt) {
    let result = sentiment.analyze(txt);
    return result.comparative>0.2;
}

async function setCommentStatus(id, status) {
    let commentUrl = `${process.env.WP_GATEWAY_URL}/wp/v2/comments/${id}?status=${status}`;
    const update = await axios.post(commentUrl);
    return update;
}

async function getComment(id) {
    let commentUrl = `${process.env.WP_GATEWAY_URL}/wp/v2/comments/${id}?context=edit`
    let response = await axios.get(commentUrl);
    return response.data;
}

module.exports = {
    main: async function (event, context) {
        let status = "hold";
        let comment = await getComment(event.data.commentId);
        let positive = await isPositive(comment.content.raw);
        if (positive) {
            status = "approved"
        } 
        setCommentStatus(comment.id, status);
    }
};
```

In the dependencies section add:
```
{
  "dependencies": {
    "axios": "^0.19.0",
    "sentiment": "^5.0.1"
  }
}
```

# Binding
TODO:
- Create Wordpress instance
- Bind it to Lambda (use prefix WP_)

# Test it
TODO
- Write to comments: positive and negative
- Log in as Wordpress admin and check status.
- Check Lambda Logs

# Explore the benefits

## Independence
The first and obvious benefit is that you have the wordpress extension outside of your wordpress deployment. You can modify it, scale it, or completely rewrite. 

## Monitoring
TODO:
- Grafana Dashboards

## Tracing

TODO
- Jaeger 

# Summary
Why should you try Kyma? If you start a new project on Kubernetes, you will get carefully selected, best tools from Cloud Native landscape, already configured and integrated. If you want to move only part of your project to the cloud and you have to keep legacy application around, Kyma will help you to build extension for them using modern tools on top of Kuberbetes.
Please remember that Kyma is actively developed open source project (~80 contributors and ~600 github stars) with the support from such big company as SAP. 