# Pattern: Database Wrapping Service

Eine bestehende Datenbank wird mit einem Service "umwickelt". Zugriffe laufen ab jetzt
über die Service-API, nicht mehr direkt auf die Tabellen — damit lässt sich sicherstellen,
dass sich die Datenbank nicht unkontrolliert verändert.

## Wann nutzt man Database Wrapping Service?

  * Um eine API vor eine bestehende Datenbank zu setzen und dadurch Veränderungen zu
    verhindern bzw. einzuschränken, was Clients tun dürfen
  * Als Zwischenschritt, wenn Clients heute noch direkt auf die DB zugreifen, aber
    perspektivisch auf eine echte API umgestellt werden sollen

### Wann eher nicht

  * Wenn ohnehin niemand außer dem einen Service auf die Datenbank zugreift — dann gibt
    es nichts zu "wrappen", die Datenbank ist bereits privat

## Verwandte Patterns

  * [Monolith as Data Access Layer](/microservices/database-patterns/monolith-as-data-access-layer.md) —
    dieselbe Grundidee (API statt direktem DB-Zugriff), aber aus Sicht eines neuen Service,
    der noch keine eigene Datenbank hat.
  * [Change Data Ownership](/microservices/database-patterns/change-data-ownership.md) —
    der nächste Schritt, sobald klar ist, wer die Daten wirklich besitzen soll.

## ShopMax-Beispiel

Pattern 13 in [Database Patterns anhand ShopMax](/microservices/datenmigration-patterns-shopmax.md) —
direkter SQL-Zugriff auf `orders` wird unterbunden, inkl. Grafik.
