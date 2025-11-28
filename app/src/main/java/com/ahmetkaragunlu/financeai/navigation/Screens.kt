package com.ahmetkaragunlu.financeai.navigation

enum class Screens(val route : String){
    SplashScreen("SplashScreen"),
    SignInScreen("LoginScreen"),
    SignUpScreen("SignUpScreen"),
    HomeScreen("HomeScreen"),
    PasswordResetRequestScreen("PasswordResetRequestScreen"),
    PasswordResetScreen("password_reset_screen?oobCode={oobCode}"),
    MAIN_GRAPH("Main_graph"),
    TRANSACTION_HISTORY_SCREEN("TransactionHistoryScreen"),
    DetailScreen("Detail_Screen/{transactionId}"),
    AnalysisScreen("AnalysisScreen"),
    AddTransaction("AddTransaction"),
    ScheduledTransactionScreen("ScheduledTransactionScreen"),
    AiChatScreen("AiChatScreen"),
    SettingsScreen("Settings_Screen"),
}