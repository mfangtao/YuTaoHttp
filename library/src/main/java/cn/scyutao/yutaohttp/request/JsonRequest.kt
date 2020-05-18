package cn.scyutao.yutaohttp.request

import com.android.volley.Response
import com.android.volley.VolleyLog
import java.io.UnsupportedEncodingException

class JsonRequest(method: Int, url: String,val requestBody: String, errorListener: Response.ErrorListener?)
    : ByteRequest(method, url, errorListener) {

    private val PROTOCOL_CHARSET = "utf-8"

    private val PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET)

    @Deprecated("", ReplaceWith("getBody()"))
    override fun getPostBody(): ByteArray? {
        return getBody()
    }

    override fun getBodyContentType(): String {
        return PROTOCOL_CONTENT_TYPE
    }

    override fun getBody(): ByteArray? {
        try {
            return requestBody.toByteArray(charset(PROTOCOL_CHARSET))
        } catch (uee: UnsupportedEncodingException) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                    requestBody, PROTOCOL_CHARSET)
            return null
        }

    }
}