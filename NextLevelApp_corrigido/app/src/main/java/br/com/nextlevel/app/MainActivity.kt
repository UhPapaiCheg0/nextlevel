package br.com.nextlevel.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HabitStore.init(this)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.tabCriar).setOnClickListener { openCreate() }
        findViewById<Button>(R.id.tabDiario).setOnClickListener { openDiario() }
        findViewById<Button>(R.id.tabGeral).setOnClickListener { openGeral() }
        findViewById<Button>(R.id.btnPremium).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Next Level Premium")
                .setMessage("Backup automático dos seus hábitos no Google Drive vinculado ao seu Gmail e sincronização entre aparelhos. (Recurso em desenvolvimento)")
                .setPositiveButton("Entendi", null)
                .show()
        }
        if (savedInstanceState == null) openDiario()
    }

    private fun show(f: Fragment, tab: Int) {
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
        listOf(R.id.tabCriar, R.id.tabDiario, R.id.tabGeral).forEachIndexed { i, id ->
            findViewById<Button>(id).setTextColor(
                getColor(if (i == tab) R.color.blue else R.color.muted)
            )
        }
    }

    fun openCreate() = show(CriarFragment.newInstance(-1L), 0)
    fun openEdit(id: Long) = show(CriarFragment.newInstance(id), 0)
    fun openDiario() = show(DiarioFragment(), 1)
    fun openGeral() = show(GeralFragment(), 2)
}
