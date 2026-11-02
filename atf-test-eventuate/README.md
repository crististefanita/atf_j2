# atf-test-eventuate — mesagerie și Kafka · WIP

## Pe scurt

Acesta este modulul de teste business J2; SQL are separat teste native ale bibliotecii. Observerul Kafka al framework-ului este implementat și probat pe broker real; [Setup](../atf-setup/README.md) are deja o probă Java de infrastructură validată pe broker real, separată de integrarea Eventuate.

Modulul compune platforma ATF cu trei oracole deterministe pentru Eventuate:
contract API, convergență cu retry de citire și validarea dovezilor de mesagerie. Trei componente SQL embedded verifică separat witness-ul JDBC pe H2.
Suportul reutilizabil este în main; adaptorul JUnit și fixture-urile sunt în test.

| Responsabilitate | Limită |
|---|---|
| Deține | clientul HTTP scriptat, convergența, witness-ul JDBC, oraclele de dovezi și compoziția raportului |
| Kit | evaluarea unică a comparațiilor, lifecycle-ul fazelor și scope-ul class/suite |
| Reporting JAR | contractele publice, redacția, preview-ul, HTML-ul și navigarea Extent/fallback |
| Aplicația reală | saga, rezervarea creditului, deduplicarea, outbox-ul și proiecția CQRS; neexecutate în acest increment |

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Fixture["Fixture"] -->|MOCK| Client["Client"]
    Client -->|valori| Kit["Kit"]
    SQL["Witness JDBC"] -->|SQL_EMBEDDED| Kit
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
| [EventuateApiClient](src/main/java/atf/eventuate/api/EventuateApiClient.java) | MOCK implicit; LIVE prin compoziție explicită, POST o tentativă; politica de GET depinde de profil |
| [OrderAwaiter](src/main/java/atf/eventuate/await/OrderAwaiter.java) | clock/pause injectate, buget unic și OrderContract integral; observațiile respinse rămân în istoric |
| [MessageEvidenceContract](src/main/java/atf/eventuate/validation/MessageEvidenceContract.java) | identitate, livrări distincte și witness independent; nu implementează deduplicarea aplicației |
| [JdbcOrderEffectsReader](src/main/java/atf/eventuate/sql/JdbcOrderEffectsReader.java) | witness JDBC independent; [contract și atlas de pachet](src/main/java/atf/eventuate/sql/README.md), cu H2 exclusiv în test |
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

## SQL embedded — witness independent

[Suita SQL](src/test/java/atf/eventuate/tests/suites/EventuateSqlComponentSuite.java)
selectează exact trei clase: tranzacții, identitate/duplicate și failure JDBC.
[Atlasul pachetului SQL](src/main/java/atf/eventuate/sql/README.md) deține
schema fixture-ului, durata de viață și contractul main/test.

```powershell
mvn -B -pl atf-test-eventuate -am "-Dtest=EventuateSqlComponentSuite" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

H2 este driver test-scoped cu versiune fixată; main folosește numai JDBC din
JDK. Witness-ul citește prin altă conexiune, cu expected fixat independent,
iar raportul păstrează SQL_EMBEDDED separat de MOCK_EVENT_FEED și HTTP.

[Matricea SQL](../docs/ATF_Eventuate_Testing.md#sql-embedded--acceptanță-consumer)
acoperă individual, suită, combinație cu cele trei MOCK și failure-uri controlate.
Probele locale nu certifică saga, SQL-ul aplicației sau Kafka.

## Snapshot-uri terminale și faze

Clientul publică un rezultat Completed sau un Terminal cu snapshot-ul Core și
aceeași excepție originală. Terminal nu fabrică răspuns HTTP; lipsa răspunsului
rămâne explicită în proiecția bibliotecii.

| Boundary | Contract |
|---|---|
| EventuateApiClient | un singur callback per execuție; eroarea capturării devine suppressed, fără reexecuție |
| EventuateExchange | sumă typed Completed / Terminal; request, provenance și response availability provin din Core |
| EventuateEvidence | ordine per invocare și fază; entries include ambele variante, exchanges este numai proiecția Completed compatibilă |
| adaptor JUnit | scope SETUP și TEARDOWN restaurat după excepții; TEST BODY este faza implicită |
| publisher | publică fiecare intrare în faza sa, inclusiv când body-ul nu pornește; păstrează tentativele și timpul capturat |
| assertions audit | status derivat din comparații; eroarea execuției rămâne pe fază/request, fără a marca FAIL comparațiile trecute |
| verdict | excepția neconsumată produce FAIL; o excepție așteptată și verificată prin assertions păstrează PASS-ul testului și faptele transportului |

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Core["Core<br/>result / snapshot"] -->|capturează| Client["Client"]
    Client -->|Completed / Terminal| Evidence["Evidence<br/>fază + ordine"]
    Evidence -->|snapshot| Publisher["Publisher"]
    Client -->|aceeași excepție| JUnit["JUnit"]
    JUnit -->|verdict fază| Publisher
    Publisher -->|API JAR| Report["Extent / fallback"]
```

<details>
<summary><strong>Legendă — fapte capturate și verdict independent</strong></summary>

- Clientul capturează snapshot-ul înainte să propage excepția; callback-ul pentru Completed este separat de catch-ul execuției.
- Collectorul rămâne serial și local invocării; scope-ul fazei nu transferă ownership între thread-uri asincrone.
- HTTP evidence source reflectă provenance-ul capturat; NO_HTTP_EXECUTION nu certifică sursa evenimentelor de mesagerie.
- Request-ul terminal păstrează cauza și outcome-ul; callback-ul de finalizare folosește verdictul fazei, fără să transforme o excepție așteptată în test FAIL.
- Indexul Fails poate prezenta diagnosticul și execuția ca dovezi distincte; snapshot-ul request-ului este publicat o singură dată.

