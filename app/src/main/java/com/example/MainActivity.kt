package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.VyraButtonGradient
import com.example.ui.theme.VyraTheme
import com.example.ui.theme.VyraViolet
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface VyraDestination {
    data class MainTab(val tab: VyraNavTab) : VyraDestination
    data class UserProfileView(val userId: String) : VyraDestination
    data object Notifications : VyraDestination
    data object Settings : VyraDestination
    data object CreatorStudio : VyraDestination
    data object AdminVerification : VyraDestination
    data object CloseFriends : VyraDestination
    data object BlockedUsers : VyraDestination
    data object MutedUsers : VyraDestination
    data object RestrictedUsers : VyraDestination
    data object Moments : VyraDestination
    data object Playlists : VyraDestination
    data class WatchTogether(val videoUrl: String = "", val sessionId: String = "") : VyraDestination
    data class ProfileInsights(val userId: String) : VyraDestination
    data object LivingProfileCustomizer : VyraDestination
    data object About : VyraDestination
    data class ChatThread(val conversationId: String) : VyraDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VyraTheme {
                AppRoot()
            }
        }
    }
}

/**
 * FEATURE 7: Intro / Splash Screen and Authentication Root
 * Clearly displays: VYRA and Created by Gogi
 */
@Composable
fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (showSplash) {
        // Splash Intro Screen: VYRA — Created by Gogi
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F14)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(VyraButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "VYRA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = VyraViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Created by Gogi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC4B5FD),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedApp(currentUserId = uid)
        }
    }
}

@Composable
fun AuthenticatedApp(currentUserId: String) {
    val context = LocalContext.current
    val repository = remember(currentUserId) { VyraRepository(context) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        repository.observeUserProfile(currentUserId).collect { p ->
            userProfile = p
            isProfileLoaded = true
        }
    }

    if (!isProfileLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            VyraLogo(size = 48.dp)
        }
    } else if (userProfile == null || userProfile?.username.isNullOrBlank()) {
        ProfileSetupScreen(
            currentUserId = currentUserId,
            repository = repository,
            onSetupComplete = {
                // profile saved; observeUserProfile listener will automatically pick it up
            }
        )
    } else {
        VyraMainScaffold(
            currentUserId = currentUserId,
            currentUser = userProfile!!,
            repository = repository
        )
    }
}

