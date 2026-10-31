# Reporting J2 — adoptarea binarului

## Pe scurt

J2 deține adaptorul și acceptanța sa, nu implementarea bibliotecii.
J7 este binarul consumat; J6 păstrează proveniența istorică demo2, cu owner curent J0. Contractul local este
în [atlasul JAR](../ATF_Reporting_Library_Atlas.md).

Auditul scalar J2 confirmă valorile și verdicturile în ambele renderere, dar
validarea API fallback nu are disclosure propriu. [Criteriul și regresia J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-fallback-validation-disclosure-99--validarea-api-colapsabilă)
păstrează acest gap; acceptanța structurală locală nu îl declară rezolvat.

- Matricea media type confirmă valori și verdicturi, dar badge-ul Warning depășește RESULT în fallback. Criteriul REPORT-FALLBACK-RESULT-WIDTH-101 și componentele neutre necesare sunt în [feedback-ul J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md).

## REPORT-J2-ADOPTION-62 — prioritate 89

Pe J7, matricea J2 nominală repetată păstrează nouă rezultate neconforme ale inspectorului implicit: șapte fallback și două assertion-only. Inspectorul generic refuză scenariul fără HTTP și aplică cerințe
Extent pe fallback. Raportul este inspectabil, iar receipt-ul J2 păstrează
rezultatul original și verdictul oracolului pe tabele, fără fals verde universal.

- Implementarea și regresia neutră sunt la [REPORT-ARTIFACT-PROFILE-62 în J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-artifact-profile-62--aplicabilitate-explicită-a-inspectorului).
- Nu adăuga request-uri fictive și nu injecta CSS/JavaScript în consumer pentru a satisface inspectorul.
- După livrarea unui candidat nou, repetă identitatea JAR/POM, CodeSource și matricea J2 individual/suită, inclusiv negativele și randarea.

Închidere locală: noul binar este adoptat cu proveniență și oracol explicit
assertion-only/HTTP și renderer. Green-ul J0 sau J1 nu certifică J2.

Catalogul nativ finit J2 este reconciliat explicit cu J7, fără wildcard sau skip. Această acceptanță nu închide automat guvernanța ori adopția din J0/J1.

## REPORT-J2-TIMING-83 — prioritate 88

Intervalul publicat al copilului și execuția capturată a fazei pot descrie
momente diferite și pot apărea incompatibile. [Dovada consemnată](../ATF_J2_Verification.md#revizie-semantică-a-codului-și-documentației-j2)
nu este înlocuită de nominale verzi; închiderea cere latență controlată și
delimitarea execuției de publicarea dovezii.

- [Ownerul J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-recorded-timing-83--execuție-capturată-și-cost-de-publicare) păstrează artifactul, analiza și regresia neutră necesară.
- J2 verifică adaptorul ComponentReportRun și adoptă contractul demonstrat; nu atribuie automat întreaga diferență rendererului.
- Închiderea locală cere latență controlată la publicare, individual/suită și Extent/fallback, fără clamp sau mutarea assertions pentru a ascunde diferența.
