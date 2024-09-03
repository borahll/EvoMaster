package org.evomaster.core.utils

import java.io.*
import java.net.Socket
import java.util.regex.Pattern
import kotlin.system.exitProcess

class VersionMismatchException(message: String) : Exception(message)

object SendVersion {

    @Throws(IOException::class)
    fun extractVersionFromYml(filePath: String): String? {
        BufferedReader(FileReader(filePath)).use { reader ->
            val pattern = Pattern.compile("evomaster-version: (\\S+)")
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val matcher = line?.let { pattern.matcher(it) }
                if (matcher != null) {
                    if (matcher.find()) {
                        return matcher.group(1)
                    }
                }
            }
        }
        return null
    }

    fun sendVersion() {
        val serverAddress = "localhost"
        val port = 9999

        try {
            // Extract version
            println("Current working directory: ${System.getProperty("user.dir")}")

            val ymlVersion = extractVersionFromYml("../../.github/workflows/release.yml")
            // Connect to the server
            Socket(serverAddress, port).use { socket ->
                socket.getOutputStream().use { output ->
                    socket.getInputStream().use { input ->
                        val writer = PrintWriter(output, true)
                        val reader = BufferedReader(InputStreamReader(input))

                        // Send version to the server
                        writer.println(ymlVersion)

                        // Read multiple messages from the server
                        var response: String?
                        while (reader.readLine().also { response = it } != null) {
                            if (response == "-1") {
                                writer.println("done")
                                throw VersionMismatchException("[ERROR] Version mismatch with EMB and EvoMaster")
                            } else {
                                println("Server response: $response")
                            }
                        }
                    }
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: VersionMismatchException) {
            println("\u001b[31m ${e.message}  \u001b[0m\\")
            exitProcess(1)
        }
    }
}
