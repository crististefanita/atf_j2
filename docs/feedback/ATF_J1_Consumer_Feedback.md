# ATF J1 — gate-uri și boundary-uri ale consumului binar

## Pe scurt

J1 deține schimbările comune non-Reporting; biblioteca și regresiile ei
generice au owner în `atf_j0`. [Procesul J1](../ATF_J1_Consumer.md) fixează
sincronizarea, iar [atlasul JAR](../ATF_Reporting_Library_Atlas.md) permite integrarea fără sursă.

Guvernanța source/binary și controlul Git au contract la
[ownerul ATF Tests](../../atf-tests/README.md#guvernanța-deployment-ului-sursă-sau-binar),
iar controalele verificate sunt în [baseline](../ATF_Test_Changes_Baseline.md). Regresiile Reporting rămân la producător, iar J1 păstrează integrarea adaptoarelor.

<a id="j1-reporting-tests-02--retragere-cu-dovadă-păstrată"></a>

Contractul retragerii și acceptarea fiecărui JAR sunt la
[gate-ul extern](../ATF_J1_Consumer.md#gate-ul-extern-reporting).

## Pachetul documentar și compatibilitatea bibliotecii



Atlasul local explică integrarea, nu înlocuiește livrarea documentației unei
versiuni susținute. [Reporting](ATF_Reporting_Feedback.md) deține documentele
de adopție, suprafața API și compatibilitatea; [Reporting Tests](ATF_Reporting_Tests_Feedback.md) deține oracolele neutre.

Un upgrade cere JAR/POM și hash-uri coerente în proveniența unică, exemplu compilat
fără sursă, CodeSource verificat și acceptanța adaptoarelor. Un hash al surselor
ori compatibilitatea unor simboluri nu certifică întregul API sau comportamentul.








