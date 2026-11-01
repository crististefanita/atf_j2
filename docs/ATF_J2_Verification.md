# J2 — perimetru verificat

## Acceptanța curentă SQL/LIVE pe C2/J10

[Incrementul SQL/LIVE](ATF_SQL_Live_Verification.md) adaugă explicit al optulea modul și separă probele reale de aplicația NOT RUN.

### Livrarea anterioară J10 — înaintea modulului SQL

[J10](ATF_Reporting_J10_Verification.md) fixează candidatul curent: guvernanța
completă J2, componentele API individual/prin suită în Extent/fallback și
suita SQL H2 au acceptanță proprie. Core C2 și scope-ul de șapte module rămân.

Matricele SQL/browser J9 de mai jos păstrează execuțiile și hash-urile lor
istorice. Selecția J10 nu declară repetate toate cele 18 artifacte SQL,
brokerul remote ori o capabilitate SQL Reporting încă nelivrată.

## Istoric C2/J9 și incrementuri anterioare

## Revizie live — priorități și handoff SQL

La 1 noiembrie 2026, snapshot-ul inițial de 372 de fișiere era identic cu
staging-ul final Kit. În timpul reviziei, livrarea paralelă Core C2/Reporting
J9 a schimbat codul, binarele și documentele; staging-ul este resincronizat
cu cele 374 de fișiere curente înainte de validarea finală.

| Dovadă | Rezultat și limită |
|---|---|
| anti-undo | guard-ul a refuzat promovarea pe feedback-ul J0 schimbat concurent; noile criterii SQL sunt adăugate peste versiunea live, fără restaurarea ToDo-urilor închise |
| contraexemplu Mock inițial | pe C1, nominalul și zecimala distinctă peste 2^53 selectau ambele 201, iar controlul 42.0 selecta fallback 409 |
| reconfirmare după sync | pe sursele Mock curente și Core C2: 201 pentru nominal, 409 pentru valoarea distinctă și 409 pentru control; criteriul numeric este închis |
| verificări curente | 14 controale Git/documentare și 11 teste Eventuate trec pe C2/J9; opt documente J2 și trei J0 sunt verificate cu validatorii ownerilor, fără a declara guvernanța J0 completă verde |
| matrice SQL pe binarul curent | 18 HTML-uri Extent/fallback, 40 invocări, opt FAIL-uri controlate, oracole și mutanți refuzați; individual/suită/combinate, CodeSource și SHA-256 C2/J9 confirmate |
| limită vizuală | matricea verifică artifactele și semantica; nu este un audit browser nou al stării vecinilor, restanța J0 114 rămâne deschisă |
| ownership și scop | Reporting J0, șapte module J2 și preview 240; această revizie schimbă numai documente, fără implementare sau teste de proiect |
| restanță SQL de raportare | query/bind-uri/schema lipsesc din snapshot; cererea J0 116 și componentele dedicate sunt backlog, fără capabilitate SQL nouă în J9 |

Topul J2 rămas este laborator LIVE, idempotency pe aplicație și păstrarea
cauzei primare Setup. Citirea bounded precede primul LIVE, iar idempotency
urmează laboratorului; adoptările J0 au acceptanță separată în [registru](feedback/README.md).

Dovezi TMP: atf-j2-live-review-156e509f8a, snapshot.json,
atf_j2-initial-baseline.json, atf_j2-baseline.json,
mock-numeric-live-review.log, mock-numeric-c2-review.log, c2-j9-review-gates.log,
c2-j9-runtime-identity.log și sql-c2-j9-matrix/acceptance.json. Rezultatele
anterioare de mai jos rămân istorice pentru C1/J8; nu sunt prezentate ca
rerulări C2/J9. Infrastructura Kafka nu este rulată în revizia documentară.

## Coeziune Kit — relocare verificată în J2

Snapshot-ul are 373 de intrări la baza Git
b5ea7e22d4fbad7deac5d7b8a651de1a2f7dd16c. Cele două regresii TEST_BODY sunt
mutate în assertions.requirements, cu numai package/import schimbate;
implementarea main, regula opt, allow-list-urile și selecțiile SQL sunt păstrate.

