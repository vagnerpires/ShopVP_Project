package com.example.shopvp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopvp.databinding.FragmentCartBinding
import com.example.shopvp.model.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.shopvp.ProductDetailFragment.CartItem2
import com.example.shopvp.databinding.CartItemBinding

class CartFragment: Fragment() {

    private lateinit var binding : FragmentCartBinding
    lateinit var cartViewModel: CartViewModel
    lateinit var cartAdapter: CartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCartBinding.bind(view)

        cartViewModel.getCartItems().observe(viewLifecycleOwner){
            cartAdapter.differ.submitList(it)
        }

        cartAdapter.setOnRemoveClickListener {
            cartViewModel.deleteCart(it)
        }

        cartAdapter.incrementClickListener {
            Log.i("CartFragment","I don click the increment shit")
            cartViewModel.increment(it)
        }

        cartAdapter.decrementClickListener {
            Log.i("CartFragment","I don click the decrement shit")
            cartViewModel.decrement(it)
        }

        cartViewModel.totalItems.observe(viewLifecycleOwner){
            binding.cartItemsInfo.text = "Total $it Items"
        }

        cartViewModel.totalItemsPrice.observe(viewLifecycleOwner){
            binding.cartItemsPrice.text = "USD ${Utils.formatPrice(it.toString())}"
        }

        binding.cartRecyclerView.adapter = cartAdapter

        binding.cartBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cartClearAll.setOnClickListener {
            cartViewModel.clearCart()
        }

    }

    class CartViewModel @Inject constructor(
        private val cartUseCase: ProductDetailFragment.CartUseCase
    ) : ViewModel() {

        val totalItems : MutableLiveData<Int> = MutableLiveData()
        val totalItemsPrice : MutableLiveData<Double> = MutableLiveData()

        fun getCartItems() = liveData {
            cartUseCase.getCartItems().collect{
                emit(it)
                computeTotal(it)
            }
        }

        fun computeTotal(cartItems : List<CartItem2>) = viewModelScope.launch(Dispatchers.IO){
            var price = 0.00
            cartItems.forEach { product ->
                price += product.price.toDouble()
            }
            totalItemsPrice.postValue(price)
            totalItems.postValue(cartItems.size)
        }

        fun deleteCart(cartItem2: CartItem2) = viewModelScope.launch(Dispatchers.IO) {
            cartUseCase.deleteCartItem(cartItem2)
        }

        fun clearCart() = viewModelScope.launch(Dispatchers.IO) {
            cartUseCase.clearCart()
        }

        fun increment(cartItem: CartItem2){
            updateProductInCart(quantity = cartItem.quantity + 1, price = cartItem.price.toDouble() + cartItem.pricePerItem,cartItem)
        }

        fun decrement(cartItem: CartItem2){
            if (cartItem.quantity > 1){
                updateProductInCart(quantity = cartItem.quantity -1, price = cartItem.price.toDouble() - cartItem.pricePerItem,cartItem)
            }
        }

        private fun updateProductInCart(quantity: Int, price: Double, cartItem: CartItem2) = viewModelScope.launch(
            Dispatchers.IO
        ){
            val copy = cartItem.copy(price = Utils.formatPrice(price.toString()), quantity = quantity)
            cartUseCase.updateCartItem(copy)
        }
    }

    class CartAdapter : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

        private val callback = object : DiffUtil.ItemCallback<CartItem2>() {
            override fun areItemsTheSame(oldItem: CartItem2, newItem: CartItem2): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CartItem2, newItem: CartItem2): Boolean {
                return oldItem == newItem
            }

        }

        val differ = AsyncListDiffer(this, callback)

        private var incrementListener : ((CartItem2) -> Unit) = {}

        private var decrementListener : ((CartItem2) -> Unit) = {}

        private var removeListener : ((CartItem2) -> Unit) = {}

        fun setOnRemoveClickListener(listener : (CartItem2) -> Unit){
            removeListener = listener
        }

        fun incrementClickListener(listener : (CartItem2) -> Unit){
            incrementListener = listener
        }

        fun decrementClickListener(listener : (CartItem2) -> Unit){
            decrementListener = listener
        }


        inner class CartViewHolder(private val binding : CartItemBinding) : RecyclerView.ViewHolder(binding.root){
            fun bindData(cartItem: CartItem2){

                binding.cartItemName.text = cartItem.title
                binding.cartItemPrice.text = "USD ${Utils.formatPrice(cartItem.price)}"
                binding.cartItemQuantity.text = "${cartItem.quantity}"

                Glide.with(binding.cartItemImage)
                    .load(cartItem.image)
                    .into(binding.cartItemImage)

                binding.cartItemDelete.setOnClickListener {
                    removeListener(cartItem)
                }

                binding.cartItemPlus.setOnClickListener {
                    incrementListener(cartItem)
                }

                binding.cartItemMinus.setOnClickListener {
                    decrementListener(cartItem)
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return CartViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
            val cartItem = differ.currentList[position]
            holder.bindData(cartItem)

        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }


    }

}