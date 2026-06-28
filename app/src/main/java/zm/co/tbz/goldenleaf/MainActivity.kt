package zm.co.tbz.goldenleaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import zm.co.tbz.goldenleaf.data.local.preferences.AppPreferences
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.navigation.TbzNavHost
import zm.co.tbz.goldenleaf.ui.auth.SessionViewModel
import zm.co.tbz.goldenleaf.ui.theme.TBZGoldenLeafTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sessionViewModel: SessionViewModel = hiltViewModel()
            val session by sessionViewModel.state.collectAsState()
            val darkThemePrefs by userPreferences.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences(),
            )
            TBZGoldenLeafTheme(darkTheme = darkThemePrefs.darkTheme) {
                if (!session.isReady) {
                    SplashContent()
                } else {
                    val navController = rememberNavController()
                    TbzNavHost(
                        navController = navController,
                        startDestination = session.startDestination,
                        userPreferences = userPreferences,
                        onFinishOnboarding = { userPreferences.setOnboardingCompleted(true) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = "Loading",
            modifier = Modifier.fillMaxSize(0.45f),
            contentScale = ContentScale.Fit,
        )
        LoadingBox(Modifier.align(Alignment.BottomCenter))
    }
}
