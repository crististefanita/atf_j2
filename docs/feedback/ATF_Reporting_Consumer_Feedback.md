# Reporting J2 — adoptarea binarului

## Pe scurt

J2 deține adaptorul și acceptanța sa, nu implementarea bibliotecii.
J10 este binarul consumat; J6/J7/J8/J9 păstrează proveniența istorică, cu owner curent J0. Contractul local este
în [atlasul JAR](../ATF_Reporting_Library_Atlas.md).

[Livrarea J8](../ATF_Reporting_J8_Verification.md) închide lipsa disclosure-ului API fallback. Regresia neutră J0 și matricea Validation păstrează valorile, verdicturile per rând, preview-ul 240 și tastatura. Inspectorul, timing-ul și geometria badge-ului rămân criterii distincte.

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

Catalogul nativ finit J2 este reconciliat explicit cu J8, fără wildcard sau skip. Această acceptanță nu închide automat guvernanța ori adopția din J0/J1.

- Matricea Mock JSON repetată pe J8 păstrează 14 artifacte assertion-only
individuale/combinate, toate cu oracol per rând și mutanți refuzați.
Inspectorul generic le refuză pe toate: pe Extent, check-ul request response
evidence split este fals pentru cazul legitim fără HTTP. Rezultatele originale
sunt păstrate; închiderea disclosure-ului API nu închide profilul inspectorului.

## REPORT-J2-TIMING-83 — prioritate 88

Intervalul publicat al copilului și execuția capturată a fazei pot descrie
momente diferite și pot apărea incompatibile. [Dovada consemnată](../ATF_J2_Verification.md#revizie-semantică-a-codului-și-documentației-j2)
nu este înlocuită de nominale verzi; închiderea cere latență controlată și
delimitarea execuției de publicarea dovezii.

- [Ownerul J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-recorded-timing-83--execuție-capturată-și-cost-de-publicare) păstrează artifactul, analiza și regresia neutră necesară.
- J2 verifică adaptorul ComponentReportRun și adoptă contractul demonstrat; nu atribuie automat întreaga diferență rendererului.
- Suita Eventuate pe J8 reconfirmă vizual TEST BODY 141 ms și copilul GET 215 ms în componenta API. Este o observație nouă, fără latență controlată; închiderea cere corelarea modelului Core, timpului capturat al fazei și publicării, fără a atribui automat diferența unui singur owner.
- Închiderea locală cere latență controlată la publicare, individual/suită și Extent/fallback, fără clamp sau mutarea assertions pentru a ascunde diferența.


[Contractul J10](../ATF_Reporting_J10_Verification.md) închide resetarea vecinilor,
cu regresie permanentă neutră în J0 și acceptanță separată pe JAR. Handoff-ul
rezolvat nu rămâne criteriu activ; inspectorul și timing-ul sunt distincte.

## SQL — handoff către J0 și adoptare ulterioară

J2 are trei componente SQL embedded și matricea de 18 HTML-uri, cu rânduri,
erori și assertions auditabile prin API-ul generic observations. Noul atf-sql capturează query/bind-uri/schema/rânduri typed; nu există încă tabel SQL semantic în J10.

- [REPORT-SQL-STEPS-116 în J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile) deține implementarea cerută: query și rezultate configurabile separat, defaults propuse 4.096 și 16.384, bind-uri 2.048 și payload SQL cumulat 1.048.576; preview-ul assertions 240 rămâne separat.
- [Probele componente dedicate](../../../atf_j0/docs/feedback/ATF_Reporting_Tests_Feedback.md#report-test-sql-steps-116--probe-dedicate-sql) rămân în Reporting Tests, cu fixture-uri neutre; nu se copiază harness-ul în J2.
- După release, J2 va proiecta snapshot-ul atf-sql deja capturat din aceeași execuție JDBC și va verifica sursa faptelor față de HTML, fără replay SELECT sau interpolare executabilă.
- Adoptarea cere JAR/POM și CodeSource exacte, individual/suită/combinate în Extent/fallback, limite efective și lipsa payload-ului integral ascuns. Nu injectați CSS/JS și nu inventați HTTP pentru inspector.

Acesta este un handoff și un criteriu viitor de adoptare; upgrade-ul J10 nu
implementează contractul SQL. Bugetele propuse nu sunt proprietăți disponibile
acum și nu închid profilul inspectorului ori timing-ul.

### Reconfirmare pe J10 — SQL generic

- Matricea nouă are 12 HTML-uri și 36 invocări, inclusiv patru FAIL-uri controlate. Oracolul SQL independent și mutanții trec.
- Inspectorul generic refuză toate cele 12: șase Extent fără HTTP și șase fallback evaluate după contracte Extent. Rezultatele originale sunt în [dovada incrementului](../ATF_SQL_Live_Verification.md); criteriul 62 rămâne deschis.
- Captura SQL este acum implementată în J2; API-ul SQL semantic și bugetele HTML rămân cerute în J0.
