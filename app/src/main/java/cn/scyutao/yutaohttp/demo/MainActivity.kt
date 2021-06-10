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
        Http.init(this)
        Http.debug = true
        Http.baseUrl = "https://jkmb.scyutao.com/"

        val map = HashMap<String,String>()
        map["token"] = "123"
        map["sss"] = "456"
        get("/wxminiapp/user/login",map){
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
