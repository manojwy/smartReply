package com.manoj.smartreply

import android.app.job.JobParameters
import android.app.job.JobService
import android.net.Uri
import android.util.Log
import java.io.File

class SmartReplyJobService: JobService() {
    companion object {
        val UPDATE_UI = "com.manoj.smartreply.UPDATE_UI"
        val JOB_ID: Int = 12007
    }

    var smartReplyUtil = SmartReplyUtil(null)

    override fun onStartJob(params: JobParameters): Boolean {

        Log.d("MM", "SmartReplyJobService: onStartJob")
        val path = params.extras.getString("file_path")
        val uri = Uri.parse(path)

        Log.i("MM", "Uri: $uri")

        smartReplyUtil.fileUri = uri
        smartReplyUtil.appContext = applicationContext
        smartReplyUtil.start()

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

}