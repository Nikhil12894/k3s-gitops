# #!/bin/bash
# set -euo pipefail

# ### CONFIGURATION ###
# K3S_VERSION="v1.30.1+k3s2"
# ISTIO_VERSION="1.26.3"
# DOMAIN="explorewithnk.com"
# EMAIL="admin@$DOMAIN"

# NAMESPACE_ISTIO="istio-system"
# NAMESPACE_CERTMGR="cert-manager"
# NAMESPACE_OBS="observability"
# NAMESPACE_AUTH="keycloak"

# echo "[1/10] Installing K3s..."
# curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="$K3S_VERSION" sh -
# export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# echo "[2/10] Installing Helm..."
# curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# echo "[3/10] Adding Helm repos..."
# helm repo add grafana https://grafana.github.io/helm-charts
# helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
# helm repo add bitnami https://charts.bitnami.com/bitnami
# helm repo add jetstack https://charts.jetstack.io
# helm repo update

# echo "[4/10] Installing Istio..."
# curl -L https://istio.io/downloadIstio | ISTIO_VERSION="$ISTIO_VERSION" sh -
# cd istio-"$ISTIO_VERSION"
# export PATH=$PWD/bin:$PATH
# istioctl install --set profile=default -y
# cd ..
# kubectl label namespace default istio-injection=enabled --overwrite

# echo "[5/10] Installing cert-manager..."
# kubectl create namespace "$NAMESPACE_CERTMGR" || true
# helm install cert-manager jetstack/cert-manager \
#   --namespace "$NAMESPACE_CERTMGR" \
#   --set installCRDs=true

# echo "[6/10] Creating ClusterIssuer for Let's Encrypt..."
# cat <<EOF | kubectl apply -f -
# apiVersion: cert-manager.io/v1
# kind: ClusterIssuer
# metadata:
#   name: letsencrypt-prod
# spec:
#   acme:
#     email: "$EMAIL"
#     server: https://acme-v02.api.letsencrypt.org/directory
#     privateKeySecretRef:
#       name: letsencrypt-prod
#     solvers:
#     - http01:
#         ingress:
#           class: istio
# EOF

# echo "[7/10] Creating namespaces..."
# kubectl create namespace "$NAMESPACE_OBS" || true
# kubectl create namespace "$NAMESPACE_AUTH" || true

# echo "[8/10] Installing Prometheus + Grafana + Loki + Tempo..."
# helm install promstack prometheus-community/kube-prometheus-stack \
#   -n "$NAMESPACE_OBS" \
#   --set grafana.ingress.enabled=true \
#   --set grafana.ingress.annotations."kubernetes\.io/ingress\.class"=istio \
#   --set grafana.ingress.hosts[0]="grafana.$DOMAIN" \
#   --set grafana.ingress.tls[0].hosts[0]="grafana.$DOMAIN" \
#   --set grafana.ingress.tls[0].secretName=grafana-tls \
#   --set grafana.adminPassword="admin"

# helm install loki grafana/loki-stack \
#   -n "$NAMESPACE_OBS" \
#   --set grafana.enabled=false \
#   --set promtail.enabled=true

# helm install tempo grafana/tempo-distributed \
#   -n "$NAMESPACE_OBS" \
#   --set tempo.metrics.enabled=true \
#   --set tempo.storage.trace.backend=local

# echo "[9/10] Installing Keycloak..."
# helm install keycloak bitnami/keycloak \
#   -n "$NAMESPACE_AUTH" \
#   --set auth.adminUser=admin \
#   --set auth.adminPassword=admin123 \
#   --set ingress.enabled=true \
#   --set ingress.hostname="auth.$DOMAIN" \
#   --set ingress.ingressClassName="istio" \
#   --set ingress.annotations."cert-manager\.io/cluster-issuer"="letsencrypt-prod" \
#   --set ingress.tls=true \
#   --set ingress.extraTls[0].hosts[0]="auth.$DOMAIN" \
#   --set ingress.extraTls[0].secretName="keycloak-tls"

# echo "[10/10] Waiting for everything to be ready..."
# kubectl wait --for=condition=ready pod -n "$NAMESPACE_OBS" --timeout=600s --all
# kubectl wait --for=condition=ready pod -n "$NAMESPACE_AUTH" --timeout=600s --all
# kubectl wait --for=condition=ready pod -n "$NAMESPACE_CERTMGR" --timeout=600s --all
# kubectl wait --for=condition=ready pod -n "$NAMESPACE_ISTIO" --timeout=600s --all

# echo
# echo "✅ K3s + Istio + Keycloak + Observability installed!"
# echo
# echo "🔗 Services (accessible via DNS):"
# echo "🌐 Grafana:     https://grafana.$DOMAIN (admin/admin)"
# echo "🌐 Keycloak:    https://auth.$DOMAIN (admin/admin123)"
# echo "🌐 Prometheus:  via port-forward or custom Ingress"
# echo "🌐 Tempo + Loki: via Grafana Explore tab"



#!/bin/bash
set -e

# Install K3s
curl -sfL https://get.k3s.io | sh -

# Set kubeconfig permissions for user
sudo mkdir -p $HOME/.kube
sudo cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH
istioctl install --set profile=demo -y
kubectl label namespace default istio-injection=enabled --overwrite

# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml

# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
