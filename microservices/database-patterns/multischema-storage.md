# Pattern: Multischema Storage

Der Service speichert einen Teil seiner Daten bereits selbst. Ein anderer Teil kommt
weiterhin aus dem Monolithen (über
[Monolith as Data Access Layer](/microservices/database-patterns/monolith-as-data-access-layer.md)
oder eine der Zugriffs-Patterns aus der [Gesamtübersicht](/microservices/databases/patterns/overview.md)).

```
[Neuer Service]
      |
      +--eigene Tabellen--> [Service-DB]         (neu geschriebene/erfasste Daten)
      |
      +--API-Aufruf--------> [Monolith]--> [Monolith-DB]   (noch nicht migrierte Altdaten)
```

## Wann nutzt man dieses Pattern?

  * Wenn sich die Daten eines Bounded Context klar in "neu, ab jetzt hier erfasst" und
    "alt, noch im Monolithen" trennen lassen — z.B. neue Bestellungen im neuen Service,
    historische Bestellungen bleiben vorerst im Altsystem

### Wann eher nicht

  * Als dauerhafter Zustand — Multischema Storage ist eine Übergangsphase, kein Zielbild.
    Sie wird mit [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md)
    aufgelöst, sobald auch die historischen Daten migriert sind.

## Verwandte Patterns

  * [Monolith as Data Access Layer](/microservices/database-patterns/monolith-as-data-access-layer.md) —
    die Vorstufe, bei der der Service noch gar keine eigenen Tabellen hat.
  * [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md) —
    löst den Übergangszustand endgültig auf.
