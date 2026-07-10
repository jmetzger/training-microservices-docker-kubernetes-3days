# Database Patterns: Gesamtübersicht

Dieses Dokument ist der zentrale Einstiegspunkt für alle Database Patterns. Jedes
Pattern hat eine eigene Datei unter
[microservices/database-patterns/](/microservices/database-patterns/) — hier geht es
darum, **welches Pattern in welcher Situation** das richtige ist.

## Status quo: Shared Database ist der Ausgangspunkt, keine Wahl

Ein Monolith hat per Definition **eine** gemeinsame Datenbank, auf die alle Module direkt
zugreifen — das ist kein Pattern, das man auswählt, sondern der Zustand, von dem aus jede
der folgenden Migrationen startet. Jedes Pattern unten ist ein Werkzeug, um sich davon in
Richtung Database per Service zu bewegen.

**Nachteile des Status quo:** Single Point of Failure (außer im Cluster-Betrieb),
Performance-Engpässe (durch Optimierung teilweise behebbar), und vor allem
*Development-Time-Coupling*:

```
Ein Entwickler, der z.B. am OrderService arbeitet, muss Schema-Änderungen mit den
Entwicklern aller anderen Services abstimmen, die dieselben Tabellen nutzen. Diese
Kopplung und der Abstimmungsaufwand verlangsamen die Entwicklung.
```

Zwei Ausnahmefälle, in denen ein geteilter Zugriff bewusst laenger bestehen bleibt, aber
dann kontrolliert statt roh (siehe [Database-as-a-Service
Interface](/microservices/database-patterns/database-as-a-service-interface.md)): reine
Referenzdaten-Lesezugriffe, oder ein Service bietet seine DB absichtlich als Endpunkt an.
Details und wann das noch vertretbar ist: [Shared
Database](/microservices/database-patterns/shared-database.md).

Referenz: https://microservices.io/patterns/data/shared-database.html

## Zielbild: Database per Service

Die Basis-Entscheidung, bevor überhaupt ein Einzelpattern zum Einsatz kommt — kann auch
pro Service unterschiedlich ausfallen.

**Prämisse:** Ein anderer Service darf nur über die API zugreifen. Synchronisierung
erfolgt ggf. asynchron (Messaging, Saga), nicht über einen gemeinsamen DB-Zugriff.

**Umsetzung, von locker bis strikt:**

  * Private-tables-per-service — jeder Service besitzt Tabellen, auf die nur er zugreifen darf
  * Schema-per-service — jeder Service hat ein eigenes, privates Datenbankschema
  * Database-server-per-service — jeder Service hat einen eigenen Datenbankserver

**Vorteile:** maximale Unabhängigkeit, problemloses unabhängiges Deployment.

**Nachteile:** Transaktionen über Service-Grenzen hinweg funktionieren nicht mehr auf
DB-Ebene (siehe [Saga Pattern](/microservices/databases/patterns/database-per-service/handling-of-transactions.md)),
Joins über Services hinweg sind schwierig.

Referenz: https://microservices.io/patterns/data/database-per-service.html

---

## Was sollte ich zuerst aufteilen: Code oder Datenbank, oder gleichzeitig?

  * Datenbank zuerst nur dann, wenn akute Sorge wegen der Performance besteht
  * Code zuerst ist der übliche Weg (macht die Mehrheit der Teams so)
    * Vorteil: Man weiß danach genau, welche Daten der neue Service tatsächlich braucht,
      bevor man das Schema festzurrt
  * **Gleichzeitig aufteilen: auf keinen Fall** — zwei bewegliche Ziele gleichzeitig zu
    treffen (Code-Grenze *und* Datenbank-Grenze) macht Fehler schwer lokalisierbar

---

## Entscheidungshilfe: Welches Pattern wann?

### A. Zugriff über Service-Grenzen, ohne die Datenbank aufzuteilen

| Situation | Pattern |
|---|---|
| Ich will das Schema nicht aufteilen, aber trotzdem einen kontrollierten, stabilen Lese-Vertrag anbieten | [Database View](/microservices/database-patterns/database-view.md) |
| Fremde Clients (auch Legacy-Systeme) brauchen reinen Lese-Zugriff auf aktuelle Daten | [Database-as-a-Service Interface](/microservices/database-patterns/database-as-a-service-interface.md) |
| Ich will verhindern, dass sich das Schema unter mir verändert, obwohl noch direkt zugegriffen wird | [Database Wrapping Service](/microservices/database-patterns/database-wrapping-service.md) |
| Ein neuer Service entsteht, der Monolith bleibt aber vorerst Herr über die Daten | [Aggregate Exposing Monolith](/microservices/database-patterns/aggregate-exposing-monolith.md) |
| Ein neuer Service soll ab sofort für ein Datum verantwortlich sein | [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md) |
| Historische **und** laufende Daten müssen schrittweise vom alten ins neue Schema | [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md) |
| Nicht die ganze Datenbank, sondern nur einzelne Tabellen sollen nach und nach umziehen | [Tracer Write](/microservices/database-patterns/tracer-write.md) |

