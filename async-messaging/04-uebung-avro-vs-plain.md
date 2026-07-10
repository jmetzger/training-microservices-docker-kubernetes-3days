# Uebung: Kafka Producer/Consumer mit und ohne Schema Registry (Java, Kubernetes)

**Dauer:** ca. 60-90 Minuten
**Level:** Fortgeschritten
**Stack:** Java 17, Maven, Kafka, Confluent Schema Registry, Avro, Kubernetes

> **Node-Bedarf:** Getestet mit 7 Teilnehmern + Trainer (8 parallele Schema-Registry-
> Instanzen) auf einem Node-Pool mit Autoscaling **3-8 Nodes** (`s-2vcpu-4gb`, aktuell
> 8 Nodes das Maximum). Im Test reichten durchgehend **3 Nodes** — kein Autoscaling-Event
> ausgeloest, Speicherauslastung lag bei 65-83% pro Node. Pro Teilnehmer braucht die
> Schema Registry ~150-250MB RAM (durch `SCHEMA_REGISTRY_HEAP_OPTS` und `resources.limits`
> in Teil 2.1 gedeckelt) plus kurzlebige Producer/Consumer-Pods (~50-100MB, nur Sekunden
> aktiv). Faustregel: **bis zu ~10 Teilnehmer pro 3 Nodes dieser Groesse**, danach
> uebernimmt der Cluster-Autoscaler automatisch bis zum Maximum von 8 Nodes.

---

## Lernziel

Du fuehrst denselben Producer/Consumer-Vorgang zweimal aus: einmal **ohne** Schema
Registry (rohe JSON-Strings) und einmal **mit** Schema Registry (Avro). Dabei siehst
du live, was der Vergleich aus [03-schema-registry.md](./03-schema-registry.md) in
der Praxis bedeutet:

- Ohne Registry: ein Breaking Change (Feld umbenannt) laesst sich klaglos produzieren,
  der Consumer bricht still — niemand hat's verhindert.
- Mit Registry: eine kompatible Schema-Erweiterung (neues optionales Feld) funktioniert
  automatisch bei altem und neuem Consumer. Eine inkompatible Aenderung wird von der
  Registry **abgelehnt**, bevor ueberhaupt eine Nachricht rausgeht.

Nach der Uebung kannst du:

- Kafka-Producer/Consumer in Java sowohl mit `StringSerializer` als auch mit
  `KafkaAvroSerializer` betreiben
- eine Schema Registry in Kubernetes pro Teilnehmer isoliert betreiben
  (eigenes `_schemas`-Topic auf gemeinsamem Kafka-Cluster)
- Avro-Schema-Evolution (kompatibel) von einem Breaking Change (inkompatibel) unterscheiden

---

## Architektur

**Kafka wird nur einmal vom Trainer aufgesetzt** (Namespace `kafka`, gemeinsam fuer
alle Teilnehmer). **Jeder Teilnehmer betreibt seine eigene Schema Registry** im eigenen
Namespace — das isoliert Schema-Historie und Kompatibilitaetspruefung pro Teilnehmer,
obwohl der Kafka-Broker geteilt wird.

```
                     Namespace "kafka" (Trainer, 1x)
                     ┌─────────────────────────┐
                     │   Kafka (3 Broker,       │
                     │   KRaft-Mode)            │
                     └────────────┬─────────────┘
                                  │ kafka.kafka.svc.cluster.local:9092
              ┌───────────────────┼───────────────────┐
              │                   │                   │
   Namespace "jochen"   Namespace "alex"     Namespace "<dein-name>"
   ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
   │ schema-registry   │ │ schema-registry   │ │ schema-registry   │
   │ Topic: _schemas-  │ │ Topic: _schemas-  │ │ Topic: _schemas-  │
   │   jochen          │ │   alex             │ │   <dein-name>      │
   │                    │ │                    │ │                    │
   │ Topics:            │ │ Topics:            │ │ Topics:            │
   │  jochen-orders-*   │ │  alex-orders-*     │ │  <dein-name>-orders-* │
   └──────────────────┘ └──────────────────┘ └──────────────────┘
```

Wichtig: Da alle Teilnehmer denselben Kafka-Broker nutzen, muessen **drei** Dinge pro
Teilnehmer eindeutig sein — sonst kollidieren Schema-IDs und Nachrichten zwischen
Teilnehmern, oder die Schema Registry crasht direkt beim Start:

