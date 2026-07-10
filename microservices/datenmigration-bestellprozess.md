# Datenmigration bei stark verzahnten Foreign Keys: Order-Service, Payment-Service, Customer-Service

Die [Datenmigration des Notification Service](/microservices/datenmigration-notification-service.md)
zeigt den einfachen Fall: eine Tabelle, zwei ausgehende Foreign Keys, ein Zielservice.
In der [Musterlösung](/microservices/uebung-monolith-schneiden-musterloesung.md) steht aber auch,
warum **Bestellprozess zuletzt** geschnitten wird:

> Geteilte DB mit Foreign Keys zu fast allen Tabellen.

Dieses Dokument zeigt den schwierigen Fall: eine Tabelle (`orders`), auf die **mehrere andere
Tabellen** per Foreign Key zeigen — und die gleichzeitig in **drei verschiedene Services**
aufgeteilt werden muss. Hier reicht "Backfill, Dual Write, Cutover" pro Tabelle nicht mehr aus,
weil die Reihenfolge zwischen den Tabellen selbst zum Problem wird.

---

## Ausgangslage: Ein Foreign-Key-Netz statt einer einzelnen Kante

Produktkatalog und Lagerbestand sind laut Musterlösung bereits eigene Services (Phase 2).
Übrig im Monolithen: `customers`, `orders`, `order_items`, `payments`, `shipments`, `invoices`.

```sql
-- Monolith-DB (PostgreSQL)

CREATE TABLE customers (
    id      BIGSERIAL PRIMARY KEY,
    email   VARCHAR(255) NOT NULL,
    phone   VARCHAR(50)
);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT NOT NULL REFERENCES customers(id),
    status        VARCHAR(20) NOT NULL,   -- NEU, BEZAHLT, VERSENDET, ABGESCHLOSSEN, STORNIERT
    total_amount  NUMERIC(10,2) NOT NULL,
    currency      VARCHAR(3) NOT NULL DEFAULT 'EUR',
    placed_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    product_id  BIGINT NOT NULL,           -- kein FK mehr moeglich: products liegt bereits
                                            -- im Produkt-Service (siehe "bereits geloest" unten)
    quantity    INT NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL
);

CREATE TABLE payments (
    id        BIGSERIAL PRIMARY KEY,
    order_id  BIGINT NOT NULL REFERENCES orders(id),
    method    VARCHAR(20) NOT NULL,        -- KREDITKARTE, PAYPAL, RECHNUNG
    amount    NUMERIC(10,2) NOT NULL,
    status    VARCHAR(20) NOT NULL,        -- OFFEN, ERFOLGREICH, FEHLGESCHLAGEN, ERSTATTET
    paid_at   TIMESTAMP
);

CREATE TABLE shipments (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders(id),
    carrier      VARCHAR(50),
    tracking_no  VARCHAR(100),
    shipped_at   TIMESTAMP,
    delivered_at TIMESTAMP
);

CREATE TABLE invoices (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    invoice_no  VARCHAR(50) NOT NULL UNIQUE,
    issued_at   TIMESTAMP NOT NULL,
    pdf_url     VARCHAR(255)
);
```

```
                       products (extern, Produkt-Service)
                            ^
                            : (kein FK mehr, s.u.)
                            :
customers ---FK--- orders <---FK--- order_items
(wird auch          ^  ^  ^
 extrahiert)         \ |   \
                      \|    \
                   FK  |     FK
                       |
                  payments   shipments   invoices
                  (Payment-  (bleibt zu- (bleibt zu-
                   Service)   naechst      naechst
                              im Order-    im Order-
                              Service)     Service)
```

**Der Unterschied zum Notification Service:** Dort zeigte eine Tabelle *nach außen* auf zwei
andere (`user_id`, `order_id`) — die Richtung der Migration war eindeutig. Hier zeigen
**vier Tabellen gleichzeitig auf `orders`**, und `orders` selbst zeigt auf `customers`. Man
kann `orders` nicht verschieben, ohne vorher zu klären, was mit jeder einzelnen eingehenden
Kante passiert — und diese Kanten gehen an unterschiedliche Ziele:

