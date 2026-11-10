package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.model.UserProfile
import com.example.ui.components.VyraLogo
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val FIREBASE_PROJECT_ID = "gen-lang-client-0268529939"
const val FIREBASE_AUTH_CONSOLE_URL = "https://console.firebase.google.com/project/$FIREBASE_PROJECT_ID/authentication/providers"

val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300"
)

enum class AuthMode {
    LOGIN, SIGN_UP, FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { VyraRepository(context) }
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VyraLogo(size = 52.dp, showWordmark = true)

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Created by Gogi",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VyraViolet,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (authMode) {
                AuthMode.LOGIN -> {
                    LoginView(
                        repository = repository,
                        onAuthSuccess = onAuthSuccess,
                        onSwitchToSignUp = { authMode = AuthMode.SIGN_UP },
                        onForgotPassword = { authMode = AuthMode.FORGOT_PASSWORD }
                    )
                }
                AuthMode.SIGN_UP -> {
                    SignUpView(
                        repository = repository,
                        onAuthSuccess = onAuthSuccess,
                        onSwitchToLogin = { authMode = AuthMode.LOGIN }
                    )
                }
                AuthMode.FORGOT_PASSWORD -> {
                    ForgotPasswordView(
                        repository = repository,
                        onBackToLogin = { authMode = AuthMode.LOGIN }
                    )
                }
            }
        }
    }
}

