# Adăugarea unui test API în J2

## Pe scurt

Testul API J2 aparține consumerului Eventuate. Primitivele HTTP rămân în Core,
transportul determinist în Mock, iar publicarea folosește Reporting JAR.

| Citește întâi | Scop |
|---|---|
| [Eventuate](../atf-test-eventuate/README.md) | client, validator, fixture și extensie |
| [integrarea](ATF_Eventuate_Integration.md) | DTO upstream versus extensia sintetică de audit |
| [selecțiile](ATF_Eventuate_Testing.md) | cele trei cazuri și acceptanța individual/suită |
| [atlasul Reporting](ATF_Reporting_Library_Atlas.md) | API-ul J6 confirmat și limita rendererului |

## Pașii obligatorii

1. Fixează independent operația, statusul, identitatea și expected-ul complet.
2. Alege explicit MOCK sau o integrare reală separată; clientul curent este legat concret la ScriptedApiTransport.
3. Capturează schimbul Core și provenance înainte de evaluarea business.
4. Publică assertions prin extensia de audit; păstrează failure-ul original și cleanup-ul separat.
5. Verifică individual/suită, Extent/fallback și selecția completă relevantă.

## Ce verifică un test API

| Axă | Oracol |
|---|---|
| wire contract | metodă, cale, query, header și body independente de răspuns |
| răspuns | status, tipuri, orderId, orderState și rejectionReason nullable |
| business | identitate și stare pentru aceeași comandă, nu numai textul APPROVED |
| execuție | tentative, retry eligibil și o singură expediere a mutației |
| evidence | modelul Core și cauza originală înainte de redacție/preview |

Fixture-ul auditDescription este o extensie sintetică pentru proba 240;
nu devine câmp obligatoriu al aplicației Eventuate reale.

## Durată și status în raport

Durata HTTP, durata fazei și timpul virtual al convergenței au sensuri distincte.
Un clock virtual nu probează anularea unui request real la expirarea bugetului.
Momentul publicării nu substituie execuția fazei.

## Separarea responsabilităților

- Core deține execuția și dovezile neutre, fără ruta și expected-ul produsului.
- Callerul Eventuate deține identitatea, contractul și semnalul de convergență.
- Kit evaluează o singură dată; Reporting primește comparația deja decisă.
- Browserul de audit inspectează HTML-ul extern, fără modul UI în consumer.

## Retry, cleanup și failure

POST are o singură tentativă. GET 503 poate avea cel mult două tentative;
convergența repetă citirea fără replay-ul creării.

Traseele terminale și combinațiile stare/identitate sunt criterii distincte
în [feedback-ul Eventuate](feedback/ATF_Eventuate_Feedback.md). Nu se închid
dintr-un nominal MOCK sau numai din apariția unui mesaj în raport.

## Ownership-ul raportului

Raportul implicit este Extent; fallback este un renderer distinct al JAR-ului.
Ownerul class/suite publică la close, iar extensia notifică apoi calea absolută.
Nu se injectează CSS/JavaScript în consumer pentru repararea bibliotecii.
