# Consum Reporting — contractul J2

## Pe scurt

J2 este WIP pentru framework și teste Kafka/mesagerie cu Setup Podman. Cele trei componente sunt MOCK, iar Reporting J6 nu certifică un broker ori un client Kafka Java.

J2 integrează JAR/POM J6 și propriul adaptor Eventuate. Implementarea,
documentația bibliotecii și regresiile generice au owner J0; J1 deține
scenariile web și platforma comună, reconciliate selectiv.

## Ownership și direcția sincronizării

| Schimbare | Owner și livrare |
|---|---|
| Reporting / regresii generice | J0; binar versionat și probat separat în J2 |
| platformă comună | ownerul comportamentului în J0/J1; reconciliere selectivă, numai pentru modulele aplicabile J2 |
| Setup / Eventuate | J2; fără import automat în reactorul J0/J1 |
| POM, atlas, index și feedback J2 | deployment J2; fără mirror al contextului web |

## Traseul bibliotecii

lib conține cele două perechi Core/Reporting JAR/POM. Proveniența unică de la rădăcină păstrează
identitatea versiunii și build-ul istoric; ownerul de mentenanță este J0.

## Contractul unui modul împachetat

Consumatorul folosește API-ul public și tranzitivele POM-ului livrat.
Nu primește sursa Reporting pentru a face classpath-ul verde și nu copiază
harness-ul producătorului în checkout.

## Deschiderea proiectului

Importă POM-ul root în IntelliJ și folosește JDK 21 pentru proiect și Maven.
Bootstrap-ul root validate instalează JAR-ul local cu POM-ul real; nu este
necesară adăugarea manuală a unei biblioteci în IDE.

```powershell
mvn -B -N validate
mvn -B -pl atf-test-eventuate -am -DskipTests install
mvn -B -pl atf-test-eventuate -Dtest=EventuateComponentSuite test
```

Agentul rulează aceste comenzi numai în [TMP](ATF_Build_And_Execution_Safety.md).
Un cache separat per execuție/consumer evită înlocuirea unui JAR ținut deschis
de alt JVM pe Windows.

## Compatibilitatea acestui increment

[Atlasul J6](ATF_Reporting_Library_Atlas.md) descrie callerii efectivi.
Catalogul live are șapte module; [scopul J2](ATF_J2_Scope.md) exclude UI/Debug
și Playwright. Nu copiați catalogul journeys sau foundation peste Eventuate.

## Reporting Tests la producător

Regresiile permanente și sursa rămân în [J0](../../atf_j0/atf-reporting-tests/README.md).
Matricea consumerului verifică adaptorul J2 și nu substituie gate-ul bibliotecii.

## Gate-ul extern Reporting

Un candidat se construiește o singură dată la owner. Harness-ul J0 verifică
JAR-ul fizic în staging de consumer; sursa și harness-ul nu sunt promovate
în J2. Acceptanța J1 nu certifică J2 și invers.

## Validarea consumerului

CodeSource/hash, POM embedded și proveniență coerentă preced acceptanța
individual/suită și Extent/fallback. [Verificarea executată](ATF_J2_Verification.md)
nu este moștenită de o versiune viitoare.

## Actualizarea bibliotecii

Actualizează JAR/POM și proveniența împreună, într-un increment verificat.
Păstrează build-ul istoric al J6; nu îl rescrie ca execuție produsă în J0
sau ca acceptanță Kafka doar pentru că s-a schimbat directorul.

[Feedback-ul consumerului](feedback/ATF_Reporting_Consumer_Feedback.md) păstrează
impactul local și revalidarea cerută; backlog-ul generic este numai în J0.

<a id="j1-reporting-tests-02--retragere-cu-dovadă-păstrată"></a>

[Gate-ul la producător](#gate-ul-extern-reporting) păstrează regresiile, fără harness local.
