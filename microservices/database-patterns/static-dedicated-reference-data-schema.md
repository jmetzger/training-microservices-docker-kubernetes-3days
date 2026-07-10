# Pattern: Static Dedicated Reference Data Schema

Ein dediziertes Schema für Referenzdaten (z.B. Ländercodes) in einer eigenen Datenbank.
Alle Services greifen direkt darauf zu — es gibt dort einen Vertrag, an dem sich
Strukturänderungen kritisch für alle Konsumenten auswirken.

## Nachteil

Probleme beim Ändern des Encodings in der Datenbank — eine Migration von alter auf neue
Datenbank betrifft dann alle Konsumenten gleichzeitig, nicht nur einen Service.

## Wann nutzt man dieses Pattern?

  * Wenn [Duplicate Static Reference Data](/microservices/database-patterns/duplicate-static-reference-data.md)
    zu redundant wäre (z.B. weil sich die Daten doch öfter ändern als angenommen), ein
    voller Service aber zu viel Infrastruktur-Overhead bedeuten würde

### Wann eher nicht

  * Wenn unterschiedliche Programmiersprachen im Spiel sind und ein einheitlicher
    DB-Zugriff ohnehin pro Sprache neu implementiert werden müsste — dann eher
    [Static Reference Data Library](/microservices/database-patterns/static-reference-data-library.md)
    oder [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md)

## Verwandte Patterns

  * [Duplicate Static Reference Data](/microservices/database-patterns/duplicate-static-reference-data.md) —
    die redundante, aber koordinationsfreie Alternative.
  * [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md) —
    die API-basierte Alternative statt direktem DB-Zugriff.
