# Documentație J2 — fidelitate semantică

## Pe scurt

README-urile și atlasele descriu deployment-ul verificat, nu catalogul unui
alt workspace. Regulile editoriale rămân la [ownerul documentației](../README_Atf_DocumentationBestPractices.md);
acest registru păstrează numai protecția semantică încă nedemonstrată.

## DOC-DEPLOYMENT-FACTS-105 — prioritate 77

Guvernanța structurală verde nu a detectat afirmațiile curente J6 în pagini
care consumă J7, nici tabelul cu opt module J2 față de cele șapte din POM.
Textele sunt reconciliate; lipsește proba permanentă care previne reapariția contradicției.

- Owner: guvernanța documentației din J2; sursele faptelor sunt POM-ul, catalogul explicit și proveniența bibliotecilor.
- Delimitați afirmațiile curente de inventarele istorice J6; nu înlocuiți global numerele ori versiunile din istoricul livrării.
- Definiți un set mic de fapte verificabile la owner și referințe către el; evitați duplicarea acelorași numere în fiecare footer.
- Păstrați documentele self-explanatory și regulile actuale; nu introduceți skip-uri, praguri permisive sau matching rigid al întregii proze.

Închidere: mutanți controlați cu modul lipsă, versiune curentă greșită și
inventar istoric legitim. Primele două sunt refuzate, al treilea rămâne valid;
testele native ale guvernanței și documentele reale trec în același staging.

[ATF Tests](../../atf-tests/README.md#alege-gate-ul-după-schimbare) delimitează
gate-ul structural existent; succesul său nu constituie încă această probă semantică.

## DOC-CONTRACT-OWNER-107 — prioritate 74

README-ul Validation descrie prezența și schema în două secțiuni normative:
[JSON — prezență, document și capabilități](../../atf-validation/README.md#json--prezență-document-și-capabilități)
și [Contract JSON și subset de schemă](../../atf-validation/README.md#contract-json-și-subset-de-schemă).
Capabilitățile se suprapun; două puncte de întreținere cresc costul contextului și riscul de drift după sync.

- Owner: documentația Validation, reconciliată selectiv cu ownerul comun; fără schimbarea implementării ori a guvernanței.
- Păstrați un singur contract pentru presence, scalar și schema subset; celălalt traseu devine orientare/link, cu ancorele existente conservate.
- Promovați împreună limitele și probele utile; nu eliminați semantica strict/warning, BigDecimal, Unicode sau refuzul keyword-urilor nesuportate.

Închidere: fiecare regulă are un singur owner normativ, ambele trasee vechi
rămân navigabile și README-ul trece validatoarele existente. Review-ul
compară explicit tabelul de capabilități cu parserul, evaluatorul și testele.
