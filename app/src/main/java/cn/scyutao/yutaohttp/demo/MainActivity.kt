package cn.scyutao.yutaohttp.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cn.scyutao.yutaohttp.request.Http
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.Interceptor
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapHeader = HashMap<String, String>()
        mapHeader["token"] = "thisfasjkfhkajlshfkl"
        Http.init(this)
        Http.debug = true
        Http.cache = Cache(File(externalCacheDir, "YUTAOHTTP"), 1024 * 1024 * 50)

        Http.baseUrl = "https://jkmb.scyutao.com/"
        Http.headers = mapHeader
        Http.networkInterceptor = NetCacheInterceptor

        val map = HashMap<String, String>()
        map["token"] = "123"
        map["sss"] = "456"
        get("/wxminiapp/v5.user/login", map) {
            Log.e("222", "$it")
        }
    }

    fun get(urlStr: String, paramss: HashMap<String, String>, success: (String) -> Unit) {
        Http.get {
            url = urlStr
            _params = paramss
            onSuccess {

            }
        }
    }

    val NetCacheInterceptor = Interceptor { chain ->
        val request = chain.request()
//            val response = chain.proceed(request)
        val urlBuilder: HttpUrl.Builder = request.url().newBuilder()
        var onlineCacheTime = 3 //在线的时候的缓存过期时间，如果想要不缓存，直接时间设置为0

        val httpUrl: HttpUrl = urlBuilder.build()
        val map = HashMap<String, String>()
        // 打印所有get参数
        val paramKeys = httpUrl.queryParameterNames()
        for (key in paramKeys) {
            val value = httpUrl.queryParameter(key)
            map[key] = value!!
        }

        val response = chain.proceed(request.newBuilder().headers(request.headers()).build())

        response.newBuilder()
            .header("Cache-Control", "public, max-age=$onlineCacheTime")
            .removeHeader("Pragma")
            .build()
    }
}
