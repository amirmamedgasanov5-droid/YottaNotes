package com.dynamicisland.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Раз в ~15 минут проверяет, жив ли остров, и поднимает его, если система его остановила. */
class WatchdogJob : JobService() {
    override fun onStartJob(p: JobParameters?): Boolean {
        if (Prefs(this).enabled && !IslandBus.running && IslandAccessibility.instance == null && Settings.canDrawOverlays(this)) {
            try { startForegroundService(Intent(this, IslandService::class.java)) } catch (_: Exception) {}
        }
        return false
    }

    override fun onStopJob(p: JobParameters?) = false

    companion object {
        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java)
            if (js.getPendingJob(77) != null) return
            js.schedule(
                JobInfo.Builder(77, ComponentName(ctx, WatchdogJob::class.java))
                    .setPeriodic(15 * 60 * 1000L).setPersisted(true).build(),
            )
        }
    }
}
