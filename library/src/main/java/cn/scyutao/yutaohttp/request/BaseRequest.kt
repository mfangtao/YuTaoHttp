package cn.scyutao.yutaohttp.request

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser

open class ByteRequest(method: Int, url: String, errorListener: Response.ErrorListener? = Response.ErrorListener {})
: BaseRequest<ByteArray>(method, url, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<ByteArray>? {
        return Response.success(response?.data, HttpHeaderParser.parseCacheHeaders(response))
    }
}

abstract class BaseRequest<D>(method: Int, url: String, errorListener: Response.ErrorListener? = Response.ErrorListener {})
: Request<D>(method, url, errorListener) {
    protected val DEFAULT_CHARSET = "UTF-8"

    internal var _listener: Response.Listener<D>? = null
    protected val _heads: MutableMap<String, String> = mutableMapOf()
    protected val _params: MutableMap<String, String> = mutableMapOf() // used for a POST or PUT request.

    public override fun getParams(): MutableMap<String, String> {
        return _params
    }

    override fun getHeaders(): MutableMap<String, String> {
        return _heads
    }

    fun setHeaders(heads: MutableMap<String, String>){
        _heads.putAll(heads)
    }

    fun setParams(params: MutableMap<String, String>){
        _params.putAll(params)
    }

    override fun deliverResponse(response: D?) {
        _listener?.onResponse(response)
    }
}

