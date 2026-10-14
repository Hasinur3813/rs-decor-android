package com.example.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.Design
import com.example.ui.common.ContactActions
import com.example.ui.common.DesignCard
import com.example.ui.common.ShimmerBlock
import com.example.ui.theme.CallBlue
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.LuxuryGoldBright
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.CatalogViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CatalogViewModel,
    onDesignClick: (Design) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var searchDialogOpen by remember { mutableStateOf(false) }
    fun applyFilters(categorySlug: String, subcategorySlug: String) {
        viewModel.selectFilters(categorySlug, subcategorySlug)
        coroutineScope.launch { gridState.animateScrollToItem(0) }
    }

    // 1. Rock-solid Infinite Scroll detection using snapshotFlow
    LaunchedEffect(gridState, state.hasMorePages, state.isLoadingMore) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4
        }.collect { isNearEnd ->
            if (isNearEnd && state.hasMorePages && !state.isLoadingMore) {
                viewModel.loadNextPage()
            }
        }
    }

    // 2. Tap to Top Button visibility state
    val showTapToTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 2
        }
    }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.syncFirestore() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screen_grid")
            ) {
            // Header with the user's official app logo, title & Direct Contact buttons
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // App Logo + Title (admin-managed via settings/app)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PureWhite,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE50914)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                if (state.appSettings.logoImageUrl.isBlank()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.rs_logo),
                                        contentDescription = "RS 3D Wallpaper & Floor Logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .height(38.dp)
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(state.appSettings.logoImageUrl)
                                            .crossfade(true)
                                            .placeholder(R.drawable.rs_logo)
                                            .error(R.drawable.rs_logo)
                                            .build(),
                                        contentDescription = "RS 3D Wallpaper & Floor Logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .height(38.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = state.appSettings.appTitle.ifBlank { AppSettings.DEFAULT_APP_TITLE },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LuxuryGoldBright,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.appSettings.appSubtitle.ifBlank { AppSettings.DEFAULT_APP_SUBTITLE },
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Sync button
                        IconButton(
                            onClick = { viewModel.syncFirestore() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("sync_firestore_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "সিঙ্ক",
                                tint = LuxuryGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Persistent Animated Offline Warning Banner
                    AnimatedVisibility(
                        visible = !state.isOnline,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2A1515),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE53935)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .testTag("offline_warning_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ইন্টারনেট সংযোগ নেই",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                        Text(
                                            text = "অ্যাপ ব্যবহারের জন্য ইন্টারনেট চালু করুন।",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFFCDD2)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { viewModel.retryConnection() },
                                    enabled = !state.isCheckingConnection,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE53935),
                                        contentColor = PureWhite
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (state.isCheckingConnection) {
                                        CircularProgressIndicator(
                                            color = PureWhite,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "পুনরায় চেষ্টা",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Animated Connection Restored Banner
                    AnimatedVisibility(
                        visible = state.showOnlineRestoredBanner,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF132A1C),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4CAF50)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .testTag("online_restored_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ইন্টারনেট সংযোগ ফিরে এসেছে! ক্যাটালগ সচল।",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE8F5E9)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissOnlineRestoredBanner() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "বন্ধ করুন",
                                        tint = Color(0xFFA5D6A7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hero showcase banner (admin-managed via settings/app)
                    HeroShowcaseBanner(settings = state.appSettings)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct Contact Action Bar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { ContactActions.callNow(context, state.appSettings.phoneNumber) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CallBlue,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("header_call_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "কল করুন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                ContactActions.openWhatsApp(
                                    context,
                                    "আসসালামু আলাইকুম RS Decor! আমি আপনাদের ৩ডি ওয়ালপেপার ও ফ্লোর ডিজাইন ক্যাটালগ দেখছি। বিস্তারিত জানতে চাই।",
                                    state.appSettings.whatsappNumber
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("header_whatsapp_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "হোয়াটসঅ্যাপ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                            // Search opens a focused result browser without changing feed filters.
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { searchDialogOpen = true }
                                    .testTag("open_search_dialog")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = LuxuryGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "ডিজাইন খুঁজুন (নাম, ক্যাটাগরি বা রুম)...",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Categories: ৩ডি ওয়ালপেপার, ৩ডি সিলিং পেপার, ৩ডি ইপক্সি ফ্লোর
                    Text(
                        text = "মূল ক্যাটাগরি:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryGoldBright,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterPill(
                            title = "সবগুলো",
                            isSelected = state.selectedCategorySlug == "all",
                            onClick = { applyFilters("all", state.selectedSubcategorySlug) }
                        )
                        state.categories.forEach { cat ->
                            val catSlug = if (cat.slug.isNotBlank()) cat.slug else when {
                                cat.id.contains("wallpaper", ignoreCase = true) -> "3d-wallpaper"
                                cat.id.contains("ceiling", ignoreCase = true) -> "3d-ceiling-paper"
                                cat.id.contains("epoxy", ignoreCase = true) -> "3d-epoxy-floor"
                                else -> cat.id
                            }
                            FilterPill(
                                title = cat.getDisplayName(),
                                isSelected = state.selectedCategorySlug == catSlug,
                                onClick = { applyFilters(catSlug, state.selectedSubcategorySlug) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subcategories: ALL VISIBLE ON SCREEN (সব রুম, বেডরুম, লিভিং রুম, কিডস রুম, অফিস, কিচেন, ডাইনিং রুম)
                    Text(
                        text = "রুম ক্যাটাগরি:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryGoldBright,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            state.subcategories.forEach { sub ->
                                FilterPill(
                                    title = sub.name,
                                    isSelected = state.selectedSubcategorySlug == sub.slug,
                                    onClick = { applyFilters(state.selectedCategorySlug, sub.slug) },
                                    isSubcategory = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Results Counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ডিজাইন সমূহ (${if (state.isLoading) "…" else state.totalDesignCount})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (state.selectedCategorySlug != "all" || state.selectedSubcategorySlug != "all") {
                            Text(
                                text = "সব ফিল্টার রিসেট",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LuxuryGold,
                                modifier = Modifier
                                    .clickable {
                                       applyFilters("all", "all")
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Content states: loading → error → empty → designs.
            // Every branch reflects the real cloud state; no sample content.
            if (state.isLoading && state.allDesigns.isEmpty()) {
                items(4) {
                    DesignSkeletonCard()
                }
            } else if (state.filteredDesigns.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    val catalogEmpty = state.allDesigns.isEmpty()
                    val streamFailed = state.designsError != null || state.categoriesError != null
                    when {
                        !state.isOnline && catalogEmpty -> {
                            OfflineCatalogCard(
                                isCheckingConnection = state.isCheckingConnection,
                                onRetry = { viewModel.retryConnection() }
                            )
                        }
                        streamFailed && catalogEmpty && state.isOnline -> {
                            CatalogErrorCard(
                                message = state.designsError ?: state.categoriesError
                                    ?: "ডিজাইন লোড করা যায়নি।",
                                onRetry = { viewModel.retryLoading() }
                            )
                        }
                        catalogEmpty -> {
                            CatalogEmptyCard()
                        }
                        else -> {
                            FilterEmptyCard()
                        }
                    }
                }
            } else {
                // Paged Designs (Infinite Scroll Stream)
                items(state.pagedDesigns, key = { it.id }) { design ->
                    DesignCard(
                        design = design,
                        onDesignClick = { onDesignClick(it) }
                    )
                }

                // Pagination Loading Indicator & Fallback Button
                if (state.hasMorePages) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!state.isOnline) {
                                // Offline Pagination Warning Notice
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WifiOff,
                                                contentDescription = null,
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "পরবর্তী ডিজাইনগুলো দেখতে ইন্টারনেট অন করুন",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.retryConnection() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = LuxuryGold,
                                                contentColor = DarkObsidian
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(text = "পুনরায় চেষ্টা", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (state.isLoadingMore) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = LuxuryGold,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "পরবর্তী ডিজাইনগুলো লোড হচ্ছে...",
                                        fontSize = 13.sp,
                                        color = LuxuryGoldBright,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.loadNextPage() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = LuxuryGoldBright
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "আরও ডিজাইন দেখুন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Tap to Top Floating Button
        AnimatedVisibility(
            visible = showTapToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        gridState.animateScrollToItem(0)
                    }
                },
                containerColor = LuxuryGold,
                contentColor = DarkObsidian,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("tap_to_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "উপরে যান",
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        if (searchDialogOpen) {
            SearchDesignDialog(
                recentDesigns = state.pagedDesigns,
                searchResults = state.searchResults,
                isSearching = state.isSearching,
                searchError = state.searchError,
                onSearchQueryChange = { viewModel.searchDesigns(it) },
                onDismiss = {
                    searchDialogOpen = false
                    viewModel.clearSearch()
                },
                onDesignClick = { design ->
                    searchDialogOpen = false
                    viewModel.clearSearch()
                    onDesignClick(design)
                }
            )
        }
    }
}

@Composable
private fun DesignSkeletonCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("design_skeleton_card")
    ) {
        Column {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(14.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(11.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchDesignDialog(
    recentDesigns: List<Design>,
    searchResults: List<Design>,
    isSearching: Boolean,
    searchError: String?,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onDesignClick: (Design) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = if (query.isBlank()) recentDesigns.take(24) else searchResults
    val configuration = LocalConfiguration.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkObsidian,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = (configuration.screenHeightDp - 32).dp)
                .testTag("design_search_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ডিজাইন খুঁজুন",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "নাম, ক্যাটাগরি বা রুম দিয়ে সার্চ করুন",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "বন্ধ করুন",
                            tint = TextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearchQueryChange(it)
                    },
                    placeholder = {
                        Text("যেমন: বেডরুম, ওয়ালপেপার...", color = TextSecondary)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = LuxuryGold)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = {
                                query = ""
                                onSearchQueryChange("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "মুছুন", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("design_search_field")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (query.isBlank()) "সাম্প্রতিক ডিজাইন" else "সার্চের ফলাফল",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryGoldBright
                    )
                    Text(
                        text = if (query.isBlank()) {
                            "সর্বশেষ ${matches.size}টি"
                        } else {
                            "${matches.size}${if (matches.size >= 40) "+" else ""}টি ডিজাইন"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LuxuryGold, modifier = Modifier.size(30.dp))
                    }
                } else if (searchError != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(searchError, color = TextSecondary, textAlign = TextAlign.Center)
                        TextButton(onClick = { onSearchQueryChange(query) }) {
                            Text("আবার চেষ্টা করুন", color = LuxuryGoldBright)
                        }
                    }
                } else if (matches.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = LuxuryGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = when {
                                query.isBlank() -> "এখনো কোনো ডিজাইন নেই"
                                query.trim().length < 2 -> "কমপক্ষে ২টি অক্ষর লিখুন"
                                else -> "মিলযুক্ত ডিজাইন পাওয়া যায়নি"
                            },
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        if (query.isNotBlank()) {
                            TextButton(onClick = {
                                query = ""
                                onSearchQueryChange("")
                            }) {
                                Text("সার্চ মুছুন", color = LuxuryGoldBright)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("design_search_results")
                    ) {
                        items(matches, key = { it.id }) { design ->
                            DesignCard(
                                design = design,
                                onDesignClick = onDesignClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroShowcaseBanner(
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (settings.heroImageUrl.isBlank()) {
                // Bundled art: identical look to the shipped app, works offline.
                Image(
                    painter = painterResource(id = R.drawable.hero_banner_interior),
                    contentDescription = "RS Decor Showcase",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Admin-managed hero image from Cloudinary, with safe fallbacks.
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(settings.heroImageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.hero_banner_interior)
                        .error(R.drawable.hero_banner_interior)
                        .build(),
                    contentDescription = "RS Decor Showcase",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // High-contrast gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                DarkObsidian.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = settings.heroTitle.ifBlank { AppSettings.DEFAULT_HERO_TITLE },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PureWhite
                )
                Text(
                    text = settings.heroSubtitle.ifBlank { AppSettings.DEFAULT_HERO_SUBTITLE },
                    fontSize = 12.sp,
                    color = LuxuryGoldBright,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = settings.heroDescription.ifBlank { AppSettings.DEFAULT_HERO_DESCRIPTION },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CatalogLoadingCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .testTag("catalog_loading_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = LuxuryGold, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ডিজাইন লোড হচ্ছে...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "সর্বশেষ ক্যাটালগ আনা হচ্ছে",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun OfflineCatalogCard(
    isCheckingConnection: Boolean,
    onRetry: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE53935).copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .testTag("full_offline_error_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B1717)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "অফলাইন",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ইন্টারনেট সংযোগ প্রয়োজন",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "আর এস ডেকর ৩ডি ডিজাইন ক্যাটালগ ও ছবি দেখতে একটি সক্রিয় ইন্টারনেট সংযোগ (ওয়াইফাই বা মোবাইল ডেটা) প্রয়োজন।",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRetry,
                enabled = !isCheckingConnection,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxuryGold,
                    contentColor = DarkObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("offline_retry_button")
            ) {
                if (isCheckingConnection) {
                    CircularProgressIndicator(
                        color = DarkObsidian,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সংযোগ যাচাই করা হচ্ছে...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "পুনরায় চেষ্টা করুন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LuxuryGoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "টিপস: ফোনের Wi-Fi বা মোবাইল ডেটা অন করে আবার চেষ্টা করুন।",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, LuxuryGold.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .testTag("catalog_error_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "ত্রুটি",
                    tint = LuxuryGoldBright,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ক্যাটালগ লোড করা যায়নি",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxuryGold,
                    contentColor = DarkObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("catalog_retry_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "আবার চেষ্টা করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogEmptyCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .testTag("catalog_empty_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = LuxuryGold,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "নতুন ডিজাইন শীঘ্রই আসছে",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "ক্যাটালগে এখনো কোনো ডিজাইন যোগ হয়নি। কিছুক্ষণ পর আবার দেখুন।",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun FilterEmptyCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = LuxuryGold,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "কোনো ডিজাইন পাওয়া যায়নি",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "ফিল্টার পরিবর্তন করুন অথবা অন্য নাম দিয়ে সার্চ করুন।",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun FilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isSubcategory: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) LuxuryGold else DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) LuxuryGoldBright else DarkCardBorder
        ),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("filter_pill_${title.replace(" ", "_")}")
    ) {
        Text(
            text = title,
            fontSize = if (isSubcategory) 12.sp else 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DarkObsidian else TextPrimary,
            modifier = Modifier.padding(
                horizontal = if (isSubcategory) 11.dp else 14.dp,
                vertical = 6.dp
            )
        )
    }
}
