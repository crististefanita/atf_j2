# Validation — contracte și feedback

## Pe scurt

Contractele implementate și limitele lor sunt în [README Validation](../../atf-validation/README.md).
[Dovada J2](../ATF_J2_Verification.md) păstrează rezultatele și perimetrul
acceptanței; criteriile demonstrate sunt retrase din registrul activ.

| Responsabilitate | Owner și probă |
|---|---|
| reguli și verdict | Validation; API public, strict/warning și comparație înainte de preview |
| clasificare media type | vocabular Core; Validation păstrează regula semantică distinctă de căutarea literală |
| publicarea rezultatului | port neutru în Validation și adaptorul consumerului; fără reevaluare în renderer |
| randare și regresii generice | J0 Reporting și Reporting Tests; consumerul păstrează numai acceptanța sa |

Un criteriu nou trebuie să identifice un contraexemplu public, impactul,
ownerul și proba de închidere. Feedback-ul nu dublează contractele stabile
sau backlog-ul rendererului.
