package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.UserProfile
import com.example.model.VerificationRequest
import com.example.ui.components.VyraUserBadgeRow
import com.example.ui.components.VyraVerificationBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * FEATURE 1: Developer Verification Application Form
 * MUST contain ONLY:
 * - Developer Name
 * - Years of Experience
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperVerificationSheet(
    currentUser: UserProfile,
    repository: VyraRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var developerName by remember { mutableStateOf(currentUser.displayName.ifBlank { currentUser.username }) }
    var experienceYears by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userRequests by repository.observeUserVerificationRequests(currentUser.userId).collectAsState(initial = emptyList())
    val existingDevRequest = userRequests.firstOrNull { it.badgeType == "developer" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("developer_verification_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header with Purple Developer Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VyraVerificationBadge(badgeType = "developer", size = 26.dp)
                    Column {
                        Text(
                            text = "Developer Verification",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Purple code badge for software developers",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if ("developer" in currentUser.badges) {
                Surface(
                    color = VyraViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VyraViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VyraVerificationBadge(badgeType = "developer", size = 24.dp)
                        Column {
                            Text("You are a Verified Developer!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VyraViolet)
                            Text("Your purple badge is active and visible on your profile, posts, comments, and search results.", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
                return@Column
            }

            if (existingDevRequest != null && existingDevRequest.status == "pending") {
                Surface(
                    color = Color(0xFF2E2410),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFFF59E0B))
                            Text("Application Under Review", fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                        }
                        Text(
                            text = "Your developer verification application has been submitted and is pending review by @owneranant.",
                            fontSize = 12.sp,
                            color = Color(0xFFFEF3C7)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
                return@Column
            }

            if (submitSuccess) {
                Surface(
                    color = VyraGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VyraGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VyraGreen)
                            Text("Application Submitted!", fontWeight = FontWeight.Bold, color = VyraGreen)
                        }
                        Text(
                            text = "Your application was sent to @owneranant. You will receive a notification once reviewed.",
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
                return@Column
            }

            // ONLY TWO FIELDS: Developer Name & Years of Experience
            OutlinedTextField(
                value = developerName,
                onValueChange = { developerName = it },
                label = { Text("Developer Name") },
                placeholder = { Text("e.g. Alex Chen") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = experienceYears,
                onValueChange = { experienceYears = it },
                label = { Text("Years of Experience") },
                placeholder = { Text("e.g. 4 years") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_experience_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (developerName.isBlank() || experienceYears.isBlank()) {
                        errorMessage = "Please enter your developer name and years of experience."
                        return@Button
                    }
                    isSubmitting = true
                    errorMessage = null

                    scope.launch {
                        try {
                            repository.submitDeveloperVerification(
                                developerName = developerName,
                                experienceYears = experienceYears
                            )
                            isSubmitting = false
                            submitSuccess = true
                        } catch (e: Exception) {
                            isSubmitting = false
                            errorMessage = e.localizedMessage ?: "Failed to submit application."
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_dev_verification_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Submit Application", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * FEATURE 1: Influencer Verification Application Form
 * MUST contain ONLY:
 * - Channel Name
 * - Channel URL/Link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencerVerificationSheet(
    currentUser: UserProfile,
    repository: VyraRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var channelName by remember { mutableStateOf("") }
    var channelUrl by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userRequests by repository.observeUserVerificationRequests(currentUser.userId).collectAsState(initial = emptyList())
    val existingInfluencerRequest = userRequests.firstOrNull { it.badgeType == "influencer" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("influencer_verification_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header with Gold Influencer Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VyraVerificationBadge(badgeType = "influencer", size = 26.dp)
                    Column {
                        Text(
                            text = "Influencer Verification",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Gold star badge for public creators & influencers",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if ("influencer" in currentUser.badges) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VyraVerificationBadge(badgeType = "influencer", size = 24.dp)
                        Column {
                            Text("You are a Verified Influencer!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFF59E0B))
                            Text("Your gold star creator badge is active across all your content and profile.", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
                return@Column
            }

            if (existingInfluencerRequest != null && existingInfluencerRequest.status == "pending") {
                Surface(
                    color = Color(0xFF2E2410),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFFF59E0B))
                            Text("Application Under Review", fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                        }
                        Text(
                            text = "Your influencer verification application has been submitted and is pending review by @owneranant.",
                            fontSize = 12.sp,
                            color = Color(0xFFFEF3C7)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
                return@Column
            }

            if (submitSuccess) {
                Surface(
                    color = VyraGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VyraGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VyraGreen)
                            Text("Application Submitted!", fontWeight = FontWeight.Bold, color = VyraGreen)
                        }
                        Text(
                            text = "Your application was sent to @owneranant. You will receive a notification once reviewed.",
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
                return@Column
            }

            // ONLY TWO FIELDS: Channel Name & Channel URL/Link
            OutlinedTextField(
                value = channelName,
                onValueChange = { channelName = it },
                label = { Text("Channel Name") },
                placeholder = { Text("e.g. Tech With Maya, Gogi Studios") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = channelUrl,
                onValueChange = { channelUrl = it },
                label = { Text("Channel URL / Link") },
                placeholder = { Text("e.g. https://youtube.com/@mychannel") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_url_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (channelName.isBlank() || channelUrl.isBlank()) {
                        errorMessage = "Please enter your channel name and channel URL/link."
                        return@Button
                    }
                    isSubmitting = true
                    errorMessage = null

                    scope.launch {
                        try {
                            repository.submitInfluencerVerification(
                                channelName = channelName,
                                channelUrl = channelUrl
                            )
                            isSubmitting = false
                            submitSuccess = true
                        } catch (e: Exception) {
                            isSubmitting = false
                            errorMessage = e.localizedMessage ?: "Failed to submit application."
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_influencer_verification_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Submit Application", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * FEATURE 1: Admin-Only Verification Approval System
 * STRICT CRITICAL REQUIREMENT:
 * ONLY @owneranant can approve or reject applications.
 *
 * Distinct Sections:
 * - Developer Applications
 * - Influencer Applications
 *
 * Each Section shows:
 * - PENDING applications
 * - APPROVED applications
 * - REJECTED applications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(
    currentUserId: String,
    currentUser: UserProfile,
    repository: VyraRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isAuthorizedAdmin = remember(currentUser) {
        repository.isUserAdmin(currentUser)
    }

    var selectedSection by remember { mutableStateOf("developer") } // "developer" or "influencer"
    var selectedStatusTab by remember { mutableStateOf("pending") } // "pending", "approved", "rejected"
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val allRequests by repository.observeAllVerificationRequests().collectAsState(initial = emptyList())

    val sectionRequests = remember(allRequests, selectedSection) {
        allRequests.filter { it.badgeType == selectedSection }
    }

    val pendingCount = remember(sectionRequests) { sectionRequests.count { it.status == "pending" } }
    val approvedCount = remember(sectionRequests) { sectionRequests.count { it.status == "approved" } }
    val rejectedCount = remember(sectionRequests) { sectionRequests.count { it.status == "rejected" } }

    val filteredList = remember(sectionRequests, selectedStatusTab) {
        sectionRequests.filter { it.status == selectedStatusTab }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Admin Verification Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Authorized to @owneranant", fontSize = 11.sp, color = VyraMagenta)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VyraMagenta.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("@owneranant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VyraMagenta)
                    }
                }
            )
        }
    ) { padding ->
        if (!isAuthorizedAdmin) {
            // Unauthorized access barrier
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                        }

                        Text(
                            text = "Access Restricted",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Only the platform administrator (@owneranant) is authorized to review, approve, or reject verification badge applications.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Return to Settings")
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Main Section Tabs: Developer Applications vs Influencer Applications
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Developer tab button
                val isDevSelected = selectedSection == "developer"
                Surface(
                    color = if (isDevSelected) VyraViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(if (isDevSelected) 1.5.dp else 1.dp, if (isDevSelected) VyraViolet else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSection = "developer" }
                        .testTag("admin_tab_developer")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        VyraVerificationBadge(badgeType = "developer", size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Developer Apps",
                            fontSize = 12.sp,
                            fontWeight = if (isDevSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDevSelected) VyraViolet else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Influencer tab button
                val isInfSelected = selectedSection == "influencer"
                Surface(
                    color = if (isInfSelected) Color(0xFFF59E0B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(if (isInfSelected) 1.5.dp else 1.dp, if (isInfSelected) Color(0xFFF59E0B) else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSection = "influencer" }
                        .testTag("admin_tab_influencer")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        VyraVerificationBadge(badgeType = "influencer", size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Influencer Apps",
                            fontSize = 12.sp,
                            fontWeight = if (isInfSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isInfSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-status Filter Tabs: PENDING, APPROVED, REJECTED
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("pending", "PENDING", pendingCount),
                    Triple("approved", "APPROVED", approvedCount),
                    Triple("rejected", "REJECTED", rejectedCount)
                ).forEach { (statusKey, label, count) ->
                    val isSelected = selectedStatusTab == statusKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusTab = statusKey },
                        label = {
                            Text(
                                text = "$label ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (statusKey) {
                                "approved" -> VyraGreen.copy(alpha = 0.3f)
                                "rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                else -> if (selectedSection == "developer") VyraViolet.copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Feedback banners
            if (actionMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = VyraGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionMessage ?: "",
                        color = VyraGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (actionError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (selectedStatusTab) {
                                "approved" -> Icons.Filled.Verified
                                "rejected" -> Icons.Filled.Cancel
                                else -> Icons.Filled.FactCheck
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${selectedStatusTab.uppercase()} ${if (selectedSection == "developer") "Developer" else "Influencer"} applications.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { req ->
                        AdminVerificationCard(
                            request = req,
                            onApprove = {
                                actionMessage = null
                                actionError = null
                                scope.launch {
                                    try {
                                        repository.reviewVerificationRequest(
                                            requestId = req.id,
                                            applicantUserId = req.userId,
                                            badgeType = req.badgeType,
                                            approve = true
                                        )
                                        actionMessage = "Approved @${req.username}! Badge granted and moved to Approved."
                                    } catch (e: Exception) {
                                        actionError = e.localizedMessage ?: "Failed to approve application."
                                    }
                                }
                            },
                            onReject = { reason ->
                                actionMessage = null
                                actionError = null
                                scope.launch {
                                    try {
                                        repository.reviewVerificationRequest(
                                            requestId = req.id,
                                            applicantUserId = req.userId,
                                            badgeType = req.badgeType,
                                            approve = false,
                                            reason = reason
                                        )
                                        actionMessage = "Rejected @${req.username}. Moved to Rejected section."
                                    } catch (e: Exception) {
                                        actionError = e.localizedMessage ?: "Failed to reject application."
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVerificationCard(
    request: VerificationRequest,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    val context = LocalContext.current
    val isDev = request.badgeType == "developer"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_verification_card_${request.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            width = 1.dp,
            color = when (request.status) {
                "approved" -> VyraGreen.copy(alpha = 0.5f)
                "rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                else -> if (isDev) VyraViolet.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Applicant Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (request.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = request.avatarUrl,
                            contentDescription = request.username,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = request.username.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = request.displayName.ifBlank { request.username },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            VyraVerificationBadge(badgeType = request.badgeType, size = 16.dp)
                        }
                        Text("@${request.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Status chip
                Surface(
                    color = when (request.status) {
                        "approved" -> VyraGreen.copy(alpha = 0.2f)
                        "rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (request.status) {
                            "approved" -> VyraGreen
                            "rejected" -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Specific Submitted Form Details
            if (isDev) {
                // Developer Form: Developer Name & Years of Experience
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Developer Name: ${request.developerName.ifBlank { request.displayName.ifBlank { request.username } }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Years of Experience: ${request.experienceYears.ifBlank { request.platformOrExperience.ifBlank { "Not specified" } }}",
                        fontSize = 13.sp,
                        color = VyraViolet,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Influencer Form: Channel Name & Channel URL/Link
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Channel Name: ${request.channelName.ifBlank { request.platformOrExperience.ifBlank { request.displayName } }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val url = request.channelUrl.ifBlank { request.proofLink }
                    if (url.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (ignored: Exception) {}
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = url,
                                fontSize = 12.sp,
                                color = VyraCyan,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Channel", tint = VyraCyan, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (request.status == "rejected" && request.rejectionReason.isNotBlank()) {
                Text(
                    text = "Rejection Reason: ${request.rejectionReason}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action Buttons: Only shown when status is PENDING
            if (request.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReject("Application requirements not met.") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_reject_button_${request.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_approve_button_${request.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDev) VyraViolet else Color(0xFFD97706)
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
