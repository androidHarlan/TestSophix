# TestSophix
阿里云热修复
# 1.2 集成准备
gradle远程仓库依赖, 打开项目找到app的build.gradle文件，添加如下配置：<br>
添加maven仓库地址：
~~~~
repositories {
   maven {
       url "http://maven.aliyun.com/nexus/content/repositories/releases"
   }
}
~~~~
添加gradle坐标版本依赖：
~~~~
compile 'com.aliyun.ams:alicloud-android-hotfix:3.1.9'
~~~~
# 1.2.3 权限说明
Sophix SDK使用到以下权限
~~~~
<! -- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<! -- 外部存储读权限，调试工具加载本地补丁需要 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
~~~~
# 1.2.4 配置AndroidManifest文件
在AndroidManifest.xml中间的application节点下添加如下配置：
~~~~
<meta-data
android:name="com.taobao.android.hotfix.IDSECRET"
android:value="App ID" />
<meta-data
android:name="com.taobao.android.hotfix.APPSECRET"
android:value="App Secret" />
<meta-data
android:name="com.taobao.android.hotfix.RSASECRET"
android:value="RSA密钥" />
~~~~
将上述value中的值分别改为通过平台HotFix服务申请得到的App Secret和RSA密钥，出于安全考虑，建议使用setSecretMetaData这个方法进行设置，详见1.3.2.1的方法说明。
# 1.2.5 混淆配置
~~~~
#基线包使用，生成mapping.txt
-printmapping mapping.txt
#生成的mapping.txt在app/buidl/outputs/mapping/release路径下，移动到/app路径下
#修复后的项目使用，保证混淆结果一致
#-applymapping mapping.txt
#hotfix
-keep class com.taobao.sophix.**{*;}
-keep class com.ta.utdid2.device.**{*;}
#防止inline
-dontoptimize
~~~~
# 1.3 SDK接口使用说明
initialize的调用应该尽可能的早，必须在Application.attachBaseContext()或者Application.onCreate()的最开始进行SDK初始化操作，初始化之前不能用到其他自定义类，否则极有可能导致崩溃。而查询服务器是否有可用补丁的操作可以在后面的任意地方。
~~~~
// initialize最好放在attachBaseContext最前面，初始化直接在Application类里面，切勿封装到其他类
SophixManager.getInstance().setContext(this)
                .setAppVersion(appVersion)
                .setAesKey(null)
                .setEnableDebug(true)
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        // 补丁加载回调通知
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            // 表明补丁加载成功
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 表明新补丁生效需要重启. 开发者可提示用户或者强制重启;
                            // 建议: 用户可以监听进入后台事件, 然后调用killProcessSafely自杀，以此加快应用补丁，详见1.3.2.3
                        } else {
                            // 其它错误信息, 查看PatchStatus类说明
                        }
                    }
                }).initialize();
// queryAndLoadNewPatch不可放在attachBaseContext 中，否则无网络权限，建议放在后面任意时刻，如onCreate中
SophixManager.getInstance().queryAndLoadNewPatch();
~~~~
# 1.3.2 接口说明
1.3.2.1 initialize方法
initialize(): <必选>

该方法主要做些必要的初始化工作以及如果本地有补丁的话会加载补丁, 但不会自动请求补丁。因此需要自行调用queryAndLoadNewPatch方法拉取补丁。这个方法调用需要尽可能的早, 推荐在Application的onCreate方法中调用, initialize()方法调用之前你需要先调用如下几个方法, 方法调用说明如下:

setContext(application): <必选> 传入入口Application即可

setAppVersion(appVersion): <必选> 应用的版本号

setSecretMetaData(idSecret, appSecret, rsaSecret): <可选，推荐使用> 三个Secret分别对应AndroidManifest里面的三个，可以不在AndroidManifest设置而是用此函数来设置Secret。放到代码里面进行设置可以自定义混淆代码，更加安全，此函数的设置会覆盖AndroidManifest里面的设置，如果对应的值设为null，默认会在使用AndroidManifest里面的。

setEnableDebug(isEnabled): <可选> isEnabled默认为false, 是否调试模式, 调试模式下会输出日志以及不进行补丁签名校验. 线下调试此参数可以设置为true, 查看日志过滤TAG:Sophix, 同时强制不对补丁进行签名校验, 所有就算补丁未签名或者签名失败也发现可以加载成功. 但是正式发布该参数必须为false, false会对补丁做签名校验, 否则就可能存在安全漏洞风险

