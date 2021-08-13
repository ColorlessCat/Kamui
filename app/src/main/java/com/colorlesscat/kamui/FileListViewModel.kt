package com.colorlesscat.kamui

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.core.os.EnvironmentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File

const val sdCardFolder = "/sdcard/"

class FileListViewModel(application: Application) : AndroidViewModel(application) {
    private val fileList by lazy {
        MutableLiveData<List<ItemFile>>().also {
            firstLoad(it)
        }
    }
    private val selectedFile = MutableLiveData<File>()
    var rootFolder: String = Environment.getExternalStorageDirectory().let {
        if (it != null) it.absolutePath else sdCardFolder
    }
    var currentFile: File = File(rootFolder)


    var downloadDir: File? =
        //application.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        // 想了想还是直接在SD卡创建目录吧 上面的文件夹自带的文件管理都打不开......
        File(Environment.getExternalStorageDirectory(), "神威")
    private val hasStorePermission = MutableLiveData(false)
    fun getStorePermissionState() = hasStorePermission
    private fun firstLoad(it: MutableLiveData<List<ItemFile>>) {
        it.value = loadFileList(File(rootFolder))
    }

    fun getFiles() = fileList
    fun getSelectedFile() = selectedFile
    fun loadFileList(file: File): List<ItemFile>? {
        val list = emptyList<ItemFile>().toMutableList()
        val lf = file.listFiles()
            ?: return null
        lf.forEach {
            list.add(ItemFile(it))
        }
        return list
    }
}