package com.example.shopvp

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.example.shopvp.databinding.FragmentLoginBinding
import com.example.shopvp.model.ShopItem
import com.example.shopvp.model.Utils.validateLoginRequest
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okio.IOException
import retrofit2.HttpException
import java.io.Serializable

class LoginFragment : Fragment() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentLoginBinding.bind(view)

        binding.loginButton.setOnClickListener {

            val username = binding.loginUsername.editableText.toString()
            val password = binding.loginPassword.editableText.toString()

            val result = validateLoginRequest(username, password)

            if (result.successful){
                binding.loginButton.isEnabled = false

                viewModel.loginUser(username, password)

                viewModel.successful.observe(viewLifecycleOwner){successful ->
                    if (successful == true){
                        binding.loginButton.isEnabled = true
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                        viewModel.navigated()
                    }else if(successful == false){
                        binding.loginButton.isEnabled = true
                        Snackbar.make(binding.loginButton,"${viewModel.error.value}",Snackbar.LENGTH_SHORT).show()
                        viewModel.navigated()
                    }
                }
            }
            else{
                Snackbar.make(binding.loginButton,"${result.error}",Snackbar.LENGTH_SHORT).show()
            }

        }


        binding.loginSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

    }

    private class LoginViewModel constructor(
        private val authUseCase: AuthUseCase,
        private val sharedPrefUtil: SharedPreference,
    ) : ViewModel(){

        val successful : MutableLiveData<Boolean?> = MutableLiveData()
        val error : MutableLiveData<String?> = MutableLiveData()

        private fun saveUserAccessToken(token: String) = sharedPrefUtil.saveUserAccessToken(token)

        fun loginUser(username: String, password: String){
            authUseCase.loginUser(username, password).onEach { result ->
                when(result) {
                    is Resource.Loading -> {
                        Log.i("LoginViewModel","I dey here, Loading")
                    }
                    is Resource.Error -> {
                        error.postValue("${result.message}")
                        successful.postValue(false)
                        Log.i("LoginViewModel","I dey here, Error ${result.message}")
                    }
                    is Resource.Success -> {
                        successful.postValue(true)
                        saveUserAccessToken("${result.data?.token}")
                        Log.i("LoginViewModel","I dey here, Success ${result.data?.token}")
                    }
                }
            }.launchIn(viewModelScope)
        }

        fun navigated(){
            successful.postValue(null)
            error.postValue(null)
        }

    }

    class AuthUseCase constructor(
        private val repository: ShopRepository
    ) {

        fun loginUser(username : String, password : String) : Flow<Resource<Auth>> = flow{
            emit(Resource.Loading())
            try {
                val login = Login(username, password)
                val response = repository.loginUser(login)
                Log.i("AuthUseCase","I dey here, ${response.data?.token}")
                emit (response)
            }catch (e : HttpException){
                Log.i("AuthUseCase","${e.localizedMessage}")
                emit (Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
            }catch (e : IOException){
                Log.i("AuthUseCase","${e.localizedMessage}")
                emit (Resource.Error("Couldn't reach server. Check your internet connection."))
            }
        }

        fun registerUser(username: String,password: String) : Flow<Resource<User>> = flow {
            emit(Resource.Loading())
            val user = User(address = Address(city = "new city", geolocation = Geolocation("10","10"), number = 3, street = "new street", zipcode = "new zipcode"), email = "new email", id = 10, name = Name(firstname = "new firstname", lastname = "new lastname"),password = password, phone = "new phone",username = username, v = 1)
            try {
                val response = repository.registerUser(user)
                emit(response)
            }catch (e : HttpException){
                Log.i("AuthUseCase","${e.localizedMessage}")
                emit (Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
            }
            catch (e : IOException){
                Log.i("AuthUseCase","${e.localizedMessage}")
                emit (Resource.Error("Couldn't reach server. Check your internet connection."))
            }

        }

    }

    class SharedPreference constructor(
        private val sharedPreferences : SharedPreferences
    ) {

        fun isFirstAppLaunch(): Boolean {
            return sharedPreferences.getBoolean(Constants.IS_FIRST_APP_LAUNCH, true)
        }

        fun saveFirstAppLaunch(value: Boolean) {
            sharedPreferences.edit().putBoolean(Constants.IS_FIRST_APP_LAUNCH, value).apply()
        }

        private fun userIsLoggedIn() : Boolean {
            val token = sharedPreferences.getString(Constants.USER_IS_LOGGED_IN, null)
            return token != null
        }

        fun saveUserAccessToken(token: String) {
            sharedPreferences.edit().putString(Constants.USER_IS_LOGGED_IN, token).apply()
        }

        fun deleteAccessToken(): Boolean {
            sharedPreferences.edit().remove(Constants.USER_IS_LOGGED_IN).apply()
            return userIsLoggedIn()
        }
    }

    sealed class Resource<T> (
        val data : T? = null,
        val message : String? = null
    ){
        class Success<T>(data: T) : Resource<T>(data)
        class Loading<T>(data : T? = null) : Resource<T>(data)
        class Error<T>(message : String, data: T? = null) : Resource<T>(data,message)
    }

    data class ValidationResult(
        val successful: Boolean,
        val error: String? = null
    )

    interface ShopRepository {

        suspend fun getAllProducts() : Resource<HomeFragment.Shop>
        suspend fun getAllCategories() : Resource<HomeFragment.Category>
        suspend fun getCategoryProducts(category : String) : Resource<HomeFragment.Shop>
        suspend fun getUser(id : Int) : Resource<User>
        suspend fun loginUser(login: Login) : Resource<Auth>
        suspend fun registerUser(user : User) : Resource<User>

        suspend fun addToCartItems(cartItem2: ProductDetailFragment.CartItem2)
        fun getCartItems() : Flow<List<ProductDetailFragment.CartItem2>>
        suspend fun updateCartItems(cartItem2: ProductDetailFragment.CartItem2)
        suspend fun deleteCartItems(cartItem2: ProductDetailFragment.CartItem2)
        suspend fun clearCart()

        suspend fun addToWishlist(shopItem: ShopItem)
        fun getWishlistItems() : Flow<List<ShopItem>>
        suspend fun deleteWishlistItem(shopItem: ShopItem)
        suspend fun clearWishlist()

    }

    data class Auth(
        val token: String,
    )

    data class Login(
        val username: String,
        val password: String
    )

    data class User(
        @SerializedName("address")
        val address: Address,
        @SerializedName("email")
        val email: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("name")
        val name: Name,
        @SerializedName("password")
        val password: String,
        @SerializedName("phone")
        val phone: String,
        @SerializedName("username")
        val username: String,
        @SerializedName("__v")
        val v: Int
    ) : Serializable

    data class Address(
        @SerializedName("city")
        val city: String,
        @SerializedName("geolocation")
        val geolocation: Geolocation,
        @SerializedName("number")
        val number: Int,
        @SerializedName("street")
        val street: String,
        @SerializedName("zipcode")
        val zipcode: String
    ) : Serializable

    data class Name(
        @SerializedName("firstname")
        val firstname: String,
        @SerializedName("lastname")
        val lastname: String
    ) : Serializable

    data class Geolocation(
        @SerializedName("lat")
        val lat: String,
        @SerializedName("long")
        val long: String
    ) : Serializable

    object Constants {
        const val USER_IS_LOGGED_IN = "USER_IS_LOGGED_IN"
        const val IS_FIRST_APP_LAUNCH = "IS_FIRST_APP_LAUNCH"
    }

}