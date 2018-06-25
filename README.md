# A sample Application for running Spring Boot on Kubernetes with Mongodb

This App runs on Minikube (https://kubernetes.io/docs/setup/minikube/). To deploy Mongodb use Helm (https://helm.sh/):

helm install --name first-mongo   --set persistence.size=2Gi,    stable/mongodb --namespace <your namespace>

If you use istio, ensure that proxies are auto injected. (kubectl label namespace <namespace> istio-injection=enabled)

Project is built using: mvn clean package (Docker must be installed)

To use istio (https://istio.io/) service mesh deploy using:

kubectl apply -f mongo-kubernetes-configmap.yaml -n <your namespace>
kubectl apply -f mongo-kubernetes.yaml -n <your namespace>

(file must be adapted to you mongo db deployment)

kubectl apply -f istio-virtualservice.yaml

(assuming ingress gateway "istio-gateway-generic") exists

