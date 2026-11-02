# Documentație — fidelitatea deployment-ului J2

## Pe scurt

Guard-ul factual pentru arhitectură există și compară modulele și scope-urile
Kit cu POM-urile. Nu îl mai listați drept implementare lipsă. Restanța este
acoperirea afirmațiilor curente despre versiunile binare și reducerea duplicării.

## DOC-DEPLOYMENT-FACTS-105 — prioritate 81

- DocumentedBuildContractTest păstrează nominalul și mutanții pentru numărul
  modulelor și scope-ul Kit; incrementul SQL a refuzat inclusiv o formulare
  care nu mai expunea declarația factuală. Controalele nu au fost relaxate.
- Deriva rămasă este demonstrată în indexul docs: Core C1/J8 și șapte module,
  în timp ce consumerul actual folosește C2/J10 și catalogul SQL de opt module.
  Textele curente sunt reconciliate; dovezile istorice păstrează versiunea execuției.
- Extindeți faptele verificate la versiunile curente din index/owner README și
  proveniența JAR/POM. Folosiți un contract mic, fără matching al întregii proze.
- Reduceți footerele care repetă catalogul; navigarea către owner este preferabilă
  duplicării aceleiași afirmații în fiecare atlas.

Închidere: mutantul unei versiuni curente greșite este refuzat, iar istoricul
legitim J6/J8/J9 și catalogul unui alt deployment rămân valide. Păstrați
probele existente de module/scope, limitele editoriale și linkurile reale.

[Guvernanța documentară](../../atf-tests/README.md#alege-gate-ul-după-schimbare)
și [stilul proiectului](../README_Atf_DocumentationBestPractices.md) rămân ownerii regulilor.

### Current semantic drift — surgical examples

- Mock README still states that high-precision decimal matching has an open
  criterion, while its final exact-numeric section and the feedback index say
  it is delivered. Its earlier Core C1/parser description also needs an
  explicit historical boundary rather than a current limitation.
- Workspace synchronization labels a J10 delivery through the J8 verification
  link. Preserve the historical document; correct the current navigation.
- Root atf.yaml still advertises api/reporting/ui and a GoRest API default.
  This is not the Maven reactor and does not restore UI at runtime. Clarify
  which compatibility settings are consumed and which Eventuate LIVE settings
  are authoritative; do not describe the stale list as the current deployment.
- J0's synchronization table still gives J2 seven modules and an unimplemented
  observer. J2 currently has eight modules and implemented observer contracts;
  full Eventuate application acceptance remains NOT RUN.

Fix current claims at their owner; retain dated evidence unchanged. Add small
fact contracts for current declarations, not a regex for every paragraph.
New feedback is English; a future language cleanup should preserve the approved
README diagrams/navigation and remove contradictions before expanding prose.
