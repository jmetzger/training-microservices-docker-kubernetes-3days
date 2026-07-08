# Example: static pod nginx

## Walkthrough 

```
cd 
mkdir -p manifests
cd manifests
mkdir -p web
cd web
```

```
nano nginx-static.yml
```

```
apiVersion: v1
kind: Pod
metadata:
  name: nginx-static-web
  labels:
    webserver: nginx
spec:
  containers:
  - name: web
    image: nginx:1.23

```

```
kubectl apply -f nginx-static.yml
kubectl get pods nginx-static-web -o wide
```

```
# Debuggen
kubectl describe pod nginx-static-web 
# show config 
kubectl get pod/nginx-static-web -o yaml
 
```

```
# pod auf Basis von manifest löschen
kubectl delete -f nginx-static.yml
```