- **`SCHEMA_REGISTRY_KAFKASTORE_TOPIC`** — das interne Speicher-Topic fuer Schemas
- **`SCHEMA_REGISTRY_SCHEMA_REGISTRY_GROUP_ID`** — die Kafka-Consumer-Gruppe fuer die
  Leader-Election zwischen mehreren Schema-Registry-Instanzen. **Ohne diese Einstellung
  ist der Default schlicht `"schema-registry"` — bei mehreren Teilnehmern auf demselben
  Kafka-Broker joinen dann alle dieselbe Gruppe, was zu einem sofortigen Crash fuehrt**
  (`IllegalStateException: The schema registry group contained multiple members
  advertising the same URL`)
- **deine eigenen Kafka-Topics** (`${NS}-orders-plain`, `${NS}-orders-avro`)

### Alle Teilnehmer gleichzeitig

Das Setup ist fuer Parallelbetrieb ausgelegt: jeder Namespace, jedes Topic und jedes
`_schemas`-Topic ist pro Teilnehmer eindeutig, Pod-Namen (`plain-producer`, `avro-consumer`, ...)
muessen nur innerhalb eines Namespace eindeutig sein. 8 Teilnehmer koennen also parallel
durch die Uebung gehen, ohne sich gegenseitig zu stoeren.

Ein Punkt braucht Aufmerksamkeit: Die Schema Registry ist ein **dauerhaft laufender
JVM-Prozess** (~250-300MB RAM), waehrend Producer/Consumer nur wenige Sekunden leben.
Bei 7 Teilnehmern + Trainer sind das 8 gleichzeitig laufende Schema-Registry-Instanzen.
Auf kleinen Clustern (z.B. 3 Nodes mit 2 vCPU / 4GB) kann das eng werden — das Manifest
in Teil 2.1 begrenzt deshalb bewusst den JVM-Heap (`SCHEMA_REGISTRY_HEAP_OPTS`) und setzt
`resources.limits`, damit eine einzelne Instanz nicht mehr als ~384Mi belegt. Der Trainer
sollte trotzdem waehrend der Uebung kurz `kubectl top nodes` im Blick behalten.

---

## Code-Struktur

```
async-messaging/uebung-avro-vs-plain/
├── pom.xml
├── Dockerfile
├── src/main/java/de/t3isp/schemademo/
│   ├── Main.java             # Einstiegspunkt (plain-producer | plain-consumer | avro-producer | avro-consumer)
│   ├── Env.java              # liest Konfiguration aus Umgebungsvariablen
│   ├── PlainProducer.java    # sendet rohe JSON-Strings, keine Schema-Kontrolle
│   ├── PlainConsumer.java    # erwartet hart das Feld "quantity"
│   ├── AvroProducer.java     # sendet Avro-Records ueber die Schema Registry
│   └── AvroConsumer.java     # liest Avro-Records, Schema wird automatisch aufgeloest
└── src/main/resources/
    ├── order-v1.avsc              # id, product, quantity
    ├── order-v2.avsc              # + optionales Feld "customer" (default: null) -> kompatibel
    └── order-v3-incompatible.avsc # "quantity" in "qty" umbenannt -> inkompatibel (Pflichtfeld ohne default)
```

Ein fertiges Image liegt bereits auf Docker Hub: `dockertrainereu/kafka-schema-demo:1.0`
(gebaut aus genau diesem Code). Du kannst es direkt verwenden — oder selbst bauen:

```
cd async-messaging/uebung-avro-vs-plain
docker build -t dockertrainereu/kafka-schema-demo:1.0 .
docker push dockertrainereu/kafka-schema-demo:1.0
```

### Wichtige Abhaengigkeiten (`pom.xml`)

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.8.0</version>
</dependency>
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
    <version>7.7.1</version>
