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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFC107), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "KBJU Tracker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.welcome_title),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.choose_language),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            LanguageButton(text = stringResource(R.string.english)) { onLanguageSelected("en") }
            LanguageButton(text = stringResource(R.string.russian)) { onLanguageSelected("ru") }
            LanguageButton(text = stringResource(R.string.polish)) { onLanguageSelected("pl") }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinueClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.button_continue), color = Color.Black)
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
            .height(50.dp)
    ) {
        Text(text, color = Color.Black)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerControlTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LanguageSelectionScreen(
                        onLanguageSelected = { langCode ->
                            // TODO: сохранить язык
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