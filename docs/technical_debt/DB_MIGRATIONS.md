# Technical Debt: Room Database Migrations

## Item
Replace destructive migration with explicit Room migrations before Beta.

## Context
During the Alpha development phase (DFC-12), the `AccountEntity` schema was modified to add the `balance_minor` column. To avoid the overhead of complex SQL migration scripts during rapid prototyping, `fallbackToDestructiveMigration(dropAllTables = true)` was enabled and the database version was incremented to `2`.

## Impact
- **Alpha:** Users will lose local data upon schema changes (Acceptable for Alpha/Internal builds).
- **Beta/Production:** Data loss is unacceptable.

## Resolution Requirement
- [ ] Implement `Migration` classes for all version jumps.
- [ ] Disable `fallbackToDestructiveMigration` in `DatabaseModule`.
- [ ] Add automated migration tests.

**Status:** Open (Target: Beta Milestone)
