package generators

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import models.ViewerData
import java.io.BufferedWriter
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.text.Charsets.UTF_8

/**
 * This script pulls data from the ViewerTable dynamodb
 * and generates a set of GEOJson files for use with mapbox.
 *
 * A GEOJson file is generated for each of the attributes of the dynamodb table
 * that we want to show on the map
 */
fun main(args: Array<String>) {
    //Load the data from Dynamo
    val dynamo = AmazonDynamoDBAsyncClientBuilder.standard()
            .withRegion("us-east-1")
            .withCredentials(ProfileCredentialsProvider("spatial-data"))
            .build()
    val dbmapper = DynamoDBMapper(dynamo)
    val rawResults = dbmapper.parallelScan(ViewerData::class.java, DynamoDBScanExpression(), 12)

    val ignoreThese = listOf("parcelId", "lat", "lng")
    val files: MutableMap<String, Pair<File, BufferedWriter>> = mutableMapOf()
    val smallFiles: MutableMap<String, Pair<File, BufferedWriter>> = mutableMapOf()
    ViewerData::class.memberProperties
            .filterNot { ignoreThese.contains(it.name) }
            .forEach {
                val file = File("${it.name}.json.gz")
                val smallFile = File("${it.name}-small.json.gz")
                val out = GZIPOutputStream(file.outputStream()).bufferedWriter(UTF_8)
                val smallOut = GZIPOutputStream(smallFile.outputStream()).bufferedWriter(UTF_8)
                out.write("""{
                "type": "FeatureCollection",
                "features": [""".trimIndent())
                smallOut.write("""{
                "type": "FeatureCollection",
                "features": [""".trimIndent())

                files[it.name] = Pair(file, out)
                smallFiles[it.name] = Pair(smallFile, smallOut)
            }

    val resultsIter = rawResults.iterator()
    var count = 0
    while (resultsIter.hasNext()) {
        val video = resultsIter.next()!!
        for (member in video::class.memberProperties) {
            if (!ignoreThese.contains(member.name)) {
                val weight = (member as KProperty1<ViewerData, Any?>).get(video)
                if (weight != null && video.lat != null && video.lng != null) {
                    files[member.name]?.second?.write("""{
                        "type": "Feature",
                        "properties": {
                            "weight": $weight,
                            "id": "${video.parcelId}"
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [ ${video.lng}, ${video.lat} ]
                        }}""".trimIndent()
                    )
                    if (resultsIter.hasNext())
                        files[member.name]?.second?.write(",")

                    if (count < 25) {
                        smallFiles[member.name]?.second?.write("""{
                        "type": "Feature",
                        "properties": {
                            "weight": $weight,
                            "id": "${video.parcelId}"
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [ ${video.lng}, ${video.lat} ]
                        }}""".trimIndent()
                        )
                        if (resultsIter.hasNext() && count < 24)
                            smallFiles[member.name]?.second?.write(",")
                    }

                }
            }
        }
        count++
    }

    val s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(ProfileCredentialsProvider("spatial-data"))
            .build()

    ViewerData::class.memberProperties
            .filterNot { ignoreThese.contains(it.name) }
            .forEach {
                //close the geojson object and features array
                files[it.name]?.second?.write("]}")
                files[it.name]?.second?.close()
                smallFiles[it.name]?.second?.write("]}")
                smallFiles[it.name]?.second?.close()

                //upload to s3
                println("sending ${it.name} | ${files[it.name]?.first}")
                val md = ObjectMetadata()
                md.contentType = "application/json"
                md.contentEncoding = "gzip"
                s3.putObject(
                        PutObjectRequest(
                                "spatial-data-web-support",
                                "${it.name}.json.gz",
                                files[it.name]?.first!!.inputStream(),
                                md
                        ).withCannedAcl(CannedAccessControlList.PublicRead)
                )
                println("sending ${it.name} | ${smallFiles[it.name]?.first}")
                val smd = ObjectMetadata()
                smd.contentType = "application/json"
                smd.contentEncoding = "gzip"
                s3.putObject(
                        PutObjectRequest(
                                "spatial-data-web-support",
                                "${it.name}-small.json.gz",
                                smallFiles[it.name]?.first!!.inputStream(),
                                smd
                        ).withCannedAcl(CannedAccessControlList.PublicRead)
                )

                files[it.name]?.first?.delete()
            }
}
