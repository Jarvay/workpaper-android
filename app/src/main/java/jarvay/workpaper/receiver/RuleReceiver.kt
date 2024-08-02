package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.data.day.RuleDao
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.defaultSharedPreferences
import javax.inject.Inject

@AndroidEntryPoint
class RuleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleDao: RuleDao

    override fun onReceive(context: Context?, intent: Intent?) {
        val ruleId = intent?.getLongExtra(RULE_ID_KEY, -1) ?: -1
        Log.d(javaClass.simpleName, ruleId.toString())

        if (ruleId > -1) {
            val r = ruleDao.findWithAlbumById(ruleId)
            context?.let {
                val sp = defaultSharedPreferences(context)
                sp.edit().putLong(SharePreferenceKey.CURRENT_ALBUM_ID_KEY, r.album.albumId)
                    .putInt(SharePreferenceKey.LAST_INDEX_KEY, 0).apply()
            }
        }

        val i = Intent(context, NextRuleReceiver::class.java)
        context?.sendBroadcast(i)
    }

    companion object {
        const val RULE_ID_KEY = "ruleId"
    }
}