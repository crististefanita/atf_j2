# Display J2 — comparații și artifact

## Pe scurt

Consumerul J2 păstrează valoarea, identitatea și verdictul înainte de display.
Rendererul și layout-ul aparțin Reporting JAR; [contractul complet](../../atf_j0/docs/ATF_Reporting_Display_Contract.md)
și regresiile generice rămân în J0.

| Suprafață | Contract J2 |
|---|---|
| assertions | patru coloane distincte; expected/actual integral comparate înainte de preview 240 |
| tabele | spațiul disponibil și lățimea conținutului, fără colapsarea într-o singură coloană |
| HTTP | request, response, tentative și timing capturate de Core |
| mesagerie MOCK | assertions neutre; fără offset Kafka inventat din ordinalul fixture-ului |
| diagnostic | FAIL și identitatea dovezii originale conservate în Tests/Fails |
| interacțiune | expandare, ultimul rând, scroll și tastatură verificate pe HTML |

Consumerul nu injectează CSS/JavaScript pentru repararea bibliotecii.
Fallback este un renderer distinct; paritatea privește semantica și accesul
la dovezi, fără impunerea aceleiași structuri DOM.

[Atlasul J6](ATF_Reporting_Library_Atlas.md) și [verificarea executată](ATF_J2_Verification.md)
delimitează API-ul și suprafețele inspectate; cerințele viitoare se verifică pe noul binar.
