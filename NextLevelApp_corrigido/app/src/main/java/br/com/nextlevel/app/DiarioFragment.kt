package br.com.nextlevel.app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class DiarioFragment : Fragment() {

    private lateinit var list: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_diario, container, false)
        list = v.findViewById(R.id.habitList)
        v.findViewById<ImageButton>(R.id.fab).setOnClickListener { showTimer() }
        render(v)
        return v
    }

    private fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    private fun render(v: View) {
        val habits = HabitStore.loadHabits()
        val cal = Calendar.getInstance()
        val wd = arrayOf("domingo","segunda-feira","terça-feira","quarta-feira","quinta-feira","sexta-feira","sábado")
        val mn = arrayOf("janeiro","fevereiro","março","abril","maio","junho","julho","agosto","setembro","outubro","novembro","dezembro")
        v.findViewById<TextView>(R.id.tvDate).text =
            "${wd[cal.get(Calendar.DAY_OF_WEEK)-1]}, ${cal.get(Calendar.DAY_OF_MONTH)} de ${mn[cal.get(Calendar.MONTH)]}"
        val done = habits.count { HabitStore.success(it, HabitStore.todayKey) }
        val pb = v.findViewById<android.widget.ProgressBar>(R.id.progressDay)
        pb.max = habits.size.coerceAtLeast(1)
        pb.progress = done

        list.removeAllViews()
        val t = HabitStore.todayKey
        habits.forEach { h ->
            val hist = HabitStore.hist(h.id)
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(requireContext(), 4), 0, dp(requireContext(), 4))
            row.layoutParams = lp
            row.setPadding(dp(requireContext(), 12), dp(requireContext(), 10), dp(requireContext(), 12), dp(requireContext(), 10))

            val icon = TextView(requireContext()).apply {
                text = h.icon
                textSize = 20f
                setTextColor(android.graphics.Color.parseColor(HabitStore.colorOf(h.color)))
            }
            val ilp = LinearLayout.LayoutParams(dp(requireContext(), 32), LinearLayout.LayoutParams.WRAP_CONTENT)
            row.addView(icon, ilp)

            val mid = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            val mlp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(mid, mlp)
            mid.addView(TextView(requireContext()).apply {
                text = h.name; textSize = 15f; setTextColor(0xFFFFFFFF.toInt())
            })
            val sub = TextView(requireContext()).apply {
                textSize = 12f
                setTextColor(0xFF6B7280.toInt())
                text = buildString {
                    append(h.cat)
                    h.alarm?.let { append("   ⏱ $it") }
                    append("   ♨ ${HabitStore.streak(h)} dia(s)")
                }
            }
            mid.addView(sub)

            if (h.type == "build") {
                val n = hist.optInt(t)
                val btn = Button(requireContext()).apply {
                    text = if (n >= h.times) "✔" else {
                        if (h.times > 1) "○  " + "●".repeat(n) + "○".repeat(h.times - n) else "○"
                    }
                    setTextColor(0xFF3B82F6.toInt())
                    backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF0B0B12.toInt())
                }
                btn.setOnClickListener {
                    val o = HabitStore.hist(h.id)
                    o.put(t, if (n >= h.times) 0 else n + 1)
                    HabitStore.saveHist(h.id, o)
                    render(view ?: return@setOnClickListener)
                }
                row.addView(btn)
            } else {
                val vq = hist.optString(t)
                val bRes = Button(requireContext()).apply {
                    text = "◇ Resistiu"; textSize = 12f
                    setTextColor(0xFFFFFFFF.toInt())
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (vq == "resisted") 0xFF17324F.toInt() else 0xFF0B0B12.toInt())
                }
                bRes.setOnClickListener {
                    val o = HabitStore.hist(h.id)
                    if (vq == "resisted") o.remove(t) else o.put(t, "resisted")
                    HabitStore.saveHist(h.id, o)
                    render(view ?: return@setOnClickListener)
                }
                val bRec = Button(requireContext()).apply {
                    text = "☠ Recaída"; textSize = 12f
                    setTextColor(0xFFFFFFFF.toInt())
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (vq == "relapsed") 0xFF17324F.toInt() else 0xFF0B0B12.toInt())
                }
                bRec.setOnClickListener {
                    val o = HabitStore.hist(h.id)
                    if (vq == "relapsed") o.remove(t) else {
                        o.put(t, "relapsed")
                        Toast.makeText(requireContext(), "Recaída registrada — sequência reiniciada", Toast.LENGTH_SHORT).show()
                    }
                    HabitStore.saveHist(h.id, o)
                    render(view ?: return@setOnClickListener)
                }
                row.addView(bRes)
                row.addView(bRec)
            }

            row.setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setItems(arrayOf("Editar", "Excluir")) { _, which ->
                        if (which == 0) (activity as MainActivity).openEdit(h.id)
                        else {
                            AlertDialog.Builder(requireContext())
                                .setMessage("Excluir \"${h.name}\"?")
                                .setPositiveButton("Excluir") { _, _ ->
                                    HabitStore.saveHabits(HabitStore.loadHabits().filter { it.id != h.id })
                                    AlarmReceiver.cancel(requireContext(), h.id)
                                    render(view ?: return@setPositiveButton)
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    }
                    .show()
                true
            }
            list.addView(row)
        }
    }

    private fun showTimer() {
        val habits = HabitStore.loadHabits()
        if (habits.isEmpty()) {
            Toast.makeText(requireContext(), "Crie um hábito primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        val ctx = requireContext()
        val lay = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 12), dp(ctx, 20), dp(ctx, 4))
        }
        val sp = Spinner(ctx)
        sp.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, habits.map { it.name })
        lay.addView(sp)
        val chrono = Chronometer(ctx)
        chrono.textSize = 32f
        chrono.gravity = Gravity.CENTER
        chrono.setTextColor(0xFFFFFFFF.toInt())
        chrono.setPadding(0, dp(ctx, 12), 0, dp(ctx, 12))
        lay.addView(chrono)

        var base = 0L
        var running = false
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val btnStart = Button(ctx).apply { text = "Iniciar" }
        val btnStop = Button(ctx).apply { text = "Encerrar e salvar" }
        row.addView(btnStart, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(btnStop, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        lay.addView(row)

        val dlg = AlertDialog.Builder(ctx)
            .setTitle("Cronômetro de atividade")
            .setView(lay)
            .setNegativeButton("Fechar", null)
            .create()

        btnStart.setOnClickListener {
            if (!running) {
                chrono.base = SystemClock.elapsedRealtime() - base
                chrono.start()
                btnStart.text = "Pausar"
                running = true
            } else {
                base = SystemClock.elapsedRealtime() - chrono.base
                chrono.stop()
                btnStart.text = "Retomar"
                running = false
            }
        }
        btnStop.setOnClickListener {
            val elapsed = if (running) SystemClock.elapsedRealtime() - chrono.base else base
            val min = ((elapsed / 60000.0).roundToInt()).coerceAtLeast(1)
            HabitStore.addTime(habits[sp.selectedItemPosition].id, min)
            Toast.makeText(ctx, "Registrado: ${HabitStore.fmtTime(min)} em ${habits[sp.selectedItemPosition].name}", Toast.LENGTH_LONG).show()
            dlg.dismiss()
        }
        dlg.show()
    }
}
