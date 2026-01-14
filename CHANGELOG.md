# 5.0.0

### FEATURES
- `IFS-4734`: Verwendungen des Begriffs "Service" und damit verwandter Begriffe korrigieren
- `IFS-4924`: Anpassungen im Text und Grafiken wegen der Einführung des Begriffs "API-Gateway"
- `IFS-4577`: Portierung fehlender Tickets aus `isyfact-standards`
- `IFS-4591`: Hinzufügen von Authentifizierungsmethoden zur Authentifizierung von Clients und Systemen ohne Issuer-URI.
- `IFS-4754`: Einführung von Caching im Authentifizierungsprozess
- `IFS-4752`: Wiederherstellen der initialen Authentication nach Authentifizierung mit @Authenticate-Annotation
- `IFS-4785`: Hinzufügen einer Property für die Restlebensdauer gecachter OAuth2-Token
- `IFS-4810`: Ausbau der Validierung des "aud"-Claims erstellter Tokens
- `IFS-4583`: Wiedereinführung der Quality-Gates

### BREAKING CHANGE
- `IFS-4812`: Verwendung sicherer Hashfunktion mit SHA-512 für Caching
    * Rückgabe eines Byte-Arrays statt eines Integers in der Methode `generateCacheKey` der Klasse `AbstractClientRegistrationAuthenticationToken`
    * Konfigurierbare Properties für Hashfunktion und Bytegröße des Salts
- `IFS-4922`: Aktualisierung von Java 17 auf 25

### DEPENDENCY UPGRADES
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependency_review_template.yml von Version 1.7.0 auf 1.8.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/docs_build_template.yml von Version 1.7.0 auf 1.8.0
- `IFS-4655`: Update von Maven Checkstyle Plugin auf Version 3.6.0
- `IFS-4580`: Spring Boot Update auf Version 3.4.5
- `IFS-4531`: Update von Flatten Maven Plugin auf Version 1.7.1
    * Hinzufügen von Maven Enforcer Plugin auf Version 3.6.0
    * Setzen der Maven Version auf 3.6.3
