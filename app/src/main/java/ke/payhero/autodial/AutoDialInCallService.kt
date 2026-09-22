package ke.payhero.autodial

import android.telecom.InCallService
import android.telecom.Call

/**
 * Required by Android when an app requests ROLE_DIALER.
 *
 * For a production deployment, replace this with a proper incoming/
 * ongoing call UI. The app's main purpose is handling tel: intents.
 */
class AutoDialInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
    }
}
