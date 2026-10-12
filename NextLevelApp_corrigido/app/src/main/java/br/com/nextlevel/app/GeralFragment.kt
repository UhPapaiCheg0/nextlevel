package br.com.nextlevel.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Calendar

class GeralFragment : Fragment() {

    private var calY = Calendar.getInstance().get(Calendar.YEAR)
    private var calM = Calendar.getInstance().get(Calendar.MONTH)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_geral, container, false)
        val mn = arrayOf("janeiro","fevereiro","março","abril","maio","junho","julho","agosto","setembro","outubro","novembro","dezembro")
        val tvMonth = v.findViewById<TextView>(R.id.tvMonth)

        fun updateTitle() { tvMonth.text = "${mn[calM]} $calY" }

        v.findViewById<android.widget.Button>(R.id.btnPrev).setOnClickListener {
            calM--; if (calM < 0) { calM = 11; calY-- }
            updateTitle(); renderCalendar(v); renderCharts(v)
        }
        val now = Calendar.getInstance()
        v.findViewById<android.widget.Button>(R.id.btnNext).setOnClickListener {
            if (calY == now.get(Calendar.YEAR) && calM == now.get(Calendar.MONTH)) return@setOnClickListener
            calM++; if (calM > 11) { calM = 0; calY++ }
            updateTitle(); renderCalendar(v); renderCharts(v)
        }

        v.findViewById<RadioGroup>(R.id.rgPeriod).setOnCheckedChangeListener { _, _ -> renderTime(v) }

        updateTitle()
        renderCalendar(v)
        renderTime(v)
        renderCharts(v)
        return v
    }

    // ---------- calendário ----------
    private fun renderCalendar(v: View) {
        val grid = v.findViewById<GridLayout>(R.id.calGrid)
        grid.removeAllViews()
        val habits = HabitStore.loadHabits().take(4)
        val wd = arrayOf("D","S","T","Q","Q","S","S")
        wd.forEach { addCell(grid, it, 0xFF6B7280.toInt(), null, null) }

        val first = Gregorian(calY, calM, 1).get(Calendar.DAY_OF_WEEK) - 1
        val dim = Gregorian(calY, calM, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
        val now = Calendar.getInstance()
        for (i in 0 until first) addCell(grid, "", 0, null, null)
        for (d in 1..dim) {
            val k = "%04d-%02d-%02d".format(calY, calM + 1, d)
            val future = calY == now.get(Calendar.YEAR) && calM == now.get(Calendar.MONTH) && d > now.get(Calendar.DAY_OF_MONTH)
            val isToday = k == HabitStore.todayKey
            addCell(grid, d.toString(), if (isToday) 0xFF3B82F6.toInt() else 0xFFFFFFFF.toInt(), if (isToday) 0xFF14263F.toInt() else null,
                if (future) null else habits.map { h ->
                    when {
                        HabitStore.success(h, k) -> HabitStore.colorOf(h.color)
                        HabitStore.hist(h.id).optString(k) == "relapsed" -> "#3B82F6"
                        k != HabitStore.todayKey -> "#374151"
                        else -> "#232336"
                    }
                })
        }
    }

    private fun Gregorian(y: Int, m: Int, d: Int) = Calendar.getInstance().apply { set(y, m, d) }

    private fun addCell(grid: GridLayout, label: String, color: Int, bg: Int?, dots: List<String>?) {
        val cell = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            if (bg != null) setBackgroundColor(bg)
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }
        val size = (resources.displayMetrics.widthPixels - dp(60)) / 7
        val lp = ViewGroup.MarginLayoutParams(size, size)
        grid.addView(cell, lp)
        cell.addView(TextView(requireContext()).apply {
            text = label; textSize = 11f; setTextColor(color); gravity = Gravity.CENTER
        })
        dots?.let { list ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            list.forEach { hex ->
                val dot = View(requireContext())
                val dlp = LinearLayout.LayoutParams(dp(5), dp(5))
                dlp.setMargins(dp(1), dp(2), dp(1), 0)
                dot.setBackgroundColor(Color.parseColor(hex))
                row.addView(dot, dlp)
            }
            cell.addView(row)
        }
    }

    // ---------- relatório de tempo ----------
    private fun period(v: View): String = when {
        v.findViewById<RadioButton>(R.id.rbAno).isChecked -> "ano"
        v.findViewById<RadioButton>(R.id.rbMes).isChecked -> "mes"
        else -> "semana"
    }

    private fun renderTime(v: View) {
        val rows = v.findViewById<LinearLayout>(R.id.timeRows)
        rows.removeAllViews()
        val habits = HabitStore.loadHabits()
        val sums = habits.map { it to HabitStore.timeSum(it.id, period(v)) }
        val mx = (sums.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        sums.forEach { (h, sum) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(3), 0, dp(3))
            row.layoutParams = lp
            row.setPadding(dp(10), dp(8), dp(10), dp(8))
            row.addView(TextView(requireContext()).apply {
                text = h.icon; textSize = 15f
                setTextColor(Color.parseColor(HabitStore.colorOf(h.color)))
            }, LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.WRAP_CONTENT))
            val mid = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            row.addView(mid, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            mid.addView(TextView(requireContext()).apply {
                text = "${h.name}   ·   ${HabitStore.fmtTime(sum)}"
                textSize = 12f; setTextColor(0xFFA7ADBA.toInt())
            })
            val pb = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                max = mx; progress = sum
                progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(HabitStore.colorOf(h.color)))
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0xFF232336.toInt())
            }
            mid.addView(pb)
            rows.addView(row)
        }
    }

    // ---------- gráficos de evolução ----------
    private fun renderCharts(v: View) {
        val charts = v.findViewById<LinearLayout>(R.id.chartList)
        charts.removeAllViews()
        val now = Calendar.getInstance()
        HabitStore.loadHabits().forEach { h ->
            val dim = Gregorian(calY, calM, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
            val last = if (calY == now.get(Calendar.YEAR) && calM == now.get(Calendar.MONTH))
                now.get(Calendar.DAY_OF_MONTH) else dim
            var ok = 0
            for (d in 1..last) if (HabitStore.success(h, "%04d-%02d-%02d".format(calY, calM + 1, d))) ok++
            val pct = if (last > 0) ok.toFloat() / last else 0f

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(3), 0, dp(3))
            row.layoutParams = lp
            row.setPadding(dp(10), dp(8), dp(10), dp(8))

            row.addView(PieView(requireContext(), pct, Color.parseColor(HabitStore.colorOf(h.color))),
                LinearLayout.LayoutParams(dp(56), dp(56)))

            val mid = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            row.addView(mid, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            mid.addView(TextView(requireContext()).apply {
                text = "${h.icon} ${h.name}"
                textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
            })
            mid.addView(TextView(requireContext()).apply {
                text = "sequência: ${HabitStore.streak(h)} dias · $ok/$last"
                textSize = 12f; setTextColor(0xFF6B7280.toInt())
            })
            charts.addView(row)
        }
    }

    class PieView(c: Context, private val pct: Float, private val color: Int) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val cx = w / 2; val cy = height / 2f
            val r = (minOf(w, height.toFloat()) / 2) - 8
            p.style = Paint.Style.STROKE
            p.strokeWidth = 10f
            p.color = 0xFF232336.toInt()
            canvas.drawCircle(cx, cy, r, p)
            p.color = color
            p.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * pct, false, p)
            p.style = Paint.Style.FILL
            p.textSize = 22f
            p.textAlign = Paint.Align.CENTER
            canvas.drawText("${(pct * 100).toInt()}%", cx, cy + 8f, p)
        }
    }
}
