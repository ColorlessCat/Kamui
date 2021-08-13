package com.colorlesscat.kamui

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import kotlin.concurrent.thread

/**
 * Author:ColorlessCat
 * Date:2021/07/02 07:46
 * Describe:用Socket简单实现HTTP协议
 */


class WebServer4Http @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    //TODO 放到单独的文件里
    val fileUploadByJS = "<html onclick='\n" +
            "var input = document.createElement(\"input\");\n" +
            "input.type = \"file\";\n" +
            "input.click();\n" +
            "input.onchange=function(){\n" +
            "console.log(233);\n" +
            "var file=input.files[0]\n" +
            "var form = new FormData();\n" +
            "form.append(file.name, file,file.size); \n" +
            "var xhr = new XMLHttpRequest();\n" +
            "var host=window.location.hostname\n" +
            "var action = \"http://\"+host+\":7777/upload\"; \n" +
            "xhr.open(\"POST\", action);\n" +
            "xhr.send(form);\n" +
            "}\n" +
            "'/>\n" +
            "\n"
    private val responseHeader4Upload = { length: Long, name: String ->
        "HTTP/1.1 200 OK\r\n" +
                "Server:Kamui = =\r\n" +
                "Content-type:application/octet-stream\r\n" +
                "Content-Length:$length\r\n" +
                "Content-Disposition:attachment; filename=$name\r\n\r\n"
    }
    private val responseData4Download by lazy {
        ("HTTP/1.1 200 OK\r\n" +
                "Server:Kamui = =\r\n" +
                "Content-type:text/html\r\n" +
                "Content-Length:El psy conguroo\r\n\r\n" +
                fileUploadByJS + "\r\n").let {
            it.replace("El psy conguroo", fileUploadByJS.toByteArray().size.toString())
        }
    }

    //写完才发现整个过程有些不可控threadController为了方便关闭ServerSocket 以免占用端口
    fun startServer4Upload(
        file: File,
        ssController: (ss: ServerSocket) -> Unit,
        callback: (wroteBytes: Long, totalBytes: Long) -> Unit
    ) {
        thread {
            val socket = waitingResponse(ssController)
            socket ?: return@thread
            val bw = socket.getOutputStream().bufferedWriter()
            bw.write(responseHeader4Upload(file.length(), file.name))
            bw.flush()
            val input = file.inputStream()
            val bytes = ByteArray(4 * 1024)
            val output = socket.getOutputStream()
            var length = -1
            var count = 0L
            while (true) {
                length = input.read(bytes)
                if (length == -1) break
                output.write(bytes, 0, length)
                output.flush()
                count += length
                callback(count, file.length())
            }
            //收工 ヾ(≧▽≦*)o
            socket.close()
            input.close()
            output.close()

        }
    }

    fun startServer4Download(
        dir: File,
        ssController: (th: ServerSocket) -> Unit,
        callback: (readBytes: Long, totalBytes: Long, fileName: String) -> Unit
    ) {
        thread {
            val socket = waitingResponse(ssController)
            socket ?: return@thread
            val bw = socket.getOutputStream().bufferedWriter()
            bw.write(responseData4Download)
            bw.flush()
            bw.close()
            socket.close()
            val downloadSocket = ServerSocket(7777).accept()
            val input = downloadSocket.getInputStream()
            //  val br = input.bufferedReader()
            var line: String
            var endFlag = ""
            var fileSize = 0L
            var fileName = ""
            while (true) {
                line = readLine4Heanders(input)
                if (line.startsWith("--")) {
                    //三次  目的是把位置移到文件内容
                    endFlag = "$line--"
                    line = readLine4Heanders(input)
                    readLine4Heanders(input)
                    readLine4Heanders(input)
                    fileName = line.substring(line.indexOf("=") + 2, line.lastIndexOf(";") - 1)
                    fileSize = line.substring(line.lastIndexOf("=") + 2, line.lastIndex).toLong()
                    break
                }
            }
            //开始处理文件内容
            val file = File(dir, fileName)
            //TODO 暂时的处理
            if (!file.createNewFile() && !file.exists()) return@thread
            val byteArr = ByteArray(4 * 1024)
            val out = file.outputStream()
            var readCount = 0
            var length = -1
            while (true) {
                length = input.read(byteArr)
                readCount += length
                //最后会有一个结尾的标识符 已读字节数大于文件的大小以后 认为已经读到末尾 不写入最后的标识符 -4是标识符前后两个\r\n
                if (readCount > fileSize) {
                    //下面的代码不行：存在一种情况 4个字节的末尾包含了一半的标识符 这样的话就会少写入几个字节
                    //   length = (byteArr.size - endFlag.toByteArray().size - 4)
                    //直接减去超出文件长度的部分
                    length -= (readCount - fileSize).toInt()
                }
                out.write(byteArr, 0, length)
                out.flush()
                callback(readCount.toLong(), fileSize, fileName)
                //没办法在下次循环时用length=-1来判断读取完毕 因为浏览器在发送完毕后会关闭这个链接导致异常 所以length不等于缓冲区长度就认为读写完毕
                if (length < byteArr.size && readCount >= fileSize)
                    break

            }
            //ψ(｀∇´)ψ收工
            input.close()
            out.close()
            downloadSocket.close()
        }
    }

    private fun readLine4Heanders(ins: InputStream): String {
        val line = StringBuilder()
        while (true) {
            val char = ins.read()
            if (char.toChar() == '\r')
                break
            line.append(char.toChar())
        }
        ins.read()
        return line.toString()
    }

    //无尽递归地狱 ψ(｀∇´)ψ
    private fun waitingResponse(ssController: (ss: ServerSocket) -> Unit): Socket? {
        //想了下 80端口经常被奇奇怪怪的东西占用 所以......
        val ss = ServerSocket(8848)
        ssController(ss)
        try {
            val socket = ss.accept()
            val line = socket.getInputStream().bufferedReader().readLine()
            val location = line.split(" ")[1]
            ss.close()
            if (location == "/kamui")
                return socket
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
            ss.close()
            return null
        }
        return waitingResponse(ssController)
    }
}