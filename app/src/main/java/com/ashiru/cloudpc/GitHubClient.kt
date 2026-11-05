package com.ashiru.cloudpc

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WorkflowState(val status: String, val conclusion: String?, val htmlUrl: String, val runId: Long)

class GitHubClient(private val token: String) {
    private val owner = "hamitbull"
    private val repo = "ASHIRU-CLOUD-PC"
    private val api = "https://api.github.com/repos/$owner/$repo"

    private fun request(path: String, method: String = "GET", body: String? = null): String {
        val connection = (URL(api + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try { JSONObject(text).optString("message", text) } catch (_: Exception) { text }
                throw IllegalStateException("GitHub API HTTP $code: $message")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    fun dispatch(jobType: String, payload: String) {
        if (jobType == "build_apk") {
            val json = JSONObject().put("ref", "main")
            request("/actions/workflows/android-build.yml/dispatches", "POST", json.toString())
            return
        }
        val json = JSONObject()
            .put("ref", "main")
            .put("inputs", JSONObject().put("job_type", jobType).put("payload", payload.take(12000)))
        request("/actions/workflows/cloud-job.yml/dispatches", "POST", json.toString())
    }

    private fun latestRun(workflow: String): WorkflowState? {
        val json = JSONObject(request("/actions/workflows/$workflow/runs?per_page=1"))
        val runs = json.optJSONArray("workflow_runs") ?: return null
        if (runs.length() == 0) return null
        val run = runs.getJSONObject(0)
        return WorkflowState(
            status = run.optString("status", "unknown"),
            conclusion = run.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
            htmlUrl = run.optString("html_url"),
            runId = run.optLong("id")
        )
    }

    fun latestRun(): WorkflowState? = latestRun("cloud-job.yml")
    fun latestBuildRun(): WorkflowState? = latestRun("android-build.yml")

    fun latestArtifactUrl(build: Boolean = false): String? {
        val run = if (build) latestBuildRun() else latestRun() ?: return null
        val json = JSONObject(request("/actions/runs/${run.runId}/artifacts"))
        val artifacts = json.optJSONArray("artifacts") ?: return null
        if (artifacts.length() == 0) return null
        return artifacts.getJSONObject(0).optString("archive_download_url").takeIf { it.isNotBlank() }
    }
}
