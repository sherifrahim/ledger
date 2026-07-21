# ============================================================================
# PART V — PRODUCT EXPERIENCE & SCREEN SPECIFICATION
# ============================================================================

# Chapter 33 — Information Architecture

Ledger organizes information according to user intent.

Not database structure.

Not backend domains.

Not implementation convenience.

Every screen should answer one primary question.

If a screen answers multiple unrelated questions,
it should be redesigned.

---

## Core User Questions

Every interaction ultimately belongs to one of these categories.

1.

Where do I stand today?

↓

Dashboard

---

2.

What happened?

↓

Financial Story

---

3.

What requires my attention?

↓

Review Queue

---

4.

Why did this happen?

↓

Insights

Merchant Pages

Explainability

---

5.

What will happen next?

↓

Forecast

Upcoming Events

Safe to Spend

---

6.

Where can I find something?

↓

Universal Search

---

7.

How does Ledger work?

↓

Settings

Privacy

Learning

Data Sources

---

Navigation exists to answer these questions.

Not to expose features.

---

# Chapter 34 — Navigation Philosophy

Navigation should remain stable.

Users should develop spatial memory.

Changing navigation frequently destroys confidence.

---

## Primary Navigation

The application consists of five permanent destinations.

Dashboard

Story

Review

Search

Settings

These should remain stable across major releases.

Future features should integrate into these destinations rather than creating additional navigation.

---

Dashboard

Purpose

Current Financial State

---

Story

Purpose

Financial Timeline

Relationships

Historical Context

---

Review

Purpose

Human Validation

Learning

Corrections

Trust

---

Search

Purpose

Universal Discovery

Accounts

Merchants

Transactions

Institutions

Insights

Forecasts

---

Settings

Purpose

Control

Privacy

Learning

Connections

Preferences

---

Secondary experiences

Merchant

Institution

Forecast

Budget

Goal

Subscription

Account

should appear through navigation from primary destinations.

Never become primary destinations unless product strategy fundamentally changes.

---

# Chapter 35 — Dashboard

Primary Question

"Where do I stand right now?"

The Dashboard is Ledger's Financial Command Center.

Not a summary page.

Not a widget collection.

Every section should influence a financial decision.

---

Dashboard hierarchy

1.

Financial Story Summary

2.

Urgent Actions

3.

Review Queue

4.

Upcoming Events

5.

Safe to Spend

6.

Forecast

7.

Insights

8.

Accounts

9.

Recent Activity

Transactions appear last.

---

## Hero Section

Should communicate the user's financial state within five seconds.

Possible content

Net Worth

Cash Available

Safe to Spend

Current Trend

Next Major Event

One primary insight

Never overload the Hero.

---

## Financial Story Card

Instead of displaying metrics only,

display narrative.

Example

Salary received yesterday.

Rent due in four days.

Dining spending increased 12%.

Emergency fund reached 82%.

Three subscriptions renew this week.

Review Queue has two pending decisions.

This becomes the emotional center of the application.

---

## Urgent Actions

Displayed only when action matters.

Examples

Review transaction

Reconnect institution

Potential duplicate

Large unusual purchase

Upcoming overdraft

Never display "urgent" items simply to fill space.

---

# Chapter 36 — Financial Story

Primary Question

"What happened in my financial life?"

The Story screen is Ledger's defining experience.

It replaces the traditional transaction feed.

---

Instead of

Transaction

↓

Category

↓

Amount

Ledger presents

Financial Event

↓

Context

↓

Relationships

↓

Outcome

↓

Evidence

---

Every story item should answer

What happened?

Why does it matter?

What changed?

What happens next?

---

Example

Netflix renewed.

Subscription cost increased AED 5.

This is the third increase in two years.

Projected annual cost:

AED XXX.

Suggested alternatives available.

Confidence

98%.

Explain

Available.

---

Transactions become expandable evidence.

Not the headline.

---

# Chapter 37 — Review Queue

Primary Question

"What does Ledger need from me?"

Review Queue exists to reduce future work.

Not create work.

Every confirmation should improve the system.

---

Examples

Unknown Merchant

Low Confidence Category

Duplicate Detection

Recurring Payment Suggestion

Subscription Detection

Forecast Correction

Relationship Confirmation

Learning Proposal

---

Every review card displays

Suggestion

Confidence

Evidence

Accept

Modify

Reject

Explain

Never ask users to make blind decisions.

---

# Chapter 38 — Merchant Experience

Merchants become living entities.

Not strings.

Merchant page contains

Overview

Financial Relationship

Visit Frequency

Monthly Spend

Category

Subscriptions

Related Merchants

Insights

Timeline

Evidence

Learning History

---

Example

Starbucks

Visited

18 times

Average Spend

AED 28

Morning purchases

83%

Most visited branch

Al Wahda Mall

Confidence

99%

Merchant pages should answer

"What is my relationship with this merchant?"

Not merely

"What transactions exist?"

---

# Chapter 39 — Institution Experience

Institutions become financial homes.

Institution pages contain

Accounts

Balances

Connections

History

Reliability

Synchronization

Imported Sources

Linked Cards

Security

Institution Health

---

Users should immediately understand

Which institution owns which financial data.

---

# Chapter 40 — Forecast Experience

Primary Question

"What happens next?"

Forecasts should never feel speculative.

They should communicate confidence.

---

Forecast timeline

Tomorrow

This Week

This Month

Three Months

One Year

---

Forecasts should include

Upcoming Bills

Expected Salary

Recurring Payments

Subscription Renewals

Projected Cash Flow

Safe to Spend

Savings Trend

---

Every prediction should expose

Confidence

Evidence

Reasoning

Adjustment History

---

# Chapter 41 — Universal Search

Search should feel instantaneous.

Users should not need to know where information lives.

Search indexes

Merchants

Transactions

Accounts

Institutions

Categories

Insights

Forecasts

Subscriptions

Goals

Review Items

Learning Rules

---

Search becomes an intelligence layer.

Not a filter.

---

# Chapter 42 — Notifications

Notifications should protect attention.

Never compete for it.

---

Allowed notifications

Large transaction

Forecast risk

Review required

Subscription renewal

Salary received

Institution disconnected

Potential fraud

Goal milestone

---

Avoid

Daily reminders

Artificial engagement

Streaks

Promotional notifications

Meaningless summaries

---

# Chapter 43 — User Journeys

Journey 1

First Launch

↓

Onboarding

↓

Import

↓

Normalization

↓

Story Creation

↓

Review Queue

↓

Dashboard

---

Journey 2

Daily User

↓

Dashboard

↓

Story

↓

Review

↓

Exit

Average session should require minimal effort.

---

Journey 3

Unexpected Spending

↓

Notification

↓

Dashboard

↓

Story

↓

Merchant

↓

Evidence

↓

Resolution

---

Journey 4

Budget Planning

↓

Dashboard

↓

Forecast

↓

Safe to Spend

↓

Decision

---

# Chapter 44 — Product Experience Rules

1.

Users should understand the Dashboard within five seconds.

---

2.

Every AI suggestion must expose evidence.

---

3.

Every screen answers one primary question.

---

4.

Every review improves future automation.

---

5.

Financial Story replaces transaction-first thinking.

---

6.

Search discovers everything.

---

7.

Navigation remains stable.

---

8.

Forecasts distinguish certainty from probability.

---

9.

Users remain in control.

---

10.

Ledger should feel calmer after five minutes than before opening it.

---

# End of Part V
