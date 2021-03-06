package me.singleneuron.hook

import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.singleneuron.base.BaseDelayableConditionalHookAdapter
import me.singleneuron.util.QQVersion
import nil.nadph.qnotified.util.LicenseStatus
import nil.nadph.qnotified.util.Utils

object ForceSystemCamera : BaseDelayableConditionalHookAdapter("forceSystemCamera") {
    override fun doInit(): Boolean {
        //特征字符串："CaptureUtil"
        val captureUtilClass = Class.forName(getClass())
        //特征字符串："GT-I9500"
        XposedHelpers.findAndHookMethod(captureUtilClass,"a",object : XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                if (LicenseStatus.sDisableCommonHooks) return
                if (!isEnabled) return
                Utils.logd("ForceSystemCamera babq.a():"+(param!!.result as Boolean))
                param.result = false
            }
        })
        return true
    }

    override val condition: () -> Boolean
        get() = {Utils.getHostVersionCode()== QQVersion.QQ_8_3_9 || Utils.getHostVersionCode()==QQVersion.QQ_8_4_1}

    override fun getClass(): String {
        return when(Utils.getHostVersionCode()) {
            QQVersion.QQ_8_3_9 -> "babg"
            QQVersion.QQ_8_4_1 -> "bann"
            else -> super.getClass()
        }
    }
}