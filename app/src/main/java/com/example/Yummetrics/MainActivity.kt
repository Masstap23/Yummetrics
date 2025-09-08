package com.example.Yummetrics

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Yummetrics.ui.theme.TrackerControlTheme
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

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
        if (restartActivity) activity.recreate()
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
    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }
    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("onboarding_completed", false)
    }
}

@Composable
fun AppHeader(text: String = "Yummetrics", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFFFC107), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun BackgroundScreen(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
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
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFC107),
            disabledContainerColor = Color(0xFFFFE082),
            disabledContentColor = Color(0xFF616161)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
            .fillMaxWidth()
            .height(55.dp)
    ) {
        Text(text = text, color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                ) { selectedLang = "en"; onLanguageSelected("en") }
                LanguageButton(
                    text = "🇷🇺 " + stringResource(R.string.russian),
                    isSelected = selectedLang == "ru"
                ) { selectedLang = "ru"; onLanguageSelected("ru") }
                LanguageButton(
                    text = "🇵🇱 " + stringResource(R.string.polish),
                    isSelected = selectedLang == "pl"
                ) { selectedLang = "pl"; onLanguageSelected("pl") }
            }
            BottomContinueButton(enabled = true, onClick = onContinueClicked)
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
                enabled = true,
                onClick = { if (name.isNotBlank()) onNameEntered(name) }
            )
        }
    }
}

@Composable
fun KbjuQuestionScreen(
    name: String,
    onYesEnter: () -> Unit,
    onNoCalculate: () -> Unit
) {
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
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(R.string.hello_name, name.ifBlank { "User" }),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.kbju_question),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(40.dp))
                LanguageButton(text = stringResource(R.string.kbju_yes_enter)) { onYesEnter() }
                LanguageButton(text = stringResource(R.string.kbju_no_calculate)) { onNoCalculate() }
            }
        }
    }
}

@Composable
fun KbjuInputScreen(
    initialCalories: Int,
    initialProteins: Int,
    initialFats: Int,
    initialCarbs: Int,
    onFinish: (cal: Int, p: Int, f: Int, c: Int) -> Unit
) {
    var calories by remember { mutableStateOf(if (initialCalories > 0) initialCalories.toString() else "") }
    var proteins by remember { mutableStateOf(if (initialProteins > 0) initialProteins.toString() else "") }
    var fats by remember { mutableStateOf(if (initialFats > 0) initialFats.toString() else "") }
    var carbs by remember { mutableStateOf(if (initialCarbs > 0) initialCarbs.toString() else "") }
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    val valid = calories.isNotBlank() && proteins.isNotBlank() && fats.isNotBlank() && carbs.isNotBlank()
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
                    text = stringResource(R.string.kbju_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = onlyDigits(it) },
                    label = { Text(stringResource(R.string.calories_kcal)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = proteins,
                    onValueChange = { proteins = onlyDigits(it) },
                    label = { Text(stringResource(R.string.proteins_g)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = fats,
                    onValueChange = { fats = onlyDigits(it) },
                    label = { Text(stringResource(R.string.fats_g)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = onlyDigits(it) },
                    label = { Text(stringResource(R.string.carbs_g)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BottomContinueButton(
                enabled = valid,
                onClick = {
                    val cal = calories.toIntOrNull() ?: 0
                    val p = proteins.toIntOrNull() ?: 0
                    val f = fats.toIntOrNull() ?: 0
                    val c = carbs.toIntOrNull() ?: 0
                    onFinish(cal, p, f, c)
                },
                label = stringResource(R.string.button_finish)
            )
        }
    }
}

data class CalcInput(
    var gender: String = "",
    var age: Int = 0,
    var weight: Int = 0,
    var height: Int = 0,
    var activity: String = "",
    var goal: String = ""
)

data class Kbju(val calories: Int, val proteins: Int, val fats: Int, val carbs: Int)

private fun calculateKbjuPlan(
    gender: String,
    age: Int,
    height: Int,
    weight: Int,
    activity: String,
    goal: String
): Kbju {
    val bmr = if (gender == "male")
        10 * weight + 6.25 * height - 5 * age + 5
    else
        10 * weight + 6.25 * height - 5 * age - 161
    val activityFactor = when (activity) {
        "sedentary" -> 1.2
        "light" -> 1.375
        "moderate" -> 1.55
        "active" -> 1.725
        "very_active" -> 1.9
        else -> 1.2
    }
    val goalFactor = when (goal) {
        "lose" -> 0.85
        "gain" -> 1.15
        else -> 1.0
    }
    val targetCalories = (bmr * activityFactor * goalFactor).roundToInt()
    val proteinG = max((1.8 * weight).roundToInt(), 1)
    val fatG = max((0.9 * weight).roundToInt(), 1)
    val carbsG = max(((targetCalories - proteinG * 4 - fatG * 9) / 4.0).roundToInt(), 0)
    return Kbju(targetCalories, proteinG, fatG, carbsG)
}

@Composable
fun GenderSelectScreen(selected: String, onSelect: (String) -> Unit, onContinue: () -> Unit) {
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.select_gender_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(24.dp))
                LanguageButton(
                    text = stringResource(R.string.gender_male),
                    isSelected = selected == "male"
                ) { onSelect("male") }
                LanguageButton(
                    text = stringResource(R.string.gender_female),
                    isSelected = selected == "female"
                ) { onSelect("female") }
            }
            BottomContinueButton(enabled = true, onClick = { if (selected.isNotBlank()) onContinue() })
        }
    }
}

@Composable
fun AgeInputScreen(ageText: String, onChange: (String) -> Unit, onContinue: () -> Unit) {
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.enter_age_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { onChange(onlyDigits(it)) },
                    label = { Text(stringResource(R.string.age_years)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BottomContinueButton(enabled = true, onClick = { if (ageText.isNotBlank()) onContinue() })
        }
    }
}

@Composable
fun WeightInputScreen(weightText: String, onChange: (String) -> Unit, onContinue: () -> Unit) {
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.enter_weight_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { onChange(onlyDigits(it)) },
                    label = { Text(stringResource(R.string.weight_kg)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BottomContinueButton(enabled = true, onClick = { if (weightText.isNotBlank()) onContinue() })
        }
    }
}

@Composable
fun HeightInputScreen(heightText: String, onChange: (String) -> Unit, onContinue: () -> Unit) {
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.enter_height_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { onChange(onlyDigits(it)) },
                    label = { Text(stringResource(R.string.height_cm)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BottomContinueButton(enabled = true, onClick = { if (heightText.isNotBlank()) onContinue() })
        }
    }
}

@Composable
fun ChoiceButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF64B5F6) else Color(0xFFFFF3E0)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(72.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF4E342E),
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ActivitySelectScreen(selected: String, onSelect: (String) -> Unit, onContinue: () -> Unit) {
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(120.dp))
                Text(
                    text = stringResource(R.string.select_activity_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(16.dp))
                ChoiceButton(
                    title = stringResource(R.string.activity_sedentary),
                    subtitle = stringResource(R.string.activity_sedentary_hint),
                    isSelected = selected == "sedentary"
                ) { onSelect("sedentary") }
                ChoiceButton(
                    title = stringResource(R.string.activity_light),
                    subtitle = stringResource(R.string.activity_light_hint),
                    isSelected = selected == "light"
                ) { onSelect("light") }
                ChoiceButton(
                    title = stringResource(R.string.activity_moderate),
                    subtitle = stringResource(R.string.activity_moderate_hint),
                    isSelected = selected == "moderate"
                ) { onSelect("moderate") }
                ChoiceButton(
                    title = stringResource(R.string.activity_active),
                    subtitle = stringResource(R.string.activity_active_hint),
                    isSelected = selected == "active"
                ) { onSelect("active") }
                ChoiceButton(
                    title = stringResource(R.string.activity_very_active),
                    subtitle = stringResource(R.string.activity_very_active_hint),
                    isSelected = selected == "very_active"
                ) { onSelect("very_active") }
            }
            BottomContinueButton(enabled = true, onClick = { if (selected.isNotBlank()) onContinue() })
        }
    }
}

