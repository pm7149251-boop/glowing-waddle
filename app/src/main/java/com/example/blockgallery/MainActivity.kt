package com.example.blockgallery

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Blender UI Theme Colors
val BgBase = Color(0xFF1C1C1C)
val BgHeader = Color(0xFF232323)
val BgPanel = Color(0xFF303030)
val BgCard = Color(0xFF1A1A1A)
val TextMain = Color(0xFFCCCCCC)
val TextMuted = Color(0xFF8C8C8C)
val AccentBlue = Color(0xFF4A90E2)
val BorderDark = Color(0xFF1A1A1A)
val UploadedBg = Color(0xFF2F4F2F)
val UploadedBorder = Color(0xFF3A6B3A)
val UploadedText = Color(0xFF98E898)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Disable default Android ripple/tap flash across the app
            CompositionLocalProvider(LocalIndication provides NoOpIndication) {
                BlenderGalleryApp()
            }
        }
    }
}

// Add this custom transparent indication object at the bottom of MainActivity.kt
private object NoOpIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(
        interactionSource: androidx.compose.foundation.interaction.InteractionSource
    ): androidx.compose.ui.node.DelegatableNode {
        return NoOpIndicationNode()
    }
}

private class NoOpIndicationNode : androidx.compose.ui.Modifier.Node(), androidx.compose.ui.node.DrawModifierNode {
    override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() {
        drawContent() // Draws the content without any ripple overlay
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlenderGalleryApp() {
    val context = LocalContext.current
    var blocks by remember { mutableStateOf(BlockRepository.loadBlocks(context)) }
    var showAddModal by remember { mutableStateOf(false) }
    var blockToDelete by remember { mutableStateOf<BlockItem?>(null) }

    val configuration = LocalConfiguration.current
    val isMobilePortrait = configuration.screenWidthDp < 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .systemBarsPadding()
    ) {
        // Blender Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(BgHeader)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF444444))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddModal = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "Add Block", color = Color.White, fontSize = 13.sp)
            }
        }

        HorizontalDivider(color = BorderDark, thickness = 1.dp)

        // Main Workspace Area
        Box(modifier = Modifier.fillMaxSize()) {
            if (blocks.isEmpty()) {
                Text(
                    text = "No blocks loaded.\n\nLong-press a block to delete it.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = if (isMobilePortrait) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 260.dp),
                    contentPadding = PaddingValues(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(blocks, key = { it.id }) { block ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BgCard)
                                .border(1.dp, Color(0xFF222222), RoundedCornerShape(4.dp))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { /* Static view */ },
                                    onLongClick = { blockToDelete = block }
                                )
                        ) {
                            IsometricBlockCanvas(block = block)
                        }
                    }
                }
            }
        }
    }

    // Add Block Modal
    if (showAddModal) {
        AddBlockModal(
            onDismiss = { showAddModal = false },
            onSave = { newBlock ->
                val updated = blocks + newBlock
                blocks = updated
                BlockRepository.saveBlocks(context, updated)
                showAddModal = false
            }
        )
    }

    // Long-Press Delete Confirmation Dialog
    blockToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { blockToDelete = null },
            containerColor = BgPanel,
            titleContentColor = Color.White,
            textContentColor = TextMain,
            title = { Text("Delete Block", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("Delete this block from memory?", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = blocks.filter { it.id != target.id }
                    blocks = updated
                    BlockRepository.saveBlocks(context, updated)
                    blockToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFCC4343))
                }
            },
            dismissButton = {
                TextButton(onClick = { blockToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun AddBlockModal(
    onDismiss: () -> Unit,
    onSave: (BlockItem) -> Unit
) {
    val context = LocalContext.current
    var topB64 by remember { mutableStateOf<String?>(null) }
    var sideB64 by remember { mutableStateOf<String?>(null) }
    var faceB64 by remember { mutableStateOf<String?>(null) }
    var activeSlot by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = BlockRepository.uriTo16pxBase64(context, uri)
            when (activeSlot) {
                "top" -> topB64 = encoded
                "side" -> sideB64 = encoded
                "face" -> faceB64 = encoded
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(BgPanel)
                .border(1.dp, BorderDark, RoundedCornerShape(4.dp))
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgHeader)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create Pixel Block (16x16)", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "✕",
                    color = TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                )
            }

            HorizontalDivider(color = BorderDark, thickness = 1.dp)

            // Modal Body
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Upload Textures (Auto-converts to 16px).\nTip: Upload just 1 texture and it will auto-fill all 3 sides.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                // Option Order: Top, Side, Face
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UploadSlotButton(
                        label = "Top",
                        isUploaded = topB64 != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        activeSlot = "top"
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    UploadSlotButton(
                        label = "Side",
                        isUploaded = sideB64 != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        activeSlot = "side"
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    UploadSlotButton(
                        label = "Face",
                        isUploaded = faceB64 != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        activeSlot = "face"
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }

                // Save Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AccentBlue)
                        .border(1.dp, Color(0xFF2B629E), RoundedCornerShape(3.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val block = BlockRepository.createSmartBlock(topB64, sideB64, faceB64)
                            if (block == null) {
                                Toast.makeText(context, "Please upload at least one texture.", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(block)
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save to Memory", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun UploadSlotButton(
    label: String,
    isUploaded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isUploaded) UploadedBg else BgCard
    val borderColor = if (isUploaded) UploadedBorder else BorderDark
    val textColor = if (isUploaded) UploadedText else TextMain
    val displayText = if (isUploaded) "$label ✓" else label

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = displayText, color = textColor, fontSize = 13.sp)
    }
}