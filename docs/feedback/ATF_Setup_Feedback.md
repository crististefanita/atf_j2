# Setup — acceptanța infrastructurii reale

## Pe scurt

Kafka remote și clientul Java din VM sunt validate individual și în două
runde complete. Contractul stabil este în [Setup](../../atf-setup/README.md),
iar probele și eșecurile inițiale sunt în [registrul verificărilor](../ATF_J2_Verification.md#kafka-remote--broker-și-client-extern-validate).
Backlog-ul păstrează durabilitatea și ownership-ul gazdei, fără a redeschide brokerul demonstrat.

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
