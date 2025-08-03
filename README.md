<!-- ## Phase 1: Prepare the VPS
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
| homepage     | <a href="https://explorewithnk.com" target="_blank">`explorewithnk.com`</a> |

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
--- -->

# K3s GitOps Cluster Setup

This guide walks you through setting up a K3s cluster from scratch, securing it, and deploying a modern GitOps pipeline using **Argo CD**, **Istio**, and **cert-manager**.

## 🚀 Phase 1: Prepare Your VPS

First, we'll set up a secure, non-root user on your Virtual Private Server (VPS) and enable SSH key authentication for a password-less login.

### 1\. Generate SSH Keys (On your local machine)

If you don't already have SSH keys, run this command on your local machine to generate a new pair.

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
```

This creates two files in your `~/.ssh` directory: `id_ed25519` (your private key) and `id_ed25519.pub` (your public key).

### 2\. Configure Your VPS

Log in to your VPS as the **root** user and perform the following steps to create a new user and add your public key.

```bash
# Log in as root
ssh root@your-public-ip

# Create a new user (e.g., 'nkuser') and add them to the 'sudo' group
adduser nkuser
usermod -aG sudo nkuser

# Create the .ssh directory and set permissions
mkdir -p /home/nkuser/.ssh
chmod 700 /home/nkuser/.ssh

# Copy your local public key to the authorized_keys file on the server
# Use a text editor like `nano` to paste the contents of ~/.ssh/id_ed25519.pub
nano /home/nkuser/.ssh/authorized_keys

# Paste your public key here (it's a long string of characters)
# Then save and exit nano (Ctrl+X, Y, Enter)

# Secure the authorized_keys file and set ownership
chmod 600 /home/nkuser/.ssh/authorized_keys
chown -R nkuser:nkuser /home/nkuser/.ssh

# (Optional but HIGHLY Recommended) Disable root login for better security
nano /etc/ssh/sshd_config
# Find the line 'PermitRootLogin' and change it to 'no'.
# Set: PermitRootLogin no

# Restart the SSH service to apply changes
systemctl restart sshd
```

### 3\. Test Your New Login

Log out of the root session and try logging in with your new user from your local machine.

```bash
ssh nkuser@your-public-ip
```

If you log in successfully, you're ready for the next phase\!

-----

## 🏗️ Phase 2: Install and Configure K3s

Now we'll install K3s, a lightweight Kubernetes distribution, with some key configurations.

### 1\. Install K3s

Use the following command to install K3s. This command disables the default Traefik Ingress controller because we'll be using Istio and an Nginx Ingress controller later.

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="\
  --disable=traefik
  --bind-address 80.188.231.97 \
  --tls-san 80.188.231.97 \
  --tls-san explorewithnk.com \
  --node-external-ip 80.188.231.97" sh -
```

### 2\. Verify the Installation

Check that your K3s node is running and ready.

```bash
sudo kubectl get nodes
```

-----

## 🔑 Phase 3: Set up `kubectl` for Your User

By default, the `kubectl` configuration file is owned by the root user. To use `kubectl` without `sudo`, you need to set up the config file for your non-root user.

### 1\. Copy the Kubeconfig File

```bash
# Create the .kube directory
sudo mkdir -p /home/nkuser/.kube
# Copy the config file and set ownership
sudo cp /etc/rancher/k3s/k3s.yaml /home/nkuser/.kube/config
sudo chown -R nkuser:nkuser /home/nkuser/.kube
# Set correct permissions
chmod 600 /home/nkuser/.kube/config
```

### 2\. Make `kubectl` Config Permanent

Add the `KUBECONFIG` environment variable to your shell's profile to automatically load the configuration file every time you log in.

```bash
echo 'export KUBECONFIG=$HOME/.kube/config' >> ~/.bashrc
source ~/.bashrc
```

### 3\. Test the Setup

Now you should be able to run `kubectl` commands without `sudo`.

```bash
kubectl get nodes
```

### 4\. Copy Kubeconfig to Your Local Machine

For managing your cluster from your local machine, you'll need a copy of the config file.

```bash
scp nkuser@80.188.231.97:/home/nkuser/.kube/config ~/.kube/config
```

-----

## 🛠️ Phase 4: Core Services for Your GitOps Pipeline

This section installs the essential tools for a production-ready Kubernetes setup.

### Nginx Ingress Controller