@Composable
fun GoalSelectScreen(selected: String, onSelect: (String) -> Unit, onContinue: () -> Unit) {
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.select_goal_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(16.dp))
                LanguageButton(
                    text = stringResource(R.string.goal_lose),
                    isSelected = selected == "lose"
                ) { onSelect("lose") }
                LanguageButton(
                    text = stringResource(R.string.goal_maintain),
                    isSelected = selected == "maintain"
                ) { onSelect("maintain") }
                LanguageButton(
                    text = stringResource(R.string.goal_gain),
                    isSelected = selected == "gain"
                ) { onSelect("gain") }
            }
            BottomContinueButton(enabled = true, onClick = { if (selected.isNotBlank()) onContinue() })
        }
    }
}

@Composable
fun KbjuResultScreen(result: Kbju, onFinish: () -> Unit) {
    BackgroundScreen {
        Box(Modifier.fillMaxSize()) {
            AppHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 120.dp)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.kbju_result_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Spacer(Modifier.height(16.dp))
                Text(text = "${stringResource(R.string.calories_kcal)}: ${result.calories}", fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = "${stringResource(R.string.proteins_g)}: ${result.proteins}", fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = "${stringResource(R.string.fats_g)}: ${result.fats}", fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = "${stringResource(R.string.carbs_g)}: ${result.carbs}", fontSize = 20.sp)
            }
            BottomContinueButton(label = stringResource(R.string.button_thanks), onClick = onFinish)
        }
    }
}