| Probă în staging propriu | Rezultat și limită |
|---|---|
| integritatea migrării | corpurile testelor și fixture-urilor sunt identice; pachetul inițial trece de la zece la opt clase, requirements are două |
| modulul Kit complet | 133 invocări, zero eșecuri, erori sau skip; cele două clase mutate execută aceleași șapte plus trei cazuri |
| guvernanță completă | 167 controale trec, inclusiv coeziunea și controlul Git; failure-ul Kit din incrementul SQL este închis fără excepție de prag |
| consumer Eventuate | 11 invocări: cinci oracole SQL și câte trei componente MOCK/SQL prin suitele explicite; toate trec |
| matrice SQL | 18 HTML-uri, 40 invocări și opt FAIL-uri controlate; individual/suită/combinate în Extent/fallback, cu oraclele și mutanții existente |
| audit TAB MCP | trei rapoarte: combinat PASS, SQL failure prin suită și duplicat fallback; Enter/Space, JSON integral, patru coloane, ultimul rând și Fails către cauza originală |
| documente finale | 14 controale Git/documentare verzi și șase documente validate; criteriul Kit închis este retras, cu istoria păstrată |
| proveniență | 170 de intrări curente; noua reconciliere păstrează previousPath și amprentele anterioare, inclusiv importul original J1 |
| perimetru | șapte module, aceleași Core C1 și Reporting J8 și preview 240; fără modificări Reporting ori infrastructură LIVE |

