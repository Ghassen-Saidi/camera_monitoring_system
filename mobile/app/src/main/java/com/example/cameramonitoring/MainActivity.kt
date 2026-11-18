package com.example.cameramonitoring

import android.content.Intent
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
        root.addView(ScrollView(this).apply { addView(cameraContainer) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }

    private fun refreshCameras() {
        statusText.text = "Loading cameras..."
        executor.execute {
            try {
                val connection = openConnection("/cameras", "GET")
                val cameras = parseCameras(readBody(connection))
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
            cameraContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 16) })
        }
    }

    private fun sendAction(id: Int, action: String) = executor.execute {
        try {
            post("/cameras/$id/record", "{\"action\":\"$action\"}")
            runOnUiThread { statusText.text = "Camera $id set to $action"; refreshCameras() }
        } catch (error: Exception) { runOnUiThread { showError(error.message ?: "Action failed") } }
    }

    private fun sendAllAction(action: String) = executor.execute {
        try {
            post("/cameras/all/record", "{\"action\":\"$action\"}")
            runOnUiThread { statusText.text = "All cameras set to $action"; refreshCameras() }
        } catch (error: Exception) { runOnUiThread { showError(error.message ?: "Action failed") } }
    }

    private fun openStream(camera: Camera) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(camera.streamUrl)))
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        return (URL(BuildConfig.API_BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 5000
        }
    }

    private fun readBody(connection: HttpURLConnection): String {
        connection.inputStream.use { stream ->
            return BufferedReader(InputStreamReader(stream)).readText()
        }
    }

    private fun post(path: String, body: String) {
        openConnection(path, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
            if (responseCode !in 200..299) error("HTTP $responseCode")
            disconnect()
        }
    }

    private fun parseCameras(json: String): List<Camera> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Camera(item.optInt("cameraId", item.optInt("camerald")), item.optString("status"), item.optString("type", "HTTP"), item.optString("streamUrl"))
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
