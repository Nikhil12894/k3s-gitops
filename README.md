# K3s GitOps Cluster Setup

This repository contains the declarative GitOps manifests and bootstrap documentation for a single-node **K3s** Kubernetes cluster hosted on a VPS, managed via **Argo CD**, fronted by **NGINX Ingress Controller**, and secured with **cert-manager** (Let's Encrypt).

---

## 🏗️ Architecture & Core Components

- **K3s** (Lightweight Kubernetes v1.33+, Traefik disabled in favor of NGINX Ingress)
- **NGINX Ingress Controller** (Edge reverse proxy handling HTTP/HTTPS external ingress via NodePort/LoadBalancer on public IP)
- **Cert-Manager** (Automated TLS certificate issuance and renewals via Let's Encrypt ACME HTTP-01 challenge)
- **Kubernetes-Replicator** (Automated secret/config replication across namespaces)
- **Argo CD** (Declarative GitOps continuous delivery using the "App of Apps" pattern)
- **Keycloak** (Enterprise Identity & Access Management with PostgreSQL backend)
- **Homepage** (Landing page application)

---

## 🗺️ Domain Routing & Services (`explorewithnk.com`)

All public traffic is routed through **NGINX Ingress Controller** with automatic HTTPS redirection and Let's Encrypt TLS certificates:

| Application | Domain / URL | Namespace | Backend Service | Ingress Class | TLS Secret |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Homepage** | [`explorewithnk.com`](https://explorewithnk.com) | `default` | `homepage:80` | `nginx` | `explorewithnk-root-tls` |
| **Argo CD** | [`argocd.explorewithnk.com`](https://argocd.explorewithnk.com) | `argocd` | `argocd-server:80` | `nginx` | `explorewithnk-argocd-tls` |
| **Keycloak** | [`keycloak.explorewithnk.com`](https://keycloak.explorewithnk.com) | `keycloak` | `keycloak-app:80` | `nginx` | `explorewithnk-keycloak-tls` |

> ℹ️ **Decommissioned stacks**: Grafana, Prometheus, Loki, and Istio have been decommissioned and removed from the active routing topology.

---

## 📁 Repository Structure

```text
k3s-gitops/
├── argo/
│   ├── root-app.yaml                   # Argo CD Root "App of Apps"
│   └── apps/                           # Child Application manifests
│       ├── argocd.yaml                 # Argo CD ingress & certsync
│       ├── homepage.yaml               # Homepage deployment & ingress
│       ├── keycloak.yaml               # Keycloak app & certs
│       ├── grafana.yaml                # (Disabled / Commented)
│       ├── kibana.yaml                 # (Disabled / Commented)
│       ├── minio.yaml                  # (Disabled / Commented)
│       └── test-rout-app.yaml          # (Disabled / Commented)
├── bootstrap/
│   └── install/
│       └── k3s-install.sh              # Single-node cluster bootstrap script
├── charts/                             # Helm values for deployed services
│   ├── argocd/values.yaml              # Custom values for Argo CD
│   ├── keycloak/values.yaml            # Custom values for Bitnami Keycloak
│   ├── minio/                          # Archived Helm values
│   ├── elastic-apm/                    # Archived Helm values
│   └── observability-stack/            # Archived Helm values
├── manifests/                          # Raw Kubernetes manifests
│   ├── argocd/                         # Argo CD TLS certificate
│   ├── cert-manager/                   # ClusterIssuer (letsencrypt-prod)
│   ├── homepage/                       # Homepage deployment, service & ingress
│   ├── keycloak/                       # Keycloak TLS certificate
│   ├── grafana/                        # Decommissioned Grafana cert
│   ├── kibana/                         # Decommissioned Kibana cert
│   ├── minio/                          # Decommissioned MinIO cert
│   └── rout-based-app-test/            # Route-based test manifests
└── README.md                           # Main cluster documentation
```

---

## 🚀 Cluster Setup & Bootstrap Guide

### Phase 1: VPS Hardening & SSH Setup

1. **Generate SSH key pair** (on your local machine):
   ```bash
   ssh-keygen -t ed25519 -C "your_email@example.com"
   ```

2. **Configure non-root user on VPS**:
   ```bash
   ssh root@<VPS_IP>

   # Create non-root user with sudo access
   adduser nkuser
   usermod -aG sudo nkuser

   # Add SSH public key
   mkdir -p /home/nkuser/.ssh
   chmod 700 /home/nkuser/.ssh
   cat <<EOF > /home/nkuser/.ssh/authorized_keys
   <PASTE_LOCAL_ED25519_PUBLIC_KEY>
   EOF
   chmod 600 /home/nkuser/.ssh/authorized_keys
   chown -R nkuser:nkuser /home/nkuser/.ssh

   # Harden SSH config (disable root login)
   sed -i 's/^#*PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
   systemctl restart sshd
   ```

---

### Phase 2: Install K3s

Install K3s with the default Traefik ingress disabled (we use NGINX Ingress Controller for production-grade ingress):

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="\
  --disable=traefik \
  --bind-address 180.188.231.97 \
  --tls-san 180.188.231.97 \
  --tls-san explorewithnk.com \
  --node-external-ip 180.188.231.97" sh -
```

Verify K3s:
```bash
sudo kubectl get nodes
```

---

### Phase 3: Configure `kubectl` for Non-Root User & Local Machine

1. **On VPS (for `nkuser`)**:
   ```bash
   mkdir -p $HOME/.kube
   sudo cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
   sudo chown -R nkuser:nkuser $HOME/.kube
   chmod 600 $HOME/.kube/config
   echo 'export KUBECONFIG=$HOME/.kube/config' >> ~/.bashrc
   source ~/.bashrc
   ```

2. **On Local Machine**:
   ```bash
   scp nkuser@180.188.231.97:/home/nkuser/.kube/config ~/.kube/config-k3s
   # Update server IP inside config if necessary:
   sed -i '' 's/127.0.0.1/180.188.231.97/g' ~/.kube/config-k3s
   export KUBECONFIG=~/.kube/config-k3s
   kubectl get nodes
   ```

---

### Phase 4: Core Infrastructure Services

#### 1. NGINX Ingress Controller
Deploy ingress-nginx for cloud/VPS provider:
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml
```

Verify controller pod & load balancer service:
```bash
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx
```

#### 2. Helm
Install Helm 3:
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

#### 3. Cert-Manager & Let's Encrypt ClusterIssuer
Deploy cert-manager CRDs and controllers:
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
```

Apply the Let's Encrypt production `ClusterIssuer`:
```yaml
# manifests/cert-manager/my-cluster-issuer.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    email: nknk4343@gmail.com
    server: https://acme-v02.api.letsencrypt.org/directory
    privateKeySecretRef:
      name: letsencrypt-prod-private-key
    solvers:
    - http01:
        ingress:
          class: nginx
```
```bash
kubectl apply -f manifests/cert-manager/my-cluster-issuer.yaml
```

#### 4. Kubernetes Replicator
Replicates TLS secrets from `cert-manager` to application namespaces:
```bash
helm repo add mittwald https://helm.mittwald.de
helm repo update
kubectl create namespace kubernetes-replicator --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install kubernetes-replicator mittwald/kubernetes-replicator \
  --namespace kubernetes-replicator \
  --set replicationEnabled.secrets=true
```

---

### Phase 5: Core Application Deployments

#### 1. Argo CD
```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f manifests/argocd/argocd-cert-manager.yaml
helm repo add argo https://argoproj.github.io/argo-helm
helm upgrade --install argocd argo/argo-cd -f charts/argocd/values.yaml --namespace argocd
```

Fetch initial admin password:
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```

#### 2. Keycloak (SSO / IAM)
```bash
kubectl create namespace keycloak --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f manifests/keycloak/keycloak-cert-manager.yaml
helm upgrade --install keycloak-app oci://registry-1.docker.io/bitnamicharts/keycloak -f charts/keycloak/values.yaml -n keycloak
```

---

### Phase 6: Enable GitOps with Argo CD (Root App)

Once Argo CD is online, activate continuous synchronization of all child applications using the root application:

```bash
kubectl apply -f argo/root-app.yaml
```

Argo CD syncs from `argo/apps/`:
- `homepage`: Automatically reconciles `manifests/homepage` into namespace `default`
- `keycloak-app`: Automatically reconciles `manifests/keycloak` into namespace `keycloak`
- `argocd-ingress`: Automatically reconciles `manifests/argocd` into namespace `argocd`

Check synchronization status:
```bash
kubectl get applications -n argocd
```

---

## 🧹 Maintenance & Cleanup Guide

When removing stacks (like Prometheus, Grafana, Loki, or Istio), Helm or manual manifest deletion often leaves behind headless services, CRDs, RBAC roles, and namespace ConfigMaps.

### 1. Remove Leftover Istio ConfigMaps
Istio automatically injected `istio-ca-root-cert` into every namespace during installation:
```bash
for ns in $(kubectl get ns -o jsonpath='{.items[*].metadata.name}'); do
  kubectl delete cm istio-ca-root-cert -n "$ns" --ignore-not-found
done
```

### 2. Remove Leftover Prometheus Headless Services (in `kube-system`)
```bash
kubectl delete svc -n kube-system \
  observability-stack-kube-p-coredns \
  observability-stack-kube-p-kube-controller-manager \
  observability-stack-kube-p-kube-etcd \
  observability-stack-kube-p-kube-proxy \
  observability-stack-kube-p-kube-scheduler \
  observability-stack-kube-p-kubelet \
  --ignore-not-found
```

### 3. Remove Leftover Prometheus Operator CRDs (if monitoring is fully decommissioned)
```bash
kubectl delete crd \
  alertmanagerconfigs.monitoring.coreos.com \
  alertmanagers.monitoring.coreos.com \
  podmonitors.monitoring.coreos.com \
  probes.monitoring.coreos.com \
  prometheusagents.monitoring.coreos.com \
  prometheuses.monitoring.coreos.com \
  prometheusrules.monitoring.coreos.com \
  scrapeconfigs.monitoring.coreos.com \
  servicemonitors.monitoring.coreos.com \
  thanosrulers.monitoring.coreos.com \
  --ignore-not-found
```

### 4. Remove Stale RBAC from Observability Stacks
```bash
kubectl delete clusterrole \
  grafana-clusterrole \
  loki-clusterrole \
  loki-tiny-k3s-clusterrole \
  observability-stack-kube-p-prometheus \
  --ignore-not-found

kubectl delete clusterrolebinding \
  grafana-clusterrolebinding \
  loki-clusterrolebinding \
  loki-tiny-k3s-clusterrolebinding \
  observability-stack-kube-p-prometheus \
  --ignore-not-found
```

### 5. Remove Orphaned PVCs & Completed Admission Jobs
```bash
# Delete orphaned pending PVC in default namespace:
kubectl delete pvc nk-log-pvc -n default --ignore-not-found

# Delete completed admission pods from ingress-nginx (optional):
kubectl delete pod -n ingress-nginx -l app.kubernetes.io/component=admission-webhook --field-selector=status.phase=Succeeded
```

---

## 🔄 Useful Cluster Operations

- **Node Status & Metrics**:
  ```bash
  kubectl get nodes -o wide
  kubectl top nodes
  kubectl top pods -A
  ```

- **Check Ingress & TLS Status**:
  ```bash
  kubectl get ingress -A
  kubectl get certificates -A
  ```

- **Restart Deployment**:
  ```bash
  kubectl rollout restart deployment/homepage -n default
  ```

- **Uninstall K3s (Complete wipe)**:
  ```bash
  sudo /usr/local/bin/k3s-uninstall.sh
  ```