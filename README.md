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

## Login über Keycloak (Branch `step-6-keycloak`)

Ab diesem Schritt kennt der Shop keine Passwörter und keine Passkeys mehr. Er ist OIDC-Client,
Login, Registrierung und Passkeys macht Keycloak.

```bash
docker compose up -d db keycloak   # Keycloak importiert keycloak/nerdshirts-realm.json beim ersten Start
mvn spring-boot:run
```

- Shop: `http://localhost:8080`, "Anmelden" leitet zu Keycloak weiter
- Keycloak-Admin-Konsole: `http://localhost:8180/admin/master/console/#/nerdshirts` (`admin` / `admin`)
- Nutzer: `admin@nerdshirts.de` (Rolle `ADMIN`, muss beim ersten Login einen Passkey anlegen) und `kunde@example.com`, beide mit Passwort `changeme`
- Passkeys verwalten: Keycloak-Account-Konsole, verlinkt als "Passkeys" in der Navigation

Den Realm-Import greift Keycloak nur beim ersten Start. Zurücksetzen auf den Stand der Datei:

```bash
docker compose rm -sf keycloak && docker volume rm demo-webshop_keycloak-data && docker compose up -d keycloak
```

Die Passkeys gehören jetzt zu Keycloak (RP-ID = Domain von Keycloak, hier `localhost`).
Für eine echte Domain müssen Keycloak (`KC_HOSTNAME`) und die URLs im Realm (Redirect-URI,
Web Origins) und in der `application.yml` (`issuer-uri`) angepasst werden.

