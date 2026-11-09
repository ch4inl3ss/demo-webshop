# Nerdshirts Webshop

Spring Boot basiertes Beispielprojekt für einen Nerdshirt-Webshop inklusive Authentifizierung, Beispielprodukten und vollständiger Docker-Compose-Umgebung.

## Features

- Spring Boot 3 (Java 17) REST-API
- Bootstrap-basiertes Thymeleaf-Frontend mit Produktübersicht und Detailseiten
- Spring Security mit Formular-Login, Passwort-Hashing und Basic Auth für die API
- Beispielprodukte (8 Stück) und Admin-Account werden automatisch angelegt
- Persistenz mit PostgreSQL (Docker) bzw. H2 In-Memory (lokal)
- Dockerfile und docker-compose für reproduzierbare Umgebung
- Skript zum Erzeugen eines auslieferbaren ZIP-Archivs

## Schnellstart (lokal ohne Docker)

```bash
mvn spring-boot:run
```

Die Web-Oberfläche erreichst du unter `http://localhost:8080`. Die REST-API ist weiterhin unter `/api` verfügbar.

## Docker Compose

```bash
docker-compose up --build
```

Die Anwendung verwendet automatisch das Profil `docker` und verbindet sich mit der PostgreSQL-Datenbank aus `docker-compose.yml`.

## Web-Oberfläche

- `GET /` – Produktübersicht mit Karten-Layout
- `GET /products/{id}` – Produktdetailseite
- `GET /login` – Login-Formular für bestehende Konten
- `GET /register` – Registrierungs-Formular mit Passwortvalidierung

## Beispiel-Endpunkte

- `GET /api/products` – Liste aller Produkte (öffentlich)
- `GET /api/products/{id}` – Details zu einem Produkt (öffentlich)
- `POST /api/products` – Neues Produkt anlegen (nur Rolle `ADMIN`)
- `POST /api/auth/register` – Neues Kundenkonto registrieren
- `POST /api/auth/login` – Login mit Basic-Auth-Credentials validieren

### Standard-Accounts

- Admin: `admin@nerdshirts.de` / Passwort: `changeme`

Weitere Nutzer können über den Registrierungs-Endpunkt erstellt werden.

## ZIP-Archiv erstellen

```bash
./package.sh
```

Das Skript erzeugt `webshop-docker.zip`, welches den gesamten Projektstand (ohne Build-Artefakte) enthält.

## Tests

```bash
mvn test
```

## Passkeys auf einer echten Domain (z. B. Demo mit dem iPhone)

WebAuthn braucht HTTPS (nur `localhost` ist ausgenommen). Die Domain, an die die Passkeys
gebunden sind, kommt aus der Konfiguration:

```bash
docker compose up -d db
NERDSHIRTS_PASSKEYS_RP_ID=shop.example.org \
NERDSHIRTS_PASSKEYS_ALLOWED_ORIGINS=https://shop.example.org \
java -jar target/webshop-0.0.1-SNAPSHOT.jar
```

Davor ein TLS-Tunnel oder Reverse Proxy, der auf `localhost:8080` zeigt und `X-Forwarded-*` setzt
(`server.forward-headers-strategy: framework` ist aktiv).

Wichtig: Die Domain muss **fest** sein. Ein Tunnel mit zufälliger Domain pro Start macht alle
registrierten Passkeys beim nächsten Start unbrauchbar – die RP-ID ist Teil jedes Passkeys.
