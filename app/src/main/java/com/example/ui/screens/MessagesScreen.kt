package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.*
import com.example.ui.components.VyraUserBadgeRow
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * CRITICAL BUG 1 FIX & SOCIAL UPDATE: Stable One-to-One and Group Messaging
 * with Chat Polls, Voice Notes, Personalization Themes, and Watch Together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    initialConversationId: String? = null,
    onNavigateBack: () -> Unit = {},
    onStartWatchSession: (videoUrl: String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var activeConversationId by remember { mutableStateOf<String?>(initialConversationId) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val conversations by repository.observeUserConversations().collectAsStateWithLifecycle(initialValue = emptyList())
    val allUsers by repository.observeAllUsers().collectAsStateWithLifecycle(initialValue = emptyList())
    val blockedUsers by repository.observeBlockedUsers().collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(initialConversationId) {
        if (!initialConversationId.isNullOrBlank()) {
            activeConversationId = initialConversationId
        }
    }

    if (activeConversationId != null) {
        val targetConv = conversations.firstOrNull { it.id == activeConversationId }
        ChatThreadView(
            conversationId = activeConversationId!!,
            initialConversation = targetConv,
            currentUserId = currentUserId,
            currentUser = currentUser,
            allUsers = allUsers,
            repository = repository,
            onBack = {
                activeConversationId = null
                onNavigateBack()
            },
            onStartWatchTogether = { videoUrl ->
                onStartWatchSession(videoUrl)
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Direct Messages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.testTag("button_new_chat")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "New Message", tint = VyraViolet)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your Messages",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Send direct messages, share voice notes, create polls, and personalize your conversations.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showNewChatDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet),
                                modifier = Modifier.testTag("button_start_conversation_empty")
                            ) {
                                Text("Start a Conversation")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("conversations_list"),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            val otherUserId = conv.memberIds.firstOrNull { it != currentUserId }
                            val otherUser = allUsers.firstOrNull { it.userId == otherUserId }

                            val title = if (conv.type == "group") conv.name.ifBlank { "Group Chat" }
                            else otherUser?.displayName?.ifBlank { otherUser.username } ?: conv.name.ifBlank { "Chat" }

                            val avatar = if (conv.type == "group") conv.avatarUrl else otherUser?.avatarUrl ?: conv.avatarUrl
                            val unreadCount = conv.unreadCounts[currentUserId] ?: 0L

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeConversationId = conv.id }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("conversation_item_${conv.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box {
                                    if (avatar.isNotBlank()) {
                                        AsyncImage(
                                            model = avatar,
                                            contentDescription = title,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(if (conv.type == "group") VyraMagenta else VyraViolet),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (conv.type == "group") Icons.Filled.Group else Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    if (otherUser != null && otherUser.showOnlineStatus) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(VyraGreen)
                                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = title,
                                                fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (otherUser != null) {
                                                VyraUserBadgeRow(badges = otherUser.badges, size = 13.dp)
                                            }
                                        }
                                        if (conv.lastMessageTimestamp != null) {
                                            Text(
                                                text = formatTimeAgo(conv.lastMessageTimestamp.toDate()),
                                                fontSize = 11.sp,
                                                color = if (unreadCount > 0) VyraViolet else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = conv.lastMessageText.ifBlank { "Start chatting" },
                                            fontSize = 13.sp,
                                            color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (unreadCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 6.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(VyraViolet),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        if (showNewChatDialog) {
            NewChatDialog(
                currentUserId = currentUserId,
                allUsers = allUsers.filter { it.userId != currentUserId && it.userId !in blockedUsers },
                repository = repository,
                onDismiss = { showNewChatDialog = false },
                onConversationCreated = { newConvId ->
                    showNewChatDialog = false
                    activeConversationId = newConvId
                }
            )
        }
    }
}

/**
 * Thread View supporting One-to-One and Group Chats
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadView(
    conversationId: String,
    initialConversation: Conversation?,
    currentUserId: String,
    currentUser: UserProfile?,
    allUsers: List<UserProfile>,
    repository: VyraRepository,
    onBack: () -> Unit,
    onStartWatchTogether: (videoUrl: String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val liveConversation by repository.observeConversation(conversationId).collectAsStateWithLifecycle(initialValue = initialConversation)
    val conversation = liveConversation ?: initialConversation ?: Conversation(id = conversationId, memberIds = listOf(currentUserId))

    val messages by repository.observeMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())

    // Mark as read when entering
    LaunchedEffect(conversationId) {
        repository.markConversationRead(conversationId)
    }

    // Resolve other user info
    val otherUserId = conversation.memberIds.firstOrNull { it != currentUserId }
    val otherUser = remember(allUsers, otherUserId) { allUsers.firstOrNull { it.userId == otherUserId } }

    val chatTitle = if (conversation.type == "group") conversation.name.ifBlank { "Group Chat" }
    else otherUser?.displayName?.ifBlank { otherUser.username } ?: conversation.name.ifBlank { "VYRA User" }

    // Theme & Personalization
    var showPersonalizationMenu by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showWatchTogetherDialog by remember { mutableStateOf(false) }

    val currentTheme = conversation.theme.ifBlank { "default" }
    val isConversationMuted = currentUserId in conversation.mutedUserIds

    // Theme gradient brush
    val backgroundBrush = remember(currentTheme) {
        when (currentTheme) {
            "cyberpunk" -> Brush.verticalGradient(listOf(Color(0xFF0F051D), Color(0xFF1E0A3C), Color(0xFF090014)))
            "midnight_blue" -> Brush.verticalGradient(listOf(Color(0xFF070B19), Color(0xFF0F172A), Color(0xFF020617)))
            "sunset" -> Brush.verticalGradient(listOf(Color(0xFF1F0D15), Color(0xFF2E1020), Color(0xFF13060B)))
            "emerald" -> Brush.verticalGradient(listOf(Color(0xFF061510), Color(0xFF0D281E), Color(0xFF030C09)))
            else -> Brush.verticalGradient(listOf(Color(0xFF0F0F14), Color(0xFF151821), Color(0xFF0F0F14)))
        }
    }

    val bubbleColorPrimary = remember(currentTheme) {
        when (currentTheme) {
            "cyberpunk" -> Color(0xFF9333EA)
            "midnight_blue" -> Color(0xFF2563EB)
            "sunset" -> Color(0xFFEA580C)
            "emerald" -> Color(0xFF059669)
            else -> VyraViolet
        }
    }

    // Voice recording states
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, start recording
            try {
                val output = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                val recorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(output.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                recordedAudioFile = output
                isRecordingVoice = true
                recordingDurationSec = 0
            } catch (e: Exception) {
                android.util.Log.e("VyraVoice", "Recorder error: ${e.message}")
            }
        }
    }

    // Recording duration timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            while (isRecordingVoice) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val avatar = if (conversation.type == "group") conversation.avatarUrl else otherUser?.avatarUrl ?: ""
                        if (avatar.isNotBlank()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = chatTitle,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (conversation.type == "group") VyraMagenta else bubbleColorPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(chatTitle.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = chatTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (otherUser != null) {
                                    VyraUserBadgeRow(badges = otherUser.badges, size = 14.dp)
                                }
                            }
                            Text(
                                text = if (conversation.type == "group") "${conversation.memberIds.size} members"
                                else if (otherUser?.showOnlineStatus == true) "Active on VYRA" else "@${otherUser?.username ?: "user"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Watch Together button
                    IconButton(onClick = { showWatchTogetherDialog = true }) {
                        Icon(Icons.Filled.Tv, contentDescription = "Watch Together", tint = VyraCyan)
                    }

                    // Personalization Menu
                    Box {
                        IconButton(onClick = { showPersonalizationMenu = true }) {
                            Icon(Icons.Filled.Palette, contentDescription = "Personalization", tint = bubbleColorPrimary)
                        }

                        DropdownMenu(
                            expanded = showPersonalizationMenu,
                            onDismissRequest = { showPersonalizationMenu = false }
                        ) {
                            Text(
                                text = "Chat Personalization",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Default Obsidian") },
                                onClick = {
                                    scope.launch { repository.updateConversationTheme(conversationId, "default") }
                                    showPersonalizationMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.BrightnessAuto, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cyberpunk Neon") },
                                onClick = {
                                    scope.launch { repository.updateConversationTheme(conversationId, "cyberpunk") }
                                    showPersonalizationMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.FlashOn, contentDescription = null, tint = Color(0xFF9333EA)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Midnight Blue") },
                                onClick = {
                                    scope.launch { repository.updateConversationTheme(conversationId, "midnight_blue") }
                                    showPersonalizationMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.NightlightRound, contentDescription = null, tint = Color(0xFF2563EB)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sunset Ember") },
                                onClick = {
                                    scope.launch { repository.updateConversationTheme(conversationId, "sunset") }
                                    showPersonalizationMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color(0xFFEA580C)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Emerald Matrix") },
                                onClick = {
                                    scope.launch { repository.updateConversationTheme(conversationId, "emerald") }
                                    showPersonalizationMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Lens, contentDescription = null, tint = Color(0xFF059669)) }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(if (isConversationMuted) "Unmute Notifications" else "Mute Notifications") },
                                onClick = {
                                    scope.launch { repository.toggleMuteConversation(conversationId, !isConversationMuted) }
                                    showPersonalizationMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isConversationMuted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "End-to-end secured on VYRA",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No messages yet.\nSay hello to $chatTitle!",
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        reverseLayout = false
                    ) {
                        items(messages, key = { it.id.ifBlank { "msg_${it.createdAt?.seconds}_${it.text.hashCode()}" } }) { msg ->
                            val isMine = msg.senderId == currentUserId

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                            ) {
                                if (!isMine && conversation.type == "group") {
                                    Text(
                                        text = msg.senderUsername,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                    )
                                }

                                when (msg.mediaType) {
                                    "poll" -> {
                                        PollMessageCard(
                                            pollQuestion = msg.pollQuestion,
                                            pollOptions = msg.pollOptions,
                                            pollVotes = msg.pollVotes,
                                            currentUserId = currentUserId,
                                            bubbleColor = bubbleColorPrimary,
                                            onVote = { optIdx ->
                                                scope.launch {
                                                    repository.voteOnPoll(conversationId, msg.id, optIdx)
                                                }
                                            }
                                        )
                                    }

                                    "voice" -> {
                                        VoiceMessageCard(
                                            audioUrl = msg.mediaUrl,
                                            durationMs = msg.voiceDurationMs,
                                            isMine = isMine,
                                            bubbleColor = bubbleColorPrimary
                                        )
                                    }

                                    "image", "video" -> {
                                        Surface(
                                            color = if (isMine) bubbleColorPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.widthIn(max = 280.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                AsyncImage(
                                                    model = msg.mediaUrl,
                                                    contentDescription = "Media",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .clip(RoundedCornerShape(12.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                if (msg.text.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = msg.text,
                                                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    else -> {
                                        // Standard text or shared post
                                        if (msg.sharedPostId.isNotBlank()) {
                                            Surface(
                                                color = if (isMine) bubbleColorPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    if (msg.mediaUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = msg.mediaUrl,
                                                            contentDescription = "Shared Media",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(160.dp)
                                                                .clip(RoundedCornerShape(10.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                    }
                                                    Text(
                                                        text = msg.text.ifBlank { "Shared a post" },
                                                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                color = if (isMine) bubbleColorPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isMine) 16.dp else 4.dp,
                                                    bottomEnd = if (isMine) 4.dp else 16.dp
                                                ),
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (msg.createdAt != null) {
                                    Text(
                                        text = formatTimeAgo(msg.createdAt.toDate()),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Recording Active Bar
                AnimatedVisibility(visible = isRecordingVoice) {
                    Surface(
                        color = Color(0xFF1E1020),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Text(
                                    text = "Recording Voice: ${recordingDurationSec / 60}:${String.format("%02d", recordingDurationSec % 60)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        try {
                                            mediaRecorder?.stop()
                                            mediaRecorder?.release()
                                        } catch (_: Exception) {}
                                        mediaRecorder = null
                                        isRecordingVoice = false
                                        recordedAudioFile?.delete()
                                        recordedAudioFile = null
                                    }
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            mediaRecorder?.stop()
                                            mediaRecorder?.release()
                                        } catch (_: Exception) {}
                                        mediaRecorder = null
                                        isRecordingVoice = false

                                        val file = recordedAudioFile
                                        if (file != null && file.exists()) {
                                            val durMs = recordingDurationSec * 1000L
                                            scope.launch {
                                                // Send voice note
                                                repository.sendMessage(
                                                    conversationId = conversationId,
                                                    text = "",
                                                    mediaUrl = file.absolutePath,
                                                    mediaType = "voice",
                                                    voiceDurationMs = durMs.coerceAtLeast(1000L)
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = bubbleColorPrimary)
                                ) {
                                    Text("Send Note")
                                }
                            }
                        }
                    }
                }

                // Input bar
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Options (+) button for Polls & Watch Together
                        var showAttachmentMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Attachments", tint = bubbleColorPrimary)
                            }

                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Create Poll") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        showPollDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Poll, contentDescription = null, tint = bubbleColorPrimary) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Watch Together") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        showWatchTogetherDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Tv, contentDescription = null, tint = VyraCyan) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        // Voice record / Send toggle
                        if (messageText.isBlank()) {
                            IconButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        // Start recording directly
                                        try {
                                            val output = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                                            val recorder = MediaRecorder().apply {
                                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                setOutputFile(output.absolutePath)
                                                prepare()
                                                start()
                                            }
                                            mediaRecorder = recorder
                                            recordedAudioFile = output
                                            isRecordingVoice = true
                                            recordingDurationSec = 0
                                        } catch (e: Exception) {
                                            android.util.Log.e("VyraVoice", "Mic start error: ${e.message}")
                                        }
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.testTag("chat_voice_record_button")
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Record Voice Note", tint = bubbleColorPrimary)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        val textToSend = messageText
                                        messageText = ""
                                        isSending = true
                                        scope.launch {
                                            repository.sendMessage(
                                                conversationId = conversationId,
                                                text = textToSend
                                            )
                                            isSending = false
                                        }
                                    }
                                },
                                enabled = messageText.isNotBlank() && !isSending,
                                modifier = Modifier.testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = bubbleColorPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // FEATURE 9: Chat Poll Creation Dialog
    if (showPollDialog) {
        CreatePollDialog(
            onDismiss = { showPollDialog = false },
            onCreatePoll = { question, options ->
                showPollDialog = false
                scope.launch {
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = "Poll: $question",
                        mediaType = "poll",
                        pollQuestion = question,
                        pollOptions = options
                    )
                }
            }
        )
    }

    // FEATURE 11: Watch Together Dialog
    if (showWatchTogetherDialog) {
        AlertDialog(
            onDismissRequest = { showWatchTogetherDialog = false },
            title = { Text("Watch Together", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Invite friends in this chat to watch a video together in real-time with synchronized playback and live reactions.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Starts a shared viewing room for: $chatTitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VyraCyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWatchTogetherDialog = false
                        onStartWatchTogether("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VyraCyan)
                ) {
                    Text("Start Watch Session", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatchTogetherDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * FEATURE 9: Poll Message Bubble with Live Voting
 */
@Composable
fun PollMessageCard(
    pollQuestion: String,
    pollOptions: List<String>,
    pollVotes: Map<String, List<String>>,
    currentUserId: String,
    bubbleColor: Color,
    onVote: (Int) -> Unit
) {
    val totalVotes = pollVotes.values.sumOf { it.size }.coerceAtLeast(1)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, bubbleColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .widthIn(min = 240.dp, max = 320.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Poll, contentDescription = null, tint = bubbleColor, modifier = Modifier.size(18.dp))
                Text("Chat Poll", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = bubbleColor)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pollQuestion,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            pollOptions.forEachIndexed { index, option ->
                val voters = pollVotes[index.toString()] ?: emptyList()
                val count = voters.size
                val percentage = if (totalVotes > 0) (count * 100) / totalVotes else 0
                val hasVotedThis = currentUserId in voters

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onVote(index) }
                        .background(if (hasVotedThis) bubbleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hasVotedThis) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = bubbleColor, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (hasVotedThis) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Text(
                            text = "$percentage% ($count)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = bubbleColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${pollVotes.values.sumOf { it.size }} total votes",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * FEATURE 10: Voice Message Card with Play/Pause and Elapsed Timeline
 */
@Composable
fun VoiceMessageCard(
    audioUrl: String,
    durationMs: Long,
    isMine: Boolean,
    bubbleColor: Color
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableStateOf(0L) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(audioUrl) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(200)
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            currentPosMs = player.currentPosition.toLong()
                        } else {
                            isPlaying = false
                            currentPosMs = 0L
                        }
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                }
            }
        }
    }

    Surface(
        color = if (isMine) bubbleColor else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(min = 220.dp, max = 290.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        try {
                            if (mediaPlayer == null) {
                                val player = MediaPlayer()
                                player.setDataSource(audioUrl)
                                player.prepare()
                                player.setOnCompletionListener {
                                    isPlaying = false
                                    currentPosMs = 0L
                                }
                                mediaPlayer = player
                            }
                            mediaPlayer?.start()
                            isPlaying = true
                        } catch (e: Exception) {
                            android.util.Log.e("VyraVoice", "Voice playback error: ${e.message}")
                            isPlaying = false
                        }
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMine) Color.White.copy(alpha = 0.25f) else bubbleColor.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isMine) Color.White else bubbleColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val totalSec = (durationMs / 1000).coerceAtLeast(1)
                val currSec = currentPosMs / 1000

                // Simulated audio waveform / slider
                LinearProgressIndicator(
                    progress = { if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isMine) Color.White else bubbleColor,
                    trackColor = if (isMine) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${currSec / 60}:${String.format("%02d", currSec % 60)}",
                        fontSize = 11.sp,
                        color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${totalSec / 60}:${String.format("%02d", totalSec % 60)}",
                        fontSize = 11.sp,
                        color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * FEATURE 9: Dialog to create a new Poll inside chat
 */
@Composable
fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (question: String, options: List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Poll", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("Ask a question...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Poll Options (Min 2, Max 6):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                options.forEachIndexed { index, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = opt,
                            onValueChange = { options[index] = it },
                            placeholder = { Text("Option ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (options.size > 2) {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove option", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (options.size < 6) {
                    TextButton(
                        onClick = { options.add("") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validOpts = options.map { it.trim() }.filter { it.isNotBlank() }
                    if (question.isNotBlank() && validOpts.size >= 2) {
                        onCreatePoll(question.trim(), validOpts)
                    }
                },
                enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= 2
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to start a 1-on-1 or group conversation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(
    currentUserId: String,
    allUsers: List<UserProfile>,
    repository: VyraRepository,
    onDismiss: () -> Unit,
    onConversationCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isGroupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }
    var isCreating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isGroupMode) "New Group Chat (Max 50)" else "New Direct Message")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isGroupMode,
                        onClick = { isGroupMode = false; selectedUserIds.clear() },
                        label = { Text("1-on-1 Chat") }
                    )
                    FilterChip(
                        selected = isGroupMode,
                        onClick = { isGroupMode = true },
                        label = { Text("Group Chat") }
                    )
                }

                if (isGroupMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Members:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(allUsers, key = { it.userId }) { user ->
                        val isSelected = user.userId in selectedUserIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isGroupMode) {
                                        if (isSelected) selectedUserIds.remove(user.userId)
                                        else if (selectedUserIds.size < 49) selectedUserIds.add(user.userId)
                                    } else {
                                        selectedUserIds.clear()
                                        selectedUserIds.add(user.userId)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (isGroupMode) {
                                        if (checked == true && selectedUserIds.size < 49) selectedUserIds.add(user.userId)
                                        else selectedUserIds.remove(user.userId)
                                    } else {
                                        selectedUserIds.clear()
                                        if (checked == true) selectedUserIds.add(user.userId)
                                    }
                                }
                            )

                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    VyraUserBadgeRow(badges = user.badges, size = 12.dp)
                                }
                                Text("@${user.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUserIds.isNotEmpty()) {
                        isCreating = true
                        scope.launch {
                            val convId = if (isGroupMode) {
                                repository.createGroupConversation(
                                    name = groupName.ifBlank { "Group" },
                                    memberIds = selectedUserIds.toList()
                                )
                            } else {
                                val targetUser = allUsers.first { it.userId == selectedUserIds[0] }
                                repository.getOrCreateDirectConversation(targetUser)
                            }
                            isCreating = false
                            onConversationCreated(convId)
                        }
                    }
                },
                enabled = selectedUserIds.isNotEmpty() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text(if (isGroupMode) "Create Group" else "Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