@Composable
fun FirebaseProviderDisabledCard(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF261A0C),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Email/Password Provider Disabled",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFFDE68A)
                )
            }

            Text(
                text = "Firebase Authentication requires the Email/Password sign-in method to be enabled in your Firebase Console project.",
                fontSize = 12.sp,
                color = Color(0xFFFEF3C7)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1207)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFB45309))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Steps to enable in Firebase Console:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "1. Open Firebase Console for '$FIREBASE_PROJECT_ID'\n" +
                               "2. Go to Build > Authentication > Sign-in method\n" +
                               "3. Click 'Email/Password'\n" +
                               "4. Turn 'Enable' ON and click Save\n" +
                               "5. Return here and tap 'Retry'",
                        fontSize = 11.sp,
                        color = Color(0xFFF3F4F6),
                        lineHeight = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(FIREBASE_AUTH_CONSOLE_URL))
                        copied = true
                        Toast.makeText(context, "Firebase Console URL copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copied) "Copied Link" else "Copy URL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text(
                        text = "Retry",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Dismiss Notice", fontSize = 11.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun LoginView(
    repository: VyraRepository,
    onAuthSuccess: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProviderDisabledNotice by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to VYRA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Log in with your username and password",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Username input
            OutlinedTextField(
                value = usernameInput,
                onValueChange = {
                    usernameInput = it.trim()
                    errorMessage = null
                    showProviderDisabledNotice = false
                },
                label = { Text("Username") },
                placeholder = { Text("@username or email") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = VyraViolet)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_username_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input
            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    errorMessage = null
                    showProviderDisabledNotice = false
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = VyraMagenta)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onForgotPassword,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Forgot password?",
                        fontSize = 12.sp,
                        color = VyraViolet
                    )
                }
            }

            if (showProviderDisabledNotice) {
                Spacer(modifier = Modifier.height(10.dp))
                FirebaseProviderDisabledCard(
                    onDismiss = { showProviderDisabledNotice = false },
                    onRetry = {
                        showProviderDisabledNotice = false
                    }
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Log In Action Button
            Button(
                onClick = {
                    val rawUsername = usernameInput.trim()
                    if (rawUsername.isBlank() || passwordInput.isBlank()) {
                        errorMessage = "Please enter both username and password."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    showProviderDisabledNotice = false

                    scope.launch {
                        try {
                            // Resolve email if user entered a username
                            val emailToUse = if (rawUsername.contains("@")) {
                                rawUsername
                            } else {
                                val resolved = repository.getEmailForUsername(rawUsername)
                                if (resolved.isNullOrBlank()) {
                                    isLoading = false
                                    errorMessage = "No account found for username \"@$rawUsername\"."
                                    return@launch
                                }
                                resolved
                            }

                            // Authenticate securely with Firebase Authentication
                            Firebase.auth.signInWithEmailAndPassword(emailToUse, passwordInput).await()
                            isLoading = false
                            onAuthSuccess()
                        } catch (e: FirebaseAuthInvalidUserException) {
                            isLoading = false
                            errorMessage = "No account exists with those credentials."
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            isLoading = false
                            errorMessage = "Incorrect password. Please try again."
                        } catch (e: FirebaseAuthException) {
                            isLoading = false
                            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                                e.message?.contains("not allowed", ignoreCase = true) == true ||
                                e.message?.contains("provider is disabled", ignoreCase = true) == true
                            ) {
                                showProviderDisabledNotice = true
                                errorMessage = null
                            } else {
                                errorMessage = e.localizedMessage ?: "Authentication failed."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            if (e.message?.contains("not allowed", ignoreCase = true) == true ||
                                e.message?.contains("provider is disabled", ignoreCase = true) == true
                            ) {
                                showProviderDisabledNotice = true
                                errorMessage = null
                            } else {
                                errorMessage = e.localizedMessage ?: "Failed to log in. Please check your connection."
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Log In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Switch to Sign Up
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sign Up",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VyraMagenta,
                    modifier = Modifier
                        .clickable(onClick = onSwitchToSignUp)
                        .padding(4.dp)
                        .testTag("switch_to_signup_button")
                )
            }
        }
    }
}

@Composable
fun SignUpView(
    repository: VyraRepository,
    onAuthSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(PRESET_AVATARS[0]) }
    var isPrivate by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProviderDisabledNotice by remember { mutableStateOf(false) }

    // Live username uniqueness checking
    LaunchedEffect(username) {
        val clean = username.trim().lowercase().removePrefix("@")
        if (clean.length >= 3) {
            isCheckingUsername = true
            isUsernameAvailable = repository.checkUsernameAvailable(clean)
            isCheckingUsername = false
        } else {
            isUsernameAvailable = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Join VYRA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Create your account and choose your username",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Avatar picker
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, VyraViolet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedAvatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PRESET_AVATARS.forEach { url ->
                    val isSelected = selectedAvatar == url
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) VyraMagenta else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    errorMessage = null
                },
                label = { Text("Display Name") },
                placeholder = { Text("e.g. Alex Rivera") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_display_name_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Unique Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(30)
                    errorMessage = null
                },
                label = { Text("Unique Username") },
                placeholder = { Text("@username") },
                singleLine = true,
                trailingIcon = {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (isUsernameAvailable == true) {
                        Icon(Icons.Filled.Check, contentDescription = "Available", tint = VyraGreen)
                    } else if (isUsernameAvailable == false) {
                        Icon(Icons.Filled.Close, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                    }
                },
                supportingText = {
                    val clean = username.trim().lowercase().removePrefix("@")
                    if (clean.length in 1..2) {
                        Text("Username must be at least 3 characters", color = MaterialTheme.colorScheme.error)
                    } else if (isUsernameAvailable == false) {
                        Text("@$clean is already taken. Try another.", color = MaterialTheme.colorScheme.error)
                    } else if (isUsernameAvailable == true) {
                        Text("@$clean is available!", color = VyraGreen)
                    }
                },
                isError = isUsernameAvailable == false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_username_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    errorMessage = null
                },
                label = { Text("Email (Account & Recovery)") },
                placeholder = { Text("alex@example.com") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password (Min 6 characters)") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = VyraViolet) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = VyraMagenta) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_confirm_password_input"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Private Account", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Only approved followers can view your posts and stories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it }
                )
            }

            if (showProviderDisabledNotice) {
                Spacer(modifier = Modifier.height(10.dp))
                FirebaseProviderDisabledCard(
                    onDismiss = { showProviderDisabledNotice = false },
                    onRetry = {
                        showProviderDisabledNotice = false
                    }
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sign Up Action Button
            Button(
                onClick = {
                    val cleanUsername = username.trim().lowercase().removePrefix("@")
                    val cleanEmail = email.trim()

                    if (displayName.isBlank()) {
                        errorMessage = "Please enter your display name."
                        return@Button
                    }
                    if (cleanUsername.length < 3) {
                        errorMessage = "Username must be at least 3 characters."
                        return@Button
                    }
                    if (isUsernameAvailable != true) {
                        errorMessage = "Please choose an available username."
                        return@Button
                    }
                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                        errorMessage = "Please enter a valid email address."
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    showProviderDisabledNotice = false

                    scope.launch {
                        try {
                            // Check username availability again to prevent race conditions
                            val isStillAvailable = repository.checkUsernameAvailable(cleanUsername)
                            if (!isStillAvailable) {
                                isLoading = false
                                errorMessage = "The username @$cleanUsername is already taken. Please choose another."
                                return@launch
                            }

                            // 1. Create Firebase Auth user
                            val authResult = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                            val uid = authResult.user?.uid ?: throw IllegalStateException("User ID was null")

                            // 2. Save user profile document and reserve username
                            val profile = UserProfile(
                                userId = uid,
                                username = cleanUsername,
                                displayName = displayName.trim(),
                                email = cleanEmail,
                                bio = "Hey, I'm new to VYRA!",
                                websiteUrl = "",
                                avatarUrl = selectedAvatar,
                                isPrivate = isPrivate
                            )
                            repository.saveUserProfile(profile)

                            isLoading = false
                            onAuthSuccess()
                        } catch (e: FirebaseAuthUserCollisionException) {
                            isLoading = false
                            errorMessage = "An account with this email already exists. Try logging in."
                        } catch (e: FirebaseAuthWeakPasswordException) {
                            isLoading = false
                            errorMessage = "Password is too weak. Please use at least 6 characters with mixed letters and numbers."
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            isLoading = false
                            errorMessage = "Please enter a valid email address."
                        } catch (e: FirebaseAuthException) {
                            isLoading = false
                            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                                e.message?.contains("not allowed", ignoreCase = true) == true ||
                                e.message?.contains("provider is disabled", ignoreCase = true) == true
                            ) {
                                showProviderDisabledNotice = true
                                errorMessage = null
                            } else {
                                errorMessage = e.localizedMessage ?: "Registration failed."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            if (e.message?.contains("not allowed", ignoreCase = true) == true ||
                                e.message?.contains("provider is disabled", ignoreCase = true) == true
                            ) {
                                showProviderDisabledNotice = true
                                errorMessage = null
                            } else {
                                errorMessage = e.localizedMessage ?: "Sign up failed. Please check your connection."
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Create VYRA Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch to Login
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Log In",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VyraViolet,
                    modifier = Modifier
                        .clickable(onClick = onSwitchToLogin)
                        .padding(4.dp)
                        .testTag("switch_to_login_button")
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordView(
    repository: VyraRepository,
    onBackToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProviderDisabledNotice by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reset Password",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enter your username or email to receive reset instructions",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.trim()
                    errorMessage = null
                    showProviderDisabledNotice = false
                },
                label = { Text("Username or Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (showProviderDisabledNotice) {
                Spacer(modifier = Modifier.height(10.dp))
                FirebaseProviderDisabledCard(
                    onDismiss = { showProviderDisabledNotice = false },
                    onRetry = { showProviderDisabledNotice = false }
                )
            }

            if (successMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(successMessage ?: "", color = VyraGreen, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val raw = input.trim()
                    if (raw.isBlank()) {
                        errorMessage = "Please enter your username or email."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    showProviderDisabledNotice = false
                    scope.launch {
                        try {
                            val emailToUse = if (raw.contains("@")) raw
                            else repository.getEmailForUsername(raw)

                            if (emailToUse.isNullOrBlank()) {
                                isLoading = false
                                errorMessage = "No account found for \"$raw\"."
                                return@launch
                            }

                            Firebase.auth.sendPasswordResetEmail(emailToUse).await()
                            isLoading = false
                            successMessage = "Password reset email sent to $emailToUse. Please check your inbox."
                        } catch (e: FirebaseAuthException) {
                            isLoading = false
                            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                                e.message?.contains("not allowed", ignoreCase = true) == true ||
                                e.message?.contains("provider is disabled", ignoreCase = true) == true
                            ) {
                                showProviderDisabledNotice = true
                                errorMessage = null
                            } else {
                                errorMessage = e.localizedMessage ?: "Failed to send reset email."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            if (e.message?.contains("not allowed", ignoreCase = true) == true ||
                                e.message?.contains("provider is disabled", ignoreCase = true) == true
                            ) {
                                showProviderDisabledNotice = true
                                errorMessage = null
                            } else {
                                errorMessage = e.localizedMessage ?: "Failed to send reset email."
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Send Reset Link", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(onClick = onBackToLogin) {
                Text("Back to Log In", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    currentUserId: String,
    repository: VyraRepository,
    onSetupComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(PRESET_AVATARS[0]) }

    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(username) {
        val clean = username.trim().lowercase().removePrefix("@")
        if (clean.length >= 3) {
            isCheckingUsername = true
            isUsernameAvailable = repository.checkUsernameAvailable(clean)
            isCheckingUsername = false
        } else {
            isUsernameAvailable = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VyraLogo(size = 44.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Complete Your Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Pick an avatar and a unique username to get started",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar preview
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, VyraViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESET_AVATARS.forEach { url ->
                        val isSelected = selectedAvatar == url
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) VyraMagenta else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Display Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        errorMessage = null
                    },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Jordan Lee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(30)
                        errorMessage = null
                    },
                    label = { Text("Unique Username") },
                    placeholder = { Text("@username") },
                    singleLine = true,
                    trailingIcon = {
                        if (isCheckingUsername) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else if (isUsernameAvailable == true) {
                            Icon(Icons.Filled.Check, contentDescription = "Available", tint = VyraGreen)
                        } else if (isUsernameAvailable == false) {
                            Icon(Icons.Filled.Close, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    supportingText = {
                        val clean = username.trim().lowercase().removePrefix("@")
                        if (clean.length in 1..2) {
                            Text("Username must be at least 3 characters", color = MaterialTheme.colorScheme.error)
                        } else if (isUsernameAvailable == false) {
                            Text("@$clean is already taken.", color = MaterialTheme.colorScheme.error)
                        } else if (isUsernameAvailable == true) {
                            Text("@$clean is available!", color = VyraGreen)
                        }
                    },
                    isError = isUsernameAvailable == false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Optional)") },
                    placeholder = { Text("Creator, traveler, tech enthusiast...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cleanUsername = username.trim().lowercase().removePrefix("@")
                        if (displayName.isBlank()) {
                            errorMessage = "Please enter your display name."
                            return@Button
                        }
                        if (cleanUsername.length < 3) {
                            errorMessage = "Username must be at least 3 characters."
                            return@Button
                        }
                        if (isUsernameAvailable != true) {
                            errorMessage = "Please choose an available username."
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        scope.launch {
                            try {
                                val currentUserEmail = Firebase.auth.currentUser?.email ?: ""
                                val profile = UserProfile(
                                    userId = currentUserId,
                                    username = cleanUsername,
                                    displayName = displayName.trim(),
                                    email = currentUserEmail,
                                    bio = bio.trim(),
                                    websiteUrl = "",
                                    avatarUrl = selectedAvatar,
                                    isPrivate = false
                                )
                                repository.saveUserProfile(profile)
                                isLoading = false
                                onSetupComplete()
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.localizedMessage ?: "Failed to save profile."
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_setup_done_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Get Started with VYRA", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
