## Phase 1: Prepare the VPS
### 1. Create a Non-Root User + SSH Key Authentication
On your local machine (Linux/macOS):

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
```
This generates keys at `~/.ssh/id_ed25519` and `~/.ssh/id_ed25519.pub`.

### On your VPS:

```bash
# Log in as root
ssh root@your-public-ip

# Create a user
adduser nkuser
usermod -aG sudo nkuser

# Create .ssh dir
mkdir -p /home/nkuser/.ssh
chmod 700 /home/nkuser/.ssh

# Paste your public key
nano /home/nkuser/.ssh/authorized_keys
# Paste contents of ~/.ssh/id_ed25519.pub

# Set permissions
chmod 600 /home/nkuser/.ssh/authorized_keys
chown -R nkuser:nkuser /home/nkuser/.ssh

# Disable root login (optional but recommended)
nano /etc/ssh/sshd_config
# Set: PermitRootLogin no


# Restart SSH
systemctl restart sshd
```

### Test login from local:

```bash
ssh nkuser@your-public-ip
```


## Phase 2: Install and Configure k3s
Install K3s with Traefik disabled:

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="\
  --disable=traefik
  --bind-address 80.188.231.97 \
  --tls-san 80.188.231.97 \
  --tls-san explorewithnk.com \
  --node-external-ip 80.188.231.97" sh -
```


### Verify k3s
```bash
sudo kubectl get nodes
```


After installing K3s, the `kubectl` config file is owned by `root`, so running `kubectl` as a normal user (without `sudo`) requires a one-time setup.

---

## Phase 3: Steps to Use `kubectl` Without `sudo`

### 1. **Copy kubeconfig to your user's `.kube` directory**

Assuming your user is `nkuser`, run:

```bash
sudo mkdir -p /home/nkuser/.kube
sudo cp /etc/rancher/k3s/k3s.yaml /home/nkuser/.kube/config
sudo chown -R nkuser:nkuser /home/nkuser/.kube
```

### 2. **(Optional) Set correct permissions**

```bash
chmod 600 /home/nkuser/.kube/config
```

### 3. **Set the `KUBECONFIG` environment variable**

You can set it **temporarily** like this:

```bash
export KUBECONFIG=$HOME/.kube/config
```

Or make it **permanent** by adding it to your shell config (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
echo 'export KUBECONFIG=$HOME/.kube/config' >> ~/.bashrc
source ~/.bashrc
```

### ✅ Now test:

```bash
kubectl get nodes
```
### copy k3s config to local machine
```bash
scp nkuser@180.188.231.97:/home/nkuser/.kube/config ~/.kube/config
```
You should see your K3s node **without using `sudo`**.

## Phase 4: Install Nginx Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml
```

## Phase 5: Install Helm
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

## Phase 6: Install Istio with `istioctl` 

#### Step-by-Step: Install istioctl
1. Download Istio
```bash
curl -L https://istio.io/downloadIstio | sh -
```
This will:

Download the latest Istio release (e.g., istio-1.22.0/)

Place it in your current directory

2. Add istioctl to your PATH
```bash
cd istio-*
export PATH=$PWD/bin:$PATH
```
To make this permanent, add that line to your shell config:

```bash
echo 'export PATH=$HOME/istio-*/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```
Replace ~ with the full path if needed.

3. Verify
```bash
istioctl version
```
You should now see both client and control plane versions.

4. install istio on node 
```bash
istioctl install --set profile=demo -y
```
4. kubectl create namespace default
```sh
kubectl label namespace default istio-injection=enabled
```

### Phase 7: Install cert-manager if you want to use Let's Encrypt
1. Install cert-manager
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
```
2. Verify
```bash
kubectl get pods -n cert-manager
```
3. Install Let's Encrypt Issuer `my-cluster-issuer.yaml`
```yaml
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



helm repo add mittwald https://helm.mittwald.de

kubectl create namespace kubernetes-replicator

helm install kubernetes-replicator mittwald/kubernetes-replicator \
  --namespace kubernetes-replicator \
  --set replicationEnabled.secrets=true

## Phase 8: Install Argo CD use [values.yaml](./charts/argocd/values.yaml)

```bash
kubectl create namespace argocd
kubectl apply -f manifests/argo/argocd-cert-manager.yaml # this will create custom certificate for ingress for argocd and the use the same secretsName in values.yaml
helm install argocd argo/argo-cd -f values.yaml --namespace argocd
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

## Phase 9: Install keycloak use [values.yaml](./charts/keycloak/values.yaml)
```bash
kubectl create namespace keycloak
kubectl apply -f manifests/keycloak/keycloak-cert-manager.yaml
helm install keycloak-app oci://registry-1.docker.io/bitnamicharts/keycloak -f values.yaml -n keycloak
```
---

## 📁 Folder Structure

```bash
k3s-gitops/
├── install/
│   └── k3s-install.sh               # K3s, Istio, cert-manager, ArgoCD bootstrap script
├── argo/
│   ├── root-app.yaml               # Argo CD root ApplicationSet
│   └── apps/
│       ├── istio.yaml
│       ├── cert-manager.yaml
│       ├── keycloak.yaml
│       ├── grafana.yaml
│       ├── prometheus.yaml
│       ├── loki.yaml
│       ├── tempo.yaml
│       ├── vault.yaml
│       └── your-app.yaml
├── manifests/
│   ├── istio/
│   │   ├── gateway.yaml
│   │   └── virtualservices/*.yaml
│   ├── cert-manager/
│   │   └── cluster-issuer.yaml
│   └── tls/
│       └── certificates/*.yaml
└── README.md
```

---

## 🚀 Domain Routing (`explorewithnk.com`)

| App          | Subdomain                     |
|--------------|-------------------------------|
| Argo CD      | `argocd.explorewithnk.com`    |
| Grafana      | `grafana.explorewithnk.com`   |
| Prometheus   | `prometheus.explorewithnk.com`|
| Loki         | `loki.explorewithnk.com`      |
| Tempo        | `tempo.explorewithnk.com`     |
| Keycloak     | `auth.explorewithnk.com`      |
| Vault        | `vault.explorewithnk.com`     |
| homepage     | `explorewithnk.com`       |

> 🔐 All services are exposed via Istio Ingress Gateway with TLS using Let's Encrypt.

Bootstrap Argo CD root app:

```bash
kubectl apply -f argo/root-app.yaml
```
Argo CD will auto-sync the rest.








---
###  Uninstall K3s:
```bash
sudo /usr/local/bin/k3s-uninstall.sh
```
---