package com.hemu1089.uhf

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.ui.AppBarConfiguration
import com.hemu1089.rf_reader.RfidScanner
import com.hemu1089.uhf.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val rfid = RfidScanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        rfid.initialize(this)
        binding.fab.setOnClickListener { view ->
            binding.tags.text = rfid.start().toString()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            if (event!!.repeatCount == 0) {
                binding.tags.text = rfid.start().toString()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}