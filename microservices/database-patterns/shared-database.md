# Pattern: Shared Database

Mehrere Services greifen direkt auf dieselbe Datenbank zu. Das einfachste aller
Database-Patterns — und meist das erste, das man wieder loswerden will.

## Das Problem

Bei einer gemeinsam genutzten Datenbank ist Information-Hiding schwierig: Jeder, der
Zugriff auf das Schema hat, kann es faktisch auch verändern oder falsch nutzen. Ein
Entwickler, der z.B. am Order-Service arbeitet, muss Schema-Änderungen mit den Teams
aller anderen Services abstimmen, die dieselben Tabellen nutzen — diese Abstimmung
verlangsamt jede Weiterentwicklung (*Development-Time-Coupling*).

## Wann nutzt man Shared Database?

Nur in zwei Situationen vertretbar:

1. **Lesen statischer Referenzdaten** (Postleitzahlen, Geschlecht, Bundesländer) — Daten,
   die sich praktisch nie ändern und bei denen Kopplung kaum schadet.
2. **Ein Service bietet die Datenbank bewusst als definierten Endpunkt an** — dann aber
   besser über eine kontrollierte Alternative: [Database-as-a-Service Interface](/microservices/database-patterns/database-as-a-service-interface.md).

### Wann eher nicht

  * Sobald mehrere Teams **schreibend** auf dieselben Tabellen zugreifen — dann ist es kein
    bewusst gewähltes Pattern mehr, sondern der Grund, warum man aus einem Monolithen
    überhaupt erst Microservices schneiden will.
  * Als Zielarchitektur für neue Services — siehe [Database per Bounded Context](/microservices/database-patterns/database-per-bounded-context.md).

## Wie häufig?

Eher selten als bewusste Design-Entscheidung — meistens ein Übergangszustand während einer
Migration, kein Zielbild.

## Verwandte Patterns

  * [Database-as-a-Service Interface](/microservices/database-patterns/database-as-a-service-interface.md) —
    die kontrollierte Variante, wenn Clients wirklich nur lesen müssen.
  * [Database View](/microservices/database-patterns/database-view.md) —
    ein Vertrag statt direktem Tabellenzugriff, wenn ein voller Split (noch) nicht möglich ist.

## Referenz

  * https://microservices.io/patterns/data/shared-database.html

## ShopMax-Beispiel

Kein eigenes Pattern in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
dort der Status-quo-Rahmen ganz am Anfang: ShopMax startet mit genau dieser einen
gemeinsamen Datenbank, von der aus alle 18 Patterns wegmigrieren.
