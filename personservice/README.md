# A sample Application based on Spring Boot showing the features of Kyma

## Table of Contents

- [A sample Application based on Spring Boot showing the features of Kyma](#a-sample-application-based-on-spring-boot-showing-the-features-of-kyma)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Deploy the application](#deploy-the-application)
    - [Namespace Setup](#namespace-setup)
    - [Mongo DB](#mongo-db)
    - [Java Build](#java-build)
    - [Docker Credential Helper Setup](#docker-credential-helper-setup)
    - [Deploy Kyma locally (Minikube)](#deploy-kyma-locally-minikube)
    - [Deploy Kyma in a cluster](#deploy-kyma-in-a-cluster)
    - [Checks](#checks)
    - [Try out on Kyma](#try-out-on-kyma)
  - [Connect your Service to Kyma as Extension Platform](#connect-your-service-to-kyma-as-extension-platform)
    - [About](#about)
    - [Create new Application Connector Instance on Kyma](#create-new-application-connector-instance-on-kyma)
    - [Pair Person Service with Kyma Application Connector](#pair-person-service-with-kyma-application-connector)
    - [Option 1: Manual Pairing with Kyma](#option-1-manual-pairing-with-kyma)
    - [Option 2: Automatic Pairing with Kyma](#option-2-automatic-pairing-with-kyma)
    - [Checks](#checks-1)
  - [Extend your Person Service](#extend-your-person-service)
    - [Intro](#intro)
    - [Create Service Instance](#create-service-instance)
    - [Develop your Lambda Function](#develop-your-lambda-function)
    - [Deploy your Lambda Function](#deploy-your-lambda-function)
    - [Test your Lambda](#test-your-lambda)
  - [Bind your Person Service to a brokered Redis Backing Service](#bind-your-person-service-to-a-brokered-redis-backing-service)
    - [Intro](#intro-1)
    - [Create Redis Service Instance and Bind it to the Person Service](#create-redis-service-instance-and-bind-it-to-the-person-service)
    - [Update Kubernetes Deployment Configuration](#update-kubernetes-deployment-configuration)
    - [Test the Service](#test-the-service)
  - [Protect the Service](#protect-the-service)
    - [Intro](#intro-2)
    - [Deploy OAuth2 Authorization Server](#deploy-oauth2-authorization-server)
    - [Adapt Kubernetes Deployment](#adapt-kubernetes-deployment)
    - [Optional: Create IDP Preset](#optional-create-idp-preset)
    - [Secure your API](#secure-your-api)
    - [Security For Lambdas (Not on Minikube)](#security-for-lambdas-not-on-minikube)
    - [Test the Service](#test-the-service-1)
  - [Operate your Service: Make it Self-Healing](#operate-your-service-make-it-self-healing)
    - [Intro](#intro-3)
    - [Preparation](#preparation)
    - [Determining whether your service is alive](#determining-whether-your-service-is-alive)
    - [Determining whether your service is ready to serve traffic](#determining-whether-your-service-is-ready-to-serve-traffic)
    - [Deploying to Kyma](#deploying-to-kyma)
    - [Testing](#testing)
  - [Operate your Service: Traces and Logs](#operate-your-service-traces-and-logs)
    - [Intro](#intro-4)
    - [Testing Tracing](#testing-tracing)
    - [Testing Logging](#testing-logging)
      - [Option 1: Loki Query via Terminal](#option-1-loki-query-via-terminal)
      - [Option 2: Loki Query via UI](#option-2-loki-query-via-ui)
  - [Operate your Service: Metrics](#operate-your-service-metrics)
    - [Intro](#intro-5)
    - [Collecting Metrics in Prometheus](#collecting-metrics-in-prometheus)
    - [Creating a Dashboard](#creating-a-dashboard)

## Overview

This sample application was created to give you a running end to end sample application implemented in Java / Spring Boot running on Kyma. In the end state it should make all the features Kyma delivers visible to you as developers. Also it should help you to get started and implement your own scenarios on Kyma.

> **NOTE:** This showcase is not meant to be Best Practice / Production ready code. Instead it is often kept simple with manual steps to make clear what actually happens. If you have issues/remarks while using it, please feel free to feedback.  

## Prerequisites

This application runs on [Kyma](https://kyma-project.io). Therefore, to try out this example on your local machine you need to [install Kyma](https://kyma-project.io/docs/latest/root/kyma#getting-started-local-kyma-installation) first, or have access to Kyma cluster.  

**![alt text](images/kyma_symbol_text.svg "Logo Title Text 1")  
This example is tested and based on [Kyma 1.1.0](https://github.com/kyma-project/kyma/releases/tag/1.1.0). Compatibility with other versions is not guaranteed.**

## Deploy the application

### Namespace Setup

A Namespace is a custom Kyma security and organizational unit based on the concept of Kubernetes Namespaces. Kyma Namespaces allow you to divide the cluster into smaller units to use for different purposes, such as development and testing. Learn more from official documentation about [Namespaces](https://kyma-project.io/docs/latest/root/kyma/#details-namespaces).

To setup the namespace for this showcase call this command from the project root:

`kubectl apply -f namespace.yaml`

Now, once you call `kubectl get namespaces -l=env=true` among other namespaces you will see the one you just created.

Issue the following commands to delete default resource constraints and re-create them a little more relaxed:

`kubectl delete -n personservice LimitRange kyma-default`  
`kubectl delete -n personservice ResourceQuota kyma-default`  
`kubectl apply -f namespace-resources.yaml -n personservice`

This was to ensure we don't hit ceilings in terms of memory usage (However, on Minikube/Local installation this might be challenging). For more details read [Configure Default Memory Requests and Limits for a Namespace](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/memory-default-namespace/)

### Mongo DB

To deploy Mongodb use Helm (https://helm.sh). To install helm do the following:

1. First we need to fetch certificates from our cluster to have a secure communication via helm. Navigate to your `~/.helm` directory and get the certificates:  
`kubectl get -n kyma-installer secret helm-secret -o jsonpath="{.data['global\.helm\.ca\.crt']}" | base64 --decode > "ca.pem"`  
`kubectl get -n kyma-installer secret helm-secret -o jsonpath="{.data['global\.helm\.tls\.crt']}" | base64 --decode > "cert.pem"`  
`kubectl get -n kyma-installer secret helm-secret -o jsonpath="{.data['global\.helm\.tls\.key']}" | base64 --decode > "key.pem"`

2. Initialize Helm (if not already done, client-only option as kyma already comes with tiller installed):  
`helm init --client-only`

2. Then deploy Mongo DB:  
`helm install --tls --name first-mongo --set "persistence.size=2Gi" stable/mongodb --namespace personservice` 

### Java Build
Project is built using:  
`mvn clean package`  

It uses jib (https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin) to build and push to a docker registry (which does not require a local docker install). You **must** use the following maven properties to adapt your local installation:

- docker.repositoryname: Docker repository that the image will be published to (just replace by username on docker hub)
- jib.credentialhelper: Docker credential helper that will be used to acquire docker hub credentials (see: https://docs.docker.com/engine/reference/commandline/login/ heading: "Credential helper protocol") (adapt to YOUR OS, pass or secretservice for Linux, wincred for Windows and osxkeychain for Mac)

You **can** use the following maven properties to adapt to your local installation:

* project.version: Tag that will be assigned to docker image
* jib.version: Version of the jib plugin that will be used

For editing the code I recommend either Eclipse with Spring plugins installed or Spring Tool Suite (https://spring.io/tools/sts/all). You will also need to install the Lombok plugin (https://projectlombok.org/setup/overview). Lombok is used to generate getters/setters and sometimes constructors. It keeps the code lean and neat.

### Docker Credential Helper Setup

Docker credential helpers can be downloaded from https://github.com/docker/docker-credential-helpers. There are various versions for different Operating Systems. If you want to use docker-credential-pass please ensure that gpg and pass are installed. A detailed walkthrough is available under https://github.com/docker/docker-credential-helpers/issues/102 (Steps 1 to 10).

To provide your credentials create a json file like the one below:

```
{ 
    "ServerURL": "registry.hub.docker.com",
    "Username": "<username>",
    "Secret": "<password>"
}
```

To push this file into the credentials helper enter the following statement under Linux:

`cat credentials.json | docker-credential-pass store`

Windows:

`type credentials.json | docker-credential-wincred store`

To delete a set of credentials:

`echo <ServerURL> | docker-credential-pass erase`

To read a set of credentials:

`echo <ServerURL> | docker-credential-pass get`

### Deploy Kyma locally (Minikube)

Deploying Kyma requires to upload a configmap and also a kubernetes deployment and a service.

Before deploying the attached files you need to adapt `mongo-kubernetes-local1.yaml` to your cluster. The parts that require attention are marked with `#changeme:` and instructions are available in the comments.

The below commands do this: 

`kubectl apply -f mongo-kubernetes-configmap-local.yaml -n personservice`  
`kubectl apply -f mongo-kubernetes-local1.yaml -n personservice`

`mongo-kubernetes-local1.yaml` creates the following Kubernetes objects:

* Kubernetes Deployment for the Spring App (including Istio setup)
* Kubernetes Service pointing towards the pods created by the Deployment
* Kyma API exposing the service through an Istio Ingress

To make the API accessible from your browser, you need to add the following entry to your /etc/hosts (Windows: C:\Windows\System32\drivers\etc\hosts) file, behind all other kyma entries `personservice.kyma.local`.

Also make sure you trust the self-signed kyma ssl server certificate in the browser.

### Deploy Kyma in a cluster

Deployment to kyma requires to upload a configmap and also a kubernetes deployment and a service.

Before deploying the attached files you need to adapt `mongo-kubernetes-cluster1.yaml` to your cluster. The parts that require attention are marked with `#changeme:`

The below commands do this:

`kubectl apply -f mongo-kubernetes-configmap-cluster.yaml -n personservice`  
`kubectl apply -f mongo-kubernetes-cluster1.yaml -n personservice`

`mongo-kubernetes-cluster1.yaml` creates the following Kubernetes objects:

* Kubernetes Deployment for the Spring App (including Istio setup)
* Kubernetes Service pointing towards the pods created by the Deployment
* Kyma API exposing the service through an Istio Ingress

Your service should now be accessible on whatever you specified under `personservice.{clusterhost}` (clusterhost being the hostname of your kyma cluster).


### Checks

To check whether everything is up and running please issue the following command:  
`kubectl get pods -n personservice`

All pods should have status running. Otherwise wait and repeat until this is the case.

### Try out on Kyma

After deployyment you can access and try out the swagger documentation under  
`https://personservice.{clusterhost}/swagger-ui.html`

If you don't like Open API (fka. Swagger) here is some other documentation:  

The API is accessible using the following endpoints:

* GET /api/v1/person: List all persons in the DB
* POST /api/v1/person: Create person with the following data
```
{
	"firstName":"John",
	"lastName":"Doe",
	"streetAddress":"Nymphenburger Str.",
	"houseNumber":"86",
	"zip":"80636",
	"city":"Muenchen",
	"extensionFields":{
		"countryIso2":"De"
	}
}
``` 
* GET /api/v1/person/{personid}: Return all data for person with id `personid`
* DELETE /api/v1/person/{personid}: Delete person with id `personid`
* PATCH /api/v1/person/{personid}: Update person's name with id `personid` with the following data:
```
{
	"firstName":"Jack",
}
```

* POST /api/v1/person/search: Search for persons matching the following criteria:
```
{
	"city":"Muenchen",
	"extensionFields":{
		"countryIso2":"De"
	}
}
```

## Connect your Service to Kyma as Extension Platform

### About
Altough our Person Service application is running inside of Kyma, we will now treat it like any other external application (hence you can also try this outside of your Kyma instance). This is just to demonstrate what you would do with your legacy application to hook it up to Kyma. The below picture highlights what we are going to do:  

![Connect Remote Environment Response Screenshot](images/extensibility.png)

1. Create/Deploy new Application Connector Instance on Kyma &rarr; This is to provide a dedicated Enpoint for the Person Service to connect to Kyma
2. Pair Person Service with Kyma Application Connector &rarr; This is to establish secure connectivity from Person Service to Kyma (even over public networks)
3. Register Person Service Metadata at the Application Connector &rarr; This is to fill the API and Event Catalog on Kyma
4. Fire Events to the Kyma Event Bridge &rarr; This is to execute event driven logic/extensions on Kyma

Steps 3 and 4 are based on deploying additional configuration to the kyma instance.

### Create new Application Connector Instance on Kyma

To connect external systems (called Applications) you need to use the application connector. For information see: https://kyma-project.io/docs/latest/components/application-connector.

To create one, you need to issue the following command:  
`kubectl apply -f app-personservice.yaml`

To check whether the pods are up and running issue  
`kubectl get pod -n kyma-integration`  

The result should look like this:

```
NAME                                                           READY     STATUS    RESTARTS   AGE
personservicekubernetes-application-gateway-78447c489d-d55fk   2/2       Running   0          45m
personservicekubernetes-event-service-7466dc4c8f-2xfxf         2/2       Running   0          45
```

After Pods are recreated, your new Application shall show up in the Kyma Console under `Integration -> Applications`.  

###  Pair Person Service with Kyma Application Connector  
To pair the Person Service with Kyma we will present two approaches.  
In the [automatic flow](#Automatic-Pairing-with-Kyma)  you pass the URL generated in Kyma to the Person Service and the certificate requests and actual registration is handled by the service itself. This is the easy way and lets you move on fast.  
The [manual flow](#Manual-Pairing-with-Kyma) gives you full control of the steps and guides you through the requests to generate a certificate and register at Kyma. This includes more work but provides a deeper insight into the mechanics of Application Connectivity.  

First, you need to update the application to enable the Application Connector profile. This will show you the REST endpoints for the automatic and manual flow.

We first create a configmap with the registrationfile that the Person Service posts against Kyma to register its API and events. After that execute the following command from the `/registration` directory:  
`kubectl create configmap registrationfile --from-file=registrationfile.json -n personservice`

To enable the Application Connector endpoints we reconfigure our service:  

* Cluster: `kubectl apply -f mongo-kubernetes-cluster2.yaml -n personservice`
* Minikube: `kubectl apply -f mongo-kubernetes-local2.yaml -n personservice`

Make sure the new pod is created (otherwise delete the old pod, Kubernetes will recreate one). To check whether your changes are active, issue the following command until you again have **exactly** 1 Pod of `personservice-*-*` in status running:  
`kubectl get pods -n personservice`

### Option 1: Manual Pairing with Kyma
This sub chapter describes how to manually issue the necessary requests and handle the certificates. This will allow you to fully understand the steps of how applications register to Kyma. At the end of this section we will manually upload the JKS to our "manual" endpoint.

1. Click on `Connect Application` and **open the link in the popup box in another browser tab**.
![Connect Remote Environment Screenshot](images/applicationpairing.png)

2. Copy the `csrUrl` address and make sure to save the `infoUrl` somewhere. We will need it for the REST request in the last step.
![Connect Remote Environment Response Screenshot](images/remoteenvironmentpairing2.png)

3. Create a Certificate Signing request using OpenSSL (https://www.openssl.org/) a series of commands. Before doing this create a new directory called `security` and then go ahead with OpenSSL in the new dir.
Create Key: 

```
openssl genrsa -out personservicekubernetes.key 2048
openssl req -new -sha256 -out personservicekubernetes.csr -key personservicekubernetes.key -subj "/OU=OrgUnit/O=Organization/L=Waldorf/ST=Waldorf/C=DE/CN=personservicekubernetes"
```

4. Encode the content of `personservicekubernetes.csr` (Base64) and use the REST client of your choice to create the following POST call to the full URL (csrUrl) with the token you copied previously:
![Connect Remote Environment CSR Screenshot](images/remoteenvironmentpairing3.png)
5. After sending you will receive a base 64 encoded signed certificate. Decode the response and save as `personservicekubernetes.crt`. The decoded response will contain separators with `BEGIN CERTIFICATE` and `END CERTIFICATE` respectively.
6. Now you can use OpenSSL and java keytool (part of the jdk) to create a PKCS#12 (P12, also good for browser based testing) file and based on that create a Java Key Store (JKS, for the Person Service) for our service. **Do not change any passwords, except if you really know what you are doing!!!**
   ```
   openssl pkcs12 -export -name personservicekubernetes -in personservicekubernetes.crt -inkey personservicekubernetes.key -out personservicekubernetes.p12 -password pass:kyma-project
   keytool -importkeystore -destkeystore personservicekubernetes.jks -srckeystore personservicekubernetes.p12 -srcstoretype pkcs12 -alias personservicekubernetes  -srcstorepass kyma-project -storepass kyma-project
   ```
7. Now copy the resulting `personservicekubernetes.jks` file to `security` directory.
8. Use the POST `/applicationconnector/registration/manual` endpoint to register the application now. You need to pass the `infoUrl` the password we used for the JKS (`kyma-project`) and the JKS file.

![Connect Remote Environment Manual Endpoint](images/remoteenvironmentpairing4.png)

To test your deployed application connector instance you can also import the personservicekubernetes.p12 file into your Browser and call the url depicted as metadataUrl in the initial pairing response JSON.

### Option 2: Automatic Pairing with Kyma
This section describes the "automatic" approach where the application takes care of the requests and certificates. You can use the POST `/applicationconnector/registration/automatic` endpoint of the personservice where you insert the URL created from the Kyma "Connect URL" button into the JSON (see manual process step 1). The service will create the CSR itself, get the certificate from Kyma and store an object in the mongodb.

![Insert connect URL in POST body](images/applicationpairing_auto.png)

The request should succeed with a status code 200 and an ID in the response body. You can now see the registered Person Service in the Kyma Application.

### Checks

After either the manual or automatic flow you should be able to see the following under Applications:  
![Application Registration Screenshot](images/applicationregistration2.png)

This means now you can bind this Application to a Kyma Namespace and process events in Serverless Lambda functions. You can either use the UI or kubectl. In the UI, bind the Application by clicking `Create Binding`. On kubectl issue kubectl apply `kubectl apply -f app-personservice-namespace-map.yaml` to do the same.

Then navigate to the Namespace "personservice" and select the "Catalog" and explore the Service 'Person API' (see screenshot).

![Application Registration Screenshot](images/applicationregistration3.png)

Now you are ready to instantiate the service and bind it to Lambda's and deployments which implement your extension logic.

## Extend your Person Service

### Intro

The preferred way to extend business applications with Kyma is through event driven Lambda (serverless) functions. In the subsequent chapters you will see how to implement such a Lambda function using Node.js and how to bind it to the person maintained Event. 

The function as such is pretty simple. It is triggered when a person was created. Then the logic goes as follows:

1. Call personservice API to retrieve details about the person that what changed (GET /api/v1/person/{id})
2. Call personservice API to retrieve a list of persons with the same values (POST /api/v1/search)
3. Call personservice API to persist a list of IDs on the extension field "duplicatePersons" (PATCH /api/v1/person/{id}) for all identified duplicates

As always this function is not intended for productive use. 

### Create Service Instance

A precondition for this scenario is that all steps mentioned in [Pair Person Service with Kyma](#Pair-Person-Service-with-Kyma-Application-Connector) have been executed properly (manually or automatically).

In your Kyma Namespace "Personservice" go to the Catalog and create a new service instance of the Person API:

![Service Instance Creation Screenshot](images/serviceinstance1.png)

After that verify your instance by selecting "Instances" in the Service Catalog:

![Service Instance Creation Screenshot](images/serviceinstance3.png)


### Develop your Lambda Function

When developing Lambda functions, I like to start locally using the IDE of my choice. Kyma lambda functions are basically "normal" Node.js modules containing the containing the following snippet:

```
module.exports = { 
	main: function (event, context) {
		//code goes here
	} 
}

```

The meaning and contents of the event and context object is described in https://kyma-project.io/docs/latest/components/serverless.  
As soon as a Lambda requires NPM dependencies, it also require a package.json file (easiest way to create a skeleton is `npm init`). Dependencies are then managed in the dependencies section:

```
{
  "name": "personservicelambda",
  "version": "1.0.0",
  "description": "Demo Lambda for the Person Service",
  "main": "personservice.js",
  "keywords": [
    "kyma"
  ],
  "dependencies": {
    "axios": "^0.18.0",
    "winston": "^3.0.0",
    "express": "^4.16.3",
    "dotenv":"^6.0.0"
  }
}
```

Kyma injects service bindings (including Applications) as Environment Variables. To be able to mimic this behavior locally the package `dotenv` can be used. It basically converts the contents of a `.env` (no file extension, literally ".env") to normal environment variables that can be accessed through `process.env.environment_variable_name` in your code. Sample for that would be (getting the internal URL of the API Connector pointing to the Remote Environment):

```
require('dotenv').config();

console.log(`GATEWAY_URL = ${process.env.GATEWAY_URL}`);
```

To run your Lambda locally you also need a wrapper. The below snippet is actually used to run the code of the sample Lambda:

```
require('dotenv').config();
const personservice = require('./personservicemodule');
var express = require('express');
var app = express();

console.log(`GATEWAY_URL = ${process.env.GATEWAY_URL}`);

app.get("/", async function (req, res) {
    var event = {
        "data": {
            "personid":req.query.personId
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
```


The code for the sample Lambda function is contained in the "Lambda" folder. in order to run it locally, ensure you have a Node.js (https://nodejs.org/en/download/) environment (Version 8.x+ installed, to check, execute `node --version`). You also need to make a few changes to the ".env" file. Again you need to replace all occurrences of replaceme. "GATEWAY_URL" must have a value pointing to your deployment of personservice (only root, no "/" in the end).
The wrapper starts a local http server on port 3000 that uses the query parameter personId which must be the ID of a person in your mongo db (basically GET /api/v1/person/{PERSON_ID} must return a 200 status code). 

Then install the dependencies: `npm install axios winston dotenv express`
After that you can run your service: `node personservicecaller.js`

Once this is done you can send requests through `http://localhost:3000?personid=<your personid>` and execute the function locally.

This should give you a fair idea of how to develop Lambdas.


### Deploy your Lambda Function

To deploy your Lambda you need to go to your "personservice" Kyma namespace. Click on Lambdas and create a new one. Fill all the fields as shown in the below screenshots and then save:

![Lambda Creation Screenshot](images/lambda1.png)

![Lambda Creation Screenshot](images/lambda2.png)
**Important:** Leave the prefix empty!

![Lambda Creation Screenshot](images/lambda3.png)

![Lambda Creation Screenshot](images/lambda4.png)

![Lambda Creation Screenshot](images/lambda5.png)

Now the command `kubectl get pods -n personservice -l app=mark-duplicate-persons` should return a pod in status running (might take several repetions though). Now you can issue the following command to inspect the logs:  
`kubectl logs -n personservice -l app=mark-duplicate-persons -c mark-duplicate-persons`  

As nothing is happening, you should only see the periodic health checks:

```
::ffff:127.0.0.1 - - [28/Aug/2018:14:37:48 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:37:53 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:37:58 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:38:03 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:38:08 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:38:13 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:38:18 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
::ffff:127.0.0.1 - - [28/Aug/2018:14:38:23 +0000] "GET /healthz HTTP/1.1" 200 2 "-" "curl/7.38.0"
```

### Test your Lambda

Now you can again use the API as depicted in section [Try out on Kyma](#try-out-on-kyma). Here you basically only have to create a new person using the POST /api/v1/person operation (make sure the record is **unique in your Database**. The easiest way to ensure that, is to invoke the DELETE /api/v1/person operation to empty the database). Make sure you get a 201 response code.

The log, inspected by `kubectl logs -n personservice -l app=mark-duplicate-persons -c mark-duplicate-persons`, should contain something similar to:

```
Log level 'info'
{"level":"info","message":"Event received for personid '5b85601a8a50350001a0c160'"}
::ffff:127.0.0.1 - - [28/Aug/2018:14:45:47 +0000] "POST / HTTP/1.1" 200 - "-" "Go-http-client/1.1"
{"level":"info","message":"Number of matching Persons found: 1"}
```

This means the lambda was executed and no duplicates have been found (search returned only one result). Now create a duplicate using the POST /api/v1/person operation. Again make sure you get a 201 response code.

Now the log, inspected by `kubectl logs -n personservice -l app=mark-duplicate-persons -c mark-duplicate-persons`, should contain something similar to:

```
{"level":"info","message":"Event received for personid '5b85601a8a50350001a0c160'"}
::ffff:127.0.0.1 - - [28/Aug/2018:14:49:01 +0000] "POST / HTTP/1.1" 200 - "-" "Go-http-client/1.1"
{"level":"info","message":"Number of matching Persons found: 2"}
{"level":"info","message":"Person 5b8560dd4b2eaa0001897227 successfully updated"}
{"level":"info","message":"Person 5b85601a8a50350001a0c160 successfully updated"}
```

Also invoking GET /api/v1/person/{id} should now show a result with extensionFields/duplicatePersons containing an array of person IDs:

```
{
  "id": "5b8560dd4b2eaa0001897227",
  "firstName": "John",
  "lastName": "Doe",
  "streetAddress": "Nymphenburger Str.",
  "houseNumber": "86",
  "zip": "80636",
  "city": "Muenchen",
  "extensionFields": {
    "duplicatePersons": [
      "5b85601a8a50350001a0c160",
      "5b8560dd4b2eaa0001897227"
    ]
  }
}

```

## Bind your Person Service to a brokered Redis Backing Service 

### Intro

Applications often times require backing services such as databases, caches, message brokers, etc. In a cloud world these are generally provided and managed by the cloud provider with clear SLAs. These services are generally discovered in a central service catalog. Once a service is deemed relevant it is instantiated (i.e. a Service instance is created, which would map to a e.g. a database instance). This instance is then provided to your application using a binding (i.e. the configuration needed to access the service is injected into the application/container using environment variables). This process nicely decouples application from cloud infrastructure and hence makes it portable.

In this example we are going to address the issue with frequent callbacks of the lambda to our service. To mitigate the consequences for the database we are going to introduce caching. For that we are going to enable caching in the Spring Application and hook it up to a Redis cache which is provisioned via the Helm broker (a hidden feature of Kyma making cloud like service brokers also available locally, it basically wraps the same procedure we have used to provision the [Mongo DB](#mongo-db) in a "normal" service broker process). 

### Create Redis Service Instance and Bind it to the Person Service

To create a service instance you need to go to the service catalog in your Kyma Namespace ("personservice"). Navigate to Redis:

![Service Provisioning Screenshot](images/serviceprovisioning1.png)

Add and configure a new instance to your namespace:

![Service Provisioning Screenshot](images/serviceprovisioning2.png)

After you have created your instance you have to bind it to the Persons Service. To do that, go to Instances and navigate to your created instance.

![Service Binding Screenshot](images/servicebinding1.png)

Bind a new application and click through the wizard:

![Service Binding Screenshot](images/servicebinding2.png)

![Service Binding Screenshot](images/servicebinding3.png)

After that, check whether your personservice pods are running: `kubectl get pods -n personservice -l app=personservice`

Then invoke the environment api of the person service (this is returning a json representation of all environment variables part of the container):`https://personservice.{cluster}/api/v1/environment` and search for the following variables:

* HOST
* PORT
* REDIS_PASSWORD

If you don't find them, restart your pods and try again: `kubectl delete pods -n personservice -l app=personservice`.

The variables are injected through the Binding (details are available in the documentation).

```
spec:
  containers:
  - envFrom:
    - secretRef:
        name: redis-instance-binding
```

To see the concrete values, issue the following command: `kubectl get secret -o yaml <name_of_secret>`. You can find the name of the secret in the "secret" column of your "bound applications". The values are base64 encoded.


### Update Kubernetes Deployment Configuration

In order for the personservice to properly connect to the redis cache, you need to set a couple of environment variables. To do so, `mongo-kubernetes-local3.yaml` or `mongo-kubernetes-cluster3.yaml` have been adapted. However you still need to replace the values depicted with `#changeme` to cater to your environment. Below the changed values to bind to the service:

```
              - name: spring_profiles_active
                value: "ApplicationConnector,Cache"
              - name: logging_level_com_sap_demo_service
                value: "TRACE"
              - name: spring_redis_host
                value: "${HOST}"   
              - name: spring_redis_port
                value: "${PORT}"
              - name: spring_redis_password
                value: "${REDIS_PASSWORD}"
```

Make the following settings:

* `spring_profiles_active`: This is used to activate the PersonServiceCache.java implementation
* `logging_level_com_sap_demo_service`: Activate logging to be able to see cache in trace results
* `spring_redis_host`: This is to provide a reference to the redis host. By putting the value in the proposed format (${HOST}) you are pointing spring to another environment variable which is introduced through the binding.
* `spring_redis_port`: This is to provide a reference to the redis port.
* `spring_redis_password`: This is to provide a reference to the redis password.

Now you can update your deployment and restart the pods:

* Local:

`kubectl apply -f mongo-kubernetes-local3.yaml -n personservice`  

* Cluster:

`kubectl apply -f mongo-kubernetes-cluster3.yaml -n personservice`  


### Test the Service

To test this you will have to stream the logs of your personservice. To do this issue the following command:  
`kubectl logs -n personservice {your pod}  personservice --follow` (replace "{your pod}" with the name of your pod).

Now invoke GET /api/v1/person/{personid}. During the first call you should see something along the lines of the below example. All subsequent calls will not appear as they will be directly fetched from the cache.

```
Entering public com.sap.demo.entity.Person com.sap.demo.service.PersonServiceCache.findPerson(java.lang.String) with Arguments:
class java.lang.String: 5b8560dd4b2eaa0001897227
2018-08-31 11:20:50.577 DEBUG 1 --- [nio-8080-exec-2] com.sap.demo.service.PersonServiceCache  : 2e8ff093-4c1c-9b1b-a274-5e8a63f1cc84: Cache miss for Person ID: 5b8560dd4b2eaa0001897227 
2018-08-31 11:20:50.577 TRACE 1 --- [nio-8080-exec-2] com.sap.demo.service.PersonServiceCache  : 2e8ff093-4c1c-9b1b-a274-5e8a63f1cc84:
Exiting PersonServiceCache.findPerson(..) with result: Person(id=5b8560dd4b2eaa0001897227, firstName=John, lastName=Doe, streetAddress=Nymphenburger Str., houseNumber=86, zip=80636, city=Muenchen, extensionFields={duplicatePersons=[5b85601a8a50350001a0c160, 5b8560dd4b2eaa0001897227]})
```

## Protect the Service

### Intro

Kyma (through Istio Authentication Policies) allows to add JWT protection to your API. This gives you the chance to offload offline JWT validation to Kyma and focus on processing the authorizations in your application. In this section we are going to setup a Dummy OAuth2 Authorization Server for issuing the tokens we need, enhance our person service to authorize users using Spring Security, make the Kyma API Check for Authorizations and update the Application Registration in the Apllication Connector to also supply OAuth2 tokens.

### Deploy OAuth2 Authorization Server

This example uses the Dummy Authorization Server made available under https://github.com/akrausesap/jwt_issuer. Either clone the repository and update the `kubernetes-kyma.yaml` file with your preferred host (marked with `#changeme`) or deploy as is: 

```
apiVersion: gateway.kyma-project.io/v1alpha2
kind: Api
metadata:
  name: tokenissuer
spec:
  #changeme
  hostname: tokenissuer
  service:
    name: tokenissuer
    port: 8080

```

Then deploy to your cluster (custom host):  
`kubectl apply -f kubernetes-kyma.yaml -n personservice`

Or directly reference the the file from git:

`kubectl apply -n personservice -f https://raw.githubusercontent.com/akrausesap/jwt_issuer/master/kubernetes-kyma.yaml`

After that check whether the pod is running with:  
`kubectl get pods -n personservice -l app=tokenissuer`

Then you can go to `https://tokenissuer.{hostname}/swagger-ui.html`. A restart of your personservice pod may be required (just delete the pod). Test the following operations:

* GET /jwk: To get the public key used for validating the signature. Should return something like: 

```
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "4d55898e-01eb-4670-bf2e-ac8158cbcd6d",
      "n": "ofqo3O3rfJ04thtPf5VSM1EEr8AdffYcGEGIlBU-ml9JVal6YFgTdPXkpLBB9ckOMau6kr49fzGYZW9T44D0XwLFd1sv-ix8eKmsXcfmHpUCG4faSAb-LWpQS_Y2Kq_t_mMuHG4qco0umYGw2PmmD-8fDsyvkZUXqp3tERdIztC46GEaPQ0A0pNO49EhNk-yRmPibHYgC6hdPBdpsgFZwXm1xKr6XZufIXgoaR0rgAzs2EOS8brDKcM_2GwiYzCU4SktA8M96Mbw4bgBwwTW9dpt3Ciq0iQuPPY-3KDrw7yPBecKcVt2SbddhW7ifsbTwJADiphwx0g_oMza4rgQ8Q",
      "e": "AQAB"
    }
  ]
}
```


* Any Verb /oauth2/token/query?subject=testsubject: To get a JWT Token. Should return something like: 

```
{
  "access_token": "eyJraWQiOiI0ZDU1ODk4ZS0wMWViLTQ2NzAtYmYyZS1hYzgxNThjYmNkNmQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0c3ViamVjdCIsImlzcyI6IkR1bW15SldUSXNzdWVyIn0.bFJLdn4BOhgCkMLN770tOwyodqE5Gu6LRYGfrrVZuHztmlnmyY3OVBdCsNH31CPMgmO7Z1ZDPnJw1tNXnXCv58r11BNcMlHBbGjaZPzNPz5__05qQeTvlmEtgP7ehYBGOqlhRmDlbWZYAb2CVhOmncAzgloGZlXPFFoa3knMgoe5ewH85DOBx9-_PifQERbgBjvZmCq85rlt_CWynfVOLjVZYGBlK40k9opT9ci8VZNid4bSHMgrC3v8pdCXMB9FnRwf5rnNHy4OxPDd4_nTKfA5dsRINc8g1Q9aeD6ufJLsCoPzhBhXgBNg1dTcuV84DpYKWoysmM6r6nnDSjdo4A",
  "token_type": "Bearer"
}
```

If you are running on locally on Minikube you also need to adapt your hosts file to make sure the hostname for the tokenissuer is properly mapped to your minikube IP. [Deploy to Local Kyma (Minikube)](#deploy-to-local-kyma-minikube) describes how to do this (in the very end).

### Adapt Kubernetes Deployment

In order for the person service API to validate JWT tokens, you need to flip an environment variable (Activate the Security Profile):

```
          - name: spring_profiles_active
            value: "ApplicationConnector,Cache,Security"
```

This is already done in `mongo-kubernetes-local4.yaml` and `mongo-kubernetes-cluster4.yaml`, however you still need to update the `changeme` parts as depicted previously.

Now you can update your deployment, the api and restart the pods:

* Local:

`kubectl apply -f mongo-kubernetes-local4.yaml -n personservice`  

* Cluster:

`kubectl apply -f mongo-kubernetes-cluster4.yaml -n personservice` 

Now you can test the spring application. When calling GET /api/v1/person you should receive the following 401 response:

```
{
    "error": "unauthorized",
    "error_description": "Full authentication is required to access this resource"
}
```

Now you should acquire a token from the previously deployed Dummy Authorization Server. To do so, send the following request:


```
POST /oauth2/token/body

{
	"subject": "ItsMe",
	"issuer":"https://tokenissuer.{cluster}.kyma.cx",
	"expirationDurationMinutes": 3600,
	"scopes": ["person_write", "person_read"],
	"includeIssuedAt": true
}

```

Copy the `access_token` from the below sample response:

```
{
    "access_token": "eyJraWQiOiI5YzRlNDVmZS01ODhiLTQ5N2QtODY0MS01NjEyODAzYTI0ZTQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJJdHNNZSIsImlzcyI6Imh0dHBzOi8vdG9rZW5pc3N1ZXIue2NsdXN0ZXJ9Lmt5bWEuY3giLCJzY29wZSI6WyJwZXJzb25fd3JpdGUiLCJwZXJzb25fcmVhZCJdLCJleHAiOjE1MzYzNTg0MTQsImlhdCI6MTUzNjE0MjQxNH0.ksftPuP3Vn5WugXz6dpGsvmS-P25ubW8iJdBuWrBA6D3qek_VVCbMP2mNvb-1dQ0SYb6-r8AY5BuGgn3Kx7dYN1cp2ujw3kzfJCrK5yI0cX4ZZ4fe6d3ZVdmrCgmedQUgiMSWUAqDPCZD6PzuDuHxgntobrEZxvExMpNEN-Xqp8LoQ1mUJNLrMZazQL3h0pE32qQfV1W7VbnirOVedIf_C7Uv1_zl3sflvEnK1V79y6iA3bSmcRq0-O2r5HxgKkvfRB_iBWz8oZEP-AOYtNh84ZHk54NtWct7c__9OQVZ4kGqo4Tv39r3c6gWi9aRBpu7Qwk3pPhyc4KgVhV3JAgrQ",
    "token_type": "Bearer",
    "expires_in": 216000
}
```

When calling GET /api/v1/person include the `access_token` as in an HTTP Header:

```
Authorization:Bearer eyJraWQiOiI5YzRlNDVmZS01ODhiLTQ5N2QtODY0MS01NjEyODAzYTI0ZTQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJJdHNNZSIsImlzcyI6Imh0dHBzOi8vdG9rZW5pc3N1ZXIue2NsdXN0ZXJ9Lmt5bWEuY3giLCJzY29wZSI6WyJwZXJzb25fd3JpdGUiLCJwZXJzb25fcmVhZCJdLCJleHAiOjE1MzYzNTg0MTQsImlhdCI6MTUzNjE0MjQxNH0.ksftPuP3Vn5WugXz6dpGsvmS-P25ubW8iJdBuWrBA6D3qek_VVCbMP2mNvb-1dQ0SYb6-r8AY5BuGgn3Kx7dYN1cp2ujw3kzfJCrK5yI0cX4ZZ4fe6d3ZVdmrCgmedQUgiMSWUAqDPCZD6PzuDuHxgntobrEZxvExMpNEN-Xqp8LoQ1mUJNLrMZazQL3h0pE32qQfV1W7VbnirOVedIf_C7Uv1_zl3sflvEnK1V79y6iA3bSmcRq0-O2r5HxgKkvfRB_iBWz8oZEP-AOYtNh84ZHk54NtWct7c__9OQVZ4kGqo4Tv39r3c6gWi9aRBpu7Qwk3pPhyc4KgVhV3JAgrQ
```

Now your call should work as expected. If not, revisit the configuration.

For those familiar with JWT Tokens, you might wonder that there was no key exchanged. This is on purpose. Our Spring Application actually ignores the whole signature checking piece. It basically trusts every token coming in its way. It is Kyma's job to ensure calls with invalid/no tokens (broken signature, invalid issuer, etc.) are not forwarded to our service. The Spring Application is configured as shown below to achieve this behavior ( `com.sap.demo.security.OAuth2ResourceServerConfig.java` Other security things are also done in package `com.sap.demo.security`):

```
@Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
       
        // Signature is checked in Kyma API / Istio, hence we do not need to bother
        // retrieving the key, decoding it and putting it in here
        converter.setVerifier(new SignatureVerifier() {			
			@Override
			public String algorithm() {
				return "RS256";			
				// If your token uses a different Algorithm, depict it here
			}
			
			@Override
			public void verify(byte[] content, byte[] signature) {
				// Do nothing, token is already verified by istio	
			}
		});
        
        converter.setJwtClaimsSetVerifier(jwtClaimsSetVerifier());

        return converter;
    }
```

Credits go to @eugenp and https://www.baeldung.com/spring-security-oauth-jwt for a lot of the code and inspiration.


### Optional: Create IDP Preset

Kyma allows to persist configuration of trusted Token providers and re-use it upon API Exposure (In the UI). They basically serve as copy templates when exposing a new API. To do that, go to the home screen select "IDP Presets" and choose "Create Preset". Now you can provide Name, Issuer and JWKS URI.

* Name is just free Text (e.g. "my-dummy-issuer")
* Issuer must be depicted as "iss" in your JWT Token. Kyma only accepts a URL or an email address. (see https://istio.io/docs/reference/config/istio.authentication.v1alpha1/). Recommendation is https://{hostname}/
* JWKS URI must match https://{hostname}/jwk

See sample below:

![IDP Preset Screenshot](images/idppreset.png)

### Secure your API

To have Kyma/Istio deal with all the complexities of validating JWT Tokens, we need to change our definition of the API a little. Basically all we need to do, is to adapt the below snippet to your own environment:

```
apiVersion: gateway.kyma-project.io/v1alpha2
kind: Api
metadata:
  name: personservice
  labels:
     app: personservice
spec:
  authentication: 
    - type: JWT
      jwt:
        issuer: "{issuer}"
        jwksUri: "{jwk_url}"
  hostname: personservice.{yourhost}
  service:
    name: personservice
    port: 8080
```

To get there issue: `kubectl edit api personservice -n personservice`

Then adapt the `authentication` section to match the below sample (replacing parts in curly braces `{}`):

```
  authentication: 
    - type: JWT
      jwt:
        issuer: "{issuer}"
        jwksUri: "{jwk_url}"
```

Mind the values you have configured in [Deploy OAuth2 Authorization Server](#deploy-oauth2-authorization-server). Also mind the restrictions for issuer mentioned in (Optional: Create IDP Preset)[optional:-create-idp-preset]. After that you can apply it:  
`kubectl apply -n personservice -f my-protected-personservice-api.yaml`

Alternatively you can also use `kubectl edit api personservice -n personservice`

After that (it might take a while to reflect in the system) you should get an `Origin authentication failed.` message when invoking the service. 

### Security For Lambdas (Not on Minikube)

**If you are running this locally you will have issues with failed DNS lookups of the lambda function. There are probably ways to fix this, but they are not described here. If you are running on a cluster where APIs have public DNS entries, you can continue.**

When you create a new person, your Lambda will fail. To fix it, we need to adapt the registration file. Specifically we need to provide the OAuth2 configuration:

```
"api": {
    "targetUrl": "https://personservice.<kymahost>/",
    "spec":{swagger is here, but removed for readability}
    "credentials": {
      "oauth": {
        "url": "https://<tokenservicehost>/oauth2/token/query?expirationDurationMinutes=3600&includeIssuedAt=true&issuer=<issuer>&notBeforeMinutesInThePast=0&scopes=person_read%2Cperson_write&subject=itsme",
        "clientId": "doesntmatter",
        "clientSecret": "doesntmatter"
    }    
}

```

Replace:

* `<tokenservicehost/>`: host name configured for token service
* `<issuer/>`: issuer configured in API
* `<kymahost/>`: hostname of kyma instance

Then issue the following commands:

```
kubectl delete configmap -n personservice registrationfile
kubectl create configmap -n personservice registrationfile --from-file=registrationfile.json -n personservice
kubectl delete pod -n personservice -l app=personservice

```

After that again issue a POST against `/api/v1/applicationconnector/registration`. Before doing so, acquire a new JWT Token as described before and supply it in an Authorization header. Now your registration should be updated and all lambda calls will acquire a token from the service and supply it for the outbound calls. 

### Test the Service

To test the service you will now need a REST client like curl or postman, as even the swagger ui requires a JWT token (you could also get there through a browser pluging, but again beyond the scope of this). We have already seen that requests without JWT Token are rejected. Now we want to get a valid JWT Token and make the call. Therefore we need to make the following call to get one:

```
POST https://{tokenservicehost}/oauth2/token/body

{
	"subject": "Itsme",
	"issuer":"https://{tokenservicehost}/",
	"expirationDurationMinutes": 3600,
	"scopes": ["person_read"],
	"includeIssuedAt": true
}
```
Make sure to replace the 2 occurrences of `{tokenservicehost}`. 

The resulting token will then be used for a GET Call to https://{personservicehost}/api/v1/person which should go through as expected.

Now make a POST call to the same URL with the below JSon: 

```
{
   "id":"5b8fbe690e66110001f267e2",
   "firstName":"John",
   "lastName":"Doe",
   "streetAddress":"Nymphenburger Str.",
   "houseNumber":"86",
   "zip":"80636",
   "city":"Muenchen",
   "extensionFields":{
      "countryIso2":"DE"
   }
}
```

This should now return an unauthorized response telling you that you are missing scope `person_write`.

Through that you have seen how Kyma and your application complement each other with regards to security.

## Operate your Service: Make it Self-Healing

### Intro 
Kubernetes (which Kyma is based on) is based on the assumption that you as a developer declare a target state and kubernetes manages the way to get there. This means that e.g. you specify that your deployment should consist of 2 instances of personservice and kubernetes will ensure that there are always (if resources permit) 2 instances running. Sometimes however we need to get more granular as your service might appear running but is actually hanging and hence damaged, or it is simply to busy to serve traffic. This is where the Self-Healing which we are enabling in this section kicks in.

### Preparation
In order to free-up resources in your cluster, we need to change a couple of things. Basically we need to go back to an older version of our deployment which does not require a redis cache anymore or authentication and authorization. To do that go to your service catalog and first unbind your redis service instance from personservice and then delete the service instance. 

Also delete the OAuth 2 Service from your cluster: `kubectl delete -f kubernetes-kyma.yaml -n personservice` or if you used the standard manifest `kubectl delete -n personservice -f https://raw.githubusercontent.com/akrausesap/jwt_issuer/master/kubernetes-kyma.yaml`.


The `kubectl get pods -n personservice` should now yield an output comparable to the one below (ignoring whether the personservice is actually running or damaged):

```
NAME                                      READY     STATUS    RESTARTS   AGE
first-mongo-mongodb-d4575cb7d-jjk2w       1/1       Running   0          1d
mark-duplicate-persons-84b974f549-8kwn9   2/2       Running   0          9m
personservice-745849798f-w4f48            2/2       Running   0          32s
```

Also ensure that Authentication is disabled on the personservice API. This can be ensured by removing ny traces to:

```
spec:
    authentication:
    - jwt:
        issuer: https://tokenissuer.cluster.kyma.cx
        jwksUri: https://tokenissuer.cluster.kyma.cx/jwk
      type: JWT
``` 

This can be done either in the UI or using `kubectl edit  API -n personservice personservice` or alternatively the UI.

Also if you updated the registration file ([Security For Lambdas (Not on Minikube)](#security-for-lambdas-not-on-minikube)), these changes need to be reversed. To do so, ensure that `registration/registrationfile.json` again looks like below (Credentials removed):

```
"api": {
    "targetUrl": "https://personservice.<kymahost>/",
    "spec":{swagger is here, but removed for readability}
}
```

To update the system, do the following:


```
cd registration
kubectl delete configmap -n personservice registrationfile
kubectl create configmap -n personservice registrationfile --from-file=registrationfile.json -n personservice
kubectl delete pod -n personservice -l app=personservice

```

After that again issue a POST against `/api/v1/applicationconnector/registration`.


### Determining whether your service is alive 

Spring Boot comes with a functionality called actuator. This lets you control and determine the status of the spring application. For the person service we have activated it and exposed it as REST API on port 8081. This nicely separates it from the externally exposed api and keeps it reachable only from within the cluster. The key endpoint for determining service health is the /actuator/health resource. It will return "UP" (HTTP 200) or "DOWN" (HTTP 503). Now we are going to exploit this in kubernetes.

Basically we will make the Kubelet invoke this actuator periodically and based on the result 200 or 503 determine whether the service is up or down. If the service is down it should dispose it and start a new one to get back to the target state. To do so we need to look the deployment spec (`mongo-kubernetes-cluster5.yaml` or `mongo-kubernetes-local5.yaml`) and find the section for the livenessProbe:

```
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 8081
          name: actuatorhttp
        livenessProbe:
          httpGet:
              path: /actuator/health
              port: 8081
          initialDelaySeconds: 60
          periodSeconds: 60
          failureThreshold: 3  
                    
```

Under ports we have exposed port 8081 with the name actuatorhttp. We have subsequently defined `livenessProbe` which periodically (every 60 seconds) makes a GET request to /actuator/health. If it fails 3 times in a row the container will be disposed and recreated.

### Determining whether your service is ready to serve traffic

Sometimes services are simply just too busy to serve traffic (e.g. whne executing batch loads, etc.). This is where kubernetes offers to remove a service from loadbalancing until it reports back. To support this `DemoReadinessIndicator.java` was implemented. It is a custom actuator that reports the readiness status. HTTP 200 means ready and HTTP 503 means not ready.

To periodically invoke this endpoint the following section was added to the deployment manifest (`mongo-kubernetes-cluster5.yaml` or `mongo-kubernetes-local5.yaml`):

```
        readinessProbe:
          httpGet:
              path: /actuator/ready
              port: 8081
          periodSeconds: 30
          initialDelaySeconds: 20
          failureThreshold: 1
          successThreshold: 2  
```

We have defined `readinessProbe` which periodically (every 30 seconds) makes a GET request to /actuator/ready. If it fails 1 time the pod will be excluded from loadbalancing. Only after 2 successful calls it will be again used for loadbalancing.

### Deploying to Kyma

In order for the personservice to be self-healing, `mongo-kubernetes-local5.yaml` or `mongo-kubernetes-cluster5.yaml` have been adapted. However you still need to replace the values depicted with `#changeme` to cater to your environment. 

* Local:

`kubectl apply -f mongo-kubernetes-local5.yaml -n personservice`  


* Cluster:

`kubectl apply -f mongo-kubernetes-cluster5.yaml -n personservice` 

The `kubectl get pods -n personservice` should after some time yield an output comparable to the one below:

```
NAME                                      READY     STATUS        RESTARTS   AGE
first-mongo-mongodb-d4575cb7d-jjk2w       1/1       Running       0          1d
mark-duplicate-persons-84b974f549-8kwn9   2/2       Running       0          17m
personservice-7d6657fdc-g4mzs             2/2       Running       0          1m
personservice-86c54fdc5b-2dt8l            2/2       Running       0          3m
```

### Testing

First of all you should verify that both pods are serving traffic to you. In order to do that, call the `/api/v1/person` endpoint (GET) a couple of times and in the responses check the header `x-serving-host`. It should change frequently and give you back the pod names.

Now we can start breaking the service. To see the results we first of all issue the following command `kubectl get pods -n personservice -w`. It will automatically refresh the status of the pods on the commandline.

To make one of the services appear not alive issue a POST Request to `/api/v1/monitoring/health?isUp=false`. This will show the following picture after some time (around 3 minutes):

```
first-mongo-mongodb-b98bd6f8b-7dv5q   1/1       Running   0          8h
personservice-755f847c9d-x5xks        2/2       Running   0          1h
personservice-755f847c9d-x5z29        2/2       Running   0          1h
personservice-755f847c9d-x5z29   1/2       Running   1         1h

```

Now you can in a separate terminal window execute the command `kubectl describe pod -n personservice <podname>` for the pod that was failing. In the event log you will see the following picture:

```
Events:
  Type     Reason     Age               From                           Message
  ----     ------     ----              ----                           -------
  Warning  Unhealthy  26s (x6 over 1h)  kubelet, k8s-agent-27799012-2  Liveness probe failed: HTTP probe failed with statuscode: 503
  Normal   Pulling    22s (x3 over 1h)  kubelet, k8s-agent-27799012-2  pulling image "personservice/mongokubernetes:1.0.0"
  Normal   Killing    22s (x2 over 1h)  kubelet, k8s-agent-27799012-2  Killing container with id docker://personservice:Container failed liveness probe.. Container will be killed and recreated.
  Normal   Pulled     21s (x3 over 1h)  kubelet, k8s-agent-27799012-2  Successfully pulled image "personservice/mongokubernetes:1.0.0"
  Normal   Created    21s (x3 over 1h)  kubelet, k8s-agent-27799012-2  Created container
  Normal   Started    21s (x3 over 1h)  kubelet, k8s-agent-27799012-2  Started container
```

It basically shows that kubernetes is recreating the failing container. After that the container is back alive.

Now you can issue a POST request to /api/v1/monitoring/readiness?isReady=60. This will make the readinessProbe fail for 60 seconds. You will again see this in your pod monitor. Now you can in a separate terminal window execute the command `kubectl describe pod -n personservice <podname>` for the pod that was failing. In the event log you will see the following picture:

```
  Type     Reason     Age               From                           Message
  ----     ------     ----              ----                           -------
  Normal   Created    4m (x3 over 1h)   kubelet, k8s-agent-27799012-2  Created container
  Normal   Started    4m (x3 over 1h)   kubelet, k8s-agent-27799012-2  Started container
  Warning  Unhealthy  12s (x4 over 4m)  kubelet, k8s-agent-27799012-2  Readiness probe failed: HTTP probe failed with statuscode: 503
```

Also all API calls to `/api/v1/person` endpoint (GET) will have the same value for `x-serving-host`. Hence the other pod was sucessfully excluded from loadbalancing. 

## Operate your Service: Traces and Logs

### Intro
Kyma comes with tracing and logging support through Kubernetes, Istio and Jaeger. Tracing is mainly influenced by Istio (https://istio.io/docs/tasks/telemetry/distributed-tracing/) and Jaeger (https://www.jaegertracing.io/). Tracing enables you to corelate requests as they travel from service to service. All you need to do is propagate a set of tracing headers. The rest is taken care of by Istio and Jaeger.

```
   Client Span                                                Server Span
┌──────────────────┐                                       ┌──────────────────┐
│                  │                                       │                  │
│   TraceContext   │           Http Request Headers        │   TraceContext   │
│ ┌──────────────┐ │          ┌───────────────────┐        │ ┌──────────────┐ │
│ │ TraceId      │ │          │ X─B3─TraceId      │        │ │ TraceId      │ │
│ │              │ │          │                   │        │ │              │ │
│ │ ParentSpanId │ │ Extract  │ X─B3─ParentSpanId │ Inject │ │ ParentSpanId │ │
│ │              ├─┼─────────>│                   ├────────┼>│              │ │
│ │ SpanId       │ │          │ X─B3─SpanId       │        │ │ SpanId       │ │
│ │              │ │          │                   │        │ │              │ │
│ │ Sampled      │ │          │ X─B3─Sampled      │        │ │ Sampled      │ │
│ └──────────────┘ │          └───────────────────┘        │ └──────────────┘ │
│                  │                                       │                  │
└──────────────────┘                                       └──────────────────┘
```

Logs are just written to stdout so that they are accessible through `kubectl logs` and log aggregation solutions such as fluentd (not included in kyma).

To embed all of this into our Spring Boot Application, no coding is necessary. All we need to do is embed Spring Cloud Sleuth (https://cloud.spring.io/spring-cloud-sleuth/):

```
<dependency> 
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>
```

This will ensure our App:

* Extracts and propagates Headers
* Decorates logs with TraceId, ParentSpanId and SpanId

In our Spring app logging uses SLF4J and is mainly based on the `LoggingAspect.java` which uses Aspect Oriented Programming to proxy and log every call to a class within the package hierarchy.

### Testing Tracing

To test the tracing you need to launch the Jaeger UI. Details can be found under https://kyma-project.io/docs/latest/components/tracing. You can access the Jaeger UI either locally at `https://jaeger.kyma.local` or on a cluster at `https://jaeger.{domain-of-kyma-cluster}`.

Now we send a simple GET to /api/v1/person. In Jaeger we will make the following selections:

* Service: personservice
* Limit Results: 20

![Tracing](images/tracing1.png)

Selecting the relevant record we will see the following picture (highlighting the Span created by our Spring Boot Application with application level data):

![Tracing](images/tracing2.png)


This shows a simple request which is answered by the Personservice directly without propagation. It enters through Istio. Mixer ensures that all rules are followed and then the person service answers the request. The Application adds further tags to it, to make tracing more verbose:

![Tracing](images/tracing6.png)

Now we want to get more advanced and change a person which triggers a request to the events endpoint described in [Connect your Service to Kyma as Extension Platform](#connect-your-service-to-kyma-as-extension-platform). It will now also show up in the trace, but be more complex, as there is now also an outbound call to the events API (PATCH /api/v1/person/{id}).

![Tracing](images/tracing4.png)

Trace shows the following:

![Tracing](images/tracing3.png)

For those of you who really want to know what is going on, you can also create a new person and trace it all the way through the Lambda processing. However this requires your lambda to be properly hooked up to the service as described in [Extend your Person Service](#extend-your-person-service). Then you would see something along the lines of:

![Tracing](images/tracing5.png)

### Testing Logging

Now since we know how the request was going through the Service Mesh, we also want to inspect what was happening within the App. Therefore we need to look into the logs. Fortunately Spring Cloud Sleuth has decorated them for us.

To get there we open one of the traces again and extract the trace id and the pod it was executed on (Example is based on GET /api/v1/person)..

![Getting the Log](images/gettingthelog1.png))

Under View Options we select Trace JSON. This will provide access to the trace ID:

![Getting the Log](images/gettingthelog2.png)

For the pod we found we will issue the following kubectl command to print the logs into a text file:  
`kubectl logs -n personservice -l app=personservice -c personservice > logs.txt`

Based on the trace ID we can now search the logfile and see what happened inside the pod:

![Getting the Log](images/gettingthelog3.png) 

To make this more simple, Kyma comes with Loki and Promtail (https://kyma-project.io/docs/latest/components/logging). These tools help to aggregate logs within the cluster. We can use Loki in two ways via the terminal or UI.

**Be careful, log collection runs asynchronously and hence there might be a small delay.**

#### Option 1: Loki Query via Terminal  
If you choose the terminal option, you directly query the logging service in Kyma.
1. `kubectl port-forward -n kyma-system svc/logging 3100:3100`
2. Then issue the following command with your trace ID to fetch the logs from Loki:  
`curl -G 'http://localhost:3100/api/prom/query' --data-urlencode 'query={container_name="personservice"}' --data-urlencode 'limit=1000' --data-urlencode 'regexp=TRACE' | grep <traceID>`

Then you will see which parts of the logs contain your trace ID.  

![Getting the Log](images/gettingthelog4.png)

#### Option 2: Loki Query via UI
Kyma also provides an UI to query the Loki service in an easier way. Go to the Kyma home page and select "Logs" on the sidebar. There you can enter the following query to get all the traces:  
`{app="personservice", container_name="personservice"} (TRACE)`  

![Getting the Log](images/gettingthelog5.png)

This gives you all the log entries on the level `TRACE`. You can now search within these in your browser:

![Getting the Log](images/gettingthelog6.png)

## Operate your Service: Metrics

### Intro

Kyma comes with a Prometheus Operator included. This means you can instrument your Services and scrape metrics as described in https://kyma-project.io/docs/latest/components/monitoring. In order to instrument the Person Service and get application level metrics I have added the following dependency to the Maven POM file:

```
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Furthermore I have added the following lines of code to the `PersonServiceDefault` class:

```
@Autowired
public PersonServiceDefault(MeterRegistry registry, PersonRepository repository) {
  this.repository = repository;
  Gauge.builder("personservice.persistency.repository.size", repository, PersonRepository::count)
      .tag("Repository", "Persons").register(registry);
  
}
```

These add a metric called `personservice_persistency_repository_size` to the `/actuator/prometheus` endpoint so that we can plot them in a chart using Grafana.

### Collecting Metrics in Prometheus

In oder to be able to collect the metrics from Prometheus the following Service was added to `mongo-kubernetes-local6.yaml` and `mongo-kubernetes-cluster6.yaml`:

```
apiVersion: v1
kind: Service
metadata:
  labels:    
     app: personservice
     version: "1.0.0"
  annotations:
     auth.istio.io/80: NONE
  name: personservice-actuator
spec:
  ports:
  - name: actuatorhttp
    port: 8081
  selector:
    app: personservice
    version: "1.0.0"
  type: ClusterIP
```
Deploy them using   
`kubectl apply -n personservice -f mongo-kubernetes-cluster6.yaml` or  
`kubectl apply -n personservice -f mongo-kubernetes-local6.yaml`

Now you can configure Prometheus to scrape the metrics. To do so you need to create a resource of type ServiceMonitor. This needs to be part of the `kyma-system` namespace:

```
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: personservicekubernetes
  labels:
    app: personservice
    prometheus: monitoring #links to kyma prometheus instance
spec:
  jobLabel: "Personservice"
  selector:
    matchLabels:
      app: personservice # label selector for service
  namespaceSelector:
    matchNames:
    - personservice # links to person service namespace for metric collection
  endpoints:
  - port: actuatorhttp # port name in service
    path: "/actuator/prometheus" # enpoint of spring metrics actuator
```

To create this resource issue: `kubectl apply -f service-monitor.yaml -n kyma-system`

Now you can check whether it is working in Prometheus. To do so you need to expose prometheus on your localhost using `kubectl port-forward -n kyma-system svc/prometheus-operated 9090:9090`. Now you can open http://localhost:9090/targets in a browser. You should find a target like in the screenshot below:

![Prometheus](images/prometheus1.png)

Furthermore you can go to `http://localhost:9090/graph` and issue the following PromQl statement as a verification  `up{job="personservice-actuator"}`. This will should yield something along these lines:

![Prometheus](images/prometheus2.png)

You besides creating dashboards, you can also use the collected metrics for alerting. This will not be depicted here but is rather straight forward.

### Creating a Dashboard

For Dashboarding we can use Grafana. Grafana will visualize the following PromQL (https://prometheus.io/docs/prometheus/latest/querying/basics/) queries:

* container_memory_usage_bytes{namespace="personservice",container_name="personservice"}/1000000 ==> Memory usage of the Person Service Pods in Megabytes
* sum(up{job="personservice-actuator"}) ==> Number of instances of the person service
* max(personservice_persistency_repository_size{Repository="Persons"}) ==> Our custom metric for providing the number of Person records in the Mongo DB
* sum by (job,method) (rate(http_server_requests_seconds_count{job="personservice-actuator"}[1m])*60) ==> request rate per Minute and HTTP method

Grafana is accessed under `https://grafana.{clusterhost}/login` or from `https://console.{clusterhost}/` Administration -> Stats & Metrics. After Login you should see the below picture and go to "Import":


![Dashboards](images/grafana1.png)

Now upload `Person_Service_Dashboard.json`. 

![Dashboards](images/grafana2.png)

This will require to wire the dashboard to a datasource called `Prometheus`:

![Dashboards](images/grafana3.png)

After doing so your dashboard will look as follows:

![Dashboards](images/grafana4.png)