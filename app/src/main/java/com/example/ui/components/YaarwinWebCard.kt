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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.util.PeriodUtils
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
import androidx.compose.runtime.LaunchedEffect
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

    val gameCode = when (currentGameMode) {
        "30s" -> "WinGo_30S"
        "3Min" -> "WinGo_3M"
        "5Min" -> "WinGo_5M"
        "10Min" -> "WinGo_10M"
        else -> "WinGo_1M"
    }
    val yaarwinWingoUrl = "https://www.20yaarwin.com/#/saasLottery/WinGo?gameCode=$gameCode&lottery=WinGo"
    val wingoAnalystUrl = "https://wingoanalyst.com/#/wingo_1m"
    val tirangaWingoUrl = "https://www.tirangagames.in/#/saasLottery/WinGo"
    val damanWingoUrl = "https://damangames.in/#/saasLottery/WinGo"
    val yaarwinClubUrl = "https://yaarwin.club"

    var currentActiveUrl by remember(currentGameMode) { mutableStateOf(yaarwinWingoUrl) }
    var customUrlInput by remember { mutableStateOf("") }
    var showCustomUrlBar by remember { mutableStateOf(false) }

    LaunchedEffect(currentGameMode) {
        if (currentActiveUrl.contains("20yaarwin.com")) {
            currentActiveUrl = yaarwinWingoUrl
            webViewRef?.loadUrl(yaarwinWingoUrl)
        }
    }

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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        currentActiveUrl = yaarwinWingoUrl
                        webViewRef?.loadUrl(yaarwinWingoUrl)
                        showCustomUrlBar = false
                    },
                    modifier = Modifier.weight(1.3f).height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentActiveUrl == yaarwinWingoUrl) NeonGold else ImmersiveCardBorder
                    )
                ) {
                    Text(
                        text = "20YAARWIN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentActiveUrl == yaarwinWingoUrl) ImmersiveSurface else TextPrimary
                    )
                }

                Button(
                    onClick = {
                        currentActiveUrl = tirangaWingoUrl
                        webViewRef?.loadUrl(tirangaWingoUrl)
                        showCustomUrlBar = false
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentActiveUrl == tirangaWingoUrl) NeonGold else ImmersiveCardBorder
                    )
                ) {
                    Text(
                        text = "TIRANGA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentActiveUrl == tirangaWingoUrl) ImmersiveSurface else TextPrimary
                    )
                }

                Button(
                    onClick = {
                        currentActiveUrl = damanWingoUrl
                        webViewRef?.loadUrl(damanWingoUrl)
                        showCustomUrlBar = false
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentActiveUrl == damanWingoUrl) NeonGold else ImmersiveCardBorder
                    )
                ) {
                    Text(
                        text = "DAMAN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentActiveUrl == damanWingoUrl) ImmersiveSurface else TextPrimary
                    )
                }

                Button(
                    onClick = {
                        showCustomUrlBar = !showCustomUrlBar
                    },
                    modifier = Modifier.weight(0.9f).height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCustomUrlBar) LiveGreen else ImmersiveCardBorder
                    )
                ) {
                    Text(
                        text = "+ URL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showCustomUrlBar) ImmersiveSurface else TextPrimary
                    )
                }
            }

            if (showCustomUrlBar) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = { Text("Paste custom WinGo platform URL...", fontSize = 10.sp, color = TextMuted) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedContainerColor = ImmersiveCardBorder.copy(alpha = 0.3f),
                            unfocusedContainerColor = ImmersiveCardBorder.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (customUrlInput.isNotBlank()) {
                                var formatted = customUrlInput.trim()
                                if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                    formatted = "https://$formatted"
                                }
                                currentActiveUrl = formatted
                                webViewRef?.loadUrl(formatted)
                            }
                        },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LiveGreen)
                    ) {
                        Text("LOAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveSurface)
                    }
                }
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
                    webViewRef?.loadUrl(yaarwinWingoUrl)
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RELOAD 20YAARWIN WINGO GAME",
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
                            var pId = String(item.period || item.issueNumber || item.periodId || item.issue).replace(/\*/g, '').trim();
                            var num = item.number !== undefined ? item.number : (item.winningNumber !== undefined ? item.winningNumber : item.result);
                            if (num !== undefined && num !== null && !isNaN(parseInt(num))) {
                                parsedRecords.push({
                                    periodId: pId,
                                    number: parseInt(num),
                                    bigSmall: item.bigSmall || item.size || (parseInt(num) >= 5 ? 'BIG' : 'SMALL')
                                });
                            }
                        }
                    });

                    if (parsedRecords.length > 0 && window.YaarwinBridge) {
                        window.YaarwinBridge.onYaarwinHistoryReceived(JSON.stringify(parsedRecords));
                    }
                }

                if (!window.__yaarwin_interval) {
                    window.__yaarwin_interval = setInterval(function() {
                        try {
                            scanDomAndReport();
                        } catch(e) {}
                    }, 2500);
                }

                function scanDomAndReport() {
                    var records = [];
                    var seenPeriods = {};

                    // 1. Scan entire page text for patterns like "*010570 3 Small" or "010570 3 Small"
                    var fullText = document.body ? (document.body.innerText || '') : '';
                    var globalRegex = /\*?(\d{5,20})[\s\n\t]+([0-9])[\s\n\t]+(BIG|SMALL|Big|Small)/gi;
                    var match;
                    while ((match = globalRegex.exec(fullText)) !== null) {
                        var pId = match[1];
                        var num = parseInt(match[2]);
                        var bs = match[3].toUpperCase();
                        if (!seenPeriods[pId]) {
                            seenPeriods[pId] = true;
                            records.push({
                                periodId: pId,
                                number: num,
                                bigSmall: bs
                            });
                        }
                    }

                    // 2. Scan DOM elements (tables, list items, etc.)
                    var selectors = [
                        'tr', '.record-item', '.van-list__item', '.list-item', 
                        'div[class*="period"]', 'div[class*="history"]', 'div[class*="game"]', '.van-row', 'li'
                    ];

                    selectors.forEach(function(sel) {
                        var elements = document.querySelectorAll(sel);
                        elements.forEach(function(el) {
                            var text = el.innerText || '';
                            var m = text.match(/\*?(\d{5,20})[\s\S]*?\b([0-9])\b[\s\S]*?(BIG|SMALL|Big|Small|green|red|violet)?/i);
                            if (m) {
                                var pId = m[1];
                                var num = parseInt(m[2]);
                                var bs = m[3] ? m[3].toUpperCase() : (num >= 5 ? 'BIG' : 'SMALL');
                                if (!seenPeriods[pId] && num >= 0 && num <= 9) {
                                    seenPeriods[pId] = true;
                                    records.push({
                                        periodId: pId,
                                        number: num,
                                        bigSmall: bs
                                    });
                                }
                            }
                        });
                    });

                    if (window.YaarwinBridge && records.length > 0) {
                        window.YaarwinBridge.onYaarwinHistoryReceived(JSON.stringify(records));
                    }
                }

                scanDomAndReport();
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
        val rawPId = obj.optString("periodId", obj.optString("period", obj.optString("issueNumber", obj.optString("issue", ""))))
            .replace("*", "").trim()
        var num = obj.optInt("number", -1)
        if (num == -1) num = obj.optInt("winningNumber", -1)
        if (num == -1) num = obj.optInt("result", -1)

        val cleanDigits = rawPId.filter { it.isDigit() }
        if (cleanDigits.length >= 4 && num in 0..9) {
            val pId = PeriodUtils.normalizePeriodId(rawPId, gameMode)
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
                timestamp = System.currentTimeMillis(),
                isRealVerified = true
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

