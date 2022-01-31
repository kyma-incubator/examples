# How to get kubeconfig for the service account

> **Note:** You need [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl) (configured with your kubernetes cluster) and [jq](https://stedolan.github.io/jq/) installed to run this example


Create config map and service account that can read the config map in the `test` namespace:
```
kubectl create ns test
kubectl apply -f example-sa.yaml -n test

```

Create kubeconfig for `test-service-account` in `test` namespace:
```
./create-kubeconfig.sh test-service-account test >test-kubeconfig.yaml
```


Now use it to list and view config maps in the test namespace. 
```
kubectl --kubeconfig test-kubeconfig.yaml get cm -n test test-config-map -oyaml
```
All other actions should be forbidden. This command to list pods should fail:
```
kubectl --kubeconfig test-kubeconfig.yaml get pod -n test
```