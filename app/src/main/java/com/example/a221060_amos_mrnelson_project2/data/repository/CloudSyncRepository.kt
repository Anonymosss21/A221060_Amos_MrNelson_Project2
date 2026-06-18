package com.example.a221060_amos_mrnelson_project2.data.repository

import android.content.Context
import android.net.Uri
import com.example.a221060_amos_mrnelson_project2.data.Task
import com.example.a221060_amos_mrnelson_project2.data.User
import com.example.a221060_amos_mrnelson_project2.data.local.DailyMetricsEntity
import com.example.a221060_amos_mrnelson_project2.data.remote.ImgBBResponse
import com.example.a221060_amos_mrnelson_project2.data.remote.ImgBBRetrofitClient
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CloudSyncRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val IMGBB_API_KEY = "69e71ec143891461944da23348612140" // Replace with actual key if needed

    fun registerUserAccount(context: Context, user: User, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        if (user.profilePicUri.isNotEmpty() && !user.profilePicUri.startsWith("http")) {
            uploadToImgBB(context, user.profilePicUri, { downloadUrl ->
                val updatedUser = user.copy(profilePicUri = downloadUrl)
                saveUserToFirestore(updatedUser, { onSuccess(updatedUser) }, onFailure)
            }, { error ->
                // Fallback: If image upload fails (e.g. DNS error), save user without profile pic
                android.util.Log.e("CloudSync", "ImgBB upload failed, falling back: ${error.message}")
                val fallbackUser = user.copy(profilePicUri = "")
                saveUserToFirestore(fallbackUser, { onSuccess(fallbackUser) }, onFailure)
            })
        } else {
            saveUserToFirestore(user, { onSuccess(user) }, onFailure)
        }
    }

    private fun uploadToImgBB(context: Context, uriString: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            val uri = Uri.parse(uriString)
            val file = getFileFromUri(context, uri)
            if (file == null) {
                onFailure(Exception("Failed to get file from URI"))
                return
            }

            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            ImgBBRetrofitClient.instance.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
                override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess(response.body()!!.data.url)
                    } else {
                        onFailure(Exception("ImgBB Upload Failed: ${response.message()}"))
                    }
                }

                override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                    onFailure(Exception(t))
                }
            })
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_image_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun saveUserToFirestore(user: User, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userMap = hashMapOf(
            "id" to user.id,
            "username" to user.username,
            "fullName" to user.fullName,
            "email" to user.email,
            "gender" to user.gender,
            "dob" to user.dob,
            "type" to user.type,
            "pass" to user.pass,
            "profilePicUri" to user.profilePicUri
        )
        firestore.collection("user_accounts")
            .document(user.id)
            .set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getAllAccounts(onSuccess: (List<User>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("user_accounts")
            .get()
            .addOnSuccessListener { result ->
                val users = result.map { doc ->
                    User(
                        id = doc.getString("id") ?: "",
                        username = doc.getString("username") ?: "",
                        fullName = doc.getString("fullName") ?: "",
                        email = doc.getString("email") ?: "",
                        gender = doc.getString("gender") ?: "",
                        dob = doc.getString("dob") ?: "",
                        type = doc.getString("type") ?: "",
                        pass = doc.getString("pass") ?: "",
                        profilePicUri = doc.getString("profilePicUri") ?: ""
                    )
                }
                onSuccess(users)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun saveTaskToCloud(userId: String, task: Task, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val taskMap = hashMapOf(
            "id" to task.id,
            "title" to task.title,
            "description" to task.description,
            "location" to task.location,
            "startTime" to task.startTime,
            "endTime" to task.endTime,
            "reminder" to task.reminder,
            "priority" to task.priority,
            "isCompleted" to task.isCompleted,
            "estimatedHours" to task.estimatedHours
        )
        firestore.collection("user_accounts")
            .document(userId)
            .collection("tasks")
            .document(task.id)
            .set(taskMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun removeTaskFromCloud(userId: String, taskId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("user_accounts")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserTasks(userId: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("user_accounts")
            .document(userId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                val tasks = result.map { doc ->
                    Task(
                        id = doc.getString("id") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        reminder = doc.getString("reminder") ?: "Never",
                        priority = doc.getString("priority") ?: "Low",
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        estimatedHours = (doc.get("estimatedHours") as? Number)?.toFloat() ?: 1.5f
                    )
                }
                onSuccess(tasks)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun syncDailyMetricsToCloud(userId: String, metrics: List<DailyMetricsEntity>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val batch = firestore.batch()
        metrics.forEach { m ->
            val mDoc = firestore.collection("user_accounts")
                .document(userId)
                .collection("metrics")
                .document(m.dateIsoString)
            
            val mMap = hashMapOf(
                "date" to m.dateIsoString,
                "burnoutScore" to m.burnoutScore,
                "screenTime" to m.screenTimeMillis,
                "completionRate" to m.completionRate,
                "weightedRate" to m.weightedCompletionRate
            )
            batch.set(mDoc, mMap)
        }
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteUserAccountAndData(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("user_accounts")
            .document(userId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
