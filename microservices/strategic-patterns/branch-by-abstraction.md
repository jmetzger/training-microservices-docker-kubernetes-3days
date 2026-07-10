# Branch by Abstraction

## Die Ausgangslage

Im ShopMax-Monolithen greift der Bestellprozess an rund 15 Stellen direkt auf die
Lagerbestand-Tabelle zu — mal per JPA-Repository, mal per Rohzugriff über JDBC, historisch
gewachsen. Der Plan: Lagerbestand wird ein eigener **Inventory Service** mit eigener DB.

Der naheliegende Ansatz — ein langlebiger Feature-Branch, in dem alle 15 Stellen in Ruhe
umgebaut werden — hat ein bekanntes Problem: Je länger der Branch lebt, desto weiter
läuft er vom `main`-Branch weg. Merge-Konflikte häufen sich, andere Teams committen
weiter auf denselben Dateien, und am Ende steht ein Big-Bang-Merge mit hohem Risiko —
genau das, was man eigentlich vermeiden wollte.

## Die Idee

Statt in einem Branch zu arbeiten, baut man die **Abstraktion direkt in `main`** ein —
in kleinen, jederzeit deploybaren Schritten. Der Name ist etwas irreführend: Es geht
nicht um einen Git-Branch, sondern darum, den Code selbst so umzubauen, dass zwei
Implementierungen *hinter derselben Schnittstelle* koexistieren können, bis die alte
verschwindet.

## Die vier Schritte

### Schritt 1: Abstraktion für die zu ersetzende Funktionalität erstellen

Ein Interface `InventoryRepository` wird eingeführt, das die aktuelle (direkte)
Implementierung kapselt:

```java
public interface InventoryRepository {
    int getStock(String productId);
    void reserve(String productId, int quantity);
}

// Erste Implementierung: wrapt nur den bestehenden Code
public class LegacyInventoryRepository implements InventoryRepository {
    // ruft intern weiterhin die alten JPA/JDBC-Zugriffe auf
}
```

Fachlich ändert sich **nichts** — die Abstraktion ist zunächst nur eine dünne Hülle um
das, was schon da ist. Dieser Schritt ist gefahrlos deploybar.

### Schritt 2: Aufrufer auf die neue Abstraktion umstellen

Alle 15 Stellen im Bestellprozess, die bisher direkt auf Lagerbestand zugegriffen haben,
werden nach und nach umgestellt, `InventoryRepository` statt der alten Klassen zu
verwenden — Stelle für Stelle, jede einzeln committet und deploybar. Der Code läuft
weiter exakt wie vorher, nur der Aufrufweg hat sich geändert.

**Das ist der Kern des Patterns:** Diese 15 kleinen Änderungen können über Tage oder
Wochen verteilt werden, jede für sich risikolos, ohne dass zwischendurch ein
inkonsistenter Zustand entsteht — weil sich fachlich nichts ändert, nur der interne
Aufrufpfad.

### Schritt 3: Neue Implementierung der Abstraktion bauen

Jetzt erst entsteht die zweite Implementierung, die den neuen Inventory Service
anspricht:

```java
public class InventoryServiceClient implements InventoryRepository {
    public int getStock(String productId) {
        return inventoryServiceHttpClient.get("/stock/" + productId);
    }
    // ...
}
```

Diese Implementierung kann in Ruhe entwickelt und gegen den neuen Service getestet
werden — sie hat noch keinen einzigen Aufrufer in Produktion.

### Schritt 4: Abstraktion umstellen

Ein zentraler Schalter (Konfiguration, Dependency-Injection-Bean, Feature-Flag)
entscheidet, welche Implementierung `InventoryRepository` tatsächlich ist:

```java
@Bean
public InventoryRepository inventoryRepository(FeatureFlags flags) {
    return flags.isEnabled("inventory-service")
        ? new InventoryServiceClient(...)
        : new LegacyInventoryRepository(...);
}
```

Weil es nur **eine** Stelle gibt, an der umgeschaltet wird, ist der Umstieg risikoarm —
und bei Problemen sofort per Konfig-Flip rückgängig zu machen, ohne Code-Rollback.

### Schritt 5: Abstraktion aufräumen

Sobald die neue Implementierung sich bewährt hat, wird `LegacyInventoryRepository`
gelöscht. Ob das Interface `InventoryRepository` selbst bleibt oder auch verschwindet
(weil der `InventoryServiceClient` die einzige Implementierung ist), ist Geschmackssache —
oft bleibt es, weil es beim nächsten Umbau wieder nützlich ist.

## Warum man das so macht

- **Kein langlebiger Branch, keine Merge-Hölle** — jeder Schritt geht direkt in `main`
  und ist einzeln deploybar.
- **Fachliche Änderung und technischer Umbau sind getrennt** — Schritt 2 (Aufrufer
  umstellen) verändert nichts am Verhalten, nur am Aufrufweg. Das macht Code-Reviews
  einfach: "Verhält sich das noch genauso?" lässt sich leicht beantworten.
- **Ein einziger Umschaltpunkt** (Schritt 4) statt 15 einzelner Umschaltungen — das
  Risiko konzentriert sich auf einen Moment, den man bewusst steuert.
- **Rückrollbar per Konfig**, nicht per Code-Revert.

## Wann Branch by Abstraction statt Strangler Fig?

[Strangler Fig](/microservices/strategic-patterns/strangler-fig.md) leitet **externen
Traffic** um (HTTP-Proxy, Feature Toggle auf API-Ebene, Message-Routing) — es setzt
voraus, dass Aufrufer und Zielfunktion bereits über eine Grenze hinweg kommunizieren
(HTTP, Events). Branch by Abstraction arbeitet **innerhalb des Codes**, oft als
Vorstufe: Erst wird intern abstrahiert (dieses Pattern), *dann* — sobald die neue
Implementierung hinter der Abstraktion tatsächlich einen externen Service anspricht —
ist man technisch schon fast bei Strangler Fig angekommen. Bei ShopMax greift z.B.
Schritt 4 hier praktisch ineinander mit der Umleitung aus dem Strangler-Fig-Beispiel.

## Verwandte Patterns

- **[Strangler Fig](/microservices/strategic-patterns/strangler-fig.md)** — die
  Umleitung auf Ebene von HTTP/Events, meist der nächste Schritt, nachdem die
  Abstraktion steht.
- **[Parallel Run](/microservices/strategic-patterns/parallel-run.md)** — lässt sich
  in Schritt 3/4 einbauen: die neue Implementierung rechnet erst im Hintergrund mit,
  bevor sie den Schalter in Schritt 4 tatsächlich übernimmt.
- **[Decorating Collaborator](/microservices/strategic-patterns/decorator-collaborator.md)** —
  ähnliche Grundidee (Schaltstelle mit gleichem Interface), aber mit eingebautem
  Schatten-Modus und prozentualem Traffic-Ramp-up statt binärem Umschalter.