| Tabelle | Zeigt auf | Ziel-Service | Beziehung zu `orders` nach der Migration |
|---|---|---|---|
| `order_items` | `orders`, `products` | **Order-Service** (mit `orders`) | bleibt intern — beide Tabellen wandern zusammen |
| `shipments` | `orders` | **Order-Service** (mit `orders`) | bleibt intern — wandert mit |
| `invoices` | `orders` | **Order-Service** (mit `orders`) | bleibt intern — wandert mit |
| `payments` | `orders` | **Payment-Service** (eigener Service — PCI-DSS-Scope isolieren) | wird zur **service-übergreifenden** Kante |
| `orders` | `customers` | — (`orders` selbst wandert zum Order-Service) | `customers` wird zur **service-übergreifenden** Kante |

Nur eine von fünf Kanten ist wirklich hart: `payments.order_id → orders.id`. Die drei
Kind-Tabellen `order_items`, `shipments`, `invoices` wandern einfach *mit* `orders` in dieselbe
Datenbank — ihre Foreign Keys bleiben lokale Foreign Keys, nur eben in der neuen Order-Service-DB.
Das ist kein Migrationsproblem, sondern ein reines "Tabellen gemeinsam kopieren".

**Bereits gelöst (Referenz auf frühere Phase):** `order_items.product_id` zeigte ursprünglich
per Foreign Key auf `products`. Als der Produkt-Service in Phase 2 extrahiert wurde, musste
genau dieser Constraint zuerst mit **Move Foreign Key Relationship to Code**
(siehe [Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md)) aufgelöst werden —
sonst hätte `products` nie in eine eigene Datenbank wandern können, solange `order_items` noch
im selben Schema lag. Das Muster ist hier bereits bekannt; es wiederholt sich weiter unten für
`payments`.

---

## Grundregel: Erst alle eingehenden Kanten kappen, dann den Knoten verschieben

Bei einem Hub wie `orders`, auf den mehrere Tabellen zeigen, gilt eine einfache Reihenfolge-Regel:

> **Man darf eine Tabelle erst physisch in eine andere Datenbank verschieben, wenn jede
> eingehende Foreign-Key-Kante vorher auf Code-Ebene abgesichert ist** (Pattern: *Move Foreign
> Key Relationship to Code*). Sonst zerbricht beim Verschieben irgendeine der Kanten, ohne
> dass es vorher getestet wurde.

Für `orders` heißt das konkret: **bevor** `orders` in die Order-Service-DB wandert, müssen
`order_items`, `payments`, `shipments` und `invoices` bereits ohne physischen Datenbank-Constraint
gegen `orders` validieren — auch wenn drei davon (Order-Items, Shipments, Invoices) am Ende
wieder in derselben Datenbank landen wie `orders`. Der Grund: Man will die Anwendung *vor* dem
Umzug beweisen sehen, nicht danach. Ein fehlgeschlagener DB-Constraint in Produktion nach dem
Umzug ist teurer zu debuggen als eine Anwendungsvalidierung, die man vorher in Ruhe getestet hat.

---

## Migrationsplan Schritt für Schritt

### Schritt 1 — FK-Graph aufzeichnen, Zielservice pro Tabelle festlegen

Die Tabelle oben *ist* Schritt 1: für jede Tabelle im betroffenen Teilbereich des Schemas
klären, in welche Datenbank sie wandert. Ohne diesen Schritt migriert man Tabelle für Tabelle
"wie es gerade kommt" und merkt erst mittendrin, dass zwei Tabellen, die zusammengehören,
in verschiedenen Phasen verschoben wurden.

### Schritt 2 — Move Foreign Key Relationship to Code (für jede eingehende Kante)

Noch bevor irgendetwas physisch verschoben wird: der DB-Constraint wird entfernt, die
Anwendung übernimmt die Prüfung. Das passiert für **alle vier** Tabellen, die auf `orders`
zeigen — unabhängig davon, ob sie am Ende im selben Service landen oder nicht.

