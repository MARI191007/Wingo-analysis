package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PeriodRecord
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun YaarwinWebCard(
    currentGameMode: String,
    onHistoryExtracted: (List<PeriodRecord>) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var syncStatusText by remember { mutableStateOf("Open 20yaarwin.com WinGo history -> Click SYNC or let auto-interceptor fetch history.") }
    var lastSyncedCount by remember { mutableStateOf(0) }
    var isExpandedHeight by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val wingoAnalystUrl = "https://wingoanalyst.com/#/wingo_1m"
    val yaarwinWingoUrl = "https://www.20yaarwin.com/#/saasLottery/WinGo?gameCode=WinGo_30S&lottery=WinGo"
    val yaarwinClubUrl = "https://yaarwin.club"

    var currentActiveUrl by remember { mutableStateOf(wingoAnalystUrl) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ImmersiveSurface)
            .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .animateContentSize()
    ) {
        // Header Bar with Yaarwin status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = NeonGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "YAARWIN LIVE PORTAL & PREDICTOR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (currentActiveUrl.contains("wingoanalyst")) "wingoanalyst.com Live 1M Stream" else if (currentActiveUrl.contains("saasLottery")) "20yaarwin.com WinGo 30S Game" else "yaarwin.club Portal",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LiveGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LiveGreen
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { isExpandedHeight = !isExpandedHeight },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpandedHeight) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Expand",
                        tint = NeonGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fast URL switcher chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    currentActiveUrl = wingoAnalystUrl
                    webViewRef?.loadUrl(wingoAnalystUrl)
                },
                modifier = Modifier.weight(1.3f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentActiveUrl == wingoAnalystUrl) NeonGold else ImmersiveCardBorder
                )
            ) {
                Text(
                    text = "WINGOANALYST 1M",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentActiveUrl == wingoAnalystUrl) ImmersiveSurface else TextPrimary
                )
            }

            Button(
                onClick = {
                    currentActiveUrl = yaarwinWingoUrl
                    webViewRef?.loadUrl(yaarwinWingoUrl)
                },
                modifier = Modifier.weight(1.1f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentActiveUrl == yaarwinWingoUrl) NeonGold else ImmersiveCardBorder
                )
            ) {
                Text(
                    text = "20YAARWIN",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentActiveUrl == yaarwinWingoUrl) ImmersiveSurface else TextPrimary
                )
            }

            Button(
                onClick = {
                    currentActiveUrl = yaarwinClubUrl
                    webViewRef?.loadUrl(yaarwinClubUrl)
                },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentActiveUrl == yaarwinClubUrl) NeonGold else ImmersiveCardBorder
                )
            ) {
                Text(
                    text = "YAARWIN.CLUB",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentActiveUrl == yaarwinClubUrl) ImmersiveSurface else TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = NeonGold,
                trackColor = ImmersiveCardBorder
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // WebView Container with full touch scrolling support
        val containerHeight = if (isExpandedHeight) 520.dp else 360.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = true

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                        }

                        // Allow smooth inner scrolling inside Compose parent
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onYaarwinHistoryReceived(jsonStr: String) {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        val records = parseYaarwinJson(jsonStr, currentGameMode)
                                        if (records.isNotEmpty()) {
                                            lastSyncedCount = records.size
                                            syncStatusText = "Extracted ${records.size} drawn period records from 20yaarwin! History saved permanently offline."
                                            onHistoryExtracted(records)
                                        }
                                    }
                                }
                            },
                            "YaarwinBridge"
                        )

                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                injectYaarwinScraperScript(view)
                            }
                        }

                        loadUrl(currentActiveUrl)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveCardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (lastSyncedCount > 0) Icons.Default.CheckCircle else Icons.Default.Sync,
                contentDescription = null,
                tint = if (lastSyncedCount > 0) LiveGreen else NeonGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = syncStatusText,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    webViewRef?.let { view ->
                        injectYaarwinScraperScript(view)
                    }
                },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGold)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ImmersiveSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYNC WINGO HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ImmersiveSurface
                )
            }

            Button(
                onClick = {
                    webViewRef?.loadUrl(yaarwinWingoUrl)
                },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "20YAARWIN WINGO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun injectYaarwinScraperScript(webView: WebView?) {
    if (webView == null) return
    val jsScript = """
        (function() {
            try {
                // Intercept XHR / Fetch network requests for live WinGo game history
                if (!window.__yaarwin_hooked) {
                    window.__yaarwin_hooked = true;
                    
                    var origFetch = window.fetch;
                    if (origFetch) {
                        window.fetch = function() {
                            return origFetch.apply(this, arguments).then(function(response) {
                                try {
                                    var clone = response.clone();
                                    clone.json().then(function(data) {
                                        processNetworkData(data);
                                    }).catch(function(e){});
                                } catch(e){}
                                return response;
                            });
                        };
                    }

                    var origXhrOpen = XMLHttpRequest.prototype.open;
                    var origXhrSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        this._url = url;
                        return origXhrOpen.apply(this, arguments);
                    };
                    XMLHttpRequest.prototype.send = function() {
                        this.addEventListener('load', function() {
                            try {
                                if (this.responseText) {
                                    var data = JSON.parse(this.responseText);
                                    processNetworkData(data);
                                }
                            } catch(e){}
                        });
                        return origXhrSend.apply(this, arguments);
                    };
                }

                function processNetworkData(data) {
                    if (!data) return;
                    var list = [];
                    if (Array.isArray(data)) list = data;
                    else if (data.data && Array.isArray(data.data)) list = data.data;
                    else if (data.data && data.data.list && Array.isArray(data.data.list)) list = data.data.list;
                    else if (data.data && data.data.rows && Array.isArray(data.data.rows)) list = data.data.rows;
                    else if (data.list && Array.isArray(data.list)) list = data.list;

                    var parsedRecords = [];
                    list.forEach(function(item) {
                        if (item && (item.period || item.issueNumber || item.periodId || item.issue)) {
                            parsedRecords.push({
                                periodId: item.period || item.issueNumber || item.periodId || item.issue,
                                number: item.number !== undefined ? item.number : (item.winningNumber !== undefined ? item.winningNumber : item.result),
                                bigSmall: item.bigSmall || item.size || (item.number >= 5 ? 'BIG' : 'SMALL')
                            });
                        }
                    });

                    if (parsedRecords.length > 0 && window.YaarwinBridge) {
                        window.YaarwinBridge.onYaarwinHistoryReceived(JSON.stringify(parsedRecords));
                    }
                }

                // Also scan active DOM nodes
                var records = [];
                var selectors = [
                    'tr', '.record-item', '.van-list__item', '.list-item', 
                    'div[class*="period"]', 'div[class*="history"]', 'div[class*="game"]', '.van-row'
                ];

                selectors.forEach(function(sel) {
                    var elements = document.querySelectorAll(sel);
                    elements.forEach(function(el) {
                        var text = el.innerText || '';
                        var match = text.match(/(\d{10,20})[\s\S]*?(\d)[\s\S]*?(BIG|SMALL|Big|Small|green|red|violet)?/i);
                        if (match) {
                            records.push({
                                periodId: match[1],
                                number: parseInt(match[2]),
                                bigSmall: match[3] ? match[3].toUpperCase() : (parseInt(match[2]) >= 5 ? 'BIG' : 'SMALL')
                            });
                        }
                    });
                });

                if (window.YaarwinBridge && records.length > 0) {
                    window.YaarwinBridge.onYaarwinHistoryReceived(JSON.stringify(records));
                }
            } catch (err) {
                console.error(err);
            }
        })();
    """.trimIndent()

    webView.evaluateJavascript(jsScript, null)
}

