package com.example.shopvp.model

import com.example.shopvp.HomeFragment
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ShopItem(
    @SerializedName("category")
    val category: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("image")
    val image: String,
    @SerializedName("price")
    val price: Double,
    @SerializedName("rating")
    val rating: HomeFragment.Rating,
    @SerializedName("title")
    val title: String
) : Serializable