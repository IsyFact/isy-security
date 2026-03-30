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
- `IFS-4579`: Wiederherstellung von isy-security-test
- `IFS-5004`: Vorbereitung von isy-security für Update auf Spring Boot 4
- `IFS-5218`: Wiederherstellung Tests nach Spring Boot Update

### BREAKING CHANGE
- `IFS-4812`: Verwendung sicherer Hashfunktion mit SHA-512 für Caching
    * Rückgabe eines Byte-Arrays statt eines Integers in der Methode `generateCacheKey` der Klasse `AbstractClientRegistrationAuthenticationToken`
    * Konfigurierbare Properties für Hashfunktion und Bytegröße des Salts
- `IFS-4922`: Aktualisierung von Java 17 auf 25
- `IFS-4858`: Umstellung von isy-security auf Spring Boot 4 (inkludiert Update auf Spring Security 7)
    * Nutzer von isy-security sollten ebenfalls auf Spring Boot 4 updaten, um Kompatibilitätsprobleme zu vermeiden
    * Der Resource Owner Password Credential Flow wird mit dieser Version nicht mehr unterstützt. Anwendungen müssen zwingend auf den Client Credentials Flow umsteigen.

### DEPENDENCY UPGRADES
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_changelog_template.yml von Version 2.1.1 auf 2.3.0
- Update org.apache.maven.plugins:maven-compiler-plugin von Version 3.14.1 auf 3.15.0
- Update org.springframework.boot:spring-boot-dependencies von Version 4.0.2 auf 4.0.5
- Update org.apache.maven.plugins:maven-dependency-plugin von Version 3.9.0 auf 3.10.0
- Update org.cyclonedx:cyclonedx-maven-plugin von Version 2.7.9 auf 2.9.1
- Update org.apache.maven.plugins:maven-javadoc-plugin von Version 3.3.2 auf 3.12.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/commit_message_checker_template.yml von Version 2.0.0 auf 2.2.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_merge_template.yml von Version 2.1.0 auf 2.1.1
- Update IsyFact/isy-github-actions-templates/.github/workflows/commit_message_checker_template.yml von Version 2.0.0 auf 2.1.1
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_create_release_template.yml von Version 2.1.1 auf 2.2.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_merge_template.yml von Version 2.1.0 auf 2.2.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/docs_build_template.yml von Version 2.1.0 auf 2.2.0
- Update com.github.spotbugs:spotbugs-maven-plugin von Version 4.9.8.1 auf 4.9.8.2
- Update org.apache.maven.plugins:maven-source-plugin von Version 3.2.1 auf 3.4.0
- Update org.apache.maven.plugins:maven-gpg-plugin von Version 3.0.1 auf 3.2.8
- Update org.codehaus.mojo:flatten-maven-plugin von Version 1.7.1 auf 1.7.3
- Update IsyFact/isy-github-actions-templates/.github/workflows/next_version.yml von Version 2.1.0 auf 2.1.1
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_changelog_template.yml von Version 1.8.0 auf 2.1.1
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_dependency_scan_template.yml von Version 2.0.0 auf 2.1.1
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_build_template.yml von Version 2.0.0 auf 2.1.1
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependency_review_template.yml von Version 2.1.0 auf 2.1.1
- Update net.logstash.logback:logstash-logback-encoder von Version 8.0 auf 8.1
- Update org.apache.maven.plugins:maven-enforcer-plugin von Version 3.6.0 auf 3.6.2
- Update org.sonatype.central:central-publishing-maven-plugin von Version 0.8.0 auf 0.10.0
- Update org.springframework.boot:spring-boot-dependencies von Version 3.5.7 auf 3.5.9
- Update org.apache.maven.plugins:maven-surefire-plugin von Version 3.3.1 auf 3.5.4
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_create_release_template.yml von Version 1.7.0 auf 1.8.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_deploy_template.yml von Version 1.7.0 auf 1.8.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_merge_template.yml von Version 2.0.0 auf 2.1.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/next_version.yml von Version 2.0.0 auf 2.1.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_create_release_template.yml von Version 2.0.0 auf 2.1.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_deploy_template.yml von Version 2.0.0 auf 2.1.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependency_review_template.yml von Version 1.7.0 auf 1.8.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/docs_build_template.yml von Version 1.7.0 auf 1.8.0
- `IFS-4655`: Update von Maven Checkstyle Plugin auf Version 3.6.0
- `IFS-4580`: Spring Boot Update auf Version 3.4.5
- `IFS-4531`: Update von Flatten Maven Plugin auf Version 1.7.1
    * Hinzufügen von Maven Enforcer Plugin auf Version 3.6.0
    * Setzen der Maven Version auf 3.6.3
- `IFS-5205`: 
    * Entfernen von isy-logging
    * Hinzufügen von org.springframework.boot:spring-boot-starter-aspectj
