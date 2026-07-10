# Parallel Run

## Die Ausgangslage

ShopMax berechnet Versandkosten mit einer gewachsenen, unübersichtlichen Funktion im
Monolithen: Gewicht, Zielland, Versandart, laufende Rabattaktionen und Zoll-Regeln
fließen rein. Niemand im Team traut sich mehr, daran etwas zu ändern — aber die Logik
soll in einen eigenen **Shipping-Cost-Service** wandern, sauber getestet und unabhängig
deploybar.

Das Problem: Eine falsch berechnete Versandkostenposition merkt kein Monitoring, kein
Health-Check und kein Circuit Breaker. Sie fällt erst auf, wenn Kunden sich über falsche
Rechnungen beschweren — Tage später, oder nie. Für so eine Funktion reicht "sieht in der
Demo richtig aus" nicht als Abnahme.

## Die Idee

Statt live umzuschalten (wie beim [Strangler Fig](/microservices/strategic-patterns/strangler-fig.md)),
laufen **alter und neuer Code für dieselben Eingaben parallel** — aber nur der alte
Monolith-Code liefert das Ergebnis an den Kunden aus. Der neue Service rechnet im
Hintergrund mit, und ein Vergleichsjob (typischerweise ein Batch-Job) prüft: **kommen
beide auf dasselbe Ergebnis?**

```
Bestellung
    |
    +--> Monolith: berechneVersandkosten()  ---> Ergebnis A --> an Kunde ausgeliefert
    |
    +--> Shipping-Cost-Service: berechne()  ---> Ergebnis B --> nur geloggt

Batch-Job (naechtlich):
    vergleiche A und B fuer alle Bestellungen des Tages
    -> Abweichungen? -> Report an Team
```

Der entscheidende Unterschied zu [Decorating Collaborator](/microservices/strategic-patterns/decorator-collaborator.md):
Dort vergleicht eine Schaltstelle **live**, Request für Request, und die Umleitung
zum neuen Service passiert schrittweise über Traffic-Prozente. Bei Parallel Run läuft
der Vergleich **offline und aggregiert** — ideal für Berechnungen ohne Seiteneffekte,
bei denen man erst über Wochen Vertrauen in die Korrektheit aufbauen will, bevor
überhaupt ein einziger Kunde das neue Ergebnis zu sehen bekommt.

## Wie das konkret abläuft

1. **Beide Implementierungen existieren parallel.** Der Monolith behält seine
   Versandkosten-Berechnung. Der neue Shipping-Cost-Service bekommt dieselben
   Eingabedaten (Gewicht, Zielland, Versandart, aktive Rabattaktionen) — z.B. über
   ein Event, das der Monolith zusätzlich publiziert, oder einen synchronen
   Zweit-Aufruf, dessen Ergebnis nur geloggt wird.
2. **Ergebnisse werden protokolliert, nicht ausgeliefert.** Kein Kunde sieht je das
   Ergebnis B — nur A geht raus. Ein Fehler im neuen Service hat also **keine
   Auswirkung auf Produktion**.
3. **Ein Batch-Job vergleicht.** Nachts (oder je nach Volumen: stündlich) läuft ein
   Job über alle Bestellungen des Zeitraums und prüft, ob A und B übereinstimmen.
   Abweichungen landen als Report beim Team — mit den konkreten Bestellungen, bei
   denen es nicht gepasst hat.
4. **Ursachen klären, bis die Abweichungsrate gegen null geht.** Typischerweise sind
   die ersten Abweichungen Kanten-Fälle: Feiertagszuschläge, Sonderregionen, alte
   Rabattcodes, die der neue Service noch nicht kennt.
5. **Erst wenn die Vergleichsergebnisse über einen längeren Zeitraum sauber sind**,
   wird umgeschaltet — z.B. per [Strangler Fig](/microservices/strategic-patterns/strangler-fig.md)
   oder [Branch by Abstraction](/microservices/strategic-patterns/branch-by-abstraction.md).
   Parallel Run liefert dafür die Vertrauensbasis, ersetzt aber nicht die Umschaltung
   selbst.

## Wann Parallel Run und wann Decorating Collaborator?

| | Parallel Run | Decorating Collaborator |
|---|---|---|
| Vergleich | offline, per Batch-Job, aggregiert | live, pro Request |
| Traffic zum neuen Service | typischerweise 100 % (nur zum Mitrechnen) | schrittweise (1 % → 10 % → 100 %) |
| Passt gut zu | zustandslose Berechnungen ohne Seiteneffekte (Preise, Steuern, Versandkosten) | Abfragen/Prozesse mit Traffic-Steuerung, bei denen man live beobachten will |
| Rückschluss | "Stimmen die Ergebnisse über viele Fälle überein?" | "Ist der neue Service unter echter Last stabil und schnell genug?" |

Beide Patterns lassen sich auch kombinieren: erst Parallel Run, um Korrektheit über
historische Daten zu beweisen, danach Decorating Collaborator, um den Traffic-Umstieg
selbst kontrolliert zu steuern.

## Warum man das so macht

- **Null Risiko für den Kunden**, solange der Vergleich läuft — der neue Service liefert
  nie das tatsächliche Ergebnis aus.
- **Man deckt Kanten-Fälle auf, bevor sie in Produktion Schaden anrichten** — bei einer
  gewachsenen Funktion wie der Versandkostenberechnung gibt es fast immer Sonderfälle,
  die niemand mehr im Kopf hat.
- **Der Vergleich ist objektiv und quantifizierbar** — "99,98 % Übereinstimmung über die
  letzten 30 Tage" ist eine belastbarere Entscheidungsgrundlage als ein Bauchgefühl.

## Verwandte Patterns

- **[Strangler Fig](/microservices/strategic-patterns/strangler-fig.md)** — die
  eigentliche Umschaltung des Traffics, nachdem Parallel Run Vertrauen geschaffen hat.
- **[Decorating Collaborator](/microservices/strategic-patterns/decorator-collaborator.md)** —
  die Live-Variante des Vergleichs, mit schrittweiser Traffic-Steuerung statt Batch-Job.
