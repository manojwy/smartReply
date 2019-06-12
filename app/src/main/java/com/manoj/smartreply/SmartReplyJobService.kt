package com.manoj.smartreply

import android.app.job.JobParameters
import android.app.job.JobService
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SmartReplyJobService: JobService() {
    companion object {
    }


    override fun onStartJob(params: JobParameters): Boolean {

        Log.d("MM", "SmartReplyJobService: onStartJob")
        val path = params.extras.getString("file_path")

        Log.i("MM", "Uri: $path")

        var smartReplyUtil = SmartReplyUtil(null)

        Executors.newSingleThreadExecutor().execute {
            smartReplyUtil.fileUri = path;
            smartReplyUtil.appContext = applicationContext
            smartReplyUtil.start()
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

}