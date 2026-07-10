# Using a schema registry 

## Confluent Schema Registry (Kafka-Lösungen) 

  * Versions every single schema (versioning) 
  * data validation
  * compatibility checking
  * versioning
  * evolution

## Software (Implementation) 

  * Confluence Schema Registry

## Ohne Registry vs. mit Registry

Ohne Registry hast du keine Versionierung:

| | Ohne Registry | Mit Registry |
|--|--------------|--------------|
| Schema vorhanden | Ja | Ja |
| Kompatibilitaetspruefung | Nein | Ja |
| Versionierung | Nein | Ja |
| Schema-Evolution kontrolliert | Nein | Ja |

**Ohne Registry:**

```
V1 Schema lokal -> V2 Schema lokal
^ niemand trackt was sich geaendert hat
^ Producer und Consumer muessen manuell synchronisiert werden
^ kein Schutz vor Breaking Changes
```

**Mit Registry:**

```
V1 registriert -> ID 1
V2 registriert -> ID 2  (nur wenn kompatibel!)
^ jede Nachricht weiss welche Version sie hat
^ Registry verhindert inkompatible Aenderungen
```

Ohne Registry bist du selbst verantwortlich: Schema-Datei irgendwie verteilen, Versionen selbst tracken, Breaking Changes selbst verhindern.

## Formatvergleich: Plain JSON vs. Avro vs. Protobuf

Avro ist nicht das einzige Format, das mit der Schema Registry funktioniert. Protobuf wird
seit Confluent Platform 5.5 ebenfalls unterstuetzt. Plain JSON (ohne Schema) dient hier als
Baseline-Vergleich, da es das Format der Uebung ohne Registry ist.

| Kriterium | Plain JSON | Avro | Protobuf |
|---|---|---|---|
| Format | Text | Binaer | Binaer |
| Schema Pflicht? | Nein | Ja, immer (auch zum Serialisieren noetig) | Ja, immer (`.proto`-Datei, Compile-Zeit) |
| Schema im Payload? | Kein Schema vorhanden | Nein, nur 4-Byte Schema-ID (Schema liegt in Registry) | Nein, nur Feldnummern kodiert (Schema liegt in Registry oder `.proto`) |
| Schema-Sprache | - | Avro IDL / JSON (`.avsc`) | Protocol Buffers IDL (`.proto`) |
| Code-Generierung | Nein, dynamisch | Optional (`SpecificRecord`) oder dynamisch (`GenericRecord`) | Verpflichtend, ueber `protoc` |
| Payload-Groesse | Gross (Feldnamen als Text) | Klein (nur Werte, Feldnamen aus Schema) | Sehr klein (Feldnummern statt -namen) |
| Menschlich lesbar | Ja | Nein | Nein |
| Kompatibilitaetspruefung | Nein, es sei denn extern erzwungen | Ja, ueber Registry (BACKWARD/FORWARD/FULL) | Ja, ueber Registry (BACKWARD/FORWARD/FULL) |
| Serialisierungs-Performance | Langsam (Text-Parsing) | Schnell | Am schnellsten |
| Sprachunterstuetzung | Universal | Gut, historisch Java/Hadoop-zentriert | Sehr gut, sprachuebergreifend (Google-Standard, gRPC) |
| Typischer Einsatz | Debugging, lose gekoppelte Systeme, REST APIs | Kafka-Oekosystem (Hadoop/Spark-Herkunft) | gRPC, polyglotte Microservices, hohe Performance-Anforderungen |

**Kernaussage fuer den Vortrag:** Der Unterschied "Schema Pflicht?" ist der eigentliche Hebel,
nicht binaer vs. Text. Sowohl Avro als auch Protobuf zwingen zu einem Schema und ermoeglichen
dadurch kontrollierte Evolution ueber die Registry — Plain JSON hat schlicht keinen Mechanismus,
der eine inkompatible Aenderung ueberhaupt erkennen koennte.

Praktische Uebung dazu: [Uebung: Kafka Producer/Consumer mit und ohne Schema Registry (Java, Kubernetes)](./04-uebung-avro-vs-plain.md)
