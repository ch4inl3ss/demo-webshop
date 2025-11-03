# Nerdshirts Webshop

Spring Boot basiertes Beispielprojekt für einen Nerdshirt-Webshop inklusive Authentifizierung, Beispielprodukten und vollständiger Docker-Compose-Umgebung.

## Features

- Spring Boot 3 (Java 17) REST-API
- Spring Security mit Passwort-Hashing und Basic Auth
- Beispielprodukte und Admin-Account werden automatisch angelegt
- Persistenz mit PostgreSQL (Docker) bzw. H2 In-Memory (lokal)
- Dockerfile und docker-compose für reproduzierbare Umgebung
- Skript zum Erzeugen eines auslieferbaren ZIP-Archivs

## Schnellstart (lokal ohne Docker)

```bash
mvn spring-boot:run
```

Anschließend steht die API unter `http://localhost:8080` bereit.

## Docker Compose

```bash
docker-compose up --build
```

Die Anwendung verwendet automatisch das Profil `docker` und verbindet sich mit der PostgreSQL-Datenbank aus `docker-compose.yml`.

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
