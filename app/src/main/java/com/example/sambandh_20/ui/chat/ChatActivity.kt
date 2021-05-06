package com.example.sambandh_20.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sambandh_20.R
import com.example.sambandh_20.model.ChatMessage
import com.example.sambandh_20.model.User
import com.example.sambandh_20.ui.chat.ChatOverviewFragment.Companion.currentUser
import com.example.sambandh_20.ui.matches.MatchesOverviewActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class ChatActivity : AppCompatActivity() {

    companion object{
        var currentUser: User? = null
    }
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null

    var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        rv_chat_log.adapter = adapter
        toUser = intent.getParcelableExtra<User>(MatchesOverviewActivity.USER_KEY)
        supportActionBar?.title = toUser?.displayName
        fetchCurrentUser()
        ListenForMessages()
        btn_send_chat_log.setOnClickListener {
            if (selectedPhotoUri != null) {
                upLoadImageToFirebaseStorage()
            } else {
                performSendMessage("")
            }
        }

        btn_send_media.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type ="image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            iv_send_media.setImageBitmap(bitmap)
        }
    }

    private fun upLoadImageToFirebaseStorage(){
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        performSendMessage(it.toString())
                    }
                }
    }

    private fun ListenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    if (chatMessage.text.isNotBlank()) {
                        if (chatMessage.fromId == currentUser?.uid) {
                            val currentUser = currentUser ?: return
                            adapter.add(ChatFromItem(chatMessage.text, currentUser))
                        } else {
                            adapter.add(ChatToItem(chatMessage.text, toUser!!))
                        }
                    } else if (chatMessage.mediaLink.isNotBlank()) {
                        if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                            val currentUser = currentUser ?: return
                            adapter.add(ChatImageFromItem(chatMessage.mediaLink, currentUser))
                        } else {
                            adapter.add(ChatImageToItem(chatMessage.mediaLink, toUser!!))
                        }
                    }
                }
                rv_chat_log.scrollToPosition(adapter.itemCount -1)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun performSendMessage(imageUrl: String) {
        val text = et_chat_log.text.toString()

        if (text.isNullOrBlank() && imageUrl.isNullOrBlank()) return

        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(MatchesOverviewActivity.USER_KEY)
        val toId = user!!.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        if (fromId == null) return

        val chatMessage =
                ChatMessage(reference.key!!, text, fromId, toId, imageUrl, System.currentTimeMillis() / 1000)
        reference.setValue(chatMessage)
                .addOnSuccessListener {
                    et_chat_log.text.clear()
                    rv_chat_log.scrollToPosition(adapter.itemCount -1)
                }
        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)
        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)

        iv_send_media.setImageBitmap(null)
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)

            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}

