package tbcode.example.kotlinbitcoinwallet.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.util.Log
import tbcode.example.kotlinbitcoinwallet.MainActivity

class KitBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, KitSyncService::class.java)
        if (context != null && !KitSyncService.isRunning && isOnline(context)) {
            Log.d("btc-alert", "Starting Foreground Service from Receiver")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else context.startService(serviceIntent)
        }
        else{

            if(!MainActivity.isActive){
                Log.d("btc-alert", "App is not active. Stopping Foreground Service from Receiver")
                KitSyncService.stopSync()
            }
        }


    }
    private fun isOnline(context: Context?): Boolean {
        return try {
            val connMgr = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
            return networkInfo?.isConnected == true
        } catch (e:Exception){
            false
        }

    }

}