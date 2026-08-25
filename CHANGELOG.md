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
- `IFS-5733` Update von isy-security auf Spring Boot 4.1.0

#### Bug-Fixes

- keine

#### Interne Anpassungen

- keine

### Durchzuführende Aktionen vor dem ersten Einsatz

- keine

### DEPENDENCY UPGRADES
- Update com.github.spotbugs:spotbugs-maven-plugin von Version 4.10.3.0 auf 4.10.4.0
- Update org.codehaus.mojo:flatten-maven-plugin von Version 1.7.3 auf 1.8.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependency_review_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/docs_build_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_dependency_scan_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_build_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/semgrep.yml von Version 2.5.0 auf 3.0.0
- Update org.sonatype.central:central-publishing-maven-plugin von Version 0.10.0 auf 0.11.0
- Update org.cyclonedx:cyclonedx-maven-plugin von Version 2.9.1 auf 2.9.2
- Update org.apache.maven.plugins:maven-surefire-plugin von Version 3.5.5 auf 3.5.6
- Update org.apache.maven.plugins:maven-dependency-plugin von Version 3.10.0 auf 3.11.0
- Update org.jacoco:jacoco-maven-plugin von Version 0.8.14 auf 0.8.15
- Update com.github.spotbugs:spotbugs-maven-plugin von Version 4.9.8.3 auf 4.10.3.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/next_version.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/maven_deploy_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_changelog_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/dependabot_auto_merge_template.yml von Version 2.3.0 auf 3.0.0
- Update IsyFact/isy-github-actions-templates/.github/workflows/commit_message_checker_template.yml von Version 2.3.0 auf 3.0.0

