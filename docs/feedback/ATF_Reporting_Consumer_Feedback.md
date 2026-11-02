# Reporting J2 — adopție și criterii locale

## Pe scurt

J2 consumă Core C2 și Reporting J12 prin perechi JAR/POM plate. Implementarea, nativele și regresiile generice rămân în J0;
[acceptanța J12](../ATF_Reporting_J12_Verification.md) separă probele consumerului de cele ale producătorului.

<a id="report-j2-adoption-62--prioritate-89"></a>

## REPORT-J2-ADOPTION-62 — folosirea profilului explicit

J12 livrează [profilul explicit](../../../atf_j0/atf-reporting/README.md#inspecția-artifactului-cu-intenție-explicită).
Acceptanțele `EventuateReportAcceptance`, `MockFixtureReportAcceptance` și `SqlReportAcceptance` păstrează încă apelul legacy Extent/HTTP.
Refuzul acelui apel pentru no-HTTP ori fallback este compatibilitatea declarată, fără HTTP fictiv.

- [ ] Declarați inventarul așteptat la consumer și apelați overload-ul cu profil în cele trei acceptoare; nu derivați intenția din HTML-ul inspectat.
- [ ] Păstrați separat rezultatul legacy în receipt și oracolele business independente, cu mutanții existenți.
- [ ] Verificați individual/suită, nominal și FAIL controlat, Extent/fallback; închiderea cere identitatea JAR/POM și profil conform pentru artifactul legitim.

Biblioteca și regresia neutră nu mai sunt restanțe ale criteriului 62. Adoptarea explicită a acceptoarelor J2 rămâne un task local.

<a id="report-j2-timing-83--prioritate-88"></a>

## Contracte consolidate

| Contract | Owner și limită |
|---|---|
| disclosure API și stare vecini | probele J8/J10 sunt istorice; J12 păstrează aceleași tabele și controale |
| execuție, transport și publicare | [contractul temporal J0](../../../atf_j0/atf-reporting/README.md#originea-duratelor-sub-o-fază-capturată); fără clamp, însumare peste faza capturată ori zero inventat |
| indicator assertions | disclosure comun cu mouse/Enter/Space, colapsat implicit și preview 240 |
| catalog nativ | 16 fișiere numite explicit, incluzând cele trei clase native J12; fără wildcard sau skip |

<a id="sql--handoff-către-j0-și-adoptare-ulterioară"></a>

## SQL — proiecție semantică și bugete

Captura typed JDBC și oracolele aplicației există în J2. [REPORT-SQL-STEPS-116](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile)
și [probele neutre SQL](../../../atf_j0/docs/feedback/ATF_Reporting_Tests_Feedback.md#report-test-sql-steps-116--probe-dedicate-sql) rămân la J0.

După livrarea lor, adaptorul proiectează aceeași captură JDBC, fără replay SELECT. Bugetele propuse query 4.096, binds 2.048,
rezultate 16.384 și payload 1.048.576 nu sunt încă proprietăți HTML disponibile; assertions 240 rămâne separat.

## Geometria RESULT

[REPORT-FALLBACK-RESULT-WIDTH-101](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-fallback-result-width-101--badge-ul-statusului-depășește-coloana)
rămâne distinct: badge-ul Warning trebuie măsurat în celulă și wrapper, la viewport normal și panou îngust, cu mutant geometric refuzat.
