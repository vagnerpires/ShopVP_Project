package com.example.shopvp.model

import com.example.shopvp.LoginFragment

object Utils {

    fun formatPrice(price : String): String {
        return String.format("%.2f", price.toDouble())
    }

    fun validateLoginRequest(username: String,password: String) : LoginFragment.ValidationResult {
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

}