# Validation — contracte semantice rămase

## Pe scurt

Parsarea completă, prezența typed și subsetul schema fail-closed sunt contracte
livrate în [README Validation](../../atf-validation/README.md#json--prezență-document-și-capabilități).
Regresiile publice și acceptanța Extent/fallback rămân permanente; criteriile
închise nu sunt păstrate ca ToDo. Limita deschisă este clasificarea media type
în contractele canonice, nu rendererul.
## VALIDATION-MEDIA-92 — prioritate 86

ContractValidators.userItem acceptă text/plain cu parametrul profile care
conține application/json, deși clasificarea strictă din Core îl refuză ca JSON.
Regula contentTypeContains face literal ce spune numele; callerul canonic o
folosește pentru o cerință semantică mai strictă.

- Păstrați compatibilitatea operației contains; adăugați regula semantică necesară și folosiți vocabularul Core C1.
- Actualizați contractele canonice, nu rendererul; testați charset, sufix +json și parametrul care imită alt tip.

Închidere: parametrul înșelător nu satisface contractul JSON, iar tipurile
acceptate sunt explicite și verificate fără trafic extern.
