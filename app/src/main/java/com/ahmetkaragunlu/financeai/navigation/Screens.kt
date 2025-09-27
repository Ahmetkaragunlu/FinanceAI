package com.ahmetkaragunlu.financeai.navigation

enum class Screens(val route : String){
    SplashScreen("SplashScreen"),
    SignInScreen("LoginScreen"),
    SignUpScreen("SignUpScreen"),
    DashboardScreen("DashboardScreen"),
    PasswordResetRequestScreen("PasswordResetRequestScreen"),
    PasswordResetScreen("password_reset_screen?oobCode={oobCode}"),

    AnalysisScreen("AnalysisScreen"),
    AiChatScreen("AiChatScreen"),
    SettingsScreen("Settings_Screen"),
}