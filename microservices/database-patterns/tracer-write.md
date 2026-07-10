# Pattern: Tracer Write

Inkrementelle Verschiebung der Source of Truth — nicht die komplette Datenbank auf
einmal, sondern **Tabelle für Tabelle**. Praktisch die Klammer um
[Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md):
man wendet dessen vier Schritte wiederholt an, statt in einem großen Schnitt.

## Wann nutzt man Tracer Write?

  * Wenn ein kompletter Schema-Umzug in einem Rutsch zu riskant wäre, weil zu viele
    Tabellen gleichzeitig betroffen sind
  * Bei einem Foreign-Key-Netz, in dem mehrere Tabellen auf dieselbe Tabelle zeigen und
    in unterschiedliche Zielservices wandern — genau das Szenario im ausführlichen
    Beispiel [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md),
    wo `orders`, `order_items`, `payments`, `shipments` und `invoices` in einer bestimmten
    Reihenfolge migriert werden müssen, statt alle gleichzeitig

### Wann eher nicht

  * Bei einer einzelnen, isolierten Tabelle ohne eingehende Foreign Keys — dort reicht
    ein einmaliger Durchlauf von Synchronize Data in Application, ohne die Klammer
    "Tabelle für Tabelle" zu brauchen

## Verwandte Patterns

  * [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md) —
    der Mechanismus, der pro Tabelle wiederholt angewendet wird.
  * [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md) —
    legt fest, in welcher Reihenfolge die Tabellen migriert werden dürfen.
