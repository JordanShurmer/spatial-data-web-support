//package generators
//
////
////import com.amazonaws.auth.profile.ProfileCredentialsProvider
////import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
////import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
////import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
//import com.amazonaws.auth.profile.ProfileCredentialsProvider
//import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
//import kdtree.HyperRect
//import kdtree.KDTree
//import models.ViewerData
//
////import models.ViewerData
////import java.io.File
////import java.util.zip.GZIPOutputStream
////import kotlin.text.Charsets.UTF_8
////
/////**
//// * TODO: add javadocs
//// */
////
//
//fun main(args: Array<String>) {
//
//
//    //Load the data from Dynamo
//    val dynamo = AmazonDynamoDBAsyncClientBuilder.standard()
//            .withRegion("us-east-1")
//            .withCredentials(ProfileCredentialsProvider("spatial-data"))
//            .build()
//    val dbmapper = DynamoDBMapper(dynamo)
//    val rawResults = dbmapper.parallelScan(ViewerData::class.java, DynamoDBScanExpression(), 12)
////
//    //limit to Sequoya Hills for now
//    val data = rawResults.filter {
//        it.lat != null && it.lng != null
//                && 35.946846 > it.lat!! && it.lat!! > 35.9260523
//                && -83.956160 > it.lng!! && it.lng!! > -83.980512
//    }.map {
//        it.location = doubleArrayOf(it.lat!!, it.lng!!)
//    }.toMutableList()
//
//    //build the tree
//    val kdtree = KDTree(data, HyperRect(doubleArrayOf(-83.980512, 35.9260523), doubleArrayOf(-83.956160, 35.946846)))
////
////    val file = File("neighbor-lines.json.gz")
////    val out = GZIPOutputStream(file.outputStream()).bufferedWriter(UTF_8)
////    out.write("""{
////                "type": "FeatureCollection",
////                "features": [""".trimIndent()
////    )
//}