```sql
-- Noch in der Monolith-DB, alle Tabellen noch im selben Schema
ALTER TABLE order_items DROP CONSTRAINT order_items_order_id_fkey;
ALTER TABLE payments    DROP CONSTRAINT payments_order_id_fkey;
ALTER TABLE shipments   DROP CONSTRAINT shipments_order_id_fkey;
ALTER TABLE invoices    DROP CONSTRAINT invoices_order_id_fkey;
```

```java
// Payment-Modul im Monolithen: Pruefung wandert von der DB in die Anwendung
public Payment createPayment(Long orderId, PaymentRequest request) {
    if (!orderRepository.existsById(orderId)) {   // ersetzt den weggefallenen FK-Constraint
        throw new OrderNotFoundException(orderId);
    }
    // ... Zahlung anlegen
}
```

**Warum das *vor* dem physischen Split passiert:** Solange `payments` und `orders` noch in
derselben Datenbank liegen, kann man die neue Anwendungsvalidierung mit echtem Produktions-Traffic
beweisen — inklusive Monitoring auf `OrderNotFoundException`. Findet sich dabei ein Bug in der
Validierung, betrifft das noch eine einzige Datenbank, kein verteiltes System. Erst wenn dieser
Schritt seit einiger Zeit unauffällig läuft, folgt der physische Umzug.

### Schritt 3 — Order-Service extrahieren (Backfill, Outbox, Cutover)

Mechanik identisch zum [Notification Service](/microservices/datenmigration-notification-service.md):
neue DB aufsetzen (Database-per-Service), historische Daten per Backfill kopieren, Schreibzugriffe
über Outbox Pattern auf zwei Ziele bringen, dann Reads umstellen. `order_items`, `shipments` und
`invoices` wandern **gemeinsam mit `orders`** in dieselbe neue Datenbank — es sind nach Schritt 2
nur noch lose Referenzen (kein DB-Constraint mehr), aber sie bleiben lokale Tabellen im selben
Service, weil kein anderer Bounded Context sie beansprucht.

```
[Monolith-DB]                          [Order-Service-DB]
  orders          --Backfill+Outbox-->   orders
  order_items     --Backfill+Outbox-->   order_items
  shipments       --Backfill+Outbox-->   shipments
  invoices        --Backfill+Outbox-->   invoices
  payments (bleibt vorerst hier, Schritt 2 bereits erledigt)
  customers (bleibt vorerst hier, wandert in Schritt 5)
```

Der Order-Service veröffentlicht ab jetzt zuverlässig (Outbox-gestützt) das Event
`BestellungAufgegeben`, sobald eine neue Order angelegt wird — inklusive der Felder, die andere
Services brauchen (`orderId`, `customerId`, `totalAmount`, `currency`).

> **Warum das der wichtigste Meilenstein im ganzen Plan ist:** Payment-Service (Schritt 4) kann
> erst extrahiert werden, *nachdem* dieses Event zuverlässig fließt. Ohne verlässlichen
> `BestellungAufgegeben`-Strom hätte der neue Payment-Service keine Möglichkeit, seinen eigenen
> Order-Datenbestand aufzubauen (siehe Schritt 4). Die Reihenfolge ist also nicht verhandelbar:
> **Order-Service zuerst, weil Payment-Service von seinen Events abhängt — nicht umgekehrt.**

### Schritt 4 — Payment-Service extrahieren: die eine wirklich harte Kante

Hier unterscheidet sich der Fall vom Notification Service: `payments.order_id` verweist nach
dem Split auf eine Tabelle in einer **fremden** Datenbank. Ein SQL-`JOIN` gegen `orders` ist
danach unmöglich — der Payment-Service darf ohnehin nicht in eine fremde Datenbank schauen.

**Lösung: lokales Projektions-Cache, gefüttert durch das Event aus Schritt 3.**

```sql
-- Payment-Service-DB (eigene PostgreSQL-Instanz)

CREATE TABLE payments (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id  BIGINT NOT NULL,     -- keine FK mehr moeglich, nur noch eine Referenz-ID
    method    VARCHAR(20) NOT NULL,
    amount    NUMERIC(10,2) NOT NULL,
    status    VARCHAR(20) NOT NULL,
    paid_at   TIMESTAMP
);

-- Lokale, bewusst schmale Kopie dessen, was Payment ueber eine Order wissen muss.
-- Kein Ersatz fuer die Order-Service-DB, nur ein Lesecache fuer Validierung/Anzeige.
CREATE TABLE orders_projection (
    order_id      BIGINT PRIMARY KEY,
    customer_id   BIGINT NOT NULL,
    total_amount  NUMERIC(10,2) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);
```

