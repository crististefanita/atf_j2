# Reporting J2 — adopție și criterii locale

## Pe scurt

J2 consumă Core C3 și Reporting J12 prin perechi JAR/POM plate. Implementarea, nativele și regresiile generice rămân în J0;
[acceptanța J12](../ATF_Reporting_J12_Verification.md) separă probele consumerului de cele ale producătorului.

<a id="report-j2-adoption-62--prioritate-89"></a>

## Profil explicit — contract consolidat

Cele trei acceptoare Eventuate, Mock și SQL declară scenarii, faze, evidence și verdicturi înaintea inspecției.
[Contractul consumerului](../ATF_Eventuate_Testing.md) păstrează oracolele business și mutanții lor separat de profil.

- Rezultatul legacy Extent/HTTP rămâne în receipt, ca diagnostic distinct.
- Matricea de 40 rapoarte verifică individual/agregat, Extent/fallback, nominal și FAIL controlat.
- Mutația coloanei ACTUAL din tabelul original este refuzată pentru fiecare raport; nu este satisfăcută de template sau sibling.

Criteriul REPORT-J2-ADOPTION-62 este retras din backlog după proba pe J12.
SQL-STEPS-116 și geometria RESULT rămân criterii de bibliotecă distincte.

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