</details>

[Regresia terminală](src/test/java/atf/eventuate/tests/unit/EventuateTerminalEvidenceTest.java)
și [acceptanța artifactului](../docs/ATF_Eventuate_Testing.md#snapshot-terminal--acceptanță-consumer)
protejează cauza originală, faza, lipsa răspunsului și nominalele existente.
## Istoricul convergenței — snapshot terminal

[OrderAwaiter](src/main/java/atf/eventuate/await/OrderAwaiter.java) capturează
un [snapshot imuabil](src/main/java/atf/eventuate/await/OrderConvergenceSnapshot.java)
la convergență, deadline sau eșec. Istoricul convergenței și snapshot-ul HTTP
sunt dovezi distincte, păstrate în aceeași fază a invocării.

| Boundary | Contract stabil |
|---|---|
| execuție | un singur buget; ID, stare și rejectionReason evaluate împreună; niciun replay pentru captură sau raport |
| observație | citirea finalizată este păstrată înaintea accesului următor la clock; eșecul clock-ului nu șterge ordinul observat |
| timp | zero măsurat este diferit de unknown; timpul necunoscut rămâne Optional.empty, inclusiv în fiecare observație |
| collector | constructorul cu Consumer publică exact un snapshot terminal; constructorul existent rămâne compatibil |
| excepție | aceeași instanță și cauză sunt propagate; metadatele sunt suppressed, iar eroarea collectorului este secundară |
| recuperare | OrderConvergenceFailure.find caută în suppressed/cause fără cicluri; collectorul explicit este autoritar dacă excepția dezactivează suppression |
| rezultat normal | Result rămâne compatibil; lastObservedOrder oferă Optional, iar last păstrează excepția istorică pentru lista goală |
| raport | observations neutre, colapsabile, cu expected, istoric și oprire; comparațiile și verdictul testului rămân independente |

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Execute["Citire / pauză"] -->|capturează| Snapshot["Snapshot terminal"]
    Snapshot -->|o singură dată| Collector["Collector pe fază"]
    Snapshot -->|metadate| Original["Aceeași excepție"]
    Collector -->|fapte| Publisher["Publisher"]
    Publisher -->|observations| Report["Extent / fallback"]
```

<details>
<summary><strong>Legendă — execuție, captură și proiecție</strong></summary>

- Snapshot-ul conține expected independent, buget, interval, observații ordonate, elapsed opțional și motivul terminal.
- Read, pause și clock au motive distincte; eșecul capturării după convergență nu este reclasificat drept read failure.
- Collectorul este local invocării și serial; callback-ul nu reîncearcă execuția și nu consumă încă o observație.
- ConvergenceEvidencePublisher proiectează numai fapte prin API-ul J8. Nu inventează request HTTP sau assertion și nu deține HTML, redacția ori preview-ul de 240.
- Un eșec neconsumat produce FAIL; un eșec așteptat și verificat explicit păstrează PASS și aceeași observație terminală neutră.

</details>

[Regresiile unitare](src/test/java/atf/eventuate/tests/unit/EventuateConvergenceEvidenceTest.java)
și [matricea consumer](../docs/ATF_Eventuate_Testing.md#convergență--acceptanța-istoricului-terminal)
protejează cauza originală, absența observațiilor, întreruperea, clock-ul și lipsa replay-ului.

## Limite curente ale scaffold-ului

| Criteriu rămas | Owner și probă de închidere |
|---|---|
| identitate și deadline | [convergență](../docs/feedback/ATF_Eventuate_Feedback.md#evt-convergence-contract-86--deadline-al-citirii-live): identitatea și overflow sunt protejate; rămâne propagarea bugetului către citirea bounded LIVE |
| notificare consolă | [politica căii](../docs/feedback/ATF_Eventuate_Feedback.md#evt-console-path-79--prioritate-79): adaptorul emite direct calea și ocolește console.enabled |

Identitatea integrală și decodarea fără overflow au regresii în [EventuateOrderContractTest](src/test/java/atf/eventuate/tests/unit/EventuateOrderContractTest.java) și în componenta de convergență. API-ul await primește OrderContract, nu o stare izolată. ID-ul, starea și rejectionReason trebuie să corespundă în aceeași observație; istoricul păstrează observațiile respinse.

Celelalte criterii de mai sus și integrarea Kafka reală rămân deschise.


Fixture-ul terminal after-success serializează răspunsul înaintea execuției măsurate. Deadline-ul Core de 250 ms și proba care păstrează excepția originală a sink-ului rămân distincte de pregătirea datelor de test.

## LIVE și SQL generic — intrări curente

- [Atlasul LIVE](src/main/java/atf/eventuate/live/README.md) delimitează portul bounded, observerul și schema candidat. [atf-sql](../atf-sql/README.md) deține execuția JDBC generică; mapperul aplicației rămâne în Eventuate.
- [Dovada](../docs/ATF_SQL_Live_Verification.md) confirmă Kafka/MySQL reale pe memorie limitată, nu aplicația cu saga. Cele trei E2E sunt opt-in și încă NOT RUN pe aplicația completă.

## Profilul artifactului și oracole independente

Acceptoarele Eventuate și Mock declară intenția înaintea publicării/inspecției: renderer, scenarii, faze, inventar și verdicturi. Profilul verifică forma și ownership-ul; oracolele existente verifică separat identitatea, efectele business și valorile complete.

[Probe individual/suită](../docs/ATF_Eventuate_Testing.md) păstrează nominalele și FAIL-urile controlate în Extent/fallback. Rezultatul legacy rămâne în receipt, iar un mutant asupra tabelului original trebuie refuzat fără a folosi HTTP fictiv.
