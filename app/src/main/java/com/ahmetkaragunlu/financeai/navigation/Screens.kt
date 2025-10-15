package com.ahmetkaragunlu.financeai.navigation

enum class Screens(val route : String){
    SplashScreen("SplashScreen"),
    SignInScreen("LoginScreen"),
    SignUpScreen("SignUpScreen"),
    HomeScreen("HomeScreen"),
    PasswordResetRequestScreen("PasswordResetRequestScreen"),
    PasswordResetScreen("password_reset_screen?oobCode={oobCode}"),
    MAIN_GRAPH("Main_graph"),
    HISTORY_SCREEN("HistoryScreen"),


    AnalysisScreen("AnalysisScreen"),
    AddTransaction("AddTransaction"),
    AiChatScreen("AiChatScreen"),
    SettingsScreen("Settings_Screen"),
}