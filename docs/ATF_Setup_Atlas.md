# Atlas Setup — Podman, Kafka și probe · WIP

## Pe scurt

Setup deține infrastructura locală/remote; Eventuate deține oracolele aplicației.
Java/Podman sunt prerequisites. Un PASS MOCK sau prezența CLI-ului nu
demonstrează engine ori broker funcțional.

| Schimbare | Citește întâi | Invariant |
|---|---|---|
| instalare/elevare | [prerequisites](../atf-setup/prerequisites/README.md) | o sesiune elevată; fără UI sau restart automat |
| engine remote | [Setup-RemotePodman](../atf-setup/Setup-RemotePodman.ps1), [profil](../atf-setup/config/remote-podman.json) | endpoint host-only explicit, rootless verificat; fără presupuneri despre WSL guest |
| conexiune și smoke | [RemotePodman](../atf-setup/scripts/support/RemotePodman.psm1), [probă](../atf-setup/scripts/Test-RemotePodman.ps1) | refuză conexiune străină; digest, exit code și eliminarea containerului verificate |
| imagine/listeners | [config](../atf-setup/config/local.json), [Kafka](../atf-setup/scripts/support/Kafka.psm1) | digest, loopback, listener intern distinct |
| Kafka remote | [profil](../atf-setup/config/remote-kafka.json), [orchestrare](../atf-setup/Setup-RemoteKafka.ps1) | aceeași rețetă de provisioning; API și ruta Kafka sunt distincte |
| fizic–guest | [pași și diagnostic](../atf-setup/prerequisites/README_Remote_Host.md), [script fizic](../atf-setup/prerequisites/Configure-KafkaHostAccess.ps1) | elevare numai pe fizic; fără ștergerea API-ului Podman |
| client extern | [probă Java](../atf-setup/probes/java/KafkaExternalProbe.java), [orchestrare](../atf-setup/scripts/Test-KafkaExternal.ps1) | client în Windows, metadata înainte de publish, JAR-uri verificate în TMP |
| procese/dovezi | [AtfSetup](../atf-setup/scripts/support/AtfSetup.psm1) | argumente literale, timeout și exit code în TMP |
| provisioning | [Start-Kafka](../atf-setup/scripts/Start-Kafka.ps1) | resurse străine ori stale refuzate |
| probă reală | [Test-Kafka](../atf-setup/scripts/Test-Kafka.ps1) | mesaj/topic/grup unic și poziție observată |
| orchestrare silent | [Setup](../atf-setup/Setup.ps1) | oprește la prima eroare și păstrează stadiile în receipt |
| corelare boot | [Host](../atf-setup/scripts/support/Host.psm1) | RecordId după markerul System; fallback CIM declarat |
| E2E infrastructură | [două runde](../atf-setup/tests/Test-SetupE2e.ps1), [acceptanță](../atf-setup/scripts/support/SetupAcceptance.psm1) | aceeași resursă, dovezi noi; fără PASS din receipts replayed |

## Stare curentă — engine remote validat

La 2026-10-31, VM-ul 192.168.56.101 comunică prin rețeaua host-only cu
tcp://192.168.56.1:8888. Clientul Windows 5.8.3 folosește engine-ul Linux
amd64 5.7.0, rootless, pe gazdă. Probe native din TMP verifică engine-ul și
execuția reală; nu mai este necesară virtualizarea imbricată pentru această rută.

| Strat | Dovadă | Limită |
|---|---|---|
| client și API | versiuni fizice, podman info, conexiunea atf-j2-host | serverul este pe gazdă, nu în guest |
| container real | hello-world fixat la digest; run exit 0, output și absență după --rm | textul imaginii menționează Docker; engine-ul observat este Podman |
| repetare | două runde native PASS, probeId-uri distincte, aceeași imagine și conexiune reutilizată | nu certifică stabilitate pe multe repetări |
| refuz nativ | versiune server greșită → FAIL la inițializare; smoke neexecutat | fără upgrade sau overwrite implicit |
| componente | refuzuri de endpoint, conexiune și engine; JSON expected/actual | MOCK_REMOTE_PODMAN_COMPONENT, fără engine executat |
| Kafka real | CLI intern și Java Windows PASS, individual și două runde complete; aceeași resursă | fără certificarea restartului fizic ori a integrării Eventuate |

