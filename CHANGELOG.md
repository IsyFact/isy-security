# 5.0.0

### BREAKING CHANGE
- `IFS-4812`: Verwendung sicherer Hashfunktion mit SHA-512 für Caching
  * Rückgabe eines Byte-Arrays statt eines Integers in der Methode `generateCacheKey` der Klasse `AbstractClientRegistrationAuthenticationToken`
  * Konfigurierbare Properties für Hashfunktion und Bytegröße des Salts

### FEATURES
- `IFS-4577`: Portierung fehlender Tickets aus isyfact-standards
- `IFS-4591`: Hinzufügen von Authentifizierungsmethoden zur Authentifizierung von Clients und Systemen ohne Issuer-URI.
- `IFS-4754`: Einführung von Caching im Authentifizierungsprozess
- `IFS-4752`: Wiederherstellen der initialen Authentication nach Authentifizierung mit @Authenticate-Annotation
- `IFS-4785`: Hinzufügen einer Property für die Restlebensdauer gecachter OAuth2-Token
- `IFS-4810`: Ausbau der Validierung des "aud"-Claims erstellter Tokens

### DEPENDENCY UPGRADES
- `IFS-4655`: Update von Maven Checkstyle Plugin auf Version 3.6.0
- `IFS-4580`: Spring Boot Update auf Version 3.4.5
- `IFS-4531`: Update von Flatten Maven Plugin auf Version 1.7.1
    * Hinzufügen von Maven Enforcer Plugin auf Version 3.6.0
    * Setzen der Maven Version auf 3.6.3
