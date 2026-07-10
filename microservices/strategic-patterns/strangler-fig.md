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

## Wann nutzt man Strangler Fig?

### Signale, die dafür sprechen

- **Das System muss während der Migration durchgehend laufen.** ShopMax hat kein
  Wartungsfenster — Bestellungen kommen 24/7 rein. Ein Cutover-Wochenende, an dem alles
  offline ist, ist keine Option.
- **Der Umfang oder das Risiko des Rewrites ist zu groß, um ihn auf einmal zu verstehen.**
  Bei ShopMax weiß niemand mehr genau, was der 8 Jahre alte Bestellprozess an Sonderfällen
  abdeckt. Ein Big-Bang-Rewrite würde all das gleichzeitig neu erraten müssen.
- **Rollback muss jederzeit einfach möglich sein.** Mit einer Umleitung (Proxy-Regel,
  Feature-Flag, Consumer-Wechsel) ist der Rückweg ein Konfig-Flip. Bei einem kompletten
  Ersatz gibt es dagegen oft keinen Weg zurück außer einem Restore aus dem Backup.
- **Es gibt eine ansprechbare Schnittstelle, an der sich umleiten lässt** — eine HTTP-Route,
  ein Topic, eine klare Domain-Grenze. Ohne so eine Schnittstelle hat man nichts, worüber
  man den Traffic überhaupt schrittweise verschieben könnte.
- **Das Team soll Vertrauen und Erfahrung aufbauen, bevor der große Rest drankommt** —
  genau deshalb schneidet ShopMax zuerst Notification (unkritisch, wenig Abhängigkeiten)
  und erst zum Schluss den Bestellprozess (siehe Scoring in der
  [Musterlösung](/microservices/uebung-monolith-schneiden-musterloesung.md)).

### Wann eher nicht

- **Das System ist klein genug für einen Big-Bang.** Ein internes Tool mit fünf Nutzern
  und einem planbaren Wartungsfenster braucht keine wochenlange Migrationsinfrastruktur —
  der Aufwand für Proxy/Feature-Flag/Doppelbetrieb würde den Nutzen übersteigen.
- **Es gibt keine sinnvolle Schnittstelle zum Umleiten.** Wenn der zu ersetzende Code so
  eng mit dem Rest des Monolithen verzahnt ist, dass man ihn nicht über eine URL, ein Topic
  oder eine Methodengrenze ansprechen kann, ist [Branch by Abstraction](/microservices/strategic-patterns/branch-by-abstraction.md)
  der richtige *erste* Schritt — es schafft überhaupt erst die Schnittstelle, an der Strangler
  Fig später ansetzen kann.
- **Ein harter Stichtag erzwingt einen Cutover zu einem festen Zeitpunkt** — z.B. weil ein
  Vertrag mit einem Altsystem-Anbieter an einem bestimmten Datum ausläuft. Man kann die
  *Vorbereitung* trotzdem schrittweise machen (inkl. [Parallel Run](/microservices/strategic-patterns/parallel-run.md)
  zur Absicherung), aber der eigentliche Umschaltmoment ist dann kein schrittweiser Ramp-up
  mehr, sondern ein einmaliger Schnitt.
- **Doppelbetrieb ist aus fachlichen Gründen nicht erlaubt** — z.B. wenn regulatorisch zu
  jedem Zeitpunkt exakt eine Quelle der Wahrheit für Buchungen existieren muss und alter
  sowie neuer Code nicht parallel Bestellungen verarbeiten dürfen.
- **Die Kosten des Parallelbetriebs übersteigen den Nutzen bei einem trivialen Feature** —
  für eine Funktion, die ohnehin in einer Stunde neu geschrieben und getestet ist, lohnt
  sich keine mehrwöchige Migrationsstrecke mit Proxy und Feature-Flags.

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

## Strangler Fig vs. Decorating Collaborator — was ist der Unterschied?

