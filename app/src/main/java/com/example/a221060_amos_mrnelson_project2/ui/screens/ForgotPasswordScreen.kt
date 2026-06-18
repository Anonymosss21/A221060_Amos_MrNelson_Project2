package com.example.a221060_amos_mrnelson_project2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController, viewModel: MainViewModel) {
    var userId by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<User?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo(size = 90)
            Spacer(modifier = Modifier.height(32.dp))

            // Form inside a Card matching Register Screen style
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!isVerified) {
                        Text(
                            "Account Verification",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = userId,
                            onValueChange = { userId = it.uppercase() },
                            label = { Text("User ID") },
                            placeholder = { Text("e.g. A221060") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )

                        if (error.isNotEmpty()) {
                            Text(error, color = PriorityHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val user = viewModel.usersList.find { it.id == userId }
                                if (user != null) {
                                    // Step 1: Verification Successful
                                    isVerified = true
                                    error = ""
                                    
                                    // Step 2: Send Firebase Reset Email automatically
                                    com.google.firebase.auth.FirebaseAuth.getInstance()
                                        .sendPasswordResetEmail(user.email)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                android.widget.Toast.makeText(context, "Reset link sent to ${user.email}", android.widget.Toast.LENGTH_LONG).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Error sending email: ${task.exception?.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    error = "User ID not found in database"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Verify ID")
                        }
                    } else {
                        // Success View after verification
                        Text(
                            "Reset Email Sent",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            "An official password reset link has been dispatched to the registered email associated with User ID: $userId.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Text(
                            "Please check your inbox (and spam folder) to complete the process.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Return to Login")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login button OUTSIDE the form
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("Back to Login", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val name = parts[0]
    val domain = parts[1]
    if (name.length <= 2) return "***@$domain"
    return name.substring(0, 2) + "****@" + domain
}
