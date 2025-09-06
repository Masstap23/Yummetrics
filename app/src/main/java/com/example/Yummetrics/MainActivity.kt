package com.example.Yummetrics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.Yummetrics.ui.theme.TrackerControlTheme
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.util.Locale
import android.content.Context

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


@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: (String) -> Unit,
    onContinueClicked: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.with_sun),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 120.dp)
                .background(Color(0xFFFFC107), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Yummetrics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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

            LanguageButton(text = "🇬🇧 " + stringResource(R.string.english)) { onLanguageSelected("en") }
            LanguageButton(text = "🇷🇺 " + stringResource(R.string.russian)) { onLanguageSelected("ru") }
            LanguageButton(text = "🇵🇱 " + stringResource(R.string.polish)) { onLanguageSelected("pl") }

            Spacer(modifier = Modifier.height(120.dp))

            Button(
                onClick = onContinueClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.button_continue), color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

    @Composable
fun LanguageButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0)), // светло-бежевый
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(55.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Medium)
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LanguageSelectionScreen(
                        onLanguageSelected = { code ->
                            setLocale(this, code, restartActivity = true)
                        },
                        onContinueClicked = {
                            // TODO: переход дальше
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