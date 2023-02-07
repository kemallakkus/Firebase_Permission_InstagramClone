package com.kemalakkus.kotlininstagram.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.kemalakkus.kotlininstagram.databinding.ActivityUploadBinding
import java.util.UUID

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedPicture : Uri? = null
    private lateinit var auth  : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore
    private lateinit var storage : FirebaseStorage


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        auth = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage

    }

    fun upload (view : View) {

        val uuid = UUID.randomUUID() //fotoğraflara random isim vermek için gerekli
        val imageName = "$uuid.jpg"

        val reference = storage.reference
        val imageReference = reference.child("images").child(imageName)

        if (selectedPicture != null){
            imageReference.putFile(selectedPicture!!).addOnSuccessListener {
                //download url -> firestore

                //url'yi çekip kaydetcez, comment'i kaydetcez(comment.binding..., güncel kullanıcı email'i kaydetcez(authentication'dan alırız,tarih?)

                //yükledikten sonra url yi almamız lazım

                val uploadPictureReference = storage.reference.child("images").child(imageName)
                uploadPictureReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()

                    if (auth.currentUser != null){
                        val postMap = hashMapOf<String, Any>()
                        postMap.put("downloadUrl", downloadUrl) //url yi aldık
                        postMap.put("userEmail", auth.currentUser!!.email!!) // kullanıcı email ini aldık
                        postMap.put("comment", binding.commentText.text.toString()) // yorumu aldık
                        postMap.put("date", Timestamp.now()) // güncel zamanı aldık

                        firestore.collection("Posts").add(postMap).addOnSuccessListener {

                            finish()

                        }.addOnFailureListener {
                            Toast.makeText(this@UploadActivity,it.localizedMessage, Toast.LENGTH_LONG).show()
                        }


                    }


                }

            }.addOnFailureListener {
                Toast.makeText(this,it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }


    }

    fun selectImage (view: View) {


        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            }else {
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else {
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //start activity for result
            activityResultLauncher.launch(intentToGallery)
        }

    }


    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->

            if(result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null){
                   selectedPicture = intentFromResult.data
                    selectedPicture?.let {
                        binding.imageView.setImageURI(it)
                    }
                }
            }



        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->

            if (result){
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                //permission denied
                Toast.makeText(this@UploadActivity,"Permission needed!!",Toast.LENGTH_LONG).show()
            }

        }

    }

}