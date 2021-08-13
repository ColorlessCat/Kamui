package com.colorlesscat.kamui

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Author:ColorlessCat
 * Date:2021/07/04 02:45
 * Describe: 遇事不决工具类 ......有些东西放到Acitivity和ViewMoel都不太合适呢
 */
class Tool {
    companion object {

        fun getIpAddressV4(context: Context):String{
            val wm =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wm.connectionInfo.ipAddress.let {
                //两个误区：1.0xff对齐的是最低的八位(开始觉得是高八位用的左移) 2.ip地址的字符串和int的顺序相反 最低8位的数字在最前面(开始觉得顺序一样)
                "${it and 0xff}.${it shr 8 and 0xff}.${it shr 16 and 0xff}.${it shr 24 and 0xff}"
            }
        }

    }
}