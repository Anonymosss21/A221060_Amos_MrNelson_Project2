package com.example.a221060_amos_mrnelson_project2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityHigh
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityLow
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityMedium
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel

@Composable
fun SettingsScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val systemTheme = isSystemInDarkTheme()
    val isDark = viewModel.userThemePreference.value ?: systemTheme

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings & Profile", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Card
        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("profile") }) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val profilePicUri = viewModel.currentUser.value?.profilePicUri ?: ""
                val bitmap = remember(profilePicUri) {
                    if (profilePicUri.isNotEmpty()) {
                        try {
                            if (profilePicUri.startsWith("http")) {
                                null // We'll handle network images later or use a loader
                            } else {
                                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(profilePicUri))
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) { null }
                    } else null
                }

                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(viewModel.currentUser.value?.username?.take(1)?.uppercase() ?: "U", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(viewModel.currentUser.value?.username ?: "User", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("ID: ${viewModel.currentUser.value?.id ?: "ID"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("PREFERENCES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark Mode", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = isDark, onCheckedChange = { viewModel.userThemePreference.value = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Push Notifications", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Receive task reminders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = viewModel.pushNotificationsEnabled.value, onCheckedChange = { viewModel.pushNotificationsEnabled.value = it })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("DATA & PRIVACY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { navController.navigate("activity_log") }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("View Activity Log", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
            viewModel.clearAppData()
            Toast.makeText(context, "Account and data deleted", Toast.LENGTH_SHORT).show()
            navController.navigate("login") { popUpTo(0) }
        }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("Delete Account & Clear Data", fontSize = 16.sp, color = PriorityHigh, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("ACCOUNT SECURITY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        var showResetDialog by remember { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showResetDialog = true }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("Change Password", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("ACCOUNT ACTIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
            viewModel.currentUser.value = null
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = PriorityHigh)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PriorityHigh)
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
            viewModel.currentUser.value = null
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
            navController.navigate("register")
        }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign Up/Register New Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (showResetDialog) {
            var currentPass by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }
            var passVisible by remember { mutableStateOf(false) }
            var newPassVisible by remember { mutableStateOf(false) }
            var confirmPassVisible by remember { mutableStateOf(false) }
            var errorMsg by remember { mutableStateOf("") }

            val passStrength = remember(newPass) {
                if (newPass.isEmpty()) 0f
                else if (newPass.length < 8) 0.3f
                else {
                    val hasLetter = newPass.any { it.isLetter() }
                    val hasDigit = newPass.any { it.isDigit() }
                    val hasSymbol = newPass.any { "@#$*".contains(it) }
                    val typesCount = listOf(hasLetter, hasDigit, hasSymbol).count { it }
                    when {
                        typesCount == 3 && newPass.length in 8..12 -> 1f
                        typesCount >= 2 && newPass.length in 8..12 -> 0.6f
                        else -> 0.3f
                    }
                }
            }
            val strengthColor = when {
                passStrength > 0.7f -> PriorityLow
                passStrength > 0.4f -> PriorityMedium
                else -> PriorityHigh
            }

            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Change Password", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("Current Password") },
                            visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(imageVector = if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            placeholder = { Text("8-12 chars, mix types") },
                            visualTransformation = if (newPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPassVisible = !newPassVisible }) {
                                    Icon(imageVector = if (newPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (newPass.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = passStrength,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = strengthColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = { Text("Confirm New Password") },
                            visualTransformation = if (confirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            supportingText = {
                                if (confirmPass.isNotEmpty()) {
                                    if (confirmPass != newPass) Text("Passwords do not match", color = PriorityHigh)
                                    else Text("Passwords match", color = PriorityLow)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                                    Icon(imageVector = if (confirmPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (errorMsg.isNotEmpty()) Text(errorMsg, color = PriorityHigh, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val user = viewModel.currentUser.value
                            val hasLetter = newPass.any { it.isLetter() }
                            val hasDigit = newPass.any { it.isDigit() }
                            val hasSymbol = newPass.any { "@#$*".contains(it) }
                            val isComplex = (listOf(hasLetter, hasDigit, hasSymbol).count { it } >= 2)

                            if (user != null && user.pass == currentPass) {
                                if (newPass.length in 8..12 && isComplex && newPass == confirmPass) {
                                    viewModel.updateUser(user.copy(pass = newPass))
                                    Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                    showResetDialog = false
                                } else {
                                    errorMsg = "Invalid new password or mismatch"
                                }
                            } else {
                                errorMsg = "Incorrect current password"
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Update") }
                },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("ABOUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Burnout Guard v1.0.0", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aligned with UN Sustainable Development Goal 3: Good Health and Well-being. Designed to promote mental wellness, reduce digital burnout, and manage cognitive load through accessible UI design.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}
