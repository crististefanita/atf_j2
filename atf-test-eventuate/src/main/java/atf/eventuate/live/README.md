# Eventuate LIVE — atlas de compoziție

## Pe scurt

- Acest pachet compune HTTP LIVE, observerul Kafka și witness-ul SQL independent. Nu pornește infrastructura.
- Binding-ul existent MOCK rămâne implicit; LIVE cere `atf.eventuate.live.enabled=true`. Proba infrastructurii a trecut pe Kafka/MySQL, dar aplicația Eventuate cu CDC/saga este încă **NOT RUN**.

| Intrare | Responsabilitate și limită |
|---|---|
| [LiveSettings](LiveSettings.java) | endpoint-uri interne ale rețelei Podman, credențiale read-only, buget total 15 s și citire 2 s implicit |
| [LiveDeadline](LiveDeadline.java) | timp monoton rămas; fiecare suboperație consumă bugetul existent |
| [LiveHttpClient](LiveHttpClient.java) | Core/JDK LIVE, o tentativă HTTP; un singur POST client și comandă per instanță, inclusiv după eșec |
| [KafkaObserver](kafka/KafkaObserver.java) | partiții și end offsets înaintea mutației; poll bounded, fără commit sau grup business |
| [KafkaClientSettings](kafka/KafkaClientSettings.java) | profiluri efective admin/consumer/producător sintetic, timeout-uri coerente |
| [EventuateKafkaDecoder](kafka/EventuateKafkaDecoder.java) | envelope strict cu ID, aggregate ID, tip eveniment și payload JSON textual |
| [LiveSqlWitness](sql/LiveSqlWitness.java) | mapper al schemei aplicației; execuția bounded generică aparține `atf-sql` |
| [LiveOrderScenario](LiveOrderScenario.java) | observer → POST → convergență GET → eveniment terminal → SELECT independent |

## Invariante și dovadă

- Observerul este thread-confined, folosește assignment explicit și un grup UUID, nu comite offsets și nu creează automat topics. Limitele sunt 1.000 evenimente, 64 KiB per payload și poll de maximum 100 ms.
- Offset-ul Kafka real rămâne distinct de ordinalul unui fixture MOCK. Snapshot-ul include coordonatele și corelarea.

- Citirea HTTP primește timpul rămas, fără retry Core automat; convergența poate repeta numai GET în același deadline. Portul injectabil permite probe controlate fără rețea.
- Testele verifică și buget expirat, timeout și absența replay-ului POST. Aceste probe nu certifică singure anularea socket-ului pe serviciul real.

- Witness-ul deschide conexiuni noi cu cont SELECT-only. Cele două SELECT-uri consumă același buget; rezultatele și identitățile sunt citite independent de HTTP.
- Schema și mapping-ul sunt candidate până la validarea aplicației fixate. Nu deduceți corectitudinea schemei dintr-un SELECT sintetic MySQL.

## Execuție și criteriu rămas

Cele trei clase din `tests/e2e` sunt opt-in și selectate finit de
`EventuateLiveE2eSuite`. Profilul și tag-urile raportului disting LIVE E2E de MOCK
și SQL embedded; compilarea sau skip-ul lor nu reprezintă acceptanță business.

[Setup Eventuate](../../../../../../../atf-setup/README_Eventuate.md) deține
rețeta și limita de memorie. [Feedback-ul Eventuate](../../../../../../../docs/feedback/ATF_Eventuate_Feedback.md)
cere readiness HTTP/CDC, validarea schemei și cele trei cazuri individual/prin suită.
Nu se mărește timeout-ul și nu se reexecută mutațiile pentru a obține raport verde.
