# Microservices mit Docker und Kubernetes 

## Agenda 

  1. Einführung in Microservices
     * [Was sind Microservices ?](microservices/what-are.md)
     * [Grundkonzepte von Microservices](microservices/basics.md)
     * [Praxisbeispiele](/microservices/praxisbeispiele.md)
     * [Monolith vs. Microservices (Abgrenzung)](microservices/monolith-vs-microservice.md)
     * [Indikatoren für Microservices (Wechsel von Monolith)](microservices/analyse/indikatoren-fuer-wechsel-von-monolith-auf-microservices.md)
     * [Monolith schneiden microservices](microservices/monolith-schneiden.md)

  1. Microservices-Architektur
     * [Architektur von Microservices (Schichten/Layers)](microservices/layers.md)
     * [Was ist devops](/microservices/what-is-devops.md)

  1. Design und Entwicklung von Microservices
     * [12 factor app](microservices/principles/12-factor-app.md)
     * [gRPC vs. REST API](microservices/grpc-vs-rest-api.md)
     * [API-Abfrage über REST-API](microservices/rest-api.md)
     * [Asynchrones Messaging](microservices/asynchronous-messaging.md)
     * [EventBus Implementierungen/Überblick](/microservices/eventbus/overview.md)
     * [Überblick shared database / database-per-service](microservices/databases/patterns/overview.md)
     * [Datenbank - Patterns - Teil 1](microservices/database-patterns-teil1.md)
     * [Datenbank - Patterns - Teil 2](microservices/database-patterns-teil2.md)

  1. Übungen: Monolith schneiden
     * [Uebung: Monolith schneiden — DDD, Bounded Contexts und Strangler Fig](microservices/uebung-monolith-schneiden.md)
     * [Auswertung: EventStorming — ShopMax](microservices/uebung-monolith-schneiden-eventstorming-auswertung.md)
     * [Auswertung: Bounded Contexts — ShopMax](microservices/uebung-monolith-schneiden-boundedcontexts-auswertung.md)
     * [Weiterfuehrende Schritte: Monolith schneiden (Schritt 3–7)](microservices/uebung-monolith-schneiden-weiterfuehrend.md)
     * [Musterloesung: Monolith schneiden mit DDD und Strangler Fig (Trainer)](microservices/uebung-monolith-schneiden-musterloesung.md)

