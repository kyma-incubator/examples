

SA=${1:-default}
NAMESPACE=${2:-default}


# API server URL is api.KYMA_CLUSTER_DOMAIN
API_SERVER_URL=$(kubectl config view -ojson | jq '.clusters[0].cluster.server')

# the name of the secret containing the service account token goes here
SECRET_NAME=$(kubectl get sa -n ${NAMESPACE} ${SA} -ojsonpath='{.secrets[0].name}')

ca=$(kubectl get secret/${SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.ca\.crt}')
token=$(kubectl get secret/${SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.token}' | base64 --decode)

echo "
apiVersion: v1
kind: Config
clusters:
- name: default-cluster
  cluster:
    certificate-authority-data: ${ca}
    server: ${API_SERVER_URL}
contexts:
- name: default-context
  context:
    cluster: default-cluster
    namespace: default
    user: default-user
current-context: default-context
users:
- name: default-user
  user:
    token: ${token}
"
