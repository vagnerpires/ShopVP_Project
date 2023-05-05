package com.example.shopvp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.shopvp.databinding.FragmentProductDetailBinding
import com.example.shopvp.model.ShopItem
import com.example.shopvp.model.Utils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProductDetailFragment: Fragment() {

    private lateinit var binding : FragmentProductDetailBinding

    lateinit var viewModel : ProductDetailViewModel

    private lateinit var shopItem: ShopItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shopItem = ProductDetailFragmentArgs.fromBundle(requireArguments()).shopItem

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_detail, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductDetailBinding.bind(view)

        updateViews()

        binding.productDetailBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.productDetailAddToCart.setOnClickListener {
            val cartItem = CartItem2(shopItem.id,shopItem.image,
                Utils.formatPrice(shopItem.price.toString()),shopItem.title,1,shopItem.price)
            viewModel.saveToCart(cartItem)
            Snackbar.make(binding.productDetailAddToCart,"Added to Cart Successfully", Snackbar.LENGTH_SHORT).show()
        }

        binding.productDetailCart.setOnClickListener {
            findNavController().navigate(R.id.action_productDetailFragment_to_cartFragment)
        }

    }

    private fun updateViews() {

        binding.productDetailPrice.text = "USD ${shopItem.price}"
        binding.productDetailTitle.text = shopItem.title
        binding.productDetailDescription.text = shopItem.description

        Glide.with(binding.productDetailImage)
            .load(shopItem.image)
            .into(binding.productDetailImage)

        binding.productDetailRating.text = "${shopItem.rating.rate}"
        binding.productDetailReviews.text = "${shopItem.rating.count} Reviews"

    }

    class ProductDetailViewModel constructor(
        private val cartUseCase: CartUseCase,
    ) : ViewModel() {

        fun saveToCart(cartItem2: CartItem2) = viewModelScope.launch(Dispatchers.IO) {
            cartUseCase.addToCartItem(cartItem2)
        }

    }

    data class CartItem2(
        val id: Int,
        val image: String,
        val price: String,
        val title: String,
        val quantity: Int,
        val pricePerItem: Double,
    )

    class CartUseCase constructor(
        private val repository: LoginFragment.ShopRepository
    ) {

        suspend fun deleteCartItem(cartItem2: CartItem2){
            repository.deleteCartItems(cartItem2)
        }

        suspend fun clearCart(){
            repository.clearCart()
        }

        fun getCartItems() : Flow<List<CartItem2>> {
            return repository.getCartItems()
        }

        suspend fun addToCartItem(cartItem2: CartItem2){
            repository.addToCartItems(cartItem2)
        }

        suspend fun updateCartItem(cartItem2: CartItem2){
            repository.updateCartItems(cartItem2)
        }

    }

}