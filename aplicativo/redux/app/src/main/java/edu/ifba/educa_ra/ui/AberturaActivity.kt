package edu.ifba.educa_ra.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AberturaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.sleep(2000)

        startActivity(Intent(this@AberturaActivity, InicializacaoActivity::class.java))
        finish()
    }
}