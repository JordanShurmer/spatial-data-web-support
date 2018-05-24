package models

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted
import converters.StringDoubleConverter

/**
 * TODO: add javadocs
 */
@DynamoDBTable(tableName = "ViewerData")
class ViewerData(
        @get:DynamoDBHashKey(attributeName = "parcelId")
        var parcelId: String = "",

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "lat")
        var lat: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "lng")
        var lng: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "30DayZestimateChange")
        var zestimateChange: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "Appraisal")
        var appraisal: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "Assessment")
        var assessment: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "AssessmentRatio")
        var assessmentRatio: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "YearBuilt")
        var yearBuilt: Double? = null,

        @get:DynamoDBTypeConverted(converter = StringDoubleConverter::class)
        @get:DynamoDBAttribute(attributeName = "YearAssessed")
        var yearAssessed: Double? = null) : Locatable {

    override val location = Point(lat ?: 0.0, lng ?: 0.0)

    override fun toString(): String {
        return "ViewerData(parcelId='$parcelId', lat=$lat, lng=$lng, appraisal=$appraisal, assessment=$assessment)"
    }


}
