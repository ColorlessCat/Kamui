package com.colorlesscat.kamui


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.PermissionChecker
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.net.ServerSocket
import javax.inject.Inject
import kotlin.concurrent.thread


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var webserver: WebServer4Http
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = viewModels<FileListViewModel>().value
        setContent {
            Main(vm)
        }
    }

    private fun requestStorePermission(vm: FileListViewModel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), 1)
    }

    private fun checkStorePremission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
            else -> true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) viewModels<FileListViewModel>().value.getStorePermissionState().value =
            grantResults[0] == PermissionChecker.PERMISSION_GRANTED

    }

    @Composable
    fun Main(vm: FileListViewModel) {
        val lightColors = Colors(
            primary = Color(0xFF5B89B8),
            primaryVariant = Color(0xFF2267AD),
            secondary = Color(0xFFF86D9C),
            secondaryVariant = Color(0xFFDF3871),
            background = Color.White,
            surface = Color.White,
            error = Color.Yellow,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onError = Color.White,
            isLight = true
        )
        val selectedFile by vm.getSelectedFile().observeAsState()
        val hasStoerPermission by vm.getStorePermissionState().observeAsState()
        var isShowDialog by remember {
            mutableStateOf(true)
        }
        MaterialTheme(colors = lightColors) {
            //没有权限就一直对话框提示
            if (hasStoerPermission == false && isShowDialog)
                AlertDialog(onDismissRequest = { /*TODO*/ }, title = {
                    Text("权限问题")
                }, text = {
                    Column {
                        Text("并没有存储空间的读写权限，软件没法正常运行，怎么办？", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            Text(
                                "检测到系统版本在安卓11以上，点击授权以后需要手动开启权限。",
                                fontSize = 10.sp,
                                modifier = Modifier.alpha(0.8f)
                            )

                    }
                }, confirmButton = {
                    TextButton(onClick = {
                        requestStorePermission(vm)
                    }) {
                        Text("授权")
                    }
                }, dismissButton = {
                    TextButton(onClick = {
                        isShowDialog = false

                    }) {
                        Text("拒绝")
                    }
                })

            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val boxHeight = remember {
                    mutableStateOf(64.dp)
                }
                val isButtonHiden = remember {
                    mutableStateOf(false)
                }
                val isShowDownloadWindow = remember {
                    mutableStateOf(false)
                }
                Column(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(boxHeight.value)
                        .animateContentSize { _, _ ->
                            if (boxHeight.value == 500.dp) isButtonHiden.value = true
                        }
                        .border(1.dp, MaterialTheme.colors.primaryVariant)
                ) {
                    if (isButtonHiden.value) {
                        if (selectedFile != null) {
                            ProcessInfo(selectedFile!!)
                        } else {
                            Text(
                                text = "选择要上传的文件：",
                                Modifier.padding(8.dp),
                                color = MaterialTheme.colors.primaryVariant,
                                fontSize = 12.sp
                            )
                            FileList()
                        }
                    }

                    OutlinedButton(
                        onClick = { boxHeight.value = 500.dp },
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (!isButtonHiden.value) 1f else 0f)
                    ) {
                        if (boxHeight.value == 64.dp)
                            Text(text = "上传")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        boxHeight.value = 64.dp
                        isButtonHiden.value = false
                        vm.getSelectedFile().value = null
                        isShowDownloadWindow.value = true

                    },
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp)

                ) {
                    Text(text = "下载")
                    if (isShowDownloadWindow.value)
                        Popup(
                            alignment = Alignment.TopStart,
                            IntOffset(0, 0),
                            onDismissRequest = {
                                isShowDownloadWindow.value = false
                            },
                            properties = PopupProperties(
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            DownLoadInfo(isShow = isShowDownloadWindow)
                        }
                }
            }
        }
    }

    @Composable
    fun DownLoadInfo(vm: FileListViewModel = viewModel(), isShow: MutableState<Boolean>) {
        var isWaiting by remember {
            mutableStateOf(true)
        }
        val ip = remember {
            Tool.getIpAddressV4(this)
        }
        var fileSize by remember {
            mutableStateOf(-1L)
        }
        var wroteBytes by remember {
            mutableStateOf(-1L)
        }
        var fileName by remember {
            mutableStateOf("")
        }
        var ss by remember {
            mutableStateOf(ServerSocket())
        }
        if (isWaiting)
            thread {
                Thread.sleep(100)
                webserver.startServer4Download(
                    vm.downloadDir!!,
                    {
                        ss = it
                    }) { wb, tb, name ->
                    if (isWaiting) isWaiting = false
                    wroteBytes = wb
                    fileSize = tb
                    fileName = name
                }
            }
        Column(
            Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colors.primary)
                .padding(16.dp)
                .background(Color.White), horizontalAlignment = Alignment.Start
        ) {
            IconButton(
                onClick = {
                    if (!ss.isClosed)
                        ss.close()
                    isShow.value = false
                    //  isWaiting = true
                },
                Modifier
                    .align(Alignment.End)
                    .offset(8.dp, (-8).dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloseFullscreen,
                    contentDescription = "返回箭头",
                    tint = MaterialTheme.colors.primaryVariant,
                    modifier = Modifier
                        .size(24.dp)

                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isWaiting) {
                Text(
                    text = "正在等待PC端发送文件",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primaryVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TIP:打开浏览器，输入http://$ip:8848/kamui 然后点一下就可以选择文件了",
                    Modifier.alpha(0.8f),
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.primaryVariant
                )

            } else {
                Text(
                    text = "正在下载文件：$fileName \n\n\n 大小:${fileSize / 1024}KB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primaryVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val progress = wroteBytes / fileSize.toFloat()
                    val text = if (wroteBytes >= fileSize) "下载完成" else "${(progress * 100).toInt()}%"
                    LinearProgressIndicator(progress = progress)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text, color = MaterialTheme.colors.primaryVariant, fontSize = 10.sp)
                }
            }
        }
    }


    @Composable
    fun FileList(
        vm: FileListViewModel = viewModel(),
    ) {
        val data: State<List<ItemFile>?> = vm.getFiles().observeAsState()
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            items(data.value!!.size + 1) {
                val onClick = fun() {
                    val file =
                        if (it == 0) vm.currentFile.parentFile else File(data.value!![it - 1].path)
                    if (file.isFile) {
                        vm.getSelectedFile().value = file
                        return
                    }
                    val lf = vm.loadFileList(file)
                    if (lf == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "拒绝访问",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    vm.getFiles().value = lf
                    vm.currentFile = file
                }
                if (it == 0) {
                    val itf = ItemFile(vm.currentFile.parentFile, name = "..")
                    ItemFile(onClick = onClick, item = itf)
                    return@items
                }
                ItemFile(
                    onClick = onClick,
                    item = data.value!![it - 1]
                )
            }
        }
    }


    @Composable
    fun ProcessInfo(
        file: File, vm: FileListViewModel = viewModel(),
    ) {
        val strSourceSate = "已选择文件: ${file.name}\n" +
                "大小: ${if (file.length() < 1024 * 1024) 1 else file.length() / 1024}KB\n" +
                "正在启动服务......成功\n" +
                "等待下载中......\n"

        val strHint = remember {
            "Hint: 在电脑上打开浏览器，并在地址栏输入${Tool.getIpAddressV4(this)}:8848/kamui\n" +
                    "完成以后按回车就可以下载的刚刚选择的文件。"
        }
        var strState by remember {
            mutableStateOf("")
        }
        var process by remember {
            mutableStateOf(0f)
        }
        var readBytes by remember {
            mutableStateOf(0L)
        }
        var totalBytes by remember {
            mutableStateOf(0L)
        }
        val downText =
            if (process < 1f) "开始上传,进度:${readBytes / 1024}KB / ${totalBytes / 1024}KB" else "上传完成。"
        var ss by remember {
            mutableStateOf(ServerSocket())
        }
        if (strState == "")
            thread {
                //稍微睡一下 等视图渲染
                Thread.sleep(100)
                for (i in strSourceSate.indices) {
                    vm.getSelectedFile().value ?: return@thread
                    if (strState.endsWith("_")) strState = strState.dropLast(1)
                    strState += strSourceSate[i] + "_"
                    Thread.sleep(40)
                }
                strState = strState.dropLast(1)
                webserver.startServer4Upload(file, {
                    ss = it
                }) { rb, tb ->
                    process = rb.toFloat() / tb
                    readBytes = rb
                    totalBytes = tb
                }


            }
        Column(
            Modifier
                .fillMaxSize()
        ) {
            Text(
                text = strState,
                Modifier.padding(16.dp),
                fontSize = 12.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colors.primaryVariant
            )
            if (process > 0f)
                Text(
                    text = downText,
                    Modifier.padding(16.dp, 0.dp),
                    fontSize = 12.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colors.primaryVariant
                )
            if (process == 0f && strState == strSourceSate) {
                Text(
                    text = strHint,
                    Modifier.padding(16.dp, 16.dp),
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colors.primaryVariant
                )
            } else if (process > 0f) {
                Row(
                    Modifier
                        .fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = process,
                        Modifier
                            .padding(16.dp, 16.dp)
                            .align(Alignment.CenterVertically)
                    )
                    Text(
                        text = "${(process * 100).toInt()}%",
                        fontSize = 10.sp,
                        modifier = Modifier
                            .padding(0.dp, 16.dp)
                            .align(Alignment.CenterVertically)
                    )
                }

            }
            if (strState == strSourceSate)
                TextButton(
                    onClick = {
                        vm.getSelectedFile().value = null
                        ss.close()
                    },
                    Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("返回", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
        }
    }

    @Composable
    fun ItemFile(onClick: () -> Unit, item: ItemFile) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(MaterialTheme.colors.background)
                .clickable { onClick() }
        ) {
            Icon(
                imageVector = if (item.isFile) Icons.Rounded.Android else Icons.Rounded.Folder,
                contentDescription = "文件图标",
                Modifier.padding(8.dp),
                tint = MaterialTheme.colors.primaryVariant
            )

            Text(
                text = item.name,
                fontSize = 12.sp,
                color = if (item.isFile) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant,
                textAlign = TextAlign.Justify,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(false) {}
                    .padding(8.dp, 0.dp)
                    .background(MaterialTheme.colors.background)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModels<FileListViewModel>().value.getStorePermissionState().value =
            checkStorePremission()
    }

    override fun onStart() {
        super.onStart()
        if (checkStorePremission())
            if (!viewModels<FileListViewModel>().value.downloadDir!!.let {
                    it.exists() || it.mkdir()
                })
                Toast.makeText(this, "创建文件夹失败，可能没有存储权限。", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        //这里是为了防止用户点击返回或者房子键，如果在刚好在服务启动之前返回了 会导致端口占用然后闪退
        //但是 加上这个就会导致切回来以后总是在文件列表页面。需要更多的判断解决这个问题，暂时放着( •̀ ω •́ )✧
        // viewModels<FileListViewModel>().value.getSelectedFile().value = null
    }
}