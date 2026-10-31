# J2 — perimetru verificat

## Reporting J8 — livrarea curentă

Perechea curentă este J8, direct în `lib`, cu Core C1 păstrat.

| Boundary | Dovadă și limită |
|---|---|
| acceptanță curentă | [J8](ATF_Reporting_J8_Verification.md): 40 artifacte Validation, componente MOCK individual/prin suită și gate binar neutru |
| disclosure API fallback | corectat și verificat prin mouse/Enter/Space; verdicturile sunt păstrate per rând |
| restanțe | profilul inspectorului, timing-ul și geometria Warning rămân în feedback-ul Reporting |
| istoric J7 | acceptanțele de mai jos păstrează versiunea executată; nu certifică automat candidatul J8 |

## Reconciliere anterioară — Mock și revizia documentației

Snapshot-ul live a avansat printr-un commit concurent. Cele nouă diferențe
față de captura inițială sunt păstrate: Kit, probele Validation, documentul
de adopție și controalele/proveniența comune; promovarea rămâne selectivă.

| Verificare în staging reconciliat | Rezultat și limită |
|---|---|
| Kit / Mock / Validation | 5 / 80 / 165 invocări selectate, fără eșecuri, erori ori skip |
| Eventuate | cele trei componente prin suită trec; aceasta nu este acceptanță LIVE |
| guvernanță | 166 controale; trei refuzuri inițiale pentru baza controlului Kit și aceeași ancoră mutată sunt corectate și reexecutate cu succes, fără schimbarea testelor |
| documente aprobate | 16 documente verificate prin validatoarele ownerului, fără încălcări |
| Setup offline | 48 contracte și 31 comparații de componentă; fără pornirea engine-ului/brokerului sau recertificarea gazdei |
| artifacte | matricea Mock de 14 rapoarte rămâne aplicabilă; aceleași binare Core C1/Reporting J7 și implementare Mock, cu backlink-ul Extent reverificat |

- review-live-gates.log păstrează inclusiv refuzurile inițiale;
  review-final-corrections.log păstrează reluarea verde a celor trei controale.
- live-reconciliation.json, review-setup-contracts.log și
  review-setup-components.log delimitează snapshot-ul și probele independente.
- J6 rămâne inventar istoric; paginile de consum indică J7, iar catalogul J2
  indică șapte module. Protecția semantică suplimentară rămâne în [feedback](feedback/ATF_Documentation_Feedback.md).

## Mock — fixture JSON complet și constrângeri YAML

Mock J2 folosește un singur parser intern pentru expected, request și
response POST/PUT. Fixture-ul invalid este refuzat înainte de dispatch;
request-ul invalid ajunge la fallback, fără schimbarea priorității rutelor.

| Probă în staging Mock JSON | Rezultat și limită |
|---|---|
| baseline | 59 cazuri noi, 39 eșecuri pe implementarea anterioară; expected gol, trailing content, duplicate și obiect/array YAML fără constrângere |
| regresii permanente | 62 teste noi: constructor, loader, transport, nominale, cauză parser, comparație după preview și matching concurent |
| gate Mock | 80 teste, zero eșecuri, erori sau skip; cele 18 probe existente rămân verzi |
| integrare și guvernanță | trei componente Eventuate prin suită și 166 controale verzi, fără relaxări |
| rapoarte consumer | 14 HTML-uri Extent/fallback: șase cazuri individuale și un raport combinat per renderer, cu un mismatch controlat |
| oracole artifact | identitate, trei rânduri și patru coloane per scenariu, valori și verdict per rând; coloană eliminată și verdict corupt refuzate |
| browser | patru artifacte: expected/response refuzate și rapoarte combinate în Extent/fallback; disclosure implicit închis, Enter/Space și backlink exact, cu vecini PASS izolați |
| inspector generic J7 | 14 neconformități păstrate pentru profilul assertion-only; oracolul consumerului nu declară inspectorul universal verde |
| Core C1 | validDocument acceptă trailing content și documentul al doilea; validateWriteExchange întoarce zero încălcări în proba publică |
| limită numerică | două valori decimale distincte se potrivesc după conversia Double; criteriul nou rămâne deschis, separat de sintaxa JSON |

