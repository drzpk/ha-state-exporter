package dev.drzepka.smarthome.haexporter.infrastructure.database

import com.influxdb.client.InfluxDBClientOptions
import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import dev.drzepka.smarthome.haexporter.infrastructure.properties.InfluxDBDataSourceProperties
import okhttp3.OkHttpClient
import org.apache.logging.log4j.kotlin.Logging
import java.time.Duration

class InfluxDBClientProvider(private val properties: InfluxDBDataSourceProperties) {
    val client: InfluxDBClientKotlin
    val bucket: String
        get() = properties.bucket

    init {
        logger.info { "Creating InfluxDB client with ${properties.copy(token = "***")}" }
        val httpClient = OkHttpClient()
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
        val options = InfluxDBClientOptions.builder()
            .url(properties.url)
            .bucket(properties.bucket)
            .org(properties.org)
            .authenticateToken(properties.token.toCharArray())
            .okHttpClient(httpClient)
            .build()

        client = InfluxDBClientKotlinFactory.create(options)
    }

    companion object : Logging
}
