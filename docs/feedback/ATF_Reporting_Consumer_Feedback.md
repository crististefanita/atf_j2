# Reporting J2 — adopție și criterii locale

## Pe scurt

J2 verifică adaptoarele și oracolele Eventuate, Mock și SQL. J0 deține biblioteca și regresiile rendererului. [Contractul Reporting](../../../atf_j0/atf-reporting/README.md) · [Proveniența binarelor](../../library-provenance.json)

<a id="report-j2-adoption-62--prioritate-89"></a>

## Profil explicit — contract consolidat

[Contractul profilurilor explicite](../ATF_Eventuate_Testing.md#acceptanța-raportului) păstrează oracolele de business, diagnosticul legacy și referința la matricea verificată.

<a id="report-j2-timing-83--prioritate-88"></a>

## Contracte consolidate

[Contractul temporal](../../../atf_j0/atf-reporting/README.md#originea-duratelor-sub-o-fază-capturată) și [contractul de afișare](../../../atf_j0/docs/ATF_Reporting_Display_Contract.md) rămân la J0.

<a id="sql--handoff-către-j0-și-adoptare-ulterioară"></a>

## SQL — proiecție semantică și bugete

Captura typed JDBC și oracolele aplicației există în J2. [REPORT-SQL-STEPS-116](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile)
și [probele neutre SQL](../../../atf_j0/docs/feedback/ATF_Reporting_Tests_Feedback.md#report-test-sql-steps-116--probe-dedicate-sql) rămân la J0.

După livrarea lor, adaptorul proiectează aceeași captură JDBC, fără replay SELECT. Bugetele propuse query 4.096, binds 2.048,
rezultate 16.384 și payload 1.048.576 nu sunt încă proprietăți HTML disponibile; assertions 240 rămâne separat.

## Geometria RESULT

[REPORT-FALLBACK-RESULT-WIDTH-101](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-fallback-result-width-101--regresie-geometrică-pentru-badge)
rămâne distinct: badge-ul Warning trebuie măsurat în celulă și wrapper, la viewport normal și panou îngust, cu mutant geometric refuzat.