</dependency>
```

| Dependency | Wofuer |
|---|---|
| `kafka-clients` | `KafkaProducer`/`KafkaConsumer` — reicht allein fuer den **PlainProducer/PlainConsumer** (kein Avro, keine Registry noetig) |
| `avro` | Stellt `Schema`, `GenericRecord`, `GenericData.Record` — das In-Memory-Datenmodell fuer Avro-Nachrichten, unabhaengig von Kafka |
| `kafka-avro-serializer` (Confluent) | Bringt `KafkaAvroSerializer`/`KafkaAvroDeserializer` und den `SchemaRegistryClient` — das ist die Bruecke, die bei jedem `send()`/`poll()` automatisch mit der Schema Registry spricht |

Wichtig: `kafka-avro-serializer` liegt nicht in Maven Central, sondern im Confluent-Repository
(`https://packages.confluent.io/maven/`) — das steht im `<repositories>`-Block der `pom.xml`.
Fehlt dieser Eintrag, bricht `mvn package` mit "Could not find artifact io.confluent:..." ab.

Der `maven-shade-plugin` baut daraus ein einziges Fat-Jar mit `Main.class` als Einstiegspunkt
(`app.jar`), damit im Dockerfile kein Klassenpfad zusammengesetzt werden muss.

---

## Kernkonzepte im Code

### PlainProducer — ohne jede Schema-Kontrolle

```java
private static String buildJson(String schemaVersion, String id, int quantity) {
    return switch (schemaVersion) {
        case "v1" -> """
                {"id":"%s","product":"Schraubenzieher","quantity":%d}""".formatted(id, quantity);
        // Breaking Change: Feld "quantity" wurde ohne Ankuendigung in "qty" umbenannt.
        // Ohne Registry gibt es niemanden, der das verhindert.
        case "v2-breaking" -> """
                {"id":"%s","product":"Schraubenzieher","qty":%d}""".formatted(id, quantity);
        default -> throw new IllegalArgumentException("Unbekannte SCHEMA_VERSION: " + schemaVersion);
    };
}
```

Der Producer serialisiert mit dem stinknormalen `StringSerializer` — Kafka selbst weiss
nichts vom Inhalt der Nachricht, geschweige denn von einem Schema.

### PlainConsumer — erwartet ein festes Feld

```java
private static final Pattern QUANTITY_FIELD = Pattern.compile("\"quantity\"\\s*:\\s*(\\d+)");
...
Matcher matcher = QUANTITY_FIELD.matcher(record.value());
if (matcher.find()) {
    System.out.println("gelesen: " + record.value() + " -> quantity=" + matcher.group(1));
} else {
    System.out.println("gelesen: " + record.value() + " -> quantity=FEHLT! ...");
}
```

Wenn der Producer das Feld umbenennt, findet der Consumer es schlicht nicht mehr.
Es gibt keinen Fehler, keine Warnung — nur eine leere Stelle im Output.

### AvroProducer — warum "bei jedem Schritt" ein Schema registriert wird

```java
Properties props = new Properties();
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
...
producer.send(new ProducerRecord<>(topic, key, record), (metadata, exception) -> {
    if (exception != null) {
        System.err.println("Fehler beim Senden (Registry hat vermutlich abgelehnt): " + exception.getMessage());
    }
    ...
});
```

Das ist der Kernunterschied zum `PlainProducer` und der Grund, warum in **jedem**
Schritt von Teil 2.4 eine "Registrierung" passiert: der `KafkaAvroSerializer` prueft
bei **jedem einzelnen `send()`-Aufruf**, ob das Schema des Records der Registry schon
bekannt ist:

- **Schema unveraendert** (z.B. zweiter Producer-Lauf mit `SCHEMA_VERSION=v1`): der
  Client hat die Schema-ID bereits im lokalen Cache, es geht kein Netzwerk-Call raus —
  "Registrierung" ist hier nur eine Cache-Pruefung, keine echte Kafka-Schreiboperation.
- **Neues, kompatibles Schema** (`v2` — Feld `customer` hinzugefuegt): die Registry
  vergibt eine neue Schema-ID (Version 2) und schreibt sie ins `_schemas`-Topic.
- **Neues, inkompatibles Schema** (`v3-incompatible` — Feld `quantity` in `qty`
  umbenannt): die Registry **lehnt die Registrierung ab**, `producer.send()` wirft eine
  Exception, **bevor** ueberhaupt eine Nachricht im eigentlichen Topic landet.

Anders als bei einer zentral gepflegten `.avsc`-Datei, die man einmal hochlaedt, ist die
Registrierung hier also ein **Nebeneffekt jedes Sendevorgangs** — deshalb taucht sie in
der Uebung wiederholt auf, nicht weil du sie manuell wiederholst, sondern weil der
Serializer sie bei jedem Producer-Start automatisch ausloest.

