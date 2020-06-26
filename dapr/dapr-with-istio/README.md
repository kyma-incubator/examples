# Dapr with Istio

This example is the result of a POC showing how to use [Dapr](https://dapr.io/) besides a Service Mesh setup, like [Istio](https://istio.io/latest/).

## Goal
- Create a sample application, using Dapr for inter-cluster communication.
- Use Darp mTLS for Darp-related communication (clients to control plane, client to client).
- Expose the application through the Kyma API Gateway.
- Use Istio mTLS for Istio-related communication (user to gateway, gateway to service).

## Requirements
- A base Kyma cluster with Service Mesh installed.
- [helm v3](https://helm.sh/)
- kubectl
- base64

## System setup

### Install Dapr with helm
Use the official Helm charts to install Dapr. However, create the Namespace beforehand and disable Istio sidecar injection using the `istio-injection: disabled` label.

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
```

### Update the injector image
Temporary step for Dapr version `0.8.0` or older due to [this issue](https://github.com/dapr/dapr/issues/1650). 
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
