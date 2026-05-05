# FluxCD - GitOps for Kubernetes
[<img src="./back.png">](../README.md)

This application demonstrates GitOps using FluxCD. The logic is developed in Java with Spring Boot, and the configuration is set up to deploy the application to a Docker for Windows Kubernetes cluster using FluxCD.
## FluxCD in short
FluxCD is a GitOps tool that automates the deployment of applications to Kubernetes clusters. It continuously monitors a Git repository for changes and applies those changes to the cluster, ensuring that the desired state defined in the Git repository is always reflected in the cluster. This approach allows for version control, collaboration, and easy rollback of changes, making it an ideal solution for managing Kubernetes deployments in a GitOps workflow.    
Operating as a "pull-based" agent inside the cluster, it automates deployment by watching for repository changes, making it highly secure, auditable, and reliable for managing infrastructure and application updates.
## Prerequisites
- A Kubernetes cluster (e.g., Docker for Windows as what I did in this example)
- kubectl installed and configured to access your cluster
- FluxCD installed in your Kubernetes cluster
- GitHub repository to store your application and FluxCD configuration
## Setup FluxCD and Deploy the Application
1. Clone the GitHub repository containing the application and FluxCD configuration:
```
git clone https://github.com/agilesolutions/haven-poc.git
```
2. Navigate to the project directory and build each individual service like here under and setup FluxCD
```bash
cd apps/service-a
# this will build the service and push the Docker image to the container registry (e.g., Docker Hub, ECR)
gradle release
```
3. Bootstrap FluxCD in your Kubernetes cluster:
```
flux bootstrap github --owner=agilesolutions --repository=haven-poc --branch=master --path=fluxCD --personal
```
5. Monitor the deployment status using FluxCD and kubectl:
```bash
# To check the status of the Kustomizations, you can run:
flux get kustomizations
# To watch for changes in the Kustomizations, you can run:
flux get kustomizations --watch
# To check the status of the Helm releases, you can run:
flux get helmreleases -A
# To check the status of the pods in your cluster, you can run:
kubectl get pods -A
# To check the logs of the Keycloak pod, you can run:
kubectl logs -f pod/keycloak-keycloakx-0 -n keycloak
# To check the logs of the FluxCD controllers, you can run:
kubectl logs -f deploy/kustomize-controller -n flux-system
# To check the Nginx Ingress Controller configuration, you can run:
kubectl exec -n ingress-nginx deploy/ingress-nginx-controller -- cat /etc/nginx/nginx.conf
# To access the Keycloak UI, you can port-forward the Keycloak pod to your local machine:
kubectl -n keycloak port-forward pod/keycloak-keycloakx-0 8080
# To access the application through the Nginx Ingress Controller, you can port-forward the ingress-nginx-controller service to your local machine:
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80
``` 
- If you encounter any issues, review the FluxCD documentation and logs for troubleshooting guidance.
- For more information on FluxCD and GitOps, refer to the official FluxCD documentation: https://fluxcd.io/docs/
- For more information on the sample application and its architecture, refer to the README files in the respective service directories (client-service, account-service, gateway).


[<img src="./back.png">](../README.md)
