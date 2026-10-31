# Reporting J7 — dovada istorică J2

## Pe scurt

Acest document păstrează acceptanța J7; adopția curentă și probele sale sunt
în [livrarea J8](ATF_Reporting_J8_Verification.md). Hash-urile istorice rămân
în proveniență, fără atribuirea unei execuții noi versiunii vechi.

| Boundary J7 | Dovadă păstrată |
|---|---|
| JAR | 569D2BABE6B9EC091CB1DF6E1207110C611327EA15F1521955E847C837D9EB2A |
| consumer | șapte module, Core C1 și istoric J6 păstrat |
| acceptanță | trei componente Eventuate MOCK, guvernanța binară și controlul schimbării POM; această probă nu certifică brokerul, Podman sau un client Kafka real, iar lucrul Setup concurent este păstrat |
| producător J0 | 52 native, regresii Extent/fallback, JAR fizic și refuz cu asset lipsă, cu artifactul anterior păstrat |
| reproductibilitate | două build-uri curate, aceiași bytes |
| API | zero eliminări public/protected; variantele sealed noi cer revizuirea switch-urilor exhaustive |
| ownership | sursele și harness-ul Reporting rămân în J0 |

[Contractul observațiilor](../../atf_j0/atf-reporting/README.md#observații-publice-fără-transport)
separă INFO de assertions și verdict; simbolurile adăugate sunt opționale
pentru callerii existenți.
