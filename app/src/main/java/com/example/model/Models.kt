package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val websiteUrl: String = "",
    val avatarUrl: String = "",
    val isPrivate: Boolean = false,
    val themePreference: String = "dark",
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val badges: List<String> = emptyList(), // "developer", "influencer"
    val isAdmin: Boolean = false,
    val showOnlineStatus: Boolean = true,
    val hideLikeCounts: Boolean = false,
    val mutedUserIds: List<String> = emptyList(),
    val restrictedUserIds: List<String> = emptyList(),
    val profileTheme: String = "default", // default, cyberpunk, obsidian, sunset, emerald, frost
    val bannerStyle: String = "gradient", // gradient, mesh, geometric, minimal
    val cardBorderStyle: String = "neon", // neon, rounded, glass, sleek
    val pinnedPostIds: List<String> = emptyList(),
    val sectionOrder: List<String> = emptyList(),
    val bioLinks: List<BioLink> = emptyList(),
    val profileVisitsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BioLink(
    val title: String = "",
    val url: String = ""
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image" or "video"
    val contentType: String = "POST", // "POST" or "REEL"
    val isReel: Boolean = false,
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val isPrivate: Boolean = false,
    val allowComments: Boolean = true,
    val isPinned: Boolean = false,
    val musicTitle: String = "",
    val musicArtist: String = "",
    val musicUrl: String = "",
    val spotifyUrl: String = "",
    val originalAudioVolume: Float = 1.0f,
    val musicVolume: Float = 1.0f,
    val isOriginalAudioMuted: Boolean = false,
    val isMusicMuted: Boolean = false,
    val authorBadges: List<String> = emptyList(),
    val collaboratorUserId: String = "",
    val collaboratorUsername: String = "",
    val collaboratorStatus: String = "none", // none, pending, accepted, rejected
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val text: String = "",
    val userBadges: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val caption: String = "",
    val isCloseFriendsOnly: Boolean = false,
    val viewersCount: Long = 0L,
    val authorBadges: List<String> = emptyList(),
    val questionPrompt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoryHighlight(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val storyIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val name: String = "",
    val avatarUrl: String = "",
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val theme: String = "default", // default, cyberpunk, midnight_blue, sunset, emerald
    val wallpaperUrl: String = "",
    val unreadCounts: Map<String, Long> = emptyMap(),
    val mutedUserIds: List<String> = emptyList(),
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderAvatarUrl: String = "",
    val senderBadges: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "text", // text, image, video, voice, poll, post
    val sharedPostId: String = "",
    val voiceDurationMs: Long = 0L,
    val pollQuestion: String = "",
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<String, List<String>> = emptyMap(), // optionIndex string -> list of userIds
    val pollExpiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VyraMoment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val authorBadges: List<String> = emptyList(),
    val text: String = "",
    val mediaUrl: String = "",
    val mood: String = "", // e.g. "⚡ Focused", "☕ Chilling", "✨ Inspired", "🔥 Creating", "🎧 Vibing"
    val musicTitle: String = "",
    val musicArtist: String = "",
    val musicUrl: String = "",
    val privacy: String = "everyone", // everyone, followers, close_friends
    val likesCount: Long = 0L,
    val repliesCount: Long = 0L,
    val likedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MomentReply(
    @DocumentId val id: String = "",
    val momentId: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val userBadges: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlaylistTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val durationMs: Long = 0L,
    val spotifyUrl: String = ""
)

@IgnoreExtraProperties
data class MusicPlaylist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorUsername: String = "",
    val title: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val isPublic: Boolean = true,
    val tracks: List<PlaylistTrack> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuestionSticker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val prompt: String = "Ask me anything!",
    val storyId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuestionAnswer(
    @DocumentId val id: String = "",
    val stickerId: String = "",
    val prompt: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val isPublic: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WatchSession(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostUsername: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val participantIds: List<String> = emptyList(),
    val reactions: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ProfileInsightsData(
    val profileVisits: Long = 0L,
    val postReach: Long = 0L,
    val reelViews: Long = 0L,
    val followersGained: Long = 0L,
    val engagementRate: Double = 0.0,
    val topPosts: List<Post> = emptyList()
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val actorAvatarUrl: String = "",
    val actorBadges: List<String> = emptyList(),
    val type: String = "like", // like, comment, follow, follow_request, message, verification
    val targetId: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val caption: String = "",
    val authorUsername: String = "",
    val authorBadges: List<String> = emptyList(),
    val isReel: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val badges: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRequest(
    @DocumentId val id: String = "",
    val requesterUserId: String = "",
    val targetUserId: String = "",
    val requesterUsername: String = "",
    val requesterDisplayName: String = "",
    val requesterAvatarUrl: String = "",
    val requesterBadges: List<String> = emptyList(),
    val status: String = "pending", // pending, accepted, rejected
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VerificationRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val badgeType: String = "developer", // "developer" or "influencer"
    val status: String = "pending", // "pending", "approved", "rejected"
    // Developer application fields:
    val developerName: String = "",
    val experienceYears: String = "",
    // Influencer application fields:
    val channelName: String = "",
    val channelUrl: String = "",
    // Backwards compatibility fields:
    val proofLink: String = "",
    val details: String = "",
    val platformOrExperience: String = "",
    val category: String = "",
    val rejectionReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DraftItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val caption: String = "",
    val mediaUri: String = "",
    val mediaType: String = "image",
    val isReel: Boolean = false,
    val hashtags: List<String> = emptyList(),
    val musicTitle: String = "",
    val musicArtist: String = "",
    val musicTrackUrl: String = "",
    val spotifyUrl: String = "",
    val trimStartSeconds: Float = 0f,
    val trimEndSeconds: Float = 0f,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MusicTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val coverUrl: String = "",
    val audioUrl: String = "",
    val durationSeconds: Int = 30,
    val genre: String = "Electronic",
    val spotifySharingUrl: String = ""
)

@IgnoreExtraProperties
data class ReportItem(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post", // user, post, comment, message
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null
)
