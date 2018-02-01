package generators

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.beust.klaxon.Klaxon
import models.ViewerData
import java.io.File

/**
 *
 * This script pulls data from dynamoDB and generates
 * a giant Javascript array and prints it out into a .js file.
 * It then uploads this .js file into s3.
 *
 */
fun main(args: Array<String>) {

    //Load the data from Dynamo
    val dynamo = AmazonDynamoDBAsyncClientBuilder.standard()
            .withRegion("us-east-1")
            .withCredentials(ProfileCredentialsProvider("spatial-data"))
            .build()
    val dbmapper = DynamoDBMapper(dynamo)
    val rawResults = dbmapper.parallelScan(ViewerData::class.java, DynamoDBScanExpression(), 12)

    val klax = Klaxon()
    val vd = File("viewerData.json")

    vd.bufferedWriter().use { out ->
        out.write(klax.toJsonString(rawResults))
    }


    //upload the data to s3
    val s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(ProfileCredentialsProvider("spatial-data"))
            .build()
    s3.putObject("spatial-data-web-support", "viewer-data.json", vd)

    //clean up
    vd.delete()
}

