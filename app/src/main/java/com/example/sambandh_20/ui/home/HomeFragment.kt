package com.example.sambandh_20.ui.home


import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sambandh_20.R

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUserImage()
        }




    private fun setUserImage() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        var profileImageUrl: String? = ""

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                profileImageUrl = snapshot.child("profileImageUrL").getValue(String::class.java)
                if (profileImageUrl!!.isNotEmpty()){
                    Picasso.get().load(profileImageUrl).into(fragment_home_btn_profile)
                }
                else {
                    fragment_home_btn_profile.setImageResource(R.drawable.henk)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                sendToast("Something went wrong")
            }
        })
    }

    //sends short toast message to the user
    fun sendToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    }


