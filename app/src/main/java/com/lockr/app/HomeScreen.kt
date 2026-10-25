package com.lockr.app

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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    items: List<VaultItem>,
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
        DashboardHeader(onOpenProfile, onSearch, onOpenSettings)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DashboardHero()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick access", style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    QuickAccess("Cards", items.count { it.category == VaultCategory.CARD }, Icons.Outlined.CreditCard, Color(0xFF4A83B8), Color(0xFFE6F2FF), onOpenCards, Modifier.weight(1f))
                    QuickAccess("Documents", items.count { it.category == VaultCategory.DOCUMENT }, Icons.Outlined.Description, Color(0xFF8871B5), Color(0xFFF2EAFE), onOpenDocuments, Modifier.weight(1f))
                    QuickAccess("Passwords", items.count { it.category == VaultCategory.PASSWORD }, Icons.Outlined.Key, Color(0xFF318F87), Color(0xFFE0F6F1), onOpenPasswords, Modifier.weight(1f))
                    QuickAccess("Notes", items.count { it.category == VaultCategory.NOTE }, Icons.AutoMirrored.Outlined.Article, Color(0xFFC18B42), Color(0xFFFFF2D9), onOpenNotes, Modifier.weight(1f))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent items", style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary, modifier = Modifier.weight(1f))
                    if (items.isNotEmpty()) {
                        Text(
                            "See all",
                            color = LockrColors.Accent,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpenAllItems).padding(horizontal = 8.dp, vertical = 7.dp)
                        )
                    }
                }
                if (items.isEmpty()) {
                    EmptyRecentState()
                } else {
                    items.take(5).forEach { item -> RecentItemRow(item, onClick = { onOpenItem(item) }) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DashboardHeader(onOpenProfile: () -> Unit, onSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(68.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).clickable(onClick = onOpenProfile), contentAlignment = Alignment.Center) {
            IridescentOrb(orbSize = 42.dp)
            Icon(Icons.Outlined.Key, contentDescription = "Open vault profile", tint = Color(0xFF526B93), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Stash", style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary)
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
        Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.74f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = LockrColors.TextPrimary, modifier = Modifier.size(22.dp))
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
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xF7F5EDFA), Color(0xE4D9F3EF), Color.Transparent), endX = 760f)
            )
        )
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.64f).padding(start = 19.dp, end = 3.dp)) {
            Text("Everything,\ntogether.", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
            Text(
                "Your things, kept safe.",
                color = LockrColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 7.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuickAccess(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier.height(90.dp).clip(RoundedCornerShape(21.dp)).clickable(onClick = onClick),
        radius = 21.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(35.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = LockrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Text("$count", style = MaterialTheme.typography.labelSmall, color = LockrColors.TextSecondary)
        }
    }
}

@Composable
private fun EmptyRecentState() {
    GlassPanel(Modifier.fillMaxWidth(), radius = 21.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(43.dp).clip(CircleShape).background(Color(0xFFDFF4EF)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = LockrColors.Accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Your vault is ready", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Text("Items you import will appear here.", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun RecentItemRow(item: VaultItem, onClick: () -> Unit) {
    val (icon, tint, background) = when (item.category) {
        VaultCategory.CARD -> Triple(Icons.Outlined.CreditCard, Color(0xFF3979C3), Color(0xFFDCEBFF))
        VaultCategory.DOCUMENT -> Triple(Icons.Outlined.Description, Color(0xFF8169AF), Color(0xFFF0E6FA))
        VaultCategory.PASSWORD -> Triple(Icons.Outlined.Key, Color(0xFF168D82), Color(0xFFDDF5EF))
        VaultCategory.NOTE -> Triple(Icons.AutoMirrored.Outlined.Article, Color(0xFFBA8036), Color(0xFFFFF0D4))
    }
    GlassPanel(Modifier.fillMaxWidth(), radius = 18.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(background), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.category.label} · ${item.time}", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open ${item.title}", tint = LockrColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
