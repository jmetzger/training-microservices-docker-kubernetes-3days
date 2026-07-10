# Pattern: Aggregate Exposing Monolith

Der Monolith selbst stellt Daten über einen Serviceendpunkt bereit — als API oder als
Event-Stream. Dadurch wird explizit sichtbar, welche Informationen ein neu entstehender
Service tatsächlich braucht, statt dass dieser sich die Daten selbst aus dem Schema
zusammensucht.

## Wann nutzt man Aggregate Exposing Monolith?

  * In einer Übergangsphase: Ein neuer Service ist entstanden, die Datenhoheit liegt aber
    noch beim Monolithen
  * Wenn noch unklar ist, welche Felder ein neuer Service wirklich benötigt — das
    Definieren des Aggregats/Events zwingt dazu, das explizit festzulegen, bevor
    irgendetwas physisch migriert wird

### Wann eher nicht

  * Sobald der neue Service produktiv genug ist, um selbst Schreibhoheit zu übernehmen —
    dann ist [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md)
    der nächste Schritt, nicht eine dauerhafte Abhängigkeit vom Monolithen

## Verwandte Patterns

  * [Database View](/microservices/database-patterns/database-view.md) —
    dieselbe Idee auf DB-Ebene statt auf API-/Event-Ebene.
  * [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md) —
    der Schritt danach, sobald der neue Service reif genug ist.
