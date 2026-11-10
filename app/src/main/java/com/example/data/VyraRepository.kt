package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VyraRepository(
    private val db: FirebaseFirestore,
    val context: Context? = null
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth get() = Firebase.auth
    private val storage get() = Firebase.storage

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing this action.")
    }

    // --- User Profiles ---

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) doc.toObject(UserProfile::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "username" to profile.username.trim().lowercase(),
            "displayName" to profile.displayName.trim(),
            "email" to (auth.currentUser?.email ?: profile.email),
            "bio" to profile.bio,
            "websiteUrl" to profile.websiteUrl,
            "avatarUrl" to profile.avatarUrl,
            "isPrivate" to profile.isPrivate,
            "themePreference" to profile.themePreference,
            "postsCount" to profile.postsCount,
            "followersCount" to profile.followersCount,
            "followingCount" to profile.followingCount,
            "badges" to profile.badges,
            "isAdmin" to profile.isAdmin,
            "showOnlineStatus" to profile.showOnlineStatus,
            "hideLikeCounts" to profile.hideLikeCounts,
            "mutedUserIds" to profile.mutedUserIds,
            "restrictedUserIds" to profile.restrictedUserIds,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (profile.createdAt == null) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            val userEmail = auth.currentUser?.email ?: profile.email
            reserveUsername(profile.username.trim().lowercase(), uid, userEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getEmailForUsername(username: String): String? {
        val clean = username.trim().lowercase().removePrefix("@")
        val path = "usernames/$clean"
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            if (doc.exists() && !doc.getString("email").isNullOrBlank()) {
                doc.getString("email")
            } else {
                // Fallback: check users collection by username
                val userDocs = db.collection("users")
                    .whereEqualTo("username", clean)
                    .limit(1)
                    .get()
                    .await()
                if (!userDocs.isEmpty) {
                    userDocs.documents[0].getString("email")
                } else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun getUserIdForUsername(username: String): String? {
        val clean = username.trim().lowercase().removePrefix("@")
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            if (doc.exists() && !doc.getString("userId").isNullOrBlank()) {
                doc.getString("userId")
            } else {
                val userDocs = db.collection("users")
                    .whereEqualTo("username", clean)
                    .limit(1)
                    .get()
                    .await()
                if (!userDocs.isEmpty) {
                    userDocs.documents[0].id
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        val clean = username.trim().lowercase().removePrefix("@")
        if (clean.length < 3) return false
        val path = "usernames/$clean"
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            if (!doc.exists()) return true
            val claimedBy = doc.getString("userId")
            claimedBy == currentUserId()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun reserveUsername(username: String, userId: String, email: String) {
        val clean = username.trim().lowercase().removePrefix("@")
        val docRef = db.collection("usernames").document(clean)
        val payload = mapOf(
            "username" to clean,
            "userId" to userId,
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(UserProfile::class.java) }

    // --- Posts & Reels ---

    suspend fun createPost(
        mediaUrl: String,
        mediaType: String,
        caption: String,
        hashtags: List<String>,
        isReel: Boolean,
        isPrivate: Boolean,
        allowComments: Boolean,
        musicTitle: String = "",
        musicArtist: String = "",
        musicUrl: String = "",
        spotifyUrl: String = "",
        contentType: String = if (isReel) "REEL" else "POST",
        originalAudioVolume: Float = 1.0f,
        musicVolume: Float = 1.0f,
        isOriginalAudioMuted: Boolean = false,
        isMusicMuted: Boolean = false
    ): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val docRef = db.collection("posts").document()
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "authorBadges" to (user?.badges ?: emptyList<String>()),
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "contentType" to contentType,
            "isReel" to isReel,
            "caption" to caption,
            "hashtags" to hashtags,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "viewsCount" to 0L,
            "isPrivate" to isPrivate,
            "allowComments" to allowComments,
            "isPinned" to false,
            "musicTitle" to musicTitle,
            "musicArtist" to musicArtist,
            "musicUrl" to musicUrl,
            "spotifyUrl" to spotifyUrl,
            "originalAudioVolume" to originalAudioVolume,
            "musicVolume" to musicVolume,
            "isOriginalAudioMuted" to isOriginalAudioMuted,
            "isMusicMuted" to isMusicMuted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            // increment user posts count
            db.collection("users").document(uid).update("postsCount", FieldValue.increment(1)).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePostCaption(postId: String, newCaption: String, allowComments: Boolean = true) {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        try {
            postRef.update(
                mapOf(
                    "caption" to newCaption.trim(),
                    "allowComments" to allowComments,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postRef.path)
            throw e
        }
    }

    fun observeFeedPosts(): Flow<List<Post>> {
        val uid = auth.currentUser?.uid
        val mutedFlow = if (uid != null) {
            db.collection("users").document(uid).snapshots()
                .map { (it.get("mutedUserIds") as? List<String>) ?: emptyList() }
                .catch { emit(emptyList()) }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return combine(db.collection("posts").snapshots(), mutedFlow) { snap, mutedIds ->
            snap.toObjects(Post::class.java)
                .filter { !it.isReel && it.contentType != "REEL" && it.userId !in mutedIds }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }.catch { emit(emptyList()) }
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> =
        db.collection("posts")
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .filter {
                        !it.isReel && it.contentType != "REEL" &&
                        (it.userId == userId || (it.collaboratorUserId == userId && it.collaboratorStatus == "accepted"))
                    }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }

    fun observeUserReels(userId: String): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .filter { it.isReel || it.contentType == "REEL" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeReels(): Flow<List<Post>> =
        db.collection("posts")
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .filter { it.isReel || it.contentType == "REEL" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun toggleLikePost(post: Post, isLiked: Boolean) {
        val uid = requireUserId()
        val likeRef = db.collection("posts").document(post.id).collection("likes").document(uid)
        val postRef = db.collection("posts").document(post.id)
        try {
            if (isLiked) {
                // remove like
                likeRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            } else {
                // add like
                val payload = mapOf(
                    "userId" to uid,
                    "postId" to post.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                likeRef.set(payload).await()
                postRef.update("likesCount", FieldValue.increment(1)).await()

                if (post.userId != uid) {
                    val sender = getUserProfile(uid)
                    createNotification(
                        recipientUserId = post.userId,
                        type = "like",
                        targetId = post.id,
                        message = "${sender?.username ?: "Someone"} liked your post"
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            throw e
        }
    }

    fun isPostLiked(postId: String, userId: String): Flow<Boolean> =
        db.collection("posts").document(postId).collection("likes").document(userId)
            .snapshots()
            .map { it.exists() }

    suspend fun deletePost(postId: String) {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        try {
            postRef.delete().await()
            db.collection("users").document(uid).update("postsCount", FieldValue.increment(-1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            throw e
        }
    }

    suspend fun incrementViews(postId: String) {
        try {
            db.collection("posts").document(postId).update("viewsCount", FieldValue.increment(1)).await()
        } catch (ignored: Exception) {}
    }

    suspend fun togglePinPost(postId: String, isPinned: Boolean) {
        try {
            db.collection("posts").document(postId).update("isPinned", !isPinned).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
        }
    }

    // --- Comments ---

    suspend fun addComment(postId: String, postOwnerId: String, text: String) {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val commentsRef = db.collection("posts").document(postId).collection("comments").document()
        val payload = mapOf(
            "postId" to postId,
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "text" to text.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            commentsRef.set(payload).await()
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(1)).await()
            if (postOwnerId != uid) {
                createNotification(
                    recipientUserId = postOwnerId,
                    type = "comment",
                    targetId = postId,
                    message = "${user?.username ?: "Someone"} commented: \"${text.take(40)}\""
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentsRef.path)
            throw e
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> =
        db.collection("posts").document(postId).collection("comments")
            .snapshots()
            .map { snap ->
                snap.toObjects(Comment::class.java)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun deleteComment(postId: String, commentId: String) {
        val commentRef = db.collection("posts").document(postId).collection("comments").document(commentId)
        try {
            commentRef.delete().await()
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(-1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            throw e
        }
    }

    // --- Stories ---

    suspend fun publishStory(mediaUrl: String, mediaType: String, caption: String, isCloseFriendsOnly: Boolean): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val storyRef = db.collection("stories").document()
        val now = Timestamp.now()
        val expiresAt = Timestamp(now.seconds + 86400, now.nanoseconds) // 24 hours
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption,
            "isCloseFriendsOnly" to isCloseFriendsOnly,
            "viewersCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )
        try {
            storyRef.set(payload).await()
            return storyRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, storyRef.path)
            throw e
        }
    }

    fun observeActiveStories(): Flow<List<Story>> =
        db.collection("stories")
            .snapshots()
            .map { snap ->
                val nowSeconds = Timestamp.now().seconds
                snap.toObjects(Story::class.java)
                    .filter { (it.expiresAt?.seconds ?: Long.MAX_VALUE) > nowSeconds }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun markStoryViewed(storyId: String) {
        val uid = requireUserId()
        val viewerRef = db.collection("stories").document(storyId).collection("viewers").document(uid)
        try {
            val exists = viewerRef.get().await().exists()
            if (!exists) {
                viewerRef.set(mapOf("viewerUserId" to uid, "viewedAt" to FieldValue.serverTimestamp())).await()
                db.collection("stories").document(storyId).update("viewersCount", FieldValue.increment(1)).await()
            }
        } catch (ignored: Exception) {}
    }

    suspend fun deleteStory(storyId: String) {
        val storyRef = db.collection("stories").document(storyId)
        try {
            storyRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, storyRef.path)
        }
    }

    // --- Saved Content ---

    suspend fun savePost(post: Post) {
        val uid = requireUserId()
        val savedRef = db.collection("users").document(uid).collection("saved").document(post.id)
        val payload = mapOf(
            "userId" to uid,
            "postId" to post.id,
            "mediaUrl" to post.mediaUrl,
            "mediaType" to post.mediaType,
            "caption" to post.caption,
            "authorUsername" to post.username,
            "isReel" to post.isReel,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            savedRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, savedRef.path)
            throw e
        }
    }

    suspend fun unsavePost(postId: String) {
        val uid = requireUserId()
        val savedRef = db.collection("users").document(uid).collection("saved").document(postId)
        try {
            savedRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, savedRef.path)
            throw e
        }
    }

    fun isPostSaved(postId: String): Flow<Boolean> = flow {
        val uid = requireUserId()
        emit(db.collection("users").document(uid).collection("saved").document(postId).get().await().exists())
    }

    fun observeSavedPosts(): Flow<List<SavedPost>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("saved")
            .snapshots()
            .map { snap ->
                snap.toObjects(SavedPost::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    // --- Follows & Follow Requests ---

    suspend fun followUser(targetUser: UserProfile) {
        val uid = requireUserId()
        val currentUser = getUserProfile(uid)

        if (targetUser.isPrivate) {
            // Send follow request
            val reqRef = db.collection("users").document(targetUser.userId).collection("follow_requests").document(uid)
            val payload = mapOf(
                "requesterUserId" to uid,
                "targetUserId" to targetUser.userId,
                "requesterUsername" to (currentUser?.username ?: "user"),
                "requesterDisplayName" to (currentUser?.displayName ?: "User"),
                "requesterAvatarUrl" to (currentUser?.avatarUrl ?: ""),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            reqRef.set(payload).await()
            createNotification(
                recipientUserId = targetUser.userId,
                type = "follow_request",
                targetId = uid,
                message = "${currentUser?.username ?: "Someone"} requested to follow you"
            )
        } else {
            // Direct follow
            val followerDoc = db.collection("users").document(targetUser.userId).collection("followers").document(uid)
            val followingDoc = db.collection("users").document(uid).collection("following").document(targetUser.userId)

            val followerPayload = mapOf(
                "userId" to uid,
                "username" to (currentUser?.username ?: "user"),
                "displayName" to (currentUser?.displayName ?: "User"),
                "avatarUrl" to (currentUser?.avatarUrl ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )
            val followingPayload = mapOf(
                "userId" to targetUser.userId,
                "username" to targetUser.username,
                "displayName" to targetUser.displayName,
                "avatarUrl" to targetUser.avatarUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )

            followerDoc.set(followerPayload).await()
            followingDoc.set(followingPayload).await()

            db.collection("users").document(targetUser.userId).update("followersCount", FieldValue.increment(1)).await()
            db.collection("users").document(uid).update("followingCount", FieldValue.increment(1)).await()

            createNotification(
                recipientUserId = targetUser.userId,
                type = "follow",
                targetId = uid,
                message = "${currentUser?.username ?: "Someone"} started following you"
            )
        }
    }

    suspend fun unfollowUser(targetUserId: String) {
        val uid = requireUserId()
        val followerDoc = db.collection("users").document(targetUserId).collection("followers").document(uid)
        val followingDoc = db.collection("users").document(uid).collection("following").document(targetUserId)

        try {
            followerDoc.delete().await()
            followingDoc.delete().await()
            db.collection("users").document(targetUserId).update("followersCount", FieldValue.increment(-1)).await()
            db.collection("users").document(uid).update("followingCount", FieldValue.increment(-1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followerDoc.path)
        }
    }

    fun isFollowing(targetUserId: String): Flow<Boolean> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("following").document(targetUserId)
            .snapshots()
            .map { it.exists() }
    }

    suspend fun respondToFollowRequest(requester: FollowRequest, accept: Boolean) {
        val uid = requireUserId()
        val reqDoc = db.collection("users").document(uid).collection("follow_requests").document(requester.requesterUserId)

        if (accept) {
            val targetUser = getUserProfile(uid)
            val followerDoc = db.collection("users").document(uid).collection("followers").document(requester.requesterUserId)
            val followingDoc = db.collection("users").document(requester.requesterUserId).collection("following").document(uid)

            val followerPayload = mapOf(
                "userId" to requester.requesterUserId,
                "username" to requester.requesterUsername,
                "displayName" to requester.requesterDisplayName,
                "avatarUrl" to requester.requesterAvatarUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val followingPayload = mapOf(
                "userId" to uid,
                "username" to (targetUser?.username ?: "user"),
                "displayName" to (targetUser?.displayName ?: "User"),
                "avatarUrl" to (targetUser?.avatarUrl ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )

            followerDoc.set(followerPayload).await()
            followingDoc.set(followingPayload).await()

            db.collection("users").document(uid).update("followersCount", FieldValue.increment(1)).await()
            db.collection("users").document(requester.requesterUserId).update("followingCount", FieldValue.increment(1)).await()

            createNotification(
                recipientUserId = requester.requesterUserId,
                type = "follow",
                targetId = uid,
                message = "${targetUser?.username ?: "User"} accepted your follow request"
            )
        }

        reqDoc.delete().await()
    }

    fun observeFollowRequests(): Flow<List<FollowRequest>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("follow_requests")
            .snapshots()
            .map { snap -> snap.toObjects(FollowRequest::class.java) }
    }

    // --- Close Friends & Blocked Users ---

    suspend fun toggleCloseFriend(friendUserId: String, isFriend: Boolean) {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).collection("close_friends").document(friendUserId)
        if (isFriend) {
            doc.delete().await()
        } else {
            doc.set(mapOf("userId" to friendUserId, "createdAt" to FieldValue.serverTimestamp())).await()
        }
    }

    fun observeCloseFriends(): Flow<List<String>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("close_friends")
            .snapshots()
            .map { snap -> snap.documents.map { it.id } }
    }

    suspend fun toggleBlockUser(targetUserId: String, isBlocked: Boolean) {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).collection("blocked_users").document(targetUserId)
        if (isBlocked) {
            doc.delete().await()
        } else {
            doc.set(mapOf("userId" to targetUserId, "createdAt" to FieldValue.serverTimestamp())).await()
            // auto unfollow
            unfollowUser(targetUserId)
        }
    }

    fun observeBlockedUsers(): Flow<List<String>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("blocked_users")
            .snapshots()
            .map { snap -> snap.documents.map { it.id } }
    }

    suspend fun muteUser(targetUserId: String, mute: Boolean = true) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val mutedDoc = userRef.collection("muted_users").document(targetUserId)
        try {
            if (mute) {
                userRef.update("mutedUserIds", FieldValue.arrayUnion(targetUserId)).await()
                mutedDoc.set(mapOf("userId" to targetUserId, "createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                userRef.update("mutedUserIds", FieldValue.arrayRemove(targetUserId)).await()
                mutedDoc.delete().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
        }
    }

    suspend fun restrictUser(targetUserId: String, restrict: Boolean = true) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val restrictedDoc = userRef.collection("restricted_users").document(targetUserId)
        try {
            if (restrict) {
                userRef.update("restrictedUserIds", FieldValue.arrayUnion(targetUserId)).await()
                restrictedDoc.set(mapOf("userId" to targetUserId, "createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                userRef.update("restrictedUserIds", FieldValue.arrayRemove(targetUserId)).await()
                restrictedDoc.delete().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
        }
    }

    suspend fun getUserByUsername(username: String): UserProfile? {
        val clean = username.trim().lowercase().removePrefix("@")
        val uid = getUserIdForUsername(clean) ?: return null
        return getUserProfile(uid)
    }

    // --- Conversations & Messages ---

    suspend fun getOrCreateDirectConversation(otherUser: UserProfile): String {
        val uid = requireUserId()
        val otherUid = otherUser.userId.ifBlank { otherUser.id }
        require(otherUid.isNotBlank()) { "Target user ID cannot be blank" }
        val memberIds = listOf(uid, otherUid).sorted()
        val convId = "dm_${memberIds[0]}_${memberIds[1]}"
        val convRef = db.collection("conversations").document(convId)

        try {
            val existing = convRef.get().await()
            if (!existing.exists()) {
                val displayName = otherUser.displayName.ifBlank { "@${otherUser.username}" }
                val payload = mapOf(
                    "type" to "direct",
                    "name" to displayName,
                    "avatarUrl" to (otherUser.avatarUrl ?: ""),
                    "memberIds" to memberIds,
                    "adminIds" to listOf(uid),
                    "lastMessageText" to "",
                    "lastMessageSenderId" to "",
                    "theme" to "default",
                    "wallpaperUrl" to "",
                    "unreadCounts" to mapOf(uid to 0L, otherUid to 0L),
                    "mutedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                convRef.set(payload).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("VyraChat", "Error ensuring direct conversation $convId: ${e.message}")
        }
        return convId
    }

    suspend fun getConversation(conversationId: String): Conversation? {
        return try {
            val snap = db.collection("conversations").document(conversationId).get().await()
            if (snap.exists()) snap.toObject(Conversation::class.java) else null
        } catch (e: Exception) {
            android.util.Log.w("VyraChat", "getConversation error: ${e.message}")
            null
        }
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> =
        db.collection("conversations").document(conversationId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(Conversation::class.java) else null
            }
            .catch { error ->
                android.util.Log.w("VyraChat", "observeConversation error: ${error.message}")
                emit(null)
            }

    suspend fun createGroupConversation(name: String, memberIds: List<String>): String {
        val uid = requireUserId()
        val allMembers = (memberIds + uid).distinct()
        require(allMembers.size in 2..50) { "Groups must have between 2 and 50 members." }
        val convRef = db.collection("conversations").document()
        val unreadMap = allMembers.associateWith { 0L }
        val payload = mapOf(
            "type" to "group",
            "name" to name.trim().ifBlank { "Group Chat" },
            "avatarUrl" to "",
            "memberIds" to allMembers,
            "adminIds" to listOf(uid),
            "lastMessageText" to "Group created",
            "lastMessageSenderId" to uid,
            "theme" to "default",
            "wallpaperUrl" to "",
            "unreadCounts" to unreadMap,
            "mutedUserIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        convRef.set(payload).await()
        return convRef.id
    }

    fun observeUserConversations(): Flow<List<Conversation>> {
        val uid = requireUserId()
        return db.collection("conversations")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(Conversation::class.java)
                    .sortedByDescending { it.lastMessageTimestamp ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                android.util.Log.w("VyraChat", "observeUserConversations error: ${error.message}")
                emit(emptyList())
            }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "text",
        sharedPostId: String = "",
        voiceDurationMs: Long = 0L,
        pollQuestion: String = "",
        pollOptions: List<String> = emptyList(),
        pollExpiresAt: Timestamp? = null
    ) {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val convSnap = try {
            db.collection("conversations").document(conversationId).get().await()
        } catch (_: Exception) {
            null
        }
        val memberIds = (convSnap?.get("memberIds") as? List<String>) ?: listOf(uid)

        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document()
        val payload = mutableMapOf<String, Any>(
            "conversationId" to conversationId,
            "senderId" to uid,
            "senderUsername" to (user?.username ?: "user"),
            "senderAvatarUrl" to (user?.avatarUrl ?: ""),
            "senderBadges" to (user?.badges ?: emptyList<String>()),
            "memberIds" to memberIds,
            "text" to text.trim(),
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "sharedPostId" to sharedPostId,
            "voiceDurationMs" to voiceDurationMs,
            "pollQuestion" to pollQuestion,
            "pollOptions" to pollOptions,
            "pollVotes" to emptyMap<String, List<String>>(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (pollExpiresAt != null) {
            payload["pollExpiresAt"] = pollExpiresAt
        }

        val lastSummary = when (mediaType) {
            "voice" -> "🎤 Voice message (${voiceDurationMs / 1000}s)"
            "poll" -> "📊 Poll: $pollQuestion"
            "image" -> "📷 Photo"
            "video" -> "🎥 Video"
            else -> if (text.isNotBlank()) text.trim() else "Shared content"
        }

        try {
            msgRef.set(payload).await()
            val updatePayload = mutableMapOf<String, Any>(
                "lastMessageText" to lastSummary,
                "lastMessageSenderId" to uid,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // Increment unread counts for recipient members
            memberIds.filter { it != uid }.forEach { recipientId ->
                updatePayload["unreadCounts.$recipientId"] = FieldValue.increment(1)
            }
            db.collection("conversations").document(conversationId).update(updatePayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("VyraChat", "sendMessage failed: ${e.message}", e)
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun markConversationRead(conversationId: String) {
        val uid = requireUserId()
        try {
            db.collection("conversations").document(conversationId).update("unreadCounts.$uid", 0L).await()
        } catch (_: Exception) {
            // Ignore minor unread sync errors
        }
    }

    suspend fun voteOnPoll(conversationId: String, messageId: String, optionIndex: Int) {
        val uid = requireUserId()
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        try {
            val snap = msgRef.get().await()
            val currentVotes = (snap.get("pollVotes") as? Map<String, List<String>>) ?: emptyMap()
            val updatedVotes = currentVotes.toMutableMap()
            // Remove previous vote from any other option if present
            updatedVotes.keys.forEach { key ->
                val list = updatedVotes[key]?.toMutableList() ?: mutableListOf()
                if (list.remove(uid)) {
                    updatedVotes[key] = list
                }
            }
            // Add vote to the selected option
            val optKey = optionIndex.toString()
            val list = updatedVotes[optKey]?.toMutableList() ?: mutableListOf()
            if (!list.contains(uid)) {
                list.add(uid)
            }
            updatedVotes[optKey] = list

            msgRef.update("pollVotes", updatedVotes).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraChat", "voteOnPoll error: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateConversationTheme(conversationId: String, theme: String, wallpaperUrl: String = "") {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        try {
            val payload = mutableMapOf<String, Any>(
                "theme" to theme,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (wallpaperUrl.isNotBlank()) {
                payload["wallpaperUrl"] = wallpaperUrl
            }
            convRef.update(payload).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraChat", "updateConversationTheme error: ${e.message}", e)
            throw e
        }
    }

    suspend fun toggleMuteConversation(conversationId: String, mute: Boolean) {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        try {
            val op = if (mute) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            convRef.update("mutedUserIds", op, "updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraChat", "toggleMuteConversation error: ${e.message}", e)
            throw e
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        db.collection("conversations").document(conversationId).collection("messages")
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessage::class.java)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                android.util.Log.w("VyraChat", "observeMessages error for $conversationId: ${error.message}")
                emit(emptyList())
            }

    // --- Notifications ---

    fun observeNotifications(): Flow<List<NotificationItem>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(NotificationItem::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun markNotificationRead(notificationId: String) {
        val uid = requireUserId()
        try {
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (ignored: Exception) {}
    }

    suspend fun createNotification(
        recipientUserId: String,
        type: String,
        targetId: String,
        message: String
    ) {
        val uid = requireUserId()
        if (recipientUserId == uid) return
        val user = getUserProfile(uid)
        val notifRef = db.collection("users").document(recipientUserId).collection("notifications").document()
        val payload = mapOf(
            "userId" to recipientUserId,
            "actorId" to uid,
            "actorUsername" to (user?.username ?: "user"),
            "actorAvatarUrl" to (user?.avatarUrl ?: ""),
            "type" to type,
            "targetId" to targetId,
            "message" to message,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            notifRef.set(payload).await()
        } catch (ignored: Exception) {}
    }

    // --- Safety & Moderation ---

    suspend fun reportContent(targetType: String, targetId: String, reason: String, details: String) {
        val uid = requireUserId()
        val reportRef = db.collection("reports").document()
        val payload = mapOf(
            "reporterId" to uid,
            "targetType" to targetType,
            "targetId" to targetId,
            "reason" to reason,
            "details" to details,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            reportRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path)
        }
    }

    // --- FEATURE 2: Media & Storage Upload ---

    suspend fun uploadMedia(uri: Uri, folder: String = "uploads"): String {
        return try {
            val uid = currentUserId() ?: "anon"
            val filename = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val ref = storage.reference.child("$folder/$uid/$filename")
            val stream = context?.contentResolver?.openInputStream(uri)
            if (stream != null) {
                ref.putStream(stream).await()
                ref.downloadUrl.await().toString()
            } else {
                uri.toString()
            }
        } catch (e: Exception) {
            // Graceful fallback to content URI if cloud storage bucket is unconfigured
            uri.toString()
        }
    }

    suspend fun updateProfilePhoto(newAvatarUrl: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update("avatarUrl", newAvatarUrl, "updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun removeProfilePhoto() {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update("avatarUrl", "", "updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- FEATURE 1: Verification System ---

    fun isUserAdmin(profile: UserProfile?): Boolean {
        if (profile == null) return false
        return profile.username.equals("owneranant", ignoreCase = true) || profile.isAdmin
    }

    suspend fun submitDeveloperVerification(
        developerName: String,
        experienceYears: String
    ): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        if (user?.badges?.contains("developer") == true) {
            throw IllegalStateException("You already have the Developer badge!")
        }
        val existing = db.collection("verification_requests")
            .whereEqualTo("userId", uid)
            .whereEqualTo("badgeType", "developer")
            .whereEqualTo("status", "pending")
            .get()
            .await()
        if (!existing.isEmpty) {
            throw IllegalStateException("You already have a pending Developer verification application.")
        }

        val docRef = db.collection("verification_requests").document()
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "displayName" to (user?.displayName ?: "User"),
            "avatarUrl" to (user?.avatarUrl ?: ""),
            "badgeType" to "developer",
            "status" to "pending",
            "developerName" to developerName.trim(),
            "experienceYears" to experienceYears.trim(),
            "proofLink" to "",
            "details" to "Developer: ${developerName.trim()}, Experience: ${experienceYears.trim()}",
            "platformOrExperience" to experienceYears.trim(),
            "category" to "Software Development",
            "rejectionReason" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun submitInfluencerVerification(
        channelName: String,
        channelUrl: String
    ): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        if (user?.badges?.contains("influencer") == true) {
            throw IllegalStateException("You already have the Influencer badge!")
        }
        val existing = db.collection("verification_requests")
            .whereEqualTo("userId", uid)
            .whereEqualTo("badgeType", "influencer")
            .whereEqualTo("status", "pending")
            .get()
            .await()
        if (!existing.isEmpty) {
            throw IllegalStateException("You already have a pending Influencer verification application.")
        }

        val docRef = db.collection("verification_requests").document()
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "displayName" to (user?.displayName ?: "User"),
            "avatarUrl" to (user?.avatarUrl ?: ""),
            "badgeType" to "influencer",
            "status" to "pending",
            "channelName" to channelName.trim(),
            "channelUrl" to channelUrl.trim(),
            "proofLink" to channelUrl.trim(),
            "details" to "Channel: ${channelName.trim()}, URL: ${channelUrl.trim()}",
            "platformOrExperience" to channelName.trim(),
            "category" to "Content Creator",
            "rejectionReason" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun submitVerificationRequest(
        badgeType: String,
        proofLink: String,
        details: String,
        platformOrExperience: String,
        category: String
    ): String {
        if (badgeType == "developer") {
            return submitDeveloperVerification(
                developerName = details.ifBlank { "Developer" },
                experienceYears = platformOrExperience.ifBlank { "1+ years" }
            )
        } else {
            return submitInfluencerVerification(
                channelName = platformOrExperience.ifBlank { "Creator" },
                channelUrl = proofLink.ifBlank { "https://" }
            )
        }
    }

    fun observeUserVerificationRequests(userId: String): Flow<List<VerificationRequest>> =
        db.collection("verification_requests")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap -> snap.toObjects(VerificationRequest::class.java) }

    fun observeAllVerificationRequests(): Flow<List<VerificationRequest>> =
        db.collection("verification_requests")
            .snapshots()
            .map { snap ->
                snap.toObjects(VerificationRequest::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun reviewVerificationRequest(
        requestId: String,
        applicantUserId: String,
        badgeType: String,
        approve: Boolean,
        reason: String = ""
    ) {
        val currentUid = requireUserId()
        val currentProfile = getUserProfile(currentUid)
        if (!isUserAdmin(currentProfile)) {
            throw IllegalStateException("Only @owneranant is authorized to approve or reject verification applications.")
        }

        val reqRef = db.collection("verification_requests").document(requestId)
        try {
            if (approve) {
                // Add badge to user's badges array in Firestore
                db.collection("users").document(applicantUserId)
                    .update("badges", FieldValue.arrayUnion(badgeType)).await()
                reqRef.update(
                    "status", "approved",
                    "reviewedAt", FieldValue.serverTimestamp()
                ).await()
                createNotification(
                    recipientUserId = applicantUserId,
                    type = "verification",
                    targetId = requestId,
                    message = "Congratulations! Your verification has been approved."
                )
            } else {
                reqRef.update(
                    "status", "rejected",
                    "rejectionReason", reason.ifBlank { "Requirements not met." },
                    "reviewedAt", FieldValue.serverTimestamp()
                ).await()
                createNotification(
                    recipientUserId = applicantUserId,
                    type = "verification",
                    targetId = requestId,
                    message = "Your verification request has been rejected."
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            throw e
        }
    }

    // --- FEATURE 5: Story Highlights ---

    fun observeUserHighlights(userId: String): Flow<List<StoryHighlight>> =
        db.collection("highlights")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(StoryHighlight::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun createHighlight(title: String, coverUrl: String, storyIds: List<String>): String {
        val uid = requireUserId()
        val docRef = db.collection("highlights").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim(),
            "coverUrl" to coverUrl,
            "storyIds" to storyIds,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateHighlight(highlightId: String, title: String, coverUrl: String, storyIds: List<String>) {
        val docRef = db.collection("highlights").document(highlightId)
        try {
            docRef.update(
                "title", title.trim(),
                "coverUrl", coverUrl,
                "storyIds", storyIds,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteHighlight(highlightId: String) {
        val docRef = db.collection("highlights").document(highlightId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- FEATURE 6: Drafts ---

    fun observeUserDrafts(): Flow<List<DraftItem>> {
        val uid = currentUserId() ?: return flow { emit(emptyList()) }
        return db.collection("users").document(uid).collection("drafts")
            .snapshots()
            .map { snap ->
                snap.toObjects(DraftItem::class.java)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun saveDraft(draft: DraftItem): String {
        val uid = requireUserId()
        val draftRef = if (draft.id.isNotBlank()) {
            db.collection("users").document(uid).collection("drafts").document(draft.id)
        } else {
            db.collection("users").document(uid).collection("drafts").document()
        }
        val payload = mapOf(
            "userId" to uid,
            "caption" to draft.caption,
            "mediaUri" to draft.mediaUri,
            "mediaType" to draft.mediaType,
            "isReel" to draft.isReel,
            "hashtags" to draft.hashtags,
            "musicTitle" to draft.musicTitle,
            "musicArtist" to draft.musicArtist,
            "musicTrackUrl" to draft.musicTrackUrl,
            "spotifyUrl" to draft.spotifyUrl,
            "trimStartSeconds" to draft.trimStartSeconds,
            "trimEndSeconds" to draft.trimEndSeconds,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            draftRef.set(payload).await()
            return draftRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, draftRef.path)
            throw e
        }
    }

    suspend fun deleteDraft(draftId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("drafts").document(draftId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- FEATURE 4: Authorized Music Catalogue ---

    fun getMusicCatalogue(query: String = ""): List<MusicTrack> {
        val tracks = listOf(
            MusicTrack(
                id = "track_1",
                title = "Cyber Horizon",
                artist = "Neon Pulse",
                album = "Synthwave Dreams",
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300",
                audioUrl = "https://cdn.freesound.org/previews/682/682123_11861866-lq.mp3",
                durationSeconds = 30,
                genre = "Synthwave",
                spotifySharingUrl = "https://open.spotify.com/search/Cyber%20Horizon%20Neon%20Pulse"
            ),
            MusicTrack(
                id = "track_2",
                title = "Obsidian Nights",
                artist = "Vyra Collective",
                album = "Electronic Odyssey",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                audioUrl = "https://cdn.freesound.org/previews/665/665243_11861866-lq.mp3",
                durationSeconds = 30,
                genre = "Electronic",
                spotifySharingUrl = "https://open.spotify.com/search/Obsidian%20Nights%20Vyra"
            ),
            MusicTrack(
                id = "track_3",
                title = "Golden Hour Chill",
                artist = "Sunset Breeze",
                album = "Lo-Fi Haven",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                audioUrl = "https://cdn.freesound.org/previews/615/615462_11861866-lq.mp3",
                durationSeconds = 30,
                genre = "Lo-Fi",
                spotifySharingUrl = "https://open.spotify.com/search/Golden%20Hour%20Chill"
            ),
            MusicTrack(
                id = "track_4",
                title = "Digital Velocity",
                artist = "Code Runners",
                album = "Hacker Beats",
                coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300",
                audioUrl = "https://cdn.freesound.org/previews/563/563845_11861866-lq.mp3",
                durationSeconds = 30,
                genre = "Tech House",
                spotifySharingUrl = "https://open.spotify.com/search/Digital%20Velocity"
            ),
            MusicTrack(
                id = "track_5",
                title = "Urban Echoes",
                artist = "Streetlight Trio",
                album = "City Lights",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                audioUrl = "https://cdn.freesound.org/previews/530/530415_11861866-lq.mp3",
                durationSeconds = 30,
                genre = "Hip Hop",
                spotifySharingUrl = "https://open.spotify.com/search/Urban%20Echoes"
            ),
            MusicTrack(
                id = "track_6",
                title = "Euphoria Flow",
                artist = "Astra",
                album = "Celestial Vibes",
                coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300",
                audioUrl = "https://cdn.freesound.org/previews/410/410574_11861866-lq.mp3",
                durationSeconds = 30,
                genre = "Pop",
                spotifySharingUrl = "https://open.spotify.com/search/Euphoria%20Flow%20Astra"
            )
        )

        if (query.isBlank()) return tracks
        val q = query.trim().lowercase()
        return tracks.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.genre.lowercase().contains(q) }
    }

    // --- FEATURE 8: Additional Privacy & Moderation Controls ---

    suspend fun updatePrivacySettings(showOnlineStatus: Boolean, hideLikeCounts: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                "showOnlineStatus", showOnlineStatus,
                "hideLikeCounts", hideLikeCounts,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeMutedUsers(): Flow<List<String>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("users").document(uid).snapshots().map { snap ->
            (snap.get("mutedUserIds") as? List<String>) ?: emptyList()
        }.catch { emit(emptyList()) }
    }

    fun observeRestrictedUsers(): Flow<List<String>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("users").document(uid).snapshots().map { snap ->
            (snap.get("restrictedUserIds") as? List<String>) ?: emptyList()
        }.catch { emit(emptyList()) }
    }

    // --- FEATURE 3: VYRA Moments ---

    suspend fun publishMoment(
        text: String,
        mediaUrl: String = "",
        mood: String = "",
        musicTitle: String = "",
        musicArtist: String = "",
        musicUrl: String = "",
        privacy: String = "everyone"
    ): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val momentRef = db.collection("moments").document()
        val nowMs = System.currentTimeMillis()
        val expiresAt = Timestamp(java.util.Date(nowMs + 24 * 60 * 60 * 1000L))

        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "authorBadges" to (user?.badges ?: emptyList<String>()),
            "text" to text.trim(),
            "mediaUrl" to mediaUrl,
            "mood" to mood,
            "musicTitle" to musicTitle,
            "musicArtist" to musicArtist,
            "musicUrl" to musicUrl,
            "privacy" to privacy,
            "likesCount" to 0L,
            "repliesCount" to 0L,
            "likedUserIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )
        try {
            momentRef.set(payload).await()
            return momentRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, momentRef.path)
            throw e
        }
    }

    fun observeMoments(): Flow<List<VyraMoment>> =
        db.collection("moments")
            .snapshots()
            .map { snap ->
                val now = Timestamp.now()
                snap.toObjects(VyraMoment::class.java)
                    .filter { moment ->
                        moment.expiresAt == null || moment.expiresAt > now
                    }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                android.util.Log.w("VyraMoments", "observeMoments error: ${error.message}")
                emit(emptyList())
            }

    suspend fun toggleLikeMoment(moment: VyraMoment, isLiked: Boolean) {
        val uid = requireUserId()
        val momentRef = db.collection("moments").document(moment.id)
        try {
            val op = if (isLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            val inc = if (isLiked) -1L else 1L
            momentRef.update(
                "likedUserIds", op,
                "likesCount", FieldValue.increment(inc)
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraMoments", "toggleLikeMoment error: ${e.message}")
        }
    }

    suspend fun replyToMoment(momentId: String, text: String) {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val replyRef = db.collection("moments").document(momentId).collection("replies").document()
        val payload = mapOf(
            "momentId" to momentId,
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "userBadges" to (user?.badges ?: emptyList<String>()),
            "text" to text.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            replyRef.set(payload).await()
            db.collection("moments").document(momentId).update("repliesCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, replyRef.path)
            throw e
        }
    }

    fun observeMomentReplies(momentId: String): Flow<List<MomentReply>> =
        db.collection("moments").document(momentId).collection("replies")
            .snapshots()
            .map { snap ->
                snap.toObjects(MomentReply::class.java)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }

    suspend fun deleteMoment(momentId: String) {
        val uid = requireUserId()
        val momentRef = db.collection("moments").document(momentId)
        try {
            val snap = momentRef.get().await()
            if (snap.getString("userId") == uid) {
                momentRef.delete().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("VyraMoments", "deleteMoment error: ${e.message}")
        }
    }

    // --- FEATURE 4: Living Profiles ---

    suspend fun updateProfileCustomization(
        profileTheme: String,
        bannerStyle: String,
        cardBorderStyle: String,
        pinnedPostIds: List<String>,
        sectionOrder: List<String>,
        bioLinks: List<BioLink>
    ) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val safeLinks = bioLinks.map { mapOf("title" to it.title.take(50), "url" to it.url.take(200)) }
        try {
            userRef.update(
                mapOf(
                    "profileTheme" to profileTheme,
                    "bannerStyle" to bannerStyle,
                    "cardBorderStyle" to cardBorderStyle,
                    "pinnedPostIds" to pinnedPostIds.take(3),
                    "sectionOrder" to sectionOrder,
                    "bioLinks" to safeLinks,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun recordProfileVisit(targetUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (uid == targetUserId) return
        try {
            db.collection("users").document(targetUserId).update(
                "profileVisitsCount", FieldValue.increment(1),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (_: Exception) {
            // Non-critical visitor counter increment
        }
    }

    // --- FEATURE 5: VYRA Sound (Playlists) ---

    suspend fun createPlaylist(
        title: String,
        description: String = "",
        coverUrl: String = "",
        isPublic: Boolean = true
    ): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val playRef = db.collection("playlists").document()
        val payload = mapOf(
            "userId" to uid,
            "creatorUsername" to (user?.username ?: "user"),
            "title" to title.trim().ifBlank { "My VYRA Playlist" },
            "description" to description.trim(),
            "coverUrl" to coverUrl,
            "isPublic" to isPublic,
            "tracks" to emptyList<Map<String, Any>>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            playRef.set(payload).await()
            return playRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, playRef.path)
            throw e
        }
    }

    suspend fun updatePlaylist(
        playlistId: String,
        title: String,
        description: String,
        isPublic: Boolean
    ) {
        val uid = requireUserId()
        val playRef = db.collection("playlists").document(playlistId)
        try {
            playRef.update(
                mapOf(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "isPublic" to isPublic,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playRef.path)
            throw e
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: PlaylistTrack) {
        val uid = requireUserId()
        val playRef = db.collection("playlists").document(playlistId)
        val trackMap = mapOf(
            "id" to track.id.ifBlank { "track_${System.currentTimeMillis()}" },
            "title" to track.title,
            "artist" to track.artist,
            "audioUrl" to track.audioUrl,
            "durationMs" to track.durationMs,
            "spotifyUrl" to track.spotifyUrl
        )
        try {
            playRef.update(
                "tracks", FieldValue.arrayUnion(trackMap),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playRef.path)
            throw e
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val uid = requireUserId()
        val playRef = db.collection("playlists").document(playlistId)
        try {
            val snap = playRef.get().await()
            val tracksRaw = (snap.get("tracks") as? List<Map<String, Any>>) ?: emptyList()
            val filtered = tracksRaw.filter { it["id"] != trackId }
            playRef.update(
                "tracks", filtered,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playRef.path)
            throw e
        }
    }

    fun observeUserPlaylists(userId: String): Flow<List<MusicPlaylist>> =
        db.collection("playlists")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(MusicPlaylist::class.java)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }

    fun observePublicPlaylists(): Flow<List<MusicPlaylist>> =
        db.collection("playlists")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(MusicPlaylist::class.java)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }

    suspend fun deletePlaylist(playlistId: String) {
        val uid = requireUserId()
        val playRef = db.collection("playlists").document(playlistId)
        try {
            playRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, playRef.path)
            throw e
        }
    }

    // --- FEATURE 6: Ask Me (Interactive Question Stickers) ---

    suspend fun createQuestionSticker(prompt: String, storyId: String = ""): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val stickRef = db.collection("question_stickers").document()
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "prompt" to prompt.trim().ifBlank { "Ask me anything!" },
            "storyId" to storyId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            stickRef.set(payload).await()
            return stickRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, stickRef.path)
            throw e
        }
    }

    suspend fun submitQuestionAnswer(stickerId: String, prompt: String, text: String) {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val ansRef = db.collection("question_stickers").document(stickerId).collection("answers").document()
        val payload = mapOf(
            "stickerId" to stickerId,
            "prompt" to prompt,
            "authorId" to uid,
            "authorUsername" to (user?.username ?: "user"),
            "authorAvatarUrl" to (user?.avatarUrl ?: ""),
            "text" to text.trim(),
            "isPublic" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ansRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ansRef.path)
            throw e
        }
    }

    fun observeQuestionAnswers(stickerId: String): Flow<List<QuestionAnswer>> =
        db.collection("question_stickers").document(stickerId).collection("answers")
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionAnswer::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }

    suspend fun toggleAnswerPublic(stickerId: String, answerId: String, isPublic: Boolean) {
        val ansRef = db.collection("question_stickers").document(stickerId).collection("answers").document(answerId)
        try {
            ansRef.update("isPublic", isPublic).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraAskMe", "toggleAnswerPublic error: ${e.message}")
        }
    }

    suspend fun deleteQuestionAnswer(stickerId: String, answerId: String) {
        val ansRef = db.collection("question_stickers").document(stickerId).collection("answers").document(answerId)
        try {
            ansRef.delete().await()
        } catch (e: Exception) {
            android.util.Log.e("VyraAskMe", "deleteQuestionAnswer error: ${e.message}")
        }
    }

    // --- FEATURE 7: Collab Posts ---

    suspend fun inviteCollaborator(postId: String, collaboratorUsername: String) {
        val uid = requireUserId()
        val cleanUsername = collaboratorUsername.trim().removePrefix("@").lowercase()
        val targetUser = getUserByUsername(cleanUsername) ?: throw IllegalArgumentException("User @$cleanUsername not found")
        val postRef = db.collection("posts").document(postId)
        try {
            postRef.update(
                mapOf(
                    "collaboratorUserId" to targetUser.userId,
                    "collaboratorUsername" to targetUser.username,
                    "collaboratorStatus" to "pending",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Send notification to collaborator
            val notifRef = db.collection("users").document(targetUser.userId).collection("notifications").document()
            val currentUser = getUserProfile(uid)
            notifRef.set(
                mapOf(
                    "userId" to targetUser.userId,
                    "actorId" to uid,
                    "actorUsername" to (currentUser?.username ?: "user"),
                    "actorAvatarUrl" to (currentUser?.avatarUrl ?: ""),
                    "type" to "collab",
                    "targetId" to postId,
                    "message" to "@${currentUser?.username} invited you to collaborate on a post",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    suspend fun respondToCollaboration(postId: String, accept: Boolean) {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        try {
            val status = if (accept) "accepted" else "rejected"
            postRef.update(
                mapOf(
                    "collaboratorStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    // --- FEATURE 8: Profile Insights ---

    fun observeProfileInsights(userId: String): Flow<ProfileInsightsData> {
        return combine(
            db.collection("users").document(userId).snapshots(),
            db.collection("posts").whereEqualTo("userId", userId).snapshots(),
            db.collection("users").document(userId).collection("followers").snapshots()
        ) { userSnap, postsSnap, followersSnap ->
            val profile = userSnap.toObject(UserProfile::class.java)
            val posts = postsSnap.toObjects(Post::class.java)
            val followersCount = followersSnap.size()

            val profileVisits = profile?.profileVisitsCount ?: 0L
            val totalReach = posts.sumOf { (it.likesCount + it.commentsCount + it.viewsCount).coerceAtLeast(1L) }
            val totalReelViews = posts.filter { it.isReel || it.contentType == "REEL" }.sumOf { it.viewsCount }
            val totalLikes = posts.sumOf { it.likesCount }
            val totalComments = posts.sumOf { it.commentsCount }
            val engagementRate = if (totalReach > 0) ((totalLikes + totalComments).toDouble() / totalReach.toDouble()) * 100.0 else 0.0

            ProfileInsightsData(
                profileVisits = profileVisits,
                postReach = totalReach,
                reelViews = totalReelViews,
                followersGained = followersCount.toLong(),
                engagementRate = ((engagementRate * 10).toInt() / 10.0),
                topPosts = posts.sortedByDescending { it.likesCount + it.commentsCount }.take(5)
            )
        }.catch { emit(ProfileInsightsData()) }
    }

    // --- FEATURE 11: Watch Together ---

    suspend fun createWatchSession(title: String, videoUrl: String): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val sessRef = db.collection("watch_sessions").document()
        val payload = mapOf(
            "hostId" to uid,
            "hostUsername" to (user?.username ?: "user"),
            "title" to title.trim().ifBlank { "Watch Party" },
            "videoUrl" to videoUrl,
            "isPlaying" to true,
            "currentPositionMs" to 0L,
            "participantIds" to listOf(uid),
            "reactions" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            sessRef.set(payload).await()
            return sessRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sessRef.path)
            throw e
        }
    }

    suspend fun joinWatchSession(sessionId: String) {
        val uid = requireUserId()
        val sessRef = db.collection("watch_sessions").document(sessionId)
        try {
            sessRef.update(
                "participantIds", FieldValue.arrayUnion(uid),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraWatch", "joinWatchSession error: ${e.message}")
        }
    }

    suspend fun leaveWatchSession(sessionId: String) {
        val uid = requireUserId()
        val sessRef = db.collection("watch_sessions").document(sessionId)
        try {
            sessRef.update(
                "participantIds", FieldValue.arrayRemove(uid),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraWatch", "leaveWatchSession error: ${e.message}")
        }
    }

    suspend fun updateWatchPlayback(sessionId: String, isPlaying: Boolean, currentPositionMs: Long) {
        val sessRef = db.collection("watch_sessions").document(sessionId)
        try {
            sessRef.update(
                mapOf(
                    "isPlaying" to isPlaying,
                    "currentPositionMs" to currentPositionMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraWatch", "updateWatchPlayback error: ${e.message}")
        }
    }

    suspend fun sendWatchReaction(sessionId: String, emoji: String) {
        val uid = requireUserId()
        val sessRef = db.collection("watch_sessions").document(sessionId)
        try {
            sessRef.update(
                "reactions", FieldValue.arrayUnion("$uid:$emoji:${System.currentTimeMillis()}"),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("VyraWatch", "sendWatchReaction error: ${e.message}")
        }
    }

    fun observeWatchSession(sessionId: String): Flow<WatchSession?> =
        db.collection("watch_sessions").document(sessionId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(WatchSession::class.java) else null
            }
            .catch { emit(null) }
}

