# Feedback J2 — framework Kafka și mesagerie · WIP

## Pe scurt

Criteriile susțin un singur modul de test Kafka și infrastructura configurată prin Setup PowerShell în Podman. Observerul Java este probat pe broker real, iar atf-sql are regresii proprii; aplicația Eventuate completă rămâne NOT RUN.

Registrul urmărește numai deployment-ul Eventuate, Setup și platforma efectiv
consumată. Reporting se întreține în J0; Shop/AutoEx, UI și Debug UI au owneri
în J1. Sincronizarea surselor nu dublează backlog-ul lor în J2.

| Ordine | Owner J2 / impact local | Prioritate | Dovada care lipsește |
|---:|---|---:|---|
| 1 | [Eventuate](ATF_Eventuate_Feedback.md) | 90 | readiness HTTP/CDC, schema și trei cazuri reale individual/prin suită |
| 2 | [Adoptare Reporting](ATF_Reporting_Consumer_Feedback.md) | 89 | SQL semantic și geometria RESULT rămase la ownerul J0 |
| 3 | [SQL](ATF_SQL_Feedback.md) | 85 | mapper aplicație corelat cu snapshot-urile generice și schema reală |
| 4 | [Setup](ATF_Setup_Feedback.md) | 84 | cauza primară la eșecul receipt-ului, ownership fizic și reluare după restart |
| 5 | [Documentație](ATF_Documentation_Feedback.md) | 81 | prevenirea contradicțiilor dintre deployment și afirmațiile curente din pagini |
| 6 | [Mock](ATF_Mock_Feedback.md) | 78 | plan finit de fault-uri reutilizabil, fără duplicarea celor două fixture-uri |

Scorurile sunt priorități de intervenție, nu note ale modulelor. Ordinea
executării ține cont de dependențe: un engine și un witness funcționale
preced certificarea saga sau idempotency business.

- Topul cu implementare în J2 este Eventuate LIVE 90, idempotency reală 88 și
  witness-ul SQL al aplicației 85. Idempotency are drept precondiție laboratorul LIVE;
  citirea bounded 84 intră în primul increment, înaintea acceptanței rețelei.