[README-ul Setup](../atf-setup/README.md#podman-remote-din-vm--traseul-validat)
deține comenzile și contractul. Scripturile Kafka comune acceptă profilul
remote explicit, fără fallback; 127.0.0.1 pe gazda engine-ului diferă de
loopback-ul VM-ului și cere forwarding fizic pentru accesul extern.

- Firewall-ul host-only este declarat de operator; guest-ul nu i-a inspectat regula.
- TCP-ul actual nu autentifică și nu criptează API-ul; profilul restrânge subnetul, fără a substitui firewall-ul sau TLS.
- [Feedback-ul](feedback/ATF_Setup_Feedback.md) păstrează numai gap-urile și dovada lipsă.

<details>
<summary><strong>Diagnosticul istoric al engine-ului local</strong></summary>

Boot-urile anterioare au păstrat Event 41, VMX absent/neactivat și eșecul
WSL2 HCS_E_HYPERV_NOT_INSTALLED la podman machine init, exit 125.
WSL status exit 0 și HypervisorPresent nu dovedeau un engine funcțional.
Traseul remote nu rescrie aceste rezultate și nu declară WSL2 în guest reparat.
Probe și limite anterioare rămân în [registrul verificărilor](ATF_J2_Verification.md).

</details>

## Kafka real — rute internă și externă validate

Profilul remote compune local.json cu remote-podman.json și adresa Kafka
192.168.56.1:19092. Brokerul 4.1.2 a fost pornit real din VM prin API-ul
gazdei; două mesaje independente au fost consumate în partiția 0, offset 0.

| Gate | Stare și dovadă |
|---|---|
| provisioning comun | două porniri reutilizează același container/image/spec; fără recreare ori timeout mărit |
| roundtrip intern | două probe cu topic/grup/probeId distincte; InternalOnly păstrează hostTcpReachable false |
| client Java Windows | PASS individual și în două runde; metadata 192.168.56.1:19092, ack și consum pe aceeași partiție/offset |
| orchestrare completă | două runde PASS, aceeași identitate de container/image/spec și patru probe independente |
| ruta guest | TCP 19092 accesibil și API 8888 OK după intervenția operatorului pe fizic |
| negativ nativ | listener așteptat greșit → FAIL înainte de topic/publish; producer către port închis → status nonzero |
| script fizic | sursă și launcher reutilizabile; funcționarea rutei este demonstrată, dar ownership-ul configurației aplicate extern nu este recitit de guest |

- [Configure-KafkaHostAccess](../atf-setup/prerequisites/Configure-KafkaHostAccess.ps1) configurează numai ruta Kafka și firewall-ul .1/.101; nu elimină 8888.
- [Export-KafkaHostAccess](../atf-setup/scripts/Export-KafkaHostAccess.ps1) produce fișier autonom și arhivă cu hash-uri; transferul din VM pe fizic este explicit.
- [Acceptanța comună](../atf-setup/scripts/support/SetupAcceptance.psm1) cere dovezi interne/Java externe pentru profilul remote; un PASS intern nu este substitut.
- Serviciul API și forwarding-ul său trebuie păstrate sau înlocuite prin transport validat înaintea reluării; un port TCP deschis nu demonstrează API funcțional.

Execuția inițială prin exec -i a eșuat la închiderea sesiunii remote, deși
inspecția ulterioară arăta proces terminat. Producerul folosește acum program
fix cu argumente literale, fără stdin remote; proba inițială nu a fost retrimisă.

[Registrul verificărilor](ATF_J2_Verification.md#kafka-remote--broker-și-client-extern-validate)
separă rezultatele executate de gate-urile neînchise.

## Ceasul VM-ului și corelarea boot-ului

În boot-ul anterior, de la 14:03:29, VBoxService a mutat ceasul înainte cu
129.619 ms; CIM a deplasat ulterior
LastBootUpTime, fără un nou boot. Filtrarea exclusiv după acea oră omitea
Event 41, deși înregistrarea rămânea în jurnalul System.

| Situație | Contract și probă |
|---|---|
| marker disponibil | ultimul Kernel-General 12 delimitează evenimentele hypervisorului prin RecordId, în același jurnal |
| marker inaccesibil | CIM_TIME_FALLBACK și diagnosticul original rămân explicite |
| regresie controlată | [componentele Setup](../atf-setup/tests/Test-SetupComponents.ps1): clock înainte/înapoi, boot anterior și acces refuzat |
| readiness | engineReady cere în continuare podman info reușit; diagnosticul nu produce PASS implicit |

## Diagnostic local și limita gazdei

Get-SetupStatus folosește profilul local și colectează boot-ul, restart pending, componentele Windows,
evenimentele hypervisorului, WSL și conexiunile Podman fără elevare interactivă.
Setup păstrează etapa eșuată în receipt și nu continuă către Kafka.

Pentru varianta locală WSL2, remedierea expunerii VT-x/AMD-V aparține gazdei VirtualBox. Colectorul
[read-only](../atf-setup/prerequisites/Get-VirtualBoxHostStatus.ps1) rulează
acolo dintr-o copie accesibilă a modulului Setup, cu numele sau UUID-ul real:

```powershell
./atf-setup/prerequisites/Get-VirtualBoxHostStatus.ps1 -Vm '<VM-name>'
```

[Oracle](https://docs.oracle.com/en/virtualization/virtualbox/7.2/user/working-with-vms.html)
descrie opțiunea nested hardware virtualization;
[Microsoft](https://learn.microsoft.com/en-us/windows/wsl/troubleshooting)
delimitează eroarea WSL2. Colectorul nu oprește VM-ul și nu modifică setări.

- Colectorul păstrează versiunea, configurația efectivă și liniile HM, NEM,
  VMX, VT-x și nested; logurile native rămân în TMP pe gazdă.
- Configurația efectivă și backend-ul gazdei aleg următoarea remediere;
  guest-ul nu le poate certifica.
- Numai pentru ruta locală, după corectarea gazdei, reluați Test-SetupE2e fără instalare de prerequisites,
  cu un director nou de dovezi; două runde cer engine, mesaje noi și aceeași resursă.

Contractul operării este în [README](../atf-setup/README.md);
criteriile neînchise sunt în [feedback](feedback/ATF_Setup_Feedback.md).
