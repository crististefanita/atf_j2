# Raportarea J2 — contractul consumerului

## Pe scurt

J2 deține modelul și adaptorul aplicației; J0 deține Reporting.
Contractele complete ale bibliotecii sunt [la producător](../../atf_j0/docs/ATF_Reporting_Contract.md).
Această pagină fixează cerințele aplicabile consumerului J6.

## Arborele unui test

Sesiune class/suite → scenariu per invocare → SETUP, TEST BODY, TEARDOWN.
Fazele publică assertions colapsabile; numai faza cu schimb HTTP primește
dovezile capturate de Core.

## Context intern și diagnostic al scenariului

Statusul și cauza sunt proiectate din rezultatul runnerului, fără reevaluare
din textul redactat. Un assertion deja înregistrat nu generează un al doilea
diagnostic execution pentru aceeași cauză.

## Dovezile configurării inițiale

SETUP verifică încărcarea Reporting din JAR fizic. Configurația este
capturată la owner, iar reușita TEARDOWN nu poate șterge un FAIL anterior.

## Dovezile unei cereri

Request/response, durata și tentativele provin din modelul Core. Contractul
business se verifică separat, cu identitate și expected independent.
Provenance MOCK rămâne vizibilă și nu certifică rețeaua reală.

## Tabelul agregat de aserțiuni

ASSERTION, EXPECTED, ACTUAL, RESULT sunt coloane distincte, pe spațiul
disponibil. Comparația integrală precede redacția și preview-ul implicit 240.
O diferență după preview rămâne FAIL chiar când textele afișate coincid.

## Fails și legături

Indexul păstrează identitatea scenariului, fazei și comparației originale.
Backlink-ul deschide acea dovadă; Enter/Space și scroll-ul până la ultimul
rând sunt verificate pe artifactul nou.

## Paritate de randare

Extent și fallback păstrează populația și verdicturile, cu structuri DOM
distincte. Inspectorul generic J6 are o limită de profil documentată în
[feedback-ul J2](feedback/ATF_Reporting_Consumer_Feedback.md).

## Dovezi obligatorii pentru schimbări

Matricea [Eventuate](ATF_Eventuate_Testing.md) verifică individual/suită și
negative controlate. Regresia generică a rendererului rămâne în J0; auditul
HTML extern nu cere modulul atf-ui în consumer.
