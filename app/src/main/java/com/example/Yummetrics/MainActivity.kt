package com.example.Yummetrics

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Yummetrics.ui.theme.TrackerControlTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

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

data class FoodEntry(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val calories: Int,
    val proteins: Int,
    val fats: Int,
    val carbs: Int,
    val quantityGrams: Int,
    val ts: Long = System.currentTimeMillis()
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

data class DayStats(val calories: Int = 0, val proteins: Int = 0, val fats: Int = 0, val carbs: Int = 0)

/* ===== История для графиков ===== */

data class DayTotals(
    val dateKey: String,
    val weight: Float? = null,
    val calories: Int? = null,
    val proteins: Int? = null,
    val fats: Int? = null,
    val carbs: Int? = null
)

object HistoryStorage {
    private const val PREFS = "history_prefs"
    private const val K_DAYS = "days_json"

    private fun sdfKey(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun sdfLabel(): SimpleDateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

    fun todayKey(): String = sdfKey().format(Date())

    fun labelFromKey(key: String): String {
        return try {
            val date = sdfKey().parse(key)
            if (date != null) sdfLabel().format(date) else key
        } catch (_: Exception) { key }
    }

    fun getAll(context: Context): MutableList<DayTotals> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arrStr = p.getString(K_DAYS, "[]") ?: "[]"
        val arr = JSONArray(arrStr)
        val list = ArrayList<DayTotals>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                DayTotals(
                    dateKey = o.getString("date"),
                    weight = if (o.has("w") && !o.isNull("w")) o.getDouble("w").toFloat() else null,
                    calories = if (o.has("cal") && !o.isNull("cal")) o.getInt("cal") else null,
                    proteins = if (o.has("p") && !o.isNull("p")) o.getInt("p") else null,
                    fats = if (o.has("f") && !o.isNull("f")) o.getInt("f") else null,
                    carbs = if (o.has("c") && !o.isNull("c")) o.getInt("c") else null
                )
            )
        }
        return list
    }

    private fun saveAll(context: Context, list: List<DayTotals>) {
        val arr = JSONArray()
        list.forEach { e ->
            val o = JSONObject()
            o.put("date", e.dateKey)
            if (e.weight != null) o.put("w", e.weight.toDouble()) else o.put("w", JSONObject.NULL)
            if (e.calories != null) o.put("cal", e.calories) else o.put("cal", JSONObject.NULL)
            if (e.proteins != null) o.put("p", e.proteins) else o.put("p", JSONObject.NULL)
            if (e.fats != null) o.put("f", e.fats) else o.put("f", JSONObject.NULL)
            if (e.carbs != null) o.put("c", e.carbs) else o.put("c", JSONObject.NULL)
            arr.put(o)
        }
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putString(K_DAYS, arr.toString()).apply()
    }

    fun upsertDay(
        context: Context,
        dateKey: String,
        weight: Float? = null,
        calories: Int? = null,
        proteins: Int? = null,
        fats: Int? = null,
        carbs: Int? = null
    ) {
        val list = getAll(context)
        val idx = list.indexOfFirst { it.dateKey == dateKey }
        if (idx >= 0) {
            val old = list[idx]
            list[idx] = DayTotals(
                dateKey = dateKey,
                weight = weight ?: old.weight,
                calories = calories ?: old.calories,
                proteins = proteins ?: old.proteins,
                fats = fats ?: old.fats,
                carbs = carbs ?: old.carbs
            )
        } else {
            list.add(
                DayTotals(
                    dateKey = dateKey,
                    weight = weight,
                    calories = calories,
                    proteins = proteins,
                    fats = fats,
                    carbs = carbs
                )
            )
        }
        saveAll(context, list.sortedBy { it.dateKey })
    }

    fun recordMacrosFromStats(context: Context, cutoffMillis: Long, stats: DayStats) {
        val key = dateKeyFromCutoff(cutoffMillis)
        upsertDay(
            context,
            dateKey = key,
            calories = stats.calories,
            proteins = stats.proteins,
            fats = stats.fats,
            carbs = stats.carbs
        )
    }

    private fun dateKeyFromCutoff(cutoffMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = cutoffMillis - 1_000L
        return sdfKey().format(Date(cal.timeInMillis))
    }

    fun seriesLastDays(
        context: Context,
        days: Int,
        includeTodayStats: DayStats?
    ): List<DayTotals> {
        val all = getAll(context).toMutableList()
        if (includeTodayStats != null) {
            val today = todayKey()
            val idx = all.indexOfFirst { it.dateKey == today }
            val d = if (idx >= 0) all[idx] else DayTotals(today)
            val merged = d.copy(
                calories = includeTodayStats.calories,
                proteins = includeTodayStats.proteins,
                fats = includeTodayStats.fats,
                carbs = includeTodayStats.carbs
            )
            if (idx >= 0) all[idx] = merged else all.add(merged)
        }
        val sorted = all.sortedBy { it.dateKey }
        return if (sorted.size <= days) sorted else sorted.takeLast(days)
    }
}

/* ===== Текущая статистика дня и ночной сброс ===== */

