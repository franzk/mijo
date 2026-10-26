package com.franzk.mijo

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.franzk.mijo.data.RecipeBook
import com.franzk.mijo.platform.Host
import com.franzk.mijo.platform.KeyValueStore
import com.franzk.mijo.ui.App
import com.franzk.mijo.ui.AppState

class MainActivity : ComponentActivity(), Host {

    private lateinit var voice: VoskVoiceEngine
    private val appState = AppState()
    private var permissionCallback: ((Boolean) -> Unit)? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionCallback?.invoke(granted)
        permissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val book = RecipeBook(PrefsStore(getSharedPreferences("mijo", Context.MODE_PRIVATE)))
        voice = VoskVoiceEngine(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!appState.back()) moveTaskToBack(true)
            }
        })

        setContent { App(appState, book, voice, this) }
    }

    override fun onResume() {
        super.onResume()
        voice.onHostResume()
    }

    override fun onPause() {
        voice.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        voice.shutdown()
        super.onDestroy()
    }

    // ------------------------------------------------------------ Host

    override fun keepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun hasMicPermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun requestMicPermission(onResult: (Boolean) -> Unit) {
        permissionCallback = onResult
        micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
}

private class PrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getInt(key: String): Int? = if (prefs.contains(key)) prefs.getInt(key, 0) else null
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}