### AvroConsumer — Schema wird pro Nachricht automatisch aufgeloest

```java
GenericRecord value = record.value();
Object customer = value.getSchema().getField("customer") != null
        ? value.get("customer")
        : "<Feld existiert in diesem Schema nicht>";
System.out.println("gelesen: id=" + value.get("id") + " ... customer=" + customer);
```

Der `KafkaAvroDeserializer` holt sich anhand der in der Nachricht mitgeschickten
Schema-ID automatisch das passende Schema von der Registry — der Consumer-Code muss
nichts manuell parsen. Neue, abwaertskompatible Felder tauchen einfach zusaetzlich auf.

---

## Teil 1 — Zentraler Setup (Trainer, einmalig)

> Identisch zu Teil 1.1/1.2 aus [kafka-schema-registry.md](./kafka-schema-registry.md) —
> falls dieser Kafka-Cluster in der Namespace `kafka` schon existiert (z.B. aus einer
> anderen Uebung), kannst du diesen Schritt ueberspringen.

```
kubectl create namespace kafka
```

```
kubectl apply -n kafka -f - <<'EOF'
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-controller-headless
  namespace: kafka
spec:
  clusterIP: None
  ports:
  - name: client
    port: 9092
  - name: controller
    port: 9093
  selector:
    app.kubernetes.io/name: kafka
---
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: kafka
spec:
  ports:
  - name: client
    port: 9092
  selector:
    app.kubernetes.io/name: kafka
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka-controller
  namespace: kafka
spec:
  serviceName: kafka-controller-headless
  replicas: 3
  selector:
    matchLabels:
      app.kubernetes.io/name: kafka
  template:
    metadata:
      labels:
        app.kubernetes.io/name: kafka
    spec:
      securityContext:
        fsGroup: 1000
        runAsUser: 1000
      containers:
      - name: kafka
        image: apache/kafka:3.9.0
        command:
        - /bin/bash
        - -c
        - |
          NODE_ID=${HOSTNAME##*-}
          HOST="${HOSTNAME}.kafka-controller-headless.kafka.svc.cluster.local"
          export KAFKA_NODE_ID=$NODE_ID
          export KAFKA_ADVERTISED_LISTENERS="PLAINTEXT://${HOST}:9092,CONTROLLER://${HOST}:9093"
          mkdir -p /var/lib/kafka/data/kafka-logs
          exec /etc/kafka/docker/run
        env:
        - name: CLUSTER_ID
          value: "MkU3OEVBNTcwNTJENDM2Qk"
        - name: KAFKA_PROCESS_ROLES
          value: "broker,controller"
        - name: KAFKA_CONTROLLER_QUORUM_VOTERS
          value: "0@kafka-controller-0.kafka-controller-headless.kafka.svc.cluster.local:9093,1@kafka-controller-1.kafka-controller-headless.kafka.svc.cluster.local:9093,2@kafka-controller-2.kafka-controller-headless.kafka.svc.cluster.local:9093"
        - name: KAFKA_LISTENERS
          value: "PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093"
        - name: KAFKA_CONTROLLER_LISTENER_NAMES
          value: "CONTROLLER"
        - name: KAFKA_INTER_BROKER_LISTENER_NAME
          value: "PLAINTEXT"
        - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
          value: "3"
        - name: KAFKA_DEFAULT_REPLICATION_FACTOR
          value: "3"
        - name: KAFKA_MIN_INSYNC_REPLICAS
          value: "2"
        - name: KAFKA_LOG_DIRS
          value: "/var/lib/kafka/data/kafka-logs"
        - name: PATH
          value: "/opt/kafka/bin:/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        ports:
        - containerPort: 9092
          name: client
        - containerPort: 9093
          name: controller
        volumeMounts:
        - name: data
          mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 4Gi
EOF
```

```
kubectl -n kafka rollout status statefulset/kafka-controller
```

Erwartete Ausgabe: `partitioned roll out complete: 3 new pods have been updated...`

---

## Teil 2 — Pro Teilnehmer

> **Setze als Erstes deinen Namespace:**
>
> ```
> export NS=<dein-name>   # z.B. dein bereits zugewiesener Namespace
> ```

