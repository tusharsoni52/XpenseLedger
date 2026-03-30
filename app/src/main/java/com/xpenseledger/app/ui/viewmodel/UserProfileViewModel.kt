package com.xpenseledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.xpenseledger.app.security.profile.UserProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class UserProfile(
    val name:   String = "",
    val age:    String = "",
    val gender: String = ""
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val manager: UserProfileManager
) : ViewModel() {

    private val _profile = MutableStateFlow(
        UserProfile(
            name   = manager.getName(),
            age    = manager.getAge(),
            gender = manager.getGender()
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    /** true once a name has been saved (used to greet user in header) */
    val hasProfile: Boolean get() = manager.hasProfile()

    fun saveProfile(name: String, age: String, gender: String) {
        manager.saveProfile(name, age, gender)
        _profile.update { UserProfile(name, age, gender) }
    }

    fun clearProfile() {
        manager.clearProfile()
        _profile.update { UserProfile() }
    }
}