```java
// Payment-Service konsumiert BestellungAufgegeben und pflegt seine eigene Projektion
@EventListener
public void on(BestellungAufgegebenEvent event) {
    ordersProjectionRepository.upsert(
        event.orderId(), event.customerId(), event.totalAmount(),
        event.currency(), "NEU", now()
    );
}

public Payment createPayment(Long orderId, PaymentRequest request) {
    OrderProjection order = ordersProjectionRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));  // ersetzt den FK, jetzt gegen die lokale Kopie
    if (!order.totalAmount().equals(request.amount())) {
        throw new PaymentAmountMismatchException(orderId);
    }
    // ... Zahlung anlegen
}
```

**Was sich dadurch inhaltlich ändert:** Die Prüfung "existiert diese Order wirklich?" prüft
jetzt gegen eine **eventually consistent** Kopie, nicht gegen die Quelle der Wahrheit. Trifft
eine Zahlungsanfrage ein, bevor das `BestellungAufgegeben`-Event verarbeitet wurde (Millisekunden
Verzögerung durch die Outbox aus Schritt 3), schlägt die Prüfung fälschlich fehl. In der Praxis
löst man das über Retry mit kurzem Backoff auf Client-Seite — das ist derselbe Kompromiss, den
das Outbox Pattern beim Notification Service bereits eingeht (siehe dort: *"eventually consistent,
nicht sofort konsistent"*).

**Und der umgekehrte Weg — Payment meldet zurück an Order?** Wenn eine Zahlung erfolgreich war,
muss `orders.status` auf `BEZAHLT` wechseln — aber `orders` liegt jetzt in einer anderen
Datenbank. Ein lokales `UPDATE orders SET status = ...` ist nicht mehr möglich. Das ist exakt
das Problem, das der [Saga-Überblick](/microservices/databases/patterns/database-per-service/handling-of-transactions.md)
behandelt: Payment-Service veröffentlicht `ZahlungErfolgt`, Order-Service konsumiert es und
aktualisiert seinen eigenen Status. Schlägt ein Schritt fehl, greift dort die
Kompensationstransaktion — nicht dieses Dokument hier, das sich auf die *Datenmigration*
konzentriert.

### Schritt 5 — Customer-Service extrahieren (dieselbe Kante, andere Richtung)

`orders.customer_id → customers.id` ist strukturell dieselbe Aufgabe wie Schritt 4, nur mit
vertauschten Rollen: jetzt ist `orders` die Tabelle mit der ausgehenden Kante, nicht der Tabelle,
auf die verwiesen wird. Der Order-Service braucht nach dem Split nur eine schmale Kopie der
Kundendaten (`email`, ggf. `customerTier` für Rabattlogik) — exakt das Muster aus dem
Notification-Service-Beispiel: **Denormalisierung statt Foreign Key.**

```sql
-- Order-Service-DB: customer_id bleibt eine reine Referenz-ID, kein FK
ALTER TABLE orders DROP CONSTRAINT orders_customer_id_fkey;  -- laengst geschehen (Schritt 2)

-- Bei Bedarf: schmale Projektion, gefuellt durch KundeRegistriert / ProfilAktualisiert
CREATE TABLE customers_projection (
    customer_id  BIGINT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    tier         VARCHAR(20)
);
```

Weil dieselbe Technik wie in Schritt 4 zum Einsatz kommt (Event-Konsum, lokale Projektion),
wird sie hier nicht erneut im Detail hergeleitet — der Unterschied ist nur, welche der beiden
Seiten der Kante die Projektion braucht.

### Schritt 6 — Alte Tabellen und Constraints im Monolithen entfernen

Erst wenn alle drei Services (Order, Payment, Customer) produktiv sind, alle Reads umgestellt
sind und kein Relay-Prozess mehr offene Outbox-Einträge hat:

```sql
-- Kontrolle: keine offenen Eintraege mehr
SELECT COUNT(*) FROM order_outbox    WHERE relayed_at IS NULL;  -- muss 0 sein
SELECT COUNT(*) FROM payment_outbox  WHERE relayed_at IS NULL;  -- muss 0 sein
SELECT COUNT(*) FROM customer_outbox WHERE relayed_at IS NULL;  -- muss 0 sein

DROP TABLE orders, order_items, shipments, invoices, payments, customers CASCADE;
```

---

## Warum genau diese Reihenfolge und nicht eine andere?

```
Schritt 1: FK-Graph + Zielservice pro Tabelle    -- Uebersicht, bevor irgendwas passiert
Schritt 2: Move FK to Code (alle 4 Kanten)       -- Anwendungsvalidierung beweisen, DB-Constraint weg
Schritt 3: Order-Service extrahieren             -- inkl. order_items/shipments/invoices (wandern mit)
                                                     -- veroeffentlicht BestellungAufgegeben zuverlaessig
Schritt 4: Payment-Service extrahieren           -- braucht das Event aus Schritt 3 als Datenquelle
Schritt 5: Customer-Service extrahieren          -- gleiches Muster, andere Richtung der Kante
Schritt 6: Alte Tabellen im Monolithen loeschen  -- erst wenn alle Outbox-Queues leer sind
```

Die entscheidenden Abhängigkeiten:

- **Schritt 2 vor Schritt 3/4/5:** Man verschiebt nie eine Tabelle, solange noch ein
  physischer Foreign Key auf sie zeigt, den man nicht kontrolliert hat. Sonst bricht der
  Constraint beim Verschieben, statt vorher in Ruhe getestet zu werden.
- **Schritt 3 vor Schritt 4:** Payment-Service braucht das `BestellungAufgegeben`-Event als
  Datenquelle für seine Projektion — das Event existiert erst, nachdem Order-Service produktiv
  ist. Wer Payment zuerst extrahiert, extrahiert einen Service ohne Datenquelle.
- **Schritt 4 und Schritt 5 sind untereinander vertauschbar** — beide hängen nur von Schritt 3
  ab, nicht voneinander. Sie könnten auch parallel laufen, wenn genug Teamkapazität da ist.

**Die allgemeine Lehre für jedes Foreign-Key-Netz, nicht nur für ShopMax:** Zeichne den Graphen,
kappe *alle* eingehenden Kanten einer Tabelle auf Code-Ebene, bevor du sie verschiebst — und
migriere danach in der Reihenfolge, in der die Tabellen selbst voneinander abhängen (die
Quelle eines Events vor ihren Konsumenten), nicht in der Reihenfolge, die organisatorisch am
bequemsten wäre.

> **Faustregel:** Bei einem Hub mit N eingehenden Foreign Keys sind das nicht "eine Migration",
> sondern **N+1 einzelne Migrationsentscheidungen** (eine pro Kante, plus die Tabelle selbst) —
> jede für sich mit Backfill/Outbox/Cutover absicherbar, aber nur in einer Reihenfolge, die
> die Abhängigkeitsrichtung der Events respektiert.

## Verwandte Patterns

- **[Move Foreign Key Relationship to Code](/microservices/database-patterns/move-foreign-key-relationship-to-code.md)** —
  der Kernschritt, um überhaupt erst physisch trennen zu können.
- **[Datenmigration: Notification Service](/microservices/datenmigration-notification-service.md)** —
  dieselbe Backfill/Outbox-Mechanik für den einfachen Fall (eine Tabelle, klare Richtung).
- **[Saga Pattern / Umgang mit Transaktionen](/microservices/databases/patterns/database-per-service/handling-of-transactions.md)** —
  wie `ZahlungErfolgt` den Order-Status service-übergreifend konsistent hält, inklusive
  Kompensationstransaktionen bei Fehlern.
- **[Strangler Fig Pattern](/microservices/strategic-patterns/strangler-fig.md)** —
  wie der Code (nicht die Daten) parallel umgeschaltet wird.
