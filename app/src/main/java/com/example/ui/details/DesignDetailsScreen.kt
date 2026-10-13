package com.example.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppSettings
import com.example.data.model.Design
import com.example.ui.common.ContactActions
import com.example.ui.theme.CallBlue
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.LuxuryGoldBright
import com.example.ui.theme.LuxuryGoldContainer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen

@Composable
fun DesignDetailsScreen(
    design: Design,
    appSettings: AppSettings = AppSettings(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val fallbackResId = when {
        design.categorySlug.contains("wallpaper", ignoreCase = true) -> com.example.R.drawable.cat_3d_wallpaper
        design.categorySlug.contains("ceiling", ignoreCase = true) -> com.example.R.drawable.cat_3d_ceiling_paper
        design.categorySlug.contains("epoxy", ignoreCase = true) -> com.example.R.drawable.cat_3d_epoxy
        else -> com.example.R.drawable.hero_banner_interior
    }
    var isFullscreenOpen by remember { mutableStateOf(false) }

    // Primary inline image zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp) // space for fixed bottom CTAs
                .verticalScroll(rememberScrollState())
        ) {
            // High-Resolution Image Container with Enhanced Zoom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(DarkSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(design.imageUrl)
                        .crossfade(true)
                        .placeholder(fallbackResId)
                        .error(fallbackResId)
                        .build(),
                    contentDescription = design.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        // 1. Double tap gesture for smooth 1x <-> 2.5x toggle
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (scale > 1.1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                        val maxPanX = 350f * (2.5f - 1f)
                                        val maxPanY = 350f * (2.5f - 1f)
                                        val targetX = (size.width / 2f - tapOffset.x) * 1.5f
                                        val targetY = (size.height / 2f - tapOffset.y) * 1.5f
                                        offset = Offset(
                                            targetX.coerceIn(-maxPanX, maxPanX),
                                            targetY.coerceIn(-maxPanY, maxPanY)
                                        )
                                    }
                                }
                            )
                        }
                        // 2. Multi-touch pinch-to-zoom and panning gesture
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxPanX = 400f * (scale - 1f)
                                    val maxPanY = 400f * (scale - 1f)
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                        y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .testTag("details_zoom_image")
                )

                // Top gradient vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                )

                // Top Bar: Back & Fullscreen buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkObsidian.copy(alpha = 0.85f))
                            .testTag("details_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = PureWhite
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { isFullscreenOpen = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkObsidian.copy(alpha = 0.85f))
                                .testTag("details_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "ফুলস্ক্রিন জুম",
                                tint = LuxuryGoldBright
                            )
                        }
                    }
                }

                // Enhanced Zoom Controls Overlay (Bottom-End of image)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkObsidian.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Zoom Out (-) Button
                        IconButton(
                            onClick = {
                                val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                scale = newScale
                                if (scale == 1f) offset = Offset.Zero
                            },
                            enabled = scale > 1.05f,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "জুম কমান",
                                tint = if (scale > 1.05f) PureWhite else TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Current Scale Badge
                        Text(
                            text = "%.1fx".format(scale),
                            color = LuxuryGoldBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Zoom In (+) Button
                        IconButton(
                            onClick = {
                                scale = (scale + 0.5f).coerceAtMost(5f)
                            },
                            enabled = scale < 5f,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "জুম বাড়ান",
                                tint = if (scale < 5f) PureWhite else TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 1x Reset button (shown when zoomed)
                        if (scale > 1.05f) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(LuxuryGoldContainer)
                                    .clickable {
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "রিসেট",
                                    tint = LuxuryGoldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Interactive Zoom Tip Hint (Bottom-Start)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkObsidian.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ডাবল-ট্যাপ বা পিঞ্চ করে জুম করুন",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Design Details: Name, Category, and Subcategory ONLY
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Category & Subcategory Tags Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LuxuryGoldContainer)
                            .border(1.dp, LuxuryGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = design.category,
                            color = LuxuryGoldBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Subcategory Room Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = design.subcategory,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Design Name in large, prominent Bangla typography
                Text(
                    text = design.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Showcase info card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "মূল ক্যাটাগরি:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = design.category,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxuryGoldBright
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "প্রযোজ্য রুম:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = design.subcategory,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "সার্ভিস এলাকা:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "সমগ্র বাংলাদেশ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ordering instruction in Bangla
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceVariant.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💡 অর্ডার ও ফিটিং পরামর্শ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryGoldBright
                        )
                        Text(
                            text = "এই ডিজাইনটি আপনার রুমের নির্দিষ্ট দেয়াল, সিলিং বা ফ্লোরের মাপ অনুযায়ী কাস্টম তৈরি ও প্রফেশনাল ফিটিং করে দেয়া হবে। সরাসরি কল করুন অথবা হোয়াটসঅ্যাপে মেসেজ পাঠান।",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Fixed Bottom Action Bar with TWO direct CTA buttons
        Surface(
            color = DarkSurface,
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Call Now button
                Button(
                    onClick = { ContactActions.callNow(context, appSettings.phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CallBlue,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("details_call_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "কল করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = appSettings.phoneDisplay,
                                fontSize = 11.sp,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // WhatsApp Inquiry button
                Button(
                    onClick = {
                        val message = "আসসালামু আলাইকুম RS Decor! আমি '${design.name}' (${design.category} - ${design.subcategory}) ডিজাইনটি সম্পর্কে বিস্তারিত ও কোটেশন জানতে আগ্রহী।"
                        ContactActions.openWhatsApp(context, message, appSettings.whatsappNumber)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("details_whatsapp_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "হোয়াটসঅ্যাপ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ইনস্ট্যান্ট মেসেজ",
                                fontSize = 11.sp,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Enhanced Fullscreen Interactive Zoom Dialog
        if (isFullscreenOpen) {
            Dialog(
                onDismissRequest = { isFullscreenOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    var fullScale by remember { mutableFloatStateOf(1f) }
                    var fullOffset by remember { mutableStateOf(Offset.Zero) }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(design.imageUrl)
                            .crossfade(true)
                            .placeholder(fallbackResId)
                            .error(fallbackResId)
                            .build(),
                        contentDescription = design.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = fullScale,
                                scaleY = fullScale,
                                translationX = fullOffset.x,
                                translationY = fullOffset.y
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (fullScale > 1.1f) {
                                            fullScale = 1f
                                            fullOffset = Offset.Zero
                                        } else {
                                            fullScale = 3f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    fullScale = (fullScale * zoom).coerceIn(1f, 6f)
                                    if (fullScale > 1f) {
                                        val maxPanX = 600f * (fullScale - 1f)
                                        val maxPanY = 600f * (fullScale - 1f)
                                        fullOffset = Offset(
                                            x = (fullOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                            y = (fullOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                    } else {
                                        fullOffset = Offset.Zero
                                    }
                                }
                            }
                    )

                    // Close Button
                    IconButton(
                        onClick = { isFullscreenOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkObsidian.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = PureWhite
                        )
                    }

                    // Fullscreen Floating Zoom Bar (+ / - / Reset)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = DarkObsidian.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 80.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val next = (fullScale - 0.5f).coerceAtLeast(1f)
                                    fullScale = next
                                    if (fullScale == 1f) fullOffset = Offset.Zero
                                },
                                enabled = fullScale > 1.05f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "কমান",
                                    tint = if (fullScale > 1.05f) PureWhite else TextSecondary.copy(alpha = 0.5f)
                                )
                            }

                            Text(
                                text = "%.1fx".format(fullScale),
                                color = LuxuryGoldBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    fullScale = (fullScale + 0.5f).coerceAtMost(6f)
                                },
                                enabled = fullScale < 6f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "বাড়ান",
                                    tint = if (fullScale < 6f) PureWhite else TextSecondary.copy(alpha = 0.5f)
                                )
                            }

                            if (fullScale > 1.05f) {
                                IconButton(
                                    onClick = {
                                        fullScale = 1f
                                        fullOffset = Offset.Zero
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "রিসেট",
                                        tint = LuxuryGoldBright
                                    )
                                }
                            }
                        }
                    }

                    // Bottom info in fullscreen
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = design.name,
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${design.category} • ${design.subcategory}",
                            color = LuxuryGoldBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
