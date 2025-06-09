package ar.com.fiserv.clover_isv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.clover.sdk.Lockscreen



class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val lockscreen = Lockscreen(context)
            if (lockscreen.unlockDefault()) {
                Log.d("jpr","ENTrA")
                startMainActivity(context)
            } else {
                Log.d("jpr"," no ENTrA")
                lockscreen.unlock() // Fallback a desbloqueo normal
                startMainActivity(context)
            }
        }
    }

    private fun startMainActivity(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(launchIntent)
    }
}