@Composable
fun LanguageButton(
    text: String,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF64B5F6) else Color(0xFFFFF3E0)
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(55.dp)
    ) {
        Text(
            text = text,
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
        val onboardingCompleted = UserStorage.isOnboardingCompleted(this)
        setContent {
            TrackerControlTheme {
                var currentScreen by remember { mutableStateOf(if (onboardingCompleted) "done" else "language") }
                val ctx = LocalContext.current
                var calc by remember { mutableStateOf(CalcInput()) }
                var ageText by remember { mutableStateOf("") }
                var weightText by remember { mutableStateOf("") }
                var heightText by remember { mutableStateOf("") }
                Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        "language" -> LanguageSelectionScreen(
                            selectedLangCode = langCode,
                            onLanguageSelected = { code -> setLocale(this, code, restartActivity = true) },
                            onContinueClicked = { currentScreen = "name" }
                        )
                        "name" -> NameInputScreen(
                            onNameEntered = { name ->
                                val user = UserStorage.loadUser(this).copy(name = name)
                                UserStorage.saveUser(this, user)
                                currentScreen = "kbjuQuestion"
                            }
                        )
                        "kbjuQuestion" -> {
                            val user = UserStorage.loadUser(this)
                            KbjuQuestionScreen(
                                name = user.name,
                                onYesEnter = { currentScreen = "kbjuInput" },
                                onNoCalculate = {
                                    calc = CalcInput()
                                    ageText = ""
                                    weightText = ""
                                    heightText = ""
                                    currentScreen = "calcGender"
                                }
                            )
                        }
                        "kbjuInput" -> {
                            val user = UserStorage.loadUser(this)
                            KbjuInputScreen(
                                initialCalories = user.dailyCalories,
                                initialProteins = user.dailyProteins,
                                initialFats = user.dailyFats,
                                initialCarbs = user.dailyCarbs
                            ) { cal, p, f, c ->
                                val updated = user.copy(
                                    dailyCalories = cal,
                                    dailyProteins = p,
                                    dailyFats = f,
                                    dailyCarbs = c
                                )
                                UserStorage.saveUser(ctx, updated)
                                UserStorage.setOnboardingCompleted(ctx, true)
                                currentScreen = "done"
                            }
                        }
                        "calcGender" -> GenderSelectScreen(
                            selected = calc.gender,
                            onSelect = { calc = calc.copy(gender = it) },
                            onContinue = { currentScreen = "calcAge" }
                        )
                        "calcAge" -> AgeInputScreen(
                            ageText = ageText,
                            onChange = { ageText = it },
                            onContinue = {
                                val age = ageText.toIntOrNull() ?: 0
                                if (age > 0) {
                                    calc = calc.copy(age = age)
                                    currentScreen = "calcWeight"
                                }
                            }
                        )
                        "calcWeight" -> WeightInputScreen(
                            weightText = weightText,
                            onChange = { weightText = it },
                            onContinue = {
                                val w = weightText.toIntOrNull() ?: 0
                                if (w > 0) {
                                    calc = calc.copy(weight = w)
                                    currentScreen = "calcHeight"
                                }
                            }
                        )
                        "calcHeight" -> HeightInputScreen(
                            heightText = heightText,
                            onChange = { heightText = it },
                            onContinue = {
                                val h = heightText.toIntOrNull() ?: 0
                                if (h > 0) {
                                    calc = calc.copy(height = h)
                                    currentScreen = "calcActivity"
                                }
                            }
                        )
                        "calcActivity" -> ActivitySelectScreen(
                            selected = calc.activity,
                            onSelect = { calc = calc.copy(activity = it) },
                            onContinue = { currentScreen = "calcGoal" }
                        )
                        "calcGoal" -> GoalSelectScreen(
                            selected = calc.goal,
                            onSelect = { calc = calc.copy(goal = it) },
                            onContinue = {
                                if (calc.gender.isNotBlank() && calc.age > 0 && calc.weight > 0 &&
                                    calc.height > 0 && calc.activity.isNotBlank() && calc.goal.isNotBlank()
                                ) {
                                    val res = calculateKbjuPlan(
                                        gender = calc.gender,
                                        age = calc.age,
                                        height = calc.height,
                                        weight = calc.weight,
                                        activity = calc.activity,
                                        goal = calc.goal
                                    )
                                    val user0 = UserStorage.loadUser(this)
                                    val user = user0.copy(
                                        gender = calc.gender,
                                        age = calc.age,
                                        height = calc.height,
                                        weight = calc.weight,
                                        activityLevel = calc.activity,
                                        goal = calc.goal,
                                        dailyCalories = res.calories,
                                        dailyProteins = res.proteins,
                                        dailyFats = res.fats,
                                        dailyCarbs = res.carbs
                                    )
                                    UserStorage.saveUser(ctx, user)
                                    currentScreen = "calcResult"
                                }
                            }
                        )
                        "calcResult" -> {
                            val user = UserStorage.loadUser(this)
                            KbjuResultScreen(
                                result = Kbju(
                                    user.dailyCalories,
                                    user.dailyProteins,
                                    user.dailyFats,
                                    user.dailyCarbs
                                ),
                                onFinish = {
                                    UserStorage.setOnboardingCompleted(ctx, true)
                                    currentScreen = "done"
                                }
                            )
                        }
                        "done" -> {
                            val user = UserStorage.loadUser(this)
                            Greeting(user.name)
                        }
                    }
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
