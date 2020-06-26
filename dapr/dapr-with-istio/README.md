# Dapr with Istio

This example is the result of a POC showing how to have [Dapr](https://dapr.io/) besides a service-mesh setup, like [Istio](https://istio.io/latest/)

## Goal
- Create a sample application, using dapr for inter-cluster communication
- Use darp mTLS for dapr related communication (clients to control plane, client to client)
- Expose the application using kyma API Gateway
- Use Istio mTLS for istio related communication (user to gateway, gateway to service)

## Requirements
- A base Kyma cluster with service-mesh installed.
- [helm v3](https://helm.sh/)
- kubectl

## System setup

### Install Dapr with helm
Use the official Helm charts to install Dapr. However, we create the namespace beforehand and disable istio sidecar injection using the label `istio-injection: disabled`.

```bash
helm repo add dapr https://daprio.azurecr.io/helm/v1/repo

kubectl apply -f ./manifests/dapr-system.yaml
helm install dapr dapr/dapr --namespace dapr-system
```

### Setup Dapr StateStore (Redis)
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

helm install redis bitnami/redis --namespace dapr-system

# Get the generated Redis password, and inject into the components
REDIT_PASS=$(kubectl get secret --namespace dapr-system redis -o jsonpath="{.data.redis-password}" | base64 --decode)

cat <<EOF | kubectl apply -f -
---
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: statestore
  namespace: dapr-system
spec:
  type: state.redis
  metadata:
  - name: redisHost
    value: redis-master.dapr-system:6379
  - name: redisPassword
    value: $(echo ${REDIT_PASS})
---
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: messagebus
  namespace: dapr-system
spec:
  type: pubsub.redis
  metadata:
  - name: redisHost
    value: redis-master.dapr-system:6379
  - name: redisPassword
    value: $(echo ${REDIT_PASS})
EOF
```

###  Update the injector image
Temporary step for dapr 0.8 or older due to [This issue](https://github.com/dapr/dapr/issues/1650). 
Update the sidecar-injector image:

```bash
kubectl set image -n dapr-system deployment/dapr-sidecar-injector dapr-sidecar-injector=piotrmsc/dapr:dev2 --record
```

## Application setup

### Deploy application
The used manifests contains the following elements:
- namespace, without `istio-injection: disabled` label. The workload in this namespace should contain both dapr and istio sidecars
- nodeapp deployment and service
- pythonapp deployment

In the deployment we modify the sidecar-injection context using dapr and istio labels:

| Label | Explanation |
| :--- | :--- | 
| `dapr.io/enabled: "true"` | Enable application in dapr and inject sidecar |
| `dapr.io/id: "nodeapp"` | Uniqe ID of the app inside the dapr internal space |
| `dapr.io/port: "3000"` | Application port exposed by the app |
| `traffic.sidecar.istio.io/excludeOutboundPorts: "80,3500,50001,50002,6379"` | Set of ports that should be excluded from Istio outbound traffic capture. Those ports are used by dapr for internal communication.
| `traffic.sidecar.istio.io/excludeInboundPorts: "80,3500,50001,50002,6379"` | Set of ports that should be excluded from Istio inbound traffic capture. Those ports are used by dapr for internal communication.

```bash
kubectl apply -f ./manifests/test-app.yaml
# Wait till the applications deploy 
kubectl get pods -n dapr-demo
```

>**NOTE:** In case of failing containers, please verify if the sidecar injector image has been updated (failing readiness probes in istio-proxy sidecar), and the deployment has all required annotations (connection refused in dapr sidecar)

## Expose and access the app
```bash
KYMA_DOMAIN=$(kubectl get cm -n kyma-installer net-global-overrides -o jsonpath='{.data.global\.ingress\.domainName}')
cat <<EOF | kubectl apply -f -
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  labels:
    app: dapr-demo
  name: dapr-demo
  namespace: dapr-demo
spec:
  gateways:
  - kyma-gateway.kyma-system.svc.cluster.local
  hosts:
  - dapr-demo.$(echo ${KYMA_DOMAIN})
  http:
  - match:
    - uri:
        regex: /.*
    route:
    - destination:
        host: nodeapp
        port:
          number: 3000
EOF

curl -ik https://dapr-demo.kyma.local/order
```
