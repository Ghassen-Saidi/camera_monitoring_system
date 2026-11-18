package com.example.cameramonitoring

import android.content.Intent
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

private data class Camera(
    val id: Int,
    val status: String,
    val type: String,
    val streamUrl: String
)

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraContainer: LinearLayout
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
        refreshCameras()
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Camera Monitoring"
            textSize = 24f
            setTextColor(Color.rgb(21, 101, 192))
            setPadding(0, 0, 0, 16)
        }
        root.addView(title)

        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        actions.addView(button("Refresh") { refreshCameras() })
        actions.addView(button("Start All") { sendAllAction("START") })
        actions.addView(button("Stop All") { sendAllAction("STOP") })
        root.addView(actions)

        statusText = TextView(this).apply {
            text = "Loading cameras..."
            setPadding(0, 16, 0, 16)
        }
        root.addView(statusText)

        cameraContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(
            ScrollView(this).apply { addView(cameraContainer) },
            LinearLayout.LayoutParams(-1, 0, 1f)
        )
        return root
    }

    private fun refreshCameras() {
        statusText.text = "Loading cameras..."
        executor.execute {
            try {
                val cameras = parseCameras(request("/cameras", "GET"))
                runOnUiThread {
                    renderCameras(cameras)
                    statusText.text = "${cameras.size} camera(s) loaded"
                }
            } catch (error: Exception) {
                runOnUiThread { showError(error.message ?: "Could not reach the server") }
            }
        }
    }

    private fun renderCameras(cameras: List<Camera>) {
        cameraContainer.removeAllViews()
        cameras.forEach { camera ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.rgb(245, 247, 250))
            }
            card.addView(TextView(this).apply {
                text = "Camera ${camera.id}"
                textSize = 19f
                setTextColor(Color.DKGRAY)
            })
            card.addView(TextView(this).apply {
                text = "${camera.status}  •  ${camera.type}\n${camera.streamUrl.ifBlank { "No stream configured" }}"
                setPadding(0, 8, 0, 12)
            })
            val buttons = LinearLayout(this)
            buttons.addView(button("Start") { sendAction(camera.id, "START") })
            buttons.addView(button("Stop") { sendAction(camera.id, "STOP") })
            if (camera.streamUrl.isNotBlank()) {
                buttons.addView(button("Open Stream") { openStream(camera) })
            }
            card.addView(buttons)
            cameraContainer.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 16) }
            )
        }
    }

    private fun sendAction(id: Int, action: String) = executor.execute {
        try {
            post("/cameras/$id/record", "{\"action\":\"$action\"}")
            runOnUiThread {
                statusText.text = "Camera $id set to $action"
                refreshCameras()
            }
        } catch (error: Exception) {
            runOnUiThread { showError(error.message ?: "Action failed") }
        }
    }

    private fun sendAllAction(action: String) = executor.execute {
        try {
            val payload = JSONObject(post("/cameras/all/record", "{\"action\":\"$action\"}"))
            val succeeded = payload.optInt("succeeded", 0)
            val failed = payload.optInt("failed", 0)
            val message = payload.optString("message", "Bulk command completed")
            val failedIds = mutableListOf<String>()
            val results = payload.optJSONArray("results") ?: JSONArray()
            for (index in 0 until results.length()) {
                val result = results.optJSONObject(index) ?: continue
                if (!result.optBoolean("success", false)) {
                    failedIds.add(result.optString("cameraId", "?"))
                }
            }

            runOnUiThread {
                statusText.text = if (failed == 0) {
                    message
                } else {
                    "$message. Failed camera IDs: ${failedIds.joinToString(", ")}"
                }
                if (failed > 0) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                refreshCameras()
            }
        } catch (error: Exception) {
            runOnUiThread { showError(error.message ?: "Action failed") }
        }
    }

    private fun openStream(camera: Camera) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(camera.streamUrl))
            if (intent.resolveActivity(packageManager) == null) {
                showError("No installed app can open this ${camera.type} stream")
                return
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showError("No installed app can open this stream")
        } catch (_: IllegalArgumentException) {
            showError("Invalid stream URL")
        }
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        return (URL(BuildConfig.API_BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 5000
            if (BuildConfig.API_KEY.isNotBlank()) setRequestProperty("X-API-Key", BuildConfig.API_KEY)
        }
    }

    private fun request(path: String, method: String, body: String? = null): String {
        val connection = openConnection(path, method)
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { BufferedReader(InputStreamReader(it)).readText() }.orEmpty()
            if (responseCode !in 200..299) error("HTTP $responseCode: $response")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun post(path: String, body: String): String = request(path, "POST", body)

    private fun parseCameras(json: String): List<Camera> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Camera(
                item.optInt("cameraId", item.optInt("id", index)),
                item.optString("status"),
                item.optString("type", "HTTP"),
                item.optString("streamUrl")
            )
        }
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun showError(message: String) {
        statusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