object DailyStatsStorage {
    private const val PREFS = "daily_stats"
    private const val K_CAL = "cal"
    private const val K_P = "p"
    private const val K_F = "f"
    private const val K_C = "c"
    private const val K_LAST_CUTOFF = "last_cutoff_ms"
    private const val K_ENTRIES = "entries_json"

    fun get(context: Context): DayStats {
        ensureDailyReset(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DayStats(
            calories = p.getInt(K_CAL, 0),
            proteins = p.getInt(K_P, 0),
            fats = p.getInt(K_F, 0),
            carbs = p.getInt(K_C, 0)
        )
    }

    fun getEntries(context: Context): List<FoodEntry> {
        ensureDailyReset(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arrStr = p.getString(K_ENTRIES, "[]") ?: "[]"
        val arr = JSONArray(arrStr)
        val list = ArrayList<FoodEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                FoodEntry(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    calories = o.optInt("cal"),
                    proteins = o.optInt("p"),
                    fats = o.optInt("f"),
                    carbs = o.optInt("c"),
                    quantityGrams = o.optInt("qty"),
                    ts = o.optLong("ts")
                )
            )
        }
        return list
    }

    fun addEntry(context: Context, name: String, calPer100: Int, pPer100: Int, fPer100: Int, cPer100: Int, qtyG: Int) {
        ensureDailyReset(context)
        val factor = qtyG.coerceAtLeast(0) / 100f
        val cal = (calPer100 * factor).roundToInt()
        val p = (pPer100 * factor).roundToInt()
        val f = (fPer100 * factor).roundToInt()
        val c = (cPer100 * factor).roundToInt()
        val list = getEntries(context).toMutableList()
        val entry = FoodEntry(
            name = name.ifBlank { context.getString(R.string.food_name_default) },
            calories = cal, proteins = p, fats = f, carbs = c,
            quantityGrams = qtyG.coerceAtLeast(0)
        )
        list.add(0, entry)
        saveEntries(context, list)
        add(context, cal, p, f, c)
    }

    fun removeEntry(context: Context, id: Long) {
        ensureDailyReset(context)
        val list = getEntries(context).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val e = list[idx]
            list.removeAt(idx)
            saveEntries(context, list)
            add(context, -e.calories, -e.proteins, -e.fats, -e.carbs)
        }
    }

    fun add(context: Context, cal: Int, pr: Int, fa: Int, ca: Int) {
        ensureDailyReset(context)
        val old = get(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putInt(K_CAL, max(0, old.calories + cal))
            .putInt(K_P, max(0, old.proteins + pr))
            .putInt(K_F, max(0, old.fats + fa))
            .putInt(K_C, max(0, old.carbs + ca))
            .apply()
    }

    fun resetToZero(context: Context, setCutoffTo: Long = mostRecentCutoffMillis()) {
        val oldStats = get(context)
        HistoryStorage.recordMacrosFromStats(context, setCutoffTo, oldStats)

        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putInt(K_CAL, 0)
            .putInt(K_P, 0)
            .putInt(K_F, 0)
            .putInt(K_C, 0)
            .putString(K_ENTRIES, "[]")
            .putLong(K_LAST_CUTOFF, setCutoffTo)
            .apply()
    }

    fun ensureDailyReset(context: Context) {
        val last = lastCutoffMillis(context)
        val currentCutoff = mostRecentCutoffMillis()
        if (last < currentCutoff) resetToZero(context, currentCutoff)
    }

    fun scheduleNextResetAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyResetReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getBroadcast(context, 1010, intent, flags)
        val triggerAt = nextCutoffMillis()
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun saveEntries(context: Context, list: List<FoodEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("cal", e.calories)
                    put("p", e.proteins)
                    put("f", e.fats)
                    put("c", e.carbs)
                    put("qty", e.quantityGrams)
                    put("ts", e.ts)
                }
            )
        }
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putString(K_ENTRIES, arr.toString()).apply()
    }

    private fun lastCutoffMillis(context: Context): Long {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getLong(K_LAST_CUTOFF, 0L)
    }

    fun mostRecentCutoffMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = System.currentTimeMillis()
        var cutoff = cal.timeInMillis
        if (now < cutoff) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cutoff = cal.timeInMillis
        }
        return cutoff
    }

    fun nextCutoffMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = System.currentTimeMillis()
        var next = cal.timeInMillis
        if (now >= next) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            next = cal.timeInMillis
        }
        return next
    }
}

class DailyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DailyStatsStorage.ensureDailyReset(context)
        DailyStatsStorage.scheduleNextResetAlarm(context)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
    ) {
        val bgPainter = runCatching { painterResource(id = R.drawable.with_sun) }.getOrNull()
        if (bgPainter != null) {
            Image(
                painter = bgPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}

@Composable
fun PlainScreen(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
    ) { content() }
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
            disabledContainerColor = Color(0xFFFFC107),
            disabledContentColor = Color(0xFF212121)
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = text, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LanguageButton(
    text: String,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFFFB300) else Color(0xFFFFCA28)
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
            text,
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SelectableCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0xFFFFB300) else Color(0xFFFFCA28)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color(0xFF5D4037), fontSize = 13.sp)
            }
        }
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
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
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
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
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
                    .padding(top = 180.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.hello_name, name.ifBlank { "User" }),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.kbju_question),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
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
    onFinish: (cal: Int, p: Int, f: Int, c: Int) -> Unit,
    plainStyle: Boolean = false
) {
    var calories by remember { mutableStateOf(if (initialCalories > 0) initialCalories.toString() else "") }
    var proteins by remember { mutableStateOf(if (initialProteins > 0) initialProteins.toString() else "") }
    var fats by remember { mutableStateOf(if (initialFats > 0) initialFats.toString() else "") }
    var carbs by remember { mutableStateOf(if (initialCarbs > 0) initialCarbs.toString() else "") }
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    val valid = calories.isNotBlank() && proteins.isNotBlank() && fats.isNotBlank() && carbs.isNotBlank()

    val Container: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        if (plainStyle) { { content -> PlainScreen(content) } } else { { content -> BackgroundScreen(content) } }

    Container {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!plainStyle) {
                AppHeader(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 120.dp)
                )
            }
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
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
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

