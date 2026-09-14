#!/usr/bin/env bash
set -e

# 1. Install K3s (Traefik disabled)
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable=traefik" sh -

# Ensure user permissions for kubeconfig
chmod 644 /etc/rancher/k3s/k3s.yaml

# 2. Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# 3. Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml

# 4. Install Cert-Manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml

# 5. Install Kubernetes Replicator
helm repo add mittwald https://helm.mittwald.de
helm repo update
kubectl create namespace kubernetes-replicator --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install kubernetes-replicator mittwald/kubernetes-replicator \
  --namespace kubernetes-replicator \
  --set replicationEnabled.secrets=true

# 6. Install Argo CD
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "✅ Bootstrap complete. Now push to your Git repo and apply argo/root-app.yaml to initiate GitOps."

