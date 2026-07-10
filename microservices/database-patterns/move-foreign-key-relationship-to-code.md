# Pattern: Move Foreign Key Relationship to Code

Eine Tabelle hat einen Foreign Key auf eine Tabelle, die in eine andere Datenbank wandert
(oder von dort wegwandert). Ein physischer Foreign Key kann per Definition nicht über
zwei Datenbanken hinweg existieren — der Constraint muss also weg, *bevor* der physische
Split passiert.

```
Vorher (eine Datenbank, echter Constraint):

order_items.product_id  --FK-->  products.id

Nachher (zwei Datenbanken, nur noch eine Referenz-ID):

order_items.product_id  --(keine FK mehr, nur Text/Zahl)-->  products.id
                                                              (in fremder Datenbank)
```

## Wie, Schritt für Schritt

1. DB-Constraint entfernen (`ALTER TABLE ... DROP CONSTRAINT ...`), Spalte bleibt bestehen
2. Anwendungsvalidierung einbauen, die die Rolle des Constraints übernimmt
   (`existsById(...)`-Prüfung im Code statt im Schema)
3. Diese Validierung im **noch ungetrennten** Schema unter Produktions-Traffic beweisen —
   erst danach folgt der physische Umzug einer der beiden Tabellen
4. Nach dem Umzug: falls die abhängige Seite mehr über den referenzierten Datensatz wissen
   muss, als nur seine ID (z.B. für Anzeige oder weitergehende Validierung), braucht sie
   eine eigene, schmale Projektion — gefüllt über Events, nicht über JOIN

## Wann nutzt man dieses Pattern?

  * Immer, wenn eine Tabelle mit Foreign Key auf eine andere Tabelle zeigt, die den
    Service wechselt — egal ob die referenzierende oder die referenzierte Tabelle wandert

### Die Hub-Situation: mehrere Tabellen zeigen auf dieselbe Tabelle

Bei **mehreren** Tabellen, die auf dieselbe Tabelle zeigen, muss dieser Schritt für
**jede einzelne** eingehende Kante wiederholt werden, bevor die Hub-Tabelle selbst
verschoben werden darf:

> Man darf eine Tabelle erst physisch in eine andere Datenbank verschieben, wenn jede
> eingehende Foreign-Key-Kante vorher auf Code-Ebene abgesichert ist. Sonst zerbricht
> beim Verschieben irgendeine der Kanten, ohne dass es vorher getestet wurde.

Ein vollständig durchgerechnetes Beispiel mit vier eingehenden Kanten auf eine Tabelle,
aufgeteilt auf drei verschiedene Zielservices:
[Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md).

## Verwandte Patterns

  * [Split Table](/microservices/database-patterns/split-table.md) —
    oft die Voraussetzung dafür, dass diese Foreign Keys überhaupt erst entstehen
    (weil eine Tabelle vorher aufgeteilt wurde).
  * [Tracer Write](/microservices/database-patterns/tracer-write.md) —
    legt die Reihenfolge fest, in der mehrere so entkoppelte Tabellen migriert werden.
