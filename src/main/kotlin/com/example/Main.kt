@file:Suppress("SpellCheckingInspection")

package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val queue = mutableListOf<DownloadingObj>()

@Suppress("MemberVisibilityCanBePrivate")
class DownloadingObj(var url: String, var soundLUFS: Int = -23)
{
    var pid = -1L
    var queueID = ""
    var whenDownloadStarted = 0L
    
    init
    {
        this.queueID = (url + (System.currentTimeMillis() % 1000)).hashCode().toString()
    }
    
    fun download()
    {
        val cmd = listOf("import yt_dlp_kt", "yt_dlp_kt.orchestration(\"${this.url}\",${this.soundLUFS})")
        whenDownloadStarted = System.currentTimeMillis()
        pid = exec("python -c" + cmd.joinToString(";"))
    }
    
    fun isFinished(): Boolean
    {
        val processBuilder = ProcessBuilder()
        if(System.getProperty("os.name").contains("Windows"))
        {
            processBuilder.command("sh", "-c", "tasklist /fi \"PID eq ${this.pid}\" /fo csv")
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
    embeddedServer(Netty, port = 8080, host = "0.0.0.0")
    {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/listQueue") {
                @Suppress("unused")
                data class ListQueueRespond(val elements: List<String>)
                {
                    val size = queue.size
                }
                if(queue.size != 0)
                {
                    val tempElement = mutableListOf<String>()
                    for(i in queue)
                        tempElement.add(i.queueID)
                    call.respond(HttpStatusCode.OK, ListQueueRespond(tempElement))
                }
                else
                    call.respond(HttpStatusCode.OK, ListQueueRespond(listOf()))
            }
            
            
            get("/download") {
                val url = call.request.queryParameters.get("url")
                if(url == null)
                {
                    val errorMsg = mapOf("Error" to "body parameter \"url\" is required.") //url 존재 X->에러
                    call.respond(HttpStatusCode.BadRequest, errorMsg)
                }
                
                val target = (call.request.queryParameters.get("target") ?: "-23").toInt() // 볼륨은 없으면 -23 아니면 그걸로 설정
                val downloadInstnce = DownloadingObj(url!!, target)
                
                val order = queue.size //response 준비
                queue.add(downloadInstnce)
                downloadInstnce.download()
                
                @Suppress("unused")
                data class DownloadSuccessfulRespond(val order: Int) //response용 데이터클래스
                {
                    val state = "success"
                    val queueID = downloadInstnce.queueID
                    val whenDownloadStarted = downloadInstnce.whenDownloadStarted
                }
                
                call.respond(HttpStatusCode.Created, DownloadSuccessfulRespond(order))
            }
            
            
            get("/inspectTask") {
                val taskID = call.request.queryParameters.get("taskID")
                if(taskID == null)
                {
                    val errorMsg = mapOf("Error" to "body parameter \"taskID\" is required.") //url 존재 X->에러
                    call.respond(HttpStatusCode.BadRequest, errorMsg)
                }
                for(i in queue)
                {
                    if(i.queueID == taskID)
                    {
                        @Suppress("unused")
                        data class InspectTaskRespond(val taskID: String)
                        {
                            val state = i.isFinished()
                            val url = i.url
                            val target = i.soundLUFS
                            val whenDownloadStarted = i.whenDownloadStarted
                        }
                        call.respond(HttpStatusCode.OK, InspectTaskRespond(taskID))
                    }
                }
                val errorMsg = mapOf("Error" to "there is no queue whose taskID is \"${taskID}\".") //url 존재 X->에러
                call.respond(HttpStatusCode.NotFound, errorMsg)
            }
        }
    }.start(wait = true)
}

fun exec(cmd: String, willFork: Boolean = true): Long
{
    val processBuilder = ProcessBuilder()
    
    if(System.getProperty("os.name").contains("Windows"))
        processBuilder.command("cmd", "/c", cmd)
    else
        processBuilder.command("sh", "-c", cmd)
    
    val process: Process = processBuilder.start()
    
    return if(willFork)
    {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        val line = reader.readText()
        println(line)
        process.waitFor().toLong()
    }
    else
        process.pid()
}
