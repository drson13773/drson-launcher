package com.drson.launcher.music

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class YouTubeHiddenPlayer(
    private val context: Context,
    private val onStateChange: (Boolean, String) -> Unit
) {
    val webView: WebView = WebView(context).apply {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            domStorageEnabled = true
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {}

        loadIFrameHtml()
    }

    private fun loadIFrameHtml() {
        val htmlData = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>body { margin: 0; background-color: #000; overflow: hidden; }</style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 0,
                                'playsinline': 1,
                                'rel': 0,
                                'disablekb': 1,
                                'fs': 0,
                                'modestbranding': 1
                            },
                            events: {
                                'onReady': function(event) { Android.onPlayerReady(); },
                                'onStateChange': function(event) {
                                    if (event.data == YT.PlayerState.PLAYING) {
                                        Android.onStateChanged(true, player.getVideoData().title);
                                    } else if (event.data == YT.PlayerState.PAUSED || event.data == YT.PlayerState.ENDED) {
                                        Android.onStateChanged(false, "");
                                    }
                                }
                            }
                        });
                    }

                    function playVideo(videoId) {
                        if (player && player.loadVideoById) {
                            player.loadVideoById(videoId);
                        }
                    }

                    function togglePlay(play) {
                        if (player) {
                            if (play) player.playVideo();
                            else player.pauseVideo();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
    }

    fun loadAndPlay(videoId: String) {
        webView.post {
            webView.evaluateJavascript("javascript:playVideo('$videoId');", null)
        }
    }

    fun play() {
        webView.post {
            webView.evaluateJavascript("javascript:togglePlay(true);", null)
        }
    }

    fun pause() {
        webView.post {
            webView.evaluateJavascript("javascript:togglePlay(false);", null)
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onPlayerReady() {}

        @JavascriptInterface
        fun onStateChanged(isPlaying: Boolean, title: String) {
            onStateChange(isPlaying, title)
        }
    }
}
