# Mock — precizia comparației numerice

## Pe scurt

[Contractul fixture-urilor](../../atf-mock/README.md#document-json-complet--expected-request-și-response)
refuză documente incomplete, duplicate și constrângeri YAML coerced la gol.
Regresia și acceptanța pe JAR rămân permanente; criteriul închis este retras.

## MOCK-NUMERIC-MATCH-104 — prioritate 86

Prin API-ul public, două valori decimale distincte, 9007199254740992.0 și
9007199254740993.0, se potrivesc în expected și actual. Parserul păstrează
comportamentul numeric Jackson anterior; conversia Double pierde diferența.

- Definiți egalitatea numerică a matching-ului independent de reprezentarea Double; păstrați structura, tipurile și ordinea array-ului.
- Verificați limite mari, fracții apropiate, exponent, zero și compatibilitatea nominală înainte de schimbarea readerului sau a egalității.
- Adăugați regresie componentă publică și oracol de rutare: request-ul distinct trebuie să ajungă la fallback, iar wire body nu se rescrie.

Închidere: contraexemplul nu mai selectează ruta, nominalele sunt demonstrate
și comparația integrală precede orice preview. Proba publică este în TMP,
incrementul atf-j2-mock-json-615a89c6ad, mock-numeric-observation.log.
