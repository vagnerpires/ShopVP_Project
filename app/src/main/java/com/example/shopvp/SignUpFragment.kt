package com.example.shopvp

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
import com.example.shopvp.databinding.FragmentSignUpBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SignUpFragment : Fragment() {

    lateinit var viewModels : RegisterViewModel

    private lateinit var binding : FragmentSignUpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSignUpBinding.bind(view)

        binding.registerButton.setOnClickListener {

            val username = binding.registerUsername.editableText.toString()
            val password = binding.registerPassword.editableText.toString()

            val result = validateLoginRequest(username, password)

            if (result.successful){
                binding.registerButton.isEnabled = false

                viewModels.registerUser(username, password)

                viewModels.successful.observe(viewLifecycleOwner){successful->
                    if (successful == true){
                        binding.registerButton.isEnabled = true
                        findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                        viewModels.navigated()
                    }else if (successful == false){
                        binding.registerButton.isEnabled = true
                        Snackbar.make(binding.registerButton,"${viewModels.error.value}",Snackbar.LENGTH_SHORT).show()
                        viewModels.navigated()
                    }
                }

            }else{
                Snackbar.make(binding.registerButton,"${result.error}",Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.registerSignin.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }

    }

    private fun validateLoginRequest(username: String, password: String) : LoginFragment.ValidationResult {
        if (username.isBlank() && password.isBlank()) return LoginFragment.ValidationResult(
            false,
            "Username and password cannot be blank"
        )
        if (username.isBlank()) return LoginFragment.ValidationResult(
            false,
            "Username cannot be blank"
        )
        if (password.isBlank()) return LoginFragment.ValidationResult(
            false,
            "Password cannot be blank"
        )
        return LoginFragment.ValidationResult(true)
    }

    inner class RegisterViewModel constructor(
        private val authUseCase: LoginFragment.AuthUseCase,
        private val sharedPrefUtil: LoginFragment.SharedPreference,
    ) : ViewModel() {

        val successful : MutableLiveData<Boolean?> = MutableLiveData()
        val error : MutableLiveData<String?> = MutableLiveData()

        private fun saveUserAccessToken(token: String) = sharedPrefUtil.saveUserAccessToken(token)

        fun registerUser(username : String, password: String){
            authUseCase.registerUser(username, password).onEach { result ->
                when(result){
                    is LoginFragment.Resource.Loading -> {
                        Log.i("LoginViewModel","I dey here, Loading")
                    }
                    is LoginFragment.Resource.Error -> {
                        error.postValue("${result.message}")
                        successful.postValue(false)
                        Log.i("LoginViewModel","I dey here, Error ${result.message}")
                    }
                    is LoginFragment.Resource.Success -> {
                        successful.postValue(true)
                        saveUserAccessToken(username)
                        Log.i("LoginViewModel","I dey here, Success ${result.data.toString()}")
                    }
                }
            }.launchIn(viewModelScope)
        }

        fun navigated(){
            successful.postValue(null)
            error.postValue(null)
        }

    }

}