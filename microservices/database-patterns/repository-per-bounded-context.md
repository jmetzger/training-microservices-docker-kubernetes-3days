# Pattern: Repository per Bounded Context

Im Code gibt es pro Bounded Context einen eigenen Repository-Layer, der auf "seine"
Tabellen zugreift — auch wenn diese noch in derselben physischen Datenbank liegen.

## Wann nutzt man dieses Pattern?

  * Als Vorbereitungsschritt, bevor die Datenbank dahinter überhaupt gefahrlos aufgeteilt
    werden kann — erst wenn der Zugriff im Code sauber getrennt ist, weiß man, welche
    Tabellen tatsächlich zu welchem Context gehören
  * Wenn man **zuerst den Code, dann die Daten** aufteilen will (der übliche Weg, siehe
    [Gesamtübersicht](/microservices/databases/patterns/overview.md))

### Wann eher nicht

  * Wenn bereits klar ist, dass die Daten zuerst getrennt werden sollen (z.B. wegen akuter
    Performance-Sorgen) — dann direkt mit
    [Database per Bounded Context](/microservices/database-patterns/database-per-bounded-context.md) beginnen

## Verwandte Patterns

  * [Database per Bounded Context](/microservices/database-patterns/database-per-bounded-context.md) —
    der logische nächste Schritt, sobald der Code-Zugriff getrennt ist.

## ShopMax-Beispiel

Pattern 5 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
Vorbereitung fuer den Bestellprozess-Split, inkl. Grafik.
