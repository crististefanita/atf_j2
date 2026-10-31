# Gazdă fizică și Windows VM — Podman remote și Kafka

## Pe scurt

Engine-ul Podman rulează în Linux/WSL2 pe Windows-ul fizic; VM-ul VirtualBox
folosește clientul Windows. Această rută evită dependența de WSL2 imbricat în
guest și separă API-ul Podman de conexiunea Kafka a aplicației.

| Loc | Responsabilitate | Adresă observată |
|---|---|---|
| Windows fizic | adaptor host-only, forwarding și firewall | 192.168.56.1 |
| Linux/WSL2 pe fizic | Podman 5.7.0 rootless și brokerul Kafka | Kafka publicat pe loopback-ul engine-ului, port 19092 |
| Windows VM | Podman 5.8.3, PowerShell 7.4+, Java 21 și probe | 192.168.56.101 |
| API Podman din guest | provisioning, inspect, exec și copierea dependențelor | tcp://192.168.56.1:8888 |
| Kafka din guest | metadata și producer/consumer Java | 192.168.56.1:19092 |

Ownerul comenzilor și acceptanței este [Setup](../README.md). Profilul
[remote-kafka.json](../config/remote-kafka.json) selectează adresele; exemplele
de mai jos descriu laboratorul observat, fără alegerea automată a altui engine.

## Ordinea setup-ului

1. Pe Windows-ul fizic, verificați WSL2 funcțional și distribuția Linux folosită de engine.
2. În acea distribuție, verificați Podman rootless și serviciul API accesibil de pe gazda Windows.
3. În VirtualBox, păstrați adaptorul NAT pentru internet și activați un adaptor host-only pentru comunicarea fizic–guest.
4. Din VM, verificați API-ul Podman și executați proba de container prin Setup-RemotePodman.
5. Din VM, porniți Kafka cu profilul remote; imaginea, rețeaua și volumul sunt gestionate de scripturile comune.
6. Pe Windows-ul fizic, aplicați forwarding-ul Kafka și firewall-ul din scriptul de mai jos, într-o sesiune deja elevată.
7. Din VM, verificați clientul Java extern și apoi două runde complete de Setup-RemoteKafka.

<details>
<summary><strong>Verificări pe fizic înainte de provisioning</strong></summary>

În PowerShell pe fizic:

```powershell
wsl --status
wsl --list --verbose
wsl --distribution '<distributia-engine-ului>' --exec podman info --format json
```

