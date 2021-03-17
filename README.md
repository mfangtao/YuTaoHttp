# YuTaoHttp 使用方法

## 一、初始化
```
Http.init(this,true)
```
## 二、设置调试模式
```
Http.debug = true
```
## 三、设置BaseUrl
```
Http.baseUrl = "https://xxx.com/"   //如果正式请求的时候，检测到是完成到地址，baseUrl将无效
```
## 四、设置headers
```
Http.headers = HashMap<String,String>()  //单个请求可单独设置headers
```
## 五、请求设置
```
Http.connectTimeout
Http.readTimeout
Http.writeTimeout
Http.pingInterval
```