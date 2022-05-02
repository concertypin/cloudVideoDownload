@file:Suppress("SpellCheckingInspection")

package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val queue = mutableListOf<DownloadingObj>()

@Suppress("MemberVisibilityCanBePrivate")
class DownloadingObj(var url: String, var soundLUFS: Int = -23)
{
    var pid = -1L
    var queueID = ""
    var whenDownloadStarted = "NaN"
    
    init
    {
        this.queueID = (url + (System.currentTimeMillis() % 1000)).hashCode().toString()
    }
    
    fun download()
    {
        val cmd =
            listOf("from src\\main import yt_dlp_kt as ytdlp", "ytdlp.orchestration(\"${this.url}\",${this.soundLUFS})")
        whenDownloadStarted = System.currentTimeMillis().toString()
        pid = exec("python -c" + cmd.joinToString(";"), willWaitForFinish = true)
    }
    
    fun isFinished(): Boolean
    {
        val processBuilder = ProcessBuilder()
        if(System.getProperty("os.name").contains("Windows"))
        {
            processBuilder.command("cmd", "/c", "tasklist /fi \"PID eq ${this.pid}\" /fo csv")
            val process: Process = processBuilder.start()
            
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val line = reader.readText().split("\n")
            return line.size >= 2
        }
        else
        {
            processBuilder.command("sh", "-c", "ps -ef | grep ${this.pid}")
            val process: Process = processBuilder.start()
            
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val line = reader.readText().split("\n")
            return line.size >= 2
        }
    }
}
fun main()
{
    println(System.getProperty("user.dir"))
    embeddedServer(Netty, port = (System.getenv("PORT") ?: "0").toInt(), host = "0.0.0.0")
    {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/listQueue") {
                if(queue.size != 0)
                {
                    var tempElement = "["
                    for(i in queue)
                        tempElement += "\"${i.queueID}\","
                    
                    tempElement = tempElement.substring(0 until tempElement.length - 1) + "]"
                    call.respond(HttpStatusCode.OK, mapOf("size" to queue.size.toString(), "elements" to tempElement))
                }
                else
                    call.respond(HttpStatusCode.OK, mapOf("size" to queue.size.toString(), "elements" to "[]"))
            }
            
            
            get("/download") {
                val url = call.request.queryParameters["url"]
                if(url == null)
                {
                    val errorMsg = mapOf("Error" to "body parameter \"url\" is required.") //url 존재 X->에러
                    call.respond(HttpStatusCode.BadRequest, errorMsg)
                }
    
                val target = (call.request.queryParameters["target"] ?: "-23").toInt() // 볼륨은 없으면 -23 아니면 그걸로 설정
                val downloadInstnce = DownloadingObj(url!!, target)
    
                val order = queue.size //response 준비
                queue.add(downloadInstnce)
                downloadInstnce.download()
    
                val downloadSuccessfulRespond = mapOf(
                    "order" to order.toString(), "state" to "success",
                    "queueID" to downloadInstnce.queueID,
                    "whenDownloadStarted" to downloadInstnce.whenDownloadStarted
                )
    
    
                call.respond(HttpStatusCode.Created, downloadSuccessfulRespond)
            }
            
            
            get("/inspectTask") {
                val taskID = call.request.queryParameters["taskID"]
                if(taskID == null)
                {
                    val errorMsg = mapOf("Error" to "body parameter \"taskID\" is required.") //url 존재 X->에러
                    call.respond(HttpStatusCode.BadRequest, errorMsg)
                }
                for(i in queue)
                {
                    if(i.queueID == taskID)
                    {
    
    
                        val inspectTaskRespond = mapOf(
                            "taskID" to i.queueID,
                            "state" to i.isFinished().toString(),
                            "url" to i.url,
                            "target" to i.soundLUFS.toString(),
                            "whenDownloadStarted" to i.whenDownloadStarted
                        )
                        call.respond(HttpStatusCode.OK, inspectTaskRespond)
                    }
                }
                val errorMsg = mapOf("Error" to "there is no queue whose taskID is \"${taskID}\".") //url 존재 X->에러
                call.respond(HttpStatusCode.NotFound, errorMsg)
            }
        }
    }.start(wait = true)
}

fun exec(cmd: String, willWaitForFinish: Boolean = true): Long
{
    val processBuilder = ProcessBuilder()
    
    if(System.getProperty("os.name").contains("Windows"))
        processBuilder.command("cmd", "/c", cmd)
    else
        processBuilder.command("sh", "-c", cmd)
    
    
    val process: Process = processBuilder.start()
    
    return if(willWaitForFinish)
    {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        val line = reader.readText()
        println(line)
        process.waitFor().toLong()
    }
    else
    {
        if(System.getenv("debug") == "1")
        {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        process.pid()
    }
}
