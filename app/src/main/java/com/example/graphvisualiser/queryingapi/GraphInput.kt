package com.example.graphvisualiser.queryingapi

data class GraphInput(val appID: String, val coordinates: Array<Pair<Float, Float>>) {
    /* kotlin doesnt like if i dont do this */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphInput

        if (appID != other.appID) return false
        if (!coordinates.contentEquals(other.coordinates)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appID.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        return result
    }
}