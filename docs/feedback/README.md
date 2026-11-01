# Feedback J2 — framework Kafka și mesagerie · WIP

## Pe scurt

Criteriile susțin un singur modul de test Kafka și infrastructura configurată prin Setup PowerShell în Podman. Observerul Java este probat pe broker real, iar atf-sql are regresii proprii; aplicația Eventuate completă rămâne NOT RUN.

Registrul urmărește numai deployment-ul Eventuate, Setup și platforma efectiv
consumată. Reporting se întreține în J0; Shop/AutoEx, UI și Debug UI au owneri
în J1. Sincronizarea surselor nu dublează backlog-ul lor în J2.

| Ordine | Owner J2 / impact local | Prioritate | Dovada care lipsește |
|---:|---|---:|---|
| 1 | [Eventuate](ATF_Eventuate_Feedback.md) | 90 | readiness HTTP/CDC, schema și trei cazuri reale individual/prin suită |
| 2 | [Adoptare Reporting](ATF_Reporting_Consumer_Feedback.md) | 89 | profil artifact și timing demonstrate pe binarul consumat |
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
  [Contractul livrat](../../atf-mock/README.md#document-json-complet--expected-request-și-response)
  păstrează regula; rândul rezidual 86 este retras, fără ștergerea regresiilor.

Parsarea JSON, fixture-urile complete, schemele, contractul JSON documentat o singură dată, egalitatea scalară, media type și identitatea integrală au contracte și regresii
permanente; criteriile rezolvate sunt retrase din registrul activ. Snapshot-ul terminal al convergenței are acum contract și probe permanente; incrementul SQL embedded are contract și probe permanente, fără pretenție de acceptanță saga.

<details>
<summary><strong>Harta completă a criteriilor active</strong></summary>

| Criteriu | Prioritate | Owner și dovadă de închidere |
|---|---:|---|
| EVT-LOCAL-01 | 90 | readiness și aplicație LIVE individual/suită; observerul are deja probă reală |
| REPORT-J2-ADOPTION-62 | 89 | adoptare JAR cu profil artifact și acceptanță J2 distinctă |
| EVT-IDEMPOTENCY-02 | 88 | duplicat de transport, business și POST separate, cu efecte independente |
| REPORT-J2-TIMING-83 | 88 | latență controlată și intervale coerente captură/publicare |
| EVT-CONVERGENCE-CONTRACT-86 | 84 | probă lentă/blocată HTTP real; portul bounded este implementat |
| SQL-APPLICATION-WITNESS-118 | 85 | schema și mapping real, fapte JDBC generic păstrate în proiecție |
| SQL-DIAGNOSTIC-CAPTURE-120 | 82 | mesaje și suppressed bounded în snapshot, originalul păstrat |
| SQL-DRIVER-CONTRACT-119 | 77 | contracte negative pentru fiecare driver efectiv adoptat |
| SETUP-PRIMARY-FAILURE-87 | 84 | eșecul scrierii receipt-ului păstrează cauza inițială |
| DOC-DEPLOYMENT-FACTS-105 | 81 | mutanți pentru catalog/versiune curentă și istoric legitim; deriva este reconfirmată după J8 |
| SETUP-REMOTE-TRANSPORT-84 | 80 | ownership, transport durabil și reluare după restart |
| EVT-CONSOLE-PATH-79 | 79 | console.enabled și opt-in respectate de adaptorul J2 |
| MOCK-FAULT-SCRIPT-111 | 78 | plan finit de fault-uri comun celor două fixture-uri, cu ownership și regresii native |
| SETUP-PROBE-COVERAGE-85 | 78 | contracte pure pentru utilitarul Java în afara Maven |
| SETUP-STAGE-RUNNER-86 | 76 | un protocol de etape, cauză primară și NOT_RUN |

</details>

## Ce este livrat și unde se citește

- Coeziunea Kit este verificată în [contractul ownerului](../../atf-test-kit/README.md#pachetele-regresiilor-de-audit); criteriul închis nu rămâne în registrul activ.
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
