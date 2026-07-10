# Database Patterns: Gesamtübersicht

Dieses Dokument ist der zentrale Einstiegspunkt für alle Database Patterns. Jedes Pattern
wird konkret am ShopMax-Beispiel gezeigt in
[Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
hier geht es darum, **welches Pattern in welcher Situation** das richtige ist; die
verlinkten Abschnitte dort zeigen dann, wie die Migration konkret ablaeuft.

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
dann kontrolliert statt roh (siehe [Pattern 12 — Database-as-a-Service
Interface](/microservices/datenmigration-patterns-shopmax.md#12-database-as-a-service-interface)):
reine Referenzdaten-Lesezugriffe, oder ein Service bietet seine DB absichtlich als Endpunkt
an. Details: [Status quo: Shared
Database](/microservices/datenmigration-patterns-shopmax.md#status-quo-shared-database-kein-pattern-sondern-der-ausgangspunkt)
in der ShopMax-Datei.

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

Jeder Link springt direkt in den passenden Abschnitt der
[ShopMax-Datei](/microservices/datenmigration-patterns-shopmax.md).

### A. Zugriff über Service-Grenzen, ohne die Datenbank aufzuteilen

| Situation | Pattern |
|---|---|
| Ich will das Schema nicht aufteilen, aber trotzdem einen kontrollierten, stabilen Lese-Vertrag anbieten | [11 — Database View](/microservices/datenmigration-patterns-shopmax.md#11-database-view) |
| Fremde Clients (auch Legacy-Systeme) brauchen reinen Lese-Zugriff auf aktuelle Daten | [12 — Database-as-a-Service Interface](/microservices/datenmigration-patterns-shopmax.md#12-database-as-a-service-interface) |
| Ich will verhindern, dass sich das Schema unter mir verändert, obwohl noch direkt zugegriffen wird | [13 — Database Wrapping Service](/microservices/datenmigration-patterns-shopmax.md#13-database-wrapping-service) |
| Ein neuer Service entsteht, der Monolith bleibt aber vorerst Herr über die Daten | [14 — Aggregate Exposing Monolith](/microservices/datenmigration-patterns-shopmax.md#14-aggregate-exposing-monolith) |
| Ein neuer Service soll ab sofort für ein Datum verantwortlich sein | [2 — Change Data Ownership](/microservices/datenmigration-patterns-shopmax.md#2-change-data-ownership) |
| Historische **und** laufende Daten müssen schrittweise vom alten ins neue Schema | [9 — Synchronize Data in Application](/microservices/datenmigration-patterns-shopmax.md#9-synchronize-data-in-application) |
| Nicht die ganze Datenbank, sondern nur einzelne Tabellen sollen nach und nach umziehen | [10 — Tracer Write](/microservices/datenmigration-patterns-shopmax.md#10-tracer-write) |

**Kurzfassung, wenn unsicher:** Solange nur *gelesen* wird und sich die Daten selten
ändern → Database View oder DB-as-a-Service-Interface reichen meistens. Sobald ein
neuer Service die Daten **schreiben** und **selbst verantworten** soll → Change Data
Ownership + Synchronize Data in Application, und am Ende landet man ohnehin bei den
Aufteilungs-Patterns unten.

### B. Die Datenbank wirklich aufteilen

| Situation | Pattern |
|---|---|
| Ich schneide zuerst die Daten, der Code folgt später | [5 — Repository per Bounded Context](/microservices/datenmigration-patterns-shopmax.md#5-repository-per-bounded-context) + [6 — Database per Bounded Context](/microservices/datenmigration-patterns-shopmax.md#6-database-per-bounded-context) |
| Ich schneide zuerst den Code, die Daten bleiben vorerst zentral | [7 — Monolith as Data Access Layer](/microservices/datenmigration-patterns-shopmax.md#7-monolith-as-data-access-layer) |
| Ein neuer Service soll einen Teil seiner Daten selbst halten, den Rest (noch) vom Monolithen beziehen | [8 — Multischema Storage](/microservices/datenmigration-patterns-shopmax.md#8-multischema-storage) |
| Eine Tabelle wird von mehreren zukünftigen Services gebraucht und muss auseinandergezogen werden | [1 — Split Table](/microservices/datenmigration-patterns-shopmax.md#1-split-table) |
| Eine Tabelle hat einen Foreign Key auf eine Tabelle, die in einen anderen Service wandert (oder umgekehrt) | [3 — Move Foreign Key Relationship to Code](/microservices/datenmigration-patterns-shopmax.md#3-move-foreign-key-relationship-to-code) |
| Eine Tabelle hat Foreign Keys **von mehreren** anderen Tabellen gleichzeitig (Hub-Tabelle) | Kombination aus Split Table + Move Foreign Key Relationship to Code, angewendet auf *jede* eingehende Kante einzeln — siehe [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md) |

**Faustregel für die Reihenfolge bei mehreren betroffenen Tabellen:** Erst jede
*eingehende* Foreign-Key-Kante einer Tabelle per Move Foreign Key Relationship to Code
auflösen, danach erst die Tabelle physisch verschieben. Andernfalls bricht beim
Verschieben irgendeine der Kanten unkontrolliert.

### C. Gemeinsam genutzte, sich selten ändernde Referenzdaten

| Situation | Pattern |
|---|---|
| Mehrere Services brauchen dieselben, praktisch unveränderlichen Referenzdaten (Ländercodes) und Redundanz stört nicht | [15 — Duplicate Static Reference Data](/microservices/datenmigration-patterns-shopmax.md#15-duplicate-static-reference-data) |
| Redundanz ist nicht akzeptabel, ein eigener Service wäre Overkill | [16 — Static Dedicated Reference Data Schema](/microservices/datenmigration-patterns-shopmax.md#16-static-dedicated-reference-data-schema) |
| Alle Services nutzen dieselbe Programmiersprache | [17 — Static Reference Data Library](/microservices/datenmigration-patterns-shopmax.md#17-static-reference-data-library) |
| Polyglotter Stack (mehrere Programmiersprachen) oder häufigere Änderungen | [4 — Static Reference Data Service](/microservices/datenmigration-patterns-shopmax.md#4-static-reference-data-service) |

---

## Alle Patterns im Überblick

Jedes der 17 nummerierten Patterns hat in
[Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) eine
eigene Karte mit Ausgangslage, Migrationsschritten, Ergebnis, Grafik und der Einordnung
Migration-only vs. Dauerlösung.

### A. Zugriff über Service-Grenzen, ohne die Datenbank aufzuteilen

| # | Pattern | Zweck in einem Satz |
|---|---|---|
| 11 | Database View | Stellt Daten als View statt als Tabelle bereit, damit sich das dahinterliegende Schema noch ändern kann, ohne den Vertrag nach außen zu brechen. |
| 12 | Database-as-a-Service Interface | Eine dedizierte Read-Only-Datenbank als Endpunkt für Clients (z.B. Legacy-Systeme), getrennt von der internen Service-DB. |
| 13 | Database Wrapping Service | Ein Service "umwickelt" eine bestehende Datenbank, damit Zugriffe nur noch über die API laufen und die DB nicht mehr direkt verändert wird. |
| 14 | Aggregate Exposing Monolith | Der Monolith stellt Daten über API oder Event-Stream bereit, während er noch die Datenhoheit hat. |
| 2 | Change Data Ownership | Ein neuer Service übernimmt ab sofort die Schreibhoheit für ein Datum — alle anderen greifen nur noch lesend zu. |
| 9 | Synchronize Data in Application | Vier-Schritte-Rezept, um historische *und* laufende Daten schrittweise vom alten ins neue Schema zu überführen. |
| 10 | Tracer Write | Wendet Synchronize Data in Application wiederholt an — Tabelle für Tabelle, statt die ganze Datenbank in einem Rutsch umzuziehen. |

### B. Datenbank wirklich aufteilen

| # | Pattern | Zweck in einem Satz |
|---|---|---|
| 5 | Repository per Bounded Context | Pro Bounded Context ein eigener Repository-Layer im Code — Vorbereitung, bevor die Datenbank dahinter aufgeteilt wird. |
| 6 | Database per Bounded Context | Eigenes Schema pro Bounded Context, auch wenn alles noch im selben Deployment läuft. |
| 7 | Monolith as Data Access Layer | Ein neuer Service hat noch keine eigene DB und greift über eine schmale API auf den Monolithen zu. |
| 8 | Multischema Storage | Der Service speichert neue Daten schon selbst, holt ältere/noch nicht migrierte Daten weiterhin vom Monolithen. |
| 1 | Split Table | Eine Tabelle mit zwei fachlichen Verantwortlichkeiten wird spaltenweise auf zwei Zieltabellen aufgeteilt. |
| 3 | Move Foreign Key Relationship to Code | Löst einen physischen Foreign Key auf eine wandernde Tabelle in eine Anwendungsvalidierung auf. |

### C. Gemeinsam genutzte, selten sich ändernde Referenzdaten

| # | Pattern | Zweck in einem Satz |
|---|---|---|
| 15 | Duplicate Static Reference Data | Jeder Service hält seine eigene Kopie (z.B. Ländercodes) — redundant, aber unkritisch bei seltenen Änderungen. |
| 16 | Static Dedicated Reference Data Schema | Ein eigenes, gemeinsam genutztes Schema/DB nur für Referenzdaten, auf das alle direkt zugreifen. |
| 17 | Static Reference Data Library | Referenzdaten wandern in eine eingebundene Bibliothek statt in eine Datenbank. |
| 4 | Static Reference Data Service | Ein eigener REST-Service für Referenzdaten (z.B. Country-Code-Service). |

(Nummern folgen der Reihenfolge in der ShopMax-Datei, nicht der Kategorie — Anker oben
verlinken direkt in den passenden Abschnitt.)

### D. Ausgearbeitete Praxisbeispiele (Migration Schritt für Schritt)

Kombinieren mehrere der obigen Patterns zu einem vollständigen, durchgerechneten
Migrationsplan am ShopMax-Beispiel — der nächste Schritt, nachdem die Einzelpatterns bekannt
sind:

| Beispiel | Szenario | Eingesetzte Patterns |
|---|---|---|
| [Datenmigration: Notification Service](/microservices/datenmigration-notification-service.md) | Einfacher Fall: eine Tabelle, zwei ausgehende Foreign Keys, ein Zielservice | Database-per-Service, Backfill (Synchronize Data in Application Schritt 1), Dual Write, Outbox Pattern |
| [Datenmigration bei stark verzahnten Foreign Keys](/microservices/datenmigration-bestellprozess.md) | Schwieriger Fall: eine Hub-Tabelle mit vier eingehenden Foreign Keys, Aufteilung auf drei Services gleichzeitig | Move Foreign Key Relationship to Code (pro Kante), Split Table, Tracer Write, Outbox Pattern, Saga (für den Rückweg) |

## Siehe auch

  * [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md)
  * [Umgang mit Transaktionen (Saga Pattern)](/microservices/databases/patterns/database-per-service/handling-of-transactions.md)
