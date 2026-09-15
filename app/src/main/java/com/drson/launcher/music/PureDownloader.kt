package com.drson.launcher.music

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class PureDownloader private constructor() : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = Request.Builder().url(url)

        headers.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        if (httpMethod.equals("POST", ignoreCase = true)) {
            val body = dataToSend?.toRequestBody() ?: ByteArray(0).toRequestBody()
            requestBuilder.post(body)
        } else {
            requestBuilder.get()
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers.values(name)
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }

    companion object {
        val instance: PureDownloader by lazy { PureDownloader() }
    }
}
