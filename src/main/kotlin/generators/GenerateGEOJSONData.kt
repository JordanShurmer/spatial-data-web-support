package generators

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import models.ViewerData
import java.io.BufferedWriter
import java.io.File
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

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
    ViewerData::class.memberProperties
            .filterNot { ignoreThese.contains(it.name) }
            .forEach {
                val file = File("${it.name}.json")
                val out = file.bufferedWriter()
                out.write("""
            {
                "type": "FeatureCollection",
                "features": [
            """)

                files[it.name] = Pair(file, out)
            }

    val resultsIter = rawResults.iterator()
    while (resultsIter.hasNext()) {
        val video = resultsIter.next()!!
        for (member in video::class.memberProperties) {
            if (!ignoreThese.contains(member.name)) {
                val weight = (member as KProperty1<ViewerData, Any?>).get(video)
                files[member.name]?.second?.write("""
                {
                    "type": "Feature",
                    "properties": {
                        "weight": $weight,
                        "id": "${video.parcelId}"
                    },
                    "geometry": {
                        "type": "Point",
                        "coordinate": [ ${video.lat}, ${video.lng} ]
                    }
                }
                """)
                if (resultsIter.hasNext())
                    files[member.name]?.second?.write(",")
            }
        }
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

                //upload to s3
                println("sending ${it.name} | ${files[it.name]?.first}")
                s3.putObject("spatial-data-web-support", "${it.name}.geojson", files[it.name]?.first)

                files[it.name]?.first?.delete()
            }
}
