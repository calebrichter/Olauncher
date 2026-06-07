# oLauncher Flow

A flow-based fork of oLauncher that restricts application access based on time-based phases and bypass triggers.

## Language

**Flow**:
The daily scheduled progression of App Restrictions that dictates which apps are accessible at any given time.
_Avoid_: schedule, routing

**Phase**:
A segment of the Flow with a defined start time, end time, and a list of Allowed Apps.
_Avoid_: time window, slot

**Always Whitelisted Apps**:
The set of applications (e.g. Phone, Engage) that are exempt from all blocking rules and are always permitted.
_Avoid_: global whitelist, exempt apps

**Phase State**:
The status of a Phase, which can be **Restricted** (only the trigger app and Always Whitelisted Apps allowed) or **Unlocked** (the wider set of Phase-specific apps are allowed).
_Avoid_: lock status, open status

**Unlock Condition**:
A requirement (e.g. spending 10 cumulative minutes in the trigger app) that must be met to transition a Phase State from Restricted to Unlocked.
_Avoid_: gateway, progression key

**Allowed Apps**:
The whitelist of Android package names that the user is permitted to open or access during a Phase.
_Avoid_: whitelist, permitted apps

**Blocked App**:
Any application not present in the Allowed Apps whitelist for the current Phase.
_Avoid_: restricted app, forbidden app

**Bypass**:
A temporary lifting of the Flow restrictions.
_Avoid_: unlock, override

**Bypass Group**:
A designated Telegram group chat shared with the user's wife containing a **Bypass Bot**.
_Avoid_: override chat, unlock group

**Bypass Bot**:
A Telegram bot added to the **Bypass Group** that listens for messages and exposes them to the launcher via the Telegram Bot API.
_Avoid_: unlock bot

**Bypass Poll**:
The background polling mechanism used by the launcher to query the Telegram Bot API for new messages in the **Bypass Group**.
_Avoid_: sync worker

## Relationships

- The **Flow** consists of multiple sequential **Phases** throughout the day.
- A **Phase** starts in a **Restricted** **Phase State** if it has an **Unlock Condition**.
- Meeting the **Unlock Condition** transitions the **Phase State** to **Unlocked**, making all allowed apps for that phase available.
- **Always Whitelisted Apps** are always permitted, regardless of the active **Phase** or its **Phase State**.
- A **Bypass** temporarily overrides the current **Phase** restrictions when a new message is fetched via **Bypass Poll** from the **Bypass Bot** in the **Bypass Group**.


## Example dialogue

> **Dev:** "In the morning, can the user open Slack immediately?"
> **Domain expert:** "No. The morning **Phase** starts in the **Restricted** state. Only **Telegram** (the trigger app) and **Always Whitelisted Apps** (like Phone and Engage) can be opened. Once the **Unlock Condition** is met (spending 10 minutes in Telegram), the state becomes **Unlocked**, and Slack becomes available."

## Flagged ambiguities

- None yet.
