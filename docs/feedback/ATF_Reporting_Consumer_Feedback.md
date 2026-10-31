# Reporting J2 — adoptarea binarului

## Pe scurt

J2 deține adaptorul și acceptanța sa, nu implementarea bibliotecii.
J6 are proveniență istorică demo2 și owner curent J0; contractul local este
în [atlasul JAR](../ATF_Reporting_Library_Atlas.md).

## REPORT-J2-ADOPTION-62 — prioritate 89

Pe J6, inspectorul generic refuză scenariul fără HTTP și aplică cerințe
Extent pe fallback. Raportul este inspectabil, iar receipt-ul J2 păstrează
rezultatul original și verdictul oracolului pe tabele, fără fals verde universal.

- Implementarea și regresia neutră sunt la [REPORT-ARTIFACT-PROFILE-62 în J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-artifact-profile-62--aplicabilitate-explicită-a-inspectorului).
- Nu adăuga request-uri fictive și nu injecta CSS/JavaScript în consumer pentru a satisface inspectorul.
- După livrarea unui candidat nou, repetă identitatea JAR/POM, CodeSource și matricea J2 individual/suită, inclusiv negativele și randarea.

Închidere locală: noul binar este adoptat cu proveniență și oracol explicit
assertion-only/HTTP și renderer. Green-ul J0 sau J1 nu certifică J2.
Nu este declarată remedierea J6 în acest increment documentar.

## REPORT-J2-TIMING-83 — prioritate 88

Acceptanța paralelă fără UI a consemnat SETUP 21 ms cu assertions 94 ms și
TEST BODY 68 ms cu request 514 ms. Verdicturile și expandările verificate nu
închid coerența temporală; publicarea dovezii și execuția capturată cer delimitare.

- [Ownerul J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-recorded-timing-83--execuție-capturată-și-cost-de-publicare) păstrează artifactul, analiza și regresia neutră necesară.
- J2 verifică adaptorul ComponentReportRun și adoptă contractul demonstrat; nu atribuie automat întreaga diferență rendererului.
- Închiderea locală cere latență controlată la publicare, individual/suită și Extent/fallback, fără clamp sau mutarea assertions pentru a ascunde diferența.
