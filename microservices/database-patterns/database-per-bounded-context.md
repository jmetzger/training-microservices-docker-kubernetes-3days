# Pattern: Database per Bounded Context

Vorsichtsmaßnahme, falls aus dem Bounded Context später ein eigener Service werden soll:
Auch innerhalb *eines* Deployments (z.B. noch im Monolithen) bekommt jeder Bounded
Context ein eigenes, getrenntes Schema.

## Warum schon vor dem eigentlichen Split?

Wenn das Schema pro Context von Anfang an getrennt ist, wird der spätere Umzug in eine
eigene Datenbank zu einer reinen **Infrastrukturfrage** (neue DB-Instanz, Connection
String ändern) statt einer riskanten Schema-Änderung mitten in der Migration.

## Wann nutzt man dieses Pattern?

  * Direkt im Anschluss an [Repository per Bounded Context](/microservices/database-patterns/repository-per-bounded-context.md),
    sobald der Code-Zugriff pro Context sauber getrennt ist
  * Wenn absehbar ist, dass ein Bounded Context in absehbarer Zeit ein eigener Service wird

## Verwandte Patterns

  * [Repository per Bounded Context](/microservices/database-patterns/repository-per-bounded-context.md) —
    die Code-seitige Vorstufe.
  * [Split Table](/microservices/database-patterns/split-table.md) —
    falls eine bestehende Tabelle erst noch auf mehrere Contexts aufgeteilt werden muss,
    bevor sie in getrennte Schemas wandern kann.

## ShopMax-Beispiel

Pattern 6 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
eigene Schemas fuer Order/Payment/Customer im selben Monolith-Deployment, inkl. Grafik.
