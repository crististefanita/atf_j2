# Eventuate — testare și acceptanță

## Pe scurt

Cele trei teste MOCK și trei componente SQL embedded au suite distincte și folosesc aceeași extensie de audit. Observerul framework-ului are probă reală, dar saga rămâne NOT RUN; proba Java Setup pe Kafka real are [acceptanță de infrastructură separată](../atf-setup/README.md#kafka-remote--provisioning-și-client-extern).

Componenta este aleasă explicit prin `@EventuateComponentTest`, compusă cu
adnotarea Kit și extensia de audit. Cele trei cazuri MOCK normale au fiecare un
test executabil; controalele negative rămân în aceleași oracole.

| Caz | Ce verifică |
|---|---|
| EventuateApiContractComponentTest | identitate, stare, rejectionReason și probă sintetică 240 și proveniență HTTP mock |
| EventuateConvergenceComponentTest | retry 503 pe GET, trei observații nominale, o singură creare; ID străin refuzat în bugetul original |
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
| `atf.eventuate.probe` | `none`; `late-mismatch`, `duplicate-effect` și `wrong-identity` sunt probe controlate |

Extensia folosește `@AtfReportPath`: calea absolută apare numai după close
reușit. Configurația display este capturată la owner, iar comparația precede
preview-ul de 240 de caractere.

## Acceptanța raportului

<details>
<summary><strong>Matrice, comandă și oracol publicat</strong></summary>

`EventuateReportAcceptance` este un CLI test-scoped, nu un al patrulea test
de componentă. Primește un director absent și verifică 18 invocări JUnit,
șase failure-uri intenționate și 14 rapoarte proaspete.

Fiecare renderer produce trei rapoarte individuale, o suită comună și trei
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
artifact de hash, populație, failure-urile așteptate, profilul declarat și diagnosticul legacy separat.
Oracolul consumerului cere exact trei tabele de assertions per scenariu,
patru coloane și comparațiile nominale; eliminarea unui tabel sau inversarea
unui verdict sunt refuzate. Scripturile și CSS-ul nu satisfac acest oracol.

Cele trei acceptoare Eventuate, Mock și SQL declară profilul înaintea inspecției și păstrează oracolele business separate. Lipsa HTTP nu schimbă profilul după citirea HTML-ului.

- Eventuate declară cele trei faze, request-urile necesare, observațiile de convergență și comparațiile oprite la failure-ul controlat.
- Mock declară assertions-only din probele autorului; SQL declară observațiile și rândurile fiecărei metode, fără request-uri artificiale.
- Receipt-ul păstrează rezultatul legacy Extent/HTTP ca diagnostic distinct. Conformitatea acceptanței este verdictul profilului explicit.
- Un mutant modifică numai coloana ACTUAL a primului tabel original; template-urile, scripturile și copiile runtime nu pot satisface ownerul pierdut.

Controlul `REPORT-J2-EXPLICIT-PROFILE-130` păstrează proba adoptării. Matricele mai vechi Extent/fallback rămân dovezi ale snapshoturilor executate. [Baseline](ATF_Test_Changes_Baseline.md) · [Registrul execuțiilor](ATF_J2_Verification.md)

</details>

## Niveluri viitoare

Integration verifică adaptoare împreună cu infrastructură reală; E2E verifică
aplicația și efectele sale. Kafka, SQL, Mongo și browserul business nu sunt
executate de cele trei componente mock.

## Acceptanța semantică Validation

[ValidationReportAcceptance](../atf-test-eventuate/src/test/java/atf/eventuate/tests/support/acceptance/ValidationReportAcceptance.java)
exercită portul neutru public pe JAR-ul Reporting existent. Nouă fixture-uri
produc câte un Extent și un fallback: nominal, JSON invalid, missing/null,
null explicit, additionalProperties, items false, schemă invalidă, warning și
comparație integrală cu preview 240.

Oracolul cere un singur tabel agregat, patru coloane, toate comparațiile și
severitatea corectă; eliminarea coloanei ACTUAL sau coruperea verdictului
oprește acceptanța. Fixture-urile nu expediază cereri și sunt etichetate synthetic.
Dependency-ul Validation este test-scoped, fără cod Reporting copiat în J2.

După build, folosește același classpath și alt director absent:

```powershell
$validationOutput = Join-Path ([IO.Path]::GetTempPath()) ("j2-validation-" + [guid]::NewGuid().ToString("N"))
java -Xmx384m -cp $eventuateCp atf.eventuate.tests.support.acceptance.ValidationReportAcceptance $validationOutput
```

[Regresiile unitare Eventuate](../atf-test-eventuate/src/test/java/atf/eventuate/tests/unit/EventuateOrderContractTest.java)
acoperă separat limitele signed long, overflow, documente ambigue, identitatea
schimbată, rejectionReason și răspunsul corect sosit după deadline. Componenta
păstrează assertions colapsabile; unitarele nu moștenesc lifecycle-ul raportului.
## Snapshot terminal — acceptanță consumer

[TerminalEvidenceReportAcceptance](../atf-test-eventuate/src/test/java/atf/eventuate/tests/support/acceptance/TerminalEvidenceReportAcceptance.java)
lansează oracolul API existent individual și prin suita canonică, fără clase
business suplimentare. Cele 36 de artifacte Extent/fallback acoperă script epuizat,
transport, timeout, cancel, retry urmat de eșec și succes urmat de eșec.

| Variantă | Dovadă cerută |
|---|---|
| failure în TEST BODY | exact excepția capturată ajunge la JUnit; request, tentative, timing și no-response sunt prezente |
| failure în SETUP | snapshot-ul este în SETUP, chiar când TEST BODY nu este executat |
| failure în TEARDOWN | răspunsul nominal din body și failure-ul ulterior rămân în faze distincte |
| excepție așteptată | assertions verifică explicit outcome, identitate și absența răspunsului; JUnit și raportul păstrează PASS |
| mutanți de artifact | snapshot eliminat, fază greșită și coloană eliminată sunt refuzate |

După build, folosește classpath-ul de acceptanță și un director absent:

```powershell
$terminalOutput = Join-Path ([IO.Path]::GetTempPath()) ("j2-terminal-" + [guid]::NewGuid().ToString("N"))
java -Xmx384m -cp $eventuateCp atf.eventuate.tests.support.acceptance.TerminalEvidenceReportAcceptance $terminalOutput
```

Proprietățile atf.eventuate.terminal și atf.eventuate.terminal.phase sunt exclusiv probe test-scoped; CLI-ul le
restaurează la ieșire. Timeout-ul scurt al fixture-ului nu schimbă bugetele
binding-ului implicit și nu certifică un SLA de rețea real.

ExpectedTerminalProbe este fixture tehnic separat, selectat explicit de CLI; nu înlocuiește expected-ul business din EventuateApiContractComponentTest. Cele patru probe PASS rulează fixture-ul individual sau împreună cu ceilalți doi consumatori, într-un scope comun.

## Convergență — acceptanța istoricului terminal

[ConvergenceEvidenceReportAcceptance](../atf-test-eventuate/src/test/java/atf/eventuate/tests/support/acceptance/ConvergenceEvidenceReportAcceptance.java)
lansează JUnit pe aceleași clase și pe JAR-ul fizic consumat. Produce 24 de
rapoarte: două renderere, individual/suită și șase variante per combinație.

| Variantă | Oracol independent |
|---|---|
| read, pause, empty, sink | exact un FAIL neconsumat, aceeași excepție la JUnit; în suită ceilalți doi consumatori rămân PASS |
| expected-read | fixture tehnic separat, cinci comparații auditabile; test PASS cu observație READ_FAILURE neutră |
| expected-expired | deadline înaintea primei citiri, zero observații și istoric gol; fără un ultim ordin inventat |
| fără replay | o creare; zero, una sau două citiri și exact numărul de dispatch-uri fixat de caz |
| artifact | un HTML per lansare, fază corectă, expected 42/APPROVED, buget 25 ms, interval 10 ms, timp și istoric capturate |
| redacție și mutanți | secretul sintetic absent; snapshot omis, expected pierdut, fază/istoric corupte și coloană ACTUAL eliminată sunt refuzate |

După build, folosește classpath-ul de acceptanță și un director absent:

```powershell
$convergenceOutput = Join-Path ([IO.Path]::GetTempPath()) ("j2-convergence-" + [guid]::NewGuid().ToString("N"))
java -Xmx768m -cp $eventuateCp atf.eventuate.tests.support.acceptance.ConvergenceEvidenceReportAcceptance $convergenceOutput
```

Proprietatea atf.eventuate.convergence este exclusiv test-scoped, implicit none;
CLI-ul o restaurează la ieșire. Probele folosesc transportul finit, clientul
real ATF și un clock virtual; nu pornesc infrastructură sau mutații LIVE.

[EventuateConvergenceEvidenceTest](../atf-test-eventuate/src/test/java/atf/eventuate/tests/unit/EventuateConvergenceEvidenceTest.java)
verifică separat timpul necunoscut, observația finalizată înaintea defectării
clock-ului, întreruperea, Error, suppression dezactivat și invocările repetate.
Aceste cazuri unitare nu sunt revendicate drept randări distincte ale matricei.

Auditul TAB MCP verifică:

- snapshot-ul și body-ul JSON până la ultimul rând, inclusiv Enter/Space;
- cele patru coloane la assertions și ultima comparație;
- Fails și întoarcerea exactă la cauza originală în Tests.

[Contractul stabil](../atf-test-eventuate/README.md#istoricul-convergenței--snapshot-terminal)
păstrează observations separate de comparații și de verdict.

## SQL embedded — acceptanță consumer

[EventuateSqlComponentSuite](../atf-test-eventuate/src/test/java/atf/eventuate/tests/suites/EventuateSqlComponentSuite.java)
selectează exact trei componente. H2 2.5.252 este test-scoped; main depinde
numai de JDBC, iar schema atf_order_reservations este fixture ATF.

| Componentă | Dovadă |
|---|---|
| EventuateSqlTransactionComponentTest | conexiuni distincte, necomis invizibil, commit, rollback, cinci SELECT-uri/conexiuni închise și baza absentă după cleanup |
| EventuateSqlIdentityComponentTest | zero rânduri, ID străin, efect unic, duplicat permis de schemă și refuzat de oracol, sumă/stare greșită, zecimale mari și baze izolate |
| EventuateSqlFailureComponentTest | SQL invalid real, aceeași SQLException/cauză, cleanup suppressed, rând parțial FAILED, un callback și sink fără replay |

Din staging TMP, după bootstrap:

```powershell
mvn -B -pl atf-test-eventuate -am "-Dtest=SqlEffectsContractTest,EventuateSqlComponentSuite" "-Dsurefire.failIfNoSpecifiedTests=false" test
$sqlOutput = Join-Path ([IO.Path]::GetTempPath()) ("j2-sql-" + [guid]::NewGuid().ToString("N"))
java -Xmx768m -cp $eventuateCp atf.eventuate.tests.support.acceptance.sql.SqlEvidenceReportAcceptance $sqlOutput
```

Regenerează classpath-ul de acceptanță după schimbarea POM-ului: include acum
H2 test-scoped. CLI-ul cere director absent și restaurează proprietățile la
ieșire; atf.eventuate.sql.probe este implicit none și exclusiv test-scoped.

<details>
<summary><strong>Matricea SQL și oraclele negative</strong></summary>

- 18 HTML-uri: fiecare dintre cele trei clase individual, suita SQL, combinația cu cele trei MOCK și probele duplicate/sql-error individual/prin suită, în Extent și fallback.
- 40 invocări JUnit, opt FAIL-uri controlate, zero SKIP ori failure de container. Pentru sql-error, instanța originală ajunge la JUnit; pentru duplicate, cauza este exact comparația 1 versus 2.
- Oracolul verifică rândurile și ordinea lor față de constante independente, SQL_EMBEDDED, ID cerut, outcome, timp, fază și patru coloane de assertions. Secretul sintetic din cauză este absent din HTML.
- Fiecare snapshot este delimitat înaintea parsării. Câmp absent, fază greșită, outcome corupt, valoare zecimală schimbată, coloană eliminată și verdict mutat pe altă comparație sunt refuzate.
- Tabelele păstrează comparațiile PASS când eșecul este al execuției. SQL FAILED este observație neutră; un failure așteptat și verificat nu produce verdict fals FAIL.

Reauditarea acelorași artifacte, fără relansarea testelor:

```powershell
java -Xmx384m -cp $eventuateCp atf.eventuate.tests.support.acceptance.sql.SqlArtifactAudit $sqlOutput
```

În TAB MCP verifică metadatele, JSON până la ultimul rând, Enter/Space,
scroll-ul interior al assertions și întoarcerea Fails în Tests. [Contractul SQL](../atf-test-eventuate/src/main/java/atf/eventuate/sql/README.md)
separă limitele adaptorului de acceptanța aplicației.

</details>

## SQL generic și LIVE cu RAM limitat

[Incrementul verificat](ATF_SQL_Live_Verification.md) adaugă matricea nativă SQL și proba infrastructurii Kafka/MySQL. [Suita LIVE](../atf-test-eventuate/src/test/java/atf/eventuate/tests/suites/EventuateLiveE2eSuite.java) este finită și opt-in; un skip sau compilarea nu închid EVT-LOCAL-01. Rularea ei pe aplicație cere readiness HTTP/CDC și mapping SQL confirmat, fără replay de POST.
