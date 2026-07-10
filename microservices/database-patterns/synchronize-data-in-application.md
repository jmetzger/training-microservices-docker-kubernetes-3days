# Pattern: Synchronize Data in Application

Löst das eigentliche Kernproblem jeder Datenmigration: Wie kommen historische **und**
neu entstehende Daten schrittweise vom alten ins neue Schema, ohne dass zwischendurch
etwas verloren geht? Vier Schritte, jeder einzeln deploybar und rückrollbar.

## Schritt 1: Daten Bulk-synchronisieren

  * Einmaliger Bulk-Sync, z.B. per Batch-Job (**Backfill**)
  * Danach laufender Abgleich, z.B. über einen Change-Data-Capture-Prozess (Debezium o.ä.)

## Schritt 2: Synchrones Schreiben, noch aus dem alten Schema lesen

  * Die Anwendung schreibt bereits in beide Schemas, liest aber weiterhin nur aus dem alten
  * Erfolgt durch Deployment einer neuen Anwendungsversion

## Schritt 3: Synchrones Schreiben, aus dem neuen Schema lesen

  * Reads werden umgestellt — das neue Schema ist jetzt die Quelle der Wahrheit
  * Wieder ein eigenes Deployment, unabhängig von Schritt 2 rückrollbar

## Schritt 4: Altes Schema entfernen

  * Erst jetzt, wenn nichts mehr aus dem alten Schema liest oder schreibt, kann es
    gefahrlos entfernt werden

## Wann nutzt man dieses Pattern?

  * Immer dann, wenn eine Tabelle/ein Schema den Besitzer wechselt (siehe
    [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md))
    und dabei sowohl der historische Bestand als auch laufende Schreibvorgänge erhalten
    bleiben müssen
  * Nicht die ganze Datenbank auf einmal, sondern Tabelle für Tabelle — siehe
    [Tracer Write](/microservices/database-patterns/tracer-write.md)

## Ausgearbeitete Beispiele

Diese vier Schritte allein bleiben abstrakt — zwei vollständig durchgerechnete
ShopMax-Beispiele zeigen die konkrete Umsetzung mit SQL, Outbox Pattern und
Reihenfolge-Begründung:

  * **Einfacher Fall** (eine Tabelle, zwei ausgehende Foreign Keys, ein Zielservice):
    [Datenmigration: Notification Service](/microservices/datenmigration-notification-service.md)
  * **Schwieriger Fall** (mehrere Tabellen mit Foreign Keys auf dieselbe Tabelle,
    Aufteilung auf mehrere Services):
    [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md)

## Verwandte Patterns

  * [Tracer Write](/microservices/database-patterns/tracer-write.md) —
    wendet diese vier Schritte wiederholt an, eine Tabelle nach der anderen.
  * [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md) —
    muss vor Schritt 1 abgeschlossen sein, wenn andere Tabellen per Foreign Key auf die
    zu migrierende Tabelle zeigen.
