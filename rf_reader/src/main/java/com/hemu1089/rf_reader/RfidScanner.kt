package com.hemu1089.rf_reader

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.SoundPool
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.hemu1089.rf_reader.Helper.isEmpty
import com.hemu1089.rf_reader.Helper.isNotEmpty
import com.rscja.deviceapi.RFIDWithUHFUART
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RfidScanner {
    private lateinit var mContext: Context
    private var mReader: RFIDWithUHFUART? = null
    private var soundMap: HashMap<Int, Int> = HashMap()
    private var soundPool: SoundPool? = null
    private var volumnRatio = 0f
    private var am: AudioManager? = null
    private var epcTidUser: ArrayList<String> = ArrayList()
    private var map: java.util.HashMap<String, String> = HashMap()
    private var tagList: java.util.ArrayList<java.util.HashMap<String, String>> =
        java.util.ArrayList()
    private var tempDatas: MutableList<String> = java.util.ArrayList()

    companion object {
        const val TAG_EPC: String = "tagEPC"
        const val TAG_EPC_TID: String = "tagEpcTID"
        const val TAG_COUNT: String = "tagCount"
        const val TAG_RSSI: String = "tagRssi"
    }

    fun initialize(cxt: Context) {
        mContext = cxt
        initSound()
        initSdk()
    }

    private fun initSdk() {
        try {
            mReader = RFIDWithUHFUART.getInstance()

        } catch (ex: Exception) {
            Toast.makeText(mContext, ex.message, Toast.LENGTH_SHORT).show()
            return
        }

        if (mReader != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    mReader?.init(mContext)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(mContext, "RFID Scanner Initialized", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (ex: Exception) {
                    Toast.makeText(mContext, ex.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    fun start(): MutableList<HashMap<String, String>> {
        map.clear()
        tempDatas.clear()
        tagList.clear()
        mReader?.let { reader ->
            reader.setFilter(RFIDWithUHFUART.Bank_EPC, 0, 0, "")
            val uhfTagInfo = reader.inventorySingleTag()
            if (uhfTagInfo != null) {
                val tid = uhfTagInfo.tid
                val epc = uhfTagInfo.epc
                val user = uhfTagInfo.user
                playSound(1)
                val valueMap = addDataToList(epc, mergeTidEpc(tid, epc, user), uhfTagInfo.rssi)
                return valueMap
            } else {
                Toast.makeText(mContext, "inventory error", Toast.LENGTH_SHORT).show()
                return mutableListOf()
            }
        }.run {
            Toast.makeText(mContext, "RFID Scanner Not Initialized", Toast.LENGTH_SHORT).show()
            return mutableListOf()
        }
    }

    private fun addDataToList(
        epc: String,
        epcAndTidUser: String,
        rssi: String
    ): MutableList<HashMap<String, String>> {
        if (isNotEmpty(epc)) {
            val index: Int = checkIsExist(epc)
            map = java.util.HashMap<String, String>()
            map[TAG_EPC] = epc
            map[TAG_EPC_TID] = epcAndTidUser
            map[TAG_COUNT] = 1.toString()
            map[TAG_RSSI] = rssi
            if (index == -1) {
                tagList.add(map)
                tempDatas.add(epc)
            } else {
                val tagCount: Int = (tagList[index][TAG_COUNT]?.toInt(10) ?: 0) + 1
                map[TAG_COUNT] = tagCount.toString()
                map[TAG_EPC_TID] = epcAndTidUser
                tagList[index] = map
            }
        }
        return tagList
    }

    private fun checkIsExist(epc: String): Int {
        if (isEmpty(epc)) {
            return -1
        }
        for (k in tempDatas.indices) {
            if (epc == tempDatas[k]) {
                return k
            }
        }
        return -1
    }

    private fun mergeTidEpc(tid: String, epc: String, user: String?): String {
        epcTidUser.add(epc)
        var data = "EPC:$epc"
        if (!TextUtils.isEmpty(tid) && tid != "0000000000000000" && tid != "000000000000000000000000") {
            epcTidUser.add(tid)
            data += "\nTID:$tid"
        }
        if (!user.isNullOrEmpty()) {
            epcTidUser.add(user)
            data += "\nUSER:$user"
        }
        return data
    }

    private fun initSound() {
        soundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 5)
        soundMap[1] = soundPool!!.load(mContext, R.raw.barcodebeep, 1)
        soundMap[2] = soundPool!!.load(mContext, R.raw.serror, 1)
        am = mContext.getSystemService(AUDIO_SERVICE) as AudioManager // 实例化AudioManager对象
    }

    private fun releaseSoundPool() {
        if (soundPool != null) {
            soundPool!!.release()
            soundPool = null
        }
    }

    fun playSound(id: Int) {
        val audioMaxVolume =
            am!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() // 返回当前AudioManager对象的最大音量值
        val audioCurrentVolume =
            am!!.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() // 返回当前AudioManager对象的音量值
        volumnRatio = audioCurrentVolume / audioMaxVolume
        try {
            soundPool!!.play(
                soundMap[id]!!, volumnRatio,  // 左声道音量
                volumnRatio,  // 右声道音量
                1,  // 优先级，0为最低
                0,  // 循环次数，0不循环，-1永远循环
                1f // 回放速度 ，该值在0.5-2.0之间，1为正常速度
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

