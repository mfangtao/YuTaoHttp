package cn.scyutao.yutaohttp.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import cn.scyutao.yutaohttp.partial.partially1
import cn.scyutao.yutaohttp.request.Http
import cn.scyutao.yutaohttp.request.RequestPairs
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Http.init(this,true)
//        Http.post{
//            url = "http://121.37.135.255/api/store"
//            params{
//                "token" - "123"
//                "uid" - "456"
//            }
//            raw = "123123"
//            onSuccess {
//                Log.d("222", it)
//            }
//            onFail { Log.e("222", "$it") }
//        }
        var map = HashMap<String,String>()
        map["token"] = "123"
        map["sss"] = "456"
        get("http://121.37.135.255/api/store",map){
            Log.e("222", "$it")
        }
    }
    fun get(urlStr:String, paramss: HashMap<String,String>, success: (String) -> Unit){
        Http.get{
            url = urlStr
            _params = paramss
            headers{
                "token" - "22"
                "source" - "3"
            }
            onSuccess {

            }
        }
    }
}
