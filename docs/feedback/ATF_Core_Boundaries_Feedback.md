# ATF Core — fidelitatea datelor la boundary

## Pe scurt

Criteriile reproduse privesc acoperirea diagnosticului Authorization, headerele
explicite și clasificarea MIME. Probele folosesc valori sintetice, fără serviciu
extern; masca literală are contract stabil la Core security.

## CORE-QUOTED-AUTH-DIAGNOSTICS-74 — Authorization quoted în text și fallback

**Prioritate: 74/100** = impact 31 + lacună 22 + lucrabil 15 + documentare 6.
**Owner:** Core security, recunoașterea credențialelor în text și fallback.
**Nivel:** unit prin API public; caller neutru înainte de preview.

Cu politica implicită, headerul simplu și JSON-ul structural valid maschează
canary-ul. `redactText` aplicat la `metadata: {"authorization":"Bearer <canary>"}`
îl păstrează; `redactJson` pe același obiect fără acolada finală îl păstrează
prin fallback. Este o lacună preexistentă de recunoaștere, distinctă de codarea
literală a măștii; proba nu declară un leak în Extent-ul implicit al consumerilor.

- [ ] Recunoașteți cheia quoted Authorization în diagnosticul textual și fallback, păstrând delimitatorii și politica nominală a headerului simplu.
- [ ] Verificați exact expected-ul independent și absența întregului canary, inclusiv quoting/escape, masca implicită și măștile literale speciale.
- [ ] Păstrați separat JSON-ul valid, documentul malformat și câmpurile nesensibile; nu reparați JSON-ul și nu compensați prin truncare sau printr-un renderer ulterior.
- [ ] Probați comparația înaintea redactării și scanarea artifactului local; fără să mutați implementarea în Reporting.

**Închidere:** Core elimină canary-ul înainte de preview pe ambele trasee
reproduse, cu nominale păstrate. Unitățile Core rămân fără renderer, iar
publicarea se verifică separat prin compoziția care folosește redactorul.

## CORE-MEDIA-TYPE-69 — JSON se decide din media type

**Prioritate: 68/100** = impact 27 + lacună 18 + lucrabil 17 + documentare 6.
**Owner:** Core pentru clasificare; Mock pentru efectul asupra matching-ului.
**Nivel:** unit și componentă Mock.

`MediaTypes.isJson("text/plain; profile=json")` întoarce `true`.
Cu body `{}`, o rută Mock POST obișnuită acceptă request-ul, deși tipul principal este text/plain.

- [ ] Clasificați tipul/subtipul, separat de parametri; păstrați JSON standard, sufixele `+json`, case-insensitivity și charset-ul valid.
- [ ] Refuzați `json` doar în parametri ori într-un subtip fără semantica JSON; verificați și modul Mock `WRONG_CONTENT_TYPE`.
- [ ] Păstrați body-ul JSON valid și invalid ca axe distincte; un MIME corect nu repară un payload invalid.

**Închidere:** [MediaTypes](../../atf-core/src/main/java/atf/core/http/request/MediaTypes.java)
și [MockRoute](../../atf-mock/src/main/java/atf/mock/MockRoute.java) au controale pozitive/negative independente;
ruta aleasă și verdictul nu depind de un substring din parametri.

## CORE-EXPLICIT-HEADERS-68 — precedență egală în transporturile live

**Prioritate: 65/100** = impact 25 + lacună 18 + lucrabil 17 + documentare 5.
**Owner:** Core HTTP. **Nivel:** unit pentru request-ul construit, apoi loopback pentru ambele transporturi.

Un POST cu `content-type: application/x-www-form-urlencoded` ajunge în
`HttpClient.sendAsync` cu două valori Content-Type: JSON și form-urlencoded.
[HttpClientTransport](../../atf-core/src/main/java/atf/core/http/transport/HttpClientTransport.java)
adaugă headerele, iar [UrlConnectionTransport](../../atf-core/src/main/java/atf/core/http/transport/UrlConnectionTransport.java)
le înlocuiește. Proba înregistrează request-ul real construit și exact un send, fără rețea externă.

- [ ] Definiți precedența case-insensitive a headerului explicit față de default; păstrați Accept când nu este suprascris.
- [ ] Verificați valorile complete văzute la boundary-ul transportului, inclusiv nume cu casing diferit și JSON fără override.
- [ ] Păstrați autentificarea și o singură expediere; nu introduceți replay pentru a compensa MIME-ul greșit.

**Închidere:** ambele transporturi livrează același header efectiv pentru același request;
proba nu verifică numai map-ul inițial `ApiRequest.headers()`.

## Proces

Atlasul [pachetului security](../../atf-core/src/main/java/atf/core/security/README.md)
delimitează redactarea de preview. Schimbările și probele se execută prin
[procesul izolat](../ATF_Build_And_Execution_Safety.md); controalele testelor rămân în
[baseline](../ATF_Test_Changes_Baseline.md).
