# ATF workspace rules

- Use `C:\work\1\atf_j0` as the current Reporting source and test reference.
  `demo2` is historical; do not edit or synchronize it for new work.
- Read `docs/ATF_Workspace_Synchronization.md` and
  `docs/ATF_Build_And_Execution_Safety.md` before changing or delivering code.
- Keep Reporting implementation/native tests in `atf_j0/atf-reporting` and
  permanent mock, contract and browser regressions in `atf_j0/atf-reporting-tests`.
- After relevant J0 changes, assess both `atf_j1` and `atf_j2`. Deliver and
  validate each initialized consumer separately; J2 is currently empty.
  Do not claim its initialization or acceptance from J1's results.
- Binary consumers retain only the latest Reporting JAR/POM directly in `lib`,
  with one root provenance file. Do not copy Reporting source or its harness
  into the live binary consumers.
- Reconcile common code and consumer adapters selectively in both directions.
  Preserve local work, reactor POMs, configurations and independent Git bases.
  A successful file promotion is not an atomic transaction across workspaces.
- Build/test only in owned TMP staging; keep generated reports/cache/output
  there. Preserve the 240-character default reporting preview and auditable,
  collapsible component/E2E assertions.
- Keep owner documentation and feedback current with the actual code and
  tests. Preserve historical build provenance; a directory rename alone does
  not require a new Reporting version or imply a fresh test execution.
