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
- base64

## System setup

### Install Dapr with helm
Use the official Helm charts to install Dapr. However, we create the namespace beforehand and disable istio sidecar injection using the label `istio-injection: disabled`.

```bash
helm repo add dapr https://daprio.azurecr.io/helm/v1/repo

kubectl apply -f ./manifests/dapr-ns.yaml
helm install dapr dapr/dapr --namespace dapr-system
```

### Setup Dapr StateStore (Redis)
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

helm install redis bitnami/redis --namespace dapr-redis

# Get the generated Redis password, and inject into the components
REDIT_PASS=$(kubectl get secret --namespace dapr-redis redis -o jsonpath="{.data.redis-password}" | base64 --decode)

cat <<EOF | kubectl apply -f -
---
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: statestore
  namespace: dapr-redis
spec:
  type: state.redis
  metadata:
  - name: redisHost
    value: redis-master.dapr-redis:6379
  - name: redisPassword
    value: $(echo ${REDIT_PASS})
---
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: messagebus
  namespace: dapr-redis
spec:
  type: pubsub.redis
  metadata:
  - name: redisHost
    value: redis-master.dapr-redis:6379
  - name: redisPassword
    value: $(echo ${REDIT_PASS})
EOF
```

### Update the injector image
Temporary step for dapr `0.8.0` or older due to [This issue](https://github.com/dapr/dapr/issues/1650). 
Update the sidecar-injector image:

```bash
kubectl scale --replicas 0 -n dapr-system deployment dapr-sidecar-injector
kubectl set image -n dapr-system deployment/dapr-sidecar-injector dapr-sidecar-injector=piotrmsc/dapr:dev2 --record
kubectl scale --replicas 1 -n dapr-system deployment dapr-sidecar-injector
```

## Application setup
Set of application setups:
- [Port capturing method](./port-capture.md)
- [Istio configuration method](./)