- Folosiți distribuția efectivă; numele ei nu a fost colectat prin API-ul remote și nu este presupus Ubuntu.
- Serverul observat este Linux amd64 rootless, versiunea 5.7.0; clientul din VM are versiune independentă 5.8.3.
- [Podman system service](https://docs.podman.io/en/v5.7.0/markdown/podman-system-service.1.html) rulează pe Linux; un client Windows remote nu creează acest serviciu.
- API-ul existent și ruta 8888 au fost pregătite separat pe fizic; instalarea/persistența lor completă rămâne în [feedback](../../docs/feedback/ATF_Setup_Feedback.md#setup-remote-transport-84--prioritate-80).
- Nu opriți serviciul existent și nu ștergeți ruta 8888 în timpul configurării Kafka; provisioning-ul și probele CLI depind încă de ea.

[Host-only VirtualBox](https://docs.oracle.com/en/virtualization/virtualbox/7.2/user/networkingdetails.html)
asigură comunicarea dintre gazdă și guest. IP-ul VM-ului poate depinde de DHCP;
verificați-l înaintea modificării firewall-ului și actualizați explicit profilul dacă s-a schimbat.

</details>

## Comenzi din VM

Dintr-o copie TMP a proiectului:

```powershell
./atf-setup/Setup-RemotePodman.ps1
./atf-setup/scripts/Start-Kafka.ps1 -ConfigPath ./atf-setup/config/remote-kafka.json
./atf-setup/scripts/Test-Kafka.ps1 -ConfigPath ./atf-setup/config/remote-kafka.json -InternalOnly
```

- Start-Kafka folosește API-ul remote; nu necesită portul Kafka deschis în guest.
- InternalOnly verifică mesajul în broker prin CLI-ul containerului; hostTcpReachable rămâne false și proba nu certifică ruta externă.
- Rețeta publică 127.0.0.1:19092 în Linux, dar anunță clienților externi 192.168.56.1:19092; forwarding-ul fizic leagă cele două adrese.

## Forwarding Kafka pe Windows-ul fizic

Copiați modulul Setup pe fizic păstrând directoarele sau exportați din guest
un singur launcher autonom, care include sursele și profilul verificate:

```powershell
./atf-setup/scripts/Export-KafkaHostAccess.ps1
```

Transferul fișierului pe fizic este explicit. O cale TMP din VM nu există
automat pe gazdă; launcherul exportat nu presupune C:\Temp\ATF-Kafka și nu
descarcă surse. În PowerShell 7.4+ deja deschis ca administrator pe fizic:

```powershell
# Varianta cu modulul copiat și directoarele păstrate:
./atf-setup/prerequisites/Configure-KafkaHostAccess.ps1

# Alternativ, fișierul unic exportat și copiat pe fizic:
./Run-KafkaHostAccess.ps1
```

| Acțiune | Contract |
|---|---|
| preflight | refuză guest-ul, IP fizic absent, rută străină și listener conflictual; cere Kafka accesibil pe localhost fizic |
| forwarding | numai 192.168.56.1:19092 către 127.0.0.1:19092 |
| firewall | numai TCP, IP local .1 și client .101; descriere owner și rută verificată la reluare |
| repetare | reutilizează numai configurația identică; fără delete, reset, adoptare ori înlocuire implicită |
| execuție | fără RunAs, ferestre noi sau restart; Access is denied cere o sesiune administrativă deschisă manual pe fizic |
| dovadă | host-kafka-access.json și loguri TMP; PASS pe fizic nu certifică încă metadata din VM |

WSL permite accesul din Windows prin localhost, dar ruta trebuie verificată
pe sistemul efectiv. [Documentația Microsoft](https://learn.microsoft.com/en-us/windows/wsl/networking)
explică separat localhost, NAT și accesul de pe alte mașini.

## Acceptanță din VM

După aplicarea rutei pe fizic:

```powershell
./atf-setup/scripts/Test-KafkaExternal.ps1
./atf-setup/Setup-RemoteKafka.ps1
./atf-setup/tests/Test-RemoteKafkaE2e.ps1
```

- Proba Java rulează în Windows VM și verifică metadata anunțată înainte să creeze topicul sau să trimită mesajul.
- Clientul și SLF4J provin din imaginea Kafka fixată; hash-urile sunt verificate înainte de compilare și execuție în TMP.
- Două runde complete cer aceeași resursă, mesaje independente pe rutele internă/externă și confirmarea aceleiași poziții de către producer și consumer.
- Setup este infrastructură cu receipts native; integrarea Eventuate, rapoartele Extent și deduplicarea business au contracte distincte.

## Limite și depanare

| Simptom | Verificare și responsabilitate |
|---|---|
| WSL2 guest refuză importul | ruta remote nu cere repararea virtualizării imbricate; verificați engine-ul pe fizic |
| TCP 8888 deschis, API reset/refuzat | verificați serviciul Linux și destinația forward-ului API; TCP singur nu certifică Podman |
| Kafka intern PASS, guest 19092 inaccesibil | verificați localhost fizic, portproxy, IP host-only și firewall |
| bootstrap accesibil, metadata indică localhost | profil/advertised listener greșit; clientul Java trebuie să refuze înainte de publish |
| Access is denied pe fizic | elevarea nu se poate acorda din permisiunile folderului sau din guest |
| regulă existentă diferită | păstrați diagnosticul și revizuiți explicit ownership-ul; scriptul nu o rescrie |

API-ul actual este TCP fără autentificare/TLS, limitat de operator la laborator;
automatizarea serviciului și transportul durabil SSH/mTLS rămân în feedback.
Nu eliminați accesul Podman înaintea validării înlocuitorului.
