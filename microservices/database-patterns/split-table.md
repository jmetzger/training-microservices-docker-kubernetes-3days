# Pattern: Split Table

Eine einzelne Tabelle wird über Service-Grenzen hinweg gebraucht und muss deshalb
aufgeteilt werden — jede Spalte wandert dorthin, wo sie fachlich hingehört.

**Grund:** Tabellen, die über Service-Grenzen hinweg existieren, müssen so aufgeteilt
werden, dass am Ende jede resultierende Tabelle nur noch einem Service gehört.

```
Vorher: eine Tabelle, zwei Bounded Contexts greifen zu

products
  id, name, description         <- gehoert zu Produktkatalog
  stock_quantity, warehouse_id   <- gehoert zu Lagerbestand

Nachher: zwei Tabellen in zwei Datenbanken

Produkt-Service-DB               Lagerbestand-Service-DB
  products                         inventory
    id, name, description           product_id, stock_quantity, warehouse_id
```

## Wie, Schritt für Schritt

1. Spalten den beiden künftigen Tabellen zuordnen (fachlich, nicht technisch entscheiden)
2. Neue Tabelle für den zweiten Teil anlegen — zunächst noch in derselben Datenbank
3. Schreibzugriffe auf beide Tabellen synchron halten (siehe
   [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md),
   Schritt 1+2), bis beide vollständig befüllt sind
4. Lesezugriffe umstellen, danach die physische Trennung in zwei Datenbanken vollziehen
   ([Database per Bounded Context](/microservices/database-patterns/database-per-bounded-context.md))

## Wann nutzt man Split Table?

  * Wenn eine Tabelle offensichtlich zwei (oder mehr) fachliche Verantwortlichkeiten in
    sich trägt — typisches Erkennungszeichen: zwei verschiedene Teams pflegen
    unterschiedliche Spalten derselben Tabelle mit unterschiedlicher Änderungsfrequenz
    (siehe die Begründung, warum Lagerbestand kein Teil von Produktkatalog ist, in der
    [Musterlösung](/microservices/uebung-monolith-schneiden-musterloesung.md))

### Wann eher nicht

  * Wenn die Tabelle bereits eindeutig einem einzigen Bounded Context gehört — dann ist
    das Problem eher ein Foreign Key auf eine *andere* Tabelle, siehe
    [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md)

## Verwandte Patterns

  * [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md) —
    wird oft direkt im Anschluss nötig, weil andere Tabellen per Foreign Key auf eine der
    beiden neuen Tabellen zeigen.
  * Ausführliches Beispiel mit mehreren Foreign Keys gleichzeitig:
    [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md)

## ShopMax-Beispiel

Pattern 1 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
Product-Service vs. Inventory-Service, inkl. Grafik und Migrationsschritten.
