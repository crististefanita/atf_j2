# Eventuate — feedback deschis

## Pe scurt

Cele trei componente verifică oracolele și integrarea ATF pe fixture-uri.
Acceptanța aplicației reale rămâne deschisă; raportul MOCK nu este dovadă
pentru procesarea brokerului sau pentru tranzacțiile bazei de date.

## EVT-LOCAL-01 — prioritate 95

Pregătește un laborator separat cu versiuni și imagini pin-uite, fără output-uri
de build în checkout. Verifică readiness-ul serviciilor și observerul înaintea
mutației; construiește prima probă aprobare/respingere cu date proprii.

Închidere: aceleași cazuri trec individual și în suită, cu provenance live,
identități corelate, citiri independente și randări Extent/fallback inspectate.
Nu mări timeout-ul pentru a ascunde o precondiție neconfirmată.

## EVT-IDEMPOTENCY-02 — prioritate 93

Separă duplicatul de transport cu același message ID de repetarea operației
business cu ID nou și de repetarea POST-ului. Proiecția Mongo folosește un
detector fără deduplicare; replay-ul vechi este un risc static de reprodus.

Închidere: demonstrează procesarea duplicatului, efectul SQL unic și starea
CQRS coerentă; verifică și creditul concurent și recuperarea CDC. Un defect
upstream se raportează cu dovadă, fără relaxarea oraclelor ATF.

## EVT-CONVERGENCE-CONTRACT-86 — identitate și deadline real

**Prioritate: 88/100** = impact 34 + lacună 25 + lucrabil 20 + documentare 9.
**Owner:** callerul Eventuate și politica de observare, înainte de integrarea reală.

Proba directă pe sursa live confirmă că awaiterul state-only acceptă
APPROVED pentru orderId 99. Testul curent de convergență nu compară ID-ul
comenzii la fiecare observație; scriptul nominal furnizează numai ID 42.

| Probă de buget | Rezultat | Limită |
|---|---|---|
| 25 ms disponibili, citire de 40 ms în clock-ul virtual | PASS refuzat; revenire la 40 ms | deadline observat, fără dovada anulării citirii sau propagării timpului rămas către HTTP |

- [ ] Compuneți în caller un criteriu independent pentru identitate, stare și contract, fără a muta politica Eventuate în Kit.
- [ ] Probați ID greșit, identitate schimbată între citiri, stale APPROVED și răspuns terminal; raportul trebuie să arate comparația relevantă.
- [ ] Pentru LIVE, propagați timpul rămas către citirea bounded și păstrați cauza la expirare; fără timeout mărit sau replay de mutații.

Închidere: nominalul și negativele trec individual/suită, cu timp măsurat
și dovadă auditabilă. Proba clock-ului virtual rămâne distinctă de SLA real.

## EVT-TERMINAL-EVIDENCE-87 — snapshot-ul Core înaintea excepției

**Prioritate: 86/100** = impact 34 + lacună 24 + lucrabil 19 + documentare 9.
**Owner:** EventuateEvidence și adaptorul de publicare J2.

Cu transportul scriptat epuizat, observe produce ApiExecutionException cu
snapshot terminal Core disponibil; collectorul Eventuate are zero exchanges.
Callback-ul de record este executat numai după returnarea reușită a executorului.

- [ ] Păstrați separat rezultatul și eșecul terminal typed, inclusiv request, tentative, provenance și timing deja capturate.
- [ ] Propagați aceeași excepție originală spre JUnit; nu transformați excepția în PASS și nu inventați un răspuns HTTP.
- [ ] Probați script epuizat, timeout/cancel și eroare de transport, apoi verificați artifactul Extent/fallback și cauza exactă.

Închidere: lipsa rezultatului normal nu elimină snapshot-ul disponibil din
raport; failure-ul și metadatele sale rămân corelate și publicate o singură dată.
Constatarea collectorului este reprodusă offline; artifactul negativ complet
rămâne proba de închidere, nu este declarat verificat în acest audit documentar.
