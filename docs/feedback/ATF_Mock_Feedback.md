# Mock — precizie și reutilizarea probelor de eșec

## Pe scurt

[Contractul fixture-urilor](../../atf-mock/README.md#document-json-complet--expected-request-și-response)
refuză documente incomplete, duplicate și constrângeri YAML coerced la gol.
Regresia și acceptanța pe JAR rămân permanente; criteriul închis este retras.

## MOCK-FAULT-SCRIPT-111 — prioritate 78

TerminalFailureFixtures și ConvergenceFailureFixtures repetă compoziția
transportului finit, a erorilor și a numărării dispatch-urilor. Există acum
două utilizări concrete de comparat înaintea unei extracții.

- Evaluați în atf-mock/main un plan imuabil de rezultate complete/fail/cancel/pending, cu stare locală invocării; păstrați adaptoarele JUnit și selecțiile probelor în test.
- Construiți o excepție nouă per execuție; reutilizarea instanței poate transporta suppressed metadata din altă invocare. Distingeți tentativele expediate de rutele potrivite și de răspunsurile încheiate.
- Păstrați matching-ul JSON, metoda, MIME, autentificarea, query-ul, wildcard-ul și ordinea strictă; epuizarea nu produce răspuns implicit verde.
- Nu extrageți politica OrderContract, saga sau witness-ul SQL în Mock/Kit. H2 va proba adaptorul SQL în Eventuate; un transport scriptat nu certifică efecte persistente.

Închidere: regresie componentă publică în Mock și aceleași două matrice
consumer pe noua primitivă, inclusiv eșecul sink-ului și cauza originală.
Extracția trebuie să reducă duplicarea fără alt modul ori modificări Reporting;
în incrementul curent nu este implementată.
