# Ledger Engineering Standards

## Core Principles

- Offline first.
- Buildable after every sprint.
- One feature branch per feature.
- Every reusable component gets a Preview.
- Every feature must have a clear user goal.
- Money is stored as Long minor units.
- Currency belongs to every transaction.
- UI components live in the Design System.
- Features never directly depend on UI components outside the Design System.

## Git

main
    Stable

develop
    Integration

feature/*
    Feature work

## Commits

Use Conventional Commits.

Examples:

feat:
fix:
refactor:
docs:
test:
chore:

## Build

Every feature must:

Compile

Run on emulator

Be reviewable

before merge.
