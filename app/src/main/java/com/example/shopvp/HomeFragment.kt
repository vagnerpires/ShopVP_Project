package com.example.shopvp

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopvp.databinding.FragmentHomeBinding
import com.example.shopvp.databinding.SingleItemBinding
import com.example.shopvp.model.Network.isNetworkAvailable
import com.google.android.material.chip.Chip
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.example.shopvp.model.ShopItem

class HomeFragment : Fragment() {

    lateinit var viewModel : HomeViewModel

    lateinit var adapter : HomeAdapter
    private lateinit var binding : FragmentHomeBinding

    private var category2 = mutableListOf<Category2>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)

        viewModel.getAllCategories()

        viewModel.categories.observe(viewLifecycleOwner){response ->
            when(response){
                is LoginFragment.Resource.Success -> {
                    val categories = response.data
                    val chip = Chip(requireContext())
                    chip.text = "All"
                    chip.id = 0
                    chip.isChecked = true
                    category2.clear()
                    binding.chipGroup.removeAllViews()
                    category2.add(Category2(0,"All"))
                    binding.chipGroup.addView(chip)
                    categories?.forEachIndexed { index, category ->
                        val chip = Chip(requireContext())
                        chip.text = category
                        chip.id = index+1
                        category2.add(Category2(index,category))
                        binding.chipGroup.addView(chip)
                    }

                }
                is LoginFragment.Resource.Loading -> {
                    Log.i("HomeFragment","Loading...")
                }
                is LoginFragment.Resource.Error -> {
                    Log.i("HomeFragment","${response.message}")
                }
            }
        }

        viewModel.getAllProducts()

        viewModel.products.observe(viewLifecycleOwner){response ->
            when(response){
                is LoginFragment.Resource.Success -> {
                    adapter.differ.submitList(response.data)
                    binding.homeRecyclerView.visibility = View.VISIBLE
                    Log.i("HomeFragment","${response.data}")
                }
                is LoginFragment.Resource.Loading -> {
                    //binding.homeRecyclerView.visibility = View.INVISIBLE
                    Log.i("HomeFragment","Loading...")
                }
                is LoginFragment.Resource.Error -> {
                    Log.i("HomeFragment","${response.message}")
                }
            }
        }

        adapter.setOnItemClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(it)
            findNavController().navigate(action)
        }

        binding.homeRecyclerView.adapter = adapter

        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            group.isSingleSelection = true
            val chipid = group.checkedChipId
            val category = category2[chipid].category

            viewModel.getCategoryProducts(category)
        }

        binding.homeCart.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_cartFragment)
        }

        binding.homeProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

    }

    class HomeViewModel constructor(
        private val app : Application,
        private val productUseCase: ProductUseCase
    ) : AndroidViewModel(app){

        val products : MutableLiveData<LoginFragment.Resource<Shop>> = MutableLiveData()
        val categories : MutableLiveData<LoginFragment.Resource<Category>> = MutableLiveData()

        fun getAllCategories() = viewModelScope.launch(Dispatchers.IO) {
            categories.postValue(LoginFragment.Resource.Loading())
            try {
                if (isNetworkAvailable(app)){
                    val apiResult = productUseCase.getAllCategories()
                    categories.postValue(apiResult)
                }else{
                    categories.postValue(LoginFragment.Resource.Error(message = "Internet not available"))
                }
            }catch (e : Exception){
                categories.postValue(LoginFragment.Resource.Error(message = "${e.localizedMessage} ?: Unknown Error"))
            }
        }

        fun getAllProducts() = viewModelScope.launch(Dispatchers.IO) {
            products.postValue(LoginFragment.Resource.Loading())
            try {
                if (isNetworkAvailable(app)){
                    val apiResult = productUseCase.getAllProducts()
                    products.postValue(apiResult)
                }else{
                    products.postValue(LoginFragment.Resource.Error(message = "Internet not available"))
                }
            }catch (e : Exception){
                products.postValue(LoginFragment.Resource.Error(message = e.localizedMessage ?: "Unknown Error"))
            }
        }

        fun getCategoryProducts(category : String) = viewModelScope.launch(Dispatchers.IO) {
            if(category != "All"){
                products.postValue(LoginFragment.Resource.Loading())
                try {
                    if (isNetworkAvailable(app)){
                        val apiResult = productUseCase.getCategoryProducts(category)
                        products.postValue(apiResult)
                    }else{
                        products.postValue(LoginFragment.Resource.Error(message = "Internet not available"))
                    }
                }catch (e : Exception){
                    products.postValue(LoginFragment.Resource.Error(message = e.localizedMessage ?: "Unknown Error"))
                }
            }else{
                getAllProducts()
            }
        }

    }

    inner class HomeAdapter : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

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

        fun setOnItemClickListener(listener : (ShopItem)-> Unit){
            onItemClickListener = listener
        }

        inner class HomeViewHolder(private val binding : SingleItemBinding) : RecyclerView.ViewHolder(binding.root) {

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

            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
            val binding = SingleItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return HomeViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
            val shopItem = differ.currentList[position]
            holder.bindData(shopItem)
        }

        override fun getItemCount() =  differ.currentList.size



    }

    data class Category2(
        var id : Int,
        var category : String
    )

//    data class ShopItem(
//        @SerializedName("category")
//        val category: String,
//        @SerializedName("description")
//        val description: String,
//        @SerializedName("id")
//        val id: Int,
//        @SerializedName("image")
//        val image: String,
//        @SerializedName("price")
//        val price: Double,
//        @SerializedName("rating")
//        val rating: Rating,
//        @SerializedName("title")
//        val title: String
//    ) : Serializable

    inner class ProductUseCase constructor(
        private val repository: LoginFragment.ShopRepository
    ) {

        suspend fun getAllProducts() : LoginFragment.Resource<Shop> {
            return repository.getAllProducts()
        }

        suspend fun getAllCategories() : LoginFragment.Resource<Category> {
            return repository.getAllCategories()
        }

        suspend fun getCategoryProducts(category : String) : LoginFragment.Resource<Shop> {
            return repository.getCategoryProducts(category)
        }

    }

    inner class Shop : ArrayList<ShopItem>()

    class Category : ArrayList<String>()

    data class Rating(
        @SerializedName("count")
        val count: Int,
        @SerializedName("rate")
        val rate: Double
    )



}