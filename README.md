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

```Bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable=traefik" sh -
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

You should see your K3s node **without using `sudo`**.

## Phase 4: Install Istio with `istioctl` 



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

### Phase 5 (Optional): Install cert-manager if you want to use Let's Encrypt
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
```

### Phase 6 (Optional if phase 5 is installed): Generate a self-signed certificate


Here is a step-by-step guide to create a self-signed certificate and the corresponding Kubernetes secret.

#### Step 1: Generate the Self-Signed Certificate and Key
Use the openssl command to generate a private key (tls.key) and a self-signed certificate (tls.crt). This is a common method for development and testing environments.

Note: Replace explorewithnk.com with your domain name.

```bash
openssl genrsa -out tls.key 2048


openssl req -new -x509 -sha256 -key tls.key -out tls.crt -days 3650 \
-subj "/CN=explorewithnk.com" \
-addext "subjectAltName = DNS:explorewithnk.com, DNS:*.explorewithnk.com, DNS:learnwithnk.in, DNS:*.learnwithnk.in"
```



This command will:

req -x09: Create a new self-signed certificate.

-nodes: Skip the passphrase.

-days 365: Set the certificate validity to one year.

-newkey rsa:2048: Generate a new 2048-bit RSA private key.

-keyout tls.key: Save the private key to a file named tls.key.

-out tls.crt: Save the certificate to a file named tls.crt.

-subj: Set the subject information.

After running this command, you will have two files in your current directory: tls.key and tls.crt.

#### Step 2: Create a TLS Secret for Each Namespace
Kubernetes secrets are namespaced, meaning a secret in one namespace is not accessible from another. To use the same certificate for applications in different namespaces, you must create a separate secret in each namespace.

If you have an application in the argocd namespace and another in xyz, you would run the kubectl create secret command for each one.

Note: You only need to run Step 1 once to generate the key and certificate files. Then, run the following commands for each namespace.

-  For the 'argocd' namespace
kubectl create secret tls explorewithnk-com-tls-secret --cert=tls.crt --key=tls.key --namespace=argocd

-  For the 'xyz' namespace
kubectl create secret tls explorewithnk-com-tls-secret --cert=tls.crt --key=tls.key --namespace=xyz

This command will:
```bash
kubectl create secret tls explorewithnk-com-tls-secret --cert=tls.crt --key=tls.key --namespace=istio-system
```
create secret tls: Tell kubectl to create a secret of type kubernetes.io/tls.

explorewithnk-com-tls-secret: Name the secret exactly as it's referenced in your istio-gateway document.

--cert=tls.crt: Point to the certificate file.

--key=tls.key: Point to the private key file.

--namespace=...: Create the secret in the correct namespace.

Step 3: Verify the Secrets
Finally, run the get secret command to confirm the secret was created successfully in each namespace.

#### For the 'default' namespace
kubectl get secret explorewithnk-com-tls-secret -n default

#### For the 'xyz' namespace
kubectl get secret explorewithnk-com-tls-secret -n xyz

You should now see the secret listed in the output for each namespace.

Once the secrets are successfully created, the Istio Gateway should be able to pick them up and secure your ingress traffic.


# Guide to Cross-Namespace Istio Gateway Routing
This guide provides a step-by-step process for configuring a single Istio Gateway to route traffic to services in multiple, different namespaces.

The Concept
The core idea is to:

Deploy a single Gateway in a dedicated namespace. This gateway acts as the central entry point for all traffic and holds the TLS secrets.

Deploy VirtualService resources in the application namespaces. Each VirtualService defines the routing rules for its specific service and "attaches" to the centralized Gateway.

This model allows for a clear separation of concerns:

Platform/Infra Team: Manages the shared Gateway and its TLS certificates.

Application Teams: Manage their own VirtualService and Service resources within their respective namespaces.

Step 1: Deploy the Shared Gateway
First, you need to create a dedicated namespace for your gateway (e.g., istio-ingress). The Gateway resource you provided will be deployed here.

gateway-shared.yaml

### This Gateway will be deployed in the 'istio-ingress' namespace.
```yaml
apiVersion: networking.istio.io/v1
kind: Gateway
metadata:
  name: explorewithnk-gateway
  namespace: istio-ingress # Changed from 'default' to 'istio-ingress'
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: explorewithnk-com-tls-secret # Ensure this secret exists in the 'istio-ingress' namespace
    hosts:
    - "explorewithnk.com"
    - "*.explorewithnk.com"
```
Apply this file:
kubectl apply -f gateway-shared.yaml

Note: You must ensure the explorewithnk-com-tls-secret secret also exists in the istio-ingress namespace. If it was in the default namespace, you'll need to copy it over.

Step 2: Deploy the Application and VirtualService
Next, in your application's namespace (e.g., my-app-namespace), you will deploy your service and a VirtualService that routes traffic to it.

The key is in the VirtualService's gateways field. You must specify the namespace of the gateway you want to use, followed by the gateway's name.

virtualservice-my-app.yaml

### This VirtualService will be deployed in the 'my-app-namespace' namespace.
```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: my-app-virtualservice
  namespace: my-app-namespace # Deploy in the application's namespace
spec:
  hosts:
  - "test.explorewithnk.com" # Example hostname for a service in this namespace
  gateways:
  - istio-ingress/explorewithnk-gateway # Correctly reference the shared gateway
  http:
  - match:
    - uri:
        prefix: "/my-service"
    route:
    - destination:
        host: my-app-service.my-app-namespace.svc.cluster.local # FQDN of your service
        port:
          number: 8080 # The port of your application service
```
Apply this file:
kubectl apply -f virtualservice-my-app.yaml

Step 3: Repeat for Other Applications
You can now repeat Step 2 for any other application in a different namespace. For example, for an application in another-app-namespace, you would create a new VirtualService that also references istio-ingress/explorewithnk-gateway.

Example VirtualService for another app

### This VirtualService would be in the 'another-app-namespace'
```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: another-app-virtualservice
  namespace: another-app-namespace
spec:
  hosts:
  - "another.learnwithnk.com" # A different subdomain
  gateways:
  - istio-ingress/explorewithnk-gateway
  http:
  - route:
    - destination:
        host: another-app-service.another-app-namespace.svc.cluster.local
        port:
          number: 80
```

## Phase 7: Install Istio Gateway
```bash
kubectl apply -f manifests/istio/gateway.yaml
```






## Phase 8: Install Argo CD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
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
| Your App     | `app.explorewithnk.com`       |

> 🔐 All services are exposed via Istio Ingress Gateway with TLS using Let's Encrypt.

Bootstrap Argo CD root app:

```bash
kubectl apply -f argo/root-app.yaml
```
Argo CD will auto-sync the rest.