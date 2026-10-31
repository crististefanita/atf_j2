# Atlas Setup — Podman, Kafka și probe · WIP

## Pe scurt

Setup deține infrastructura locală; Eventuate deține oracolele aplicației.
Java/Podman sunt prerequisites. Un PASS MOCK sau prezența CLI-ului nu
demonstrează engine ori broker funcțional.

| Schimbare | Citește întâi | Invariant |
|---|---|---|
| instalare/elevare | [prerequisites](../atf-setup/prerequisites/README.md) | o sesiune elevată; fără UI sau restart automat |
| imagine/listeners | [config](../atf-setup/config/local.json), [Kafka](../atf-setup/scripts/support/Kafka.psm1) | digest, loopback, listener intern distinct |
| procese/dovezi | [AtfSetup](../atf-setup/scripts/support/AtfSetup.psm1) | argumente literale, timeout și exit code în TMP |
| provisioning | [Start-Kafka](../atf-setup/scripts/Start-Kafka.ps1) | resurse străine ori stale refuzate |
| probă reală | [Test-Kafka](../atf-setup/scripts/Test-Kafka.ps1) | mesaj/topic/grup unic și poziție observată |
| orchestrare silent | [Setup](../atf-setup/Setup.ps1) | oprește la prima eroare și păstrează stadiile în receipt |
| corelare boot | [Host](../atf-setup/scripts/support/Host.psm1) | RecordId după markerul System; fallback CIM declarat |
| E2E infrastructură | [două runde](../atf-setup/tests/Test-SetupE2e.ps1), [acceptanță](../atf-setup/scripts/support/SetupAcceptance.psm1) | aceeași resursă, dovezi noi; fără PASS din receipts replayed |

## Stare observată după restart

Observațiile fixează snapshot-ul postrestart; scripturile revalidează hostul
și engine-ul la fiecare execuție. Acceptanța anterioară rămâne separată în
[registrul verificărilor](ATF_J2_Verification.md).

La 2026-10-31, boot-ul de la 14:23:22, ora București, confirmă noul restart;
componentRestartPending este false. Setup a fost reluat silent din TMP cu
aceeași rețetă Kafka; importul WSL2 a eșuat din nou, înainte de broker.

| Strat | Dovadă nouă | Verdict |
|---|---|---|
| prerequisites | Podman 5.8.3, WSL 2.7.14, VirtualMachinePlatform activ | instalate; restartul anterior este rezolvat |
| diagnostic WSL | version și status exit 0; versiune implicită 2 | nu dovedește pornirea unei distribuții |
| Podman machine init | exit 125; importul WSL2 eșuează cu HCS_E_HYPERV_NOT_INSTALLED | FAIL nativ, înainte de engine/Kafka |
| boot Windows | Hyper-V-Hypervisor Event 41: VMX absent sau neactivat | lansarea hypervisorului guest a eșuat |
| provisioning | setup.json FAIL, etapa Initialize-Podman FAIL | Start-Kafka și Test-Kafka nu sunt declarate executate |
| Eventuate | cele trei componente păstrează proveniența MOCK | fără acceptanță saga ori broker real |

HypervisorPresent poate fi true concomitent cu indicatori CPU false; scriptul
păstrează aceste fapte și cere podman info reușit. [Explicația Microsoft](https://devblogs.microsoft.com/scripting/use-powershell-to-detect-if-hypervisor-is-present/)
justifică separarea indicatorului WMI de proba funcțională, nu un PASS implicit.

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

## Reluare și limita gazdei

Get-SetupStatus colectează boot-ul, restart pending, componentele Windows,
evenimentele hypervisorului, WSL și conexiunile Podman fără elevare interactivă.
Setup păstrează etapa eșuată în receipt și nu continuă către Kafka.

Remedierea expunerii VT-x/AMD-V aparține gazdei VirtualBox. Colectorul
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
- După corectarea gazdei, reluați Test-SetupE2e fără instalare de prerequisites,
  cu un director nou de dovezi; două runde cer engine, mesaje noi și aceeași resursă.

Contractul operării este în [README](../atf-setup/README.md);
criteriile neînchise sunt în [feedback](feedback/ATF_Setup_Feedback.md).