**Kurzfassung, wenn unsicher:** Solange nur *gelesen* wird und sich die Daten selten
ändern → Database View oder DB-as-a-Service-Interface reichen meistens. Sobald ein
neuer Service die Daten **schreiben** und **selbst verantworten** soll → Change Data
Ownership + Synchronize Data in Application, und am Ende landet man ohnehin bei den
Aufteilungs-Patterns unten.

### B. Die Datenbank wirklich aufteilen

| Situation | Pattern |
|---|---|
| Ich schneide zuerst die Daten, der Code folgt später | [Repository per Bounded Context](/microservices/database-patterns/repository-per-bounded-context.md) + [Database per Bounded Context](/microservices/database-patterns/database-per-bounded-context.md) |
| Ich schneide zuerst den Code, die Daten bleiben vorerst zentral | [Monolith as Data Access Layer](/microservices/database-patterns/monolith-as-data-access-layer.md) |
| Ein neuer Service soll einen Teil seiner Daten selbst halten, den Rest (noch) vom Monolithen beziehen | [Multischema Storage](/microservices/database-patterns/multischema-storage.md) |
| Eine Tabelle wird von mehreren zukünftigen Services gebraucht und muss auseinandergezogen werden | [Split Table](/microservices/database-patterns/split-table.md) |
| Eine Tabelle hat einen Foreign Key auf eine Tabelle, die in einen anderen Service wandert (oder umgekehrt) | [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md) |
| Eine Tabelle hat Foreign Keys **von mehreren** anderen Tabellen gleichzeitig (Hub-Tabelle) | Kombination aus Split Table + Move Foreign Key Relationship to Code, angewendet auf *jede* eingehende Kante einzeln — siehe [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md) |

**Faustregel für die Reihenfolge bei mehreren betroffenen Tabellen:** Erst jede
*eingehende* Foreign-Key-Kante einer Tabelle per Move Foreign Key Relationship to Code
auflösen, danach erst die Tabelle physisch verschieben. Andernfalls bricht beim
Verschieben irgendeine der Kanten unkontrolliert.

### C. Gemeinsam genutzte, sich selten ändernde Referenzdaten

| Situation | Pattern |
|---|---|
| Mehrere Services brauchen dieselben, praktisch unveränderlichen Referenzdaten (Ländercodes) und Redundanz stört nicht | [Duplicate Static Reference Data](/microservices/database-patterns/duplicate-static-reference-data.md) |
| Redundanz ist nicht akzeptabel, ein eigener Service wäre Overkill | [Static Dedicated Reference Data Schema](/microservices/database-patterns/static-dedicated-reference-data-schema.md) |
| Alle Services nutzen dieselbe Programmiersprache | [Static Reference Data Library](/microservices/database-patterns/static-reference-data-library.md) |
| Polyglotter Stack (mehrere Programmiersprachen) oder häufigere Änderungen | [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md) |

---

## Alle Einzelpatterns im Überblick

Die folgende Tabelle listet **jedes** Pattern mit seinem Zweck in einem Satz. Für die
Details, das "Wann"/"Wann eher nicht" und Code-Beispiele siehe die jeweils verlinkte Datei.

### A. Zugriff über Service-Grenzen, ohne die Datenbank aufzuteilen

| Pattern | Zweck in einem Satz |
|---|---|
| [Database View](/microservices/database-patterns/database-view.md) | Stellt Daten als View statt als Tabelle bereit, damit sich das dahinterliegende Schema noch ändern kann, ohne den Vertrag nach außen zu brechen. |
| [Database-as-a-Service Interface](/microservices/database-patterns/database-as-a-service-interface.md) | Eine dedizierte Read-Only-Datenbank als Endpunkt für Clients (z.B. Legacy-Systeme), getrennt von der internen Service-DB. |
| [Database Wrapping Service](/microservices/database-patterns/database-wrapping-service.md) | Ein Service "umwickelt" eine bestehende Datenbank, damit Zugriffe nur noch über die API laufen und die DB nicht mehr direkt verändert wird. |
| [Aggregate Exposing Monolith](/microservices/database-patterns/aggregate-exposing-monolith.md) | Der Monolith stellt Daten über API oder Event-Stream bereit, während er noch die Datenhoheit hat. |
| [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md) | Ein neuer Service übernimmt ab sofort die Schreibhoheit für ein Datum — alle anderen greifen nur noch lesend zu. |
| [Synchronize Data in Application](/microservices/database-patterns/synchronize-data-in-application.md) | Vier-Schritte-Rezept, um historische *und* laufende Daten schrittweise vom alten ins neue Schema zu überführen. |
| [Tracer Write](/microservices/database-patterns/tracer-write.md) | Wendet Synchronize Data in Application wiederholt an — Tabelle für Tabelle, statt die ganze Datenbank in einem Rutsch umzuziehen. |