### 2.1 Eigene Schema Registry deployen

Das `SCHEMA_REGISTRY_KAFKASTORE_TOPIC` bekommt deinen Namespace als Suffix, damit sich
Teilnehmer nicht gegenseitig die Schema-Historie ueberschreiben:

```
kubectl apply -n ${NS} -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: schema-registry
spec:
  replicas: 1
  selector:
    matchLabels:
      app: schema-registry
  template:
    metadata:
      labels:
        app: schema-registry
    spec:
      enableServiceLinks: false
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: schema-registry
              namespaceSelector: {}
              topologyKey: kubernetes.io/hostname
      containers:
      - name: schema-registry
        image: confluentinc/cp-schema-registry:7.7.1
        env:
        - name: SCHEMA_REGISTRY_HOST_NAME
          value: "schema-registry.${NS}.svc.cluster.local"
        - name: SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS
          value: "PLAINTEXT://kafka.kafka.svc.cluster.local:9092"
        - name: SCHEMA_REGISTRY_LISTENERS
          value: "http://0.0.0.0:8081"
        - name: SCHEMA_REGISTRY_KAFKASTORE_TOPIC
          value: "_schemas-${NS}"
        - name: SCHEMA_REGISTRY_KAFKASTORE_TOPIC_REPLICATION_FACTOR
          value: "3"
        - name: SCHEMA_REGISTRY_SCHEMA_REGISTRY_GROUP_ID
          value: "schema-registry-${NS}"
        - name: SCHEMA_REGISTRY_KAFKASTORE_TIMEOUT_MS
          value: "10000"
        - name: SCHEMA_REGISTRY_HEAP_OPTS
          value: "-Xms192m -Xmx192m"
        resources:
          requests:
            cpu: 100m
            memory: 256Mi
          limits:
            cpu: 300m
            memory: 384Mi
        ports:
        - containerPort: 8081
          name: http
        readinessProbe:
          httpGet:
            path: /subjects
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 5
          failureThreshold: 24
---
apiVersion: v1
kind: Service
metadata:
  name: schema-registry
spec:
  selector:
    app: schema-registry
  ports:
  - port: 8081
    name: http
    targetPort: 8081
EOF
```

```
kubectl -n ${NS} rollout status deployment/schema-registry
```

Erwartete Ausgabe: `deployment "schema-registry" successfully rolled out`

### 2.2 Eigene Topics anlegen

```
kubectl -n kafka exec -it kafka-controller-0 -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --topic ${NS}-orders-plain \
    --partitions 3 --replication-factor 2

kubectl -n kafka exec -it kafka-controller-0 -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --topic ${NS}-orders-avro \
    --partitions 3 --replication-factor 2
```

---

### 2.3 Schritt A — Ohne Registry: Breaking Change ungebremst

**Producer V1 — was wird gesendet:**

```
{"id":"o-1","product":"Schraubenzieher","quantity":1}
{"id":"o-2","product":"Schraubenzieher","quantity":2}
{"id":"o-3","product":"Schraubenzieher","quantity":3}
```

```
kubectl -n ${NS} run plain-producer --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="TOPIC=${NS}-orders-plain" \
  --env="SCHEMA_VERSION=v1" \
  --env="MESSAGE_COUNT=3" \
  -- plain-producer

kubectl -n ${NS} wait --for=condition=Ready pod/plain-producer --timeout=60s
kubectl -n ${NS} logs plain-producer
```

Erwartete Ausgabe (Reihenfolge kann variieren):

```
[plain-producer] gesendet: {"id":"o-1","product":"Schraubenzieher","quantity":1} -> partition=0 offset=0
[plain-producer] gesendet: {"id":"o-2","product":"Schraubenzieher","quantity":2} -> partition=0 offset=1
[plain-producer] gesendet: {"id":"o-3","product":"Schraubenzieher","quantity":3} -> partition=1 offset=0
[plain-producer] fertig, 3 Nachrichten gesendet
```

**Consumer:**

```
kubectl -n ${NS} delete pod plain-producer

kubectl -n ${NS} run plain-consumer --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="TOPIC=${NS}-orders-plain" \
  --env="GROUP_ID=plain-consumer-1" \
  -- plain-consumer

kubectl -n ${NS} wait --for=condition=Ready pod/plain-consumer --timeout=60s
sleep 20
kubectl -n ${NS} logs plain-consumer
```

