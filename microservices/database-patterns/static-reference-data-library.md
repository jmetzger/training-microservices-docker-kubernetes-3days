# Pattern: Static Reference Data Library

Referenzdaten (z.B. Ländercodes) wandern von der Datenbank in eine Bibliothek, die
einfach in jeden Service eingebunden wird — keine Datenbank, kein Netzwerk-Call, nur
ein Dependency-Update.

## Nachteil

Schwierig, wenn verschiedene Programmiersprachen im Einsatz sind: die Bibliothek muss
dann für jede Sprache separat gepflegt und synchron gehalten werden.

## Wann nutzt man dieses Pattern?

  * Wenn alle (oder die meisten) Services dieselbe Technologie-Stack-Sprache nutzen und
    ein Dependency-Update akzeptabel ist, um Referenzdaten zu aktualisieren

### Wann eher nicht

  * Bei einem polyglotten Stack (mehrere Programmiersprachen über die Services hinweg) —
    dort ist [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md)
    die robustere Wahl, weil er sprachunabhängig über HTTP angesprochen wird

## Verwandte Patterns

  * [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md) —
    die sprachunabhängige Alternative.

## ShopMax-Beispiel

Pattern 17 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
Bestellstatus-Codes als gemeinsame Java-Bibliothek, inkl. Grafik.
