# Security

## 5.2.0

### Hinweise & bekannte Probleme

- keine

### Umgesetzte Tickets

- keine

#### Dokumentation

- `IFS-5826` Anpassung des Security-Konzepts gemäß StB-ADR058 zur Nicht-Auswertung des BHKNZ bei der Authentifizierung direkt im IAM-Service verwalteter Clients

#### Features

- keine

#### Bug-Fixes

- keine

#### Interne Anpassungen

- keine

### Durchzuführende Aktionen vor dem ersten Einsatz

- keine

### DEPENDENCY UPGRADES

# Security

## 5.1.0

### Hinweise & bekannte Probleme

- keine

### Umgesetzte Tickets

#### Dokumentation

- `IFS-5748` Aufnahme des ROPC-Supports in die IsyFact-Dokumentation

#### Features

- `IFS-5745` Wiederherstellung der ROPC-Infrastruktur
    - BREAKING CHANGE:
        - Einführung der Unterstützung des ROPC-flows auf Basis einer eigenen Implementierung.
- `IFS-5582` Anpassung des Moduls `isy-security-test` an aktuelles Spring-Framework
    - BREAKING CHANGE:
        - Wegfall der Unterstützung für `password`-basierte OAuth2-Authentifizierung
        - Entfernen des entsprechenden Codes für `password`-basierte OAuth2-Authentifizierung
- `IFS-5733` Update von isy-security auf Spring Boot 4.1.0
- `IFS-5746` Integration des ROPC-Flows in bestehenden Authentifzierungsmanager
    - BREAKING CHANGE:
        - Änderungen in den geworfenen Exceptions an diversen Stellen: Austausch von IllegalArgumentException durch BadCredentialsException
- `IFS-5747` Erweiterung der isy-security-test um ROPC-Flow

#### Bug-Fixes

- keine

#### Interne Anpassungen

- keine

### Durchzuführende Aktionen vor dem ersten Einsatz

- keine