Erwartete Ausgabe:

```
[plain-consumer] gelesen: {"id":"o-1","product":"Schraubenzieher","quantity":1} -> quantity=1
[plain-consumer] gelesen: {"id":"o-2","product":"Schraubenzieher","quantity":2} -> quantity=2
[plain-consumer] gelesen: {"id":"o-3","product":"Schraubenzieher","quantity":3} -> quantity=3
[plain-consumer] fertig, 3 Nachrichten gelesen, keine neuen mehr seit 15s -> beende
```

**Jetzt der Breaking Change:** Ein zweiter Producer benennt `quantity` in `qty` um —
niemand prueft das, es gibt keine Registry. Was jetzt gesendet wird:

```
{"id":"o-1","product":"Schraubenzieher","qty":1}
{"id":"o-2","product":"Schraubenzieher","qty":2}
{"id":"o-3","product":"Schraubenzieher","qty":3}
```

```
kubectl -n ${NS} delete pod plain-consumer

kubectl -n ${NS} run plain-producer-v2 --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="TOPIC=${NS}-orders-plain" \
  --env="SCHEMA_VERSION=v2-breaking" \
  --env="MESSAGE_COUNT=3" \
  -- plain-producer

kubectl -n ${NS} wait --for=condition=Ready pod/plain-producer-v2 --timeout=60s
kubectl -n ${NS} logs plain-producer-v2
```

Der Producer sendet klaglos weiter — kein Fehler, keine Warnung, obwohl das Feld jetzt anders heisst.

```
kubectl -n ${NS} delete pod plain-producer-v2

kubectl -n ${NS} run plain-consumer-v2 --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="TOPIC=${NS}-orders-plain" \
  --env="GROUP_ID=plain-consumer-v2" \
  -- plain-consumer

kubectl -n ${NS} wait --for=condition=Ready pod/plain-consumer-v2 --timeout=60s
sleep 20
kubectl -n ${NS} logs plain-consumer-v2
```

**Erwartete Ausgabe — der Fehler, der niemandem aufgefallen ist:**

```
[plain-consumer] gelesen: {"id":"o-1","product":"Schraubenzieher","qty":1} -> quantity=FEHLT! Feld nicht gefunden, Consumer erwartet "quantity", Producer hat es umbenannt.
[plain-consumer] gelesen: {"id":"o-2","product":"Schraubenzieher","qty":2} -> quantity=FEHLT! Feld nicht gefunden, Consumer erwartet "quantity", Producer hat es umbenannt.
[plain-consumer] gelesen: {"id":"o-3","product":"Schraubenzieher","qty":3} -> quantity=FEHLT! Feld nicht gefunden, Consumer erwartet "quantity", Producer hat es umbenannt.
```

```
kubectl -n ${NS} delete pod plain-consumer-v2
```

---

### 2.4 Schritt B — Mit Registry: kompatible Evolution vs. Breaking Change

**Producer V1 (Avro) — was wird gesendet:** dasselbe Datenmodell wie in Schritt A,
nur diesmal als Avro-Record statt als roher JSON-String:

```
{"id": "o-1", "product": "Schraubenzieher", "quantity": 1}
{"id": "o-2", "product": "Schraubenzieher", "quantity": 2}
{"id": "o-3", "product": "Schraubenzieher", "quantity": 3}
```

```
kubectl -n ${NS} run avro-producer --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="SCHEMA_REGISTRY_URL=http://schema-registry.${NS}.svc.cluster.local:8081" \
  --env="TOPIC=${NS}-orders-avro" \
  --env="SCHEMA_VERSION=v1" \
  --env="MESSAGE_COUNT=3" \
  -- avro-producer

kubectl -n ${NS} wait --for=condition=Ready pod/avro-producer --timeout=60s
kubectl -n ${NS} logs avro-producer
```

**Consumer:**

```
kubectl -n ${NS} delete pod avro-producer

kubectl -n ${NS} run avro-consumer --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="SCHEMA_REGISTRY_URL=http://schema-registry.${NS}.svc.cluster.local:8081" \
  --env="TOPIC=${NS}-orders-avro" \
  --env="GROUP_ID=avro-consumer-1" \
  -- avro-consumer

kubectl -n ${NS} wait --for=condition=Ready pod/avro-consumer --timeout=60s
sleep 20
kubectl -n ${NS} logs avro-consumer
```

