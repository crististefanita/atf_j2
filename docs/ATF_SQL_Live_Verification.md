# SQL generic și Eventuate LIVE — dovada incrementului J2

## Pe scurt

Incrementul din 2026-11-01 adaugă `atf-sql` și porturile LIVE Eventuate, pe
snapshot-ul Git `3c013eb9b5940381340d46cbf137c046c44f8973`, reconciliat cu
livrarea paralelă Core C2/Reporting J10. Snapshotul verificat avea opt module,
inclusiv biblioteca SQL nouă. Catalogul curent este în [scopul J2](ATF_J2_Scope.md).

Toate build-urile, cache-urile, logurile și rapoartele sunt în staging-ul propriu:
`%TEMP%/atf-j2-eventuate-live-9fcc5a07ac`. Niciun output generat nu intră în live.

| Suprafață | Dovadă | Limită |
|---|---|---|
| SQL generic | 14 teste: 9 unit + 5 component, zero failure/skip | H2 și double JDBC; nu toate dialectele |
| porturi LIVE | 9 teste controlate: deadline, Kafka envelope/config, HTTP fără replay | nu sunt acceptanță a serviciilor reale |
| SQL artifact | 12 HTML-uri, 36 invocări, patru SQLException neconsumate controlate | observations JSON și assertions; fără pas SQL nativ |
| infrastructură reală | 14 comparații, un snapshot Kafka și două SQL; Extent/fallback | eveniment business sintetic, aplicația NOT RUN |
| Setup offline | 18 comparații, configurații invalide, memorie și receipt failure | fără apel real Podman în aceste componente |

## Execuții și probe negative

`frozen-foundation-tests.log` păstrează build-ul pe sursele SQL/LIVE înghețate:
23 invocări trecute. Selecțiile business existente și guvernanța sunt consemnate
separat în închiderea incrementului; nu sunt deduse din aceste 23.

- `sql-native-matrix-2/acceptance.json` păstrează fiecare selecție individuală și suita SQL în ambele renderere. Oracolul verifică template, binds typed, metadata cu labels duplicate, SQL NULL/gol/text, DECIMAL exact, rânduri duplicate, identitatea invocării și verdictul.
- Mutanții ID/query lipsă și număr rotunjit sunt refuzați. FAIL-ul neconsumat păstrează `SQLSyntaxErrorException`; un SQL error așteptat și verificat rămâne observație într-un test PASS.

