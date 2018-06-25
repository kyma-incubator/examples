# A sample Application for running Spring Boot on Kubernetes with Mongodb

This App runs on Minikube (https://kubernetes.io/docs/setup/minikube/). To deploy Mongodb use Helm (https://helm.sh/):

helm install --name first-mongo   --set persistence.size=2Gi,    stable/mongodb

project is built using: mvn clean package