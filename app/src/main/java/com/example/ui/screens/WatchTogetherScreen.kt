package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.UserProfile
import com.example.model.WatchSession
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchTogetherScreen(
    currentUserId: String,
    currentUser: UserProfile,
    repository: VyraRepository,
    initialSessionId: String? = null,
    initialVideoUrl: String? = null,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeSessionId by remember { mutableStateOf(initialSessionId) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newSessionTitle by remember { mutableStateOf("") }
    var newSessionUrl by remember {
        mutableStateOf(
            initialVideoUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        )
    }

    BackHandler {
        if (activeSessionId != null) {
            scope.launch {
                repository.leaveWatchSession(activeSessionId!!)
            }
        }
        onNavigateBack()
    }

    val watchSession by remember(activeSessionId) {
        if (!activeSessionId.isNullOrBlank()) {
            repository.observeWatchSession(activeSessionId!!)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    // Automatically join session when active
    LaunchedEffect(activeSessionId) {
        activeSessionId?.let { sId ->
            repository.joinWatchSession(sId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeSessionId != null) {
                                scope.launch { repository.leaveWatchSession(activeSessionId!!) }
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("watch_together_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Watch Together", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Surface(
                                color = VyraMagenta.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("LIVE ROOM", color = VyraMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (watchSession != null) {
                            Text(watchSession?.title ?: "Party", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    if (activeSessionId != null) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("VYRA Watch Session ID", activeSessionId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Session ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("share_watch_room_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Room", tint = VyraCyan)
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
                .background(Color(0xFF0F0F14))
        ) {
            if (activeSessionId == null || watchSession == null) {
                // Lobby View
                WatchLobbyView(
                    onCreateParty = { showCreateDialog = true },
                    onJoinParty = { showJoinDialog = true }
                )
            } else {
                // Active Synchronized Watch Party View
                WatchPartyRoomView(
                    session = watchSession!!,
                    currentUserId = currentUserId,
                    onTogglePlay = { isPlaying, pos ->
                        scope.launch {
                            repository.updateWatchPlayback(activeSessionId!!, isPlaying, pos)
                        }
                    },
                    onSendReaction = { emoji ->
                        scope.launch {
                            repository.sendWatchReaction(activeSessionId!!, emoji)
                        }
                    },
                    onLeaveRoom = {
                        scope.launch {
                            repository.leaveWatchSession(activeSessionId!!)
                            activeSessionId = null
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Start Watch Party") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Watch videos and reels together in real time with synchronized playback and live emoji reactions.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = newSessionTitle,
                        onValueChange = { newSessionTitle = it },
                        label = { Text("Room Title") },
                        placeholder = { Text("e.g. Movie Night, Highlight Reel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_watch_room_title")
                    )
                    OutlinedTextField(
                        value = newSessionUrl,
                        onValueChange = { newSessionUrl = it },
                        label = { Text("Video Stream URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_watch_video_url")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSessionUrl.isNotBlank()) {
                            scope.launch {
                                val sId = repository.createWatchSession(
                                    title = newSessionTitle.ifBlank { "VYRA Watch Party" },
                                    videoUrl = newSessionUrl.trim()
                                )
                                activeSessionId = sId
                                showCreateDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_create_watch_party_button")
                ) {
                    Text("Create Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Watch Party") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the 20-character session code shared by your friend:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it },
                        label = { Text("Session ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_join_watch_code")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCodeInput.isNotBlank()) {
                            activeSessionId = joinCodeInput.trim()
                            showJoinDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_join_watch_party_button")
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WatchLobbyView(
    onCreateParty: () -> Unit,
    onJoinParty: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(VyraMagenta.copy(alpha = 0.4f), VyraViolet.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
                .border(2.dp, VyraMagenta, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Watch Together",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sync video playback in real time with friends. React with floating emojis, laugh together, and enjoy shared media.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateParty,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("button_host_watch_party"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Host a Watch Party", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onJoinParty,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("button_join_existing_party"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Group, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Join via Session Code", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun WatchPartyRoomView(
    session: WatchSession,
    currentUserId: String,
    onTogglePlay: (Boolean, Long) -> Unit,
    onSendReaction: (String) -> Unit,
    onLeaveRoom: () -> Unit
) {
    val isHost = session.hostId == currentUserId
    var localPositionMs by remember { mutableLongStateOf(session.currentPositionMs) }

    // Client-side playback clock simulation when playing
    LaunchedEffect(session.isPlaying) {
        if (session.isPlaying) {
            while (true) {
                delay(1000)
                localPositionMs += 1000
            }
        }
    }

    LaunchedEffect(session.currentPositionMs) {
        localPositionMs = session.currentPositionMs
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Video Preview / Theater Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black)
        ) {
            // Simulated / Stream Video Surface
            AsyncImage(
                model = if (session.videoUrl.contains("http")) session.videoUrl else "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                contentDescription = session.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // Center play/pause indicator
            IconButton(
                onClick = {
                    if (isHost) {
                        onTogglePlay(!session.isPlaying, localPositionMs)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("watch_play_pause_button")
            ) {
                Icon(
                    imageVector = if (session.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (session.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Sync Status Badge
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (session.isPlaying) VyraGreen else Color.Gray)
                    )
                    Text(
                        text = if (session.isPlaying) "SYNCED • PLAYING" else "PAUSED",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Floating Emoji Reactions overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    session.reactions.takeLast(4).forEach { rxn ->
                        val parts = rxn.split(":")
                        val emoji = parts.getOrNull(1) ?: "❤️"
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        // Room Controls Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(session.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Hosted by @${session.hostUsername} • ${session.participantIds.size} watching",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onLeaveRoom,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("leave_watch_party_button")
                    ) {
                        Text("Leave", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playback Position Bar
                val seconds = (localPositionMs / 1000) % 60
                val minutes = (localPositionMs / (1000 * 60))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        String.format("%02d:%02d", minutes, seconds),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { (localPositionMs % 180000L).toFloat() / 180000f },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = VyraMagenta
                    )
                    Text("03:00", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (!isHost) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Host controls video playback. Your stream stays locked to the host.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reactions Toolbar
        Text(
            "Send Live Reaction",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val emojiSet = listOf("❤️", "🔥", "😂", "👏", "🍿", "🚀", "🎉", "😱")
            items(emojiSet) { emoji ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clickable { onSendReaction(emoji) }
                        .testTag("reaction_button_$emoji")
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Participants header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Party Members (${session.participantIds.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(color = VyraCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    "SYNC ID: ${session.id.take(8)}",
                    fontSize = 10.sp,
                    color = VyraCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(session.participantIds) { pId ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(VyraViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pId == session.hostId) "★" else pId.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (pId == currentUserId) "You" else if (pId == session.hostId) "Host" else "Friend",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
