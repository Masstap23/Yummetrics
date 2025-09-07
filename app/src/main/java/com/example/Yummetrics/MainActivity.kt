package com.example.Yummetrics

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Yummetrics.ui.theme.TrackerControlTheme
import java.util.Locale

fun setLocale(activity: ComponentActivity, langCode: String, restartActivity: Boolean = false) {
    val locale = Locale(langCode)
    Locale.setDefault(locale)
    val config = activity.resources.configuration
    config.setLocale(locale)
    activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
    val prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val current = prefs.getString("language", "en")
    if (current != langCode) {
        prefs.edit().putString("language", langCode).apply()
        if (restartActivity) {
            activity.recreate()
        }
    }
}

data class UserData(
    val name: String = "",
    val gender: String = "",
    val age: Int = 0,
    val height: Int = 0,
    val weight: Int = 0,
    val activityLevel: String = "",
    val goal: String = "",
    val dailyCalories: Int = 0,
    val dailyProteins: Int = 0,
    val dailyFats: Int = 0,
    val dailyCarbs: Int = 0
)

object UserStorage {
    private const val PREFS_NAME = "user_prefs"
    fun saveUser(context: Context, user: UserData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("name", user.name)
            .putString("gender", user.gender)
            .putInt("age", user.age)
            .putInt("height", user.height)
            .putInt("weight", user.weight)
            .putString("activityLevel", user.activityLevel)
            .putString("goal", user.goal)
            .putInt("dailyCalories", user.dailyCalories)
            .putInt("dailyProteins", user.dailyProteins)
            .putInt("dailyFats", user.dailyFats)
            .putInt("dailyCarbs", user.dailyCarbs)
            .apply()
    }
    fun loadUser(context: Context): UserData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return UserData(
            name = prefs.getString("name", "") ?: "",
            gender = prefs.getString("gender", "") ?: "",
            age = prefs.getInt("age", 0),
            height = prefs.getInt("height", 0),
            weight = prefs.getInt("weight", 0),
            activityLevel = prefs.getString("activityLevel", "") ?: "",
            goal = prefs.getString("goal", "") ?: "",
            dailyCalories = prefs.getInt("dailyCalories", 0),
            dailyProteins = prefs.getInt("dailyProteins", 0),
            dailyFats = prefs.getInt("dailyFats", 0),
            dailyCarbs = prefs.getInt("dailyCarbs", 0)
        )
    }
}

@Composable
fun AppHeader(text: String = "Yummetrics", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFFFC107), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun BackgroundScreen(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.with_sun),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}

private val BottomContinueReservedSpace = 136.dp

@Composable
fun BoxScope.BottomContinueButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    label: String? = null
) {
    val text = label ?: stringResource(R.string.button_continue)
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color(0xFFFFC107),
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFFFFC107).copy(alpha = 0.85f),
            disabledContentColor = Color.Black.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp, pressedElevation = 10.dp, disabledElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
            .fillMaxWidth()
            .height(55.dp)
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LanguageSelectionScreen(
    selectedLangCode: String,
    onLanguageSelected: (String) -> Unit,
    onContinueClicked: () -> Unit
) {
    var selectedLang by remember { mutableStateOf(selectedLangCode) }
    BackgroundScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(120.dp))
                Text(
                    text = stringResource(R.string.welcome_title),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Text(
                    text = stringResource(R.string.choose_language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
                Spacer(modifier = Modifier.height(80.dp))
                LanguageButton(
                    text = "🇬🇧 " + stringResource(R.string.english),
                    isSelected = selectedLang == "en"
                ) {
                    selectedLang = "en"
                    onLanguageSelected("en")
                }
                LanguageButton(
                    text = "🇷🇺 " + stringResource(R.string.russian),
                    isSelected = selectedLang == "ru"
                ) {
                    selectedLang = "ru"
                    onLanguageSelected("ru")
                }
                LanguageButton(
                    text = "🇵🇱 " + stringResource(R.string.polish),
                    isSelected = selectedLang == "pl"
                ) {
                    selectedLang = "pl"
                    onLanguageSelected("pl")
                }
            }
            BottomContinueButton(
                enabled = true,
                onClick = onContinueClicked
            )
        }
    }
}

@Composable
fun NameInputScreen(onNameEntered: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    BackgroundScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.ask_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BottomContinueButton(
                enabled = name.isNotBlank(),
                onClick = { onNameEntered(name) }
            )
        }
    }
}

@Composable
fun LanguageButton(text: String, isSelected: Boolean = false, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF64B5F6) else Color(0xFFFFF3E0)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(55.dp)
    ) {
        Text(
            text,
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", "en") ?: "en"
        setLocale(this, langCode, restartActivity = false)
        setContent {
            TrackerControlTheme {
                var currentScreen by remember { mutableStateOf("language") }
                when (currentScreen) {
                    "language" -> LanguageSelectionScreen(
                        selectedLangCode = langCode,
                        onLanguageSelected = { code ->
                            setLocale(this, code, restartActivity = true)
                        },
                        onContinueClicked = {
                            currentScreen = "name"
                        }
                    )
                    "name" -> NameInputScreen(
                        onNameEntered = { name ->
                            val user = UserStorage.loadUser(this).copy(name = name)
                            UserStorage.saveUser(this, user)
                            currentScreen = "kbju"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.welcome_message),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrackerControlTheme {
        Greeting("Android")
    }
}
