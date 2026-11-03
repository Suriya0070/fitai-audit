package com.jsf.app.pravin.ui.screens.trainer

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.remote.TrainerDataSource
import com.jsf.app.pravin.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrainerProfileSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferences(application)
    private val dataSource = TrainerDataSource()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(bio: String, specialization: String, certifications: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val uid = prefs.currentUserId.first()
            if (uid.isNotEmpty()) {
                dataSource.updateTrainerProfile(uid, bio, specialization, certifications)
            }
            _isSaving.value = false
            _saved.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerProfileSetupScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val vm: TrainerProfileSetupViewModel = viewModel()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()

    var bio by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var certifications by remember { mutableStateOf("") }

    if (saved) {
        LaunchedEffect(Unit) { onSaved() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainer Profile", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        },
        containerColor = LightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Complete your trainer profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                "This helps clients understand your expertise",
                fontSize = 14.sp,
                color = TextGray
            )

            ProfileTextField(
                value = bio,
                onValueChange = { bio = it },
                label = "Bio",
                placeholder = "Tell clients about yourself...",
                minLines = 3,
                maxLines = 6
            )

            ProfileTextField(
                value = specialization,
                onValueChange = { specialization = it },
                label = "Specialization",
                placeholder = "e.g. Strength training, Weight loss, Sports performance"
            )

            ProfileTextField(
                value = certifications,
                onValueChange = { certifications = it },
                label = "Certifications",
                placeholder = "e.g. NASM-CPT, ACE, CrossFit L1"
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.save(bio, specialization, certifications) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSaving && bio.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DeepSpace, strokeWidth = 2.dp)
                } else {
                    Text("Save Profile", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextGray) },
        placeholder = { Text(placeholder, color = TextGray.copy(alpha = 0.5f)) },
        minLines = minLines,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = LightBorder,
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark,
            cursorColor = CyanPrimary,
            focusedContainerColor = LightCard,
            unfocusedContainerColor = LightCard
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
