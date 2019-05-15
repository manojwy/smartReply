package com.manoj.smartreply

import android.app.job.JobParameters
import android.app.job.JobService
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

class SmartReplyJobService: JobService() {
    companion object {
        val UPDATE_UI = "com.manoj.smartreply.UPDATE_UI"
        val JOB_ID: Int = 12007
        val TYPE_PROCESSED = "PROCESSED"
    }

    var smartReplyUtil = SmartReplyUtil()

    override fun onStartJob(params: JobParameters): Boolean {

        Log.d(SmartReplyUtil.TAG, "SmartReplyJobService: onStartJob")
        val path = params.extras.getString("file_path")
        val uri = Uri.parse(path)

        Log.d(SmartReplyUtil.TAG, "Uri: $uri")

        smartReplyUtil.fileUri = uri
        smartReplyUtil.appContext = applicationContext


        Executors.newSingleThreadExecutor().execute{
            smartReplyUtil.start()
        }


        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

}