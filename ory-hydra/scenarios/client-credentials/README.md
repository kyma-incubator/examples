# Oauth2 Token Introspection example

This tutorial shows you how to secure your resources with Oathkeeper rules and access the secured resources with appropriate calls using an OAuth2 client you registered. The tutorial uses the [ORY](https://www.ory.sh/) components:  

- [Hydra](https://www.ory.sh/docs/hydra/) - OAuth 2.0 and OpenID Connect Server.
- [Oathkeeper](https://www.ory.sh/docs/oathkeeper/) - Identity and Access Proxy.
- [Oathkeeper Maester](https://github.com/ory/oathkeeper-k8s-controller) - A Kubernetes Controller that manages Oathkeeper rules using Custom Resources.
- [Hydra Maester](https://github.com/ory/hydra-maester) - A Kubernetes controller that manages OAuth2 clients using Custom Recources.

The tutorial comes with a sample HttpBin service and a sample lambda function.

Every request to resources secured with Oathkeeper rules is processed in the following manner:

- ORY Oatkeeper finds an access rule that matches the requested URL.
- The access rule uses its `oauth2_introspection` authenticator to extract the access token from the Authorization Header.
- The token is forwarded to Hydra's OAuth 2.0 Token Introspection endpoint for liveness and scope validation.
- If the token is valid, the request is forwarded to the target service provided in the access rule.

>**NOTE:** To learn more about the Oathkeeper access rules, see the official [Oathkeeper documentation](https://www.ory.sh/docs/oathkeeper/api-access-rules).

## Register an OAuth2 client and get tokens

1. Export these values as environment variables:

  - The name of your client and the Secret which stores the client credentials:

    ```
    export CLIENT_NAME={YOUR_CLIENT_NAME}
    ```

  - The Namespace in which you want to create the client and the Secret that stores its credentials:

    ```
    export CLIENT_NAMESPACE={YOUR_CLIENT_NAMESPACE}
    ```

  - The domain of your cluster:

    ```
    export DOMAIN={CLUSTER_DOMAIN}
    ```

2. Create an OAuth2 client with `read` and `write` scopes. Run:

  ```
  cat <<EOF | kubectl apply -f -
  apiVersion: hydra.ory.sh/v1alpha1
  kind: OAuth2Client
  metadata:
    name: $CLIENT_NAME
    namespace: $CLIENT_NAMESPACE
  spec:
    grantTypes:
      - "client_credentials"
    scope: "read write"
    secretName: $CLIENT_NAME
  EOF
  ```

3. Export the credentials of the created client as environment variables. Run:

  ```
  export CLIENT_ID="$(kubectl get secret -n $CLIENT_NAMESPACE $CLIENT_NAME -o jsonpath='{.data.client_id}' | base64 --decode)"
  export CLIENT_SECRET="$(kubectl get secret -n CLIENT_NAMESPACE $CLIENT_NAME -o jsonpath='{.data.client_secret}' | base64 --decode)"
  ```

4. Encode your client credentials and export them as an environment variable:

  ```
  export ENCODED_CREDENTIALS=$(echo -n "$CLIENT_ID:$CLIENT_SECRET" | base64)
  ```

5. Get tokens to interact with secured resources:

<div tabs>
  <details>
  <summary>
  Token with "read" scope
  </summary>

  1. Get the token:

      ```
      curl -ik -X POST "https://oauth2.$DOMAIN/oauth2/token" -H "Authorization: Basic $ENCODED_CREDENTIALS" -F "grant_type=client_credentials" -F "scope=read"
      ```

  2. Export the issued token as an environment variable:

      ```
      export ACCESS_TOKEN_READ={ISSUED_READ_TOKEN}
      ```

  </details>
  <details>
  <summary>
  Token with "write" scope
  </summary>

    1. Get the token:

        ```
        curl -ik -X POST "https://oauth2.$DOMAIN/oauth2/token" -H "Authorization: Basic $ENCODED_CREDENTIALS" -F "grant_type=client_credentials" -F "scope=write"
        ```

    2. Export the issued token as an environment variable:

        ```
        export ACCESS_TOKEN_WRITE={ISSUED_WRITE_TOKEN}
        ```

   </details>
</div>

## Deploy sample resources and secure them

Follow the instructions in the tabs to deploy an instance of the HttpBin service or a sample lambda function, expose them, and secure them with Oathkeeper rules.

<div tabs>

  <details>
  <summary>
  HttpBin - secure endpoints of a service
  </summary>

1. Deploy an instance of the HttpBin service:

  ```
  kubectl apply -f https://raw.githubusercontent.com/istio/istio/master/samples/httpbin/httpbin.yaml
  ```

2. Expose the service by creating a VirtualService:

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

>**NOTE:** If you are running Kyma on Minikube, add `httpbin-proxy.kyma.local` to the entry with Minikube IP in your system's `/etc/hosts` file.

3. Secure the service with rules by creating custom resources:

- Require tokens with "read" scope for `GET` requests in the entire service

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

- Require tokens with "write" scope for `POST` requests to the `/post` endpoint of the service

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

  </details>

  <details>
  <summary>
  Secure a lambda function
  </summary>

1. Create a lambda function using the supplied code:

  ```
  kubectl apply -f lambda.yaml
  ```

2. Expose the lambda function by creating a VirtualService:

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

>**NOTE:** If you are running Kyma on Minikube, add `lambda-proxy.kyma.local` to the entry with Minikube IP in your system's `/etc/hosts` file.  

3. Create this CR to secure the lambda with a rule that requires all `GET` requests to have a valid token with the "read" scope:

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
      url: <http|https>://lambda-proxy.$DOMAIN/lambda
    authenticators:
      - handler: oauth2_introspection
        config:
          required_scope: ["read"]
    authorizer:
      handler: allow
  EOF
```


  </details>
</div>

## Access the secured resources

Follow the instructions in the tabs to call the secured service or lambda functions using the tokens issued for the client you registered.

<div tabs>

  <details>
  <summary>
  Call secured endpoints of a service
  </summary>

1. Send a `GET` request with a token with the "read" scope to the HttpBin service:

  ```
  curl -ik -X GET https://httpbin-proxy.$DOMAIN/headers -H "Authorization: Bearer $ACCESS_TOKEN_READ"
  ```

2. Send a `POST` request with a token with the "write" scope to the HttpBin's `/post` endpoint:

  ```
  curl -ik -X POST https://httpbin-proxy.$DOMAIN/post -d "test data" -H "Authorization: bearer $ACCESS_TOKEN_WRITE"
  ```

These calls return a code `200` response. If you call the service without a token, you get a code `401` response. If you call the service or its secured endpoint with a token with the wrong scope, you get the code `403` response.

  </details>

  <details>
  <summary>
  Call the secured lambda function
  </summary>

Send a `GET` request with a token with the "read" scope to the lambda function:

  ```
  curl -ik https://lambda-proxy.$DOMAIN/lambda -H "Authorization: bearer $ACCESS_TOKEN_READ"
  ```

This call returns a code `200` response. If you call the service without a token, you get a code `401` response. If you call the lambda function with a token with the wrong scope, you get the code `403` response.

  </details>
</div>
