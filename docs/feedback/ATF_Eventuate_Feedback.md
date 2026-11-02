# Eventuate — criterii rămase după porturile LIVE

## Pe scurt

- Observerul Kafka, portul HTTP bounded și witness-ul bazat pe atf-sql sunt implementate. [Dovada incrementului](../ATF_SQL_Live_Verification.md) include 14 comparații pe Kafka/MySQL reale, dar evenimentul business este sintetic.
- Aplicația Eventuate cu servicii/CDC/saga este **NOT RUN**. Resursele altor procese rămân active; testele controlate sunt alese explicit când RAM-ul nu permite laboratorul complet.

## EVT-LOCAL-01 — prioritate 90

Lipsa observerului și compoziției LIVE nu mai sunt ToDo-uri de implementare.
Cele trei clase E2E sunt opt-in, finite și compilate; nu sunt încă acceptate pe aplicația reală.

- Confirmați readiness HTTP și CDC înainte de prima mutație, împreună cu end offsets capturate de observer.
  Start înregistrează STARTED_REQUIRES_READINESS; proces pornit nu înseamnă aplicație pregătită.
- Validați schema și migrațiile commit-ului upstream fixat, cele două query-uri și contul SELECT-only.
  Proba MySQL sintetică nu certifică mapperul business; [criteriul SQL](ATF_SQL_Feedback.md#sql-application-witness-118--prioritate-85) delimitează această parte.
- Rulați aprobare, credit insuficient și client necunoscut individual și prin suită cu date owned,
  un singur POST per operație, corelare message/aggregate/order/customer și efecte independente.
- Documentați cleanup-ul datelor business și starea topic-urilor care lipsesc; nu creați automat
  un topic aplicație numai pentru a trece readiness-ul și nu introduceți simulator în Kit.
- Păstrați memoria ca precondiție explicită. Nu opriți engine-ul sau alte containere și nu afirmați
  acceptanță saga din raportul de infrastructură ori dintr-un skip E2E.

Închidere: aceleași trei cazuri trec individual/prin suită, Extent/fallback au faptele corelate
și randările inspectate; readiness, schema și cleanup-ul sunt probate inclusiv negativ.
Fără timeout mărit sau replay al mutațiilor. Mongo/CQRS nu este inclus în rețeta redusă actuală.

## EVT-IDEMPOTENCY-02 — prioritate 88

După acceptanța aplicației, separați duplicatul de transport cu același message ID de operația
business cu ID nou și de repetarea POST-ului. Proiecția Mongo upstream și detectorul său cer
reproducere pe varianta fixată; nu deduceți un defect executat din inspecția statică.

- Demonstrați două livrări și un singur efect SQL, rezervarea creditului și proiecția CQRS coerentă.
- Includeți credit concurent și recuperarea CDC, cu witness-uri independente.
- Păstrați H2 ca regresie locală, fără a confunda UNIQUE-ul fixture-ului cu deduplicarea upstream.

Închidere: efecte reale și raport corelat pentru nominal, duplicat și eșec; defectele upstream
sunt raportate cu dovadă, fără relaxarea oracolelor. Criteriul depinde de EVT-LOCAL-01.

## EVT-CONVERGENCE-CONTRACT-86 — deadline al citirii LIVE

**Prioritate: 84/100.** Propagarea timpului rămas este implementată prin BoundedOrderReader,
LiveDeadline și transportul HTTP cu o tentativă. Componentele controlate verifică reducerea
bugetului, expirarea înainte de dispatch și absența replay-ului; nu le redeschideți ca API lipsă.

Rămâne proba pe rețea: răspuns lent sau blocat, anulare reală, starea terminală și cauza originală,
individual/prin suită. Timpul total nu se reînnoiește între GET-uri; raportul păstrează istoricul
și absența răspunsului. Proba MySQL bounded verifică JDBC, nu anularea socket-ului HTTP.

- Închidere: portul și transportul real sunt măsurate sub deadline, inclusiv negative, fără timeout mărit. Clock-ul virtual și transportul injectat dovedesc politica, nu SLA-ul serviciului.

## EVT-CONSOLE-PATH-79 — prioritate 79

Cu reporting.console.enabled false, loaderul Console confirmă disabled,
dar EventuateComponentExtension emite direct ATF_REPORT_PATH după un test PASS.
Primitivele Console au probe proprii verzi; bypass-ul este în adaptorul J2.

- Refolosiți publicarea căii și politica Console existente, păstrând opt-in-ul AtfReportPath independent de mesajele de diagnostic.
- Evitați System.out și un formatter local în adaptor; dependența necesară rămâne test-scoped, fără module noi.
- Probați enabled/disabled și opt-in on/off, individual/suită, cale cu spații și eșec de publicare.

Închidere: nu se emite cale când politica autoritară este disabled; enabled
emite o singură cale absolută numai după publicarea reușită.

## EVT-KAFKA-CAPTURE-121 — bound every retained record field

**Priority: 84/100** = impact 34 + gap 24 + actionable 18 + documentation 8.
**Owner:** Eventuate Kafka capture in J2; renderer implementation stays in J0.

- A controlled call to the current EventuateKafkaDecoder accepts an envelope of 106 characters together with a 1,048,576-character key and retains that key in KafkaEvidence.Event.
- No broker was contacted.
- The 65,536-character check covers record.value only; max.poll.records and fetch settings do not bound all retained fields.
- The observer also lacks an aggregate evidence budget.

- [ ] Define effective limits for keys, envelope fields and aggregate retained
  evidence, with explicit units. Reject before retaining the over-budget event;
  retain the accepted prefix, broker coordinates and the omission/failure reason.
- [ ] Prove boundary values, oversized keys, many individually valid events,
  multibyte text and failure publication. Keep the original exception primary.
- [ ] Do not call a truncated prefix COMPLETE, silently drop foreign records or
  replay a mutation. Kafka client buffering and ATF retained evidence are
  separate resource contracts; document both limits precisely.

**Closure:** bounded retained evidence and truthful failure artifacts pass
pure contracts and the adopted consumer matrix. This does not certify CDC/saga.

### Current audit — console and LIVE scope

EVT-CONSOLE-PATH-79 remains reproduced: an explicit YAML with
reporting.console.enabled=false is loaded as false, yet one passing API
component emits ATF_REPORT_PATH directly from the extension owner. Route the
path through the existing configured Console policy; do not add a second
console switch. The initial property-only diagnostic did not configure this
YAML key and is not the proof used here.

LIVE remains NOT RUN. Prioritize actual readiness/schema before treating the
three compiled E2E classes as application acceptance. SQL, MOCK and real
infrastructure evidence retain their separate provenance.

## Core/Kit producer handoff — 2026-11-01

- CORE-TRANSPORT-CANCEL-125 reproduces mapped-future cancellation without an
  upstream cancel call in HttpClientTransport. Fix and characterize that at
  [Core](../../../atf_j0/docs/feedback/ATF_Core_Feedback.md), then retain J2's
  real HTTP deadline/cleanup acceptance in EVT-CONVERGENCE-CONTRACT-86.
- A cancelled future is not server rollback. Keep one mutation dispatch and
  preserve uncertain outcomes; no automatic POST replay or deadline increase.
- CORE-CONSOLE-REDACTION-124 concerns a configured console sink. The default
  Eventuate LIVE composition has a no-op sink, so this audit does not demonstrate
  a credential leak from that composition. EVT-CONSOLE-PATH-79 stays separate.
- KIT-UNICODE-PREVIEW-127 affects the neutral preview before rendering. Its
  [Kit owner](../../../atf_j0/docs/feedback/ATF_Common_Test_Support_Feedback.md)
  needs a native fix and regression before J2 adoption; keep the 240 default.

No C2/J10 JAR is replaced by this feedback handoff. Generic fixes and tests
stay in J0; Kafka/SQL capture and actual application acceptance remain in J2.

### Reconfirmare J14 — bootstrap LIVE indisponibil

| Probă explicită | Rezultat verificat |
|---|---|
| EventuateApprovedOrderE2eTest | un ERROR, fără skip; KafkaException cu ConfigException: No resolvable bootstrap urls |
| efect business | comanda nu a fost executată |
| Extent | FAIL și faza TEST BODY; Fails → Tests ajunge la cauza originală |
| diagnostic | în HTML apare wrapper-ul; cauza imbricată este completă în JUnit |
| componente | acceptanță Extent/fallback distinctă, fără eticheta LIVE |

- Criteriul existent de readiness LIVE rămâne deschis până la conexiunea laboratorului și probele aplicației.
- Îmbunătățiți la caller diagnosticul cauzei imbricate cu redacția existentă; fără replay POST ori timeout mărit.

Dovadă: [audit J14](../ATF_Reporting_J14_Verification.md), journeys-j2-live.log și consumer-journeys/j2-live din staging-ul datat.
