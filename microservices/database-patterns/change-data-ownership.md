# Pattern: Change Data Ownership

Der neue Dienst übernimmt die Verantwortung (Ownership) für ein Datum. Ab diesem
Zeitpunkt darf **nur noch der neue Service schreiben** — alle anderen (inklusive des
Monolithen) greifen nur noch lesend zu, z.B. über API oder Event.

## Wann nutzt man Change Data Ownership?

  * Sobald fachlich klar ist, welcher Bounded Context für ein Datum zuständig ist — meist
    der Schritt direkt vor dem physischen Datenbank-Split
  * Nachdem eine Übergangsphase mit [Aggregate Exposing Monolith](/microservices/database-patterns/aggregate-exposing-monolith.md)
    oder [Monolith as Data Access Layer](/microservices/database-patterns/monolith-as-data-access-layer.md)
    gezeigt hat, welche Daten der neue Service tatsächlich braucht

### Wann eher nicht

  * Solange noch mehrere Contexts dasselbe Datum gleichberechtigt schreiben müssten —
    dann ist die fachliche Grenze noch nicht sauber gezogen (siehe die Diskussion zu
    falsch geschnittenen Bounded Contexts in der
    [Musterlösung](/microservices/uebung-monolith-schneiden-musterloesung.md))

## Wie geht es danach weiter?

Sobald die Ownership fachlich feststeht, folgt in der Regel
[Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md),
um die eigentliche physische Migration durchzuführen — Change Data Ownership legt nur
fest, *wer am Ende schreiben darf*, nicht *wie die Daten dorthin kommen*.

## Verwandte Patterns

  * [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md) —
    der Migrationsmechanismus, der die neue Ownership technisch umsetzt.
  * [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md) —
    notwendig, wenn andere Tabellen per Foreign Key auf das Datum zeigen, dessen Ownership wechselt.
