package com.example.gymbuddy_tabatatimer.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser

class MainViewModel : ViewModel() {

    private val _user= MutableLiveData<FirebaseUser>(null)
    val user: LiveData<FirebaseUser> = _user
    fun setUser(user:FirebaseUser?) {_user.value=user}

    private val _rewardGranted = MutableLiveData(false)
    var rewardGranted: LiveData<Boolean> = _rewardGranted
    fun setRewardGranted(rewardGranted: Boolean){
        _rewardGranted.value=rewardGranted
    }

}