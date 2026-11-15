package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.data.model.FriendConnection
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.VirusCrimsonError
import com.example.viewmodel.DashboardUiState

@Composable
fun PlayerChatAndFriendsTab(
    state: DashboardUiState,
    currentUserId: String,
    onRequestGoogleSignIn: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSelectDirectChatFriend: (String) -> Unit,
    onSendFriendRequest: (String, String, String) -> Unit,
    onAcceptFriendRequest: (FriendConnection) -> Unit,
    onRemoveFriend: (FriendConnection) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isGoogleSignedIn) {
        GoogleSignInRequiredForChatAndFriendsScreen(
            onRequestGoogleSignIn = onRequestGoogleSignIn,
            modifier = modifier
        )
        return
    }

    var messageDraft by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val incomingRequests = remember(state.friendships, currentUserId) {
        state.friendships.filter { it.status == "PENDING" && it.recipientUserId == currentUserId }
    }
    val outgoingRequests = remember(state.friendships, currentUserId) {
        state.friendships.filter { it.status == "PENDING" && it.requesterUserId == currentUserId }
    }
    val acceptedFriends = remember(state.friendships) {
        state.friendships.filter { it.status == "ACCEPTED" }
    }

    val activeFriend = remember(acceptedFriends, state.activeDirectChatFriendId, currentUserId) {
        state.activeDirectChatFriendId.takeIf { it.isNotBlank() }?.let { targetId ->
            acceptedFriends.firstOrNull { it.otherPlayerId(currentUserId) == targetId }
        }
    }

    val activeChannelId = remember(state.activeDirectChatFriendId, currentUserId) {
        val targetId = state.activeDirectChatFriendId
        if (targetId.isBlank()) {
            "global"
        } else {
            FriendConnection.buildDirectChannelId(currentUserId, targetId)
        }
    }

    val filteredMessages = remember(state.chatMessages, activeChannelId) {
        state.chatMessages.filter { it.channelId == activeChannelId }
    }

    val knownFriendUserIds = remember(state.friendships, currentUserId) {
        state.friendships.map { it.otherPlayerId(currentUserId) }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Friends Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
            border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Player Chat and Friends",
                            tint = HoloCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "TAMER COMMS // CHAT & FRIENDS",
                                style = MaterialTheme.typography.titleSmall,
                                color = HoloCyanPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Google Linked • Your Tamer ID: ${currentUserId.take(10)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showAddFriendDialog = true },
                        modifier = Modifier.testTag("open_add_friend_dialog_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BioEmeraldHealth),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Friend",
                            tint = BioEmeraldHealth,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ADD FRIEND",
                            style = MaterialTheme.typography.labelSmall,
                            color = BioEmeraldHealth,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Incoming Friend Requests
                if (incomingRequests.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "INCOMING FRIEND REQUESTS (${incomingRequests.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = EvolutionGoldSecondary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        incomingRequests.forEach { request ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EvolutionGoldSecondary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = request.requesterTrainerName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Partner: ${request.requesterPartnerName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EvolutionGoldSecondary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { onAcceptFriendRequest(request) },
                                            modifier = Modifier.testTag("accept_friend_${request.id}"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BioEmeraldHealth,
                                                contentColor = Color.Black
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Accept Friend Request",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ACCEPT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onRemoveFriend(request) },
                                            modifier = Modifier.testTag("decline_friend_${request.id}"),
                                            border = BorderStroke(1.dp, VirusCrimsonError.copy(alpha = 0.7f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Decline Friend Request",
                                                tint = VirusCrimsonError,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Channel & Friends Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.activeDirectChatFriendId.isBlank(),
                        onClick = { onSelectDirectChatFriend("") },
                        modifier = Modifier.testTag("channel_chip_global"),
                        label = {
                            Text(
                                text = "GLOBAL CHAT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Global Channel",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HoloCyanPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = HoloCyanPrimary,
                            selectedLeadingIconColor = HoloCyanPrimary
                        )
                    )

                    acceptedFriends.forEach { friend ->
                        val friendUserId = friend.otherPlayerId(currentUserId)
                        val friendName = friend.otherPlayerName(currentUserId)
                        val isSelected = state.activeDirectChatFriendId == friendUserId

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDirectChatFriend(friendUserId) },
                            modifier = Modifier.testTag("channel_chip_friend_$friendUserId"),
                            label = {
                                Text(
                                    text = "DM: $friendName",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Friend $friendName",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveFriend(friend) }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BioEmeraldHealth.copy(alpha = 0.22f),
                                selectedLabelColor = BioEmeraldHealth
                            )
                        )
                    }

                    outgoingRequests.forEach { pending ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HoloCardElevated,
                            border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PENDING: ${pending.recipientTrainerName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Request",
                                    tint = EvolutionGoldSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveFriend(pending) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Channel Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (activeFriend != null) {
                BioEmeraldHealth.copy(alpha = 0.14f)
            } else {
                HoloCyanPrimary.copy(alpha = 0.12f)
            },
            border = BorderStroke(
                1.dp,
                if (activeFriend != null) BioEmeraldHealth.copy(alpha = 0.5f) else HoloCyanPrimary.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val friendName = activeFriend?.otherPlayerName(currentUserId)
                val friendPartner = activeFriend?.otherPlayerPartner(currentUserId)
                Text(
                    text = if (friendName != null) {
                        "DIRECT FRIEND LINK // $friendName (${friendPartner ?: "Rookie"})"
                    } else {
                        "GLOBAL TAMER LOBBY // LIVE REAL-PLAYER CHAT"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activeFriend != null) BioEmeraldHealth else HoloCyanPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${filteredMessages.size} MSGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Chat Message List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloSlateSurface.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "No Messages Yet",
                            tint = HoloCyanPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (activeFriend != null) {
                                "No direct messages with ${activeFriend.otherPlayerName(currentUserId)} yet. Say hello!"
                            } else {
                                "No messages in Global Tamer Chat yet. Send the first message to other live Tamers!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMessages, key = { it.id }) { msg ->
                        ChatMessageRow(
                            message = msg,
                            isCurrentUser = msg.senderUserId == currentUserId,
                            canAddFriend = msg.senderUserId != currentUserId &&
                                !knownFriendUserIds.contains(msg.senderUserId),
                            onAddSenderAsFriend = {
                                onSendFriendRequest(
                                    msg.senderUserId,
                                    msg.senderTrainerName,
                                    msg.senderPartnerName
                                )
                            }
                        )
                    }
                }
            }
        }

        // Message Composer Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageDraft,
                onValueChange = { if (it.length <= 280) messageDraft = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                placeholder = {
                    Text(
                        text = if (activeFriend != null) {
                            "Message ${activeFriend.otherPlayerName(currentUserId)}..."
                        } else {
                            "Broadcast to Global Tamer Chat..."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HoloCyanPrimary,
                    unfocusedBorderColor = HoloCyanPrimary.copy(alpha = 0.35f),
                    focusedContainerColor = HoloCardElevated,
                    unfocusedContainerColor = HoloSlateSurface
                )
            )

            IconButton(
                onClick = {
                    val trimmed = messageDraft.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendChatMessage(trimmed)
                        messageDraft = ""
                    }
                },
                enabled = messageDraft.isNotBlank(),
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (messageDraft.isNotBlank()) HoloCyanPrimary else HoloCardElevated
                    )
                    .testTag("send_chat_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Chat Message",
                    tint = if (messageDraft.isNotBlank()) Color.Black else Color.Gray
                )
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendByTamerIdDialog(
            onDismiss = { showAddFriendDialog = false },
            onSendRequest = { targetId, targetName ->
                onSendFriendRequest(targetId, targetName, "Agumon")
                showAddFriendDialog = false
            }
        )
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    isCurrentUser: Boolean,
    canAddFriend: Boolean,
    onAddSenderAsFriend: () -> Unit
) {
    val accentColor = if (isCurrentUser) HoloCyanPrimary else BioEmeraldHealth
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isCurrentUser) 14.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 14.dp
            ),
            color = if (isCurrentUser) {
                HoloCyanPrimary.copy(alpha = 0.14f)
            } else {
                HoloCardElevated
            },
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = if (isCurrentUser) "${message.senderTrainerName} (YOU)" else message.senderTrainerName,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "• ${message.senderPartnerName} (${message.senderPartnerStage})",
                            style = MaterialTheme.typography.labelSmall,
                            color = EvolutionGoldSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (canAddFriend) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAddSenderAsFriend() }
                                .testTag("add_friend_from_chat_${message.senderUserId}"),
                            shape = RoundedCornerShape(6.dp),
                            color = BioEmeraldHealth.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, BioEmeraldHealth.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Add Friend",
                                    tint = BioEmeraldHealth,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "+ FRIEND",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BioEmeraldHealth,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInRequiredForChatAndFriendsScreen(
    onRequestGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_friends_google_locked_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
            border = BorderStroke(1.5.dp, VirusCrimsonError.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = VirusCrimsonError.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, VirusCrimsonError)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Google Sign-In Required",
                        tint = VirusCrimsonError,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(36.dp)
                    )
                }

                Text(
                    text = "FRIENDS & PLAYER CHAT LOCKED",
                    style = MaterialTheme.typography.titleMedium,
                    color = VirusCrimsonError,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You are currently playing in Local Mode without Google Sign-In. You can raise, train, and evolve your monsters locally, but you can only use Friends and Player Chat (and progress on Rankings) when logged into Google.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRequestGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_tab_google_sign_in_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HoloCyanPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "SIGN IN WITH GOOGLE TO UNLOCK FRIENDS & CHAT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFriendByTamerIdDialog(
    onDismiss: () -> Unit,
    onSendRequest: (String, String) -> Unit
) {
    var targetUserId by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HoloCardElevated,
        title = {
            Text(
                text = "ADD REAL-PLAYER FRIEND",
                style = MaterialTheme.typography.titleMedium,
                color = HoloCyanPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tip: You can also tap '+ FRIEND' directly on any player in Global Chat or the Rankings Leaderboard!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetUserId,
                    onValueChange = { targetUserId = it.trim() },
                    label = { Text("Friend's Tamer ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_id_input")
                )
                OutlinedTextField(
                    value = targetName,
                    onValueChange = { targetName = it },
                    label = { Text("Friend's Callsign / Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (targetUserId.isNotBlank()) {
                        onSendRequest(
                            targetUserId.trim(),
                            targetName.trim().ifBlank { "Tamer-${targetUserId.take(5)}" }
                        )
                    }
                },
                enabled = targetUserId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BioEmeraldHealth,
                    contentColor = Color.Black
                )
            ) {
                Text("SEND REQUEST", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
