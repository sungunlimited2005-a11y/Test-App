package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.UserProfile
import com.example.ui.theme.VyraButtonGradient
import com.example.ui.theme.VyraMagenta
import com.example.ui.theme.VyraViolet

@Composable
fun QrProfileDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    val context = LocalContext.current
    var isSearchScanMode by remember { mutableStateOf(false) }
    var searchHandleInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("qr_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VyraLogo(size = 24.dp, showWordmark = true)

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_qr_dialog_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isSearchScanMode) {
                    // Profile Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (user.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, VyraViolet, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.displayName.ifBlank { user.username },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VyraUserBadgeRow(badges = user.badges, size = 15.dp)
                            }
                            Text(
                                text = "@${user.username}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stylized QR code
                    VyraQrCodeView(
                        data = "vyra://user/${user.username}",
                        size = 200.dp,
                        modifier = Modifier.testTag("user_qr_canvas")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan or share this code to connect on VYRA",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Connect with @${user.username} on VYRA")
                                    putExtra(Intent.EXTRA_TEXT, "Connect with me on VYRA: vyra://user/${user.username}\nhttps://vyra.social/@${user.username}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share VYRA QR"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_qr_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share QR", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { isSearchScanMode = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("scan_resolver_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan / Search", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // QR / Handle Resolver view
                    Text(
                        text = "Connect by Handle or Code",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter a VYRA @username or pasted QR payload (e.g. vyra://user/alex)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchHandleInput,
                        onValueChange = { searchHandleInput = it },
                        placeholder = { Text("e.g. alex or vyra://user/alex") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = VyraViolet)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_lookup_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isSearchScanMode = false },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back to QR")
                        }

                        Button(
                            onClick = {
                                val cleaned = searchHandleInput.trim()
                                    .removePrefix("vyra://user/")
                                    .removePrefix("@")
                                    .removePrefix("https://vyra.social/@")
                                if (cleaned.isNotBlank()) {
                                    onDismiss()
                                    onNavigateToUser(cleaned)
                                }
                            },
                            enabled = searchHandleInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("qr_resolve_go_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
                        ) {
                            Text("Open Profile")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
