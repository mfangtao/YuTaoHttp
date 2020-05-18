package cn.scyutao.yutaohttp.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import cn.scyutao.yutaohttp.request.Http

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Http.init(this,true)
        Http.post{
            url = "http://121.37.135.255/api/store"
            params{
                "token" - "123"
                "uid" - "456"
            }
            raw = "123123"
            onSuccess {
                Log.d("222", String(it))
            }
            onFail { Log.e("222", "$it") }
        }
    }
}
