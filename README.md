# A sample Application for running Spring Boot on Kubernetes/Kyma with Mongodb

## Kyma

This App runs on Kyma (https://kyma-project.io/; for local installation see: https://github.com/kyma-project/kyma/tree/master/installation). 

## Mongo DB

To deploy Mongodb use Helm (https://helm.sh/). To install helm do the following:

Innitialize Helm (if not already done, client-only option as kyma already comes with tiller installed):
`helm init --client-only` 

Then deploy Mongo DB

`helm install --name first-mongo   --set persistence.size=2Gi,    stable/mongodb --namespace <your namespace>`

## Java Build

Project is built using: mvn clean package or mvn clean install. It uses jib (https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin)to build and push to a docker registry (which does not require a local docker install). You can use the following maven properties to adapt to your local installation: 

* project.version: Tag that will be assigned to docker image 
* jib.version: Version of the jib plugin that will be used
* docker.repositoryname: Docker repository that the image will be published to
* jib.credentialhelper: docker credential helper that will be used to acquire docker hub credentials (see: https://docs.docker.com/engine/reference/commandline/login/ heading: "Credential helper protocol")

## Docker Credential Helper Setup

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


## Deploy to Kyma

Deployment to kyma requires to upload a configmap and also a kubernetes deployment and a service.
Before deploying the configmap you must update the values contained based on the installation of your mongo db:

mongodb_host: first-mongo-mongodb.*d046471*.svc.cluster.local

 The below commands do this: 

```
kubectl apply -f mongo-kubernetes-configmap.yaml -n <your namespace>
kubectl apply -f mongo-kubernetes.yaml -n <your namespace>
```

`mongo-kubernetes.yaml` creates the following Kubernetes objects:

* Kubernetes Deployment for the Spring App (including Istio setup)
* Kubernetes Service pointing towards the pods created by the Deployment
* Kyma API exposing the service through an Istio Ingress

**mongo-kubernetes-configmap.yaml must be adapted to you mongo db deployment.**

## Try out on Kyma

After deployyment you can access the swagger documentation under https://{kymahost}/swagger-ui.html. This also allows you to try it out. 

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
* PATCH /api/v1/person/{personid}: Update person with id `personid` with the following data:
```
{
	"firstName":"Jack",
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

* POST /api/v1/person/search: Search for persons matching the following criteria:
```
{
	"city":"Muenchen",
	"extensionFields":{
		"countryIso2":"De"
	}
}
``` 




