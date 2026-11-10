package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class VyraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_succeeds() = runBlocking {
        val email = "alice_${UUID.randomUUID().toString().take(6)}@example.com"
        val uid = signInTestUser(email)
        val repository = VyraRepository(firestore)

        val profile = UserProfile(
            userId = uid,
            username = "alice_${uid.take(4)}",
            displayName = "Alice Wonderland",
            email = email,
            bio = "Building on VYRA"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(profile)
        }

        val loaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(uid)
        }

        assertNotNull(loaded)
        assertEquals(profile.displayName, loaded?.displayName)
    }

    @Test
    fun createPost_andObserve_succeeds() = runBlocking {
        val email = "bob_${UUID.randomUUID().toString().take(6)}@example.com"
        val uid = signInTestUser(email)
        val repository = VyraRepository(firestore)

        val profile = UserProfile(
            userId = uid,
            username = "bob_${uid.take(4)}",
            displayName = "Bob The Creator",
            email = email
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(profile)
        }

        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost(
                mediaUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
                mediaType = "image",
                caption = "Sunset vibes in neon #vyra #vibes",
                hashtags = listOf("vyra", "vibes"),
                isReel = false,
                isPrivate = false,
                allowComments = true
            )
        }

        assertTrue(postId.isNotEmpty())

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFeedPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId })
    }

    @Test
    fun unauthenticated_cannotPerformProtectedWrite(): Unit = runBlocking {
        auth.signOut()
        val repository = VyraRepository(firestore)

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repository.createPost(
                    mediaUrl = "https://example.com/test.jpg",
                    mediaType = "image",
                    caption = "Unauthorized post",
                    hashtags = emptyList(),
                    isReel = false,
                    isPrivate = false,
                    allowComments = true
                )
            }
        }
        Unit
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
