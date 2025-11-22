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
- `POST /api/passkeys/register/*` & `/api/passkeys/login/*` – Passkey-Registrierung und -Login (siehe unten)

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

## Passkey Login

Der Login-Flow unterstützt passwortlose Passkeys, die direkt im Browser erzeugt werden. Der Ablauf:

1. Auf der Login-Seite E-Mail + Gerätename eingeben und auf „Passkey erstellen" klicken.
2. Der Browser erzeugt ein Schlüsselpaar, meldet den öffentlichen Schlüssel beim Backend an und speichert den privaten Schlüssel lokal.
3. Für einen Login wird über „Mit Passkey einloggen" automatisch eine Challenge signiert und an `/api/passkeys/login` geschickt.

Die privaten Schlüssel verlassen das Gerät nie. Für Tests auf einem neuen Gerät muss der Passkey erneut erzeugt werden.
