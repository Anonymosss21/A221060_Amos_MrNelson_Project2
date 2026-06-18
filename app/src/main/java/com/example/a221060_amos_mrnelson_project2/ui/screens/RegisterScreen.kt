package com.example.a221060_amos_mrnelson_project2.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.a221060_amos_mrnelson_project2.data.User
import com.example.a221060_amos_mrnelson_project2.ui.components.AppLogo
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityHigh
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityLow
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityMedium
import com.example.a221060_amos_mrnelson_project2.util.pickDate
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: MainViewModel) {
    var id by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("Male") }
    var dob by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirmPass by rememberSaveable { mutableStateOf("") }
    var profilePicUri by rememberSaveable { mutableStateOf("") }
    
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPassVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val context = LocalContext.current
    
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { profilePicUri = it.toString() }
    }

    // Password Strength Logic
    val passStrength = remember(pass) {
        if (pass.isEmpty()) 0f
        else if (pass.length < 8) 0.3f // Weak
        else {
            val hasLetter = pass.any { it.isLetter() }
            val hasDigit = pass.any { it.isDigit() }
            val hasSymbol = pass.any { "@#$*".contains(it) }
            val typesCount = listOf(hasLetter, hasDigit, hasSymbol).count { it }
            when {
                typesCount == 3 && pass.length in 8..12 -> 1f // Strong
                typesCount >= 2 && pass.length in 8..12 -> 0.6f // Medium
                else -> 0.3f // Weak
            }
        }
    }

    val strengthColor = when {
        passStrength > 0.7f -> PriorityLow
        passStrength > 0.4f -> PriorityMedium
        else -> PriorityHigh
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Register Account", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Profile Picture Picker
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { photoLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profilePicUri.isNotEmpty()) {
                    val bitmap = remember(profilePicUri) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(Uri.parse(profilePicUri))
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Select Profile Picture", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isIdValid = id.matches(Regex("^[AK][0-9]{6}$"))
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it.uppercase() },
                        label = { Text("User ID") },
                        placeholder = { Text("e.g. A123456") },
                        supportingText = {
                            if (id.isEmpty()) Text("A for Student, K for Lecturer")
                            else if (isIdValid) Text("ID Format Valid", color = PriorityLow)
                            else Text("Format: A/K + 6 digits", color = PriorityHigh)
                        },
                        trailingIcon = {
                            if (id.isNotEmpty()) {
                                if (isIdValid) Icon(Icons.Default.CheckCircle, "Valid", tint = PriorityLow)
                                else Icon(Icons.Default.Warning, "Invalid", tint = PriorityHigh)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = id.isNotEmpty() && !isIdValid,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("example@email.com") },
                        supportingText = {
                            if (email.isNotEmpty()) {
                                if (isEmailValid) Text("Email Format Valid", color = PriorityLow)
                                else Text("Invalid Email Format", color = PriorityHigh)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = email.isNotEmpty() && !isEmailValid,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column {
                        Text("Gender", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                            Text("Male")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                            Text("Female")
                        }
                    }

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { },
                        label = { Text("Date of Birth") },
                        modifier = Modifier.fillMaxWidth().clickable { pickDate(context) { dob = it } },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        placeholder = { Text("8-12 chars: a-z, 0-9, @#$*") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(imageVector = if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (pass.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(progress = passStrength, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = strengthColor)
                            Text(if (passStrength > 0.7f) "Strong Password" else if (passStrength > 0.4f) "Medium Password" else "Weak Password", style = MaterialTheme.typography.labelSmall, color = strengthColor)
                        }
                    }

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = {
                            if (confirmPass.isNotEmpty()) {
                                if (confirmPass != pass) Text("Passwords do not match", color = PriorityHigh)
                                else Text("Passwords match", color = PriorityLow)
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                                Icon(imageVector = if (confirmPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (error.isNotEmpty()) Text(error, color = PriorityHigh, modifier = Modifier.padding(top = 8.dp))

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val hasLetter = pass.any { it.isLetter() }
                            val hasDigit = pass.any { it.isDigit() }
                            val hasSymbol = pass.any { "@#$*".contains(it) }
                            val isComplex = (listOf(hasLetter, hasDigit, hasSymbol).count { it } >= 2)

                            if (id.isEmpty() || username.isEmpty() || fullName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                                error = "All fields are required"
                            } else if (viewModel.usersList.any { it.id == id }) {
                                error = "The Account already exists."
                            } else if (pass != confirmPass) {
                                error = "Passwords do not match"
                            } else if (pass.length !in 8..12) {
                                error = "Password must be 8-12 characters"
                            } else if (!isComplex) {
                                error = "Password too simple (need mix of types)"
                            } else if (!isIdValid) {
                                error = "Invalid ID Format (A/K + 6 digits)"
                            } else {
                                val type = if (id.startsWith("A")) "Student" else "Lecturer"
                                val newUser = User(id, username, fullName, email, gender, dob, type, pass, profilePicUri)
                                viewModel.registerUser(
                                    user = newUser,
                                    onSuccess = {
                                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onFailure = { error = it }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Register Account") }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
