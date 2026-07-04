# ADR 0002 — Known Limitation: Identical Purchases Within One Reconciliation Window

Status: Accepted (deferred). Do not resolve until the Evidence Model milestone.

## Context

Ledger deduplicates transactions using a content-derived fingerprint
(`accountId | amountMinor | currencyCode | hourBucket | merchant`) plus a fuzzy
score (amount, time-proximity, type). Source/channel is deliberately excluded so
that the same financial event arriving through multiple channels (bank app
notification + SMS + Wallet) collapses to a single transaction.

## Limitation

Content-based identity cannot distinguish two byte-identical *real* purchases
from one purchase seen twice. Two separate AED 50 Costa Coffee taps a few minutes
apart, on the same account, produce an identical fingerprint (same hour bucket,
same amount, same merchant) and a fuzzy score above the duplicate threshold. The
second genuine purchase is therefore merged into the first and not persisted.

This is an intentional tradeoff, not an oversight:

- Under-counting a rare repeated identical purchase is less harmful for a finance
  app than systematically double-counting every cross-channel event.
- Tightening the time window to separate the two taps would reintroduce
  cross-source duplicates, because a bank push and its SMS echo arrive seconds
  apart and would then double-insert.

There is no correct resolution using content alone.

## Deferred resolution — Evidence Model milestone

The fix belongs to a future milestone and requires source-native unique IDs:

- Thread a source-native identifier into `TransactionCandidate` (notification
  `key`, SMS `_id`, Wallet txn id, email message id). The `NotificationEnvelope`
  already carries `notificationKey`; it is currently unused for identity.
- Introduce a `TransactionEvidence` concept: one transaction may be supported by
  multiple pieces of evidence from different channels. Same-channel + same native
  ID = transport redelivery (dedupe). Different native IDs = distinct events
  (persist both), even when content is identical.
- Redesign the fingerprint to incorporate evidence rather than content alone.

Explicitly OUT OF SCOPE for the current ingestion-stability work: the evidence
model, native IDs, the `TransactionEvidence` table, and the fingerprint redesign.
Do not implement these until the ingestion engine is considered stable.
