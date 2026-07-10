# Strategic Patterns: Monolith schrittweise ablösen

Alle Patterns auf dieser Seite lösen dasselbe Grundproblem: Wie ersetzt man Code in
einem laufenden System, **ohne** einen riskanten Big-Bang-Rewrite zu riskieren? Die
Antwort ist immer dieselbe Grundidee — klein schneiden, schrittweise umstellen, jeder
Schritt einzeln deploybar und rückrollbar — nur an unterschiedlichen Stellen angesetzt:
am Traffic, am Code, oder am Vertrauen in die neue Implementierung.

Alle vier Patterns werden anhand desselben durchgängigen Beispiels erklärt: **ShopMax**,
der Online-Shop-Monolith aus der [Übung: Monolith schneiden](/microservices/uebung-monolith-schneiden.md).

## Die vier Patterns im Überblick

| Pattern | Setzt an bei | Kernfrage | ShopMax-Beispiel |
|---|---|---|---|
| **[Strangler Fig](/microservices/strategic-patterns/strangler-fig.md)** | externem Traffic (HTTP, Events) | Wie leite ich Aufrufe schrittweise vom Monolithen zum neuen Service um? | Notification Service herauslösen |
| **[Branch by Abstraction](/microservices/strategic-patterns/branch-by-abstraction.md)** | internem Code | Wie tausche ich eine Implementierung aus, ohne einen langlebigen Branch zu pflegen? | Lagerbestand-Zugriff auf `InventoryRepository`-Abstraktion umstellen |
| **[Parallel Run](/microservices/strategic-patterns/parallel-run.md)** | Vertrauen in Korrektheit | Liefert die neue Implementierung für dieselben Eingaben dieselben Ergebnisse? | Versandkosten-Berechnung offline vergleichen |
| **[Decorating Collaborator](/microservices/strategic-patterns/decorator-collaborator.md)** | Vertrauen + Traffic-Steuerung | Wie beobachte ich den neuen Service live, bevor ich ihm Traffic gebe? | Userdaten-Abfrage schrittweise umstellen |

## Wie die Patterns zusammenspielen

Die Patterns schließen sich nicht gegenseitig aus — in der Praxis kombiniert man sie:

```
1. Branch by Abstraction   Interne Abstraktion einziehen, Aufrufer umstellen
                                    |
2. Parallel Run            Neue Implementierung im Hintergrund mitrechnen lassen,
                            Ergebnisse per Batch-Job gegen die alte pruefen
                                    |
3. Decorating Collaborator  Schrittweise echten Traffic auf die neue
   oder Strangler Fig       Implementierung / den neuen Service umleiten
                                    |
4. Aufraeumen               Alte Implementierung aus dem Monolithen entfernen
```

Nicht jeder Umbau braucht alle vier Stufen — bei einer einfachen, zustandslosen
Funktion reicht oft Branch by Abstraction direkt gefolgt von einem Umschalt-Flag.
Bei einer riskanten, business-kritischen Berechnung (Preise, Versandkosten,
Zahlungslogik) lohnt sich der volle Weg über Parallel Run.

## Vertiefung: Datenmigration

Traffic und Code umzustellen ist nur ein Teil der Übung — sobald ein Service seine
eigene Datenbank bekommen soll, kommt die Frage dazu, wie die Daten migriert werden
(Backfill, Dual Write, Outbox Pattern). Das wird ausführlich am selben
ShopMax-Beispiel durchgespielt in
[Datenmigration: Notification Service](/microservices/datenmigration-notification-service.md).

## Siehe auch

- [Übung: Monolith schneiden — DDD, Bounded Contexts und Strangler Fig](/microservices/uebung-monolith-schneiden.md)
- [Musterlösung: Migrationsreihenfolge für ShopMax](/microservices/uebung-monolith-schneiden-musterloesung.md)
- [Weiterführende Schritte: Strangler Proxy, Outbox, Saga](/microservices/uebung-monolith-schneiden-weiterfuehrend.md)