## Backlog

  1. Docker-Grundlagen
     * [Übersicht Architektur](architektur.md)
     * [Was ist ein Container ?](container.md)
     * [Was sind container images](container-images.md)
     * [Container vs. Virtuelle Maschine](container-vs-vm.md)
     * [Was ist ein Dockerfile](dockerfile.md)

  1. Kubernetes - Überblick
     * [Warum Kubernetes, was macht Kubernetes](warum-kubernetes.md)
     * [Aufbau Allgemein](/kubernetes/architecture.md)
     * [Structure Kubernetes Deep Dive](https://github.com/jmetzger/training-kubernetes-advanced/assets/1933318/1ca0d174-f354-43b2-81cc-67af8498b56c)
     * [Ausbaustufen Kubernetes](installer/kubernetes-ausbaustufen.md)
     * [Aufbau mit helm,OpenShift,Rancher(RKE),microk8s](/kubernetes/aufbau-helm-microk8s-kubernetes.md)
     * [Welches System ? (minikube, micro8ks etc.)](welches-system.md)

  1. Kubernetes - Client Tools und Verbindung einrichten
     * [Tools installieren und bash-completion / syntax highlightning](install-helm-kubectl-syntax-highlightning.md)
     * [Remote-Verbindung zu Kubernetes einrichten](/kubectl/kubectl-einrichten.md)
     * [Tool zum Konvertion von docker-compose.yaml file manifesten](/tools/kompose.md)

  1. Kubernetes (Debugging)
     * [Netzwerkverbindung zu pod testen](/tipps-tricks/verbindung-zu-pod-testen.md)

  1. Kubernetes Netzwerk
     * [DNS - Resolution - Services](kubernetes-networks/dns-resolution-services.md)

  1. Kubernetes Praxis API-Objekte
     * [Das Tool kubectl (Devs/Ops) - Spickzettel](/kubectl/spickzettel.md)
     * [kubectl example with run](/kubectl/run-with-example.md)
     * [Bauen einer Applikation mit Resource Objekten](bauen-einer-webanwendung.md)
     * [Anatomie einer Webanwendung](anatomie-einer-webanwendung.md)
     * [kubectl/manifest/pod](/kubectl-examples/01-pod-nginx.md)
     * ReplicaSets (Theorie) - (Devs/Ops)
     * [kubectl/manifest/replicaset](/kubectl-examples/01a-replicaset-nginx.md)
     * Deployments (Devs/Ops)
     * [kubectl/manifest/deployments](/kubectl-examples/03-nginx-deployment.md)
     * [Services - Aufbau](/kubernetes/services-aufbau.md)
     * [kubectl/manifest/service](/kubectl-examples/03b-service.md)
     * DaemonSets (Devs/Ops)
     * [Hintergrund Ingress](/kubernetes/ingress.md)
     * [Ingress Controller auf Digitalocean (doks) mit helm installieren](/digitalocean/ingress-auf-digitalocean-mit-helm.md)
     * [Documentation for default ingress nginx](https://kubernetes.github.io/ingress-nginx/user-guide/nginx-configuration/configmap/)
     * [Beispiel Ingress](/kubectl-examples/04-ingress-nginx.md)
     * [Install Ingress On Digitalocean DOKS](/digitalocean/install-ingress-helm.md)
     * [Beispiel Ingress mit Hostnamen](/kubectl-examples/04-ingress-nginx-with-hostnames.md)
     * [Achtung: Ingress mit Helm - annotations](/ingress-mit-helm-class-achtung.md)
     * [Permanente Weiterleitung mit Ingress](/kubectl-examples/05-ingress-permanent-redirect.md)
     * [ConfigMap Example](/kubectl-examples/06-configmap.md)
     * [Configmap MariaDB - Example](kubectl-examples/06a-configmap-mariadb.md)
     * [Configmap MariaDB my.cnf](kubectl-examples/06b-mariadb-configmap-configfile.md)
     * [Secret MariaDB - Example](kubectl-examples/07-mariadb-secret.md)
     * [Secrets aus HashiCorp Vault - 3 Wege](kubernetes-security/vault-secrets-integration.md)
     * [Security und Compliance im Betrieb von Kubernetes-Clustern](kubernetes-security/security-compliance-betrieb.md)

  1. Kubernetes Praxis (Teil 2) - API Objekte
     * [Hintergrund Statefulsets](kubernetes/statefulsets.md)
     * [Übung Statefulsets](kubectl-examples/10-statefulset.md)

  1. Kubernetes Praxis (Teil 3)
     * [Using private registry](kubectl-examples/11-pod-private-registry.md)

  1. Kubernetes Ingress
     * [Ingress Controller on Detail](ingress/ingress-controller-on-detail.md)
     * [Traefik mit Helm installieren](ingress/traefik/install-with-helm.md)
     * [Beispiel Ingress Traefik mit Hostnamen](kubectl-examples/04-ingress-traefik-with-hostnames-deployment.md)
     * [Https/LetsEncrypt mit Traefik](ingress/https-letsencrypt-ingress-traefik.md)

  1. Kubernetes Scaling / Resource Management
     * [Autoscaling Pods/Deployments](/kubernetes/autoscaling.md)
     * [Resources and Limits for containers](kubernetes-resource-control/limits-resources.md)
     * [ResourceQuota und LimitRange im Namespace (Uebung)](kubectl-examples/20-resourcequota-limitrange.md)
     * [ResourceQuotas and LimitQuotas by Namespace](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/quota-memory-cpu-namespace/)
     * [ProjectTemplate auch für Ressourcen in OCP- OpenShift](/kubectl-examples/21-project-template-ocp.md)

  1. Kubernetes NetworkPolicy
     * [Übung Kubernetes Network Policy](/kubernetes-networkpolicy/00-simple-exercises-group.md)

  1. Grundwissen Microservices (weitere Themen)
     * [Microservices-Trends 2026](microservices/trends-2026.md)
     * [Best Practices fuer Multi-Cluster- und Hybrid-Umgebungen](microservices/multi-cluster-hybrid.md)
     * [Was ist ein API Gateway](microservices/api-gateway.md)
     * [Microservice and Database](microservices/basics/database.md)
     * [Brainstorming Domäne](microservices/brainstorming-domaene.md)
     * [Strategische Patterns](microservices/strategic-patterns.md)
     * [IAM als Bounded Context — fachlich oder technisch?](microservices/iam-als-bounded-context.md)
     * [Authentication in Kubernetes (Kunde / Mobile App)](microservices/authentication.md)
     * [API Gateway vs. Istio Service Mesh – Authentication](microservices/authentication-gateway-istio.md)
     * [JWT mit Keycloak und Istio — Login-Flow, Client Credentials, Validierung](microservices/jwt-keycloak-istio.md)
     * [Datenmigration: Notification Service (Dual Write, Outbox, Backfill)](microservices/datenmigration-notification-service.md)

  1. Micro-Frontends
     * [Micro-Frontends — Teams am Frontend ohne Kollisionen](microservices/micro-frontends.md)
     * [Micro-Frontends — Kommunikation zwischen MFEs](microservices/micro-frontends-kommunikation.md)
     * [Micro-Frontends — Module Federation (Webpack/Vite, TypeScript)](microservices/micro-frontends-module-federation.md)

  1. Grundwissen Microservices - Synchrones Messaging
     * [OpenAPI-Spec aus Code generieren (Go, Python, Java, TS, Rust, C#, PHP)](microservices/openapi-spec-aus-code-generieren.md)

  1. Grundwissen Microservices - Async Messaging
     * [Kafka Schaubild](/microservices/eventbus/kafka.md)
     * [Schema Registry (confluent)](async-messaging/03-schema-registry.md)
     * [Uebung: Kafka Schema Registry — Avro und Schema-Evolution](async-messaging/kafka-schema-registry.md)
     * [Topic/Queue ohne Downtime migrieren](/async-messaging/01-migrate-topic-without-downtime.md)
     * [Disruptive Änderungen im Schema migrieren](/async-messaging/02-change-schema-breaking-change.md)

  1. Grundwissen Microservices - Fehlertoleranz
     * [Circuit-Breaker und Fehlertoleranz](microservices/retry-circuit-breaker-fehlerhandling.md)

  1. Grundwissen Microservices - Tests
     * [Testing-Strategie: Was, wieviel, wann?](microservices/tests/00-testing-uebersicht.md)
     * [Static Tests](microservices/tests/01-testing-static.md)
     * [Unit-Tests](microservices/tests/02-testing-unit.md)
     * [Integration Testing mit Testcontainers](microservices/tests/03-testing-integration-testcontainers.md)
     * [Contract Testing mit OpenAPI](microservices/tests/04-testing-contract-openapi.md)
     * [Consumer-Driven Contract Testing mit Pact](microservices/tests/05-testing-contract-pact.md)
     * [End-to-End - e2e - Tests](microservices/tests/06-testing-e2e.md)
     * [Integration in GitLab CI/CD](microservices/tests/07-testing-ci-cd-gitlab.md)

  1. Example with Dockerfile
     * [Ubuntu mit ping](ubuntu-ping.md)
     * [Slim multistage-build](slim-multistage-build.md)

  1. Docker Security
     * [Docker Security](docker/security/overview.md)
     * [Scanning docker image with docker scan/snyx(Deprecated)](docker/security/docker-scan-snyk.md)

  1. Docker Compose
     * [Ist docker compose installiert?](docker-compose-installed.md)
     * [Example with Wordpress / MySQL](example-wordpress-mysql.md)
     * [Example with Ubuntu and Dockerfile](example-docker-compose-ubuntu-build.md)
     * [Logs in docker - compose](docker-compose-logs.md)
     * [docker compose Reference](https://docs.docker.com/compose/compose-file/compose-file-v3/)

  1. Docker - compose (Testprojekte)
     * [Testprojekt mit api und mongodb](docker-compose/01-test-project-api-mongodb-nodejs.md)

  1. Microservices - Daten
     * [Umgang mit Joins bei database-per-service](microservices/databases/patterns/database-per-service/handling-of-joins.md)
     * [Umgang mit Transaktionen bei database-per-service (SAGA)](microservices/databases/patterns/database-per-service/handling-of-transactions.md)
     * [Uebung: SAGA-Pattern mit Temporal (Docker Compose, Java)](microservices/uebung-saga-temporal.md)
     * [Apache Camel (EIP) vs. Temporal — Vergleich und Entscheidungshilfe](microservices/apache-camel-vs-temporal.md)
     * [Event Sourcing](microservices/databases/patterns/database-per-service/event-sourcing.md)

  1. Microservice - flightapp - concepts
     * [Vorgehensweise nach dem SEED-Verfahren](microservices-flightapp/concept/00-design-with-seed-method.md)
     * [Vorgehensweise nach SEED on Detail](microservices-flightapp/concept/00-design-with-seed-method-in-details.md)
     * [SEED vs. DDD mit EventStorming — Wann nehme ich was?](microservices/seed-vs-ddd-eventstorming.md)

  1. Microservice - flightapp - reservations
     * [Template for microservice with python flask](microservices-flightapp/00-microservice-python-flask-template.md)
     * [Create microservice - reservations](microservices-flightapp/reservations/01-create-microservice.md)
     * [Upload image microservice - reservations](microservices-flightapp/reservations/02-uploadimage.md)
     * [Build image reservations with gitlab ci/cd](microservices-flightapp/reservations/03-build-image-on-gitlab-ci-cd.md)

  1. Microservice - flightapp - flights
     * [Template for microservice flights with node bootstrap](microservices-flightapp/00-microservice-template-node-bootstrap.md)
     * [Build flight app](microservices-flightapp/flights/01-create-microservice.md)
     * [Use premade version of flight app - with fixes already](/microservices-flightapp/flights/01-create-microservice-v2.md)
     * [Upload image flight app](microservices-flightapp/flights/02-uploadimage-microservice.md)

  1. Microservice - flightapp - Deployment Kubernetes
     * [Manual deployment](microservices-flightapp/deploy-to-kubernetes/01-manifests-manually.md)
     * [gitlab Deployment](microservices-flightapp/deploy-to-kubernetes/02-deployment-with-gitlab-ci-cd-pipeline.md)
     * [github Deployment](github-actions/deploy-manifests.md)
     * [github Deployment-with-secret-not-working](github-actions/deploy-manifests-using-secret-not-working.md)

  1. Microservice - flightapp - Uebungen: Manuell in Kubernetes deployen
     * [Uebung: ms-reservations manuell deployen und Service erstellen](microservices-flightapp/deploy-to-kubernetes/03-reservations-manual.md)
     * [Loesung: Service fuer ms-reservations](microservices-flightapp/deploy-to-kubernetes/03-reservations-manual-loesung.md)

  1. Kubernetes - Einsatz
     * [Kubernetes Einsatz -> Risiken](kubernetes/risks.md)
     * [Kubernetes Datenbanken in Kubernetes oder ausserhalb](kubernetes/dbs.md)

  1. Kubernetes mit microk8s (Installation und Management)
     * [Installation Ubuntu - snap](microk8s/installation-ubuntu-snap.md)
     * [Create a cluster with microk8s](microk8s/cluster.md)
     * [Remote-Verbindung zu Kubernetes (microk8s) einrichten](microk8s/connect-from-remote.md)

  1. Kubernetes mit k3s
     * [Kubernetes mit k3s](kubernetes/install/k3s/overview.md)

  1. ServiceMesh
     * [Istio — Service Mesh Überblick](/istio/istio-overview.md)
     * [Why a ServiceMesh ?](istio/overview/benefits-of-a-service-mesh.md)
     * [How does a ServiceMeshs work? (example istio](/istio/overview/overview-classic-sidecar.md)
     * [istio security features](istio/overview/security-features.md)
     * [istio-service mesh - ambient mode](/istio/overview/ambient-mode.md)
     * [istio-traffic-management](/istio/traffic-management/overview.md)
     * [Retry und Circuit Breaker](istio/traffic-management/retry-circuit-breaker.md)
     * [Performance comparison - baseline,sidecar,ambient](/istio/overview/performance-comparison-baseline-sidecar-ambient.md)
     * [Übung: JWT-Token mit RBAC (RequestAuthentication + AuthorizationPolicy)](istio/12-jwt-rbac.md)

  1. Kubernetes NetworkPolicy
     * [NetworkPolicy - Pod-Traffic absichern (CIS 5.3)](kubernetes-networkpolicy/networkpolicy-exercise.md)
     * [Debugging: FE zu Backend Verbindungen mit kubectl debug und NetworkPolicy](kubernetes-networkpolicy/20-debug-networkpolicy.md)

  1. Kubernetes Tipps & Tricks
     * [Oomkiller and maxReadySeconds for safe migration to new pods](tipps-tricks/oomkiller-test-max-ready-seconds.md)
     * [Pod-Netzwerk debuggen durch weiteren Pod der daneben liegt kubectl debug](tipps-tricks/kubectl-debug.md)
     * [Aus pod mit curl api-server abfragen](/kubernetes-advanced/curl-api-server.md)

  1. Kubernetes - Monitoring
     * [Überblick Monitoring-Stack (Prometheus + Grafana)](kubernetes-monitoring/monitoring-overview.md)
     * [metrics-server aktivieren (microk8s und vanilla)](/microk8s/metrics-server.md)
     * [Prometheus Überblick](/prometheus/overview.md)
     * [Prometheus Kubernetes Stack installieren](prometheus-grafana/install-with-helm.md)
     * [Prometheus - Services scrapen die keine Endpunkte für Prometheus haben](prometheus-grafana/z_blackbox-exporter.md)

  1. Kubernetes Storage (CSI)
     * [Überblick Persistant Volumes (CSI)](kubernetes-csi/overview.md)
     * [Liste der Treiber mit Features (CSI)](https://kubernetes-csi.github.io/docs/drivers.html)
     * [Übung Persistant Storage](kubernetes-csi/nfs-exercise.md)
     * [Beispiel mariadb](kubernetes-csi/example-mariadb.md)

  1. Helm
     * [Helm internals / secret a.s.o](helm/deep-dive.yml)

  1. Kubernetes with ServerLess
     * [Kubernetes with Serverless](/kubernetes/serverless/overview.md)

  1. Literatur / Documentation / Information (Microservices)
     * [Sam Newman - Microservices](https://www.amazon.de/Building-Microservices-English-Sam-Newman-ebook/dp/B09B5L4NVT/)
     * [Sam Newman - Vom Monolithen zu Microservices](https://www.amazon.de/Vom-Monolithen-Microservices-bestehende-umzugestalten/dp/3960091400/)
     * [Microservices.io Patterns](https://microservices.io)
     * [BFF](https://blog.bitsrc.io/bff-pattern-backend-for-frontend-an-introduction-e4fa965128bf)
     * [Microservices Up and Running](https://www.amazon.de/Kubernetes-Running-Dive-Future-Infrastructure/dp/109811020X/ref=sr_1_1)

  1. FAQ
     * [Verschlüsselung mit Thales docker-container](encryption/thales.md)

  1. gitlab ci/cd
     * [Einfaches Beispielscript](gitlab-ci-cd/01-show-content.md)
     * [Docker image bauen mit fastapi (python) und kaniko](gitlab-ci-cd/02-simple-example-create-image-with-fastapi.md)