private fun parseYaarwinJson(jsonStr: String, gameMode: String): List<PeriodRecord> {
    val list = mutableListOf<PeriodRecord>()
    try {
        if (jsonStr.startsWith("[")) {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                parseObjectToRecord(obj, gameMode)?.let { list.add(it) }
            }
        } else if (jsonStr.startsWith("{")) {
            val obj = JSONObject(jsonStr)
            parseObjectToRecord(obj, gameMode)?.let { list.add(it) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun parseObjectToRecord(obj: JSONObject, gameMode: String): PeriodRecord? {
    try {
        val pId = obj.optString("periodId", obj.optString("period", obj.optString("issueNumber", obj.optString("issue", ""))))
        var num = obj.optInt("number", -1)
        if (num == -1) num = obj.optInt("winningNumber", -1)
        if (num == -1) num = obj.optInt("result", -1)

        if (pId.length >= 8 && num in 0..9) {
            val bs = if (num >= 5) "BIG" else "SMALL"
            val col = when (num) {
                0, 5 -> "VIOLET"
                1, 3, 7, 9 -> "GREEN"
                else -> "RED"
            }
            return PeriodRecord(
                periodId = pId,
                gameMode = gameMode,
                number = num,
                bigSmall = bs,
                color = col,
                timestamp = System.currentTimeMillis()
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

