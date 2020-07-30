export GO111MODULE=off
export GOPATH=$HOME/go/

go get -u github.com/alexellis/hash-browns
port=3000 $GOPATH/bin/hash-browns

export TOKEN="blablabla" 
export NAMESPACE=pb
export PROXY_NAME=commerce

kubectl create ns $NAMESPACE
helm install $PROXY_NAME inlets -n $NAMESPACE --set token=$TOKEN

kubectl create secret -n $NAMESPACE generic inlets-token --from-literal token=${TOKEN} 

kubectl apply -f inlet-exit-node.yaml -n $NAMESPACE

export REMOTE="wss://$(kubectl get virtualservice -n $NAMESPACE -l apirule.gateway.kyma-project.io/v1alpha1=$PROXY_NAME-inlets.$NAMESPACE -o jsonpath='{ .items[0].spec.hosts[0] }')"

inlets client \
 --remote=$REMOTE \
 --upstream=http://127.0.0.1:10000 \
 --token=$TOKEN

kubectl delete secret inlets-token -n $NAMESPACE
kubectl delete -f inlet-exit-node.yaml -n $NAMESPACE