data class CalcState(
    val gender: String? = null,
    val weight: String = "",
    val height: String = "",
    val age: String = "",
    val activity: String? = null,
    val goal: String? = null
)

@Composable
fun CalcGenderScreen(plainStyle: Boolean, value: String?, onSelect: (String) -> Unit, onNext: () -> Unit) {
    val Container: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        if (plainStyle) { { content -> PlainScreen(content) } } else { { content -> BackgroundScreen(content) } }

    Container {
        Box(Modifier.fillMaxSize()) {
            if (!plainStyle) {
                AppHeader(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 120.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = if (plainStyle) 80.dp else 160.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(stringResource(R.string.calc_gender_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                LanguageButton(text = stringResource(R.string.gender_male), isSelected = value == "male") { onSelect("male") }
                LanguageButton(text = stringResource(R.string.gender_female), isSelected = value == "female") { onSelect("female") }
            }
            BottomContinueButton(enabled = true, onClick = { if (value != null) onNext() })
        }
    }
}

@Composable
fun CalcStatsScreen(plainStyle: Boolean, weight: String, height: String, age: String, onChange: (w: String, h: String, a: String) -> Unit, onNext: () -> Unit) {
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    val valid = weight.isNotBlank() && height.isNotBlank() && age.isNotBlank()
    val Container: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        if (plainStyle) { { content -> PlainScreen(content) } } else { { content -> BackgroundScreen(content) } }

    Container {
        Box(Modifier.fillMaxSize()) {
            if (!plainStyle) {
                AppHeader(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 120.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = if (plainStyle) 80.dp else 160.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(stringResource(R.string.calc_stats_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = weight, onValueChange = { onChange(onlyDigits(it), height, age) },
                    label = { Text(stringResource(R.string.weight_kg)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = height, onValueChange = { onChange(weight, onlyDigits(it), age) },
                    label = { Text(stringResource(R.string.height_cm)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = age, onValueChange = { onChange(weight, height, onlyDigits(it)) },
                    label = { Text(stringResource(R.string.age_years)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            BottomContinueButton(enabled = true, onClick = { if (valid) onNext() })
        }
    }
}

@Composable
fun CalcActivityScreen(plainStyle: Boolean, value: String?, onSelect: (String) -> Unit, onNext: () -> Unit) {
    val Container: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        if (plainStyle) { { content -> PlainScreen(content) } } else { { content -> BackgroundScreen(content) } }

    Container {
        Box(Modifier.fillMaxSize()) {
            if (!plainStyle) {
                AppHeader(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 120.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = if (plainStyle) 80.dp else 160.dp)
                    .padding(bottom = BottomContinueReservedSpace)
            ) {
                Text(stringResource(R.string.calc_activity_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                SelectableCard(
                    title = stringResource(R.string.activity_sedentary),
                    subtitle = stringResource(R.string.activity_sedentary_desc),
                    selected = value == "sedentary",
                    onClick = { onSelect("sedentary") }
                )
                SelectableCard(
                    title = stringResource(R.string.activity_light),
                    subtitle = stringResource(R.string.activity_light_desc),
                    selected = value == "light",
                    onClick = { onSelect("light") }
                )
                SelectableCard(
                    title = stringResource(R.string.activity_moderate),
                    subtitle = stringResource(R.string.activity_moderate_desc),
                    selected = value == "moderate",
                    onClick = { onSelect("moderate") }
                )
                SelectableCard(
                    title = stringResource(R.string.activity_high),
                    subtitle = stringResource(R.string.activity_high_desc),
                    selected = value == "high",
                    onClick = { onSelect("high") }
                )
                SelectableCard(
                    title = stringResource(R.string.activity_very_high),
                    subtitle = stringResource(R.string.activity_very_high_desc),
                    selected = value == "very_high",
                    onClick = { onSelect("very_high") }
                )
            }
            BottomContinueButton(enabled = true, onClick = { if (value != null) onNext() })
        }
    }
}

@Composable
fun CalcGoalScreen(plainStyle: Boolean, value: String?, onSelect: (String) -> Unit, onNext: () -> Unit) {
    val Container: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        if (plainStyle) { { content -> PlainScreen(content) } } else { { content -> BackgroundScreen(content) } }

    Container {
        Box(Modifier.fillMaxSize()) {
            if (!plainStyle) {
                AppHeader(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 120.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = if (plainStyle) 96.dp else 176.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                verticalArrangement = Arrangement.Top
            ) {
                Text(stringResource(R.string.calc_goal_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                LanguageButton(text = stringResource(R.string.goal_lose), isSelected = value == "lose") { onSelect("lose") }
                LanguageButton(text = stringResource(R.string.goal_maintain), isSelected = value == "maintain") { onSelect("maintain") }
                LanguageButton(text = stringResource(R.string.goal_gain), isSelected = value == "gain") { onSelect("gain") }
            }
            BottomContinueButton(enabled = true, onClick = { if (value != null) onNext() })
        }
    }
}

@Composable
fun CalcResultScreen(
    plainStyle: Boolean,
    calories: Int, proteins: Int, fats: Int, carbs: Int,
    onFinish: () -> Unit
) {
    val Container: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        if (plainStyle) { { content -> PlainScreen(content) } } else { { content -> BackgroundScreen(content) } }

    Container {
        Box(Modifier.fillMaxSize()) {
            if (!plainStyle) {
                AppHeader(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 120.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.calc_result_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text("${stringResource(R.string.calories_kcal)}: $calories", color = Color(0xFF3E2723), fontSize = 18.sp)
                Text("${stringResource(R.string.proteins_g)}: $proteins", color = Color(0xFF3E2723), fontSize = 18.sp)
                Text("${stringResource(R.string.fats_g)}: $fats", color = Color(0xFF3E2723), fontSize = 18.sp)
                Text("${stringResource(R.string.carbs_g)}: $carbs", color = Color(0xFF3E2723), fontSize = 18.sp)
            }
            BottomContinueButton(
                enabled = true,
                onClick = onFinish,
                label = stringResource(R.string.button_thanks)
            )
        }
    }
}

/* ===== Главная, список, графики, настройки ===== */

@Composable
fun MainHost(onEditKbju: () -> Unit, onChangeLanguage: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        DailyStatsStorage.ensureDailyReset(ctx)
        DailyStatsStorage.scheduleNextResetAlarm(ctx)
    }
    Scaffold(
        containerColor = Color(0xFFFFF8E1),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFFE082)) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null, tint = Color(0xFF3E2723)) },
                    label = { Text(stringResource(R.string.tab_home), color = Color(0xFF3E2723)) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Color(0xFF3E2723)) },
                    label = { Text(stringResource(R.string.tab_charts), color = Color(0xFF3E2723)) }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFF3E2723)) },
                    label = { Text(stringResource(R.string.tab_settings), color = Color(0xFF3E2723)) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeDashboard()
                1 -> ChartsScreen()
                2 -> SettingsRoot(onEditKbju = onEditKbju, onChangeLanguage = onChangeLanguage)
            }
        }
    }
}

@Composable
fun HomeDashboard() {
    val ctx = LocalContext.current
    var stats by remember { mutableStateOf(DailyStatsStorage.get(ctx)) }
    var entries by remember { mutableStateOf(DailyStatsStorage.getEntries(ctx)) }
    val user = UserStorage.loadUser(ctx)
    val targetCal = max(1, user.dailyCalories)
    val leftCal = max(0, targetCal - stats.calories)
    val progress = min(1f, stats.calories.toFloat() / targetCal.toFloat())
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CalorieRing(
                valueLeft = leftCal,
                progress = progress,
                diameter = 260.dp,
                stroke = 18.dp
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MacroCard(
                title = stringResource(R.string.protein_title),
                current = stats.proteins,
                target = max(1, user.dailyProteins),
                emoji = "🍗",
                modifier = Modifier.weight(1f)
            )
            MacroCard(
                title = stringResource(R.string.fats_title),
                current = stats.fats,
                target = max(1, user.dailyFats),
                emoji = "🥑",
                modifier = Modifier.weight(1f)
            )
            MacroCard(
                title = stringResource(R.string.carbs_title),
                current = stats.carbs,
                target = max(1, user.dailyCarbs),
                emoji = "🍚",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { showSheet = true },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.add_food), color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.today_title),
            color = Color(0xFF3E2723),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp)
        )
        Spacer(Modifier.height(4.dp))
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.today_empty),
                color = Color(0xFF5D4037),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { e ->
                    FoodEntryRow(
                        entry = e,
                        onDelete = {
                            DailyStatsStorage.removeEntry(ctx, e.id)
                            entries = DailyStatsStorage.getEntries(ctx)
                            stats = DailyStatsStorage.get(ctx)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showSheet) {
        AddFoodSheet(
            onClose = { showSheet = false },
            onSave = { name, calPer100, pPer100, fPer100, cPer100, qtyG ->
                DailyStatsStorage.addEntry(ctx, name, calPer100, pPer100, fPer100, cPer100, qtyG)
                entries = DailyStatsStorage.getEntries(ctx)
                stats = DailyStatsStorage.get(ctx)
                showSheet = false
            }
        )
    }
}

@Composable
fun FoodEntryRow(entry: FoodEntry, onDelete: () -> Unit) {
    val timeStr = remember(entry.ts) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.ts))
    }
    Surface(
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFFECB3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🍽", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723))
                    if (entry.quantityGrams > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("${entry.quantityGrams} g", color = Color(0xFF5D4037), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.entry_macros, entry.calories, entry.proteins, entry.fats, entry.carbs),
                    color = Color(0xFF5D4037),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(timeStr, color = Color(0xFF5D4037), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.delete), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CalorieRing(
    valueLeft: Int,
    progress: Float,
    diameter: Dp,
    stroke: Dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            drawArc(
                color = Color(0xFFFFE082),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                size = this.size
            )
            drawArc(
                color = Color(0xFFFFC107),
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                size = this.size
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                valueLeft.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(
                stringResource(R.string.kcal_left),
                fontSize = 18.sp,
                color = Color(0xFF3E2723)
            )
        }
    }
}

@Composable
fun MacroCard(title: String, current: Int, target: Int, emoji: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF3E0), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFFFECB3), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$current / $target g",
            color = Color(0xFF3E2723),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    onClose: () -> Unit,
    onSave: (name: String, calPer100: Int, pPer100: Int, fPer100: Int, cPer100: Int, qtyG: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cal by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    val onlyDigits: (String) -> String = { it.filter { ch -> ch.isDigit() } }
    val valid = cal.isNotBlank() && p.isNotBlank() && f.isNotBlank() && c.isNotBlank() && qty.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFFFFF8E1)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.add_food), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.per100_note), color = Color(0xFF5D4037), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.food_name_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = cal, onValueChange = { cal = onlyDigits(it) },
                label = { Text(stringResource(R.string.calories_kcal)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = p, onValueChange = { p = onlyDigits(it) },
                label = { Text(stringResource(R.string.proteins_g)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = f, onValueChange = { f = onlyDigits(it) },
                label = { Text(stringResource(R.string.fats_g)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = c, onValueChange = { c = onlyDigits(it) },
                label = { Text(stringResource(R.string.carbs_g)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = qty, onValueChange = { qty = onlyDigits(it) },
                label = { Text(stringResource(R.string.quantity_g)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.button_cancel)) }
                Button(
                    onClick = {
                        val calI = cal.toIntOrNull() ?: 0
                        val pI = p.toIntOrNull() ?: 0
                        val fI = f.toIntOrNull() ?: 0
                        val cI = c.toIntOrNull() ?: 0
                        val qtyI = qty.toIntOrNull() ?: 0
                        onSave(name, calI, pI, fI, cI, qtyI)
                    },
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                ) { Text(stringResource(R.string.button_add), color = Color.Black, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SettingsRoot(onEditKbju: () -> Unit, onChangeLanguage: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingTile(
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_sub),
            onClick = onChangeLanguage
        )
        SettingTile(
            title = stringResource(R.string.settings_kbju),
            subtitle = stringResource(R.string.settings_kbju_sub),
            onClick = onEditKbju
        )
        SettingTile(
            title = stringResource(R.string.settings_reset_now),
            subtitle = stringResource(R.string.settings_reset_now_sub),
            onClick = {
                DailyStatsStorage.resetToZero(ctx)
                DailyStatsStorage.scheduleNextResetAlarm(ctx)
            }
        )
    }
}

@Composable
fun SettingTile(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFE082),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF5D4037))
        }
    }
}

/* ====== ЭКРАН ГРАФИКОВ ====== */

enum class Metric { WEIGHT, CAL, PROT, FAT, CARB }

@Composable
fun ChartsScreen() {
    val ctx = LocalContext.current

    var showAddWeight by remember { mutableStateOf(false) }
    var weights by remember { mutableStateOf(WeightStorage.getAll(ctx)) }

    val dateFmt = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

    val weightPoints = remember(weights) { weights.takeLast(14) }
    val weightValues = remember(weightPoints) { weightPoints.map { it.second } }
    val weightLabels = remember(weightPoints) { weightPoints.map { dateFmt.format(Date(it.first)) } }

    val today = DailyStatsStorage.get(ctx)
    val todayLabel = dateFmt.format(Date())
    val calValues = listOf(today.calories.toFloat())
    val protValues = listOf(today.proteins.toFloat())
    val fatValues = listOf(today.fats.toFloat())
    val carbValues = listOf(today.carbs.toFloat())
    val singleLabel = listOf(todayLabel)

    Scaffold(
        containerColor = Color(0xFFFFF8E1),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF8E1))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { showAddWeight = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                ) {
                    Text(
                        text = stringResource(R.string.add_weight),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFFF8E1))
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            ChartCard(title = "⚖️  " + stringResource(R.string.charts_title_weight)) {
                if (weightValues.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = Color(0xFF5D4037))
                } else {
                    LineChart(
                        values = weightValues,
                        xLabels = weightLabels,
                        height = 220.dp,
                        stroke = 3.dp,
                        yTicks = 5,
                        formatY = { v -> String.format(Locale.getDefault(), "%.1f", v) }
                    )
                }
            }

            ChartCard(title = "🔥  " + stringResource(R.string.charts_title_calories)) {
                if (calValues.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = Color(0xFF5D4037))
                } else {
                    LineChart(values = calValues, xLabels = singleLabel, height = 180.dp, stroke = 3.dp, yTicks = 4)
                }
            }

            ChartCard(title = "🍗  " + stringResource(R.string.charts_title_proteins)) {
                if (protValues.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = Color(0xFF5D4037))
                } else {
                    LineChart(values = protValues, xLabels = singleLabel, height = 180.dp, stroke = 3.dp, yTicks = 4)
                }
            }

            ChartCard(title = "🥑  " + stringResource(R.string.charts_title_fats)) {
                if (fatValues.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = Color(0xFF5D4037))
                } else {
                    LineChart(values = fatValues, xLabels = singleLabel, height = 180.dp, stroke = 3.dp, yTicks = 4)
                }
            }

            ChartCard(title = "🍚  " + stringResource(R.string.charts_title_carbs)) {
                if (carbValues.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = Color(0xFF5D4037))
                } else {
                    LineChart(values = carbValues, xLabels = singleLabel, height = 180.dp, stroke = 3.dp, yTicks = 4)
                }
            }

            Spacer(Modifier.height(84.dp))
        }
    }

    if (showAddWeight) {
        AddWeightSheet(
            onClose = { showAddWeight = false },
            onSave = { kg ->
                WeightStorage.addOrUpdateToday(ctx, kg)
                weights = WeightStorage.getAll(ctx)
                showAddWeight = false
            }
        )
    }
}

