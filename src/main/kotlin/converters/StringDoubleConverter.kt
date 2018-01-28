package converters

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter

/**
 * TODO: add javadocs
 */
class StringDoubleConverter : DynamoDBTypeConverter<String, Double> {
    override fun unconvert(str: String?): Double? {
        return try {
            str?.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun convert(dbl: Double?): String? {
        return dbl?.toString()
    }
}