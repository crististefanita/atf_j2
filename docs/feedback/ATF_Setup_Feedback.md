# Setup — acceptanța infrastructurii reale

## Pe scurt

Scripturile și contractele offline există. Un PASS al parserului ori al unui
fixture de ownership nu demonstrează engine sau broker funcțional.

## SETUP-KAFKA-REAL-82 — prioritate 94

Noul boot de la 2026-10-31, 14:23:22 București, este confirmat;
componentRestartPending este false și VirtualMachinePlatform activ.
Podman 5.8.3 și WSL 2.7.14 răspund, dar importul real WSL2 eșuează din nou.

| Probă postrestart | Rezultat |
|---|---|
| podman machine init, rețeta existentă | exit 125; WSL CreateVm/HCS_E_HYPERV_NOT_INSTALLED |
| jurnal boot, Hyper-V-Hypervisor | Event 41: VMX absent sau neactivat în BIOS |
| setup.json | FAIL cu etapa Initialize-Podman; fără PASS inventat pentru pașii Kafka |
| contracte și componente Setup | verifică ownership, procese, corelare după clock shift și refuzul dovezilor stale; nu închid acceptanța infrastructurii |

- [ ] Obțineți versiunea VirtualBox, configurația efectivă și liniile HM/NEM/VMX din VBox.log pe gazdă; nu repetați modificări speculative în guest după încă un restart fără efect.
- [ ] Corectați expunerea utilizabilă a virtualizării conform [diagnosticului](../ATF_Setup_Atlas.md#reluare-și-limita-gazdei); checkbox-ul nu certifică funcționarea.
- [ ] Reluați [Test-SetupE2e](../../atf-setup/tests/Test-SetupE2e.ps1) în guest, păstrând rețeta, bugetele și refuzul resurselor străine.

Închidere: Setup.ps1 produce setup.json PASS; proba Kafka reală păstrează
mesaj corelat, image/container ID, topic, grup, partiție și offset.
Repetați pornirea fără recreare; ownerul sau specificația străină rămân refuzate.
Această probă nu închide saga Eventuate ori idempotency business.

## SETUP-HOST-CLIENT-83 — prioritate 81

CLI-ul folosește listenerul intern; TCP-ul extern verifică numai portul.
Adăugați un client Kafka Java pe host care negociază metadata și execută
roundtrip prin 127.0.0.1:19092, cu diagnostic pentru advertised listener greșit.
Păstrați-l ca integration explicită, separat de cele trei componente MOCK.

Închidere: proba reală trece individual/prin suită, iar advertised listener
greșit este refuzat înaintea verdictului PASS.
