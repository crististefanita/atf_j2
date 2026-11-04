# Laborator Eventuate — Podman remote și memorie limitată

## Pe scurt

Windows-ul din VM este clientul; engine-ul Podman și Kafka rulează pe gazda
fizică. Nu încercăm să pornim WSL2 în VM pentru acest traseu. Conexiunea
`atf-j2-host` folosește ruta host-only existentă; configurarea hostului este în
[ghidul remote](prerequisites/README_Remote_Host.md). Nu opriți engine-ul,
Kafka sau containerele altor procese pentru acest laborator.

| Traseu | Adresă și rol |
|---|---|
| VM → API Podman | `192.168.56.1:8888`, transport existent limitat prin firewall la guest; durabilitatea/securizarea au feedback separat |
| VM → Kafka extern | `192.168.56.1:19092`, forward distinct către brokerul de pe gazdă |
| container → Kafka | `atf-j2-kafka:29092`, rețeaua existentă `atf-j2-kafka-net`; fără un nou port public |
| container → SQL | `atf-j2-eventuate-db:3306`, cont `atf_reader` cu SELECT, fără port host |

Scripturile sunt silent, fără ferestre sau prompt-uri de elevare. Prerequisites
și regulile hostului se pregătesc separat; un ACL pe folder nu oferă elevare
administrativă procesului. Nu introduceți credențiale de producție în rețeta demo.

## Intrări reutilizabile

| Intrare | Responsabilitate |
|---|---|
| [configurație](config/eventuate-live.json) | commit upstream și imagini fixate prin digest, owner și limite |
| [Build](Build-EventuateApplication.ps1) | arhivă Git a commit-ului exact, build Gradle Linux, JAR-uri și hash-uri în TMP |
| [Start](Start-EventuateLive.ps1) | resurse owned, MySQL read-only witness; opțional servicii și CDC polling |
| [Export](scripts/Export-EventuateRuntime.ps1) | runtime Java, YAML explicit, manifest byte-exact și classpath Linux |
| [Probe](Run-EventuateAcceptance.ps1) | container Java mic, Kafka real și SELECT MySQL; eveniment business sintetic |
| [componente offline](tests/Test-EventuateSetupComponents.ps1) | configurație, guard de memorie și păstrarea cauzei dacă receipt-ul eșuează |

Fiecare comandă primește un `OutputDirectory` nou în TMP și emite
`ATF_SETUP_RECEIPT`. Ownership-ul și hash-ul rețetei sunt verificate înaintea
reutilizării unui container. Configurația diferită este refuzată; nu există
`stop`, `rm`, `--replace` sau restart automat al engine-ului.

## Rulare incrementală din staging TMP

```powershell
& ./atf-setup/Start-EventuateLive.ps1 -DatabaseOnly -OutputDirectory "$env:TEMP/atf-eventuate-db-<unique>"
& ./atf-setup/tests/Test-EventuateSetupComponents.ps1 -OutputDirectory "$env:TEMP/atf-eventuate-offline-<unique>"
```

Înlocuiți `<unique>` cu un identificator nou. După compilarea testelor consumerului,
exportați runtime-ul din classpath-ul complet `surefire.test.class.path` al XML-ului
Surefire într-un director nou. `Export-EventuateRuntime.ps1` primește
`SurefireXml` și `OutputDirectory`; `Run-EventuateAcceptance.ps1` primește
`RuntimeDirectory` și un alt `OutputDirectory`. Probe nu invocă aplicația Eventuate.

Build-ul upstream cere minimum 1.800 MiB disponibili; DB-only 768 MiB,
proba mică 512 MiB și pornirea aplicației complete 2.560 MiB. Se măsoară
`MemAvailable`, nu numai `MemFree`. Gate-ul refuză pornirea când lipsește bugetul;
nu eliberează memoria altor procese. Limitele containerelor nu reprezintă o
garanție că toate combinațiile JVM/CDC pornesc în RAM-ul respectiv.

## Stare verificată și limite

La 2026-11-01, MySQL a fost pornit cu limită 384 MiB și cont SELECT-only.
Proba Java cu limită 384 MiB/heap 160 MiB a trecut pe Kafka și MySQL reale:
14 comparații, un snapshot Kafka și două SQL, inclusiv timeout negativ.
Aceleași fapte au fost publicate în Extent și fallback pe Core/Reporting.
Kafka a păstrat identitatea și `StartedAt`; Android-ul altui proces a rămas activ.

Build-ul fixat upstream a produs JAR-urile customer/order într-un builder Linux.
Comanda reutilizabilă Build este verificată offline; build-ul real inițial a
folosit aceeași rețetă Gradle într-o lansare diagnostică în TMP. Nu îl relansați
automat dacă memoria disponibilă este sub gate.

**Aplicația completă este NOT RUN.** Start înregistrează
`STARTED_REQUIRES_READINESS`, nu acceptanță saga. Readiness HTTP/CDC, schema
reală, cele trei cazuri individual/prin suită și cleanup-ul datelor business
au criteriu deschis în [feedback-ul Eventuate](../docs/feedback/ATF_Eventuate_Feedback.md).
Un PASS al evenimentului sintetic nu certifică outbox, deduplicare sau CQRS.

[Dovada incrementului](../docs/ATF_SQL_Live_Verification.md) păstrează și probele
eșuate, bugetele și proveniența. [Atlasul Setup](../docs/ATF_Setup_Atlas.md)
separă hostul, provisioning-ul și acceptanța aplicației.

## Configurația raportului și refuzul dovezilor incomplete

- [eventuate-report.yaml](config/eventuate-report.yaml) este copiat ca config/atf.yaml,
  inclus în hash-uri și transmis explicit runnerului. assertions rămâne 240,
  iar corpul generic este bounded la 12.000 de caractere.
- Lipsa YAML-ului explicit folosea default-ul JAR de 100 și tăia query/rows/offsets;
  aceasta era o problemă a runnerului J2, fără schimbare în Reporting.
- InfrastructureReportOracle compară faptele capturate cu ambele HTML-uri, cu
  numere exacte și identități; trunchierea, mutarea ori schimbarea verdictului produc FAIL.
- Receipt-ul separă verdictul execuției, artifactAudits și aplicația NOT_RUN.
  Un eșec de publicare nu reclasifică SELECT-ul sau execuția celeilalte proiecții.
