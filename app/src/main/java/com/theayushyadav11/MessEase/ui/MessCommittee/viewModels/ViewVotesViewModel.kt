package com.theayushyadav11.MessEase.ui.MessCommittee.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.theayushyadav11.MessEase.Models.OptionSelected
import com.theayushyadav11.MessEase.Models.Poll
import com.theayushyadav11.MessEase.utils.Constants.Companion.POLLS
import com.theayushyadav11.MessEase.utils.Constants.Companion.POLL_RESULT
import com.theayushyadav11.MessEase.utils.Constants.Companion.SELECTED_OPTION
import com.theayushyadav11.MessEase.utils.Constants.Companion.TAG
import com.theayushyadav11.MessEase.utils.Constants.Companion.USERS
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference

class ViewVotesViewModel : ViewModel() {
    fun getPoll(id:String,onResult:(Poll)->Unit)
    {
        firestoreReference.collection(POLLS).whereEqualTo("id",id).get().addOnSuccessListener {
            onResult(it.documents[0].toObject(Poll::class.java)!!)
        }
    }
    fun getoptionDetails(pollId:String, option: String, onSuccess:(Int,List<OptionSelected>)->Unit, onFailure:(String)->Unit)
    {
       firestoreReference.collection(POLL_RESULT).document(pollId).collection(USERS).whereEqualTo(
           SELECTED_OPTION,option).addSnapshotListener{
           value,error->
              if(error!=null)
              {
                onFailure(error.message!!)
                return@addSnapshotListener
              }
           val list= mutableListOf<OptionSelected>()
            for(document in value!!.documents)
            {
                val optionSelected=document.toObject(OptionSelected::class.java)
                if(optionSelected!=null)
                {
                    list.add(optionSelected)
                }
            }
           print(list)
           list.sortByDescending { it.comp }
onSuccess(list.size,list)
       }
    }
}