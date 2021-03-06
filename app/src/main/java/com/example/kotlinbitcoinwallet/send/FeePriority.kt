import com.google.gson.annotations.SerializedName

data class FeePriority(
    @SerializedName("fastestFee")
    var highFee: Long,
    @SerializedName("hourFee")
    var medFee:Long,
    @SerializedName("minimumFee")
    var lowFee:Long ){

    override fun toString(): String {
        return "Low Fee: $lowFee \t Med Fee: $medFee \t High Fee: $highFee"
    }
    fun gethighFee(): Long{
        return highFee
    }
}
