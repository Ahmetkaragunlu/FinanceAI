import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ahmetkaragunlu.financeai.feature.MainNavGraphScaffold
import com.ahmetkaragunlu.financeai.feature.authNavGraph
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.screens.splash.SplashScreen

@Composable
fun FinanceNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screens.SplashScreen.route
    ) {
        composable(Screens.SplashScreen.route) {
            SplashScreen(navController = navController)
        }
        authNavGraph(navController = navController)
        composable(route = Screens.MAIN_GRAPH.route) {
            MainNavGraphScaffold()
        }
    }
}