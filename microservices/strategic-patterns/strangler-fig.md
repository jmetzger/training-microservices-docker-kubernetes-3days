# Strangler Fig Pattern

## Die Ausgangslage

ShopMax ist ein 8 Jahre alter Monolith. Der **Benachrichtigungs-Code** (E-Mail/SMS bei
Bestellbestätigung, Versand, Rechnung) soll raus in einen eigenen Service — er hat
kaum Abhängigkeiten, ist unkritisch für den Checkout und eignet sich damit als erster
Schnitt (siehe Scoring in der [Musterlösung](/microservices/uebung-monolith-schneiden-musterloesung.md)).

Die naive Lösung wäre: Code in ein neues Repo kopieren, Monolith-Code löschen, einmal
deployen — ein **Big-Bang-Rewrite**. Das Risiko: Wenn irgendwas an den 15 Aufrufstellen
im Monolithen übersehen wurde, merkt man das erst in Produktion, mit allem gleichzeitig
kaputt.

## Die Idee

> **Strangler Fig** = eine tropische Pflanze (Würgefeige), die sich um einen Baum wickelt,
> langsam wächst und ihn irgendwann vollständig ersetzt — während der Baum die ganze Zeit
> über weiterlebt und trägt.

Genauso läuft die Migration: Der Monolith bleibt die ganze Zeit **produktiv im Einsatz**.
Neuer Code wächst daneben, übernimmt Stück für Stück einzelne Zuständigkeiten, bis vom
alten Code nichts mehr übrig ist. Es gibt nie einen Moment, an dem "alles neu" ist —
und nie einen Moment, an dem etwas komplett steht.

## Die drei Bausteine

Damit der Traffic vom Monolithen zum neuen Service wandert, ohne dass Aufrufer etwas
merken, braucht es eine **Umleitung**. Dafür gibt es drei gängige Techniken:

### 1. HTTP-Proxy

Ein Proxy (z.B. NGINX, Traefik-Ingress, oder ein Istio-Sidecar) sitzt vor Monolith und
Service und entscheidet anhand der URL, wer eine Anfrage bekommt.

```
Client -> Proxy -> /api/notifications/*  -> Notification Service (neu)
              \\--> /api/*                -> ShopMax Monolith (alt)
```

**Schritte:**

1. **Proxy einfügen** — noch leitet er alles unverändert an den Monolithen weiter.
   Nichts ändert sich fachlich, aber jetzt gibt es eine zentrale Stelle zum Umschalten.
2. **Funktionalität migrieren** — der Notification Service wird gebaut und deployt,
   läuft aber parallel, ohne Traffic zu bekommen.
3. **Aufrufe umleiten** — der Proxy leitet `/api/notifications/*` auf den neuen Service um.
   Der Rest bleibt beim Monolithen.

Das ist der Ansatz, den ShopMax nutzt (siehe [Musterlösung](/microservices/uebung-monolith-schneiden-musterloesung.md),
Migrationsreihenfolge) — und technisch die Grundlage für [Weiterführende Schritte: Strangler Proxy](/microservices/uebung-monolith-schneiden-weiterfuehrend.md).

### 2. Feature Toggle

Statt eines externen Proxys entscheidet ein **Flag im Code**, welcher Pfad läuft:

```java
if (featureFlags.isEnabled("notifications-via-new-service")) {
    notificationServiceClient.send(order);
} else {
    legacyNotificationModule.send(order);
}
```

Vorteil: kein zusätzliches Infrastruktur-Teil. Nachteil: der alte Code bleibt länger im
Monolithen liegen (das `if` muss ja auch wieder aufgeräumt werden) — die Umleitung ist
Code, nicht Infrastruktur.

### 3. Über Message Broker

Wenn Monolith und Service bereits über Events kommunizieren (z.B. Kafka), lässt sich die
Umleitung **auf Nachrichtenebene** lösen, ganz ohne HTTP-Proxy:

- Der Monolith publiziert weiterhin `BestellungAufgegeben`.
- Zu Beginn: **nur der Monolith** konsumiert das Event und verschickt die Notification.
- Nach dem Umbau: **nur der neue Notification Service** konsumiert das Event — der
  Monolith ignoriert es (oder empfängt es gar nicht mehr, weil sein Consumer entfernt wurde).

Das funktioniert besonders gut, wenn die Kommunikation ohnehin schon asynchron ist —
siehe [Asynchrones Messaging](/microservices/asynchronous-messaging.md) und
[EventBus-Überblick](/microservices/eventbus/overview.md).

## Und die Datenbank?

Traffic umleiten ist nur die halbe Miete — die `notifications`-Tabelle hängt per Foreign
Key an `users` und `orders` in der Monolith-DB. Wie man **Daten** schrittweise migriert
(Backfill, Dual Write, Outbox Pattern), ohne dass zwischendurch Notifications verloren
gehen, zeigt der Deep-Dive dazu ausführlich anhand desselben ShopMax-Beispiels:
[Datenmigration: Notification Service](/microservices/datenmigration-notification-service.md).

## Warum man das so macht

- **Reversibel** — jede Umleitung (Proxy-Regel, Flag, Consumer-Wechsel) lässt sich mit
  einem Konfig-Flip zurückdrehen, ohne Code-Rollback.
- **Risiko wird über Zeit verteilt** statt sich in einer Migrationsnacht zu ballen —
  ShopMax migriert über Monate, nicht in einem Wochenende.
- **Der Monolith bleibt bis zum Schluss lauffähig** — es gibt keinen Zeitpunkt, an dem
  "nichts mehr funktioniert, weil der Umbau nicht fertig ist".
- **Reihenfolge ist verhandelbar** — man schneidet zuerst, was am wenigsten Abhängigkeiten
  hat (Notification), und zuletzt, was am meisten kritischen Code bindet (Bestellprozess).

## Verwandte Patterns

- **[Decorating Collaborator](/microservices/strategic-patterns/decorator-collaborator.md)** —
  eine Verfeinerung der HTTP-Proxy-Variante: die Schaltstelle vergleicht zusätzlich
  Antworten im Schatten-Modus, bevor überhaupt umgeleitet wird.
- **[Branch by Abstraction](/microservices/strategic-patterns/branch-by-abstraction.md)** —
  wird genutzt, *bevor* überhaupt ein externer Service existiert: eine Abstraktion
  innerhalb des Monolithen selbst vorbereiten.
- **[Parallel Run](/microservices/strategic-patterns/parallel-run.md)** — statt live
  umzuleiten, laufen alter und neuer Code parallel und werden per Batch-Job verglichen.
