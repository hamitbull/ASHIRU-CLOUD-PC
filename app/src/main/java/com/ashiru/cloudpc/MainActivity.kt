package com.ashiru.cloudpc

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {
    // android.app.Activity is not a LifecycleOwner; use an activity-owned coroutine scope.
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var vault: TokenVault
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var tokenInput: EditText
    private lateinit var commandInput: EditText
    private var ctrl = false
    private var alt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = TokenVault(this)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 20)
            setBackgroundColor(Color.rgb(10, 16, 31))
        }
        val scroller = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroller.addView(content)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))

        val title = TextView(this).apply {
            text = "ASHIRU CLOUD PC"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(title)
        content.addView(label("Your cloud computer • GitHub Actions runner"))
        status = label("Status: Ready")
        content.addView(status)

        content.addView(section("SECURE CONNECTION"))
        tokenInput = EditText(this).apply {
            hint = "GitHub token (stored encrypted on this phone)"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        content.addView(tokenInput)
        content.addView(button("SAVE TOKEN SECURELY") {
            try {
                vault.save(tokenInput.text.toString().trim())
                tokenInput.text.clear()
                status.text = "Status: Token encrypted and saved on this device."
            } catch (e: Exception) { showError(e.message ?: "Token could not be saved") }
        })
        content.addView(button("REMOVE SAVED TOKEN") {
            vault.clear()
            status.text = "Status: Saved token removed."
        })

        content.addView(section("CLOUD COMPUTER"))
        content.addView(button("SYSTEM INFORMATION") { dispatch("system_info", "") })
        content.addView(button("BUILD APK FROM THIS REPOSITORY") { dispatch("build_apk", "") })
        content.addView(button("RUN PYTHON CODE") {
            val code = commandInput.text.toString()
            if (code.isBlank()) showError("Rubuta Python code kafin ka tura.")
            else dispatch("python", code)
        })

        content.addView(section("TERMINAL"))
        commandInput = EditText(this).apply {
            hint = "Example: pwd   |   ls   |   python3 --version"
            minLines = 3
            gravity = Gravity.TOP
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        content.addView(commandInput)
        val keys = listOf("Ctrl", "Alt", "Shift", "Tab", "Esc", "↑", "↓", "←", "→", "Home", "End", "PgUp", "PgDn", "Enter")
        val keyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        keys.forEach { key ->
            val b = Button(this).apply {
                text = key
                textSize = 10f
                setOnClickListener { insertKey(key) }
            }
            keyRow.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        content.addView(keyRow)
        content.addView(button("RUN ALLOWED TERMINAL COMMAND") {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isBlank()) showError("Shigar da umarni.")
            else dispatch("terminal", cmd)
        })

        content.addView(section("JOB CONTROL"))
        content.addView(button("CHECK LATEST CLOUD JOB") { checkStatus() })
        content.addView(button("OPEN LATEST CLOUD JOB") { openLatestRun() })
        content.addView(button("CHECK APK BUILD STATUS") { checkBuildStatus() })
        content.addView(button("OPEN APK BUILD PAGE") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hamitbull/ASHIRU-CLOUD-PC/actions/workflows/android-build.yml"))) })
        content.addView(button("OPEN LATEST RESULT ARTIFACT") { openArtifact() })

        content.addView(section("OUTPUT"))
        output = TextView(this).apply {
            text = "Sakamakon aikin zai bayyana a GitHub Actions → Artifacts."
            textSize = 13f
            setTextColor(Color.rgb(205, 218, 238))
            setPadding(12, 14, 12, 14)
        }
        content.addView(output)
        content.addView(label("Note: runner na wucin-gadi ne. Ba persistent VPS ko unrestricted shell ba."))
        setContentView(root)
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(Color.rgb(180, 197, 222))
        setPadding(0, 8, 0, 8)
    }

    private fun section(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(82, 190, 255))
        setPadding(0, 22, 0, 6)
    }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun insertKey(key: String) {
        when (key) {
            "Ctrl" -> { ctrl = !ctrl; status.text = "Ctrl: ${if (ctrl) "ON" else "OFF"} (visual shortcut state)" }
            "Alt" -> { alt = !alt; status.text = "Alt: ${if (alt) "ON" else "OFF"} (visual shortcut state)" }
            "Shift" -> status.text = "Shift selected (text entry uses Android keyboard)"
            "Tab" -> commandInput.text.insert(commandInput.selectionStart.coerceAtLeast(0), "\t")
            "Enter" -> commandInput.text.insert(commandInput.selectionStart.coerceAtLeast(0), "\n")
            "Esc" -> status.text = "Esc pressed"
            "↑", "↓", "←", "→", "Home", "End", "PgUp", "PgDn" -> status.text = "$key selected (terminal jobs are non-interactive)"
        }
    }

    private fun dispatch(type: String, payload: String) {
        val token = vault.read()
        if (token.isNullOrBlank()) { showError("Da farko saka GitHub token mai izinin Actions read/write."); return }
        status.text = "Status: Ana aika aikin..."
        activityScope.launch {
            try {
                withContext(Dispatchers.IO) { GitHubClient(token).dispatch(type, payload) }
                status.text = "Status: An aika aikin. Danna CHECK LATEST JOB STATUS."
            } catch (e: Exception) { showError(e.message ?: "Aika aikin ya gagara") }
        }
    }

    private fun checkStatus() {
        val token = vault.read() ?: run { showError("Ajiye GitHub token da farko."); return }
        status.text = "Status: Ana duba..."
        lifecycleScope.launch {
            try {
                val run = withContext(Dispatchers.IO) { GitHubClient(token).latestRun() }
                if (run == null) status.text = "Status: Ba a sami job ba tukuna."
                else {
                    status.text = "Status: ${run.status} / ${run.conclusion ?: "pending"}"
                    output.text = "Run ID: ${run.runId}\n${run.htmlUrl}"
                }
            } catch (e: Exception) { showError(e.message ?: "Duba status ya gagara") }
        }
    }


    private fun checkBuildStatus() {
        val token = vault.read() ?: run { showError("Ajiye GitHub token da farko."); return }
        status.text = "Status: Ana duba APK build..."
        lifecycleScope.launch {
            try {
                val run = withContext(Dispatchers.IO) { GitHubClient(token).latestBuildRun() }
                if (run == null) status.text = "Status: Ba a sami APK build ba tukuna."
                else {
                    status.text = "APK Build: ${run.status} / ${run.conclusion ?: "pending"}"
                    output.text = "APK build run ID: ${run.runId}\\n${run.htmlUrl}"
                }
            } catch (e: Exception) { showError(e.message ?: "Duba APK build ya gagara") }
        }
    }

    private fun openLatestRun() {
        val token = vault.read() ?: run { showError("Ajiye GitHub token da farko."); return }
        lifecycleScope.launch {
            try {
                val run = withContext(Dispatchers.IO) { GitHubClient(token).latestRun() }
                if (run != null) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl)))
                else showError("Babu job tukuna.")
            } catch (e: Exception) { showError(e.message ?: "An samu kuskure") }
        }
    }

    private fun openArtifact() {
        val token = vault.read() ?: run { showError("Ajiye GitHub token da farko."); return }
        lifecycleScope.launch {
            try {
                val url = withContext(Dispatchers.IO) { GitHubClient(token).latestArtifactUrl() }
                if (url != null) {
                    output.text = "Artifact API link:\n$url\nKa buɗe GitHub Actions domin sauke artifact idan ya gama."
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hamitbull/ASHIRU-CLOUD-PC/actions")))
                } else showError("Ba a sami artifact ba. Jira job ya gama.")
            } catch (e: Exception) { showError(e.message ?: "Artifact bai samu ba") }
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun showError(message: String) {
        status.text = "Error: $message"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
