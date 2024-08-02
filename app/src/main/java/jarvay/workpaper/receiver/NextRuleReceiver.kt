package jarvay.workpaper.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.data.day.RuleDao
import jarvay.workpaper.others.getCalendarWithRule
import jarvay.workpaper.others.nextRule
import jarvay.workpaper.receiver.RuleReceiver.Companion.RULE_ID_KEY
import javax.inject.Inject

@AndroidEntryPoint
class NextRuleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleDao: RuleDao

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val nextRule = nextRule(ruleDao.findAll())
            if (nextRule != null) {
                val calendar = getCalendarWithRule(nextRule.rule)
                val i = Intent(context, RuleReceiver::class.java)
                i.putExtra(RULE_ID_KEY, nextRule.album.albumId)
                val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    i,
                    PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager: AlarmManager? =
                    context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
                try {
                    alarmManager?.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    Log.e(javaClass.simpleName, e.toString())
                }
            }
        }
    }
}