- Inspectorul generic J10 rămâne neconform pentru acest profil legitim fără HTTP: șase Extent refuză `request response evidence split`, iar cele șase fallback primesc și cerințe ale shell-ului Extent. Rezultatele originale sunt salvate, fără request-uri fictive sau transformarea lor în PASS.
- J0 a livrat ulterior [profilul explicit de inspecție](../../atf_j0/atf-reporting/README.md#inspecția-artifactului-cu-intenție-explicită) pentru rapoarte fără HTTP. Rezultatul J10 de mai sus rămâne martorul original al incrementului.

## Kafka/MySQL reale, în același engine

`real-probe-6/eventuate-probe.json` și `results/receipt.json` înregistrează PASS,
CodeSource Core C2/Reporting J10, ambele artifactAudits PASS și aplicația `NOT_RUN`. Observerul capturează
end offsets înaintea producerului sintetic; confirmarea este verificată față
de partition, offset, message ID și aggregate ID. Topic-ul UUID al probei este
șters la final; topic-urile aplicației și grupurile business nu sunt atinse.

SQL folosește contul SELECT-only și o conexiune independentă. SELECT parametrizat
păstrează DECIMAL dincolo de precizia Double, un bind cu sintaxă SQL ca text
literal și NULL. `SELECT SLEEP(2)` este refuzat în bugetul de 100 ms, cu snapshot
FAILED și clasificare timeout; executorul nu repetă interogarea.

- Brokerul împrumutat este `atf-j2-kafka`, ID `2536abd91dce5e1a7d07cd5714cef8886e8684886eeec51902563fd059ff90c6`, StartedAt `2026-10-31T18:37:28.970838812+02:00`. MySQL owned are limită 384 MiB; proba Java 384 MiB/heap 160 MiB.
- Android-ul altui proces rămâne activ. Nu există restart/stop Podman sau broker pentru a obține memorie.

## Eșecuri păstrate și limitele concluziei

- `foundation-tests-3.log`: un prim SELECT H2 rece a depășit deadline-ul de două
  secunde sub presiunea memoriei. Rularea izolată și ulterior build-ul înghețat
  au trecut cu același cod și deadline, heap de test redus la 256 MiB. Nu este
  o demonstrație statistică de stabilitate pe orice host.
- `real-probe-1`: configurația Kafka Admin avea api timeout 5 s sub request
  timeout-ul implicit 30 s; constructorul a refuzat profilul înaintea traficului.
  Politica comună configurează explicit 3 s/5 s și are regresie pe configurația
  Kafka efectivă. Prima probă eșuată nu este rescrisă.
- Build-ul upstream fixat la `8632d030301c2a477141350df1e77f3f991f64e0`
  a produs JAR-urile customer/order. Nu au fost pornite serviciile/CDC și nu
  există verdict pe aprobarea/respingerea reală, schema aplicației sau CQRS.

Resursele actuale nu permit certificarea laboratorului complet fără a perturba
alte procese; utilizatorul a ales dezvoltarea SQL și probe locale suplimentare.
[EVT-LOCAL-01](feedback/ATF_Eventuate_Feedback.md#evt-local-01--prioritate-90)
păstrează readiness-ul HTTP/CDC, mapping-ul SQL și cazurile individual/prin suită.

## Contracte owner

[SQL](../atf-sql/README.md) · [LIVE atlas](../atf-test-eventuate/src/main/java/atf/eventuate/live/README.md)
· [Setup](../atf-setup/README_Eventuate.md) · [feedback J2](feedback/README.md).
Bugetele JDBC nu sunt bugete HTML; pașii SQL și limitele de afișare rămân
[cerință J0](../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile).

## Închidere verificată pe sursele promovate

- Gate-ul complet are 14 SQL + 20 Eventuate + 170 guvernanță, zero FAIL/ERROR/SKIP;
  logul final este final-consumer-governance-2.log. Prima rulare a refuzat importurile
  wildcard ale adnotării SQL, catalogul vechi și proza densă; probele și limitele nu au fost relaxate.
- Patru regresii suplimentare InfrastructureReportOracleTest verifică JSON trunchiat,
  identitate/verdict greșite, zecimale și offsets Kafka exacte, plus configurația explicită.
  report-audit-regressions-2.log păstrează nominalul și mutanții, iar gate-ul documentar separat trece.
- DB-only a fost pornit repetat prin script, fără înlocuire. real-probe-6 are 14 comparații,
  trei corpuri complete și două audituri artifact PASS, în PT5.576100906S execuție;
  publicarea și flush-ul sunt separate de acest timp.

### Audit browser în TAB MCP

| Suprafață | Observație verificată |
|---|---|
| SQL nativ individual/suită | snapshot-uri asociate invocării, query/binds și DECIMAL exact; nominale și SQLException controlată |
| disclosure | vecini deschiși simultan, Enter/Space, assertions standard și întoarcere Fails → Tests la cauza JDBC originală |
| scroll | JSON până la final prin Control+End; ultimul assertion accesibil în tabelul interior |
| panou îngust | patru coloane păstrate la circa 465 px; fără transformarea în coloană unică |
| lățime disponibilă | tabele FIELD/VALUE și assertions umplu content box-ul; scrollbar-ul propriu rămâne separat |
| infrastructură finală | 664/1.227/513 caractere JSON valide, Kafka/SQL complete, exact 14 comparații și ultimul rând No automatic SQL replay |
| Extent / fallback | PT5.576100906S capturat, afișare 5.577 s, aceleași fapte și verdict PASS; aplicația NOT RUN vizibilă |

### Probleme descoperite prin audit și păstrate în istoric

- Probele 2/4 au trecut verificările infrastructurii, însă runnerul fără YAML descoperea
  default-ul de 100 caractere și publica JSON trunchiat. Nu sunt dovada finală a randării.
- Exportul include acum config/atf.yaml în manifest, iar runnerul îl transmite explicit:
  assertions 240, corp generic 12.000. Acestea nu sunt bugetele SQL semantice cerute în J0.
- Proba 3 a expirat la createTopics înaintea capturilor; timeout-ul nu a fost mărit.
  Probe ulterioare au identități noi și păstrează toate rezultatele, fără replay business.
- Proba 5 a refuzat corect auditul ca etapă separată; comparația inițială a oracolului
  distingea greșit Long(0) de IntNode(0). Regresia normalizează token-uri JSON exacte,
  fără Double, și respinge în continuare schimbarea offset-ului mare sau a identității.

Acceptanța completă a aplicației rămâne în EVT-LOCAL-01. Nu deduceți stabilitate
statistică din aceste rulări și nu ștergeți logurile inițiale pentru a cosmetiza rezultatul.

## Gate final de pachet și read-only

- final-package-gate.log: 14 SQL, 24 Eventuate și 170 guvernanță, toate PASS,
  fără ERROR/SKIP. JAR-ul SQL este construit numai în target-ul staging-ului.
- jdeps confirmă numai java.base și java.sql pentru runtime-ul atf-sql;
  driverul, Kit, Reporting și JUnit nu intră în biblioteca main.
- Matricea SQL finală repetă 12 artifacte/36 invocări și patru FAIL-uri așteptate.
  Gate-ul refuză și ABORTED/SKIP ori populația root greșită, fără a modifica rendererul.
- Auditul final a deschis toate cele nouă corpuri SQL și cinci tabele assertions
  în fallback; cele trei tabele ale clasei failure Extent au patru coloane și lățimea content box-ului.
- SHOW GRANTS pentru atf_reader confirmă numai USAGE și SELECT pe eventuate.*;
  broker-final.txt și containers-final.txt păstrează identitatea brokerului și containerele active.
