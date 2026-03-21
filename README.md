# 📱 SMS Expense Tracker (SET)

A minimal, privacy-first Android app that automatically tracks your expenses by reading bank SMS messages — everything stays on your device, nothing leaves it.

> Built with curiosity and iterated into something real. No backend. No account. No nonsense.

---

## ✨ Features

### Core

- 📩 **Automatic SMS parsing** — reads transactions from your bank inbox the moment you open the app
- ⚡ **Live SMS capture** — new transactions are picked up in real time without reopening the app
- 💾 **Local database** — all data stored on-device using Room DB, persists across sessions
- 📊 **Spending overview** — see totals for Today, This Week, This Month, This Year at a glance

### Smart Parsing

- Handles all major Indian bank SMS formats: `Rs.`, `INR`, `₹`, with or without spaces
- Detects **DEBIT** and **CREDIT** from a broad vocabulary (`debited`, `spent`, `withdrawn`, `paid`, `credited`, `received`, `refunded`, etc.)
- Extracts **merchant names** from SMS text where available

### Categories

- Auto-tags every transaction into a category: `Food`, `Transport`, `Shopping`, `Entertainment`, `Health`, `Utilities`, `Fuel`, `ATM`, `Transfer`, `EMI`, `Investment`, `Other`
- **Long-press any transaction** to manually re-categorise it

### Sender Setup

- Scans your inbox and shows detected financial senders with a message preview
- **Search bar** to filter senders instantly
- **"Show All" toggle** — reveal every alphanumeric sender, not just keyword-matched ones
- **Manual entry** — paste any sender ID directly if it doesn't show up automatically
- Previously saved senders stay pre-selected when you return to setup

### Budgets

- Set a monthly spending limit per category
- Colour-coded progress bar: 🟢 on track · 🟡 warning (75%+) · 🔴 over budget

### Export

- Export all transactions to **CSV** and share via any app (WhatsApp, Drive, email, etc.)

---

## 🏗️ Architecture

```
app/src/main/java/com/example/smsexpensetracker/
│
├── activities/
│   ├── SetupActivity.java       # First-launch sender picker
│   ├── MainActivity.java        # Main dashboard
│   └── BudgetActivity.java      # Monthly budget screen
│
├── adapters/
│   ├── TransactionAdapter.java  # RecyclerView for transaction list
│   ├── SenderPickerAdapter.java # RecyclerView for setup screen
│   └── BudgetAdapter.java       # RecyclerView for budget screen
│
├── db/
│   ├── AppDatabase.java         # Room singleton
│   └── TransactionDao.java      # All DB queries
│
├── models/
│   └── Transaction.java         # Room entity
│
├── receivers/
│   └── SmsReceiver.java         # Live SMS BroadcastReceiver
│
├── utils/
│   ├── SmsParser.java           # Regex engine — amount + type + merchant
│   ├── SmsIngestor.java         # Reads inbox → parses → saves to DB
│   ├── Tagger.java              # Auto-category tagging by keywords
│   ├── Prefs.java               # SharedPreferences wrapper
│   ├── DateUtils.java           # Time range helpers (today/week/month/year)
│   └── ExportHelper.java        # CSV export + share intent
│
└── viewmodel/
    └── MainViewModel.java       # AndroidViewModel — DB ↔ UI bridge
```

---

## 🛠️ Tech Stack

| Layer        | Tech                                                       |
| ------------ | ---------------------------------------------------------- |
| Language     | Java                                                       |
| Platform     | Android (minSdk 26)                                        |
| Database     | Room (SQLite)                                              |
| Architecture | MVVM — ViewModel + LiveData                                |
| UI           | XML layouts + Material 3 + CardView                        |
| SMS Access   | Android Telephony Content Provider + BroadcastReceiver     |
| Storage      | SharedPreferences (settings/budgets) + Room (transactions) |
| Export       | FileProvider + CSV + Share Intent                          |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android device or emulator running API 26+
- A device with actual bank SMS messages for best results

### Build

```bash
git clone https://github.com/iml0kesh/SMSExpenseTracker.git
cd SMSExpenseTracker
# Open in Android Studio → Build → Run
```

### Permissions Required

| Permission    | Why                                   |
| ------------- | ------------------------------------- |
| `READ_SMS`    | Read existing bank SMS from inbox     |
| `RECEIVE_SMS` | Capture new transactions in real time |

---

## 📱 App Flow

```
Launch
  └── Senders configured?
        ├── No  → SetupActivity (scan inbox, pick senders, manual add)
        └── Yes → MainActivity
                    ├── Spending overview (Today / Week / Month / Year)
                    ├── Transaction list (long-press to re-categorise)
                    └── Menu (⋮)
                          ├── Refresh
                          ├── Add Senders
                          ├── Remove Senders
                          ├── Set Budgets → BudgetActivity
                          └── Export to CSV
```

---

## 🗺️ Roadmap

- [ ] Charts — spending breakdown pie/bar chart
- [ ] Dark mode
- [ ] Filter transactions by category / date range
- [ ] Search transactions
- [ ] Notification when budget is exceeded
- [ ] Multiple currency support
- [ ] Backup & restore

---

## ⚠️ Disclaimer

- This app reads SMS **only on your device**
- No data is sent to any server or third party
- Tested primarily with **Indian bank SMS formats**
- Some banks use unusual sender IDs — use the manual entry feature if your bank isn't detected automatically

---

## 👤 Author

**Lokesh** — [@iml0kesh](https://github.com/iml0kesh)

> Built iteratively, improved day by day. If this helped you, drop a ⭐
