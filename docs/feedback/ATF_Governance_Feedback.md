# ATF Tests — inventarul fidel al surselor compilate

## Pe scurt

Inventarul Maven este comun guard-urilor de nivel și lizibilitate. Criteriul
de mai jos separă output-ul generat de un nume legal de package în sursa declarată.

## GOV-SOURCE-EXCLUSIONS-72 — output versus sursă declarată

**Prioritate: 70/100** = impact 27 + lacună 20 + lucrabil 17 + documentare 6.
**Owner:** ATF Tests, suportul inventarului. **Nivel:** componentă filesystem și guard-uri negative.

Proba creează un POM standard și patru clase compilabile. Inventarul întoarce
numai două: omite clasele din package-urile `legal.target` și `legal.artifacts`,
deși acestea sunt sub rădăcinile main/test declarate de convenția Maven.

| Sursă | Observație |
|---|---|
| [AtfJavaSourceInventory](../../atf-tests/src/test/java/atf/api/tests/governance/layers/AtfJavaSourceInventory.java) | excluderea se aplică fiecărui segment, inclusiv în sursa Java |
| [oracolul existent](../../atf-tests/src/test/java/atf/api/tests/governance/AtfSourceReadabilityGovernanceTest.java) | exclude explicit și `shop/src/main/target/Generated.java`; problema cere reconcilierea criteriului, nu o ștergere ad-hoc a probei |
| parcurgerea root-ului | `Files.walk` intră și în output-urile eliminate ulterior de filtru; observație statică de eficiență, fără SLA nou |

- [ ] Delimitați rădăcinile de output de package-urile compilabile; păstrați ambele layout-uri Shop, default-urile Maven, overlap-ul și ordinea deterministă.
- [ ] Probați că o încălcare reală din package-ul legal `target` sau `artifacts` este detectată de guard-ul afectat, iar output-ul generat rămâne exclus.
- [ ] Reconciliați explicit oracolul vechi prin controlul testChanges potrivit; nu reduceți selecțiile, pragurile ori controalele source/binary pentru verde.
- [ ] Evitați traversarea output-urilor cunoscute înainte de filtrarea fișierelor; orice reutilizare de snapshot rămâne scoped investigației, fără cache global stale între fixture-uri.

**Închidere:** setul surselor compilabile și setul inspectat coincid în fixture-ul
nominal, iar mutanții sunt refuzați în ambele deployment-uri. XML nesigur,
rădăcinile ieșite din modul și expresiile nerezolvate rămân refuzate.


## GOV-WORKSPACE-TARGETS-77 — adoptare J0 și acceptanță per destinație

**Prioritate: 67/100** = impact 26 + lacună 19 + lucrabil 16 + documentare 6.
**Owner:** integrarea workspace-ului; Reporting deține candidatul, fiecare consumer își deține adoptarea.
**Nivel:** snapshot, bootstrap și acceptanță source/binary.

Referința activă este J0, iar distribuția minimă are contract comun reconciliat.
La 2026-10-31, J2 este gol; J1 are un increment Core security absent din J0.
Aceste stări nu sunt certificări de sincronizare și nu cer un mirror general.

| Restanță observată | Criteriu de închidere |
|---|---|
| J2 fără proiect sau bootstrap | declarați catalogul/configurația reală, inițializați consumerul și verificați propria bază, Maven/IDE, CodeSource, guvernanța și adaptoarele sale |
| Core security numai în J1 | reconciliați selectiv cele patru implementări, testul RedactionStrategiesTest, README-urile și controlul aferent; păstrați feedback-ul specific fiecărui deployment până la proba codului sincronizat |
| promovare către două ținte | păstrați o stare și o dovadă per destinație; un conflict nu autorizează suprascrierea ei sau declararea întregii distribuții ca livrată |

- [ ] Inițializați și acceptați J2 dintr-un snapshot declarat; nu transportați implicit configurația locală J1, sursa Reporting ori harness-ul în live.
- [ ] Verificați incrementul Core înaintea propagării în J0; nu închideți defectul măștii literale din J0 numai pentru că J1 l-a corectat.
- [ ] La următoarea schimbare Reporting, oferiți același JAR/POM ambelor ținte inițializate și legați verdictul fiecăreia de hash-ul candidatului.

**Închidere:** fiecare țintă declarată are adoptare demonstrată ori stare explicită
nefinalizată, iar diferențele comune sunt reconciliate. Protocolul stabil este
[sincronizarea workspace-ului](../ATF_Workspace_Synchronization.md); automatizarea
release-ului rămâne la [Reporting](ATF_Reporting_Feedback.md#report-release-contract-45--ce-conține-și-ce-promite-pachetul).
