package com.example.pucematch.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pucematch.domain.Screen

@Composable
fun RegisterScreenStateful(navController: NavController) {
    // Elevación de estado (State Hoisting)
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var career by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    // Estados de error para validaciones en tiempo real
    var isEmailError by rememberSaveable { mutableStateOf(false) }
    var isSemesterError by rememberSaveable { mutableStateOf(false) }
    var isPasswordError by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordError by rememberSaveable { mutableStateOf(false) }

    RegisterScreenStateless(
        name = name,
        email = email,
        semester = semester,
        career = career,
        password = password,
        confirmPassword = confirmPassword,
        isEmailError = isEmailError,
        isSemesterError = isSemesterError,
        isPasswordError = isPasswordError,
        isConfirmPasswordError = isConfirmPasswordError,
        onNameChange = { input -> name = input },
        onEmailChange = { input ->
            email = input
            // Validación en tiempo real del correo institucional
            isEmailError = !input.endsWith("@puce.edu.ec")
        },
        onSemesterChange = { input ->
            semester = input
            if (input.isEmpty()) {
                isSemesterError = false
            } else {
                // Validación estricta con toIntOrNull()
                val semNumber = input.toIntOrNull()
                isSemesterError = semNumber == null || semNumber !in 1..12
            }
        },
        onCareerChange = { input -> career = input },
        onPasswordChange = { input ->
            password = input
            isPasswordError = input.length < 6
            if (confirmPassword.isNotEmpty()) {
                isConfirmPasswordError = input != confirmPassword
            }
        },
        onConfirmPasswordChange = { input ->
            confirmPassword = input
            isConfirmPasswordError = password != input
        },
        onRegisterClick = {
            val isEmailValid = email.endsWith("@puce.edu.ec") && email.isNotEmpty()
            val semNum = semester.toIntOrNull()
            val isSemesterValid = semester.isNotEmpty() && semNum != null && semNum in 1..12
            val isPasswordValid = password.isNotEmpty() && password.length >= 6
            val isConfirmValid = password == confirmPassword
            val isNameValid = name.isNotEmpty()
            val isCareerValid = career.isNotEmpty()

            if (isEmailValid && isSemesterValid && isPasswordValid && isConfirmValid && isNameValid && isCareerValid) {
                // Proceder al registro y luego ir a Home
                navController.navigate(Screen.Home) {
                    popUpTo(Screen.Login) { inclusive = true }
                }
            } else {
                // Actualizar visualización de errores si se intenta enviar incompleto
                isEmailError = !isEmailValid
                isSemesterError = !isSemesterValid
                isPasswordError = !isPasswordValid
                isConfirmPasswordError = !isConfirmValid
            }
        },
        onBackToLogin = {
            navController.navigateUp()
        }
    )
}

@Composable
fun RegisterScreenStateless(
    name: String,
    email: String,
    semester: String,
    career: String,
    password: String,
    confirmPassword: String,
    isEmailError: Boolean,
    isSemesterError: Boolean,
    isPasswordError: Boolean,
    isConfirmPasswordError: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onCareerChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Brush gradiente para dar un aspecto moderno y premium
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Previene que el teclado tape los inputs
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Botón superior de retroceso
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al Login"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Crear Cuenta",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = "Regístrate con tu correo PUCE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Nombre
                        OutlinedTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = { Text("Nombre Completo") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Correo Institucional
                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            label = { Text("Correo PUCE") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            isError = isEmailError,
                            supportingText = {
                                if (isEmailError) {
                                    Text("Debe usar un correo válido de la PUCE (@puce.edu.ec)")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Semestre
                        OutlinedTextField(
                            value = semester,
                            onValueChange = onSemesterChange,
                            label = { Text("Semestre (1-12)") },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            },
                            isError = isSemesterError,
                            supportingText = {
                                if (isSemesterError) {
                                    Text("Semestre inválido (debe ser un número del 1 al 12)")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Carrera
                        OutlinedTextField(
                            value = career,
                            onValueChange = onCareerChange,
                            label = { Text("Carrera") },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Contraseña
                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            isError = isPasswordError,
                            supportingText = {
                                if (isPasswordError) {
                                    Text("La contraseña debe tener al menos 6 caracteres")
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Confirmar Contraseña
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = { Text("Confirmar Contraseña") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            isError = isConfirmPasswordError,
                            supportingText = {
                                if (isConfirmPasswordError) {
                                    Text("Las contraseñas no coinciden")
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onRegisterClick()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de Registro
                val isButtonEnabled = name.isNotEmpty() &&
                        email.isNotEmpty() && !isEmailError &&
                        semester.isNotEmpty() && !isSemesterError &&
                        career.isNotEmpty() &&
                        password.isNotEmpty() && !isPasswordError &&
                        confirmPassword.isNotEmpty() && !isConfirmPasswordError

                Button(
                    onClick = onRegisterClick,
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Validar y Registrarse",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBackToLogin) {
                    Text(
                        text = "¿Ya tienes cuenta? Inicia Sesión",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
