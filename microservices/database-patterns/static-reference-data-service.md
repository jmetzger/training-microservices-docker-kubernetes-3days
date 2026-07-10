# Pattern: Static Reference Data Service

Ein dedizierter Service (REST-API) für Referenzdaten, z.B. Ländercodes.

```
    Service        Service
   Warehouse       Finance
       \              /
        \            /
            Service
         Country Code
```

## Wann nutzt man dieses Pattern?

  * Wenn mehrere Services in unterschiedlichen Programmiersprachen auf dieselben
    Referenzdaten zugreifen müssen — HTTP ist sprachunabhängig, im Gegensatz zu
    [Static Reference Data Library](/microservices/database-patterns/static-reference-data-library.md)
  * Wenn sich die Referenzdaten häufiger ändern, als es für
    [Duplicate Static Reference Data](/microservices/database-patterns/duplicate-static-reference-data.md)
    vertretbar wäre

### Wann eher nicht

  * Bei extrem selten wechselnden Daten (Ländercodes) in einem kleinen, homogenen
    System — der Betriebsaufwand für einen eigenen Service steht dann oft in keinem
    Verhältnis zum Nutzen; [Duplicate Static Reference Data](/microservices/database-patterns/duplicate-static-reference-data.md)
    reicht

## Verwandte Patterns

  * [Static Dedicated Reference Data Schema](/microservices/database-patterns/static-dedicated-reference-data-schema.md) —
    dieselbe Zentralisierungsidee, aber über direkten DB-Zugriff statt über eine API.
