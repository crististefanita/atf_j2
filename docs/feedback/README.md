# Feedback J2 — framework Kafka și mesagerie · WIP

## Pe scurt

Criteriile susțin un singur modul de test Kafka și infrastructura configurată prin Setup PowerShell în Podman. Framework-ul și clientul/observerul Java sunt WIP, iar un rezultat MOCK nu certifică brokerul.

Registrul urmărește numai deployment-ul Eventuate, Setup și platforma efectiv
consumată. Reporting se întreține în J0; Shop/AutoEx, UI și Debug UI au owneri
în J1. Sincronizarea surselor nu dublează backlog-ul lor în J2.

| Ordine | Owner J2 / impact local | Prioritate | Dovada care lipsește |
|---:|---|---:|---|
| 1 | [Eventuate](ATF_Eventuate_Feedback.md) | 90 | aplicație și observer LIVE, deadline propagat și efecte independente |
| 2 | [Adoptare Reporting](ATF_Reporting_Consumer_Feedback.md) | 89 | profil artifact și timing demonstrate pe binarul consumat |
| 3 | [Mock](ATF_Mock_Feedback.md) | 86 | două valori decimale distincte pot selecta aceeași rută după conversia Double |
| 4 | [Setup](ATF_Setup_Feedback.md) | 84 | cauza primară la eșecul receipt-ului, ownership fizic și reluare după restart |
| 5 | [Documentație](ATF_Documentation_Feedback.md) | 77 | prevenirea contradicțiilor dintre deployment și afirmațiile curente din pagini |

Scorurile sunt priorități de intervenție, nu note ale modulelor. Ordinea
executării ține cont de dependențe: un engine și un witness funcționale
preced certificarea saga sau idempotency business.

Parsarea JSON, fixture-urile complete, schemele, egalitatea scalară, media type și identitatea integrală au contracte și regresii
permanente; criteriile rezolvate sunt retrase din registrul activ.

<details>
<summary><strong>Harta completă a criteriilor active</strong></summary>

| Criteriu | Prioritate | Owner și dovadă de închidere |
|---|---:|---|
| EVT-LOCAL-01 | 90 | aplicație, observer și efecte independente LIVE, individual/suită |
| REPORT-J2-ADOPTION-62 | 89 | adoptare JAR cu profil artifact și acceptanță J2 distinctă |
| EVT-IDEMPOTENCY-02 | 88 | duplicat de transport, business și POST separate, cu efecte independente |
| REPORT-J2-TIMING-83 | 88 | latență controlată și intervale coerente captură/publicare |
| MOCK-NUMERIC-MATCH-104 | 86 | matching numeric fără pierderea diferenței dintre valori distincte |
| EVT-CONVERGENCE-CONTRACT-86 | 84 | citirea LIVE bounded prin timpul rămas; identitatea și overflow sunt deja protejate |
| SETUP-PRIMARY-FAILURE-87 | 84 | eșecul scrierii receipt-ului păstrează cauza inițială |
| SETUP-REMOTE-TRANSPORT-84 | 80 | ownership, transport durabil și reluare după restart |
| EVT-CONSOLE-PATH-79 | 79 | console.enabled și opt-in respectate de adaptorul J2 |
| SETUP-PROBE-COVERAGE-85 | 78 | contracte pure pentru utilitarul Java în afara Maven |
| DOC-DEPLOYMENT-FACTS-105 | 77 | mutanți pentru catalog/versiune curentă și istoric legitim, fără relaxarea guvernanței |
| SETUP-STAGE-RUNNER-86 | 76 | un protocol de etape, cauză primară și NOT_RUN |
| DOC-CONTRACT-OWNER-107 | 74 | un singur contract Validation, cu limite, probe și ancore păstrate |

</details>

## Ce este livrat și unde se citește

- Cele trei componente MOCK și matricea Extent/fallback au contract în [ghidul Eventuate](../ATF_Eventuate_Testing.md).
- Hostul și provisioning-ul silent au contract în [Setup](../../atf-setup/README.md) și [atlasul său](../ATF_Setup_Atlas.md).
- Catalogul explicit cu șapte module și retragerea browserului sunt în [scopul J2](../ATF_J2_Scope.md); criteriul închis nu rămâne ToDo.
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
