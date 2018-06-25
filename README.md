# A sample Application for running Spring Boot on Kubernetes with Mongodb

This App runs on Minikube (https://kubernetes.io/docs/setup/minikube/). To deploy Mongodb use Helm (https://helm.sh/):

helm install --name first-mongo   --set persistence.size=2Gi,    stable/mongodb

Project is built using: mvn clean package

To use istio (https://istio.io/) service mesh deploy using:

kubectl apply -f mongo-kubernetes-configmap.yaml

(file must be adapted to you mongo db deployment)

kubectl apply -f (istioctl kube-inject -f istio-virtualservice.yaml)

(assuming autoinjection is disabled and an ingress gateway "istio-gateway-generic") exists

kubectl apply -f istio-virtualservice.yaml

