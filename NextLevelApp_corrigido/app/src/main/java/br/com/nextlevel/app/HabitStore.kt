package br.com.nextlevel.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

data class Habit(
    var id: Long,
    var name: String,
    var icon: String,
    var color: String,
    var type: String,      // "build" ou "quit"
    var freq: String,      // "diário" ou "semanal"
    var times: Int,
    var cat: String,
    var alarm: String?     // "HH:mm" ou null
)

data class TimeEntry(val hid: Long, val date: String, val min: Int)

object HabitStore {
    const val BLUE = "#3B82F6"
    val PALETTE = listOf(
        "azul" to "#3B82F6", "ciano" to "#22D3EE", "índigo" to "#818CF8",
        "verde" to "#22C55E", "vermelho" to "#EF4444", "roxo" to "#A855F7",
        "âmbar" to "#F59E0B", "rosa" to "#EC4899"
    )
    val GLYPHS = listOf("☺","⚡","✎","♒","↗","☾","♨","♥","★","♪","¤","☕","☀","⚑","◇","☠","⊘","◎","◉","⏱")
    val CATS = listOf("Arte","Finanças","Fitness","Saúde","Nutrição","Social","Estudo","Trabalho","Manhã","Dia","Tarde","Outro")
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayKey: String get() = sdf.format(Date())

    private lateinit var ctx: Context
    private var seeded = false

    fun init(c: Context) {
        ctx = c.applicationContext
        if (!seeded) {
            seeded = true
            if (loadHabits().isEmpty()) seed()
        }
    }

    private fun prefs() = ctx.getSharedPreferences("nextlevel", Context.MODE_PRIVATE)
    fun colorOf(name: String) = PALETTE.find { it.first == name }?.second ?: BLUE

    // ---------- hábitos ----------
    fun loadHabits(): MutableList<Habit> {
        val out = mutableListOf<Habit>()
        val raw = prefs().getString("habits", "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Habit(
                o.getLong("id"), o.getString("name"), o.getString("icon"),
                o.getString("color"), o.getString("type"), o.getString("freq"),
                o.getInt("times"), o.getString("cat"),
                if (o.isNull("alarm")) null else o.getString("alarm")
            ))
        }
        return out
    }

    fun saveHabits(list: List<Habit>) {
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(JSONObject()
                .put("id", h.id).put("name", h.name).put("icon", h.icon)
                .put("color", h.color).put("type", h.type).put("freq", h.freq)
                .put("times", h.times).put("cat", h.cat).put("alarm", h.alarm))
        }
        prefs().edit().putString("habits", arr.toString()).apply()
    }

    // ---------- histórico diário ----------
    fun hist(hid: Long): JSONObject {
        val raw = prefs().getString("hist_$hid", "{}") ?: "{}"
        return JSONObject(raw)
    }

    fun saveHist(hid: Long, o: JSONObject) {
        prefs().edit().putString("hist_$hid", o.toString()).apply()
    }

    fun success(h: Habit, k: String): Boolean =
        if (h.type == "quit") hist(h.id).optString(k) == "resisted"
        else hist(h.id).optInt(k) >= h.times

    fun streak(h: Habit): Int {
        val t = todayKey
        if (hist(h.id).optString(t) == "relapsed") return 0
        var s = 0
        val cal = Calendar.getInstance()
        if (!success(h, t)) cal.add(Calendar.DAY_OF_YEAR, -1)
        while (success(h, sdf.format(cal.time))) { s++; cal.add(Calendar.DAY_OF_YEAR, -1) }
        return s
    }

    // ---------- tempo (cronômetro) ----------
    fun addTime(hid: Long, min: Int) {
        val arr = JSONArray(prefs().getString("time", "[]") ?: "[]")
        arr.put(JSONObject().put("hid", hid).put("d", todayKey).put("min", min))
        prefs().edit().putString("time", arr.toString()).apply()
    }

    fun timeSum(hid: Long, period: String): Int {
        val arr = JSONArray(prefs().getString("time", "[]") ?: "[]")
        val now = Calendar.getInstance()
        var sum = 0
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            if (e.getLong("hid") != hid) continue
            val d = e.getString("d")
            val parts = d.split("-").map { it.toInt() }
            val c = Calendar.getInstance().apply { set(parts[0], parts[1] - 1, parts[2]) }
            val ok = when (period) {
                "semana" -> (now.timeInMillis - c.timeInMillis) / 86400000L < 7
                "mes" -> parts[0] == now.get(Calendar.YEAR) && parts[1] == now.get(Calendar.MONTH) + 1
                else -> parts[0] == now.get(Calendar.YEAR)
            }
            if (ok) sum += e.getInt("min")
        }
        return sum
    }

    fun fmtTime(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "${h}h" + (if (m > 0) " ${m}min" else "") else "${m}min"
    }

    // ---------- dados de exemplo (apenas no primeiro uso) ----------
    private fun seed() {
        val habits = mutableListOf(
            Habit(1, "Treinar 45 min", "⚡", "azul", "build", "diário", 1, "Fitness", "07:30"),
            Habit(2, "Ler 20 minutos", "✎", "índigo", "build", "diário", 1, "Estudo", "21:00"),
            Habit(3, "Parar de fumar", "⊘", "ciano", "quit", "diário", 1, "Saúde", null),
            Habit(4, "Beber 2L de água", "♒", "verde", "build", "diário", 2, "Saúde", null)
        )
        saveHabits(habits)
        val rates = mapOf(1L to 70, 2L to 58, 4L to 70)
        habits.forEach { h ->
            val o = JSONObject()
            val rnd = Random(42 + h.id)
            val cal = Calendar.getInstance()
            for (i in 60 downTo 1) {
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val k = sdf.format(cal.time)
                val r = rnd.nextInt(100)
                if (h.type == "quit") {
                    if (r < 74) o.put(k, "resisted") else if (r > 90) o.put(k, "relapsed")
                } else if (r < (rates[h.id] ?: 60)) o.put(k, 1)
            }
            saveHist(h.id, o)
        }
    }
}