Erwartete Ausgabe:

```
[avro-consumer] gelesen: id=o-1 product=Schraubenzieher quantity=1 customer=<Feld existiert in diesem Schema nicht>
[avro-consumer] gelesen: id=o-2 product=Schraubenzieher quantity=2 customer=<Feld existiert in diesem Schema nicht>
[avro-consumer] gelesen: id=o-3 product=Schraubenzieher quantity=3 customer=<Feld existiert in diesem Schema nicht>
```

**Kompatible Schema-Evolution (V2 — neues optionales Feld `customer`) — was jetzt
gesendet wird:**

```
{"id": "o-1", "product": "Schraubenzieher", "quantity": 1, "customer": "Kunde-o-1"}
{"id": "o-2", "product": "Schraubenzieher", "quantity": 2, "customer": "Kunde-o-2"}
```

```
kubectl -n ${NS} delete pod avro-consumer

kubectl -n ${NS} run avro-producer-v2 --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="SCHEMA_REGISTRY_URL=http://schema-registry.${NS}.svc.cluster.local:8081" \
  --env="TOPIC=${NS}-orders-avro" \
  --env="SCHEMA_VERSION=v2" \
  --env="MESSAGE_COUNT=2" \
  -- avro-producer

kubectl -n ${NS} wait --for=condition=Ready pod/avro-producer-v2 --timeout=60s
kubectl -n ${NS} logs avro-producer-v2
```

Die Registry akzeptiert das neue Schema (Version 2), weil `customer` optional ist
(`"default": null`). Startest du jetzt erneut den Consumer, siehst du **beide**
Versionen im selben Topic — alte Nachrichten ohne `customer`, neue mit:

```
kubectl -n ${NS} delete pod avro-producer-v2

kubectl -n ${NS} run avro-consumer-v2 --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="SCHEMA_REGISTRY_URL=http://schema-registry.${NS}.svc.cluster.local:8081" \
  --env="TOPIC=${NS}-orders-avro" \
  --env="GROUP_ID=avro-consumer-2" \
  -- avro-consumer

kubectl -n ${NS} wait --for=condition=Ready pod/avro-consumer-v2 --timeout=60s
sleep 20
kubectl -n ${NS} logs avro-consumer-v2
```

Erwartete Ausgabe (letzte zwei Zeilen mit `customer`):

```
[avro-consumer] gelesen: id=o-1 product=Schraubenzieher quantity=1 customer=<Feld existiert in diesem Schema nicht>
[avro-consumer] gelesen: id=o-2 product=Schraubenzieher quantity=2 customer=<Feld existiert in diesem Schema nicht>
[avro-consumer] gelesen: id=o-3 product=Schraubenzieher quantity=3 customer=<Feld existiert in diesem Schema nicht>
[avro-consumer] gelesen: id=o-1 product=Schraubenzieher quantity=1 customer=Kunde-o-1
[avro-consumer] gelesen: id=o-2 product=Schraubenzieher quantity=2 customer=Kunde-o-2
```

**Inkompatible Aenderung (V3 — derselbe Fehler wie in Schritt A: `quantity` wird in
`qty` umbenannt, diesmal aber mit Registry):**

```
{"id": "o-1", "product": "Schraubenzieher", "qty": 1}
```

```
kubectl -n ${NS} delete pod avro-consumer-v2

kubectl -n ${NS} run avro-producer-v3 --image=dockertrainereu/kafka-schema-demo:1.0 --restart=Never \
  --env="BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092" \
  --env="SCHEMA_REGISTRY_URL=http://schema-registry.${NS}.svc.cluster.local:8081" \
  --env="TOPIC=${NS}-orders-avro" \
  --env="SCHEMA_VERSION=v3-incompatible" \
  --env="MESSAGE_COUNT=1" \
  -- avro-producer

kubectl -n ${NS} wait --for=condition=Ready pod/avro-producer-v3 --timeout=60s || true
kubectl -n ${NS} logs avro-producer-v3
kubectl -n ${NS} get pod avro-producer-v3
```

**Erwartete Ausgabe — die Registry blockiert, bevor irgendwas produziert wird:**

