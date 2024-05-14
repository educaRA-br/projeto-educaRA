package com.smk.educara_screens.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.smk.educara_screens.R
import com.smk.educara_screens.databinding.MainActivityBinding

class SelecaoActivity: AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

//        val caminhoObj = intent.getStringExtra("caminho")
//
//        if (caminhoObj!= null) {
//            val resultIntent = Intent()
//            resultIntent.putExtra("caminho", caminhoObj)
//            setResult(RESULT_OK, resultIntent)
//            finishAffinity()
//        }

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}