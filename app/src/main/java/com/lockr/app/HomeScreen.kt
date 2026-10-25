package com.lockr.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    onOpenSettings: () -> Unit,
    onOpenAllItems: () -> Unit,
    onOpenProfile: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        HomeContent(
            items = items,
            onOpenSettings = onOpenSettings,
            onOpenAllItems = onOpenAllItems,
            onOpenProfile = onOpenProfile,
            onAdd = onAdd,
            modifier = Modifier.weight(1f)
        )
        HomeDock(onOpenCards, onOpenDocuments, onAdd)
    }
}

@Composable
private fun HomeContent(
    items: List<VaultItem>,
    onOpenSettings: () -> Unit,
    onOpenAllItems: () -> Unit,
    onOpenProfile: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val filteredItems = items.filter { it.title.contains(query, ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center
            ) {
                IridescentOrb(orbSize = 42.dp)
                Icon(Icons.Outlined.Person, contentDescription = "Vault profile", tint = Color(0xFF526B93), modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text("Stash", style = MaterialTheme.typography.headlineMedium)
                Text("Your personal vault", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            CircleAction(Icons.Outlined.Search, "Search", onClick = { searchOpen = !searchOpen })
            Spacer(Modifier.width(8.dp))
            CircleAction(Icons.Outlined.Settings, "Settings", onClick = onOpenSettings)
        }

        if (searchOpen) {
            SearchBox(value = query, onValueChange = { query = it })
            Spacer(Modifier.height(12.dp))
        }

        HeroPanel(onAdd = onAdd)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 19.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Items", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "See All",
                color = Color(0xFF246EC3),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenAllItems).padding(5.dp)
            )
        }
        if (filteredItems.isEmpty()) {
            EmptyVaultCard(
                isSearching = query.isNotBlank(),
                onAdd = onAdd,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        } else {
            filteredItems.take(5).forEach { item -> RecentItemRow(item, onClick = onOpenAllItems) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CircleAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.92f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = LockrColors.TextPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyVaultCard(isSearching: Boolean, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    GlassPanel(modifier, radius = 23.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFE7DDFF), Color(0xFFD7F4F1), Color(0xFFFFE9D8)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = Color(0xFF6277AB), modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 5.dp)) {
                Text(
                    if (isSearching) "No matches found" else "Your vault is ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = LockrColors.TextPrimary
                )
                Text(
                    if (isSearching) "Try another search term."
                    else "Your saved items will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            if (!isSearching) {
                Box(
                    Modifier.size(39.dp).clip(CircleShape)
                        .background(Color(0xD93A5963))
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add first item", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(17.dp))
            .background(Color(0xDFFFFFFF)).border(1.dp, Color.White, RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = LockrColors.TextPrimary, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LockrColors.TextPrimary),
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Search your vault", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun HeroPanel(onAdd: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(252.dp).clip(RoundedCornerShape(27.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF4E8FF), Color(0xFFD7F5F1), Color(0xFFFFE8D7))))
            .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(27.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_vault),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.56f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color(0xF2F2E7FA), Color(0xD9D9F5F0), Color(0x08FFFFFF))
                )
            )
        )
        Column(
            Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(0.62f)
                .padding(start = 18.dp, top = 17.dp, bottom = 17.dp, end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Your\nSecure Space", style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary)
            Text(
                "Cards, documents and passwords in one private place.",
                color = Color(0xFF536274),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 12.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF344B58), Color(0xFF1E303D))))
                    .clickable(onClick = onAdd).padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Text("Add New", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun RecentItemRow(item: VaultItem, onClick: () -> Unit) {
    val (icon, tint, bg) = when (item.category) {
        VaultCategory.CARD -> Triple(Icons.Outlined.CreditCard, Color(0xFF3979C3), Color(0xFFDCEBFF))
        VaultCategory.DOCUMENT -> Triple(Icons.Outlined.Description, Color(0xFF9E6DAF), Color(0xFFF0E3F6))
        VaultCategory.PASSWORD -> if (item.title.contains("Wi-Fi", true)) Triple(Icons.Outlined.Wifi, Color(0xFF1679D7), Color(0xFFDCEEFF)) else Triple(Icons.Outlined.Password, Color(0xFFCB4650), Color(0xFFFFE5E3))
        VaultCategory.NOTE -> Triple(Icons.AutoMirrored.Outlined.Article, Color(0xFFCC8C33), Color(0xFFFFF0D4))
    }
    GlassPanel(Modifier.fillMaxWidth().padding(bottom = 7.dp), radius = 18.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.category.label}  •  ${item.time}", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open item", tint = LockrColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HomeDock(onOpenCards: () -> Unit, onOpenDocuments: () -> Unit, onAdd: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 7.dp, top = 5.dp),
        radius = 29.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockAction("Cards", Icons.Outlined.CreditCard, onOpenCards)
            Box(Modifier.size(61.dp).clip(CircleShape).clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
                IridescentOrb(orbSize = 57.dp)
                Icon(Icons.Outlined.Add, contentDescription = "Add new item", tint = LockrColors.TextPrimary, modifier = Modifier.size(28.dp))
            }
            DockAction("Documents", Icons.Outlined.Description, onOpenDocuments)
        }
    }
}

@Composable
private fun DockAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(80.dp).height(58.dp).clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = LockrColors.TextPrimary, modifier = Modifier.size(23.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
