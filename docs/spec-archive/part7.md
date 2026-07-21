# ============================================================================
# PART VII — COMPONENT BIBLE
# ============================================================================

# Chapter 60 — Component Philosophy

Components are the building blocks of Ledger.

They are not merely reusable UI widgets.

Each component represents a specific financial concept or interaction.

Every component must satisfy six responsibilities:

• Display Information
• Communicate State
• Support Accessibility
• Handle Failure
• Support Loading
• Behave Predictably

A component should never require another engineer to guess:

- why it exists
- when it should be used
- how it behaves
- how it fails

Every component is a contract.

---

## Component Structure

Every reusable component should define:

Purpose

Responsibilities

Variants

Properties

States

Interactions

Motion

Accessibility

Analytics

Implementation Notes

Future Extension Points

This documentation should exist before implementation.

---

# Chapter 61 — Net Worth Hero

Purpose

Communicate the user's current financial position immediately.

Appears

Dashboard only.

Contains

Net Worth

Available Cash

Safe to Spend

Trend

Primary Insight

Upcoming Event

Rules

One primary action only.

Never overload with secondary metrics.

Must remain readable within five seconds.

States

Loading

Empty

Healthy

Warning

Critical

Offline

Accessibility

Screen reader announces:

Current value

Trend

Change since previous period

Never rely on color alone.

---

# Chapter 62 — Financial Story Card

Purpose

Transform raw financial events into meaningful narratives.

Contains

Headline

Supporting Context

Confidence

Evidence Indicator

Actions

Timestamp

Related Entities

Example

Salary received.

Savings increased 14%.

Electricity bill due tomorrow.

Dining spending exceeds monthly trend.

Interactions

Expand

Collapse

Explain

Navigate

Share

Motion

Card expands naturally.

Supporting evidence fades in beneath narrative.

Never open modal dialogs for ordinary story expansion.

---

# Chapter 63 — Review Queue Card

Purpose

Present decisions requiring user validation.

Structure

Suggestion

Reason

Confidence

Evidence

Recommended Action

Accept

Modify

Reject

Explain

Behavior

Accept improves Learning Engine.

Reject updates Learning Rules.

Modify becomes new canonical truth.

Review completion should immediately reduce future workload.

---

# Chapter 64 — Merchant Card

Purpose

Represent a merchant as an intelligent entity.

Contains

Merchant Name

Category

Relationship Summary

Visit Frequency

Average Spend

Trend

Confidence

Subscriptions

Insights

Actions

Open Merchant

View Story

View Evidence

Transactions

Compare

Never display merchants merely as text strings.

---

# Chapter 65 — Institution Card

Purpose

Represent financial institutions.

Contains

Institution

Accounts

Connection Status

Balance

Synchronization Health

Import Source

Security

Status Indicators

Healthy

Warning

Disconnected

Importing

Sync Error

---

# Chapter 66 — Forecast Card

Purpose

Communicate future financial expectations.

Contains

Forecast

Confidence

Evidence

Projected Balance

Upcoming Bills

Recurring Income

Safe to Spend

Forecasts must remain visually distinct from confirmed financial values.

---

# Chapter 67 — Evidence Timeline

Purpose

Explain reasoning.

Structure

Evidence

↓

Confidence

↓

Decision

↓

User Correction

↓

Learning

Every intelligent decision should reference an Evidence Timeline.

---

# Chapter 68 — Confidence Indicator

Purpose

Communicate certainty.

Representation

Very High

High

Medium

Low

Unknown

Avoid percentages when unnecessary.

Users understand certainty categories faster than precise values.

Detailed percentages remain available in expanded views.

---

# Chapter 69 — Insight Card

Purpose

Communicate knowledge.

Not raw statistics.

Structure

Headline

Explanation

Supporting Evidence

Suggested Action

Confidence

Dismiss

Save

Explain

Insights should answer:

"So what?"

Not merely:

"What happened?"

---

# Chapter 70 — Safe-to-Spend Widget

Purpose

Answer the question:

"What can I safely spend today?"

Inputs

Forecast

Upcoming Bills

Recurring Payments

Income

Current Balance

Output

Safe Spending Limit

Confidence

Explanation

Warnings

This widget represents one of Ledger's flagship capabilities.

---

# Chapter 71 — Budget Progress

Purpose

Visualize progress.

Never shame users.

Should communicate

Progress

Remaining

Forecast

Expected Completion

Trend

Avoid:

Red panic bars

Aggressive warnings

Fear-based language

---

# Chapter 72 — Search Result

Purpose

Present every searchable entity consistently.

Supported entities

Merchant

Institution

Financial Event

Account

Insight

Forecast

Goal

Category

Subscription

Learning Rule

Each result includes

Icon

Title

Context

Relationship

Quick Actions

---

# Chapter 73 — Notification Card

Purpose

Represent meaningful events.

Every notification must answer

What happened?

Why?

What should I do?

Can I ignore it?

Actions

Open

Dismiss

Explain

Snooze (where appropriate)

Notifications are never advertisements.

---

# Chapter 74 — Transaction Row

Purpose

Display evidence.

Not narrative.

Contains

Merchant

Amount

Time

Category

Account

Confidence

Status

Expandable

Related Story

Explain

Evidence

Transaction rows should remain intentionally minimal.

Context belongs to Financial Story.

---

# Chapter 75 — Universal Component States

Every reusable component must support:

Loading

Skeleton

Empty

Offline

Success

Warning

Failure

Disabled

Expanded

Collapsed

Focused

Selected

Error

No component should invent custom state terminology.

---

# Chapter 76 — Interaction Contract

Every interactive component defines

Primary Action

Secondary Action

Long Press

Keyboard Navigation

Screen Reader Behavior

Haptic Feedback

Animation

Analytics Events

Future Extension Points

Behavior should remain predictable across the application.

---

# Chapter 77 — Component Commandments

1.

Components communicate.

They do not decorate.

---

2.

Reuse behavior.

Not only visuals.

---

3.

Every component should degrade gracefully.

---

4.

Accessibility is mandatory.

---

5.

Loading is a first-class state.

---

6.

Failure is a designed experience.

---

7.

Evidence should always be reachable.

---

8.

Components should explain themselves.

---

9.

One responsibility per component.

---

10.

Components exist to reduce cognitive load.

Never increase it.

# End of Part VII
