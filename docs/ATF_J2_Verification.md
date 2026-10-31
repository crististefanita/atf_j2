# J2 — perimetru verificat

## Perimetru curent — Core C1 binar

J2 declară șapte module sursă: Test Kit, Mock, Validation, Console, Setup,
Eventuate și ATF Tests. Core C1 și Reporting J6 sunt cele două biblioteci
externe, fiecare cu JAR/POM direct în lib.

| Probă C1 în TMP | Rezultat și limită |
|---|---|
| reactor | șapte module sursă compilate; Core încărcat din JAR; fără UI/Debug sau Playwright |
| native Core pe binar | 221 verificări, zero eșecuri/erori/skip; 220 native plus identitatea binarului |
| Eventuate | trei componente individual și aceeași suită de trei; un singur HTML pentru suită |
| limită | MOCK, fără broker Kafka real; acceptanța Reporting J6 istorică nu este rescrisă |

[Contractul Core C1](ATF_Core_Consumer.md) descrie publicarea, bootstrap-ul și ownership-ul.

## Acceptanță anterioară — opt module fără UI

Rezultatele acestei secțiuni precedă retragerea sursei Core; nu reprezintă noua acceptanță C1.

La 2026-10-31, J2 declară opt module: Test Kit, Core, Mock, Validation, Console, Setup, Eventuate și ATF Tests. UI și Debug UI sunt retrase; Reporting rămâne același J6 direct în `lib`.

| Probă nouă în TMP | Rezultat și limită |
|---|---|
| reactor complet | opt module compilate și instalate în cache TMP; arborele Maven nu conține UI, Debug UI sau Playwright |
| Core, componente, guvernanță | 42 + 3 + 156 invocări, zero FAIL/ERROR/SKIP; include refuzul UI shadow și al dependențelor browser |
| matrice individual/suită | 16 invocări, exact patru AssertionError intenționate; 12 HTML-uri, câte șase per renderer |
| TAB MCP focalizat | Extent individual/suită, assertion-only, backlink FAIL și fallback suită; expandare prin click, Enter și Space, inclusiv ultima comparație |
| limite deschise | inspectorul generic J6 și separarea timing-ului capturat de publicare au [feedback explicit](feedback/ATF_Reporting_Consumer_Feedback.md); Kafka real nu a fost executat în acest increment |

README-urile, diagramele, linkurile și metadatele descriu perimetrul de opt module. Codul UI și testele sale rămân în J0/J1; nu sunt copiate pentru a satisface build-ul J2.

## Verificare infrastructură după restart

La 2026-10-31, Setup și prima probă E2E de infrastructură au fost reluate
silent din staging TMP. Ultimul boot observat este 14:23:22, ora București;
dovezile native nu generează un verdict Kafka PASS.

| Probă | Rezultat și limită |
|---|---|
| boot / Windows features | restart confirmat, VirtualMachinePlatform activ, fără component restart pending |
| WSL / Podman | 2.7.14 și 5.8.3; WSL status exit 0, fără distribuție pornită |
| inițializare efectivă | exit 125; importul WSL2 refuzat cu HCS_E_HYPERV_NOT_INSTALLED |
| jurnal boot | Hyper-V-Hypervisor Event 41: VMX absent sau neactivat |
| orchestrare | FAIL la Initialize-Podman, păstrat în setup.json; Kafka nu a fost atins |
| diagnostic după clock shift | în boot-ul anterior, filtrul CIM omite Event 41; ordinea RecordId îl recuperează; markerul nou păstrează eșecul din ultimul boot |
| verificări scripturi | 35 contracte offline și 31 comparații de componentă PASS; brokerExecuted false; lipsa numelui VM eșuează fără prompt |
| E2E infrastructură | runda 1 FAIL la Initialize-Podman; runda 2 și acceptanța NOT_RUN, fără provisioning Kafka |
| colector VirtualBox host | lipsa argumentului VM și rularea în guest sunt refuzate cu receipt; colectarea pe gazda fizică rămâne neexecutată |
| componente Eventuate | trei cazuri nominale PASS; matricea separată are 16 invocări, exact patru AssertionError intenționate și 12 rapoarte |
| TAB MCP focalizat | Extent/fallback suită și FAIL; setup/body/teardown, lățimi, ultima comparație, Enter/Space și backlink către assertion |
| guvernanță Setup înainte de Core C1 | 157 verificări PASS, fără FAIL/ERROR/SKIP; snapshot-ul anterior cu opt module; acceptanța C1 folosește catalogul de șapte module sursă | 