```
[avro-producer] ABGEBROCHEN: Error registering Avro schema{"type":"record","name":"Order","namespace":"de.t3isp.schemademo","fields":[{"name":"id","type":"string"},{"name":"product","type":"string"},{"name":"qty","type":"int"}]}
```

```
NAME               READY   STATUS   RESTARTS   AGE
avro-producer-v3   0/1     Error    0          10s
```

Der Unterschied zu Schritt A: dort wurde `qty` klaglos durchgereicht. Hier verhindert die
Registry den Rename schon beim Registrieren — `qty` ist ein neues Pflichtfeld ohne
`default`, das alte `quantity` verschwindet komplett. Fuer die `BACKWARD`-Kompatibilitaet
(Registry-Default) muss ein neuer Consumer mit dem **alten** Schema lesbar bleiben — das
ist hier nicht der Fall, also lehnt die Registry ab.

Registrierte Versionen pruefen (nur V1 und V2 haben es geschafft):

```
kubectl -n ${NS} run verify --image=curlimages/curl --restart=Never -i --rm -- \
  curl -s http://schema-registry.${NS}.svc.cluster.local:8081/subjects/${NS}-orders-avro-value/versions
```

Erwartete Ausgabe: `[1,2]`

```
kubectl -n ${NS} delete pod avro-producer-v3
```

---

## Diskussionsfragen

1. Warum muss `SCHEMA_REGISTRY_KAFKASTORE_TOPIC` pro Teilnehmer unterschiedlich sein,
   obwohl der Kafka-Broker geteilt wird? Was wuerde ohne diese Trennung passieren?
2. Der `PlainConsumer` erkennt das fehlende Feld nur, weil er explizit danach sucht.
   Was waere, wenn `qty` zufaellig denselben Wertebereich haette wie ein anderes,
   zufaellig vorhandenes Feld? Was sagt das ueber "stille" Fehler ohne Schema-Kontrolle?
3. Die Registry hat V3 als inkompatibel abgelehnt (BACKWARD-Kompatibilitaet ist der
   Default-Modus). Waere die Aenderung mit Kompatibilitaetsmodus `NONE` durchgegangen?
   Wann waere `NONE` trotzdem sinnvoll?
4. `AvroConsumer` nutzt `GenericRecord` statt generierter `SpecificRecord`-Klassen.
   Was ist der Trade-off (Typsicherheit vs. Flexibilitaet bei mehreren Schema-Versionen)?

---

## Aufraeumen

```
kubectl -n ${NS} delete deployment schema-registry
kubectl -n ${NS} delete service schema-registry
kubectl -n ${NS} delete pod --all

kubectl -n kafka exec kafka-controller-0 -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --delete --topic ${NS}-orders-plain
kubectl -n kafka exec kafka-controller-0 -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --delete --topic ${NS}-orders-avro
kubectl -n kafka exec kafka-controller-0 -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --delete --topic _schemas-${NS}
```

Trainer, ganz am Ende (falls der Kafka-Cluster nicht fuer weitere Uebungen gebraucht wird):

```
kubectl delete namespace kafka
```

---

## Troubleshooting

| Symptom | Ursache | Fix |
|---|---|---|
| `Error from server (BadRequest): container ... is waiting to start: ContainerCreating` | Image wird noch von Docker Hub gepullt | `kubectl -n ${NS} wait --for=condition=Ready pod/<name> --timeout=60s` abwarten |
| Consumer zeigt gar nichts an | `IDLE_POLLS_TO_EXIT` (Default 5, je 3s) noch nicht erreicht | 15-20s warten, dann `kubectl logs` erneut |
| `Error registering Avro schema` beim Producer | Inkompatible Schema-Aenderung — genau das soll die Uebung zeigen | Schema pruefen: aeltere Version abwaertskompatibel halten (neue Felder mit `default`) |
| Pod-Name schon vergeben (`AlreadyExists`) | Vorheriger Test-Pod noch nicht geloescht | `kubectl -n ${NS} delete pod <name>` vor dem naechsten Schritt |
| `SLF4J: Failed to load class "StaticLoggerBinder"` | Harmlose Warnung, Logging faellt auf No-Op zurueck | Ignorieren — die Ausgaben der Uebung laufen ueber `System.out`, nicht ueber SLF4J |
