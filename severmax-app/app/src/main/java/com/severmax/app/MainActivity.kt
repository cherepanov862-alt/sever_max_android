package com.severmax.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Хост-активность: показывает интерфейс приложения (assets/www/index.html)
 * в WebView и даёт JavaScript-коду возможность отправлять SMS через
 * мост window.Android.sendSms(number, text).
 *
 * Формат самих команд (текст SMS) нужно свести с реальным протоколом
 * подогревателя Severmax — см. константы в index.html (секция "SMS-команды").
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pendingNumber: String? = null
    private var pendingText: String? = null

    private val smsPermissionCode = 501

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.loadUrl("file:///android_asset/www/index.html")
    }

    private inner class AndroidBridge {

        /** Вызывается из JS: Android.sendSms("+79991234567", "ON") */
        @JavascriptInterface
        fun sendSms(number: String, text: String) {
            if (number.isBlank()) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Не задан номер SIM подогревателя", Toast.LENGTH_SHORT).show()
                }
                return
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingNumber = number
                pendingText = text
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.SEND_SMS),
                    smsPermissionCode
                )
                return
            }
            doSendSms(number, text)
        }
    }

    private fun doSendSms(number: String, text: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
            runOnUiThread {
                Toast.makeText(this, "Команда отправлена: $text", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка отправки SMS: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == smsPermissionCode &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val n = pendingNumber
            val t = pendingText
            if (n != null && t != null) doSendSms(n, t)
        } else {
            Toast.makeText(this, "Без разрешения SEND_SMS команды отправляться не будут", Toast.LENGTH_LONG).show()
        }
        pendingNumber = null
        pendingText = null
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
