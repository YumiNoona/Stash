package com.lockr.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    items: List<VaultItem>,
    profileName: String,
    profilePhoto: Bitmap?,
    onOpenCards: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenPasswords: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllItems: () -> Unit,
    onOpenProfile: () -> Unit,
    onSearch: () -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        DashboardHeader(profileName, profilePhoto, onOpenProfile, onSearch, onOpenSettings)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            DashboardHero()
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Shortcut(Icons.Outlined.CreditCard, "Cards", Color(0xFFDCEBFF), Color(0xFF4A83B8), onOpenCards)
                Shortcut(Icons.Outlined.Description, "Documents", Color(0xFFF2EAFE), Color(0xFF8871B5), onOpenDocuments)
                Shortcut(Icons.Outlined.Key, "Passwords", Color(0xFFE0F6F1), Color(0xFF318F87), onOpenPasswords)
                Shortcut(Icons.AutoMirrored.Outlined.Article, "Notes", Color(0xFFFFF2D9), Color(0xFFC18B42), onOpenNotes)
            }
            if (items.isEmpty()) {
                Text("Your collection starts here", style = MaterialTheme.typography.bodyMedium,
                    color = LockrColors.TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 25.dp))
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your collection", style = MaterialTheme.typography.titleLarge,
                        color = LockrColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("See all", style = MaterialTheme.typography.labelLarge, color = LockrColors.Accent,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenAllItems)
                            .padding(horizontal = 9.dp, vertical = 6.dp))
                }
                items.take(5).forEach { item -> VaultVisualCard(item, onClick = { onOpenItem(item) }) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DashboardHeader(name: String, photo: Bitmap?, onOpenProfile: () -> Unit, onSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(47.dp).clip(CircleShape).clickable(onClick = onOpenProfile), contentAlignment = Alignment.Center) {
            ProfileAvatar(photo, 43.dp)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary)
            Text("Your personal vault", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        HeaderAction(Icons.Outlined.Search, "Search your vault", onSearch)
        Spacer(Modifier.width(6.dp))
        HeaderAction(Icons.Outlined.Settings, "Settings", onOpenSettings)
    }
}

@Composable
private fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(43.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.74f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = LockrColors.TextPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Shortcut(icon: ImageVector, label: String, background: Color, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(58.dp).clip(CircleShape).background(background.copy(alpha = 0.75f))
        .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun DashboardHero() {
    Box(
        Modifier.fillMaxWidth().height(177.dp).clip(RoundedCornerShape(27.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF0E7FA), Color(0xFFD9F3EF), Color(0xFFFFE7D9))))
            .border(1.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(27.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_vault),
            contentDescription = "Illustration of cards and documents protected in a vault",
            contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.56f).height(177.dp)
                .clip(RoundedCornerShape(topEnd = 27.dp, bottomEnd = 27.dp))
        )
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xF7F5EDFA), Color(0xE4D9F3EF), Color.Transparent), endX = 760f)
        ))
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.64f).padding(start = 19.dp, end = 3.dp)) {
            Text("Everything,\ntogether.", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
            Text("Your things, kept safe.", color = LockrColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 7.dp), maxLines = 2)
        }
    }
}
