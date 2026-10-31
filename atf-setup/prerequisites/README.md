# Prerequisites — host ATF

## Pe scurt

Contractul aparține infrastructurii J2 WIP. Versiunile cerute sunt citite din [configurația Setup](../config/local.json); instalarea și starea engine-ului se verifică separat.

Java 21 și Maven servesc build-ul ATF; PowerShell 7.4+ și Podman servesc scripturile Setup. Rețetele Kafka
sunt la [ownerul Setup](../README.md), fără instalare ascunsă în testele ATF.

| Prerequisite | Verificare |
|---|---|
| Java 21 / Maven | java --version, mvn --version; toolchain ATF separat de Java din imaginea Kafka |
| Podman 5.8.3 | [Install-Podman](Install-Podman.ps1), winget oficial și versiune fizică verificată |
| WSL 2.7.14 | [Enable-Wsl](Enable-Wsl.ps1), package Microsoft și VirtualMachinePlatform prin DISM /NoRestart |
| engine rootless | [Initialize-Podman](Initialize-Podman.ps1), mașina atf-j2 și podman info pe conexiunea explicită |
| gazdă VirtualBox | [colector read-only](Get-VirtualBoxHostStatus.ps1): versiune, configurație și VBox.log; rulează pe gazdă, separat de guest |

Instalarea prerequisites cere o sesiune PowerShell deja elevată. Niciun script
nu creează prompt UAC, nu schimbă execution policy, nu dezactivează TLS și
nu repornește hostul. Maven nu invocă acești pași.

- Executabilul instalat și wsl --status exit 0 nu dovedesc engine funcțional.
- Indicatorii CPU și HypervisorPresent sunt diagnostice; Initialize-Podman păstrează host-preflight.json și cere proba nativă podman info.
- [Get-SetupStatus](../scripts/Get-SetupStatus.ps1) păstrează și Event 41 de la boot; mesajul WSL1 nu certifică un defect WSL2.
- WSL2 cere virtualizare; într-o VM, hostul trebuie să expună virtualizare imbricată.
- Componentele Windows pot cere restart manual; păstrați cauza și reluați rețeta după schimbarea stării hostului.

Referințe: [Podman Windows](https://podman.io/docs/installation),
[WSL și virtualizare](https://learn.microsoft.com/en-us/windows/wsl/troubleshooting).
