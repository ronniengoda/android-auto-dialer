package ke.payhero.autodial

import android.app.Application

class AutoDialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DialServerService.start(this)
    }
}
