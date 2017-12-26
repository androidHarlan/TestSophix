package com.harlan.lhc.testsophix;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.listener.PatchLoadStatusListener;

/**
 * Created by a1 on 2017/12/26.
 */
public class Myapplication extends Application{
    public static String appId;
    @Override
    public void onCreate() {
        super.onCreate();
        initHotfix();
    }
    private void initHotfix() {
        this.appId = "24743914-1";
        String appVersion;
        try {
            appVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (Exception e) {
            appVersion = "1.0";
        }

        SophixManager.getInstance().setContext(this)
                .setAppVersion(appVersion)
                .setSecretMetaData("24743914-1","c5d2685b0caaa9b93fd45fac7d33c7bd","MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCJxARGGVfViuGtUKGgBfBT8XqwDPdHVIJJnDp/eBSX3KU9PFEZc1Z+TQT079DWGEfttRsFb78IgVeIdLx7xNcXAVefeAhU86jbhlx1PqBRDEhMRPIF8ZQS+2zR/aj6NKOg+oNJmdMJAPfRCjSvGOlUR9QwVQEk/HK6/NWCl+KxPrFLEjkxtf0vIW2xs1cRJVwQhVBXbevbfr8Es7JVy4powJchyLeMIhLYqcOJ2e6ISfXGNzHSEAGl5DwUdOye1F9zvX+rYP9kh32LQXp8wY37FXCLMXY5l6yPqbsyQ5RFOhKgWstH8OQ/uLVKQoML6Us8mFXvupWQSzxPvkBrv7T1AgMBAAECggEADybn8W3II0xrFczQefEtRY5BmCtU5xt8WOw94tcqLKmv4tRHdy0gTjPaX7YgmeuBvbs1hZuMvzq6jd+I+3Pi1Dzpjjh9chp6b+qPMLX6m+9l5D2RAOZZCt1sUow7kYlIleP3nJi2hT7+ApfEw51RzFs6EzbGMXMyiy5TrVFXNXcs6030pSgzJkZOJ7agYgr1Sr+2T35zwlTynx0e+v2eWi6ew8mn6X2I7m5ZhOaRO78+wAX0hn81R9puCc7R8RLzzZ4/MPFPuk4LDF1LRjmMhO1kZHfXcr0YlAO2kIez/xT5hbnYQSChfTozd8RWl9FDTarZhL/Znm5WdfZOzo7o4QKBgQD1hIGY9e64uBR4na922nZ8i5xjJOIDSiVsEIY/PvsqMCd04N4Srnp5w/nOADoORi9ovQVUttgYkCZD7ympO46crjNFVcDOfOh/RVxd3SWou542/9Yu3m6o2hopDo9ngqsE32XiwaIaiVRx+9VnjVh152zEyaPYiXEO4c45qVwZXwKBgQCPpcm80ekRYETF+7HCWzeQC2i/MmCa+QfjJaAFaVUEK6LCs2fQOc2V0IWgLvv//5nAfTx/wQRZWV//YezMoC+1rnPlH+hV/0htkOSObyLCWqyYF9mTsfE1G7Hb3LuvgJs/8tf/t7b4YroLjBbKXr+ajlh8/Wjo1/j5R/qnZ0POKwKBgHZrNP8MD9p0nxsWI12WPXQQ+psvXPvtNWOMDNRQkmBk3YMYOsST92rnxYNAxL/BxngkQ3/6uPwP+wTlRBjmKwETXJNiqx5tTM6mK8jyM4nRBJzOhQYSLxmuKNQIu85XThmJjuDyODfIaTyZFPNfT31+5A1+nFKC6E8fRpK1R0DNAoGBAIZ8U+RIrikLdTfajjEFT363f8jJdHKCxiSefDO0ytiNteMNLhtfkp85S3GNq3agKaVyWqrM3bJ9H4gseROWwTQacPzJo66nw3p41dKYL/XACKiYY6aISOhM1naeMMV95Vu4kwshR9dCLyZScJ/klwQvUp8qbQbZ9IoGHqCN6IhjAoGBANX2dKwvAGj2H0Ljw/JU6/uxBBK0nK7qSBLURchLNRkggpAzlVKeqE7cFFFV0Qv8WrO1ay1STtnnhKME3MGymfhUV5QYyHvDvoI277m6RPBuduGBF+REHEohlkkE2WVovlp50mCpvXLaTi2defaYsBocgjfdfbPrTv4ezQ2zJftq")
                .setAesKey(null)

                        //.setAesKey("0123456789123456")
                .setEnableDebug(true)
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        // 补丁加载回调通知
                        Log.e("backinfo","code:"+code);
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            // 表明补丁加载成功
                            Toast.makeText(getApplicationContext(),"表明补丁加载成功",Toast.LENGTH_LONG).show();
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 表明新补丁生效需要重启. 开发者可提示用户或者强制重启;
                            // 建议: 用户可以监听进入后台事件, 然后应用自杀
                            restartApp();
                        } else if (code == PatchStatus.CODE_LOAD_FAIL) {
                            // 内部引擎异常, 推荐此时清空本地补丁, 防止失败补丁重复加载
                            SophixManager.getInstance().cleanPatches();
                        } else {

                            // 其它错误信息, 查看PatchStatus类说明
                        }
                    }
                }).initialize();
        SophixManager.getInstance().queryAndLoadNewPatch();
    }
    public void restartApp(){
        final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