- [Contractul Kit](../atf-test-kit/README.md#pachetele-regresiilor-de-audit) și
  [atlasul J2](ATF_Context_Atlas.md) dețin organizarea curentă. Feedback-ul 114
  este retras, iar controlul KIT-COHESION-115 rămâne în baseline împreună cu istoricul KIT-BODY-J2.
- Dovezi TMP: incrementul atf-j2-kit-cohesion-db49fee4ea, migration-audit.json,
  kit-full-tests.log, cohesion-reactor.log, sql-matrix/acceptance.json și
  browser-audit.json; cohesion-document-gates.log păstrează cele 14 controale finale. Rapoartele verifică integrarea consumerului pe J8, fără teste native Reporting copiate.
- Relocarea este locală J2; nu certifică sau suprascrie checkout-urile vecine.
  H2 rămâne schema fixture ATF, cu acceptanța aplicației reale separată.

## SQL embedded H2 — primul increment J2

Snapshot-ul inițial are 352 de intrări și HEAD
b5ea7e22d4fbad7deac5d7b8a651de1a2f7dd16c. H2 2.5.252 este test-scoped,
iar witness-ul main folosește numai JDBC din JDK; Core C1, Reporting J8,
cele șapte module și preview-ul 240 sunt păstrate.

| Probă TMP | Rezultat și limită |
|---|---|
| contract SQL | cinci teste pure și trei componente: tranzacții, identitate/duplicate și failure JDBC; schema aparține fixture-ului ATF |
| native după refresh live | 56 invocări Eventuate și zece Kit, fără eșecuri, erori sau skip; include cele două suite și regresiile terminale existente |
| conexiune independentă | necomis invizibil, commit vizibil, rollback păstrat; cinci SELECT-uri, cinci conexiuni noi închise și baza absentă după owner close |
| oracol SQL | zero rânduri, ID străin, duplicate, sumă/stare greșită și zecimale mari refuzate față de expected fix; baze izolate per invocare |
| failure | aceeași SQLException/cauză, cleanup suppressed, rând parțial păstrat cu FAILED; collector o dată și nicio reluare SQL |
| matrice SQL finală | 18 HTML-uri, 40 invocări, opt FAIL-uri controlate; individual, suită și combinație cu trei componente MOCK, în Extent/fallback |
| regresii artifact adiacente | 24 de convergență și 36 terminale HTTP pe Kit-ul reconciliat; 78 de artifacte finale în total, cu mutanți refuzați |
| audit TAB MCP | patru rapoarte variate și două reverificate după refresh Kit; JSON integral, Enter/Space, patru coloane, scroll interior până la ultimul rând și Fails către cauza originală |
| gate focalizat final | 11 invocări SQL/MOCK și 17 controale Git/documentare/selecții verzi; controlul SQL este verificat separat de restanța Kit |
| guvernanță completă după refresh | 166 din 167 trec; un failure Kit paralel are zece clase de teste în același pachet, peste pragul opt; nicio regulă nu este relaxată |
| proveniență Kit | două surse și două native identice cu J1, deja livrate concurent în J2; manifestul păstrează istoricul și include acum 170 de intrări |

- [Contractul SQL și diagrama](../atf-test-eventuate/src/main/java/atf/eventuate/sql/README.md)
  separă writer, witness, snapshot și oracol. [Comenzile de acceptanță](ATF_Eventuate_Testing.md#sql-embedded--acceptanță-consumer)
  folosesc H2 și JAR-ul Reporting fizic din classpath-ul consumerului.
- La închiderea incrementului SQL, [criteriul Kit](../atf-test-kit/README.md#pachetele-regresiilor-de-audit) păstra failure-ul real
  și reorganizarea necesară; mutarea ulterioară este verificată în incrementul de coeziune, separat de implementarea SQL.
- Dovezi TMP: incrementul atf-j2-sql-8309190481, sql-components-first.log,
  sql-live-reconciled-reactor-reviewed.log, sql-matrix-live/acceptance.json,
  adjacent-convergence-live/acceptance.json, adjacent-terminal-live/acceptance.json
  și browser-audit.json. sql-final-gates.log delimitează gate-ul focalizat SQL/documente.
- Refuzurile sunt păstrate: sql-oracle-thread-dump.log arată backtracking-ul
  oracolului inițial, corectat prin delimitarea snapshot-ului; sql-live-reconciled-reactor.log
  arată clasele Kit incrementale vechi, recompilate după refresh fără schimbarea codului.

H2 probează adaptorul și oraclele pe schema fixture ATF. Nu certifică schema,
dialectul, tranzacțiile aplicației, CDC, Kafka sau deduplicarea business;
criteriile de acceptanță LIVE rămân deschise în feedback.

## Istoricul terminal al convergenței — verificare J2 pe J8

Snapshot-ul inițial are 344 de intrări live și baza Git
b5ea7e22d4fbad7deac5d7b8a651de1a2f7dd16c. Incrementul deține numai
convergența Eventuate, probele consumer și documentația; Core C1 și Reporting
J8 rămân identice, cu șapte module și preview implicit 240.

| Probă în staging propriu | Rezultat și limită |
|---|---|
| reproducere inițială | PENDING, două citiri, eșec după 10 ms; aceeași excepție, fără Result sau metadata terminală |
| regresii native Eventuate | 48 invocări: 15 convergență, 19 identitate/overflow, 11 terminal HTTP și trei componente prin suita canonică; fără eșecuri, erori sau skip |
| guvernanță | 166 controale verzi; nicio regulă, selecție canonică sau assertion existent nu este relaxat |
| documentație finală | 14 controale Git/documentare verzi și opt documente aprobate validate; densitatea prozei este corectată după refuzul inițial, fără schimbarea regulii |
| matrice nouă | 24 HTML-uri, 48 invocări și 16 failure-uri controlate; individual/suită, Extent/fallback, cu excepție identică și o singură creare |
| fapte publicate | PENDING păstrat, expected independent 42/APPROVED, buget 25 ms, interval 10 ms, elapsed și zero observații explicite |
| oracole negative | snapshot, fază, expected, istoric și coloana ACTUAL corupte sunt refuzate; parola sintetică din cauză lipsește din HTML |
| regresii adiacente | matricea existentă de 14 rapoarte și cele 36 de terminal HTTP trec pe aceleași clase și binare; 74 de rapoarte în total în acest increment |
| audit TAB MCP | patru rapoarte: read failure Extent/fallback în suită, expected-read PASS și expected-expired cu istoric gol; snapshot și JSON expandabile, Enter/Space, assertions cu patru coloane |
| navigare FAIL | Fails duce exact la cauza originală din Tests; celelalte două componente rămân PASS, iar setup/teardown și comparațiile trecute nu preiau verdictul erorii |

- Dovezi TMP: incrementul atf-j2-convergence-8d493b3ab6, baseline-failure.json,
  convergence-reactor.log, convergence-matrix-reviewed/acceptance.json,
  adjacent-report-acceptance/acceptance.json, adjacent-terminal-acceptance/acceptance.json
  și browser-audit.json. Primul refuz al oracolului, pentru etichetele FIELD uppercase,
  rămâne în convergence-acceptance.log; oracolul corectat păstrează valorile exacte.
- convergence-final-document-gates.log păstrează refuzul inițial de densitate;
  convergence-final-document-gates-reviewed.log păstrează cele 14 controale verzi.
- [Contractul și diagrama](../atf-test-eventuate/README.md#istoricul-convergenței--snapshot-terminal)
  delimitează execuția, collectorul și proiecția; [acceptanța reproductibilă](ATF_Eventuate_Testing.md#convergență--acceptanța-istoricului-terminal)
  folosește API-ul J8, fără surse Reporting copiate.
- Clock defect, Error, suppression dezactivat și reutilizarea awaiter-ului au probe
  unitare; nu sunt revendicate drept cazuri vizuale suplimentare. Citirea LIVE
  bounded și acceptanța saga rămân deschise; nu a rulat infrastructură în acest increment.
- La data incrementului de convergență, SQL embedded și primitiva Mock comună erau
  planuri. SQL embedded este livrat ulterior în secțiunea de la începutul paginii;
  extracția Mock rămâne deschisă în feedback.
## Revizie anterioară pe Reporting J8 — contracte și convergență

Snapshot-ul este HEAD b5ea7e22d4fbad7deac5d7b8a651de1a2f7dd16c, cu 344 de
intrări live capturate. Revizia schimbă numai documente; sursele Java,
testele, POM-urile și perechile Core C1/Reporting J8 rămân neschimbate.

| Probă în staging propriu | Rezultat și limită |
|---|---|
| reactor focalizat | 80 Mock, trei componente Eventuate prin suită și 166 controale de guvernanță; zero eșecuri, erori sau skip |
| matrice Mock pe J8 | 14 artifacte noi, individuale/combinate, Extent/fallback; oracol per rând și mutanți verzi |
| inspector generic | șapte Extent și șapte fallback neconforme; pe Extent lipsește legitim request response evidence split în profilul assertion-only |
| browser | patru artifacte: expected/response refuzate și două combinate; patru coloane, ultima comparație, Enter/Space și backlink exact cu vecini PASS colapsați |
| raport JUnit al suitei | trei clase PASS în același HTML; componenta API are tabele colapsabile în SETUP, TEST BODY și TEARDOWN; textul lung și ultimul rând sunt accesibile |
| timing observat pe J8 | TEST BODY 141 ms și copil GET 215 ms; dovadă suplimentară pentru criteriul 83, fără latență controlată sau închidere a semanticii timpului |
| guvernanță după editarea documentelor | 16 controale focalizate trec; README-ul din src/test are control documentar explicit, fără schimbarea regulilor sau a surselor Java |
| convergență la read failure | PENDING, pauză virtuală 10 ms, a doua citire aruncă; aceeași excepție, două citiri, niciun Result și zero evidence suppressed |
| convergență fără observații | deadline expirat înaintea primei citiri, zero citiri și observații; last aruncă NoSuchElementException |
| consolidare Validation | un singur contract JSON normativ; vechile ancore rămân navigabile, iar BigDecimal, Unicode, strict/warning și subsetul refuzat sunt păstrate |

- Dovezi TMP: incrementul atf-j2-review-0639018884, live-review-baseline.log,
  j8-mock-matrix/acceptance.json, convergence-failure.json și browser-audit.json.
- ConvergenceFailureProbe compilează cele trei surse publice relevante din
  snapshot; este o probă locală de investigație, fără broker și fără publicare JUnit.
- Snapshot-ul terminal HTTP este livrat; [istoricul terminal al convergenței](../atf-test-eventuate/README.md#istoricul-convergenței--snapshot-terminal)
  era un criteriu nou și distinct, remediat ulterior prin incrementul descris la începutul paginii.

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
Eventuate și ATF Tests. Core C1 și Reporting J8 sunt cele două biblioteci
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

## SQL generic și Eventuate LIVE — 2026-11-01

[Registrul incrementului](ATF_SQL_Live_Verification.md) fixează dovezile pe Core C2/Reporting J10, cele opt module, probele SQL individual/prin suită și infrastructura Kafka/MySQL. Aplicația cu CDC/saga rămâne NOT RUN; istoricul anterior nu este rebazat.
