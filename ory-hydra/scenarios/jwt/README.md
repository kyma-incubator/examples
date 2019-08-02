# Example for Oathkeeper JWT Authentication with Dex ID Token

## Overview

This example illustrates how to secure resources and lambda functions with JWT issued by Dex with the help of the following [ORY](https://www.ory.sh/) components:
- [Oathkeeper](https://www.ory.sh/docs/oathkeeper/) - Identity and Access Proxy.
- [Maester](https://github.com/ory/oathkeeper-maester) - A Kubernetes Controller that manages Oathkeeper rules using Custom Resources.

In this scenario, ORY environment is responsible for handling incoming HTTP requests based on a set of user-specified access rules. Every request is processed in the following manner:

- ORY Oatkeeper finds an access rule that matches the requested URL
- The access rule uses its `jwt` authenticator to validate the access token from the Authorization Header
- jwt authenticator validates the token using endpoints configured in `jwks_urls` propery in  Oathkeeper configuration
- If the token is valid, the request is forwarded to the target service provided in the access rule.

To learn more about Oathkeeper's access rules, see the official [Oathkeeper documentation](https://www.ory.sh/docs/oathkeeper/api-access-rules).

## Prerequisites

- Kyma instance with the `ory` component installed.
- cURL

## Installation

1. Export necessary variables

You'll need an Id Token (JWT) issued by Dex in your Kyma installation.

```
export DOMAIN=<YOUR_DOMAIN>
export JWT=<YOUR_DEX_JWT>
```

2. Create a sample function:
```
kubectl get ns stage || kubectl create ns stage
kubectl apply -f jwt-lambda.yaml
```

3. Create a virtual service.
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: jwt-lambda-proxy
  namespace: kyma-system
spec:
  gateways:
  - kyma-gateway
  hosts:
  - jwt-lambda-proxy.$DOMAIN
  http:
  - match:
    - uri:
        regex: /.*
    route:
    - destination:
        host: ory-oathkeeper-proxy
        port:
          number: 4455
EOF
```
If you have installed Kyma on minikube, add folowing line to minikube ip in `/etc/hosts` file:
```
jwt-lambda-proxy.kyma.local
```

4. Create Oathkeeper Rule with `jwt` Authenticator:

```
cat <<EOF | kubectl apply -f -
apiVersion: oathkeeper.ory.sh/v1alpha1
kind: Rule
metadata:
  name: jwt-lambda
  namespace: default
spec:
  description: lambda access with JWT
  upstream:
    url: http://jwt-lambda.stage.svc.cluster.local:8080
  match:
    methods: ["GET"]
    url: <http|https>://jwt-lambda-proxy.$DOMAIN/lambda
  authenticators:
    - handler: jwt
      config:
        trusted_issuers:
        - https://dex.kyma.local
  authorizer:
    handler: allow
EOF
```

5. Call the function

```
curl -ik https://jwt-lambda-proxy.$DOMAIN/lambda -H "Authorization: Bearer $JWT"
```
Expected response: 200 OK