Contractul stabil și probele sunt în [README Mock](../atf-mock/README.md#document-json-complet--expected-request-și-response).

- Dovezi TMP: incrementul atf-j2-mock-json-615a89c6ad, mock-before.log,
  mock-gates.log, mock-matrix/acceptance.json și promovările controlate.
- core-policy-observation.json și mock-numeric-observation.log păstrează
  contraexemplele publice, fără a le transforma în nominale verzi.
- Rapoartele sunt acceptanțe tehnice ale proiecției; nu certifică timing-ul
  execuției, HTTP real sau brokerul Kafka.

Guard-ul local nu este livrare Core. J0 deține remedierea bibliotecii și
reconcilierea Mock comun; source/binary și deployment-urile au gate-uri separate.

## Media type JSON — contracte canonice

Cele cinci contracte canonice folosesc contentTypeJson, delegat către Core C1.
Headerul text/plain cu parametrul profile="application/json" produce un singur
rând nereușit, cu statusul HTTP și câmpurile payload nominale păstrate.

| Probă în staging media | Rezultat și limită |
|---|---|
| baseline | 27 invocări, 12 eșecuri pe implementarea anterioară; clasificarea literală accepta parametri și refuza nominale cu sufix JSON |
| regresie dedicată | 31 invocări verzi; toate cele cinci contracte în strict/warning, charset, tipuri concrete, header absent sau malformat și compunere cu contains |
| Validation | 214 verificări selectate, zero eșecuri, erori sau skip; inclusiv JSON, scalar, schemă, port neutru și preview |
| consumer și guvernanță | trei componente Eventuate prin suită și 166 controale verzi, pe Core C1 și Reporting J7 |
| artifacte | 40 HTML-uri, inclusiv cele 28 de regresii anterioare; oracole de coloane, cardinalitate, verdict și ACTUAL exact, cu mutanți refuzați |
| verdict per rând | 12 artifacte media type verificate independent; opt mutații care mută verdictul negativ pe alt rând sunt refuzate, chiar cu același număr de eșecuri |
| browser | șase rapoarte Extent/fallback: FAIL, WARNING și nominal; patru coloane, vecini PASS, Fails către dovada originală și Enter/Space Extent |
| Reporting rămas în J7 | disclosure API fallback absent și badge Warning mai lat decât RESULT; J8 închide numai disclosure-ul, iar geometria rămâne la J0 |

Contractul stabil este în [README Validation](../atf-validation/README.md#content-type--clasificare-semantică-json).

- Datele și log-urile sunt în TMP, incrementul atf-j2-media-cd3973d608:
media-before.log, media-final-gates.log, media-matrix/acceptance.json,
media-matrix/row-verdict-audit.json, browser-audit.json și promovările controlate J2/J0.

- Randarea fallback warning a măsurat RESULT de aproximativ 56,9 px și badge
de 61,8 px; wrapperul de 870 px are scrollWidth 877 px. [Feedback-ul J0](../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md)
păstrează criteriul REPORT-FALLBACK-RESULT-WIDTH-101 și proba de componentă,
fără a transfera clasificarea MIME către renderer.

- La execuția matricei, reconcilierea sursei Validation din J0 era încă deschisă.
Acceptanța J2 nu certifică reconcilierea ori execuția J0/J1/J3; implementările
Core/Reporting și cele șapte module J2 sunt păstrate.

## Egalitate scalară Validation — containere refuzate

Cele trei PASS-uri false pentru obiect/array comparat cu șir gol sunt
corectate prin aceeași regulă scalară, fără modificarea Reporting J7 sau Core C1.
Contractul stabil este în [README Validation](../atf-validation/README.md#egalitate-scalară--fără-coerciția-containerelor).

| Probă în staging scalar | Rezultat și limită |
|---|---|
| regresie înaintea corecției | 43 invocări, 12 eșecuri pe codul anterior; include verdict, diagnostic și portul neutru |
| Validation după corecție | 183 verificări selectate, inclusiv 43 scalare; strict/warning, string gol real, număr, boolean, null și comparație înainte de preview |
| integrare consumer | cele trei componente Eventuate trec prin suită; fără trafic extern sau certificare Kafka |
| matrice publică | 28 HTML-uri Extent/fallback; cele nouă fixture-uri existente și cinci cazuri scalare în fiecare renderer |
| oracole de artifact | patru coloane, cardinalitate, verdict și ACTUAL exact; eliminarea coloanei, coruperea verdictului și pierderea valorii sunt refuzate |
| structură de teste | noua regresie are subpachet scalar; limita de opt clase per pachet este păstrată fără excepție |
| guvernanță finală | 166 verificări, zero eșecuri, erori sau skip; controalele Git, proveniența și stilul documentelor rămân active |

Dovezile sunt în TMP, incrementul atf-j2-scalar-517c733433: baseline,
scalar-before.log, scalar-final-gates.log, scalar-governance-final.log,
scalar-matrix/acceptance.json, browser-audit.json și promovarea
selectivă. Valoarea null textuală rămâne compatibilă; un contract care cere
distincția de tip adaugă jsonPathType.

Auditul browser al incrementului scalar J7 a păstrat valorile/verdicturile; colapsarea API fallback lipsea pe acel binar. [Livrarea J8](ATF_Reporting_J8_Verification.md) repetă cazul și închide disclosure-ul, cu mouse/Enter/Space și backlink/reload în TAB.

## Snapshot terminal Eventuate — captură și ownership

Pierderea la eșec este corectată în consumer, pe Core C1 și Reporting J7.
Rezultatul Completed și snapshot-ul Terminal sunt capturate în ordine, cu
aceeași excepție originală și faza efectivă a callback-ului JUnit.

| Probă TMP | Rezultat și limită |
|---|---|
| baseline live | snapshot Core prezent, o tentativă și zero intrări în collector pentru script epuizat; failure-ul inițial este păstrat |
| regresii terminale | 11 teste noi: epuizare, transport, timeout/cancel, retry, ordine după succes, suppressed, self-suppression, encoding fără snapshot și scope-uri restaurate |
| regresii consumer | 19 identitate/overflow și cele trei componente canonice rămân verzi |
| matrice terminală | 36 HTML-uri în Extent/fallback, 72 invocări, 32 failure-uri controlate și patru probe de excepție așteptată cu verdict PASS |
| ownership | SETUP publică și fără TEST BODY; TEARDOWN nu preia răspunsul nominal anterior; statusul assertions este separat de eroarea fazei |
| oracole negative | snapshot omis, fază greșită, coloană ACTUAL eliminată și assertions trecute marcate FAIL sunt refuzate |
| integritate | aceeași excepție ajunge la JUnit; lipsa răspunsului rămâne explicită și timpul capturat nu este recalculat |
| guvernanță J7 | catalogul finit al consumerului include explicit noul test Observation publicat; egalitatea exactă și controalele de boundary sunt păstrate |
| revizie adiacentă istorică | trei PASS-uri pentru container JSON comparat cu string gol; corectate și reverificate în incrementul scalar de mai sus |

Contractul și diagrama sunt la [ownerul Eventuate](../atf-test-eventuate/README.md#snapshot-uri-terminale-și-faze).
[Acceptanța reproductibilă](ATF_Eventuate_Testing.md#snapshot-terminal--acceptanță-consumer)
folosește binarul consumat; sursele Reporting și Core nu sunt modificate.
## Contracte JSON și identitate Eventuate — implementare verificată

Incrementul implementează parsarea completă, prezența typed și subsetul schema
fail-closed în Validation. Eventuate compară OrderContract integral și refuză
ID-ul din afara signed long înainte de conversie; citirea bounded LIVE rămâne
criteriu separat de verdictul convergenței.

| Probă TMP | Rezultat și limită |
|---|---|
| guvernanță completă | 166 verificări verzi, fără controale relaxate; pragul de coeziune și hash-urile platformei sunt respectate |
| regresii semantice noi | 88 invocări Validation și 19 Eventuate, nominale și negative; fără timeout mărit ori replay POST |
| mutații deliberate | invalid tratat ca missing: opt eșecuri; additionalProperties ignorat: două; overflow permis: trei; state-only: două; toate sunt AssertionError, nu erori de linkage |
| consumer Eventuate | 14 HTML-uri, 18 invocări JUnit, șase failure-uri controlate; individual și suită de trei, 37 rânduri în raportul combinat |
| consumer Validation | 18 HTML-uri: nouă fixture-uri în Extent/fallback, cu PASS/FAIL/WARNING, patru coloane și mutanți de artifact refuzați |
| audit TAB MCP | JSON invalid și WARNING, schema items false în fallback, suita Extent și identitatea 42 versus 99; expandare, ultima comparație și navigare exactă din Fails |
| limita J6 existentă | nouă rezultate neconforme ale inspectorului generic în matricea extinsă: șapte fallback și două Extent assertion-only; rezultatele brute sunt păstrate |
| limite funcționale | fără modificări Reporting/Core JAR; fără broker ori aplicație Eventuate LIVE; SLA-ul rețelei nu este demonstrat de clock-ul virtual |

Contractele stabile sunt în [Validation](../atf-validation/README.md#json--prezență-document-și-capabilități),
[Eventuate](../atf-test-eventuate/README.md) și [atlas](ATF_Context_Atlas.md).
Manifestul platformei păstrează hash-urile anterioare și reconcilierea locală;
aceasta nu certifică automat sursele ori acceptanța altui deployment.
<a id="revizie-semantică-a-codului-și-documentației-j2"></a>

## Revizie semantică anterioară — diagnostic înaintea implementării

Revizia separă probele nominale, contraexemplele și implementarea viitoare.
Codul executabil nu a fost reparat în acest increment; feedback-ul de mai jos
conține criteriile rămase, fără a converti un gate structural verde în certificare semantică.

| Probă în staging TMP | Rezultat și limită |
|---|---|
| reactor și guvernanță | 206 invocări verzi, dintre care 166 de guvernanță și 40 selectate pentru Kit, Mock, Validation, Console și Eventuate; fără teste-gate relaxate |
| documente finale | 25 teste documentare verzi și 17 fișiere revizuite fără încălcări de stil, structură sau linkuri |
| matrice de artifacte | 12 HTML-uri noi, 16 invocări, patru AssertionError controlate; oracolul consumerului și contraexemplele sale trec |
| inspector generic J6 | opt neconformități păstrate: șase fallback și două Extent assertion-only; profilul generic rămâne deschis |
| timing J6 consemnat anterior | SETUP 21 ms cu assertions 94 ms și BODY 68 ms cu request 514 ms; nu este probă nouă de latență controlată în această revizie |
| randare | această revizie nu repetă auditul interactiv în browser; acceptanța structurală HTML nu este declarată probă vizuală nouă |
| Validation | zece contraexemple publice cu PASS semantic greșit: parsare/presence, schema și contractul canonic MIME |
| Eventuate | identitate greșită acceptată de awaiter, alias numeric prin overflow, snapshot terminal pierdut și calea Console emisă când enabled este false |
| Mock | expected și response cu trailing content acceptate ca fixture-uri valide |
| Setup | eșecul scrierii receipt-ului maschează cauza primară; controlul pentru listener greșit nu creează topic |
| relevanță Kafka | 16 intrări executabile neschimbate față de acceptanța nativă, aceeași resursă activă și TCP guest accesibil; cele două roundtrip-uri nu au fost reluate în această revizie |

Ownerii și criteriile reproductibile sunt în [Validation](feedback/ATF_Validation_Feedback.md),
[Eventuate](feedback/ATF_Eventuate_Feedback.md), [Mock](feedback/ATF_Mock_Feedback.md)
și [Setup](feedback/ATF_Setup_Feedback.md). Profilele/timing-ul J6 păstrează
[feedback de adoptare](feedback/ATF_Reporting_Consumer_Feedback.md), cu implementarea la J0.

## Perimetru curent — Core C1 binar

J2 declară șapte module sursă: Test Kit, Mock, Validation, Console, Setup,
Eventuate și ATF Tests. Core C1 și Reporting J7 sunt cele două biblioteci
externe, fiecare cu JAR/POM direct în lib.

Probele C1 de mai jos sunt istorice și au folosit Reporting J6, înaintea adoptării J7.
Nu reprezintă o reluare a nativelor Core în această revizie.

| Probă C1 în TMP | Rezultat și limită |
|---|---|
| reactor | șapte module sursă compilate; Core încărcat din JAR; fără UI/Debug sau Playwright |
| native Core pe binar | 221 verificări, zero eșecuri/erori/skip; 220 native plus identitatea binarului |
| Eventuate | trei componente individual și aceeași suită de trei; un singur HTML pentru suită |
| limită a probelor C1 | componente MOCK; Kafka are acceptanță nativă distinctă mai jos, fără rescrierea provenienței Reporting J6 |

[Contractul Core C1](ATF_Core_Consumer.md) descrie publicarea, bootstrap-ul și ownership-ul.

## Podman remote — acceptanță nativă din VM

La 2026-10-31, clientul deja instalat în Windows VM, 5.8.3, comunică cu
engine-ul Linux amd64 rootless 5.7.0 al gazdei prin tcp://192.168.56.1:8888.
Scripturile Setup au rulat silent în staging TMP, fără modificarea rutei locale WSL2.

| Probă nouă | Rezultat și limită |
|---|---|
| inițializare | podman info PASS prin URL explicit și conexiunea numită atf-j2-host; fără instalare necesară |
| execuție individuală | hello-world cu digest fixat, exit 0 și output verificat; containerul unic este absent după --rm |
| repetare E2E Podman | două runde PASS, probeId-uri independente, aceeași imagine și conexiune reutilizată |
| negativ nativ | serverVersion 5.7.1 cerut față de 5.7.0 observat → inițializare FAIL, exit 1; smoke neexecutat |
| componente remote | 30 comparații/refuzuri PASS; include câmpul Identity omis din JSON-ul TCP nativ, engine greșit și oprirea la configurație invalidă |
| regresii Setup locale | 41 contracte offline și 31 comparații de componentă PASS; niciuna nu certifică brokerul |
| limită | engine remote, nu WSL2 guest; Kafka, clientul Java și Eventuate real nu au fost rulate în acest increment |

Receipts native remote-setup.json, remote-podman.json, podman-smoke.json și
remote-e2e.json păstrează rutele, versiunile, probele și logurile comenzilor.
Un defect inițial al adaptorului, care presupunea câmpul Identity prezent,
a produs FAIL păstrat; proba permanentă verifică acum forma TCP observată.
Contractul reproductibil este la [Setup](../atf-setup/README.md#podman-remote-din-vm--traseul-validat).

## Kafka remote — broker și client extern validate

La 2026-10-31, scripturile Kafka comune au rulat silent în staging TMP pe
profilul remote. Sursa Core/Reporting și catalogul celor șapte module rămân
separate de această acceptanță a infrastructurii.

| Probă nativă | Rezultat și limită |
|---|---|
| Start-Kafka de două ori | aceeași identitate de container, imagine și specificație; Kafka 4.1.2 la digestul configurat |
| două roundtrip-uri CLI | PASS, topic/grup/probeId distincte, partiția 0 și offset 0 pentru fiecare mesaj |
| InternalOnly | hostTcpReachable false; receipt-ul păstrează explicit limita față de ruta externă |
| biblioteci Java | kafka-clients 4.1.2 și SLF4J din imaginea pin-uită, copiate în TMP; hash-uri păstrate și fixate în rețeta clientului |
| client Java în Windows VM | PASS individual și în două runde complete; metadata corectă, topic propriu, ack și consum la aceeași partiție/offset |
| suita remote completă | două runde PASS după eliminarea exec -i; patru probe independente, aceeași resursă și buget readiness 60 s păstrat |
| rute după intervenția fizică | API Podman _ping OK, engine rootless 5.7.0 și guest TCP 19092 accesibil |
| negativ metadata | așteptarea unui listener greșit produce FAIL înainte de topic/publish, fără schimbarea brokerului |
| negativ producer | destinație locală închisă produce status nonzero, propagat prin conducta fără stdin remote |
| verificare argumente | text cu shell metacharacters rămâne literal într-o execuție reală, fără stdin remote |
| controale Setup | 48 contracte, 31 componente locale, 30 componente Podman remote și 44 componente Kafka remote PASS |

Probele inițiale FAIL rămân păstrate: port Kafka inaccesibil, API reset și
exec -i eșuat la închiderea sesiunii. Aceste rezultate nu au fost transformate
în PASS; producerul revizuit a fost verificat cu probe noi, fără replay.

- Containerul final este identic cu cel creat la început; imaginea și specificația au rămas constante.
- Regula aplicată pe fizic aparține intervenției operatorului; guest-ul demonstrează funcționarea, fără să pretindă readback-ul ownership-ului Windows sau o probă după reboot.
- Infrastructura folosește receipts native; niciun rezultat nu certifică încă saga, efectele persistente ori idempotency business Eventuate.
- Pașii și transferul launcherului sunt în [ghidul gazdei](../atf-setup/prerequisites/README_Remote_Host.md).

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

## Istoric — verificare infrastructură locală după restart

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
| OrderAwaiter direct, istoric | APPROVED cu orderId 99 acceptat de predicatul state-only; corectat de incrementul de mai sus | [contractul curent](../atf-test-eventuate/README.md) |
| buget virtual 25 ms, citire 40 ms | PASS refuzat, revenire la 40 ms; fără dovada anulării citirii | același criteriu; SLA LIVE separat |
| transport scriptat epuizat, istoric | pierderea în collector este corectată; snapshot-ul și cauza originală sunt păstrate | [contractul terminal actual](../atf-test-eventuate/README.md#snapshot-uri-terminale-și-faze) |
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
