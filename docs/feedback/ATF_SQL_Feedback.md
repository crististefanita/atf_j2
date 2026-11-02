# SQL — criterii rămase după primul modul generic

## Pe scurt

[atf-sql](../../atf-sql/README.md) deține acum query/binds typed, captură bounded,
rezultate imuabile și cauza originală. H2 și proba MySQL reală au dovezi distincte
în [registrul incrementului](../ATF_SQL_Live_Verification.md). Nu redeschideți
aceste livrabile ca implementare lipsă și nu prezentați pasul SQL Reporting drept
livrat: rendererul și testele lui native rămân în J0.

## SQL-APPLICATION-WITNESS-118 — prioritate 85

Mapperul LIVE Eventuate folosește noul executor, dar proiectează încă o parte
din dovezi în `LiveSqlSnapshot`; schema sa nu a fost confirmată pe aplicația
fixată. Witness-ul embedded anterior și snapshot-ul generic au contracte diferite.

- Validați cele două SELECT-uri pe schema reală, migrațiile și grant-urile,
  înainte de a deduce efectul comenzii din numele unui tabel sau câmp.
- Păstrați snapshot-urile generice, ID-urile de execuție și outcome-urile în
  proiecția de domeniu; nu pierdeți parametrii typed, rândurile anterioare ori
  cleanup-ul la eșecul celui de-al doilea SELECT sau al mapperului.
- Migrați readerul fixture-ului embedded către primitive comune numai cu
  contractele sale existente păstrate; nu mutați schema business în SQL generic.
- Verificați bugetul comun pe două conexiuni, eșecul lecturii/mapping-ului și
  publicarea refuzată. Aceeași cauză și același snapshot trebuie să rămână
  observabile, fără SELECT repetat pentru raport.

Închidere: mapperul și faptele brute sunt corelate independent în componente și
în cele trei cazuri Eventuate reale individual/prin suită. H2 sau proba
MySQL sintetică nu închid criteriul schemei aplicației.

## SQL-DRIVER-CONTRACT-119 — prioritate 77

Primul increment este demonstrat pe H2 și MySQL, nu pe orice driver. Cancel,
fetch size, streaming LOB și read-only au semantici specifice. La eșec în mijlocul
unui rând se păstrează numai rândurile integral capturate anterior.

- Extindeți matricea numai la driverele adoptate de un consumer real: conectare
  lentă, ResultSet blocat, abort nesuportat, închidere refuzată și bufferizare.
- Verificați explicit diferența dintre deadline-ul clientului, resurse închise
  confirmat și anularea pe server; nu afirmați cancel garantat pe baza unui timeout.
- Dacă este necesară captura celulelor unui rând eșuat, introduceți un model
  explicit de rând parțial și regresii; nu relaxați invariantul actual al lățimii.

Închidere: fiecare driver declarat suportat are negative finite și proveniență
reală; memoria driverului nu este confundată cu bugetul payload-ului capturat.

## Reporting — handoff, fără implementare J2

[REPORT-SQL-STEPS-116](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile)
și [componentele dedicate](../../../atf_j0/docs/feedback/ATF_Reporting_Tests_Feedback.md#report-test-sql-steps-116--probe-dedicate-sql)
cer SQL semantic cu query/results configurabile separat. J2 furnizează acum
faptele typed și matricea consumer; J0 păstrează API-ul neutru și ambele renderere.

## SQL-DIAGNOSTIC-CAPTURE-120 — prioritate 82

Bugetul agregat acoperă query, binds, metadata și celule. SqlSnapshot.Failure
capturează încă mesajul Throwable și lista suppressed în afara acestui buget;
un diagnostic foarte mare poate amplifica memoria înaintea limitei HTML.

- Introduceți o politică explicită: prefix bounded, număr maxim de cauze
  secundare și indicatori de omisiune, fără a altera Throwable-ul original.
- Păstrați aceeași excepție propagată și diagnosticul complet accesibil ownerului;
  proiecția bounded nu justifică înlocuirea cauzei sau reexecutarea SQL.
- Probați mesaj mare, multe suppressed, cleanup și sink eșuate împreună;
  metadata omisiunii trebuie să rămână vizibilă în snapshot și raport.

Închidere: diagnosticul capturat respectă politica, originalul rămâne identic,
iar limitele JDBC, cele HTML și preview-ul assertions 240 rămân distincte.

### SQL-DIAGNOSTIC-CAPTURE-120 — current execution counterexample

On the current compiled SQL source, a capture budget of 256 units still
publishes a 1,048,576-character primary message plus 64 secondary descriptions
containing 525,760 characters. The original SQLException identity and one
connection attempt are preserved. This reconfirms the existing criterion;
it is not a new duplicate TODO or a claim of bounded driver allocation.

Bound the diagnostic projection independently, including secondary count and
omitted-length metadata. Never shorten or replace the original Throwable.
Proof: sql-diagnostic-probe.log in j2-quality-64e05d574c24 under TMP.

## SQL-HARNESS-DIAGNOSTICS-122 — preserve the failing JUnit cause

**Priority: 79/100** = impact 30 + gap 22 + actionable 19 + documentation 8.
**Owner:** SQL acceptance support in test scope; neutral support reuse has
[Kit handoff](../../../atf_j0/docs/feedback/ATF_Common_Test_Support_Feedback.md).

- The first native SQL matrix stops with Native SQL JUnit verdict differs and an opaque MutableTestExecutionSummary$DefaultFailure object identity.
- Its HTML correctly retains SQLTimeoutException; the launcher error alone loses the useful cause.
- A separate isolated matrix passes all 12 reports / 36 invocations with four intended failures.
- The first failure remains evidence; this does not establish stable performance or justify increasing the budget.

- [ ] Print and persist the selected case, original Throwable/cause/suppressed,
  expected versus observed JUnit/container counts and the exact artifact path
  before terminating the matrix. Persist a partial receipt even on early exit.
- [ ] Prove setup/container failure, timeout, wrong exception type and artifact
  publication failure. Keep the original failed invocation visible.
- [ ] Reuse neutral scope/diagnostic mechanics only after the concrete cases
  are covered; SQL expectations and artifact oracles stay with SQL tests.
