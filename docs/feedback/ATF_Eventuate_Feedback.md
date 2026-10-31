# Eventuate — feedback deschis

## Pe scurt

Cele trei componente verifică oracolele și integrarea ATF pe fixture-uri.
Acceptanța aplicației reale rămâne deschisă; raportul MOCK nu este dovadă
pentru procesarea brokerului sau pentru tranzacțiile bazei de date.

## EVT-LOCAL-01 — prioritate 90

Kafka și clientul Java de infrastructură sunt demonstrate în Setup; nu recreați
această restanță. Lipsește laboratorul aplicației cu servicii, CDC și baze,
împreună cu clientul/observerul framework-ului și efectele independente.

- Compuneți executori LIVE din configurația laboratorului; seam-ul de injectare există, iar binding-ul implicit rămâne MOCK. Adaptați profilul și tag-urile run-ului, nu numai transportul.
- Păstrați implicit lipsa traficului LIVE și activarea explicită a laboratorului. Provenance-ul schimburilor este capturat, însă lipsesc observerul Kafka, corelarea și politica de ownership pentru callbacks asincrone.
- Verificați compatibilitatea versiunilor aplicației și readiness-ul observerului înaintea primei mutații; brokerul standalone nu certifică outbox/CDC/saga.
- Implementați aprobare, respingere și client necunoscut cu date și cleanup proprii; nu adăugați module ori un simulator de business în Kit.

Închidere: aceleași cazuri trec individual și în suită, cu provenance live,
identități corelate, citiri independente și randări Extent/fallback inspectate.
Nu mări timeout-ul pentru a ascunde o precondiție neconfirmată.

## EVT-IDEMPOTENCY-02 — prioritate 88

Separă duplicatul de transport cu același message ID de repetarea operației
business cu ID nou și de repetarea POST-ului. Proiecția Mongo folosește un
detector fără deduplicare; replay-ul vechi este un risc static de reprodus.

Închidere: demonstrează procesarea duplicatului, efectul SQL unic și starea
CQRS coerentă; verifică și creditul concurent și recuperarea CDC. Un defect
upstream se raportează cu dovadă, fără relaxarea oraclelor ATF.

## EVT-CONVERGENCE-CONTRACT-86 — identitate și deadline real

**Prioritate: 84/100.** Criteriul rămas este citirea bounded în integrarea LIVE.
Identitatea integrală și overflow sunt protejate de [contractul livrat](../../atf-test-eventuate/README.md#limite-curente-ale-scaffold-ului),
regresii negative și probe consumer individual/suită; nu sunt ToDo-uri deschise.

| Probă de buget | Rezultat | Limită |
|---|---|---|
| 25 ms disponibili, citire de 40 ms în clock virtual | PASS refuzat, observația păstrată și elapsed 40 ms | nu demonstrează anularea unei citiri HTTP reale |

- Propagați timpul rămas către portul de citire LIVE și timeout-ul transportului, fără reînnoirea bugetului la retry.
- Păstrați cauza originală la expirare; fără timeout mărit sau replay de mutații.
- Verificați răspuns lent, cancel și deadline expirat înaintea citirii; fiecare buget și observație trebuie să fie auditabile.

Închidere: timpul real este bounded prin port/transport și probat individual
și prin suită. Clock-ul virtual dovedește politica de verdict, nu SLA-ul rețelei.
## EVT-CONSOLE-PATH-79 — prioritate 79

Cu reporting.console.enabled false, loaderul Console confirmă disabled,
dar EventuateComponentExtension emite direct ATF_REPORT_PATH după un test PASS.
Primitivele Console au probe proprii verzi; bypass-ul este în adaptorul J2.

- Refolosiți publicarea căii și politica Console existente, păstrând opt-in-ul AtfReportPath independent de mesajele de diagnostic.
- Evitați System.out și un formatter local în adaptor; dependența necesară rămâne test-scoped, fără module noi.
- Probați enabled/disabled și opt-in on/off, individual/suită, cale cu spații și eșec de publicare.

Închidere: nu se emite cale când politica autoritară este disabled; enabled
emite o singură cale absolută numai după publicarea reușită.