setAesKey(aesKey): <可选> 用户自定义aes秘钥, 会对补丁包采用对称加密。这个参数值必须是16位数字或字母的组合，是和补丁工具设置里面AES Key保持完全一致, 补丁才能正确被解密进而加载。此时平台无感知这个秘钥, 所以不用担心阿里云移动平台会利用你们的补丁做一些非法的事情。

setPatchLoadStatusStub(new PatchLoadStatusListener()): <可选> 设置patch加载状态监听器, 该方法参数需要实现PatchLoadStatusListener接口, 接口说明见1.3.2.2说明

setUnsupportedModel(modelName, sdkVersionInt):<可选> 把不支持的设备加入黑名单，加入后不会进行热修复。modelName为该机型上Build.MODEL的值，这个值也可以通过adb shell getprop | grep ro.product.model取得。sdkVersionInt就是该机型的Android版本，也就是Build.VERSION.SDK_INT，若设为0，则对应该机型所有安卓版本。

1.3.2.2 queryAndLoadNewPatch方法
该方法主要用于查询服务器是否有新的可用补丁. SDK内部限制连续两次queryAndLoadNewPatch()方法调用不能短于3s, 否则的话就会报code:19的错误码. 如果查询到可用的话, 首先下载补丁到本地, 然后

应用原本没有补丁, 那么如果当前应用的补丁是热补丁, 那么会立刻加载(不管是冷补丁还是热补丁). 如果当前应用的补丁是冷补丁, 那么需要重启生效.
应用已经存在一个补丁, 请求发现有新补丁后，本次不受影响。并且在下次启动时补丁文件删除, 下载并预加载新补丁。在下下次启动时应用新补丁。

补丁在后台发布之后, 并不会主动下行推送到客户端, 需要手动调用queryAndLoadNewPatch方法查询后台补丁是否可用.

只会下载补丁版本号比当前应用存在的补丁版本号高的补丁, 比如当前应用已经下载了补丁版本号为5的补丁, 那么只有后台发布的补丁版本号>5才会重新下载.
同时1.4.0以上版本服务后台上线了“一键清除”补丁的功能, 所以如果后台点击了“一键清除”那么这个方法将会返回code:18的状态码. 此时本地补丁将会被强制清除, 同时不清除本地补丁版本号

1.3.2.3 killProcessSafely方法
可以在PatchLoadStatusListener监听到CODE_LOAD_RELAUNCH后在合适的时机，调用此方法杀死进程。注意，不可以直接Process.killProcess(Process.myPid())来杀进程，这样会扰乱Sophix的内部状态。因此如果需要杀死进程，建议使用这个方法，它在内部做一些适当处理后才杀死本进程。

1.3.2.4 cleanPatches()方法
清空本地补丁，并且不再拉取被清空的版本的补丁。正常情况下不需要开发者自己调用，因为Sophix内部会判断对补丁引发崩溃的情况进行自动清空。

1.3.2.5 PatchLoadStatusListener接口
该接口需要自行实现并传入initialize方法中, 补丁加载状态会回调给该接口, 参数说明如下:

mode: 无实际意义, 为了兼容老版本, 默认始终为0
code: 补丁加载状态码, 详情查看PatchStatus类说明
info: 补丁加载详细说明
handlePatchVersion: 当前处理的补丁版本号, 0:无 -1:本地补丁 其它:后台补丁
### 常见状态码说明如下: 一个补丁的加载一般分为三个阶段: 查询/预加载/加载
~~~~
//兼容老版本的code说明
    int CODE_LOAD_SUCCESS = 1;//加载阶段, 成功
    int CODE_ERR_INBLACKLIST = 4;//加载阶段, 失败设备不支持
    int CODE_REQ_NOUPDATE = 6;//查询阶段, 没有发布新补丁
    int CODE_REQ_NOTNEWEST = 7;//查询阶段, 补丁不是最新的 
    int CODE_DOWNLOAD_SUCCESS = 9;//查询阶段, 补丁下载成功
    int CODE_DOWNLOAD_BROKEN = 10;//查询阶段, 补丁文件损坏下载失败
    int CODE_UNZIP_FAIL = 11;//查询阶段, 补丁解密失败
    int CODE_LOAD_RELAUNCH = 12;//预加载阶段, 需要重启
    int CODE_REQ_APPIDERR = 15;//查询阶段, appid异常
    int CODE_REQ_SIGNERR = 16;//查询阶段, 签名异常
    int CODE_REQ_UNAVAIABLE = 17;//查询阶段, 系统无效
    int CODE_REQ_SYSTEMERR = 22;//查询阶段, 系统异常
    int CODE_REQ_CLEARPATCH = 18;//查询阶段, 一键清除补丁
    int CODE_PATCH_INVAILD = 20;//加载阶段, 补丁格式非法
    //查询阶段的code说明
    int CODE_QUERY_UNDEFINED = 31;//未定义异常
    int CODE_QUERY_CONNECT = 32;//连接异常
    int CODE_QUERY_STREAM = 33;//流异常
    int CODE_QUERY_EMPTY = 34;//请求空异常
    int CODE_QUERY_BROKEN = 35;//请求完整性校验失败异常
    int CODE_QUERY_PARSE = 36;//请求解析异常
    int CODE_QUERY_LACK = 37;//请求缺少必要参数异常
    //预加载阶段的code说明
    int CODE_PRELOAD_SUCCESS = 100;//预加载成功
    int CODE_PRELOAD_UNDEFINED = 101;//未定义异常
    int CODE_PRELOAD_HANDLE_DEX = 102;//dex加载异常
    int CODE_PRELOAD_NOT_ZIP_FORMAT = 103;//基线dex非zip格式异常
    int CODE_PRELOAD_EXTRACT = 104;
    int CODE_PRELOAD_REMOVE_BASEDEX = 105;//基线dex处理异常
    int CODE_PRELOAD_MARKKEPT = 106;
    int CODE_PRELOAD_OPT_MERGED = 107; 
    //加载阶段的code说明 分三部分dex加载, resource加载, lib加载
    //dex加载
    int CODE_LOAD_UNDEFINED = 71;//未定义异常
    int CODE_LOAD_AES_DECRYPT = 72;//aes对称解密异常
    int CODE_LOAD_MFITEM = 73;//补丁SOPHIX.MF文件解析异常
    int CODE_LOAD_COPY_FILE = 74;//补丁拷贝异常
    int CODE_LOAD_SIGNATURE = 75;//补丁签名校验异常
    int CODE_LOAD_SOPHIX_VERSION = 76;//补丁和补丁工具版本不一致异常
    int CODE_LOAD_NOT_ZIP_FORMAT = 77;//补丁zip解析异常
    int CODE_LOAD_JIT_CLEAR = 78;
    int CODE_LOAD_FIND_DEX = 79;
    int CODE_LOAD_DELETE_OPT = 80;//删除无效odex文件异常
    int CODE_LOAD_HANDLE_DEX = 81;
    int CODE_LOAD_FIND_CLASS = 82;
    int CODE_LOAD_FIND_CONSTRUCTOR = 83;
    int CODE_LOAD_FIND_METHOD = 84;
    int CODE_LOAD_FIND_FIELD = 85;
    int CODE_LOAD_ILLEGAL_ACCESS = 86;
    //resource加载
    //lib加载
    int CODE_LOAD_LIB_UNDEFINED = 131;//未定义异常
    int CODE_LOAD_LIB_CPUABIS = 132;//获取primaryCpuAbis异常
    int CODE_LOAD_LIB_JSON = 133;//json格式异常
    int CODE_LOAD_LIB_LOST = 134;//lib库不完整异常
    int CODE_LOAD_LIB_UNZIP = 135;//解压异常
    int CODE_LOAD_LIB_INJECT = 136;//注入异常
    int CODE_LOAD_LIB_NS = 137;
~~~~
# 四，生成补丁
2.1 下载打包工具
patch补丁包生成需要使用到打补丁工具SophixPatchTool, 如还未下载打包工具，请前往下载Android打包工具。

Mac版本打包工具地址：http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_macos.zip

Windows版本打包工具地址：http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_windows.zip

Linux版本打包工具地址：http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_linux.zip

调试工具地址：http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/hotfix_debug_tool-release.apk

该工具提供了Windows和macOS和Linux版本，Windows下运行SophixPatchTool.exe，macOS下运行SophixPatchTool.app，Linux下（Ubuntu 16.04 64bit最佳）运行SophixPatchTool。并且需要安装Java环境且在JDK7或以上才能正常使用。

# 参考http://blog.csdn.net/xiongtao63/article/details/77847822?locationNum=3&fps=1
