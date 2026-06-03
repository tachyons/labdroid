package `in`.aboobacker.labdroid.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.model.UserUpdate
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.UpdateResult
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    user: User,
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit = {}
) {
    val isUpdating by viewModel.isUpdating.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { result ->
            when (result) {
                is UpdateResult.Success -> {
                    onBackClick()
                }

                is UpdateResult.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EditProfileContent(
                user = user,
                onBackClick = onBackClick,
                onUpdateClick = { update ->
                    viewModel.updateProfile(update)
                }
            )

            if (isUpdating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    user: User,
    onBackClick: () -> Unit = {},
    onUpdateClick: (UserUpdate) -> Unit = {}
) {
    var fullName by remember { mutableStateOf(user.name) }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var location by remember { mutableStateOf(user.location ?: "") }
    var website by remember { mutableStateOf(user.websiteUrl ?: "") }
    var publicEmail by remember { mutableStateOf(user.publicEmail ?: "") }
    var organization by remember { mutableStateOf(user.organization ?: "") }
    var jobTitle by remember { mutableStateOf(user.jobTitle ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Photo
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = selectedImageUri ?: user.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = (-4).dp, y = (-4).dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                IconButton(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }

        TextButton(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Change profile photo", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        EditField(label = "Full Name", value = fullName, onValueChange = { fullName = it })
        EditField(
            label = "Job Title",
            value = jobTitle,
            onValueChange = { jobTitle = it },
            placeholder = "e.g. Senior Software Engineer"
        )
        EditField(
            label = "Organization",
            value = organization,
            onValueChange = { organization = it },
            placeholder = "e.g. GitLab"
        )
        EditField(
            label = "Bio",
            value = bio,
            onValueChange = { bio = it },
            singleLine = false,
            minHeight = 100.dp
        )
        EditField(
            label = "Location",
            value = location,
            onValueChange = { location = it },
            icon = Icons.Default.LocationOn,
            placeholder = "San Francisco, CA"
        )
        EditField(
            label = "Website",
            value = website,
            onValueChange = { website = it },
            placeholder = "https://example.com"
        )
        EditField(
            label = "Public Email",
            value = publicEmail,
            onValueChange = { publicEmail = it },
            icon = Icons.Default.Email,
            placeholder = "user@example.com"
        )

        Text(
            "This email will be visible to all logged-in users.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 24.dp)
        )

        Button(
            onClick = {
                onUpdateClick(
                    UserUpdate(
                        name = fullName,
                        bio = bio,
                        location = location,
                        website_url = website,
                        organization = organization,
                        job_title = jobTitle,
                        public_email = publicEmail
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Update Profile", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Cancel Changes", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Danger Zone",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFD32F2F)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Delete Account", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
            placeholder = { Text(placeholder) },
            leadingIcon = icon?.let {
                {
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    LabdroidTheme {
        EditProfileContent(
            user = User(
                id = 1,
                name = "Alex Rivera",
                username = "arivera",
                avatarUrl = "https://secure.gravatar.com/avatar/sample",
                webUrl = "https://arivera.dev",
                bio = "Senior DevOps Engineer at GitLab. Passionate about CI/CD optimization and cloud-native architectures.",
                jobTitle = "Senior DevOps Engineer",
                followers = 1200,
                following = 438,
                starCount = 85
            )
        )
    }
}
