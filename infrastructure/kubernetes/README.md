# OrderFlow Kubernetes Deployment

This directory contains the Kubernetes deployment manifests for the OrderFlow system. It provisions PostgreSQL, the four Spring Boot microservices, the React frontend, and the Python remediation processor as a CronJob.

## Prerequisites
- A running Kubernetes cluster (e.g., Minikube, kind, or a managed cloud provider).
- `kubectl` CLI installed and configured.
- Container images for all services must be built and available in your cluster or a registry (the YAMLs assume images are named `order-service:latest`, `frontend:latest`, etc. and uses `imagePullPolicy: Never` for local testing. Change to `IfNotPresent` or `Always` if using a registry).

## How to Deploy
You can deploy all resources in a single step using Kustomize (which is built into kubectl):

```bash
kubectl apply -k .
```

Alternatively, apply the files individually in order:
```bash
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-config.yaml
kubectl apply -f 02-secrets.yaml
kubectl apply -f 03-postgres.yaml
kubectl apply -f 04-order-service.yaml
# ... and so on
```

## How to Check Pods
List all running pods in the `orderflow` namespace:
```bash
kubectl get pods -n orderflow
```

## How to Check Services
List all services (ClusterIPs) in the `orderflow` namespace:
```bash
kubectl get svc -n orderflow
```

## How to Inspect Logs
View logs for a specific pod (e.g., order-service):
```bash
kubectl logs <pod-name> -n orderflow
```

Tail logs continuously:
```bash
kubectl logs -f <pod-name> -n orderflow
```

## How to Manually Trigger the Python CronJob
The python-processor runs as a CronJob scheduled every 5 minutes (`*/5 * * * *`). To trigger it manually immediately without waiting for the schedule:
```bash
kubectl create job --from=cronjob/python-processor manual-processor-run -n orderflow
```
Then check its logs once the job pod starts:
```bash
kubectl logs -f job/manual-processor-run -n orderflow
```

## How to Delete the Deployment
To remove all OrderFlow resources from your cluster, you can delete the entire namespace:
```bash
kubectl delete namespace orderflow
```
Or, to keep the namespace but delete the resources:
```bash
kubectl delete -k .
```

## Service Discovery
Kubernetes automatically provides internal DNS for services. For example, the `inventory-service` exposes port `8082`. Other pods can reach it using the URL `http://inventory-service:8082`. 
We do **not** use `localhost` for inter-service communication inside Kubernetes.

## Configuration and Secrets
- **orderflow-config** (ConfigMap): Holds non-sensitive environment variables and internal DNS URLs.
- **orderflow-secrets** (Secret): Holds the PostgreSQL credentials, JWT secret, and default Admin credentials. Do not commit base64-encoded production secrets to source control. Use a secret manager in production.
- **Frontend Configuration**: The React Vite application uses `VITE_*` environment variables at *build* time. The variables are baked into the static bundle. Setting environment variables in the Kubernetes Deployment `08-frontend.yaml` does not dynamically change the JavaScript variables at runtime. To change URLs for a Kubernetes environment, you must rebuild the frontend container image with the new URLs injected.

## Persistent Resources
- **postgres-pvc**: A `PersistentVolumeClaim` is mounted to `/var/lib/postgresql/data` ensuring the PostgreSQL database preserves data across pod restarts or rescheduling.