While we'll use Istio for advanced routing, Nginx Ingress Controller is still useful for handling external traffic and is required by **cert-manager** for `http01` challenges.

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml
```

-----

### Helm

Helm is a package manager for Kubernetes.

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

-----

### Cert-Manager

Cert-manager automates the management and issuance of TLS certificates from Let's Encrypt, securing all your services.

1.  **Install Cert-Manager:**

    ```bash
    kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
    ```

2.  **Verify the Installation:**

    ```bash
    kubectl get pods -n cert-manager
    ```

3.  **Install a Let's Encrypt `ClusterIssuer`:**

    Create a file named `my-cluster-issuer.yaml` with the following content. Remember to replace the email address.

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

    Apply the file to your cluster:

    ```bash
    kubectl apply -f manifests/cert-manager/my-cluster-issuer.yaml
    ```

-----

### Istio with `istioctl`

Istio provides a powerful service mesh for advanced routing, security, and observability.

1.  **Download and Install `istioctl`:**

    ```bash
    curl -L https://istio.io/downloadIstio | sh -
    # Move into the downloaded directory
    cd istio-*
    # Add istioctl to your PATH for this session
    export PATH=$PWD/bin:$PATH
    ```

    To make `istioctl` available permanently, add the export command to your `~/.bashrc` file:

    ```bash
    echo 'export PATH=$HOME/istio-*/bin:$PATH' >> ~/.bashrc
    source ~/.bashrc
    ```

2.  **Install Istio on Your Cluster:**

    We'll use the `demo` profile for a balanced setup.

    ```bash
    istioctl install --set profile=demo -y
    ```

3.  **Enable Istio Sidecar Injection:**

    Create the `default` namespace and enable Istio's automatic sidecar injection.

    ```bash
    kubectl create namespace default
    kubectl label namespace default istio-injection=enabled
    ```

-----

## ⚙️ Phase 5: Deploy Your Core Applications with Helm

This section covers deploying your key services using Helm charts.

### **Argo CD**

Deploy Argo CD, the heart of your GitOps workflow.

1.  **Create the `argocd` namespace:**
    ```bash
    kubectl create namespace argocd
    ```
2.  **Apply `argocd-cert-manager.yaml`:**
    This manifest will create a certificate for Argo CD's ingress.
    ```bash
    kubectl apply -f manifests/argo/argocd-cert-manager.yaml
    ```
3.  **Deploy Argo CD with Helm:**
    This command installs Argo CD using your custom `values.yaml` file.
    ```bash
    helm install argocd argo/argo-cd -f values.yaml --namespace argocd
    ```
4.  **Retrieve the Initial Admin Password:**
    ```bash
    kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
    ```

### **Keycloak**

Deploy Keycloak, an open-source identity and access management solution.

1.  **Create the `keycloak` namespace:**
    ```bash
    kubectl create namespace keycloak
    ```
2.  **Apply `keycloak-cert-manager.yaml`:**
    ```bash
    kubectl apply -f manifests/keycloak/keycloak-cert-manager.yaml
    ```
3.  **Deploy Keycloak with Helm:**
    ```bash
    helm install keycloak-app oci://registry-1.docker.io/bitnamicharts/keycloak -f values.yaml -n keycloak
    ```

-----

## 📦 Folder Structure

To manage your GitOps repository, a clean folder structure is crucial.

```bash
k3s-gitops/
├── argo/
│   ├── root-app.yaml               # Argo CD's main ApplicationSet
│   └── apps/                       # Contains all sub-applications
│       ├── istio.yaml
│       ├── cert-manager.yaml
│       ├── keycloak.yaml
│       └── ...
├── manifests/                      # Manual Kubernetes manifests
│   ├── istio/
│   ├── cert-manager/
│   └── ...
├── charts/                         # Helm values for your applications
└── README.md
```

-----

## 🗺️ Domain Routing (`explorewithnk.com`)

This table shows how your services are exposed via Istio Ingress Gateway, each with its own domain and a TLS certificate from Let's Encrypt.

| App          | Subdomain                     |
|--------------|-------------------------------|
| Argo CD      | `argocd.explorewithnk.com`    |
| Grafana      | `grafana.explorewithnk.com`   |
| Prometheus   | `prometheus.explorewithnk.com`|
| Loki         | `loki.explorewithnk.com`      |
| Keycloak     | `auth.explorewithnk.com`      |
| Vault        | `vault.explorewithnk.com`     |
| Homepage     | `explorewithnk.com` |

Once all your applications are set up, a single command can kick off the GitOps synchronization.

```bash
kubectl apply -f argo/root-app.yaml
```

Argo CD will then take over and automatically sync the rest of your applications based on your Git repository.

-----

### Uninstall K3s

If you ever need to completely remove your K3s installation, you can use the built-in uninstall script.

```bash
sudo /usr/local/bin/k3s-uninstall.sh
```