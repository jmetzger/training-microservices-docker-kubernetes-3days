# Project Template (ocp) 

  * Hier können direkt die Ressourcen eingestellt werden (als Vorlage für jedes Projekt)

```
# project-request-template.yaml
apiVersion: template.openshift.io/v1
kind: Template
metadata:
  name: project-request-large
  namespace: openshift-config
objects:
- apiVersion: project.openshift.io/v1
  kind: Project
  metadata:
    name: ${PROJECT_NAME}
    annotations:
      openshift.io/display-name: ${PROJECT_DISPLAYNAME}
- apiVersion: v1
  kind: LimitRange
  metadata:
    name: default-limits
    namespace: ${PROJECT_NAME}
  spec:
    limits:
    - type: Container
      default:
        cpu: 500m
        memory: 256Mi
      defaultRequest:
        cpu: 100m
        memory: 128Mi
      max:
        cpu: "1"
        memory: 512Mi
- apiVersion: v1
  kind: ResourceQuota
  metadata:
    name: default-quota
    namespace: ${PROJECT_NAME}
  spec:
    hard:
      requests.cpu: "2"
      requests.memory: 4Gi
      limits.cpu: "4"
      limits.memory: 8Gi
      pods: "10"
parameters:
- name: PROJECT_NAME
- name: PROJECT_DISPLAYNAME
```

```
# So verwenden: 
oc new-project dev-large --template=project-request-large
```
