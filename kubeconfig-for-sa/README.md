# How to get kubeconfig for the service account


| This example works with Gardener. If you want to use it with other kubernetes cluster you need to provide API_SERVER_URL in [create-kubeconfig.sh](create-kubeconfig.sh) script.|
|---|

Create config map and service account that can read the config map in the `test` namespace:
```
kubectl create ns test
kubectl apply -f example-sa.yaml -n test

```
Create kubeconfig for `test-service-account` in `test` namespace:

```
./create-kubeconfig.sh test-service-account test >test-kubeconfig.yaml
```

In another terminal session switch kubeconfig and use it to list and view config maps in test namespace. All other actions should be forbidden:
```
export KUBECONFIG=./test-kubeconfig.yaml
kubectl get cm -n test test-config-map -oyaml
```

