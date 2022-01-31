# Get a kubeconfig file for your service account

## Prerequisites

To run this example, you need the following tools:

- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl) configured with your Kubernetes cluster
- [jq](https://stedolan.github.io/jq/)

## Steps```


1. Create a Config Map and a Service Account that can read the Config Map in the `test` Namespace:
```bash
kubectl create ns test
kubectl apply -f example-sa.yaml -n test

```

2. Create a kubeconfig file for `test-service-account` in the `test` Namespace:
```
./create-kubeconfig.sh test-service-account test >test-kubeconfig.yaml
```


3. Use the kubeconfig file to list and view the Config Map in the test Namespace. 
```
kubectl --kubeconfig test-kubeconfig.yaml get cm -n test test-config-map -oyaml
```
> **NOTE:** All other actions should be forbidden. For example, the following command to list pods should fail:
```
kubectl --kubeconfig test-kubeconfig.yaml get pod -n test
```