# Pattern: Database View

Die Daten werden nicht als Tabelle, sondern als **View** bereitgestellt. Die View ist
dann ein öffentlicher Vertrag — Änderungen daran sind Breaking Changes für alle, die
darauf zugreifen, genau wie bei einer API.

## Wann nutzt man Database View?

  * Wenn sich das monolithische Schema (noch) nicht auseinandernehmen lässt, aber
    trotzdem ein stabiler, kontrollierter Lese-Zugriff angeboten werden soll
  * Als erster Schritt in die richtige Richtung, wenn der Aufwand für die volle
    Aufteilung (siehe [Split Table](/microservices/database-patterns/split-table.md))
    aktuell zu groß ist — die View kapselt bereits, welche Spalten/Tabellen überhaupt
    nach außen sichtbar sind

### Wann eher nicht

  * Bei sehr hoher Last auf älteren MySQL-Versionen — dort sind Performance-Probleme mit
    Views bekannt
  * Wenn ohnehin schon klar ist, dass ein echter Service samt eigener Datenbank entsteht —
    dann direkt [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md)
    verfolgen, statt eine Zwischenlösung zu bauen, die man kurz danach wieder abbaut

## Verwandte Patterns

  * [Database-as-a-Service Interface](/microservices/database-patterns/database-as-a-service-interface.md) —
    ähnliche Idee, aber als eigene, dedizierte Datenbank statt einer View im selben Schema.
  * [Aggregate Exposing Monolith](/microservices/database-patterns/aggregate-exposing-monolith.md) —
    die Alternative auf API-Ebene statt auf DB-Ebene.

## ShopMax-Beispiel

Pattern 11 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
Cart-Service liest nur eine eingeschraenkte View auf `products`, inkl. Grafik.
