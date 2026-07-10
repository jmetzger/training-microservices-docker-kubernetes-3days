# Pattern: Monolith as Data Access Layer

Der neue Service besitzt noch **keine eigene Datenbank**. Er greift über eine schmale,
klar definierte Schnittstelle (API oder Client-Library) auf den Monolithen zu, der
weiterhin alle Daten hält.

```
Schritt 1 (Ist-Zustand):                Schritt 2 (Ziel-Zustand):

[Neuer Service]                         [Neuer Service]
      |                                       |
      | direkter DB-Zugriff                   | API-Aufruf
      v                                       v
[Monolith-DB]                           [Monolith] --DAO/Repository--> [Monolith-DB]
```

## Wann nutzt man dieses Pattern?

  * Als Zwischenschritt, wenn der neue Service schon als eigenständiges Deployment
    existieren soll, die Datenmigration selbst aber noch zu riskant oder zu aufwändig ist
  * Um den *Code*-Schnitt vom *Daten*-Schnitt zu entkoppeln — beide sollten laut
    Grundregel ohnehin nicht gleichzeitig passieren (siehe
    [Gesamtübersicht](/microservices/databases/patterns/overview.md))

### Nachteil

Der neue Service bleibt so lange von der Verfügbarkeit des Monolithen abhängig, bis die
Ownership per [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md)
tatsächlich wechselt. Das ist kein Zielzustand, sondern eine bewusst befristete
Übergangslösung.

## Verwandte Patterns

  * [Multischema Storage](/microservices/database-patterns/multischema-storage.md) —
    der nächste Schritt, sobald der Service anfängt, einen Teil der Daten selbst zu halten.
  * [Database Wrapping Service](/microservices/database-patterns/database-wrapping-service.md) —
    dieselbe Grundidee aus Sicht des Monolithen statt aus Sicht des neuen Service.

## ShopMax-Beispiel

Pattern 7 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
Shipping-Service entsteht neu und hat noch keine eigene DB, inkl. Grafik.
