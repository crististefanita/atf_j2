# Feedback J2 — framework Kafka și mesagerie · WIP

## Pe scurt

Criteriile susțin un singur modul de test Kafka și infrastructura configurată prin Setup PowerShell în Podman. Framework-ul și clientul/observerul Java sunt WIP, iar un rezultat MOCK nu certifică brokerul.

Registrul urmărește numai deployment-ul Eventuate, Setup și platforma efectiv
consumată. Reporting se întreține în J0; Shop/AutoEx, UI și Debug UI au owneri
în J1. Sincronizarea surselor nu dublează backlog-ul lor în J2.

| Ordine | Owner J2 / impact local | Prioritate | Dovada care lipsește |
|---:|---|---:|---|
| 1 | [Eventuate](ATF_Eventuate_Feedback.md) | 95 | aplicație/saga reală; apoi identitate, convergență și dovezi terminale complete |
| 2 | [Adoptare Reporting](ATF_Reporting_Consumer_Feedback.md) | 89 | profil artifact și timing execuție/publicare clarificate la owner, apoi revalidate pe binarul livrat |
| 3 | [Setup](ATF_Setup_Feedback.md) | 80 | Kafka și Java VM validate; rămân ownership-ul fizic, transportul durabil și reluarea după restart |

Scorurile sunt priorități de intervenție, nu note ale modulelor. Ordinea
executării ține cont de dependențe: un engine și un witness funcționale
preced certificarea saga sau idempotency business.

## Ce este livrat și unde se citește

- Cele trei componente MOCK și matricea Extent/fallback au contract în [ghidul Eventuate](../ATF_Eventuate_Testing.md).
- Hostul și provisioning-ul silent au contract în [Setup](../../atf-setup/README.md) și [atlasul său](../ATF_Setup_Atlas.md).
- Catalogul explicit cu șapte module și retragerea browserului sunt în [scopul J2](../ATF_J2_Scope.md); criteriul închis nu rămâne ToDo.
- [Verificarea executată](../ATF_J2_Verification.md) păstrează limitele dovezilor; MOCK nu certifică broker sau efecte persistente.

## Owneri externi, fără copii locale de backlog

| Domeniu | Registru canonic |
|---|---|
| Reporting și mesagerie în renderer | [J0 Reporting](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md) |
| regresii generice Reporting | [J0 Reporting Tests](../../../atf_j0/docs/feedback/ATF_Reporting_Tests_Feedback.md) |
| Shop/AutoEx | [J1 business](../../../atf_j1/docs/ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită) |
| Debug UI | [J1 Debug](../../../atf_j1/atf-debug-ui/README.md#identitatea-path-ului-uri) |

Două fișiere scurte de compatibilitate, Reporting și AI, rămân ținte ale
guvernanței existente. Ele indică ownerul extern și nu conțin ToDo-uri J2.
La reconcilierea țintelor se pot retrage explicit, fără skip după absență.

## Curățare și închidere

Contractul stabil merge în documentația ownerului; feedback-ul păstrează
numai gap-ul, prioritatea și proba lipsă. Copia de deployment nu închide un
criteriu comun numai pentru că a fost reparat într-un alt proiect.

[Baseline-ul testChanges](../ATF_Test_Changes_Baseline.md) păstrează controalele
eligibile ale diferenței Git. Un control VERIFIED nu se șterge automat odată
cu ToDo-ul și nu se rebazează pentru obținerea unui gate verde.
