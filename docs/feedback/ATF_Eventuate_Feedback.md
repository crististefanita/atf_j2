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
