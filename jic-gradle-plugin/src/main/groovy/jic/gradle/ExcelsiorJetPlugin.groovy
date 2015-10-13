package jic.gradle

import jic.client.JicClient
import org.gradle.api.Plugin
import org.gradle.api.Project

class ExcelsiorJetPlugin implements Plugin<Project> {

    private final jetCompile = 'jetCompile'
    private File jar = null

    @Override
    void apply(Project project) {
        jar = project.tasks['jar'].archivePath
        project.task(jetCompile, dependsOn: 'jar') {
            inputs.file jar
            outputs.dir 'build/native'

        }
        project.tasks[jetCompile] << {
            def client = new JicClient("http://jic-front:4567")
            try {
                println(jar.absolutePath)
                println(jar.size())
                println(jar.exists())
                println("Uploading jar")
                def fileId = client.upload(jar)
                println("Compiling jar")
                def taskId = client.compile(fileId)
                println("Waiting for result")

                def linuxThread = Thread.start {
                    def linuxResultId = client.waitForResult(taskId, "LINUX")
                    println("Download linux bin")
                    client.download(linuxResultId, new File('build/jet', jar.name + ".linux.zip"))
                    println("linux bin downloaded")
                }

                def winThread = Thread.start {
                    def winResultId = client.waitForResult(taskId, "WIN")
                    println("Download win bin")
                    client.download(winResultId, new File('build/jet', jar.name + ".win.zip"))
                    println("win bin downloaded")
                }
                linuxThread.join()
                winThread.join()
            } finally {
                client.close()
            }
        }
    }
}
