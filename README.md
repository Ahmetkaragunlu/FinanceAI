💰 Finance AI 🤖

AI-powered personal finance management application with offline-first architecture 📱 and real-time multi-device synchronization 🔄.

📋 Overview

Finance AI is an Android application that helps users track their income and expenses, manage budgets, and receive AI-powered financial recommendations. Built with modern Android development practices, the app works fully offline 📶 and syncs automatically when connected ☁️.

✨ Features

💸 Transaction Management

Track your income and expenses with categories, attach photos 📸 and location 📍 to each transaction, filter by date ranges (today, yesterday, last week, last month), and view detailed transaction history.

📅 Scheduled Transactions

Set up recurring payment reminders for rent, bills, and subscriptions. Get smart notifications 🔔 with snooze options at specific intervals and complete transactions with one tap.

🤖 AI Financial Assistant

Powered by Google Gemini 2.5 Flash. Get personalized financial advice, budget overspend warnings ⚠️, spending pattern analysis, and interactive chat 💬 for financial queries.

📉 Budget Management

Create monthly general budgets and category-specific limits. Use percentage-based allocations (e.g., 20% for entertainment 🍿). Track progress with visual indicators and receive smart alerts at the 80% threshold.

🔄 Multi-Device Synchronization

All your data syncs in real-time across all your devices. Work seamlessly between phone 📱 and tablet 📲 with automatic conflict resolution.

📊 Visual Analytics

View your spending patterns with interactive pie charts, see monthly summaries, and analyze expenses by category.

🏗️ Technical Architecture

🏛️ Architecture Pattern: MVVM with Clean Architecture principles, Repository Pattern, and Offline-First strategy.

💻 Core Technologies: Kotlin, Jetpack Compose for UI, Coroutines and Flow for asynchronous operations, Hilt for dependency injection.

🗄️ Local Database: Room Database with entity relationships, DAO patterns, and type converters.

☁️ Backend: Firebase Authentication (Google Sign-In), Firestore for cloud database with real-time listeners, Firebase Storage for photos, Cloud Messaging for push notifications, Firebase Functions (Node.js) for server-side logic.

⚙️ Background Processing: WorkManager for scheduled tasks, constraint-based execution, and retry mechanisms.

🧠 AI Integration: Google Gemini 2.5 Flash API for financial analysis and personalized recommendations.

📚 Additional Libraries: Google Maps SDK for location services, Coil for image loading and caching, Material Design 3 components.

🚀 How It Works

📶 Offline-First Approach

All data is saved to Room Database first. The app works fully functional without internet. When online, FirebaseSyncService automatically syncs data in the background.

↔️ Two-Way Synchronization

Data flows from device to cloud (push) and cloud to device (pull). Each data model tracks sync status. Unsynchronized data is marked and automatically synced when a connection is available.

⚡ Real-Time Updates

Firebase Firestore listeners provide real-time updates. When you add a transaction on your phone, it instantly appears on your tablet.

🛡️ Conflict Resolution

Uses a last-write-wins strategy with timestamp-based conflict resolution. Ensures data consistency across all devices.

🕰️ Background Tasks

WorkManager handles photo uploads 📤, data synchronization, and scheduled notifications intelligently based on network conditions.

🔔 Smart Notifications

Firebase Functions manage server-side notification logic. Supports multi-device notification delivery, snooze functionality with specific intervals 💤, and scheduled reminders.
