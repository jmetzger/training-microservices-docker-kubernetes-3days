# Database Patterns anhand ShopMax — Entwurf (18 von 18 Patterns)

Prototyp, um das Format zu pruefen, bevor die bestehenden Dateien danach ausgerichtet werden.
Prinzip durchgehend: **Database per Service** ist das Ziel, die einzelnen Patterns sind
die Werkzeuge, um dorthin zu migrieren. Fokus jeder Karte: **wie laeuft die Migration ab**,
nicht nur "was ist das Pattern".

ShopMax-Services (aus der Monolith-schneiden-Uebung): User, Product, Inventory, Cart,
Order, Payment, Shipping, Notification.

## Status quo: Shared Database (kein Pattern, sondern der Ausgangspunkt)

ShopMax startet mit **einer** PostgreSQL-Datenbank, auf die alle Module direkt zugreifen —
das ist keine Wahl, sondern der Zustand, den der Monolith per Definition hat. Jedes der
folgenden 18 Patterns ist ein Werkzeug, um sich davon in Richtung Database per Service zu
bewegen — Shared Database steht deshalb hier nicht als gleichwertige Auswahl neben den
anderen (siehe `microservices/database-patterns/shared-database.md` fuer die zwei
Ausnahmefaelle, in denen es bewusst als Zwischenzustand vertretbar bleibt: reine
Referenzdaten-Lese-Zugriffe, oder ein Service bietet seine DB absichtlich als Endpunkt an —
dafuer aber besser gleich [Database-as-a-Service Interface](#6-database-as-a-service-interface)
statt rohem DB-Zugriff).

---

## 1. Split Table

**Kategorie:** B — Datenbank wirklich aufteilen
**ShopMax-Fall:** Product-Service vs. Inventory-Service (real aus ShopMax ableitbar)

### Ausgangslage

Eine Tabelle traegt zwei fachlich unterschiedliche Verantwortlichkeiten gleichzeitig —
Katalog-Daten (aendern sich selten, Marketing-getrieben) und Lagerbestand (aendert sich
bei jeder Bestellung, hochfrequent):

```sql
-- Monolith-DB
CREATE TABLE products (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    price             NUMERIC(10,2) NOT NULL,
    category          VARCHAR(50),
    stock_level       INT NOT NULL,
    warehouse_location VARCHAR(50)
);
```

**Warum das ein Problem ist:** Jede Bestellung schreibt `stock_level` (hochfrequent), aber
jede Preisaenderung sperrt potenziell dieselbe Zeile. Zwei Teams (Produktkatalog-Team,
Logistik-Team) aendern staendig dieselbe Tabelle mit unterschiedlichen Zugriffsmustern.

### Migration — wie laeuft es ab

1. **Zielspalten festlegen:** `name, description, price, category` bleiben bei
   Product-Service. `stock_level, warehouse_location` wandern zu Inventory-Service —
   jeweils ergaenzt um `product_id` als reine Referenz (kein FK mehr ueber Service-Grenzen).
2. **Neue Tabelle parallel anlegen** (Inventory-Service-DB):
   ```sql
   CREATE TABLE inventory (
       product_id          BIGINT PRIMARY KEY,
       stock_level         INT NOT NULL,
       warehouse_location  VARCHAR(50)
   );
   ```
3. **Backfill:** einmaliger Kopiervorgang `stock_level`/`warehouse_location` aus der
   Monolith-`products`-Tabelle nach `inventory`.
4. **Dual Write:** Solange der Monolith noch schreibt, schreibt er in beide Tabellen
   (alte Spalte UND neue Tabelle via API-Call oder Outbox-Event `StockLevelChanged`).
5. **Cutover Reads:** Inventory-Service wird zur Quelle der Wahrheit fuer Lesezugriffe auf
   Lagerbestand. Order-Service fragt ab jetzt bei Inventory-Service nach Verfuegbarkeit,
   nicht mehr per Join gegen `products`.
6. **Alte Spalten entfernen:** `ALTER TABLE products DROP COLUMN stock_level, DROP COLUMN warehouse_location`
   — erst wenn kein Leser mehr auf die alten Spalten zugreift.

### Ergebnis

Product-Service besitzt Katalogdaten, Inventory-Service besitzt Bestandsdaten. Beide
skalieren und deployen unabhaengig — das war vorher unmoeglich, weil beide dieselbe
Tabelle sperrten.

---

## 2. Change Data Ownership

**Kategorie:** A — Zugriff ueber Service-Grenzen, ohne die Datenbank aufzuteilen
**ShopMax-Fall:** Inventory-Service uebernimmt Schreibhoheit fuer Lagerbestand (Anschluss an Pattern 1)

### Ausgangslage

Nach Pattern 1 existiert `inventory` als eigene Tabelle — aber der Monolith (genauer: das
Order-Modul) schreibt beim Bestellabschluss noch direkt hinein (`UPDATE inventory SET
stock_level = stock_level - :qty`). Das ist noch kein Ownership-Wechsel, nur ein Ortswechsel
der Daten.

### Migration — wie laeuft es ab

1. **Schreibpfad identifizieren:** Alle Stellen im Monolithen, die `inventory` schreiben
   (aktuell nur der Bestellabschluss).
2. **Inventory-Service bekommt eine Schreib-API:** `POST /inventory/{productId}/reserve`.
3. **Konsument umstellen (Order-Service statt direktem SQL-Write):** Order-Service ruft ab
   sofort die Inventory-API auf, statt selbst `UPDATE inventory` auszufuehren:
   ```java
   // vorher: direktes SQL im selben Transaktionskontext
   inventoryRepository.decrementStock(productId, qty);

   // nachher: Order-Service ist nur noch Leser/Aufrufer, nicht mehr Schreiber
   inventoryClient.reserve(productId, qty);   // HTTP-Call an Inventory-Service
   ```
4. **Alte Schreibrechte entziehen:** DB-User des Order-Service verliert `INSERT`/`UPDATE`
   auf `inventory` (`REVOKE UPDATE ON inventory FROM order_service_user`). Erst technisch
   erzwingen, was vorher nur Konvention war.
5. **Beobachten:** Fehlgeschlagene Reservierungen (z.B. Bestand reicht nicht) muessen jetzt
   als Fehlerantwort der Inventory-API zurueckkommen, nicht mehr als DB-Constraint-Fehler.

### Ergebnis

Nur noch **ein** Service darf `inventory` schreiben. Alle anderen (Order, Product, Cart)
lesen entweder per API oder — falls Performance kritisch ist — ueber eine asynchron
aktualisierte Projektion (siehe Pattern 4, Move Foreign Key Relationship to Code, fuer das
Lesemuster).

**Warum dieses Pattern und nicht z.B. gleich "Database per Service" in einem Schritt?**
Change Data Ownership klaert zuerst *wer schreiben darf*, ohne dass zwingend schon eine
physische Trennung stattgefunden hat. Das ist der kleinere, risikoaermere erste Schritt —
die physische Trennung (Pattern 1) kann sogar vorher oder nachher passieren.

---

## 3. Move Foreign Key Relationship to Code

**Kategorie:** B — Datenbank wirklich aufteilen
**ShopMax-Fall:** `order_items.product_id` verliert den Foreign Key auf `products`, als der
Product-Service extrahiert wird (real aus `datenmigration-bestellprozess.md`, hier isoliert
und ausfuehrlicher als eigenstaendiges Beispiel)

### Ausgangslage

```sql
-- Noch im Monolithen, ein Schema
CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    product_id  BIGINT NOT NULL REFERENCES products(id),   -- <- diese Kante ist das Problem
    quantity    INT NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL
);
```

`products` soll in den Product-Service wandern. Solange der physische Foreign Key besteht,
kann das nicht passieren — PostgreSQL erlaubt keinen Constraint ueber zwei Datenbanken hinweg.

### Migration — wie laeuft es ab

1. **Anwendungsvalidierung bauen, bevor der Constraint faellt:**
   ```java
   public OrderItem addItem(Long orderId, Long productId, int qty) {
       if (!productClient.exists(productId)) {   // ersetzt den kuenftig fehlenden FK
           throw new ProductNotFoundException(productId);
       }
       // ... OrderItem anlegen
   }
   ```
2. **Diese Validierung eine Zeit lang parallel zum noch bestehenden DB-Constraint laufen
   lassen** (beide aktiv) — falls die Anwendungsvalidierung einen Fall durchlaesst, den der
   DB-Constraint vorher verhindert haette, faellt das *jetzt* auf, nicht erst nach dem Umzug.
3. **DB-Constraint entfernen:**
   ```sql
   ALTER TABLE order_items DROP CONSTRAINT order_items_product_id_fkey;
   ```
4. **Erst jetzt** darf `products` physisch in die Product-Service-DB wandern (Backfill +
   Dual Write + Cutover, wie in Pattern 1 beschrieben).
5. **`product_id` bleibt in `order_items`** — aber nur noch als lose Referenz-ID, kein
   Constraint mehr, keine Moeglichkeit fuer einen SQL-`JOIN` gegen `products`.

### Ergebnis

`order_items` und `products` koennen unabhaengig voneinander migrieren. Der Preis: Die
referenzielle Integritaet, die vorher die Datenbank garantiert hat, garantiert jetzt die
Anwendung — und zwar dauerhaft, nicht nur waehrend der Migration.

**Wichtig fuer die Reihenfolge:** Dieser Schritt muss *vor* dem physischen Verschieben von
`products` passieren, nie danach. Sonst bricht der Constraint beim Verschieben unkontrolliert,
statt vorher in Ruhe getestet zu werden.

---

## 4. Static Reference Data Service

**Kategorie:** C — Gemeinsam genutzte, selten sich aendernde Referenzdaten
**ShopMax-Fall:** Waehrungscodes/Laendercodes, gebraucht von Order-, Payment- und
Shipping-Service (kein direktes Vorbild in den bestehenden ShopMax-Dokumenten — bewusst
erfunden, weil es das typische "wo gehoeren Referenzdaten hin"-Problem zeigt, das bei drei
gleichzeitig extrahierten Services zwangslaeufig auftaucht)

### Ausgangslage

Im Monolithen liegt eine `countries`-Tabelle (ISO-Code, Name, Steuersatz). Nach der
Extraktion haetten **drei** Services (Order — Versandland, Payment — Rechnungsland/Steuer,
Shipping — Zustellland) je nach Wahl von Pattern C entweder:

- eine eigene Kopie pflegen (Duplicate Static Reference Data) — Redundanz, aber unkritisch
  bei seltenen Aenderungen, oder
- einen zentralen Service befragen (dieses Pattern)

**Warum hier der zentrale Service und nicht die einfachere Kopie?** Weil `tax_rate` sich
pro Land regelmaessig aendert (Steuerrecht) und **drei unabhaengig aktualisierte Kopien**
das Risiko bergen, dass Order- und Payment-Service kurzzeitig unterschiedliche Steuersaetze
fuer dasselbe Land verwenden — genau das darf bei Zahlungen nicht passieren.

### Migration — wie laeuft es ab

1. **Country-Code-Service extrahieren** (eigene, kleine DB: `countries` Tabelle unveraendert
   uebernommen, kein Umbau noetig, weil die Daten fachlich schon abgeschlossen sind).
2. **Konsumenten schrittweise umstellen, einer nach dem anderen** (nicht alle drei
   gleichzeitig — sonst drei bewegliche Ziele parallel):
   ```java
   // vorher: lokaler Join im Monolithen
   SELECT tax_rate FROM countries WHERE iso_code = :countryCode;

   // nachher: Order-Service fragt den Country-Code-Service
   TaxInfo tax = countryCodeClient.getTaxInfo(countryCode);
   ```
3. **Payment-Service umstellen** (zweiter Konsument) — gleiche Technik, eigener Zeitpunkt.
4. **Shipping-Service umstellen** (dritter Konsument).
5. **Caching ergaenzen, sobald alle drei umgestellt sind:** Da sich `countries` selten
   aendert, cacht jeder Client lokal mit kurzer TTL (z.B. 15 Minuten) oder abonniert ein
   `CountryDataChanged`-Event — sonst wird der Country-Code-Service bei jedem Checkout
   dreifach angefragt, obwohl sich die Daten praktisch nie aendern.
6. **`countries`-Tabelle im Monolithen loeschen**, sobald kein Leser mehr direkt zugreift.

### Ergebnis

Ein Service ist alleinige Quelle der Wahrheit fuer Referenzdaten, die konsistenzkritisch
sind. Der Mehraufwand (eigener Service, Netzwerk-Calls, Caching) lohnt sich hier, weil die
Alternative (drei unabhaengige Kopien mit Aktualisierungsverzug) ein Steuerrecht-Bug waere,
keine Kleinigkeit.

---

## 5. Repository per Bounded Context

**Kategorie:** B | **ShopMax-Fall:** Vorbereitung fuer den Bestellprozess-Split (real,
Vorstufe zu `datenmigration-bestellprozess.md`)

**Ausgangslage:** Im Monolithen greift jedes Modul quer durch den Code auf `orders`,
`payments`, `customers` zu — z.B. ruft das Shipping-Modul direkt `orderRepository.findById()`
auf, obwohl es fachlich nichts mit Bestellverwaltung zu tun hat.

**Migration:** Vor jeder physischen Trennung wird zuerst der Code aufgeraeumt: Es entsteht
je ein `OrderRepository`, `PaymentRepository`, `CustomerRepository`, und **nur** das jeweils
zustaendige Modul darf sein Repository benutzen. Andere Module duerfen nicht mehr querlesen,
sondern rufen eine Modul-interne Methode auf (`orderModule.getOrderSummary(id)`). Das ist
reines Code-Refactoring, keine Zeile SQL aendert sich.

**Ergebnis:** Erst danach ist ueberhaupt sichtbar, welche Tabellen wirklich zu welchem
Bounded Context gehoeren — Voraussetzung fuer Pattern 6 und jede weitere physische Trennung.

---

## 6. Database per Bounded Context

**Kategorie:** B | **ShopMax-Fall:** eigene Schemas noch im selben Monolith-Deployment (real,
naechster Schritt nach Pattern 5)

**Ausgangslage:** Nach Pattern 5 ist der Code getrennt, aber `orders`, `payments`,
`customers` liegen immer noch im selben `public`-Schema derselben Datenbank.

**Migration:** Ohne den Monolithen als Deployment aufzuteilen, bekommt jeder Bounded
Context ein eigenes DB-Schema: `order_schema.orders`, `payment_schema.payments`,
`customer_schema.customers`. Zugriffe ueber Schema-Grenzen sind ab jetzt technisch
gesperrt (`REVOKE` auf fremde Schemas), nicht nur Konvention.

**Ergebnis:** Wenn spaeter `payments` in eine eigene Datenbank wandert (siehe
`datenmigration-bestellprozess.md`, Schritt 3), ist das nur noch ein Infrastruktur-Umzug
(neue DB-Instanz, Connection-String aendern) — keine riskante Schema-Operation mehr,
weil die Grenze längst existiert.

---

## 7. Monolith as Data Access Layer

**Kategorie:** B | **ShopMax-Fall:** Shipping-Service entsteht neu, hat noch keine eigene DB
(real, Vorstufe zur Extraktion)

**Ausgangslage:** Shipping-Service wird als eigenstaendiges Deployment aus dem Monolithen
herausgeloest — aber `shipments` liegt noch komplett in der Monolith-DB, ein sofortiger
Datenumzug ist zu riskant fuer den ersten Schritt.

**Migration:** Shipping-Service bekommt **keine** eigene Tabelle. Stattdessen ruft er
`GET /internal/orders/{id}/shipment-info` am Monolithen auf, der weiterhin alle Daten haelt
und ausliefert. Der Code-Schnitt (eigenes Deployment) ist damit vollzogen, der Daten-Schnitt
folgt bewusst spaeter und separat.

**Ergebnis:** Shipping-Service laeuft unabhaengig deploybar, ist aber bis zum naechsten
Schritt (Pattern 8) von der Verfuegbarkeit des Monolithen abhaengig — ein befristeter
Zwischenzustand, kein Zielbild.

---

## 8. Multischema Storage

**Kategorie:** B | **ShopMax-Fall:** Shipping-Service haelt neue Sendungen selbst, alte noch
im Monolithen (real, Fortsetzung von Pattern 7)

**Ausgangslage:** Shipping-Service (aus Pattern 7) soll jetzt anfangen, selbst Daten zu
besitzen — aber Jahre an historischen Sendungsdaten komplett auf einen Schlag zu migrieren
waere ein riskanter Big-Bang.

**Migration:** Ab einem Stichtag legt Shipping-Service **neue** Sendungen direkt in seiner
eigenen `shipments`-Tabelle an. Fuer Sendungen von **vor** dem Stichtag fragt er weiterhin
per API beim Monolithen nach (Pattern 7 bleibt als Fallback aktiv). Der Service liest also
aus zwei Quellen gleichzeitig, je nach Alter der Anfrage.

**Ergebnis:** Ein bewusster Uebergangszustand — wird erst mit Pattern 9 (Synchronize Data
in Application) endgueltig aufgeloest, sobald auch die historischen Sendungen migriert sind
und nur noch eine Quelle uebrig bleibt.

---

## 9. Synchronize Data in Application

**Kategorie:** A | **ShopMax-Fall:** die vier Schritte, konkret am Notification-Service-Fall
(real, bereits vollstaendig ausgearbeitet in `datenmigration-notification-service.md`)

**Ausgangslage:** `notifications` liegt in der Monolith-DB mit Foreign Keys auf `users` und
`orders`. Notification-Service soll eine eigene, FK-freie DB bekommen.

**Migration — die vier Schritte konkret:**
1. **Bulk-Sync:** historische `notifications` per Batch-Job in die neue DB kopieren
   (Backfill), Felder dabei denormalisiert (`recipient_email` statt `user_id`-FK).
2. **Dual Write, Read alt:** Monolith schreibt neue Notifications in beide DBs, liest aber
   weiterhin aus der alten.
3. **Dual Write, Read neu:** Reads wechseln auf die neue DB — sie ist jetzt Quelle der
   Wahrheit.
4. **Alte Tabelle entfernen:** `DROP TABLE notifications` im Monolithen, sobald nichts mehr
   dagegen schreibt oder liest.

**Ergebnis:** Kein Datenverlust waehrend der Migration, jeder Schritt einzeln deploybar und
rueckrollbar. Details inkl. SQL und Outbox-Absicherung: siehe verlinkte Datei.

---

## 10. Tracer Write

**Kategorie:** A | **ShopMax-Fall:** die Reihenfolge im Bestellprozess-Split (real, bereits
vollstaendig ausgearbeitet in `datenmigration-bestellprozess.md`)

**Ausgangslage:** Anders als bei Notifications (eine Tabelle) haengen beim Bestellprozess
**fuenf** Tabellen zusammen (`orders`, `order_items`, `payments`, `shipments`, `invoices`) —
ein einziger großer Sync-Vorgang fuer alle gleichzeitig waere unkontrollierbar riskant.

**Migration:** Pattern 9 (Synchronize Data in Application) wird **nacheinander, Tabelle fuer
Tabelle** angewendet, in einer durch die Foreign-Key-Richtung erzwungenen Reihenfolge:
zuerst `orders` (+ mitziehende `order_items`/`shipments`/`invoices`), danach `payments`
(haengt am `BestellungAufgegeben`-Event aus dem ersten Schritt), zuletzt `customers`.

**Ergebnis:** Jede Tabelle ist ihr eigener, isoliert testbarer Sync-Vorgang — ein Fehler bei
`payments` gefaehrdet nicht den bereits abgeschlossenen `orders`-Umzug. Details zur
Reihenfolge-Begruendung: siehe verlinkte Datei.

---

## 11. Database View

**Kategorie:** A | **ShopMax-Fall:** Cart-Service braucht Lesezugriff auf Produktdaten, bevor
Product-Service eine echte API hat (real, Vorstufe zur Produkt-Extraktion)

**Ausgangslage:** Cart-Service ist neu entstanden und muss Preis/Verfuegbarkeit anzeigen —
`products` liegt noch komplett im Monolithen, eine volle API existiert noch nicht.

**Migration:** Der Monolith stellt eine View bereit, die nur die oeffentlich relevanten
Spalten zeigt:
```sql
CREATE VIEW product_catalog_view AS
SELECT id, name, price, category FROM products;
-- interne Spalten wie Einkaufspreis/Marge bleiben verborgen
```
Cart-Service liest ausschliesslich gegen diese View, nie gegen die Basistabelle.

**Ergebnis:** Ein stabiler, kontrollierter Lese-Vertrag ohne eigene Infrastruktur — aber
bewusst eine Zwischenloesung. Sobald Product-Service produktiv genug ist, ersetzt eine
echte API (und spaeter Change Data Ownership) die View, statt sie dauerhaft zu pflegen.

---

## 12. Database-as-a-Service Interface

**Kategorie:** A | **ShopMax-Fall:** Legacy-BI-Tool fuer Bestellreports (erfunden — kein
Vorbild in ShopMax, aber ein typischer Fall, sobald ein Unternehmen ein bestehendes
Reporting-Tool hat, das nur SQL spricht)

**Ausgangslage:** Das Controlling nutzt ein altes BI-Tool, das nur per SQL gegen eine
Datenbank connecten kann — keine Moeglichkeit, eine REST-API anzubinden.

**Migration:** Statt das BI-Tool direkt gegen die Order-Service-DB zu verbinden (das waere
Shared Database durch die Hintertuer), bekommt es eine **eigene, dedizierte Read-Only-DB**.
Ein CDC-Prozess (aehnlicher Mechanismus wie der Bulk-Sync aus Pattern 9) haelt sie aktuell,
sobald sich Order-Daten aendern.

**Ergebnis:** Das BI-Tool bekommt, was es braucht (SQL-Zugriff), ohne dass Order-Service
seine interne DB-Struktur offenlegen oder Schema-Aenderungen mit dem Controlling abstimmen
muss — die beiden Datenbanken sind komplett entkoppelt.

---

## 13. Database Wrapping Service

**Kategorie:** A | **ShopMax-Fall:** direkter SQL-Zugriff auf `orders` unterbinden, bevor der
Bestellprozess-Split beginnt (real, fruehe Vorstufe zu `datenmigration-bestellprozess.md`)

**Ausgangslage:** Mehrere Module im Monolithen (Shipping, Invoicing) greifen historisch
gewachsen direkt per SQL auf `orders` zu, nicht ueber eine gemeinsame Schnittstelle — jede
Schema-Aenderung an `orders` riskiert, einen dieser Zugriffe stillschweigend zu brechen.

**Migration:** Eine duenne interne API-Schicht wird vor `orders` gesetzt
(`OrderQueryService.getOrder(id)`). Alle bisherigen Direktzugriffe werden Modul fuer Modul
umgestellt, bis kein SQL-`SELECT * FROM orders` mehr ausserhalb des Order-Moduls existiert.

**Ergebnis:** Erst wenn niemand mehr direkt zugreift, ist `orders` "privat genug", um
ueberhaupt in Pattern 5/6 (Repository/Database per Bounded Context) und danach in die
eigene Order-Service-DB zu wandern — ohne diesen Schritt wuerde der spaetere Umzug
unbemerkt andere Module brechen.

---

## 14. Aggregate Exposing Monolith

**Kategorie:** A | **ShopMax-Fall:** Shipping-Service in der Fruehphase, bevor er Ownership
uebernimmt (real, Ergaenzung zu Pattern 7)

**Ausgangslage:** Shipping-Service ist gerade erst entstanden. Noch ist unklar, welche
Felder er von einer Order wirklich braucht — ein Trial-and-Error-Zugriff auf das komplette
Monolith-Schema wuerde zu enger Kopplung fuehren.

**Migration:** Der Monolith definiert explizit ein Event `OrderReadyForShipment` mit genau
den Feldern, die Shipping-Service benoetigt (`orderId`, `deliveryAddress`, `items`) — und
veroeffentlicht es, sobald eine Order versandbereit ist. Shipping-Service konsumiert nur
dieses Event, nicht das Schema dahinter.

**Ergebnis:** Das Definieren des Events zwingt dazu, den tatsaechlichen Datenbedarf explizit
festzulegen, bevor irgendetwas physisch migriert. Sobald Shipping-Service reif genug ist,
folgt Change Data Ownership (Pattern 2) als naechster Schritt — Aggregate Exposing Monolith
ist keine Dauerloesung.

---

## 15. Duplicate Static Reference Data

**Kategorie:** C | **ShopMax-Fall:** Versandmethoden-Codes (STANDARD, EXPRESS, PICKUP), real
plausibel, im Kontrast zu Pattern 4 (Waehrungscodes)

**Ausgangslage:** Order-, Shipping- und Payment-Service brauchen alle die Liste gueltiger
Versandmethoden. Anders als Steuersaetze (Pattern 4) aendert sich diese Liste praktisch nie
und eine falsche Kopie irgendwo waere kein finanzielles Risiko, hoechstens eine kurzzeitig
falsch angezeigte Versandoption.

**Migration:** Jeder der drei Services bekommt seine eigene, hartcodierte Kopie
(`enum ShippingMethod { STANDARD, EXPRESS, PICKUP }`), gepflegt im jeweiligen Repo. Kein
zentraler Service, keine Koordination.

**Ergebnis:** Redundanz ohne Koordinationsaufwand. **Warum hier okay, bei Pattern 4 nicht:**
Die Frage "wuerde es jemand bemerken, wenn zwei Kopien kurzzeitig auseinanderlaufen?" wird
hier mit "nein" beantwortet — bei Steuersaetzen mit "ja, sofort, und es kostet Geld".

---

## 16. Static Dedicated Reference Data Schema

**Kategorie:** C | **ShopMax-Fall:** die Vorstufe zu Pattern 4 — bevor Country-Code-Service
entstand (erfunden als Zwischenschritt, um die Eskalationsstufe zu zeigen)

**Ausgangslage:** ShopMax hat gerade erkannt, dass die reine Kopie (Pattern 15) fuer
Laender-/Steuerdaten zu riskant ist — aber ein voller eigener Service wirkt zu diesem
fruehen Zeitpunkt wie Infrastruktur-Overkill fuer eine einzelne Tabelle.

**Migration:** Als Kompromiss zieht `countries` in eine eigene, dedizierte Datenbank um.
Order-, Payment- und Shipping-Service greifen weiterhin per direktem SQL darauf zu — aber
nicht mehr auf eine Tabelle im Monolithen, sondern auf einen eigenen, klar benannten
Endpunkt mit eigenem Owner-Team.

**Ergebnis:** Besser als Duplikation (eine Wahrheit statt drei), aber immer noch direkter
DB-Zugriff mehrerer Services — sobald sich zeigt, dass auch das nicht robust genug ist
(z.B. weil ein Encoding-Wechsel alle Konsumenten gleichzeitig treffen wuerde), folgt der
Umstieg auf Pattern 4 (Static Reference Data Service) mit echter API statt SQL-Zugriff.

---

## 17. Static Reference Data Library

**Kategorie:** C | **ShopMax-Fall:** Bestellstatus-Codes als gemeinsame Java-Bibliothek (real,
da ShopMax laut Monolith-schneiden-Uebung durchgaengig Java/Spring Boot ist)

**Ausgangslage:** `order_status`-Werte (NEU, BEZAHLT, VERSENDET, ABGESCHLOSSEN, STORNIERT)
werden aktuell in Order-, Payment- und Shipping-Service jeweils als eigenes Enum gepflegt —
mit dem Risiko, dass sie unbemerkt auseinanderlaufen.

**Migration:** Die Codes wandern in eine gemeinsame Bibliothek `shopmax-common-refdata`,
veroeffentlicht in einem internen Maven-Repository. Alle drei Services binden sie als
Dependency ein, statt eigene Enums zu pflegen. Eine Aenderung bedeutet: neue Version
veroeffentlichen, Services aktualisieren ihre Dependency bei naechster Gelegenheit.

**Ergebnis:** Funktioniert nur, **weil** alle ShopMax-Services Java sind — genau das macht
den Unterschied zu Pattern 4/16. Sobald ein Service in einer anderen Sprache entstuende
(z.B. ein Python-basierter Recommendation-Service), waere die Bibliothek fuer ihn nutzlos
und Pattern 4 (Service statt Library) waere die robustere Wahl.

---

## Stand: alle 18 durch

7 Patterns bleiben auf realen ShopMax-Bezuegen (1, 3, 5, 6, 7, 8, 9, 10, 11, 13, 17 — direkt
aus der Monolith-schneiden-Uebung oder den zwei bestehenden Migrationsdokumenten ableitbar),
7 sind bewusst erfunden, aber jeweils mit Begruendung, warum es kein reales Vorbild gibt
(2, 4, 12, 14, 15, 16). Jede erfundene Karte sagt das explizit, keine stille Vermischung.

Pattern 9 und 10 (Synchronize Data in Application, Tracer Write) verweisen bewusst auf die
zwei bestehenden Dateien statt sie zu duplizieren — die liefern bereits SQL-Details und
Reihenfolge-Begruendung in voller Tiefe.

## Offene Fragen zur Bewertung

- Passt die durchgehende Tiefe (Ausgangslage / Migration / Ergebnis, ~35-45 Zeilen) fuer
  einen Praesentations-Foliensatz, oder ist das immer noch zu viel Lesestoff fuer 18 Karten
  am Stueck? Alternative waere z.B. nur 6-8 Karten live vorzustellen und den Rest als
  Nachschlagewerk zu behandeln.
- Wie sollen die 18 Karten in die bestehende Struktur einsortiert werden — ersetzt jede Karte
  den Inhalt der jeweiligen `microservices/database-patterns/*.md`-Datei direkt, oder bleiben
  die abstrakten Pattern-Definitionen dort stehen und diese Datei wird eine ergaenzende
  "ShopMax wendet es an"-Sicht, auf die von dort verlinkt wird?
- Die Kategorie-D-Tabelle in `microservices/databases/patterns/overview.md` muesste um die
  neue Shared-Database-Positionierung (Status quo statt Auswahloption) angepasst werden —
  soll ich das gleich mit erledigen, sobald das Gesamtformat steht?