- Adoptarea Reporting 89 depinde de J0. SQL query/results este
  [cerut în J0](ATF_Reporting_Consumer_Feedback.md#sql--handoff-către-j0-și-adoptare-ulterioară),
  cu limite independente configurabile; nu este o funcționalitate J9 livrată.
- Precizia numerică Mock este închisă pe Core C2: aceeași probă publică
  selectează 201 pentru nominal și 409 pentru valoarea distinctă și control.
  [Contractul livrat](../../../atf_j0/atf-mock/README.md#json-strict-și-potrivire-numerică-exactă)
  păstrează regula; rândul rezidual 86 este retras, fără ștergerea regresiilor.

Parsarea JSON, fixture-urile complete, schemele, contractul JSON documentat o singură dată, egalitatea scalară, media type și identitatea integrală au contracte și regresii
permanente; criteriile rezolvate sunt retrase din registrul activ. Snapshot-ul terminal al convergenței are acum contract și probe permanente; incrementul SQL embedded are contract și probe permanente, fără pretenție de acceptanță saga.

<details>
<summary><strong>Harta completă a criteriilor active</strong></summary>

| Criteriu | Prioritate | Owner și dovadă de închidere |
|---|---:|---|
| EVT-LOCAL-01 | 90 | readiness și aplicație LIVE individual/suită; observerul are deja probă reală |
| EVT-IDEMPOTENCY-02 | 88 | duplicat de transport, business și POST separate, cu efecte independente |
| EVT-CONVERGENCE-CONTRACT-86 | 84 | probă lentă/blocată HTTP real; portul bounded este implementat |
| SQL-APPLICATION-WITNESS-118 | 85 | schema și mapping real, fapte JDBC generic păstrate în proiecție |
| SQL-DIAGNOSTIC-CAPTURE-120 | 82 | mesaje și suppressed bounded în snapshot, originalul păstrat |
| SQL-DRIVER-CONTRACT-119 | 77 | contracte negative pentru fiecare driver efectiv adoptat |
| SETUP-PRIMARY-FAILURE-87 | 84 | eșecul scrierii receipt-ului păstrează cauza inițială |
| DOC-DEPLOYMENT-FACTS-105 | 81 | mutanți pentru catalog/versiune curentă și istoric legitim; deriva este reconfirmată după J8 |
| SETUP-REMOTE-TRANSPORT-84 | 80 | ownership, transport durabil și reluare după restart |
| EVT-KAFKA-CAPTURE-121 | 84 | bounded keys, all retained fields and aggregate Kafka evidence |
| SQL-HARNESS-DIAGNOSTICS-122 | 79 | original JUnit failure, exact counts and partial receipt retained |
| EVT-CONSOLE-PATH-79 | 79 | console.enabled și opt-in respectate de adaptorul J2 |
| MOCK-FAULT-SCRIPT-111 | 78 | plan finit de fault-uri comun celor două fixture-uri, cu ownership și regresii native |
| SETUP-PROBE-COVERAGE-85 | 78 | contracte pure pentru utilitarul Java în afara Maven |
| SETUP-STAGE-RUNNER-86 | 76 | un protocol de etape, cauză primară și NOT_RUN |

</details>

## Feedback pentru biblioteci

Orice constatare despre Core, Reporting, Console sau Mock se înregistrează direct la ownerul J0, conform [ghidului bibliotecilor](../../lib/README.md). Aici rămân problemele domeniului și dovezile acceptanței locale, cu o trimitere la criteriul comun.


## Ce este livrat și unde se citește

- Coeziunea Kit este verificată în [contractul ownerului](../../lib/README.TestKit.md#scope-și-integrare); criteriul închis nu rămâne în registrul activ.
- Cele trei componente MOCK, trei SQL embedded și matricile Extent/fallback au contract în [ghidul Eventuate](../ATF_Eventuate_Testing.md).
- Hostul și provisioning-ul silent au contract în [Setup](../../atf-setup/README.md) și [atlasul său](../ATF_Setup_Atlas.md).
- Catalogul explicit cu opt module și retragerea browserului sunt în [scopul J2](../ATF_J2_Scope.md); criteriul închis nu rămâne ToDo.
- [Verificarea executată](../ATF_J2_Verification.md) păstrează limitele dovezilor; MOCK nu certifică broker sau efecte persistente.
- Infrastructura Kafka validată nu este redeschisă; gap-urile noi sunt reguli semantice ori contracte operaționale distincte.

## Owneri externi, fără copii locale de backlog

| Domeniu | Registru canonic |
|---|---|
| Core și validarea documentului JSON | [J0 Core](../../../atf_j0/docs/feedback/ATF_Core_Feedback.md) |
| Reporting și mesagerie în renderer | [J0 Reporting](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md) |
| regresii generice Reporting | [J0 Reporting Tests](../../../atf_j0/docs/feedback/ATF_Reporting_Tests_Feedback.md) |
| Shop/AutoEx | [J1 business](../../../atf_j1/docs/ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită) |
| Debug UI | [J1 Debug](../../../atf_j1/atf-debug-ui/README.md#identitatea-path-ului-uri) |

Fișierele scurte Validation, Reporting și AI păstrează orientarea către
ownerii contractelor și țintele existente; nu conțin ToDo-uri J2 active.
La reconcilierea țintelor se pot retrage explicit, fără skip după absență.

## Curățare și închidere

Contractul stabil merge în documentația ownerului; feedback-ul păstrează
numai gap-ul, prioritatea și proba lipsă. Copia de deployment nu închide un
criteriu comun numai pentru că a fost reparat într-un alt proiect.

[Baseline-ul testChanges](../ATF_Test_Changes_Baseline.md) păstrează controalele
eligibile ale diferenței Git. Un control VERIFIED nu se șterge automat odată
cu ToDo-ul și nu se rebazează pentru obținerea unui gate verde.

- [SQL generic](../../atf-sql/README.md) și [dovada SQL/LIVE](../ATF_SQL_Live_Verification.md) păstrează contractele livrate. Nu redeschideți observerul, query/binds typed sau captura bounded ca implementări lipsă; readiness-ul aplicației, anularea HTTP reală și rendererul SQL rămân criterii distincte.

## Live audit — 2026-11-01

This review changes feedback only. Scores are engineering judgements /100,
not coverage percentages, acceptance gates or the priorities above.

Deployment
code is **89**, documentation **89**, and the equal-weight quality-axis mean
below is **82**. Real Eventuate saga acceptance remains unverified.

| Module | Code /100 | Owner README /100 | Main reason below 95 |
|---|---:|---:|---|
| Test Kit | 92 | 90 | reuse existing scope/cleanup; avoid a renderer-aware runner |
| Mock | 93 | 87 | strong numeric contracts; contradictory current README claims |
| SQL | 85 | 92 | unbounded diagnostics and lossy application witness projection |
| Validation | 94 | 90 | strict pure contracts; distinguish reusable API from J2 active scope |
| Console | 93 | 92 | native policy is sound; Eventuate bypass belongs to its adapter |
| Setup | 82 | 90 | primary failure can be replaced by receipt publication |
| Test Eventuate | 84 | 89 | retained Kafka key budget; actual HTTP/CDC/schema acceptance missing |
| ATF Tests | 89 | 91 | structural guards do not prove current prose or application readiness |

Root README: 89; context atlas: 88; docs index: 86. These scores assess current
navigation, factual consistency, scope and reproducibility, not every historical
entry as though it were a current claim. Core C2/Reporting J10 are external;
consumer checks do not constitute a fresh review of their entire implementation.

<details>
<summary><strong>Quality axes and executed evidence</strong></summary>

| Axis | Score /100 | Constructive next proof |
|---|---:|---|
| Architecture / ownership | 92 | keep JDBC, Kafka, tests and rendering with their owners |
| Correctness | 87 | real schema/identity and independent application witnesses |
| Failure diagnostics | 83 | preserve Setup cause and SQL matrix failures |
| Resource bounds | 82 | SQL diagnostic and aggregate Kafka capture budgets |
| Audit / report integration | 88 | common inspector profile and metadata adoption from J0 |
| Tests / governance | 92 | semantic documentation negatives and retained failure evidence |
| Performance evidence | 70 | isolated/repeated phase timings, not a single green H2 run |
| Operational acceptance | 55 | actual Eventuate HTTP/CDC/saga remains NOT RUN |
| Documentation | 89 | reconcile current facts without rewriting dated receipts |

Owned TMP root: j2-quality-64e05d574c24; source snapshot: 439 files.

| Executed selection | Result and limit |
|---|---|
| platform / SQL / Eventuate defaults | Kit 133, Mock 94, SQL 14, Validation 217, Console 38 pass; Eventuate 69 pass / 3 explicit LIVE skips |
| governance with sibling source context | 175 checks pass; initial staging-only missing-link failures are retained, not classified as live defects |
| Eventuate matrix | 14 Extent/fallback HTML files; 18 invocations, exactly six controlled failures |
| SQL matrix | isolated 12 HTML files / 36 invocations / four controlled failures pass owner oracles and mutants; first SQL timeout is retained |
| generic J10 inspector | all 12 legitimate SQL-only reports remain noncompliant; existing J0 profile criterion stays open |
| browser samples | Extent late mismatch, Fails backlink, fallback SQL suite, Enter/Space disclosure and independent neighbors verified; effective preview remains 240 |
| negative review probes | oversized SQL diagnostics, retained 1 MiB Kafka key, Setup receipt masking and configured console bypass reproduced without infrastructure changes |

</details>

EVT-KAFKA-CAPTURE-121 (84) and SQL-HARNESS-DIAGNOSTICS-122 (79) join the active
owner registers. Close neither real LIVE acceptance nor a generic J0 criterion
from these controlled results. [Shared support handoff](../../../atf_j0/docs/feedback/ATF_Common_Test_Support_Feedback.md)
keeps the reusable boundary explicit; it is a proposal, not delivered code.

## Producer correlation — Core and Kit

[The J0 producer audit](../../../atf_j0/docs/feedback/README.md)
adds Core console-channel/cancellation and Kit Unicode-preview findings.
These are external source-owner criteria, not duplicate J2 TODO definitions.

- J2 reaccepts a future Core cancellation candidate against the real read
  boundary; it does not infer write rollback from future cancellation.
- Kit preview adoption preserves Unicode and the configured 240 limit.
- No generic fix closes SQL capture budgets, Kafka keys, readiness, CDC or saga.
- The existing J2 89/89/82 review grades are not increased by producer tests.

The Test Kit code score is reconciled to **92** after the producer Unicode
counterexample; this supersedes its earlier consumer-only 94. Deployment
totals retain their stated scope, with no new saga/ASR/device acceptance.
