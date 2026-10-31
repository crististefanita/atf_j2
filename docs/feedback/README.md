# ATF Java — registrul cerințelor rămase

Pentru acest deployment, [feedback-ul J1](ATF_J1_Consumer_Feedback.md)
deține criteriile distribuției fără harness local; guvernanța source/binary este contract livrat la ATF Tests.
Reporting și testele sale generice au owner în `atf_j0`; topul lor se consultă separat de lucrările comune pornite în J1.

Registrul indexează criteriile neîndeplinite. Contractele stabile aparțin
README-ului ownerului; controalele verificate aparțin baseline-ului testelor.

## Pe scurt

- UI livrează politica BE read-only, continuitatea selectului, observerii și geometria iframe; sesiunea browser păstrează cauza primară, rollback-ul și erorile secundare la release.
- Reporting verifică sursa izolată, JAR-ul fizic și refuzul constructorului; [pregătirea pentru release](ATF_Reporting_Feedback.md) păstrează API-ul, distribuția și portabilitatea. [Reporting Tests](ATF_Reporting_Tests_Feedback.md) deține oracolele per scenariu și degradările focalizate.
- Consumerii păstrează verdictul, cauza și captura per invocare; ramurile Cart și eroarea după PASS au probe distincte. Shop rezolvă output-ul prin POM-ul propriu, identic cu Reporting sursă sau JAR.
- Core prevalidează sursele și părinții destinației fără scriere; [atomicitatea per fișier](../../atf-core/README.md#manifest-și-promovare-guarded) rămâne explicită.
- Documentația verifică linkurile și fragmentele în README-uri și docs; [afirmațiile factuale critice](../../atf-tests/README.md#documente-și-responsabilități) compară modulele, scope-ul Kit și geometria selectată cu sursa executabilă; restul sensului păstrează review-ul manual.
- Validation, Console și Kit păstrează contractele verificate; criteriile noi au owner și probă distinctă, fără refactorizări fabricate pentru punctaj.

În J1 există 10 module sursă și Reporting ca bibliotecă externă.
[Ownership-ul și direcțiile sync](../ATF_J1_Consumer.md) sunt explicite;
îmbunătățim codul comun din J1:
responsabilități, stare, erori, duplicări și documentație. Flow-urile E2E Shop și
AutoEx pot fi completate în modulele lor până la rezultatul business declarat.

Propunerile de funcționalități noi și workflow-ul AI rămân în backlog;
scorul lor nu extinde scope-ul incrementului. Politica BE read-only livrată
explicit are contract la ownerii UI, Kit și consumeri.

## Top feedback-uri active

Scorul este prioritate de intervenție, nu notă a codului sau procent de acoperire.
Formula comună este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10.
Un document cu mai multe gap-uri primește scorul celui mai urgent, nu suma lor.

| Ordine | Feedback / owner | Scor /100 | Criteriul care decide ordinea |
|---:|---|---:|---|
| 1 | [Core — diagnostice și HTTP](ATF_Core_Boundaries_Feedback.md) | **74** = 31 + 22 + 15 + 6 | Authorization quoted în text/fallback (74), media type (68), header explicit (65) |
| 2 | [Guvernanță — inventar surse](ATF_Governance_Feedback.md) | **70** = 27 + 20 + 17 + 6 | surse compilabile omise prin numele package-ului; traversare înaintea filtrării |
| 3 | [Consumer — locale Shop](ATF_Consumer_Business_Test_Focus_Feedback.md) | **59** = 22 + 15 + 17 + 5 | încărcarea Shop în tr-TR |
| 4 | [Debug UI — path URI](ATF_Debug_UI_Feedback.md) | **42** = 14 + 9 + 14 + 5 | plusul literal selectează fișierul cu spațiu |

J1 nu deține un backlog activ de implementare Reporting; criteriile producătorului sunt separate mai jos.

- Reporting are backlog separat, întreținut în `atf_j0`: [API/release](ATF_Reporting_Feedback.md)
  cu prioritate 81 și [oracolele testelor](ATF_Reporting_Tests_Feedback.md) cu
  prioritate 80. J1 trimite reproducerea și verifică livrarea, fără owner concurent.
- Reporting Tests este retras din J1 și rămâne [gate extern în TMP](../ATF_J1_Consumer.md#gate-ul-extern-reporting), fără copie locală a harness-ului.

O componentă verde confirmă criteriul ei; închiderea unui gap cere întreaga
probă declarată la owner. Evaluarea calității folosește separat
[rubrica documentației](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale).

În perimetrul J1 rămân criteriile reproduse pentru Authorization quoted,
MIME/headere, locale și identitatea path-ului Debug. Inventarul
guvernanței cere separarea output-ului de sursa compilabilă; probele nominale
existente nu certifică aceste negative noi.

- Proiecția typed `UI-RECOVERY-AUDIT-01` are owner Reporting.
- Prevenirea factuală selectivă are ownerul neutru ATF Tests și declarații la ownerul documentului.

## Traiectoria modulelor peste 91

Ținta privește codul livrat, conform [rubricii de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor).
O notă editorială mare nu compensează un false PASS, o cauză pierdută sau un
scope contaminat; reevaluarea urmează probei și sincronizării live.

| Modul | Contract demonstrat sau criteriu rămas | Owner |
|---|---|---|
| Core | promovarea guarded și masca literală rămân verificate; Authorization quoted, media type și precedența headerelor au criterii distincte | [boundary-uri Core](ATF_Core_Boundaries_Feedback.md) |
| Mock | identitatea rutelor și query-ul rămân distincte; MIME-ul fals JSON cere regresie la matching | [clasificarea Core și efectul Mock](ATF_Core_Boundaries_Feedback.md) |
| Validation | cauza inițială supraviețuiește close-ului; refuzul one-shot precede callback-ul | [cauza și protocolul](../../atf-validation/README.md#cauza-primară-și-refuzul-înaintea-evaluării) |
| Console | copilul restaurează contextul/sink-ul părintelui la succes, FAIL și SKIP | [scope imbricat](../../atf-console/README.md#restaurarea-contextelor-imbricate) |
| Reporting | source/JAR gate, constructor cu rollback, lifecycle declarat, query privat și ordinalul dovezii originale; API-ul susținut și release păstrează criterii distincte | [criteriile Reporting](ATF_Reporting_Feedback.md) |
| UI | pointer nativ, rollback și close-once cu păstrarea cauzei; observerii au ownership separat | [lifecycle](../../atf-ui/README.md#lifecycle-ul-sesiunii-și-limita-probei) și [geometrie](../../atf-ui/README.md#geometria-nativă-a-contextului) |
| Debug UI | close bounded rămâne verificat; plusul literal trebuie să păstreze identitatea fișierului | [path URI](ATF_Debug_UI_Feedback.md) |
| Shop | păstrează corelarea, output-ul și cleanup-ul; încărcarea profilului depinde încă de locale-ul JVM | [configurarea consumerului](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| AutoEx | identitate și cleanup owned; suma completă și ambii adaptori păstrează FAIL-ul, cu nominal separat | [contractul facturii](../../atf-test-auto-ex/README.md#validarea-completă-a-sumei-facturii) |
| Reporting Tests | oracole independente pentru query, randare, backlink, achiziție parțială și cleanup direct/delegat | [contractele și probele](../../../atf_j0/atf-reporting-tests/README.md#erori-simultane-la-publicare-și-close) |
| Test Kit | revalidarea protocolului JUnit și a binding-ului; reutilizare neutră numai după două utilizări reale | [limita reutilizării](../../atf-test-kit/README.md) |
| ATF Tests | gate-urile existente rămân; sursele compilabile cu nume de package rezervate excluderii cer reconciliere | [inventar și oracol](ATF_Governance_Feedback.md) |

Kit și guvernanța nu primesc defecte inventate pentru a completa tabelul.
Fiecare modul se revede pe propriul boundary; fără module noi, clase auxiliare
fără motiv semantic sau funcționalități suplimentare pentru punctaj.

## Contracte și controale verificate

Regulile demonstrate sunt la README-ul ownerului și în
[ghidul de testare](../ATF_Testing_Guide.md); registrul de feedback nu dublează
inventarul lucrărilor închise. Controalele Git verificate sunt în
[baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate),
iar cele `ACTIVE` rămân lângă owner până la proba lor completă.

## Design AI — evaluare separată

[Feedback-ul AI](README_AI_Evaluation.md) separă comparatorul implementat de
publication path, run ledger și workflow-ul propus. Readiness-ul său nu este
prioritatea operațională de mai sus și nu se recalibrează editorial.

## Păstrare, promovare și verificare

| Moment | Actualizarea cerută |
|---|---|
| înaintea refactorizării | confruntați feedback-ul cu sursa live; fixați triggerul, ownerul, observația și proba de închidere |
| în timpul incrementului | reconciliați imediat constatarea nouă, limita probei ori schimbarea de scope; separați analiza statică de reproducere |
| înaintea promovării | revalidați sursele live, probele și linkurile; un criteriu parțial rămâne deschis, cu restanța exactă |
| după acceptare | promovați contractul stabil la owner și eliminați numai criteriul demonstrat, cu controalele și ancorele reconciliate |

- Regula stabilă se promovează la owner înainte de eliminarea criteriului închis.
- Fișierul fără gap-uri se elimină după reconcilierea controalelor și linkurilor;
  țintele `@DocumentationTarget` rămân protejate.
- README-urile de modul rămân documentație de utilizare chiar fără ToDo-uri.

Protocolul este în [bunele practici documentare](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului).
Verificările respectă [staging-ul și sync-ul sigur](../ATF_Build_And_Execution_Safety.md).

## Cum verifici un criteriu

| Pas | Sursă | Rezultat cerut |
|---|---|---|
| Delimitează problema | secțiunea feedback a ownerului | trigger, observație, expected independent și boundary |
| Verifică implementarea | README owner → cod/POM → test nominal și negativ | criteriul este demonstrat, nu doar descris |
| Reconciliază controlul tehnic | [baseline testChanges](../ATF_Test_Changes_Baseline.md) și [gate-ul Git](../../atf-tests/src/test/java/atf/api/tests/governance/changes/TestChangeGovernanceTest.java) | control eligibil pentru diferența și baza de review curente |
| Închide | [protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) | regula stabilă este la owner, iar gap-ul închis nu rămâne backlog |

Scorul de prioritate nu este nota README-ului și nu certifică implementarea.
Designurile AI păstrează [evaluarea lor separată](README_AI_Evaluation.md),
fără a extinde incrementul de mentenanță al codului existent.
