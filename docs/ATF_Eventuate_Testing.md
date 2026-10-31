# Eventuate — testare și acceptanță

## Pe scurt

J2 este WIP pentru Kafka/mesagerie cu Setup Podman. Cele trei teste de componentă existente folosesc numai fixture-uri MOCK; acest ghid nu declară un client Kafka Java sau o acceptanță reală implementate.

Componenta este aleasă explicit prin `@EventuateComponentTest`, compusă cu
adnotarea Kit și extensia de audit. Cele trei cazuri normale au fiecare un
test executabil; controalele negative rămân în aceleași oracole.

| Caz | Ce verifică |
|---|---|
| EventuateApiContractComponentTest | identitate, stare, rejectionReason și probă sintetică 240 și proveniență HTTP mock |
| EventuateConvergenceComponentTest | retry 503 pe GET, trei observații, o singură creare și buget nereînnoit |
| EventuateMessageEvidenceComponentTest | două livrări simulate, un singur efect și refuzul dovezilor incomplete sau stale |

## Lansare individuală și suită

```powershell
mvn -B -pl atf-test-eventuate -Dtest=EventuateApiContractComponentTest test
mvn -B -pl atf-test-eventuate -Dtest=EventuateComponentSuite test
mvn -B -pl atf-tests -Dtest=J2GovernanceSuite test
```

Bootstrap-ul Maven instalează JAR-ul Reporting cu POM-ul real prin root
`validate`. Înaintea comenzilor module-only, construiește dependențele conform
[README](../README.md); în automatizare toate acestea se execută în TMP.

## Configurație efectivă

| Proprietate | Default și contract |
|---|---|
| `atf.eventuate.report.renderer` | `EXTENT`; alternativă explicită `FALLBACK` |
| `atf.eventuate.report.directory` | `target/eventuate-reports`, față de working directory capturat |
| `atf.test.run.scope` | `class`; suita declară `suite` |
| `atf.eventuate.probe` | `none`; numai `late-mismatch` și `duplicate-effect` sunt probe controlate |

Extensia folosește `@AtfReportPath`: calea absolută apare numai după close
reușit. Configurația display este capturată la owner, iar comparația precede
preview-ul de 240 de caractere.

## Acceptanța raportului

<details>
<summary><strong>Matrice, comandă și oracol publicat</strong></summary>

`EventuateReportAcceptance` este un CLI test-scoped, nu un al patrulea test
de componentă. Primește un director absent și verifică 16 invocări JUnit,
patru failure-uri intenționate și 12 rapoarte proaspete.

Fiecare renderer produce trei rapoarte individuale, o suită comună și două
probe FAIL. Orice container failure, test lipsă, SKIP, artifact lipsă sau
eroare în afara assertion-ului oprește acceptanța.

Inspecția HTML executabilă precede auditul vizual în TAB MCP.
Verifică taburile, expandarea până la ultima comparație, lățimea tabelelor,
wrapping-ul și navigarea la failure; structura HTML singură nu probează randarea.

Din rădăcina copiei TMP, după build-ul dependențelor:

```powershell
mvn -B -pl atf-test-eventuate dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/acceptance.cp
$eventuateCp = (Get-Content atf-test-eventuate/target/acceptance.cp -Raw).Trim() + ';atf-test-eventuate/target/test-classes;atf-test-eventuate/target/classes'
$eventuateOutput = Join-Path ([IO.Path]::GetTempPath()) ("j2-evidence-" + [guid]::NewGuid().ToString("N"))
java -Xmx384m -cp $eventuateCp atf.eventuate.tests.support.acceptance.EventuateReportAcceptance $eventuateOutput
```

Directorul rezultatului trebuie să nu existe. `acceptance.json` leagă fiecare
artifact de hash, populație, failure-urile așteptate și inspectorul generic.
Oracolul consumerului cere exact trei tabele de assertions per scenariu,
patru coloane și comparațiile nominale; eliminarea unui tabel sau inversarea
unui verdict sunt refuzate. Scripturile și CSS-ul nu satisfac acest oracol.

J6 are un gap de aplicabilitate în `ReportArtifactInspector`: cere markeri
HTTP și pentru scenarii exclusiv de mesagerie; fallback-ul este verificat
față de criterii Extent. Rezultatele originale rămân în receipt, fără a declara
inspectorul universal verde. Ownerul și închiderea sunt în
[feedback-ul Reporting](feedback/ATF_Reporting_Consumer_Feedback.md).

</details>

## Niveluri viitoare

Integration verifică adaptoare împreună cu infrastructură reală; E2E verifică
aplicația și efectele sale. Kafka, SQL, Mongo și browserul business nu sunt
executate de cele trei componente mock.
