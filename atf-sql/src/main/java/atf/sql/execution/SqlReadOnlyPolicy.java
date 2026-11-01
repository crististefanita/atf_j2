package atf.sql.execution;

/** REQUIRED is the production default; REQUESTED explicitly models drivers such as H2 that do not acknowledge it. */
public enum SqlReadOnlyPolicy { REQUIRED, REQUESTED }
