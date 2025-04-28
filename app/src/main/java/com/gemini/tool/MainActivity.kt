package com.gemini.tool


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val results = if (result.resultCode == RESULT_OK && result.data?.data != null) {
            arrayOf(result.data!!.data!!)
        } else {
            null
        }
        fileChooserCallback?.onReceiveValue(results)
        fileChooserCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 Hide ActionBar and enable full-screen immersive mode
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // 🛠 WebView Settings
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        // 🌐 WebView Client (for handling inside navigation)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false  // Allow WebView to load the URL inside the app
            }

            override fun onPageFinished(view: WebView, url: String) {
                // 🧹 Remove header and auto-scroll input field
                view.evaluateJavascript(
                    """
                    (function() {
                        var header = document.querySelector('header');
                        if (header) header.style.display = 'none';

                        var input = document.getElementById('chat_input');
                        if (input) {
                            input.addEventListener('focus', function() {
                                setTimeout(function() {
                                    window.scrollTo(0, input.offsetTop - 150);
                                }, 300);
                            });
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }

        // 📂 WebChromeClient for File Uploads
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*" // Accept any file type; change to "application/pdf" for PDFs only
                }
                fileChooserLauncher.launch(Intent.createChooser(intent, "Select File"))
                return true
            }
        }

        // 🌍 Load website
        webView.loadUrl("https://huggingface.co/spaces/openfree/PDF-RAG")

        // 🆙 Push WebView when Keyboard opens
        val rootView = findViewById<View>(R.id.main)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is open → Scroll to show chat input
                webView.evaluateJavascript(
                    "document.getElementById('chat_input')?.scrollIntoView({behavior: 'smooth', block: 'center'});",
                    null
                )
            }
        }
    }
}
