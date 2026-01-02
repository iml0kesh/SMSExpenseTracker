# SMS Expense Tracker (SET)

A minimal, modern Android application that automatically tracks expenses and income by reading bank SMS messages.

This project was **vibe-coded** — built iteratively by experimenting, observing real SMS patterns, refining logic, and polishing the UI step-by-step.  
The focus was learning, clarity, and clean fundamentals rather than over-engineering.

---

## Why This Project?

Most expense apps require **manual entry**, which is slow and boring.

SET solves this by:

- Reading **bank transaction SMS**
- Extracting **credit & debit amounts**
- Showing a **clear spending overview**
- Keeping everything **offline & private**

---

## Features (Version 1)

- Reads SMS using Android Telephony API
- Detects **Debit / Credit** transactions
- Spending overview:
  - Today
  - This Week
  - This Month
  - This Year
- Minimal & modern UI (Material 3)
- No backend, no server, no data upload
- Lightweight & fast

---

## How It Works

1. App requests SMS permission
2. Reads messages from known bank senders
3. Parses transaction amount & type using regex
4. Displays totals and recent transactions

All processing happens **locally on the device**.

---

## Tech Stack

- **Language:** Java
- **Platform:** Android
- **UI:** XML + Material 3
- **APIs:** Android Telephony SMS Content Provider
- **Architecture:** Simple & readable (no overkill)

---

## Learning Highlights

This project helped me understand:

- Android runtime permissions
- SMS Content Provider querying
- Regex-based data extraction
- Git & GitHub workflow
- UI theming with Material 3

---

## Versioning

### v1.0.0 (Stable)

- Core SMS parsing
- Spending dashboard
- Minimal UI
- Local-only processing

---

## ⚠️ Disclaimer

- This app reads SMS **only on the device**
- No data is sent or stored externally
- Intended for **personal use & learning**
- Tested with Indian bank SMS formats

---

## About the Build

This app was **vibe-coded** — built with curiosity, trial-and-error, and continuous refinement rather than strict upfront design.

The goal was:

> _“Make it work → Make it clean → Make it better”_

---

## Status

✅ Version 1 completed  
🚧 Version 2 under active development

---

⭐ If you like this project, feel free to star the repo
