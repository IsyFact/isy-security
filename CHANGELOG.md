# Security

## 5.1.0

### Hinweise & bekannte Probleme

- keine

### Umgesetzte Tickets

#### Dokumentation

- keine

#### Features

- `IFS-5582` Anpassung des Moduls `isy-security-test` an aktuelles Spring-Framework
    - BREAKING CHANGE:
        - Wegfall der Unterstützung für `password`-basierte OAuth2-Authentifizierung
        - Entfernen des entsprechenden Codes für `password`-basierte OAuth2-Authentifizierung

#### Bug-Fixes

- keine

#### Interne Anpassungen

- keine

### Durchzuführende Aktionen vor dem ersten Einsatz

- keine

### DEPENDENCY UPGRADES
- Update com.github.spotbugs:spotbugs-maven-plugin von Version 4.9.8.3 auf 4.10.3.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/next_version.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_deploy_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_changelog_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_merge_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/commit_message_checker_template.yml von Version 2.3.0 auf 3.0.0

