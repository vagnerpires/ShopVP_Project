package com.example.shopvp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopvp.databinding.FragmentWishlistBinding
import com.example.shopvp.databinding.SingleWishlistBinding
import com.example.shopvp.model.ShopItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WishlistFragment: Fragment() {
    private lateinit var binding : FragmentWishlistBinding
    lateinit var viewModel : WishlistViewModel
    private lateinit var adapter: WishlistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wishlist, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WishlistAdapter()

        binding = FragmentWishlistBinding.bind(view)

        binding.wishlistBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.wishlistRecyclerView.adapter = adapter

        adapter.setOnItemClickListener {
            val action = WishlistFragmentDirections.actionWishlistFragmentToProductDetailFragment(it)
            findNavController().navigate(action)
        }

        adapter.setOnItemDeleteListener {
            viewModel.deleteWishlist(it)
        }

        viewModel.getWishlist().observe(viewLifecycleOwner){shopItems ->
            adapter.differ.submitList(shopItems)
        }

        binding.wishlistDelete.setOnClickListener {
            viewModel.clearWishlist()
        }



    }

    inner class WishlistViewModel constructor(
        private val wishlistUseCase: WishlistUseCase
    ) : ViewModel() {

        fun getWishlist() = liveData {
            wishlistUseCase.getWishlist().collect{
                emit(it)
            }
        }

        fun deleteWishlist(shopItem: ShopItem) = viewModelScope.launch(Dispatchers.IO) {
            wishlistUseCase.deleteWishlist(shopItem)
        }

        fun clearWishlist() = viewModelScope.launch(Dispatchers.IO) {
            wishlistUseCase.clearWishlist()
        }


    }

    inner class WishlistAdapter : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

        private val callback = object : DiffUtil.ItemCallback<ShopItem>() {
            override fun areItemsTheSame(oldItem: ShopItem, newItem: ShopItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShopItem, newItem: ShopItem): Boolean {
                return oldItem == newItem
            }

        }

        val differ = AsyncListDiffer(this,callback)

        private var onItemClickListener : ((ShopItem)-> Unit) = {}
        private var onItemDeleteListener : ((ShopItem)-> Unit) = {}

        fun setOnItemClickListener(listener : (ShopItem)-> Unit){
            onItemClickListener = listener
        }

        fun setOnItemDeleteListener(listener : (ShopItem)-> Unit){
            onItemDeleteListener = listener
        }

        inner class WishlistViewHolder(private val binding : SingleWishlistBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bindData(shopItem: ShopItem){

                Glide.with(binding.itemImage)
                    .load(shopItem.image)
                    .into(binding.itemImage)

                binding.itemTitle.text = shopItem.title
                binding.itemPrice.text = "USD ${shopItem.price}"
                binding.itemRating.text = "${shopItem.rating.rate}"
                binding.itemReview.text = "${shopItem.rating.count} Reviews"

                binding.itemView.setOnClickListener {
                    onItemClickListener(shopItem)
                }

                binding.itemDelete.setOnClickListener {
                    onItemDeleteListener(shopItem)
                }

            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
            val binding = SingleWishlistBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return WishlistViewHolder(binding)
        }

        override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
            val shopItem = differ.currentList[position]
            holder.bindData(shopItem)
        }

        override fun getItemCount() =  differ.currentList.size


    }

    class WishlistUseCase constructor(
        private val repository: LoginFragment.ShopRepository
    ) {

        suspend fun addToWishlist(shopItem: ShopItem){
            repository.addToWishlist(shopItem)
        }

        suspend fun deleteWishlist(shopItem: ShopItem){
            return repository.deleteWishlistItem(shopItem)
        }

        fun getWishlist() : Flow<List<ShopItem>> {
            return repository.getWishlistItems()
        }

        suspend fun clearWishlist(){
            return repository.clearWishlist()
        }

    }

}