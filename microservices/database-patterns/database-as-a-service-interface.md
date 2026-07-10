# Pattern: Database-as-a-Service Interface

Manchmal müssen Clients eine Datenbank nur **abfragen** — z.B. eine dedizierte Datenbank
als Read-Only-Endpunkt, die befüllt wird, sobald sich Daten in der zugrundeliegenden
Datenbank ändern.

Wichtig: Die nach außen angebotene Datenbank sollte von der internen, service-eigenen
Datenbank getrennt gehalten werden — sonst hat man wieder eine [Shared Database](/microservices/database-patterns/shared-database.md)
durch die Hintertür.

## Wie?

Umsetzung über eine Mapping-Engine, die aus internen Änderungen die Read-Only-Kopie aktuell
hält (technisch verwandt mit dem, was in [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md)
für den Bulk-Sync beschrieben ist).

## Wann nutzt man Database-as-a-Service Interface?

  * Wenn Legacy-Clients lesenden Zugriff benötigen und eine API-Anbindung (noch) nicht
    machbar ist — z.B. ein BI-Tool, das nur SQL sprechen kann

### Wann eher nicht

  * Wenn die Clients selbst modernisiert werden können — dann ist eine echte API
    langfristig der sauberere Vertrag, weil sie Verhalten statt nur Daten exponiert

## Verwandte Patterns

  * [Database Wrapping Service](/microservices/database-patterns/database-wrapping-service.md) —
    schränkt zusätzlich ein, *was* Clients tun dürfen, nicht nur *wie* sie lesen.
  * [Shared Database](/microservices/database-patterns/shared-database.md) —
    das unkontrollierte Gegenstück, das dieses Pattern gerade vermeiden soll.

## Referenz

  * https://microservices.io/patterns/data/database-per-service.html
