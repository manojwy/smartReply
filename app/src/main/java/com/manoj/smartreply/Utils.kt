package com.manoj.smartreply

import android.content.Context


class Utils {
    companion object {
        val COMPLETED_JOB = "com.manoj.smartreply.COMPLETED_JOB"
        val UPDATE_UI = "com.manoj.smartreply.UPDATE_UI"
        val JOB_ID: Int = 12007
        val prefName = "com.manoj.smartreply.pref"


        fun updatePrefWithLineNo(context:Context, lineNo:Long) {
            val editor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
            editor.putLong("LLP", lineNo)
            editor.apply()
        }

        fun updatePrefWithFilename(context:Context, filename:String) {
            val editor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
            editor.putString("filename", filename)
            editor.apply()
        }

        fun getFilenameFromPref(context:Context): String? {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val filename = prefs.getString("filename", null)
            return filename
        }

        fun getLLPFromPref(context:Context): Long {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val llp = prefs.getLong("LLP", 0)
            return llp
        }

        fun updatePrefWithSourceFilename(context:Context, filename:String) {
            val editor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
            editor.putString("sourcefilename", filename)
            editor.apply()
        }

        fun getSourceFilenameFromPref(context:Context): String? {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val filename = prefs.getString("sourcefilename", null)
            return filename
        }

        fun isCrashRecovery(context: Context):Boolean {
            var srcFilename = Utils.getSourceFilenameFromPref(context)
            var tgtFilename = Utils.getFilenameFromPref(context)
            var lineNo = Utils.getLLPFromPref(context)


            if (srcFilename == null || srcFilename.isEmpty() || tgtFilename == null || tgtFilename.isEmpty()) {
                Utils.updatePrefWithSourceFilename(context, "")
                Utils.updatePrefWithFilename(context, "")
                Utils.updatePrefWithLineNo(context, 0)
                return false
            }

            return true
        }

    }
}