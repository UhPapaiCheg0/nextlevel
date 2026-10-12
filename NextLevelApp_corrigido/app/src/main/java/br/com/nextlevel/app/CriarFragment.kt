package br.com.nextlevel.app

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment

class CriarFragment : Fragment() {

    companion object {
        fun newInstance(id: Long) = CriarFragment().apply {
            arguments = Bundle().apply { putLong("id", id) }
        }
    }

    private var editId = -1L
    private var icon = HabitStore.GLYPHS[1]
    private var color = "azul"
    private var alarmTime = "08:00"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_criar, container, false)
        editId = arguments?.getLong("id") ?: -1L

        // grade de símbolos minimalistas
        val iconRow = v.findViewById<LinearLayout>(R.id.iconRow)
        HabitStore.GLYPHS.forEach { g ->
            val b = Button(requireContext()).apply {
                text = g; textSize = 18f
                setTextColor(Color.parseColor(HabitStore.BLUE))
                backgroundTintList = ColorStateList.valueOf(0xFF0B0B12.toInt())
            }
            val lp = LinearLayout.LayoutParams(dp(42), dp(42))
            lp.setMargins(dp(4), 0, dp(4), 0)
            b.setOnClickListener {
                icon = g
                refreshIcons(iconRow)
            }
            iconRow.addView(b, lp)
        }

        // paleta de cores
        val colorRow = v.findViewById<LinearLayout>(R.id.colorRow)
        HabitStore.PALETTE.forEach { (nome, hex) ->
            val b = Button(requireContext())
            b.backgroundTintList = ColorStateList.valueOf(Color.parseColor(hex))
            val lp = LinearLayout.LayoutParams(dp(36), dp(36))
            lp.setMargins(dp(4), 0, dp(4), 0)
            b.setOnClickListener {
                color = nome
                colorRow.children.forEachIndexed { i, view ->
                    view.setBackgroundResource(0)
                    (view as Button).backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(HabitStore.PALETTE[i].second))
                    if (HabitStore.PALETTE[i].first == color) {
                        view.setBackgroundResource(R.drawable.bg_input)
                    }
                }
            }
            colorRow.addView(b, lp)
        }

        val spCat = v.findViewById<Spinner>(R.id.spCat)
        spCat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, HabitStore.CATS)

        var times = 1
        val tvTimes = v.findViewById<TextView>(R.id.tvTimes)
        v.findViewById<Button>(R.id.btnMinus).setOnClickListener { times = (times - 1).coerceAtLeast(1); tvTimes.text = times.toString() }
        v.findViewById<Button>(R.id.btnPlus).setOnClickListener { times = (times + 1).coerceAtMost(9); tvTimes.text = times.toString() }

        val btnTime = v.findViewById<Button>(R.id.btnTime)
        btnTime.setOnClickListener {
            val p = alarmTime.split(":").map { it.toInt() }
            TimePickerDialog(requireContext(), { _, hh, mm ->
                alarmTime = "%02d:%02d".format(hh, mm)
                btnTime.text = "Horário: $alarmTime"
            }, p[0], p[1], true).show()
        }

        // edição: preencher campos
        if (editId > 0) {
            val h = HabitStore.loadHabits().find { it.id == editId }
            if (h != null) {
                v.findViewById<TextView>(R.id.tvTitle).text = "Editar hábito"
                v.findViewById<EditText>(R.id.edName).setText(h.name)
                v.findViewById<RadioButton>(R.id.rbQuit).isChecked = h.type == "quit"
                v.findViewById<RadioButton>(R.id.rbSemanal).isChecked = h.freq == "semanal"
                icon = h.icon; color = h.color; times = h.times
                tvTimes.text = times.toString()
                spCat.setSelection(HabitStore.CATS.indexOf(h.cat).coerceAtLeast(0))
                h.alarm?.let {
                    alarmTime = it
                    v.findViewById<CheckBox>(R.id.cbAlarm).isChecked = true
                    btnTime.text = "Horário: $it"
                }
                v.findViewById<Button>(R.id.btnSave).text = "Salvar alterações"
                refreshIcons(iconRow)
            }
        }

        v.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val name = v.findViewById<EditText>(R.id.edName).text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Dê um nome ao hábito", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val type = if (v.findViewById<RadioButton>(R.id.rbQuit).isChecked) "quit" else "build"
            val freq = if (v.findViewById<RadioButton>(R.id.rbSemanal).isChecked) "semanal" else "diário"
            val alarm = if (v.findViewById<CheckBox>(R.id.cbAlarm).isChecked) alarmTime else null
            val habits = HabitStore.loadHabits()
            if (editId > 0) {
                habits.find { it.id == editId }?.let {
                    it.name = name; it.icon = icon; it.color = color; it.type = type
                    it.freq = freq; it.times = if (type == "build") times else 1
                    it.cat = spCat.selectedItem.toString(); it.alarm = alarm
                    AlarmReceiver.schedule(requireContext(), it)
                }
                Toast.makeText(requireContext(), "Hábito atualizado", Toast.LENGTH_SHORT).show()
            } else {
                val h = Habit(System.currentTimeMillis(), name, icon, color, type, freq,
                    if (type == "build") times else 1, spCat.selectedItem.toString(), alarm)
                habits.add(h)
                AlarmReceiver.schedule(requireContext(), h)
                Toast.makeText(requireContext(), "Hábito criado", Toast.LENGTH_SHORT).show()
            }
            HabitStore.saveHabits(habits)
            (activity as MainActivity).openDiario()
        }
        return v
    }

    private fun refreshIcons(row: LinearLayout) {
        row.children.forEachIndexed { i, view ->
            val b = view as Button
            val selected = HabitStore.GLYPHS[i] == icon
            b.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else HabitStore.BLUE))
            b.backgroundTintList = ColorStateList.valueOf(
                if (selected) Color.parseColor(HabitStore.BLUE) else 0xFF0B0B12.toInt())
        }
    }
}