object WeightStorage {
    private const val PREFS = "weight_stats"
    private const val KEY = "weights_json"

    fun addOrUpdateToday(context: Context, kg: Float) {
        val all = getAll(context).toMutableList()
        val today = todayStartMillis()
        val idx = all.indexOfFirst { it.first == today }
        if (idx >= 0) {
            all[idx] = today to kg
        } else {
            all.add(today to kg)
        }
        saveAll(context, all)
    }

    fun getAll(context: Context): List<Pair<Long, Float>> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = p.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(json)
        val out = ArrayList<Pair<Long, Float>>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val d = o.optLong("d", 0L)
            val kg = o.optDouble("kg", 0.0).toFloat()
            if (d > 0) out.add(d to kg)
        }
        return out.sortedBy { it.first }
    }

    private fun saveAll(context: Context, list: List<Pair<Long, Float>>) {
        val arr = JSONArray()
        list.sortedBy { it.first }.forEach {
            arr.put(JSONObject().apply {
                put("d", it.first)
                put("kg", it.second.toDouble())
            })
        }
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putString(KEY, arr.toString()).apply()
    }

    private fun todayStartMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}

@Composable
fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF3E0), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/* >>> Новая версия LineChart с осями слева <<< */
@Composable
fun LineChart(
    values: List<Float>,
    xLabels: List<String>,
    height: Dp,
    stroke: Dp,
    yTicks: Int = 4,
    formatY: (Float) -> String = { it.roundToInt().toString() }
) {
    if (values.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_data), color = Color(0xFF5D4037))
        }
        return
    }

    val minV = values.minOrNull() ?: 0f
    val maxV = values.maxOrNull() ?: 0f
    val pad = ((maxV - minV).takeIf { it > 0f } ?: 1f) * 0.1f
    val vMin = minV - pad
    val vMax = maxV + pad

    val axisWidth = 48.dp
    val axisGap = 6.dp
    val gridColor = Color(0xFFFFE0B2)
    val lineColor = Color(0xFFFFA000)
    val pointColor = Color(0xFFFFC107)

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(axisWidth)
                    .height(height),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val ticks = (0..yTicks).map { i ->
                    val frac = i / yTicks.toFloat() // 0..1
                    val value = vMin + (vMax - vMin) * (1f - frac) // сверху vMax
                    formatY(value)
                }
                ticks.forEach { label ->
                    Text(label, color = Color(0xFF5D4037), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.width(axisGap))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
            ) {
                val w = size.width
                val h = size.height
                val strokePx = stroke.toPx()

                // горизонтальные линии сетки
                for (i in 0..yTicks) {
                    val y = h * (i / yTicks.toFloat())
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (values.size == 1) {
                    val yRatio = if ((vMax - vMin) == 0f) 0.5f else (values[0] - vMin) / (vMax - vMin)
                    val y = h * (1f - yRatio)
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = pointColor,
                        radius = strokePx * 1.1f,
                        center = androidx.compose.ui.geometry.Offset(w / 2f, y)
                    )
                } else {
                    var prev: androidx.compose.ui.geometry.Offset? = null
                    values.forEachIndexed { i, v ->
                        val x = i / (values.lastIndex.toFloat()) * w
                        val yRatio = if ((vMax - vMin) == 0f) 0.5f else (v - vMin) / (vMax - vMin)
                        val y = h * (1f - yRatio)
                        val pt = androidx.compose.ui.geometry.Offset(x, y)
                        if (prev != null) {
                            drawLine(
                                color = lineColor,
                                start = prev!!,
                                end = pt,
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round
                            )
                        }
                        prev = pt
                    }
                    values.forEachIndexed { i, v ->
                        val x = i / (values.lastIndex.toFloat()) * w
                        val yRatio = if ((vMax - vMin) == 0f) 0.5f else (v - vMin) / (vMax - vMin)
                        val y = h * (1f - yRatio)
                        drawCircle(
                            color = pointColor,
                            radius = strokePx * 1.1f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
            }
        }

        if (xLabels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Spacer(Modifier.width(axisWidth + axisGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (xLabels.size > 1) Arrangement.SpaceBetween else Arrangement.Start
                ) {
                    xLabels.forEach { lbl ->
                        Text(lbl, color = Color(0xFF5D4037), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWeightSheet(
    onClose: () -> Unit,
    onSave: (kg: Float) -> Unit
) {
    var kgText by remember { mutableStateOf("") }
    val onlyNum: (String) -> String = { s ->
        buildString {
            var dotUsed = false
            s.forEach { ch ->
                if (ch.isDigit()) append(ch)
                else if ((ch == '.' || ch == ',') && !dotUsed) {
                    append('.'); dotUsed = true
                }
            }
        }
    }
    val valid = kgText.toFloatOrNull()?.let { it > 0f && it < 500f } == true

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFFFFF8E1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_weight),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = kgText,
                onValueChange = { kgText = onlyNum(it) },
                label = { Text(stringResource(R.string.weight_kg)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.button_cancel)) }
                Button(
                    onClick = {
                        val v = kgText.toFloatOrNull() ?: 0f
                        if (v > 0f) onSave(v)
                    },
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                ) { Text(stringResource(R.string.button_save), color = Color.Black, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun KbjuSettingsEntryScreen(onYesEnter: () -> Unit, onNoCalculate: () -> Unit) {
    PlainScreen {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = 100.dp)
                    .padding(bottom = BottomContinueReservedSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.kbju_settings_entry_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                LanguageButton(text = stringResource(R.string.kbju_yes_enter)) { onYesEnter() }
                LanguageButton(text = stringResource(R.string.kbju_no_calculate)) { onNoCalculate() }
            }
        }
    }
}

@Composable
fun LanguageSettingsScreen(current: String, onSelect: (String) -> Unit) {
    PlainScreen {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.language_settings_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                Spacer(Modifier.height(24.dp))
                LanguageButton(text = "🇬🇧 " + stringResource(R.string.english), isSelected = current == "en") { onSelect("en") }
                LanguageButton(text = "🇷🇺 " + stringResource(R.string.russian), isSelected = current == "ru") { onSelect("ru") }
                LanguageButton(text = "🇵🇱 " + stringResource(R.string.polish), isSelected = current == "pl") { onSelect("pl") }
            }
        }
    }
}

fun calculateKbju(
    gender: String, age: Int, height: Int, weight: Int,
    activity: String, goal: String
): UserData {
    val bmr = if (gender == "male") {
        10 * weight + 6.25f * height - 5 * age + 5
    } else {
        10 * weight + 6.25f * height - 5 * age - 161
    }
    val activityMult = when (activity) {
        "sedentary" -> 1.2f
        "light" -> 1.375f
        "moderate" -> 1.55f
        "high" -> 1.725f
        "very_high" -> 1.9f
        else -> 1.2f
    }
    var calories = (bmr * activityMult).roundToInt()
    calories = when (goal) {
        "lose" -> (calories * 0.85f).roundToInt()
        "gain" -> (calories * 1.10f).roundToInt()
        else -> calories
    }
    calories = max(calories, 1200)
    val proteinPerKg = when (goal) {
        "gain" -> 1.8f
        "lose" -> 1.8f
        else -> 1.6f
    }
    val fatPerKg = 0.8f
    val proteinsG = (weight * proteinPerKg).roundToInt()
    val fatsG = (weight * fatPerKg).roundToInt()
    val kcalFromPF = proteinsG * 4 + fatsG * 9
    val carbsG = max(0, ((calories - kcalFromPF) / 4f).roundToInt())
    return UserData(
        gender = gender,
        age = age,
        height = height,
        weight = weight,
        activityLevel = activity,
        goal = goal,
        dailyCalories = calories,
        dailyProteins = proteinsG,
        dailyFats = fatsG,
        dailyCarbs = carbsG
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", "en") ?: "en"
        setLocale(this, langCode, restartActivity = false)
        val onboardingCompleted = UserStorage.isOnboardingCompleted(this)
        val activity = this@MainActivity
        setContent {
            TrackerControlTheme {
                var currentScreen by remember { mutableStateOf(if (onboardingCompleted) "main" else "language") }
                var calc by remember { mutableStateOf(CalcState()) }
                var calcPlainStyle by remember { mutableStateOf(false) }
                var calcResult by remember { mutableStateOf<UserData?>(null) }
                var calcMode by remember { mutableStateOf("onboarding") }
                Crossfade(targetState = currentScreen, label = "root") { screen ->
                    when (screen) {
                        "language" -> LanguageSelectionScreen(
                            selectedLangCode = langCode,
                            onLanguageSelected = { code -> setLocale(activity, code, restartActivity = true) },
                            onContinueClicked = { currentScreen = "name" }
                        )
                        "name" -> NameInputScreen(
                            onNameEntered = { name ->
                                val user = UserStorage.loadUser(activity).copy(name = name)
                                UserStorage.saveUser(activity, user)
                                currentScreen = "kbjuQuestion"
                            }
                        )
                        "kbjuQuestion" -> {
                            val user = UserStorage.loadUser(activity)
                            KbjuQuestionScreen(
                                name = user.name,
                                onYesEnter = { currentScreen = "kbjuInput" },
                                onNoCalculate = {
                                    calc = CalcState()
                                    calcPlainStyle = false
                                    calcMode = "onboarding"
                                    currentScreen = "calc_gender"
                                }
                            )
                        }
                        "kbjuInputMain" -> {
                            val user = UserStorage.loadUser(activity)
                            KbjuInputScreen(
                                initialCalories = user.dailyCalories,
                                initialProteins = user.dailyProteins,
                                initialFats = user.dailyFats,
                                initialCarbs = user.dailyCarbs,
                                onFinish = { cal, p, f, c ->
                                    val updated = user.copy(
                                        dailyCalories = cal,
                                        dailyProteins = p,
                                        dailyFats = f,
                                        dailyCarbs = c
                                    )
                                    UserStorage.saveUser(activity, updated)
                                    currentScreen = "main"
                                },
                                plainStyle = true
                            )
                        }
                        "main" -> MainHost(
                            onEditKbju = { currentScreen = "kbjuSettingsEntry" },
                            onChangeLanguage = { currentScreen = "languageSettings" }
                        )
                        "kbjuSettingsEntry" -> KbjuSettingsEntryScreen(
                            onYesEnter = { currentScreen = "kbjuInputMain" },
                            onNoCalculate = {
                                calc = CalcState()
                                calcPlainStyle = true
                                calcMode = "settings"
                                currentScreen = "calc_gender"
                            }
                        )
                        "kbjuInput" -> {
                            val user = UserStorage.loadUser(activity)
                            KbjuInputScreen(
                                initialCalories = user.dailyCalories,
                                initialProteins = user.dailyProteins,
                                initialFats = user.dailyFats,
                                initialCarbs = user.dailyCarbs,
                                onFinish = { cal, p, f, c ->
                                    val updated = user.copy(
                                        dailyCalories = cal,
                                        dailyProteins = p,
                                        dailyFats = f,
                                        dailyCarbs = c
                                    )
                                    UserStorage.saveUser(activity, updated)
                                    UserStorage.setOnboardingCompleted(activity, true)
                                    currentScreen = "main"
                                },
                                plainStyle = false
                            )
                        }
                        "languageSettings" -> LanguageSettingsScreen(current = langCode) { code ->
                            setLocale(activity, code, restartActivity = true)
                        }
                        "calc_gender" -> CalcGenderScreen(
                            plainStyle = calcPlainStyle,
                            value = calc.gender,
                            onSelect = { calc = calc.copy(gender = it) },
                            onNext = { currentScreen = "calc_stats" }
                        )
                        "calc_stats" -> CalcStatsScreen(
                            plainStyle = calcPlainStyle,
                            weight = calc.weight,
                            height = calc.height,
                            age = calc.age,
                            onChange = { w, h, a -> calc = calc.copy(weight = w, height = h, age = a) },
                            onNext = { currentScreen = "calc_activity" }
                        )
                        "calc_activity" -> CalcActivityScreen(
                            plainStyle = calcPlainStyle,
                            value = calc.activity,
                            onSelect = { calc = calc.copy(activity = it) },
                            onNext = { currentScreen = "calc_goal" }
                        )
                        "calc_goal" -> CalcGoalScreen(
                            plainStyle = calcPlainStyle,
                            value = calc.goal,
                            onSelect = { calc = calc.copy(goal = it) },
                            onNext = {
                                val g = calc.gender ?: "male"
                                val w = calc.weight.toIntOrNull() ?: 0
                                val h = calc.height.toIntOrNull() ?: 0
                                val a = calc.age.toIntOrNull() ?: 0
                                val act = calc.activity ?: "sedentary"
                                val goal = calc.goal ?: "maintain"
                                val result = calculateKbju(g, a, h, w, act, goal)
                                calcResult = result
                                currentScreen = "calc_result"
                            }
                        )
                        "calc_result" -> {
                            val res = calcResult ?: UserData()
                            CalcResultScreen(
                                plainStyle = calcPlainStyle,
                                calories = res.dailyCalories,
                                proteins = res.dailyProteins,
                                fats = res.dailyFats,
                                carbs = res.dailyCarbs
                            ) {
                                val old = UserStorage.loadUser(activity)
                                val updated = old.copy(
                                    gender = if (res.gender.isNotBlank()) res.gender else old.gender,
                                    age = if (res.age > 0) res.age else old.age,
                                    height = if (res.height > 0) res.height else old.height,
                                    weight = if (res.weight > 0) res.weight else old.weight,
                                    activityLevel = if (res.activityLevel.isNotBlank()) res.activityLevel else old.activityLevel,
                                    goal = if (res.goal.isNotBlank()) res.goal else old.goal,
                                    dailyCalories = res.dailyCalories,
                                    dailyProteins = res.dailyProteins,
                                    dailyFats = res.dailyFats,
                                    dailyCarbs = res.dailyCarbs
                                )
                                UserStorage.saveUser(activity, updated)
                                if (calcMode == "onboarding") {
                                    UserStorage.setOnboardingCompleted(activity, true)
                                }
                                currentScreen = "main"
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ===== Preview ===== */

@Preview(showBackground = true)
@Composable
fun PreviewRing() {
    TrackerControlTheme {
        CalorieRing(valueLeft = 1200, progress = 0.4f, diameter = 260.dp, stroke = 16.dp)
    }
}
