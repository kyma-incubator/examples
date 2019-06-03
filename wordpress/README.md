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
First, create namespace for wordpress.  Then download the wordpress deployment file: (wordpress-deployment.yaml)[wordpress-deployment.yaml] (it is recommended to change the `mysql-pass` secret). Then go to the namespace wordpress and click `+ Deploy new resource to the namespace` link, and select wordpress-deployment.yaml file from your disk.

If you prefer you can do the same from command line (assuming that your current Kubernetes context is set to the Kyma cluster):
```
kubectl create namespace wordpress
kubectl -n wordpress apply -f https://raw.githubusercontent.com/kyma-incubator/examples/master/wordpress/wordpress-deployment.yaml
```
Wait few seconds until wordpress starts. You can check the status in Deployments section:

When all deployments are in the running state navigate to https://wordpress.35.193.85.37.xip.io (replace IP with the one for your cluster), and finish installation wizard. 

## Kyma plugin for Wordpress

Log into Wordpress as admin, and install 2 plugins:
- Basic Auth 
- Kyma Connector

First one you can find in Wordpress Plugin Directory. Install and activate it.

Kyma Connector you can download from [here](https://github.com/kyma-incubator/wordpress-connector/archive/master.zip). See more details in the [GitHub repository](ttps://github.com/kyma-incubator/wordpress-connector). Install Kyma Connector by uploading the zip file with the plugin, and activate it. Go to Settings -> Kyma Connector, uncheck Verify SSL option, and save changes (as you remember default Kyma installation uses self signed certificates). 

# Connect Wordpress to Kyma

In this step you will establish trusted connection between Wordpress instance and your Kubernetes cluster. You will also register Wordpress API and Wordpress Events in Service Catalog, and enable both in selected namespace.

In Kyma console navigate back to home and go to Applications, and create new one named `wordpress`.

Open it and press Connect Application link. Copy connection token URL to the clipboard. Go to wordpress Kyma Connector Settings, uncheck Verify SSL option, and save changes (you need it because default Kyma installation uses self-signed certificates). Now paste token URL in Kyma Connection field and press Connect button. You should see the success message in wordpress and you should see new entry inside Provided Services & Events section of worpdress application in Kyma.

# Write your code



# Use external services

# Turn it on - wiring

# Explore the benefits

## Independence
The first and obvious benefit is that you have the wordpress extension outside of your wordpress deployment. You can modify it, scale it, or completely rewrite. 

## Monitoring

## Tracing

# Summary
Why should you try Kyma? If you start a new project on Kubernetes, you will get carefully selected, best tools from Cloud Native landscape, already configured and integrated. If you want to move only part of your project to the cloud and you have to keep legacy application around, Kyma will help you to build extension for them using modern tools on top of Kuberbetes.
Please remember that Kyma is actively developed open source project (~80 contributors and ~600 github stars) with the support from such big company as SAP. 