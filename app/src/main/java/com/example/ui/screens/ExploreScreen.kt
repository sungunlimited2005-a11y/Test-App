package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val TRENDING_HASHTAGS = listOf(
    "all", "vyra", "photography", "design", "cinematic", "cyberpunk", "minimal", "reels", "travel"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    currentUserId: String,
    repository: VyraRepository,
    onNavigateToProfile: (String) -> Unit,
    onSelectPost: (Post) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("all") }

    val allPosts by repository.observeFeedPosts().collectAsState(initial = emptyList())
    val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())

    val filteredUsers = remember(searchQuery, allUsers, blockedUsers) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase().removePrefix("@")
            allUsers.filter { user ->
                user.userId !in blockedUsers &&
                (user.username.contains(q, ignoreCase = true) || user.displayName.contains(q, ignoreCase = true))
            }
        }
    }

    val filteredPosts = remember(searchQuery, selectedTag, allPosts, blockedUsers) {
        allPosts.filter { post ->
            post.userId !in blockedUsers &&
            (selectedTag == "all" || post.hashtags.any { it.equals(selectedTag, ignoreCase = true) }) &&
            (searchQuery.isBlank() ||
             post.caption.contains(searchQuery, ignoreCase = true) ||
             post.username.contains(searchQuery, ignoreCase = true) ||
             post.hashtags.any { it.contains(searchQuery.removePrefix("#"), ignoreCase = true) })
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search users, #hashtags, creators...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("explore_search_input"),
                shape = RoundedCornerShape(24.dp)
            )

            // If user is searching and matches users, show search results dropdown/list
            if (searchQuery.isNotBlank() && filteredUsers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Creators",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(filteredUsers, key = { it.userId }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToProfile(user.userId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = user.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    com.example.ui.components.VyraUserBadgeRow(badges = user.badges, size = 13.dp)
                                }
                                Text(
                                    text = "@${user.username}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (user.isPrivate) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Private",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Hashtag Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TRENDING_HASHTAGS) { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = tag },
                            label = { Text(if (tag == "all") "All" else "#$tag") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VyraViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Discover Creators Row (when "all" tag is selected and not searching)
                if (selectedTag == "all" && searchQuery.isBlank() && allUsers.size > 1) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Discover Creators",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val otherUsers = allUsers.filter { it.userId != currentUserId && it.userId !in blockedUsers }
                            items(otherUsers, key = { it.userId }) { creator ->
                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable { onNavigateToProfile(creator.userId) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AsyncImage(
                                            model = creator.avatarUrl,
                                            contentDescription = creator.username,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = creator.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${creator.username}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Explore Grid
                if (filteredPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts found for this filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("explore_grid"),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredPosts, key = { it.id }) { post ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { onSelectPost(post) }
                            ) {
                                AsyncImage(
                                    model = post.mediaUrl,
                                    contentDescription = post.caption,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                if (post.isReel) {
                                    Icon(
                                        imageVector = Icons.Filled.Movie,
                                        contentDescription = "Reel",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(16.dp)
                                    )
                                }

                                if (post.likesCount > 0) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "${post.likesCount}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