[Atlasul Setup](ATF_Setup_Atlas.md) descrie diagnosticul reproductibil și
remedierea necesară pe gazda VirtualBox. [Feedback-ul](feedback/ATF_Setup_Feedback.md)
păstrează acceptanța reală deschisă; probele anterioare MOCK/HTML rămân separate.

## Contraexemple offline pentru auditul documentar

Probele diagnostice anterioare au rulat în TMP pe sursa importată, fără trafic
extern ori broker. Rândurile Core păstrează constatări istorice; contractele
actuale se citesc la owner și nu sunt redeschise automat prin acest tabel.

| Probă | Observație | Criteriu |
|---|---|---|
| OrderAwaiter direct | APPROVED cu orderId 99 acceptat de predicatul state-only | [identitate și convergență](feedback/ATF_Eventuate_Feedback.md#evt-convergence-contract-86--identitate-și-deadline-real) |
| buget virtual 25 ms, citire 40 ms | PASS refuzat, revenire la 40 ms; fără dovada anulării citirii | același criteriu; SLA LIVE separat |
| transport scriptat epuizat | Core păstrează snapshot terminal; collectorul Eventuate are zero exchanges | [dovadă terminală](feedback/ATF_Eventuate_Feedback.md#evt-terminal-evidence-87--snapshot-ul-core-înaintea-excepției) |
| redacție Core | JSON valid mascat; quoted text și fallback JSON invalid păstrează canary-ul sintetic | [Core](../../atf_j0/atf-core/README.md#media-type-headere-și-vocabular-de-protocol) |
| media type | text/plain cu parametrul profile=json clasificat ca JSON | istoric; [contractul Core actual](../../atf_j0/atf-core/README.md#media-type-headere-și-vocabular-de-protocol) |
| Content-Type construit | default JSON și override form apar simultan; oprire înainte de send | istoric; contract Core și proba de precedență header |

Artifactul negativ terminal rămâne probă de închidere neexecutată în acest
audit. Starea hostului postrestart este documentată separat; aceste probe nu certifică Kafka.

## Dovezi anterioare Eventuate/Setup

Tabelul următor descrie acceptanța anterioară retragerii celor două module browser; numărul zece aparține acelui snapshot.

Incrementul Eventuate/Setup a fost verificat la 2026-10-31 în staging TMP,
după reconcilierea fundației live. Același JAR J6 este identificat prin
[proveniența originală](../library-provenance.json); implementarea Reporting rămâne în J0.

| Probă executată | Rezultat și limită |
|---|---|
| build reactor | zece module compilate/instalate în cache TMP; nu reprezintă execuția tuturor testelor platformei |
| WholeAtfGovernanceSuiteTest | 159 verificări, zero FAIL/ERROR/SKIP, pe baza Git reală a J2 |
| Eventuate individual și suită | 16 invocări JUnit; exact patru AssertionError intenționate, fără abort/container failure |
| Extent/fallback | 12 HTML-uri noi, câte șase per renderer; hash și verdict în receipt |
| oracol HTML al consumerului | trei faze per scenariu, patru coloane; removed-table/inverted-result refuzate pe fiecare artifact |
| browser | toate cele 12 rapoarte; setup/body/teardown, ultimul rând, comparații, HTTP, retry, Fails, backlink și Enter/Space |
| lățimi | viewport normal și 1000 px; tabele pe spațiul disponibil, fără overflow orizontal observat |
| scripturi Setup | 30 controale offline: parser, ownership, configurație, stdin, mediu scoped, timeout și erori de proces |
| prerequisites host | Podman 5.8.3 și WSL 2.7.14 instalate; VirtualMachinePlatform activat cu restart necesar, fără repornire efectuată |
| Kafka real | neexecutat: Setup refuză corect continuarea la restart pending; virtualizarea imbricată trebuie verificată după restart |

## Limitele dovezii

- În Extent și fallback, mismatch-ul după caracterul 240 rămâne FAIL, chiar
când preview-urile coincid. O cauză assertion înregistrată nu devine un al
doilea diagnostic execution; deep-link-ul deschide comparația originală.

- Inspectorul generic J6 păstrează rezultatele originale, inclusiv refuzul
scenariului fără HTTP și neaplicabilitatea unor contracte Extent pe fallback.
Acceptanța consumerului nu declară acest API universal verde;
[feedback-ul](feedback/ATF_Reporting_Consumer_Feedback.md) are owner și criteriu separat.

- Rapoartele, cache-urile și logurile sunt în TMP și nu se promovează în checkout.
[Ghidul Eventuate](ATF_Eventuate_Testing.md) și [Setup](../atf-setup/README.md)
descriu reproducerea; saga, SQL/Mongo și idempotency business reale rămân necertificate.
