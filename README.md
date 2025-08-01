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
```bash
# From nkuser (or root if needed)
curl -sfL https://get.k3s.io | sh -
```
### Verify k3s
```bash
sudo kubectl get nodes
```


After installing K3s, the `kubectl` config file is owned by `root`, so running `kubectl` as a normal user (without `sudo`) requires a one-time setup.

---

## ✅ Steps to Use `kubectl` Without `sudo`

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

You should see your K3s node **without using `sudo`**.

---

Let me know if you'd like me to wrap this into a script for initial setup.





# 🌐 K3s + Istio + Observability + Keycloak GitOps Setup

This repo bootstraps a GitOps-driven Kubernetes cluster on a single VPS using K3s, Argo CD, Istio, and an observability + auth stack. Domain: `explorewithnk.com`

---

## 🧰 Components

- **K3s** (lightweight Kubernetes)
- **Istio** (Ingress gateway + service mesh)
- **Cert-Manager** (TLS certs via Let's Encrypt)
- **Argo CD** (GitOps controller)
- **Grafana, Loki, Tempo, Prometheus** (observability stack)
- **Keycloak** (SSO/OIDC)
- **Vault** (secrets management)

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
| Your App     | `app.explorewithnk.com`       |

> 🔐 All services are exposed via Istio Ingress Gateway with TLS using Let's Encrypt.
