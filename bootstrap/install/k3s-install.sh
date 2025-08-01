#!/usr/bin/env bash
set -e

# 1. Install k3s
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable=traefik" sh -

# Ensure user permissions
chmod 644 /etc/rancher/k3s/k3s.yaml

# 2. Install Helm & Istio
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
curl -L https://istio.io/downloadIstio | sh -
export PATH=$PWD/istio-*/bin:$PATH
istioctl install --set profile=demo -y

# 3. Label default NS for sidecar injection
kubectl label namespace default istio-injection=enabled --overwrite

# 4. Install cert-manager // will use self-signed cert for now
# kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml

# 5. Install Argo CD
kubectl create ns argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "✅ Bootstrap complete. Now push the argo/ folder to your Git repo and apply root-app.yaml."