Das ist die Verwechslung, die am häufigsten auftritt: Decorating Collaborator ist
**kein eigenständiges Konkurrenz-Pattern zu Strangler Fig, sondern eine ausgearbeitete
Variante des HTTP-Proxy-Bausteins von oben.** Anders gesagt — jeder Decorating-Collaborator-Umbau
*ist* ein Strangler Fig, aber nicht jeder Strangler Fig braucht die zusätzliche Maschinerie
von Decorating Collaborator.

| | Strangler Fig (einfache Umleitung) | Decorating Collaborator |
|---|---|---|
| Umleitungsmechanismus | Proxy-Regel, Feature-Flag oder Consumer-Wechsel — meist ein einmaliger, grober Schnitt | dieselbe Schaltstelle, aber mit eingebautem Schatten-Modus und feinem Traffic-Ramp (1 % → 10 % → 100 %) |
| Vergleich alt vs. neu | keiner eingebaut — man verlässt sich auf Tests/Monitoring nach der Umleitung | Live-Vergleich Request für Request, *bevor* überhaupt echter Traffic ankommt |
| Aufwand | gering — ein Proxy-Rule-Eintrag oder ein Flag reicht | höher — die Schaltstelle muss beide Systeme parallel aufrufen, Antworten vergleichen und den Traffic-Anteil steuern können |
| Passt gut zu | unkritischer, gut verstandener Funktionalität, bei der ein Fehler schnell auffällt und wenig kostet — bei ShopMax: **Notification** (kein kritischer Pfad, Ausfall = keine E-Mail, keine Bestellungsfehler) | kritischer oder komplexer Funktionalität, bei der ein falsches Ergebnis teuer oder schwer zu bemerken wäre — bei ShopMax: **Userdaten-Abfrage**, die an 20 Stellen im Bestellprozess hängt |

**Faustregel:** Reicht "wir schalten um und beobachten das Monitoring" als Sicherheitsnetz,
ist die einfache Strangler-Fig-Umleitung genug. Braucht man dagegen belastbare Evidenz
*vor* der Umleitung — weil ein Fehler teuer, unauffällig oder schwer rückgängig zu machen
wäre — lohnt sich der Mehraufwand von Decorating Collaborator (ggf. kombiniert mit
[Parallel Run](/microservices/strategic-patterns/parallel-run.md), um schon vor der
Umleitung Vertrauen über historische Daten aufzubauen).

## Warum man das so macht

- **Reversibel** — jede Umleitung (Proxy-Regel, Flag, Consumer-Wechsel) lässt sich mit
  einem Konfig-Flip zurückdrehen, ohne Code-Rollback.
- **Risiko wird über Zeit verteilt** statt sich in einer Migrationsnacht zu ballen —
  ShopMax migriert über Monate, nicht in einem Wochenende.
- **Der Monolith bleibt bis zum Schluss lauffähig** — es gibt keinen Zeitpunkt, an dem
  "nichts mehr funktioniert, weil der Umbau nicht fertig ist".
- **Reihenfolge ist verhandelbar** — man schneidet zuerst, was am wenigsten Abhängigkeiten
  hat (Notification), und zuletzt, was am meisten kritischen Code bindet (Bestellprozess).

## Weitere verwandte Patterns

Der Vergleich zu Decorating Collaborator steht oben — daneben spielen zwei weitere
Patterns mit rein:

- **[Branch by Abstraction](/microservices/strategic-patterns/branch-by-abstraction.md)** —
  kommt ins Spiel, *bevor* überhaupt eine externe Schnittstelle existiert: eine
  Abstraktion innerhalb des Monolithen selbst vorbereiten, an der Strangler Fig später
  ansetzen kann.
- **[Parallel Run](/microservices/strategic-patterns/parallel-run.md)** — statt live
  umzuleiten, laufen alter und neuer Code parallel und werden per Batch-Job verglichen;
  liefert die Vertrauensbasis, *bevor* man sich für Strangler Fig oder Decorating
  Collaborator entscheidet.
