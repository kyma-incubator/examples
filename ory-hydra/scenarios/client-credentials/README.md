# Oauth2 Token Introspection example

## Overview

This example illustrates how to secure resources and lambda functions using the following [ORY](https://www.ory.sh/) components:
- [Hydra](https://www.ory.sh/docs/hydra/) - OAuth 2.0 and OpenID Connect Server.
- [Oathkeeper](https://www.ory.sh/docs/oathkeeper/) - Identity and Access Proxy.
- [Maester](https://github.com/ory/oathkeeper-k8s-controller) - A Kubernetes Controller that manages Oathkeeper rules using Custom Resources.

In this scenario, ORY environment is responsible for handling incoming HTTP requests based on a set of user-specified access rules. Every request is processed in the following manner:

- ORY Oatkeeper finds an access rule that matches the requested URL
- The access rule uses its `oauth2_introspection` authenticator to extract the access token from the Authorization Header
- The token is forwarded to Hydra's OAuth 2.0 Token Introspection endpoint for liveness and scope validation
- If the token is valid, the request is forwarded to the target service provided in the access rule.

To learn more about Oathkeeper's access rules, see the official [Oathkeeper documentation](https://www.ory.sh/docs/oathkeeper/api-access-rules).

## Prerequisites

- Kyma instance with the `ory` component installed.
- cURL

## Installation

This section demonstrates how to set up an Oauth2 client with given scopes.

### Setup an Oauth2 client

1. Create an Oauth2 client. For the purpose of this example, we define two scopes: `read` and `write`

```
export CLIENT_ID=<YOUR_CLIENT_ID>
export CLIENT_SECRET=<YOUR_CLIENT_SECRET>
export DOMAIN=<YOUR_DOMAIN>
curl -ik -X POST "https://oauth2-admin.$DOMAIN/clients" -d '{"grant_types":["client_credentials"], "client_id":"'$CLIENT_ID'", "client_secret":"'$CLIENT_SECRET'", "scope":"read write"}'
```

2. Encode your client credentials:
```
export ENCODED_CREDENTIALS=$(echo -n "$CLIENT_ID:$CLIENT_SECRET" | base64)
```

3. Issue a token with:
- `read` scope
```
curl -ik -X POST "https://oauth2.$DOMAIN/oauth2/token" -H "Authorization: Basic $ENCODED_CREDENTIALS" -F "grant_type=client_credentials" -F "scope=read"
```

- `write` scope:
```
curl -ik -X POST "https://oauth2.$DOMAIN/oauth2/token" -H "Authorization: Basic $ENCODED_CREDENTIALS" -F "grant_type=client_credentials" -F "scope=write"
```

4. Save the returned access tokens:
```
export ACCESS_TOKEN_READ=<READ_ACCESS_TOKEN>
export ACCESS_TOKEN_WRITE=<WRITE_ACCESS_TOKEN>
```

## Securing resources

<div tabs>

  <details>
  <summary>
  Secure sample HttpBin endpoints
  </summary>

1. Create an HttpBin instance:
```
kubectl apply -f https://raw.githubusercontent.com/istio/istio/master/samples/httpbin/httpbin.yaml
```

2. Create a virtual service.
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: httpbin-proxy
  namespace: kyma-system
spec:
  gateways:
  - kyma-gateway
  hosts:
  - httpbin-proxy.$DOMAIN
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
If you have installed Kyma on minikube, add folowing name to an entry with minikube ip in `/etc/hosts` file:
```
httpbin-proxy.kyma.local
```

3. Create the following rules:

- Read scope for GET requests in entire application
```
cat <<EOF | kubectl apply -f -
apiVersion: oathkeeper.ory.sh/v1alpha1
kind: Rule
metadata:
  name: httpbin-read
  namespace: default
spec:
  description: httpbin access with "read" scope
  upstream:
    url: http://httpbin.default.svc.cluster.local:8000
  match:
    methods: ["GET"]
    url: <http|https>://httpbin-proxy.$DOMAIN/<.*>
  authenticators:
    - handler: oauth2_introspection
      config:
        required_scope: ["read"]
  authorizer:
    handler: allow
EOF
```

- Write scope for POST requests to `/post` endpoint
```
cat <<EOF | kubectl apply -f -
apiVersion: oathkeeper.ory.sh/v1alpha1
kind: Rule
metadata:
  name: httpbin-write
  namespace: default
spec:
  description: httpbin access with "write" scope
  upstream:
    url: http://httpbin.default.svc.cluster.local:8000
  match:
    methods: ["POST"]
    url: <http|https>://httpbin-proxy.$DOMAIN/post
  authenticators:
    - handler: oauth2_introspection
      config:
        required_scope: ["write"]
  authorizer:
    handler: allow
EOF
```

4. Call the `HttpBin` service through Oathkeeper reverse proxy using the authorization token:

- Read scope
```
curl -ik -X GET https://httpbin-proxy.$DOMAIN/headers -H "Authorization: Bearer $ACCESS_TOKEN_READ"
```
Expected response: `200 OK`

- Write scope
```
curl -ik -X POST https://httpbin-proxy.$DOMAIN/post -d "test data" -H "Authorization: bearer $ACCESS_TOKEN_WRITE"
```
Expected response: `200 OK`

If the token is not present an expected response would be `401 Unauthorized` and if the token has been issued for invalid scope an expected response would be `403 Forbidden: Access credentials are not sufficient to access this resource`.

  </details>

  <details>
  <summary>
  Secure a lambda function
  </summary>

1. Create a sample function:
```
kubectl apply -f lambda.yaml
```

2. Create a virtual service.
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: lambda-proxy
  namespace: kyma-system
spec:
  gateways:
  - kyma-gateway
  hosts:
  - lambda-proxy.$DOMAIN
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
lambda-proxy.kyma.local
```

3. Create the following routing rule:
```
cat <<EOF | kubectl apply -f -
apiVersion: oathkeeper.ory.sh/v1alpha1
kind: Rule
metadata:
  name: lambda-read
  namespace: default
spec:
  description: lambda access with "read" scope
  upstream:
    url: http://lambda.stage.svc.cluster.local:8080
  match:
    methods: ["GET"]
    url: http://lambda-proxy.$DOMAIN/lambda
  authenticators:
    - handler: oauth2_introspection
      config:
        required_scope: ["read"]
  authorizer:
    handler: allow
```

4. Call the function
```
curl -ik https://lambda-proxy.$DOMAIN/lambda -H "Authorization: bearer $ACCESS_TOKEN_READ"
```
Expected response: 200 OK

If the token is not present an expected response would be `401 Unauthorized` or if the token has been issued for invalid scope an expected response would be `403 Forbidden: Access credentials are not sufficient to access this resource`.
  </details>
</div>

## Troubleshooting

In case of problems, make sure that:

- Oauth2 client has been successfully created:
```
curl -ik -X GET "https://oauth2-admin.$DOMAIN/clients"
```

- The access rules have been consumed by the Oathkeeper's api-server:
```
curl -ik -X GET https://oathkeeper-api-server.$DOMAIN/rules
```

- Your request contains a valid access token:
```
curl -ik -X POST "https://oauth2-admin.$DOMAIN/oauth2/introspect" -F "token=<ACCESS_TOKEN>"
```

### JWT Rule (To be moved to another document!)

```
cat <<EOF | kubectl apply -f -
apiVersion: oathkeeper.ory.sh/v1alpha1
kind: Rule
metadata:
  name: httpbin-jwt
  namespace: default
spec:
  description: httpbin access with JWT
  upstream:
    url: http://httpbin.default.svc.cluster.local:8000
  match:
    methods: ["GET"]
    url: <http|https>://httpbin-proxy.$DOMAIN/headers<.*>
  authenticators:
    - handler: jwt
      config:
        trusted_issuers:
        - https://dex.kyma.local
        required_scope:
        - admin*
  authorizer:
    handler: allow
EOF
```

issuer: https://dex.kyma.local
jwksUri: http://dex-service.kyma-system.svc.cluster.local:5556/keys