### B. Datenbank wirklich aufteilen

| Pattern | Zweck in einem Satz |
|---|---|
| [Repository per Bounded Context](/microservices/database-patterns/repository-per-bounded-context.md) | Pro Bounded Context ein eigener Repository-Layer im Code — Vorbereitung, bevor die Datenbank dahinter aufgeteilt wird. |
| [Database per Bounded Context](/microservices/database-patterns/database-per-bounded-context.md) | Eigenes Schema pro Bounded Context, auch wenn alles noch im selben Deployment läuft. |
| [Monolith as Data Access Layer](/microservices/database-patterns/monolith-as-data-access-layer.md) | Ein neuer Service hat noch keine eigene DB und greift über eine schmale API auf den Monolithen zu. |
| [Multischema Storage](/microservices/database-patterns/multischema-storage.md) | Der Service speichert neue Daten schon selbst, holt ältere/noch nicht migrierte Daten weiterhin vom Monolithen. |
| [Split Table](/microservices/database-patterns/split-table.md) | Eine Tabelle mit zwei fachlichen Verantwortlichkeiten wird spaltenweise auf zwei Zieltabellen aufgeteilt. |
| [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md) | Löst einen physischen Foreign Key auf eine wandernde Tabelle in eine Anwendungsvalidierung auf. |

### C. Gemeinsam genutzte, selten sich ändernde Referenzdaten

| Pattern | Zweck in einem Satz |
|---|---|
| [Duplicate Static Reference Data](/microservices/database-patterns/duplicate-static-reference-data.md) | Jeder Service hält seine eigene Kopie (z.B. Ländercodes) — redundant, aber unkritisch bei seltenen Änderungen. |
| [Static Dedicated Reference Data Schema](/microservices/database-patterns/static-dedicated-reference-data-schema.md) | Ein eigenes, gemeinsam genutztes Schema/DB nur für Referenzdaten, auf das alle direkt zugreifen. |
| [Static Reference Data Library](/microservices/database-patterns/static-reference-data-library.md) | Referenzdaten wandern in eine eingebundene Bibliothek statt in eine Datenbank. |
| [Static Reference Data Service](/microservices/database-patterns/static-reference-data-service.md) | Ein eigener REST-Service für Referenzdaten (z.B. Country-Code-Service). |

### D. ShopMax wendet jedes Pattern einzeln an

Für alle 18 Patterns aus A–C gibt es eine eigene, in ShopMax verankerte Anwendungskarte
(Ausgangslage, Migrationsschritte, Ergebnis, Grafik, Einordnung Migration-only vs.
Dauerlösung): [Database Patterns anhand
ShopMax](/microservices/datenmigration-patterns-shopmax.md).

### E. Ausgearbeitete Praxisbeispiele (Migration Schritt für Schritt)

Kombinieren mehrere der obigen Patterns zu einem vollständigen, durchgerechneten
Migrationsplan am ShopMax-Beispiel — der nächste Schritt, nachdem die Einzelpatterns aus
Abschnitt D bekannt sind:

| Beispiel | Szenario | Eingesetzte Patterns |
|---|---|---|
| [Datenmigration: Notification Service](/microservices/datenmigration-notification-service.md) | Einfacher Fall: eine Tabelle, zwei ausgehende Foreign Keys, ein Zielservice | Database-per-Service, Backfill (Synchronize Data in Application Schritt 1), Dual Write, Outbox Pattern |
| [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md) | Schwieriger Fall: eine Hub-Tabelle mit vier eingehenden Foreign Keys, Aufteilung auf drei Services gleichzeitig | Move Foreign Key Relationship to Code (pro Kante), Split Table, Tracer Write, Outbox Pattern, Saga (für den Rückweg) |

## Siehe auch

  * [Alle Einzelpattern-Dateien](/microservices/database-patterns/)
  * [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md)
  * [Umgang mit Transaktionen (Saga Pattern)](/microservices/databases/patterns/database-per-service/handling-of-transactions.md)
