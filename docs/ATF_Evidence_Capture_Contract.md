# Evidence J2 — observație, comparație și publicare

## Pe scurt

Eventuate deține observația produsului și expected-ul. Core produce schimbul
HTTP, Kit evaluează comparația, iar Reporting publică modelul. Setup păstrează
separat receipt-ul infrastructurii reale.

| Sursă | Dovadă | Limită |
|---|---|---|
| HTTP scriptat | request/response, tentative și ACCEPTANCE_MOCK | nu demonstrează endpoint real |
| message fixture | identități, livrări și witness MOCK_EVENT_FEED | ordinalul nu este offset broker |
| clock virtual | observații și durată virtuală | nu demonstrează SLA real |
| Setup | comandă, exit code, durată și stdout/stderr în TMP | numai pasul efectiv executat |
| Reporting | artifact și comparații publicate | fără reevaluarea verdictului din display |

## Alegerea dovezii înaintea verdictului

Identitatea comenzii și sursa observației sunt fixate înaintea evaluării.
O dovadă stale, lipsă sau aparținând altui ID nu devine succes numai pentru
că starea sa este APPROVED.

Un request terminal trebuie să păstreze cauza și snapshot-ul disponibil.
Traseul este implementat și protejat de [contractul terminal Eventuate](../atf-test-eventuate/README.md#snapshot-uri-terminale-și-faze).
Același obiect de excepție și evidence-ul fără răspuns inventat sunt probate în toate fazele; această dovadă nu certifică integrarea LIVE.

## Responsabilități și limite

- Main păstrează clientul, validatorul și compoziția de publicare; JUnit/fixture-urile sunt în test.
- Un collector este per invocare; raportul comun nu partajează scriptul sau starea business.
- Redacția și limitele de display nu modifică input-ul comparației.
- Captura browser a HTML-ului este audit extern, fără modul UI în Eventuate.

## Unde se aprofundează

[Atlas J2](ATF_Context_Atlas.md), [Testing](ATF_Testing_Guide.md) și
[Reporting J7](ATF_Reporting_J7_Verification.md) descriu compoziția curentă.
Contractele browser și screenshot-urile aplicațiilor web sunt la [J1](../../atf_j1/docs/ATF_Evidence_Capture_Contract.md).
