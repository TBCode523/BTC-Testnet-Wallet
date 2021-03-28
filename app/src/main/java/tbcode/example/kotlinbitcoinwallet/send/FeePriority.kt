import com.google.gson.annotations.SerializedName

data  class FeePriority(

    @SerializedName("fastestFee")
    var highFee: Int,
    @SerializedName("hourFee")
    var medFee:Int,
    @SerializedName("minimumFee")
    var lowFee:Int ){

    override fun toString(): String {
        return "Low Fee: $lowFee \t Med Fee: $medFee \t High Fee: $highFee"
    }


}
