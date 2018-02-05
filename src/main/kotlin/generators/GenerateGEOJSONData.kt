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
    val file = File("alldata.json.gz")
    val smallFile = File("alldata-small.json.gz")
    val out = GZIPOutputStream(file.outputStream()).bufferedWriter(UTF_8)
    val smallOut = GZIPOutputStream(smallFile.outputStream()).bufferedWriter(UTF_8)
    out.write("""{
                "type": "FeatureCollection",
                "features": [""".trimIndent())
    smallOut.write("""{
                "type": "FeatureCollection",
                "features": [""".trimIndent())

    val propOut = File("props.txt").bufferedWriter()

    val resultsIter = rawResults.iterator()
    var count = 0
    while (resultsIter.hasNext()) {
        val video = resultsIter.next()!!
        if (video.lat != null && video.lng != null) {
            var propertyString = """
            "lng": "${video.lng}",
            "lat": "${video.lat}",
            "id": "${video.parcelId}""""
            for (member in video::class.memberProperties) {
                if (!ignoreThese.contains(member.name)) {
                    val weight = (member as KProperty1<ViewerData, Any?>).get(video)
                    if (weight != null && weight.toString().isNotEmpty()) {
                        val prop = """"${member.name}": ${weight.toString()}"""
                        if (!prop.endsWith(" ")) {
                            propOut.write("$prop~~~\n")
                            propertyString = """$propertyString,
                            $prop"""
                        }
                    }
                }
            }
            out.write("""{
                        "type": "Feature",
                        "properties": {
                            $propertyString
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [ ${video.lng}, ${video.lat} ]
                        }}""".trimIndent()
            )
            if (resultsIter.hasNext())
                out.write(",")

            if (count < 25) {
                smallOut.write("""{
                        "type": "Feature",
                        "properties": {
                        $propertyString
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [ ${video.lng}, ${video.lat} ]
                        }}""".trimIndent()
                )
                if (resultsIter.hasNext() && count < 24)
                    smallOut.write(",")
            }

        }
        count++
    }

    out.flush()
    smallOut.flush()
    propOut.flush()

    val s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(ProfileCredentialsProvider("spatial-data"))
            .build()

//close the geojson object and features array
    out.write("]}")
    out.close()
    smallOut.write("]}")
    smallOut.close()

//upload to s3
    println("sending alldata | $file")
    val md = ObjectMetadata()
    md.contentType = "application/json"
    md.contentEncoding = "gzip"
    s3.putObject(
            PutObjectRequest(
                    "spatial-data-web-support",
                    "alldata.json.gz",
                    file.inputStream(),
                    md
            ).withCannedAcl(CannedAccessControlList.PublicRead)
    )
    println("sending alldata-small | $smallFile")
    val smd = ObjectMetadata()
    smd.contentType = "application/json"
    smd.contentEncoding = "gzip"
    s3.putObject(
            PutObjectRequest(
                    "spatial-data-web-support",
                    "alldata-small.json.gz",
                    smallFile.inputStream(),
                    smd
            ).withCannedAcl(CannedAccessControlList.PublicRead)
    )

    file.delete()
}
