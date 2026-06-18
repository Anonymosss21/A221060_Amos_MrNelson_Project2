package com.example.a221060_amos_mrnelson_project2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.a221060_amos_mrnelson_project2.ui.components.AppLogo
import com.example.a221060_amos_mrnelson_project2.ui.components.WelcomeBanner
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityHigh
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: MainViewModel) {
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showFirstTimeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(viewModel.isUsersLoaded.value) {
        if (viewModel.isUsersLoaded.value && viewModel.usersList.isEmpty()) {
            showFirstTimeDialog = true
        }
    }

    if (showFirstTimeDialog) {
        AlertDialog(
            onDismissRequest = { showFirstTimeDialog = false },
            icon = { Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) },
            title = { Text("Welcome!", fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) },
            text = { Text("Protect your well-being. Please create an account to start.", textAlign = TextAlign.Center) },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { showFirstTimeDialog = false }) { Text("Dismiss") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { showFirstTimeDialog = false; navController.navigate("register") }, shape = RoundedCornerShape(12.dp)) { Text("Register Now") }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AppLogo(size = 120)
        Spacer(modifier = Modifier.height(32.dp))
        WelcomeBanner()
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = id,
            onValueChange = { id = it.uppercase() },
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth(),
            isError = error.isNotEmpty(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            isError = error.isNotEmpty(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        if (error.isNotEmpty()) Text(error, color = PriorityHigh, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (id.isEmpty() || password.isEmpty()) {
                    error = "Please fill in all fields"
                } else {
                    val user = viewModel.usersList.find { it.id == id && it.pass == password }
                    if (user != null) {
                        viewModel.currentUser.value = user
                        viewModel.loadUserTasks()
                        Toast.makeText(context, "Welcome ${user.username}", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") { popUpTo(0) { inclusive = true } }
                    } else error = "Invalid ID or Password"
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Login") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Don't have an account? Register", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { navController.navigate("register") }, fontWeight = FontWeight.SemiBold)
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Forgot Password?",
            color = PriorityHigh, // Red color
            modifier = Modifier.clickable {
                navController.navigate("forgot")
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
