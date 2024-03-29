package cn.scyutao.yutaohttp.request

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import cn.scyutao.yutaohttp.partial.partially1
import cn.scyutao.yutaohttp.upload.UploadRequest
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class RequestWrapper {
    internal lateinit var _request: ByteRequest
    var method: Int = Request.Method.GET
    var url: String = ""
    var raw: String? = null
    var tag: Any? = null
    private var _start: (() -> Unit) = {}
    private var _success: (String) -> Unit = {}
    private var _fail: (VolleyError) -> Unit = {}
    private var _finish: (() -> Unit) = {}
    var _params: MutableMap<String, String> = mutableMapOf()
    var _fileParams: MutableMap<String, String> = mutableMapOf()
    var _headers: MutableMap<String, String> = mutableMapOf()


    fun onStart(onStart: () -> Unit) {
        _start = onStart
    }

    fun onFail(onError: (VolleyError) -> Unit) {
        _fail = onError
    }

    fun onSuccess(onSuccess: (String) -> Unit) {
        _success = onSuccess
    }

    fun onFinish(onFinish: () -> Unit) {
        _finish = onFinish
    }

    val pairs = fun(map: MutableMap<String, String>, makePairs: RequestPairs.() -> Unit) {
        val requestPair = RequestPairs()
        requestPair.makePairs()
        map.putAll(requestPair.pairs)
    }

    val params = pairs.partially1(_params)
    val headers = pairs.partially1(_headers)
    val files = pairs.partially1(_fileParams)

    fun excute() {
        var url = url
        if (url.subSequence(0, 7) != "http://" && url.subSequence(0, 8) != "https://") {
            if (Http.baseUrl.isNotEmpty()) {
                url = Http.baseUrl + url
            }
        }


        if (Request.Method.GET == method) {
            url = getGetUrl(url, _params) { it.toQueryString() }
        }
        _request = getRequest(method, url) {
            if (Http.debug) {
                val logStr = StringBuffer()
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
                logStr.append("──────────────────────────────── $time ────────────────────────────────\n")
                logStr.append("┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────\n")
                logStr.append("│请求地址：$url\n")
                logStr.append("│headers：$_headers\n")
                if (_params.isNotEmpty()) {
                    logStr.append("│ params：$_params\n")
                }
                if (raw != null) {
                    logStr.append("│    raw：$raw\n")
                }
                logStr.append("│请求结果：$it\n")
                logStr.append("└────────────────────────────────────────────────────────────────────────────────────────────────────────────────")
                Log.e("YuTaoHttp", logStr.toString())
            }
            _fail(it)
            _finish()
        }
        _request.retryPolicy = DefaultRetryPolicy(60 * 1000, 0, 1.0f)
        _request._listener = Response.Listener {
            if (Http.debug) {
                val logStr = StringBuffer()
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
                logStr.append("──────────────────────────────── $time ────────────────────────────────\n")
                logStr.append("┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────\n")
                logStr.append("│请求地址：$url\n")
                logStr.append("│headers：$_headers\n")
                if (_params.isNotEmpty()) {
                    logStr.append("│ params：$_params\n")
                }
                if (raw != null) {
                    logStr.append("│    raw：$raw\n")
                }
                logStr.append("│请求结果：${String(it)}\n")
                logStr.append("└────────────────────────────────────────────────────────────────────────────────────────────────────────────────")
                Log.d("YuTaoHttp", logStr.toString())
            }
            _success(String(it))
            _finish()
        }
        fillRequest()
        Http.getRequestQueue().add(_request)
        _start()
    }

    open fun fillRequest() {
        val request = _request
        if (tag != null) {
            request.tag = tag
        }
        // 添加 headers
        if (_headers.isEmpty()) {
            _headers = Http.headers
        }
        if (_headers.isNotEmpty()) {
            request.headers = _headers
        }

        // 设置 params
        request.params = _params

        if (request is UploadRequest) {
            request.fileParams = _fileParams
        }

    }

    open fun getRequest(
        method: Int, url: String, errorListener: Response.ErrorListener? = Response
            .ErrorListener {}
    ): ByteRequest {
        return if (!raw.isNullOrEmpty() && method in Request.Method.POST..Request.Method.PUT) {
            JsonRequest(method, url, raw!!, errorListener)
        } else if (method == Request.Method.POST && _fileParams.isNotEmpty()) {
            UploadRequest(url, errorListener)
        } else {
            ByteRequest(method, url, errorListener)
        }
    }

    private fun getGetUrl(
        url: String,
        params: MutableMap<String, String>,
        toQueryString: (map: Map<String, String>) ->
        String
    ): String {
        return if (params.isEmpty()) url else "$url?${toQueryString(params)}"
    }

    private fun <K, V> Map<K, V>.toQueryString(): String =
        this.map { "${it.key}=${it.value}" }.joinToString("&")
}

class RequestPairs {
    var pairs: MutableMap<String, String> = HashMap()
    operator fun String.minus(value: String) {
        pairs.put(this, value)
    }
}

@SuppressLint("StaticFieldLeak")
object Http {
    private var mRequestQueue: RequestQueue? = null

    /**
     * 调试模式  是否打印请求日志
     */
    var debug = false
    var baseUrl = ""

    /**
     * 连接超时时间 默认60秒
     */
    var connectTimeout = 60L
    /**
     * 读取超时时间 默认60秒
     */
    var readTimeout = 60L
    /**
     * 输出超时时间 默认60秒
     */
    var writeTimeout = 60L
    /**
     * ping超时时间 默认60秒
     */
    var pingInterval = 5L
    var headers = HashMap<String, String>()

    /**
     * 缓存设置 如：Cache(File(externalCacheDir, "YUTAOHTTP"), 1024 * 1024 * 50)
     */
    var cache: Cache? = null

    /**
     * okHttpClient.addNetworkInterceptor()
     */
    var networkInterceptor: Interceptor? = null
    var interceptors = ArrayList<Interceptor>()
    lateinit var mcontext: Context
    fun init(context: Context) {
        mcontext = context
        mRequestQueue
        // Set up the network to use OKHttpURLConnection as the HTTP client.
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .pingInterval(pingInterval, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
        if (cache != null) {
            okHttpClient.cache(cache)
        }
        if (networkInterceptor != null) {
            networkInterceptor?.let { okHttpClient.addNetworkInterceptor(it) }
        }
        for (item in interceptors) {
            okHttpClient.addInterceptor(item)
        }
        val build = okHttpClient.build()
        mRequestQueue =
            Volley.newRequestQueue(context.applicationContext, OkHttpStack(build))
    }

    fun getRequestQueue(): RequestQueue {
        return mRequestQueue!!
    }

    val request: (Int, RequestWrapper.() -> Unit) -> Request<ByteArray> = { method, init ->
        val baseRequest = RequestWrapper()
        baseRequest.method = method
        baseRequest.init() // 执行闭包，完成数据填充
        baseRequest.excute() // 添加到执行队列，自动执行
        baseRequest._request // 用于返回
    }

    val get = request.partially1(Request.Method.GET)
    val post = request.partially1(Request.Method.POST)
    val put = request.partially1(Request.Method.PUT)
    val delete = request.partially1(Request.Method.DELETE)
    val head = request.partially1(Request.Method.HEAD)
    val options = request.partially1(Request.Method.OPTIONS)
    val trace = request.partially1(Request.Method.TRACE)
    val patch = request.partially1(Request.Method.PATCH)
    val upload = request.partially1(Request.Method.POST)
}
