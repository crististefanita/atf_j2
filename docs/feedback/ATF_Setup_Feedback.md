# Setup — acceptanța infrastructurii reale

## Pe scurt

Kafka remote și clientul Java din VM sunt validate individual și în două
runde complete. Contractul stabil este în [Setup](../../atf-setup/README.md),
iar probele și eșecurile inițiale sunt în [registrul verificărilor](../ATF_J2_Verification.md#kafka-remote--broker-și-client-extern-validate).
Backlog-ul păstrează durabilitatea și ownership-ul gazdei, fără a redeschide brokerul demonstrat.

## SETUP-PRIMARY-FAILURE-87 — prioritate 84

Contraexemplul nativ refuză mai întâi listenerul așteptat greșit, fără topic
creat. Dacă destinația receipt-ului este apoi nescriibilă, finally aruncă
AccessDeniedException și pierde diagnosticul original Advertised listener differs.

- Păstrați cauza inițială și atașați eșecul publicării ca secundar; stderr trebuie să rămână util chiar când fișierul nu poate fi creat.
- Separați execuția probei de serializarea/salvarea rezultatului, fără transformarea eșecului inițial în succes sau retry de mutații.
- Probați failure simplu, output indisponibil și ambele simultan; verificați și propagarea prin wrapperul PowerShell.
- Includeți cleanup-ul Admin/Producer/Consumer în protocolul cauzei primare. Cele trei close în finally sunt o cale statică de risc; refuzul simultan execuție/close cere probă pură separată, fără a fi prezentat drept eșec Kafka observat.

Închidere: exit code rămâne FAIL, iar ambele cauze sunt observabile în ordinea
corectă, inclusiv când niciun receipt nou nu poate fi scris.

## SETUP-REMOTE-TRANSPORT-84 — prioritate 80

API-ul funcțional folosește TCP fără TLS/autentificare. Limitarea firewall la
host-only este declarată de operator. Scriptul și launcherul pentru bridge-ul
Kafka există, însă serviciul API și forwarding-ul 8888 inițial nu au încă o
rețetă completă verificată după restart.

- [ ] Capturați serviciul API și forwarding-ul său ca rețetă explicită, idempotentă, cu ownership și receipts; fără oprirea serviciilor altui proces.
- [ ] Reconciliați sursa exactă aplicată de operator pe fizic cu rețeta J2; guest-ul a verificat accesul, fără să recitească metadatele firewall/portproxy. Configurația externă nu se adoptă automat.
- [ ] Alegeți transportul durabil SSH sau mTLS înainte de lărgirea accesului; păstrați ruta actuală ca opt-in de laborator, fără relaxarea automată a firewall-ului.
- [ ] Verificați reluarea după restart, refuzul endpoint-ului/conexiunii străine și comportamentul la serviciu inaccesibil, fără fallback spre alt engine.

Închidere: ruta autorizată este reproductibilă după restart; o țintă greșită
ori lipsa autentificării cerute produce FAIL înainte de operații asupra resurselor.
[Atlasul Setup](../ATF_Setup_Atlas.md#stare-curentă--engine-remote-validat) delimitează dovada actuală.

## SETUP-PROBE-COVERAGE-85 — prioritate 78

KafkaExternalProbe.java este un utilitar în afara source-set-urilor Maven;
compilarea și verificarea lui sunt în proba nativă explicită. Contractele
offline Setup parsează PowerShell, iar componentele verifică în principal
receipts/profiluri; nu reprezintă toate ramurile clientului Java.

- Delimitați metadata, confirmarea livrării și serializarea receipt-ului prin contracte pure, probate fără broker; nu mutați tool-ul într-un modul nou.
- Acoperiți host/port greșite, lipsa ack-ului, identitate/poziție diferită și eroarea inițială plus eșecul scrierii dovezii.
- Declarați inventarul și comanda offline a utilitarului; un gate Maven verde nu trebuie prezentat ca verificare automată a acestui source-set separat.

Închidere: probele pure și compilarea pe dependențele pin-uite refuză
contraexemplele, iar acceptanța nativă păstrează verdictul distinct.

## SETUP-STAGE-RUNNER-86 — prioritate 76

Cele trei entrypoint-uri Setup repetă bucla de etape, logurile și salvarea
receipt-ului, cu variante diferite pentru etapele NOT_RUN. Acest control-flow
poate deriva chiar dacă provisioning-ul Kafka este deja comun.

- Extrageți un runner PowerShell pentru planuri explicite de etape, fără alegere automată de engine ori instalări ascunse.
- Păstrați în entrypoint configurația și selecția etapelor; păstrați în ownerii actuali instalarea, Kafka și clientul Java.
- Probați prima etapă eșuată, etapele următoare NOT_RUN și păstrarea cauzei când salvarea receipt-ului eșuează.

Închidere: local, remote Podman și remote Kafka folosesc același protocol de
orchestrare, cu regresii offline și aceeași acceptanță reală, fără timeout mărit.

## Delimitare după laboratorul Eventuate

- Noul helper Write-AtfEventuateReceipt păstrează cauza și eroarea secundară, cu probă offline simultană; rețeta și proba mică sunt [documentate](../../atf-setup/README_Eventuate.md). Aceasta nu repară încă utilitarul vechi KafkaExternalProbe și nu închide SETUP-PRIMARY-FAILURE-87.
- Reutilizați protocolul demonstrat la acel owner și probați și close-urile Java. Gate-ul de memorie și comparația broker ID/StartedAt sunt implementate; nu le mai listați drept provisioning lipsă.

### SETUP-PRIMARY-FAILURE-87 — offline reconfirmation

- The compiled current KafkaExternalProbe receives an invalid bootstrap address and a controlled receipt destination that is an existing directory.
- With a valid fresh destination the receipt records KafkaException / FAIL and topicCreated=false.
- With the directory destination the process exposes only AccessDeniedException from Files.writeString.
- No broker/topic/producer was executed.
- This confirms primary-cause loss without changing infrastructure.

Keep the Kafka/configuration failure primary and attach receipt/close errors
as secondary diagnostics. Extend the already owned failure-preserving receipt
pattern instead of introducing a second setup framework. Preserve nonzero exit
and prove both failure orders; the passing Eventuate receipt helper does not
close this older Java probe criterion.
