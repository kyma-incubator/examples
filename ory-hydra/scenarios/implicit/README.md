# Accessing Lambda functions using ORY Hydra with Dex integration example

## Overview

This example illustrates how to secure lambda functions with Id Tokens from Hydra using two components of Kyma platform:
- [Hydra](https://www.ory.sh/docs/hydra/) - OAuth 2.0 and OpenID Connect Server.
- [Dex](https://github.com/dexidp/dex) - an identity service that uses OpenID Connect to drive authentication for other apps.

In this scenario, Hydra Server is responsible for issuing Id Tokens and Dex is used as a login provider. Users can authenticate by any method that is configured in Dex.

The scenario consists of following steps:

- Installation of Kyma with `ory`  and `hydra-dex` components.
- Creating a Lambda function protected with Authn policy that points to the Hydra JWKS endpoint.
- Creating a Hydra Oauth2 client with ability to perform Implicit Grant requests with **openid** scope.
- Fetching an Id Token from Hydra with Dex as login provider.
- Calling target Lambda with the Id Token issued by Hydra.

## Prerequisites
- cURL

## Steps

### Install necessary components
- read instructions from **chart/hydra-dex** chart.
- Install the **chart/hydra-dex** chart.

### Create OpenID Connect client

* Replace `<domainName>` with the proper domain name of your ingress gateway for cluster installations or **kyma.local** for local installation.
  Run:  `export DOMAIN_NAME=<domainName>`

* Run  `curl -ik -X POST "https://oauth2-admin.$DOMAIN_NAME/clients" -d '{"grant_types":["implicit"], "response_types":["id_token"], "scope":"openid", "redirect_uris":["http://localhost:8080/callback"], "client_id":"implicit-client", "client_secret":"some-secret"}'`

_Note: The client is using `http://localhost:8080/callback` redirect URI. This doesn't have to be an URL of any real application. Since OpenID Connect Implict flow is browser-based, it's only important to have a valid URL here. The final redirect of the flow contains the token. In case the application does not exist, the browser will report a non-existing address, but the token will be present in the address bar._

### Fetch an Id Token
* create an OpenID Connect token request: `echo "http://oauth2.$DOMAIN_NAME/oauth2/auth?client_id=implicit-client&response_type=id_token&scope=openid&state=8230b269ffa679e9c662cd10e1f1b145&redirect_uri=http://localhost:8080/callback&nonce=$(date | md5)"`
* Copy the URL into your browser
* Authenticate. After successful authentication, you should be redirected to the address that looks like this: `http://localhost:8080/callback#id_token=eyJ...&state=8230b269ffa679e9c662cd10e1f1b145`
* Copy the **id_token** value from the browser address bar. It is long!
* `export JWT=<copied id_token value>`

### Create a lambda function

 - get valid Hydra **Issuer**: `echo $(kubectl get deployments/ory-hydra-oauth2 -n kyma-system -o go-template='{{range (index .spec.template.spec.containers 0).env}}{{if eq .name "OAUTH2_ISSUER_URL"}}{{.value}}{{end}}{{end}}')`
 - the valid Hydra **JWKS URI** for in-cluster calls is:`http://ory-hydra-oauth2.kyma-system.svc.cluster.local/.well-known/jwks.json`
 - Create a lambda function and expose it as HTTPS service. Provide valid **Issuer** and **JWKS URI**. Pay attention to the **Issuer** value, it has to match exactly.

 _Note: Don't create a lambda before getting the token.
  It's because the Hydra creates keys lazily. The key is generated when the first Id Token is issued.
  If you create the Lambda before that, Istio can't find any keys at JWKS endpoint and the Labmda is inaccessible._

### Call the lambda with the token

* Export the URL of your Lambda function.  You can copy the URL from the Kyma console. An example: `export LAMBDA_URL="https://demo-hydra-production.kyma.local/"`
* Ensure Lambda host is added to your **/etc/hosts** for local installations
* Call the lambda: `curl -ik -X GET "${LAMBDA_URL}/" -H "Authorization: Bearer ${JWT}"`
* If the call to lambda is successful, try to change the Id Token by adding a random letter to it. Verify the call to the labda is failing now. This proves Id Token validation is working as expected.

## Troubleshooting

- If you see: **Origin authentication failed.** message, check isito-pilot logs for the following entries:
  `warn    Failed to fetch jwt public key from "http://ory-hydra-oauth2.kyma-system.svc.cluster.local/.well-known/jwks.json"`
  Istio-pilot sometimes has problems in accessing jwks endpoint to perform JWT verification. Try one of the following solutions:

  - decode the Hydra JWT and ensure it's nonce is the same as the one you provided at the token request. Perhaps you're using an invalid token? Re-create the token and try again with a new one.
  - inspect the Authentication Policy for the Lambda. The name of the policy matches the name of the lambda, and it's created in the same namespace as the lambda. Are the issuer and jwksUri fields valid? Try to re-create the policy (backup it first).
  - restarting Istio-pilot. Note that after restart, not all envoy proxies are updated immediately. Retry for at least two minutes before giving up.
  - try to call the Hydra JWKS URI from within the cluster using a pod with bash and curl installed. Perhaps there's some networking issue not directly related to token validation.

