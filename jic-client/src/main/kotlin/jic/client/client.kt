package jic.client

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClients
import sun.tools.jar.resources.jar
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.stream.Stream

object Client {
    private val baseUrl = "http://localhost:4567"

    private fun base(tail: String) = baseUrl + "/" + tail

    @JvmStatic fun main(args: Array<String>) {

        val httpClient = HttpClients.createDefault()
        val uploadFile = HttpPost(base("upload"))

        val builder = MultipartEntityBuilder.create()
        builder.addBinaryBody("file", File("/home/azhidkov/tmp/hw/HelloWorld.jar"), ContentType.APPLICATION_OCTET_STREAM, "java")
        val multipart = builder.build()

        uploadFile.entity = multipart

        val response = httpClient.execute(uploadFile)
        val responseEntity = response.entity
        val fileUid = responseEntity.content.bufferedReader().readText()

        val compile = HttpPost(base("compile/$fileUid"))
        val r = httpClient.execute(compile)
        val re = r.entity
        val taskId = re.content.bufferedReader().readText()
        println(taskId)

        val statues = sequence {
            Thread.sleep(1000)
            val status = HttpGet(base("result/$taskId"))
            httpClient.execute(status).entity.content.bufferedReader().readText()
        }
        val resultId = statues.find { it != "null" }

        val jar = File("/tmp/jic-client/out.zip")
        jar.deleteRecursively()
        jar.parentFile.mkdirs()
        URL(base("download/$resultId")).openConnection().inputStream.use { input ->
            FileOutputStream(jar).use {
                input.copyTo(it)
            }
        }
        httpClient.close()
    }
}
