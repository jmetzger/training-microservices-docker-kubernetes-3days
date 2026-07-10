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

Praktische Uebung dazu: [Uebung: Kafka Producer/Consumer mit und ohne Schema Registry (Java, Kubernetes)](./04-uebung-avro-vs-plain.md)
