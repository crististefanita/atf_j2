# atf-test-eventuate — mesagerie și Kafka · WIP

## Pe scurt

Acesta este singurul modul de test J2. Framework-ul pentru cozi Kafka este în lucru; integrarea Java cu brokerul real nu este implementată, iar infrastructura Podman aparține [Setup](../atf-setup/README.md).

Modulul compune platforma ATF cu trei oracole deterministe pentru Eventuate:
contract API, convergență cu retry de citire și validarea dovezilor de mesagerie.
Suportul reutilizabil este în main; adaptorul JUnit și fixture-urile sunt în test.

| Responsabilitate | Limită |
|---|---|
| Deține | clientul HTTP scriptat, deadline-ul de convergență, validatorul de dovezi și compoziția raportului |
| Kit | evaluarea unică a comparațiilor, lifecycle-ul fazelor și scope-ul class/suite |
| Reporting JAR | contractele publice, redacția, preview-ul, HTML-ul și navigarea Extent/fallback |
| Aplicația reală | saga, rezervarea creditului, deduplicarea, outbox-ul și proiecția CQRS; neexecutate în acest increment |

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Fixture["Fixture"] -->|MOCK| Client["Client"]
    Client -->|valori| Kit["Kit"]
    Kit -->|evaluat| Bridge["JUnit"]
    Bridge -->|API public și close| Report["Reporting<br/>Extent / fallback"]
```

<details>
<summary><strong>Legendă — ownership și traseu</strong></summary>

Expected-ul este fixat de test, separat de răspunsul transportului ori de witness.
Kit decide verdictul înaintea redacției și a preview-ului; adaptorul transferă
rezultatul fără reevaluare. Muchiile descriu fluxul dovezii, nu dependențe Maven.

`Client` include clientul HTTP sau validatorul de mesagerie; `JUnit` este
adaptorul de lifecycle. `Reporting` publică la close artifactul HTML Extent ori
fallback, prin aceeași bibliotecă externă.

</details>

## Intrări și context

| Sursă | Rol în investigație |
|---|---|
| [EventuateApiClient](src/main/java/atf/eventuate/api/EventuateApiClient.java) | binding concret MOCK; POST o tentativă, GET 503 maximum două tentative prin Core |
| [OrderAwaiter](src/main/java/atf/eventuate/await/OrderAwaiter.java) | clock/pause injectate, buget unic; PENDING intermediar și refuzul citirii după deadline |
| [MessageEvidenceContract](src/main/java/atf/eventuate/validation/MessageEvidenceContract.java) | identitate, livrări distincte și witness independent; nu implementează deduplicarea aplicației |
| [ComponentReportRun](src/main/java/atf/eventuate/reporting/ComponentReportRun.java) | owner serial, configurație capturată, trei faze și publicare la close |
| [EventuateComponentExtension](src/test/java/atf/eventuate/tests/support/EventuateComponentExtension.java) | scope class/suite, collector per invocare; calea absolută numai după publicare |
| [EventuateComponentSuite](src/test/java/atf/eventuate/tests/suites/EventuateComponentSuite.java) | selectează exact cele trei clase, fără execuție paralelă |

## Invariante

- Expected-ul și observația au surse independente.
- Transportul este finit și ordonat; o rută absentă sau consumată nu produce răspuns verde.
- Faptele se păstrează înaintea redactării și a previzualizării.
- Excepția inițială rămâne distinctă de publicare și cleanup.
- Un assertion FAIL deja înregistrat nu devine încă un diagnostic execution identic.
- `MOCK_EVENT_FEED` și ordinalul fixture-ului nu reprezintă un offset Kafka.
- Output-ul intermediar rămâne în staging TMP.

## Verificare și navigare

Din rădăcina copiei TMP:

```powershell
mvn -B -pl atf-test-eventuate -am "-Dtest=EventuateComponentSuite" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Comanda execută cele trei componente MOCK și produce un raport comun; nu pornește Podman sau Kafka. [Matricea raportului](../docs/ATF_Eventuate_Testing.md#acceptanța-raportului) verifică separat individual/suită, Extent/fallback și failure-uri controlate.

[Atlasul J2](../docs/ATF_Context_Atlas.md) identifică ownerul și proba.
[Ghidul testării](../docs/ATF_Testing_Guide.md) separă componentele mock de acceptanța aplicației reale.

## Limite curente ale scaffold-ului

| Criteriu rămas | Owner și probă de închidere |
|---|---|
| identitate și deadline | [convergență](../docs/feedback/ATF_Eventuate_Feedback.md#evt-convergence-contract-86--identitate-și-deadline-real): awaiterul state-only nu certifică identitatea; callerul trebuie să compună întregul contract |
| execuție terminală | [snapshot înaintea excepției](../docs/feedback/ATF_Eventuate_Feedback.md#evt-terminal-evidence-87--snapshot-ul-core-înaintea-excepției): rezultatul normal absent nu trebuie să elimine evidence-ul disponibil |

Aceste criterii sunt deschise. Cele trei fixture-uri nominale și matricea publicată nu certifică negativele încă neacoperite sau integrarea Kafka reală.
