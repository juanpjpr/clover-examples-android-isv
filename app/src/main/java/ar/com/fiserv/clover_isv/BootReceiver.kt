package ar.com.fiserv.clover_isv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.clover.sdk.Lockscreen



class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("BootReceiver", "Dispositivo encendido - BOOT_COMPLETED recibido")
            val lockscreen = Lockscreen(context)

            lockscreen.unlockDefault()

            startMainActivity(context)
        }
        else
        {
            Log.d("BootReceiver", "Dispositivo encendido - no lllego")
        }
    }

    private fun startMainActivity(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(launchIntent)
    }
}
