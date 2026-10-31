# Adnotări Kit — aplicarea în J2

## Pe scurt

Kit furnizează declarații și rezolvare neutră. J2 folosește meta-adnotarea
EventuateComponentTest și extensia sa; consumerul deține resursele și publicarea.

| Axă folosită în J2 | API / owner | Contract |
|---|---|---|
| nivel | AtfComponentTest, AtfIntegrationTest | un nivel executabil per clasă |
| report path | AtfReportPath / resolver | opt-in; cale absolută numai după publicare reușită |
| audit | ComponentAuditAdapter / ComponentAuditExtension | colectare per invocare și transfer fără reevaluare |
| assertions | RecordedAssertions | comparație integrală, redacție și preview ulterior |
| scope | TestRunScope / ScopedTestResource | class implicit, suite explicit; ownerul decide close |
| documentație | DocumentationTarget | țintă obligatorie a validatorilor, fără skip dacă lipsește |

## Opt-in pentru calea raportului

Eventuate folosește AtfReportPath pentru notificarea căii absolute după close
reușit. Clasa Jupiter deține declarația; suita nu o transmite implicit prin
moștenire. Rezolvarea comună și precedența sunt în [Kit](../atf-test-kit/README.md#politica-pentru-calea-raportului-publicat).

## Scope Maven

| Consumator J2 | Scope Kit | Motiv |
|---|---|---|
| Core, Mock, Validation | test | vocabular și probe fără dependență Kit în runtime |
| Eventuate, ATF Tests | test | extensie, nivel și fixture-uri; suportul main rămâne separat |
| Console | optional compile | adaptorul generic JUnit din main citește policy resolvers |

Nu deduce scope-ul din numele adnotării. POM-ul și importurile fixează muchia;
o excepție legitimă nu se elimină pentru uniformitate vizuală.

## API comun în afara selecției J2

Kit include și vocabular UI/API, E2E/evidence și directoare de output.
Prezența sa nu activează un browser și nu introduce un canal Kafka nativ;
componenta de mesagerie curentă folosește nivelul component și assertions neutre.

Aplicarea web completă și exemplele sunt în [documentul J1](../../atf_j1/docs/ATF_Test_Infrastructure_Annotations.md).
Catalogul API rămâne la [Kit](../atf-test-kit/README.md); [atlasul J2](ATF_Context_Atlas.md)
descrie numai callerii activi ai acestui deployment.