@Composable
fun VyraMainScaffold(
    currentUserId: String,
    currentUser: UserProfile,
    repository: VyraRepository
) {
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf<VyraDestination>(VyraDestination.MainTab(VyraNavTab.HOME)) }

    val notifications by repository.observeNotifications().collectAsState(initial = emptyList())
    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    val conversations by repository.observeUserConversations().collectAsState(initial = emptyList())
    val unreadMessagesCount = 0

    Scaffold(
        topBar = {
            when (val dest = currentDestination) {
                is VyraDestination.MainTab -> {
                    if (dest.tab != VyraNavTab.REELS && dest.tab != VyraNavTab.MESSAGES) {
                        VyraTopAppBar(
                            unreadNotifications = unreadNotificationsCount,
                            unreadMessages = unreadMessagesCount,
                            onNotificationsClick = {
                                currentDestination = VyraDestination.Notifications
                            },
                            onMessagesClick = {
                                currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                            }
                        )
                    }
                }
                else -> {
                    // Secondary screens define their own custom top app bar
                }
            }
        },
        bottomBar = {
            // FIX BUG 4: Keep bottom navigation accessible across all main tabs (including REELS)
            if (currentDestination is VyraDestination.MainTab) {
                VyraBottomNavigation(
                    selectedTab = (currentDestination as VyraDestination.MainTab).tab,
                    onTabSelected = { tab ->
                        currentDestination = VyraDestination.MainTab(tab)
                    },
                    userAvatarUrl = currentUser.avatarUrl,
                    unreadMessages = unreadMessagesCount
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val dest = currentDestination) {
                is VyraDestination.MainTab -> {
                    when (dest.tab) {
                        VyraNavTab.HOME -> {
                            FeedScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateToProfile = { targetUid ->
                                    currentDestination = VyraDestination.UserProfileView(targetUid)
                                },
                                onNavigateToCreate = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.CREATE)
                                },
                                onNavigateToMessages = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                                },
                                onNavigateToMoments = {
                                    currentDestination = VyraDestination.Moments
                                },
                                onNavigateToSound = {
                                    currentDestination = VyraDestination.Playlists
                                },
                                onSharePostToChat = { post ->
                                    scope.launch {
                                        val convs = conversations
                                        if (convs.isNotEmpty()) {
                                            repository.sendMessage(
                                                conversationId = convs.first().id,
                                                text = "Check out this post on VYRA: ${post.caption}",
                                                mediaUrl = post.mediaUrl,
                                                sharedPostId = post.id
                                            )
                                            currentDestination = VyraDestination.ChatThread(convs.first().id)
                                        } else {
                                            currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                                        }
                                    }
                                }
                            )
                        }
                        VyraNavTab.EXPLORE -> {
                            ExploreScreen(
                                currentUserId = currentUserId,
                                repository = repository,
                                onNavigateToProfile = { targetUid ->
                                    currentDestination = VyraDestination.UserProfileView(targetUid)
                                },
                                onSelectPost = { post ->
                                    currentDestination = VyraDestination.UserProfileView(post.userId)
                                }
                            )
                        }
                        VyraNavTab.CREATE -> {
                            CreateScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onPublishSuccess = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                                }
                            )
                        }
                        VyraNavTab.REELS -> {
                            ReelsScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateToProfile = { targetUid ->
                                    currentDestination = VyraDestination.UserProfileView(targetUid)
                                },
                                onNavigateToCreate = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.CREATE)
                                },
                                onSharePost = { post ->
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                                },
                                onNavigateBack = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                                }
                            )
                        }
                        VyraNavTab.MESSAGES -> {
                            MessagesScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onStartWatchSession = { vUrl ->
                                    currentDestination = VyraDestination.WatchTogether(videoUrl = vUrl)
                                },
                                onNavigateBack = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                                }
                            )
                        }
                        VyraNavTab.PROFILE -> {
                            ProfileScreen(
                                profileUserId = currentUserId,
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateToSettings = {
                                    currentDestination = VyraDestination.Settings
                                },
                                onNavigateToCreatorStudio = {
                                    currentDestination = VyraDestination.CreatorStudio
                                },
                                onNavigateToCustomizer = {
                                    currentDestination = VyraDestination.LivingProfileCustomizer
                                },
                                onNavigateToInsights = { uid ->
                                    currentDestination = VyraDestination.ProfileInsights(uid)
                                },
                                onSelectPost = { post ->
                                    // viewing post
                                },
                                onNavigateToUser = { uid ->
                                    currentDestination = VyraDestination.UserProfileView(uid)
                                }
                            )
                        }
                    }
                }

                is VyraDestination.UserProfileView -> {
                    ProfileScreen(
                        profileUserId = dest.userId,
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                        },
                        onNavigateToSettings = {
                            currentDestination = VyraDestination.Settings
                        },
                        onNavigateToCreatorStudio = {
                            currentDestination = VyraDestination.CreatorStudio
                        },
                        onNavigateToCustomizer = {
                            currentDestination = VyraDestination.LivingProfileCustomizer
                        },
                        onNavigateToInsights = { uid ->
                            currentDestination = VyraDestination.ProfileInsights(uid)
                        },
                        onSelectPost = { post ->
                            // viewing post
                        },
                        onNavigateToUser = { uid ->
                            if (uid != dest.userId) {
                                currentDestination = VyraDestination.UserProfileView(uid)
                            }
                        }
                    )
                }

                is VyraDestination.Notifications -> {
                    NotificationsScreen(
                        currentUserId = currentUserId,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                        },
                        onNavigateToProfile = { targetUid ->
                            currentDestination = VyraDestination.UserProfileView(targetUid)
                        }
                    )
                }

                is VyraDestination.Settings -> {
                    SettingsScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.PROFILE)
                        },
                        onNavigateToAdminVerification = {
                            currentDestination = VyraDestination.AdminVerification
                        },
                        onNavigateToCloseFriends = {
                            currentDestination = VyraDestination.CloseFriends
                        },
                        onNavigateToBlockedUsers = {
                            currentDestination = VyraDestination.BlockedUsers
                        },
                        onNavigateToMutedUsers = {
                            currentDestination = VyraDestination.MutedUsers
                        },
                        onNavigateToRestrictedUsers = {
                            currentDestination = VyraDestination.RestrictedUsers
                        },
                        onNavigateToAbout = {
                            currentDestination = VyraDestination.About
                        },
                        onSignOutComplete = {
                            // Automatically triggers authStateListener and navigates to AuthScreen
                        }
                    )
                }

                is VyraDestination.AdminVerification -> {
                    AdminVerificationScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.Settings
                        }
                    )
                }

                is VyraDestination.CloseFriends -> {
                    CloseFriendsScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.Settings
                        }
                    )
                }

                is VyraDestination.BlockedUsers -> {
                    BlockedUsersScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.Settings
                        }
                    )
                }

                is VyraDestination.MutedUsers -> {
                    MutedUsersScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.Settings
                        }
                    )
                }

                is VyraDestination.RestrictedUsers -> {
                    RestrictedUsersScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.Settings
                        }
                    )
                }

                is VyraDestination.Moments -> {
                    MomentsScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                        }
                    )
                }

                is VyraDestination.Playlists -> {
                    VyraSoundScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                        }
                    )
                }

                is VyraDestination.WatchTogether -> {
                    WatchTogetherScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        initialSessionId = dest.sessionId.ifBlank { null },
                        initialVideoUrl = dest.videoUrl.ifBlank { null },
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                        }
                    )
                }

                is VyraDestination.ProfileInsights -> {
                    ProfileInsightsScreen(
                        userId = dest.userId,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.PROFILE)
                        }
                    )
                }

                is VyraDestination.LivingProfileCustomizer -> {
                    LivingProfileCustomizerScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.PROFILE)
                        }
                    )
                }

                is VyraDestination.About -> {
                    AboutScreen(
                        onNavigateBack = {
                            currentDestination = VyraDestination.Settings
                        }
                    )
                }

                is VyraDestination.CreatorStudio -> {
                    CreatorStudioScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.PROFILE)
                        }
                    )
                }

                is VyraDestination.ChatThread -> {
                    MessagesScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        initialConversationId = dest.conversationId,
                        onStartWatchSession = { vUrl ->
                            currentDestination = VyraDestination.WatchTogether(videoUrl = vUrl)
                        },
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                        }
                    )
                }
            }
        }
    }
}
