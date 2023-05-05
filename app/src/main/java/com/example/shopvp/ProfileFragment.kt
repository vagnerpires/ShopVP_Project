package com.example.shopvp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.example.shopvp.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileFragment: Fragment() {

    lateinit var viewModel : ProfileViewModel

    private lateinit var binding : FragmentProfileBinding

    private var user : LoginFragment.User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        viewModel.getUser(1)

        viewModel.user.observe(viewLifecycleOwner){result ->
            when(result){
                is LoginFragment.Resource.Loading -> {
                    Log.i("ProfileFragment","Loading...")
                }
                is LoginFragment.Resource.Success ->{
                    user = result.data
                    binding.profileName.text = "${result.data?.name?.firstname} ${result.data?.name?.lastname}"
                    binding.profileEmail.text = "${result.data?.email}"
                }
                is LoginFragment.Resource.Error -> {
                    Log.i("ProfileFragment","Error ${result.message}")
                }
            }
        }

        binding.profileBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.profileNotifications.setOnClickListener {
            Snackbar.make(binding.profileNotifications,"Coming soon...", Snackbar.LENGTH_SHORT).show()
        }

        binding.profileWishlist.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_wishlistFragment)
        }

        binding.profileTerms.setOnClickListener {
            Snackbar.make(binding.profileNotifications,"Coming soon...", Snackbar.LENGTH_SHORT).show()
        }

        binding.profilePrivacy.setOnClickListener {
            Snackbar.make(binding.profileNotifications,"Coming soon...", Snackbar.LENGTH_SHORT).show()
        }

        binding.profileReportBug.setOnClickListener {
            Snackbar.make(binding.profileNotifications,"Coming soon...", Snackbar.LENGTH_SHORT).show()
        }

    }

    inner class ProfileViewModel constructor(
        private val profileUseCase: ProfileUseCase,
    ) : ViewModel() {

        val user : MutableLiveData<LoginFragment.Resource<LoginFragment.User>> = MutableLiveData()

        fun getUser(id : Int) = viewModelScope.launch(Dispatchers.IO) {
            user.postValue(LoginFragment.Resource.Loading())
            try {
                val apiResult = profileUseCase.getUser(id)
                user.postValue(apiResult)
            }catch (e : Exception){
                user.postValue(LoginFragment.Resource.Error(message = e.localizedMessage ?: "Unknown Error"))
            }
        }


    }

    inner class ProfileUseCase constructor(private val repository: LoginFragment.ShopRepository) {

        suspend fun getUser(id : Int) : LoginFragment.Resource<LoginFragment.User> {
            return repository.getUser(id)
        }

    }

}