# Pattern: Duplicate Static Reference Data

Jeder Service hält seine eigene Kopie von Referenzdaten (z.B. Ländercodes). Redundant,
aber unkritisch, solange sich die Daten praktisch nie ändern — die letzte Änderung an
ISO-Ländercodes liegt z.B. bei 2011.

## Wann nutzt man dieses Pattern?

  * Bei Referenzdaten, deren Änderungshäufigkeit gegen null geht, und bei denen der
    Koordinationsaufwand eines zentralen Schemas den Nutzen übersteigen würde
  * Wenn die Frage "und ist es wirklich ein Problem, wenn sich die Daten mal ändern?"
    mit "nein, das würde niemand bemerken" beantwortet werden kann

### Wann eher nicht

  * Bei Referenzdaten, die sich doch regelmäßiger ändern (Steuersätze, Preislisten) —
    dort lohnt sich [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md)
    oder ein [Static Dedicated Reference Data Schema](/microservices/database-patterns/static-dedicated-reference-data-schema.md)

## Verwandte Patterns

  * [Static Dedicated Reference Data Schema](/microservices/database-patterns/static-dedicated-reference-data-schema.md) —
    die zentrale Alternative, wenn Redundanz nicht akzeptabel ist.

## ShopMax-Beispiel

Pattern 15 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
Versandmethoden-Codes als drei unabhaengige Kopien, im Kontrast zu Pattern 4, inkl. Grafik.
