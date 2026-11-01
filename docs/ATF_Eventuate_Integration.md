# Eventuate — contract, dovezi și extindere

## Pe scurt

**Design de integrare WIP.** J2 urmărește teste Kafka/mesagerie cu Setup în Podman; Eventuate este aplicația de referință a scaffold-ului actual, fără integrare Java Kafka ori saga reală certificată.

Aplicația de referință este [Customers and Orders](https://github.com/eventuate-tram/eventuate-tram-examples-customers-and-orders).
Snapshot-ul analizat este `8632d030301c2a477141350df1e77f3f991f64e0`;
implementarea ATF inițială folosea exclusiv fixture-uri mock. Incrementul [SQL embedded](../atf-test-eventuate/src/main/java/atf/eventuate/sql/README.md) verifică acum adaptorul JDBC pe schema fixture ATF, separat de aplicația upstream.

| Date | Contract |
|---|---|
| GET upstream | orderId, orderState, rejectionReason nullable |
| POST upstream | customerId și orderTotal |
| auditDescription | extensie exclusivă a fixture-ului, ignorată de clientul business; probează preview-ul 240 |

## Separarea contractelor

| Contract | Implementat în J2 | Acceptanță ulterioară |
|---|---|---|
| schema și identitatea răspunsului | componentă mock | API real cu identificatori proprii |
| retry GET și convergență | clock virtual și HTTP scriptat | deadline măsurat pe aplicație |
| dovezi duplicate și efecte | validator cu witness independent | replay și efecte SQL/Mongo observate |
| witness JDBC embedded | H2, tranzacții, identitate și failure-uri locale | SQL-ul aplicației, schema reală, CDC și corelarea cu Kafka |
| UI | exclus din reactorul J2; fără Playwright | inspecția manuală a portalului, Swagger și AsyncAPI nu este dependență de test |

## Laborator real

Construiește aplicația separat de Maven/JUnit ATF, cu wrapperul și toolchain-ul
upstream pin-uite. Snapshot-ul analizat folosește Java 17, Gradle, Kafka,
CDC, baze relaționale și Mongo pentru proiecția CQRS.

Observerul ATF de implementat trebuie să aibă grup Kafka propriu și să pornească înaintea mutației.
Pozițiile brokerului și citirile read-only trebuie corelate cu order ID și
message ID; nu modifica offset-urile grupului aplicației pentru inspecție.

## Cazuri de închidere

1. Aprobare, credit insuficient și client necunoscut, individual și prin suită.
2. Anulare și eliberarea creditului, cu efecte independente.
3. Același message ID livrat repetat, apoi același business event cu ID nou.
4. Replay vechi în CQRS, întrerupere CDC și comenzi concurente.

POST-ul upstream nu expune o cheie HTTP de idempotency în controllerul analizat.
Consumerul JDBC și proiecția Mongo au strategii de deduplicare diferite;
nu deduce o garanție universală dintr-un PASS al validatorului mock.

## Resurse și laborator

[Setup](../atf-setup/README.md) deține rețeta și probele brokerului;
[ghidul fizic–VM](../atf-setup/prerequisites/README_Remote_Host.md) explică
engine-ul remote și eliminarea dependenței de WSL2 imbricat în guest.

- Dimensionați serviciile, CDC și bazele pe gazda engine-ului; memoria liberă observată cândva în guest nu este un buget al laboratorului fizic.
- Istoricul restartului și al virtualizării rămâne în [registrul verificărilor](ATF_J2_Verification.md); nu este o precondiție actuală repetată în fiecare ghid.
- O revizie documentară nu pornește instalări ori servicii; măsurați starea prin probe explicite înainte de execuția LIVE.

| Etapă | Instrument și dovadă cerută |
|---|---|
| motor local | [Podman pe Windows](https://podman.io/docs/installation), machine WSL2 sau Hyper-V; `podman info` și `podman machine inspect`, nu numai existența executabilului |
| probă broker separată | imaginea oficială Kafka pin-uită; topic unic, producer și consumer cu grup propriu, identitate și timeout explicit |
| aplicație de referință | compose-ul upstream pin-uit, rețea și versiunile compatibile; readiness pentru CDC, SQL, Mongo și servicii |
| observer ATF | client Kafka și citiri read-only ale efectelor, pornite înaintea comenzii; corelare prin order/message ID |

- Un broker [Kafka standalone](https://kafka.apache.org/41/getting-started/docker/) și proba Java Setup verifică infrastructura; nu certifică adaptorul framework-ului ori saga.
- Păstrați compatibilitatea upstream; o imagine nouă cere probă distinctă.
- Laboratorul complet cere resurse suplimentare sau host dedicat; restartul rămâne manual.
