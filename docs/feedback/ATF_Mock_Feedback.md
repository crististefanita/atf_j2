# Mock — documentul întreg în contractul fixture-ului

## Pe scurt

Validarea request-ului folosește politica strictă Core, dar constructorul
fixture-ului și comparația expected-ului păstrează parsare permisivă locală.
Ownerul este Mock; verdictul Reporting nu poate repara un fixture acceptat greșit.

## MOCK-STRICT-FIXTURE-85 — prioritate 85

Prin API-ul public, expectedRequestBody egal cu un obiect JSON urmat de
trailing content este acceptat și se potrivește unui request cu obiectul curat.
Un body de răspuns POST cu trailing content este de asemenea acceptat.

| Suprafață | Owner live | Cerință |
|---|---|---|
| constructor și comparație | [MockRoute](../../atf-mock/src/main/java/atf/mock/MockRoute.java) | document complet, nu numai primul arbore Jackson |
| policy existentă | Core C1 JsonBodyPolicy | evitați validatoare de JSON cu accepții incompatibile |
| probe permanente | [teste Mock](../../atf-mock/src/test/java/atf/mock) | malformed expected, malformed response și nominal independente |

- Unificați politica de document complet pentru request, expected și response, păstrând modurile negative intenționate.
- Refuzați fixture-ul malformed înainte de dispatch; fallback-ul nu trebuie să transforme eroarea de configurare în succes.
- Păstrați matching-ul și prioritatea rutelor existente; problema nu cere modificarea semanticii wildcard sau query.
- Reconcilierea codului comun se face selectiv în J0/J1/J2; nu modificați sursa Core ori Reporting în consumer.

Închidere: configurațiile cu trailing content sunt refuzate, matching-ul
semantic nominal rămâne corect și regresiile rulează prin reactorul ownerului.
