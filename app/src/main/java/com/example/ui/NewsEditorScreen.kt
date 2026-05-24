package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NewsAnalysis
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsEditorScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.historyState.collectAsStateWithLifecycle()
    
    val selectedFileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val selectedFileMime by viewModel.selectedFileMime.collectAsStateWithLifecycle()
    
    var isMobileInstallationGuideVisible by remember { mutableStateOf(false) }
    var fileErrorMessage by remember { mutableStateOf<String?>(null) }
    
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            var name = "অডিও/ভিডিও ফাইল"
            var size = 0L
            
            try {
                contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            name = cursor.getString(nameIndex)
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex >= 0) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
                
                val mime = contentResolver.getType(it) ?: ""
                
                // Max file size limit: 12MB (To match safe Base64 streaming payload bounds)
                val maxBytes = 12 * 1024 * 1024
                if (size > maxBytes) {
                    val sizeInMb = size / (1024 * 1024)
                    fileErrorMessage = "আপনার ফাইলটির সাইজ ${sizeInMb}MB। সরাসরি ১২ মেগাবাইটের বেশি ফাইল এআই-তে এম্বেড করা যায় না। দয়া করে বক্তব্যের টেক্সট টাইপ করুন অথবা ছোট আকারের অডিও/ভিডিও ফাইল দিন।"
                } else {
                    fileErrorMessage = null
                    val inputStream: InputStream? = contentResolver.openInputStream(it)
                    val bytes = inputStream?.use { stream -> stream.readBytes() }
                    if (bytes != null) {
                        viewModel.setUploadedFile(name, mime, bytes)
                    } else {
                        fileErrorMessage = "ফাইলটি পড়া যায়নি। আবার চেষ্টা করুন!"
                    }
                }
            } catch (e: Exception) {
                fileErrorMessage = "ফাইল লোড করার সময় একটি ত্রুটি ঘটেছে: ${e.localizedMessage}"
            }
        }
    }
    
    var isHistoryExpanded by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HEAD VIBE HEADER ZONE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Custom pulsating breaking news dot icon
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    
                    Column {
                        Text(
                            text = "চিফ নিউজ এডিটর",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AI Headline & Agenda Optimizer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                // LIVE Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "LIVE EDITOR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- 1.2 MOBILE APP INSTALLATION INSTRUCTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMobileInstallationGuideVisible = !isMobileInstallationGuideVisible },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = "Install Help",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "মোবাইলে এই অ্যাপটি ইনস্টল বা নামানোর নিয়ম",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isMobileInstallationGuideVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle Guide",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                AnimatedVisibility(
                    visible = isMobileInstallationGuideVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
                        
                        Text(
                            text = "আপনার অ্যান্ড্রয়েড মোবাইলে অ্যাপটি সরাসরি ইনস্টল করতে নিচের সহজ ধাপগুলো অনুসরণ করুন:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        InstallationStep(number = "১", text = "AI Studio Build স্ক্রিনের ডানদিকের সেটিংস (Settings) আইকনে চাপুন।")
                        InstallationStep(number = "২", text = "সেখান থেকে \"Download APK\" বা \"Export Project to ZIP\" সিলেক্ট করুন।")
                        InstallationStep(number = "৩", text = "ডাউনলোড হওয়া APK ফাইলটি আপনার মোবাইলে ট্রান্সফার বা সরাসরি ফোনে ডাউনলোড করুন।")
                        InstallationStep(number = "৪", text = "মোবাইলের সেটিংস থেকে \"Install Unknown Apps\" (অজানা উৎস) সক্রিয় করুন।")
                        InstallationStep(number = "৫", text = "সবশেষে ফাইলটি ওপেন করে \"Install\" বাটনে প্রেস করুন। ব্যস, আপনার লাইভ এডিটর অ্যাপ মোবাইলে প্রস্তুত!")
                    }
                }
            }
        }

        // --- 2. INPUT DECK ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Newspaper,
                        contentDescription = "Statement Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "বক্তব্যের ট্রান্সক্রিপ্ট যুক্ত করুন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("transcript_input"),
                    placeholder = {
                        Text(
                            text = "এখানে রাজনৈতিক নেতা, মন্ত্রী, বা গুরুত্বপূর্ণ ব্যক্তির অডিও/ভিডিও থেকে প্রাপ্ত বক্তব্য বা লিখিত তথ্যটি পেস্ট করুন...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 10,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "অক্ষর সংখ্যা: ${inputText.length}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (inputText.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.updateInputText("") },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মুছে ফেলুন", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = "Upload Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "অথবা অডিও / ভিডিও ফাইল আপলোড করুন",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                fileErrorMessage?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { fileErrorMessage = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                if (selectedFileName != null) {
                    // Selected file layout indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val fileIcon = if (selectedFileMime?.startsWith("video") == true) {
                            Icons.Filled.VideoLibrary
                        } else {
                            Icons.Filled.Audiotrack
                        }
                        
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = "File Type",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFileName ?: "অডিও/ভিডিও ফাইল",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "টাইপ: ${selectedFileMime ?: "অজ্ঞাত ফাইল"} • সাইজ: ${((viewModel.selectedFileBytes?.size ?: 0) / 1024f / 1024f).let { String.format("%.2f", it) }} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.clearSelectedFile() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Deselect File",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // File selection dashed box area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                openDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Upload Cloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "ভিডিও বা অডিও ফাইল আপলোড করুন",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "সর্বোচ্চ ১২ মেগাবাইটের ফাইল এআই সরাসরি শুনতে পাবে",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // --- QUICK SAMPLE CHIPS ---
                Text(
                    text = "দ্রুত ডেমো ডেটা লোড করুন:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SampleChip(label = "বাজার ও অর্থনীতি") { viewModel.loadSample(1) }
                    SampleChip(label = "আইনশৃঙ্খলা") { viewModel.loadSample(2) }
                    SampleChip(label = "যোগাযোগ ব্যবস্থা") { viewModel.loadSample(3) }
                    SampleChip(label = "শিক্ষা ও পরীক্ষা") { viewModel.loadSample(4) }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                // SUBMIT BUTTON
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.generateHeadlines()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Execute",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "বিশ্লেষণ ও শিরোনাম জেনারেট করুন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // --- 3. DYNAMIC UI STATE DECK (LOADING/ERROR/SUCCESS) ---
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "UI State Stream"
        ) { state ->
            when (state) {
                is GeneratorUiState.Idle -> {
                    IntroPanel()
                }
                
                is GeneratorUiState.Loading -> {
                    LoadingPanel()
                }
                
                is GeneratorUiState.Error -> {
                    ErrorPanel(message = state.message)
                }
                
                is GeneratorUiState.Success -> {
                    ResultsPanel(
                        analysis = state.analysis,
                        onCopy = { label, valStr -> viewModel.copyToClipboard(context, label, valStr) },
                        onShare = { valStr -> viewModel.shareHeadline(context, valStr) }
                    )
                }
            }
        }

        // --- 4. HISTORY PANEL (COLLAPSIBLE BOX) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Collapsible header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHistoryExpanded = !isHistoryExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "History Icon",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "বিশ্লেষণের আর্কাইভ (${history.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(onClick = { isHistoryExpanded = !isHistoryExpanded }) {
                        Icon(
                            imageVector = if (isHistoryExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Toggle History"
                        )
                    }
                }
                
                if (isHistoryExpanded) {
                    if (history.isEmpty()) {
                        Text(
                            text = "কোনো পূর্ববর্তী বিশ্লেষণ খুঁজে পাওয়া যায়নি। নতুন একটি শুরু করতে বক্তব্য দিন!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    } else {
                        // Clear All Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.clearAllHistory() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete All", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("আর্কাইভ মুছুন")
                            }
                        }
                        
                        // History List Box
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            history.take(6).forEach { item ->
                                HistoryItemRow(
                                    item = item,
                                    onLoad = { viewModel.loadFromHistory(item) },
                                    onDelete = { viewModel.deleteFromHistory(item) }
                                )
                            }
                            
                            if (history.size > 6) {
                                Text(
                                    text = "আর্কাইভে আরও ${history.size - 6}টি আইটেম সংরক্ষিত আছে।",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Custom sample text loader helper
@Composable
fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun IntroPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = "Mic Info",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "শিরোনাম জেনারেশন স্টুডিও",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "নেতা বা প্রভাবশালীদের যে কোনো বক্তব্য বিশ্লেষণ করে টিভি স্ক্রল, ব্রেকিং নিউজ এবং থাম্বনেইল ক্যাপশনের জন্য হুবহু উদ্ধৃতি সহ চমৎকার ১০টি নিউজ রেডি টাইটেল বের করতে ট্রান্সক্রিপ্ট যোগ করুন বা ডেমো বাটনগুলোতে চাপ দিন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun LoadingPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "চিফ নিউজ এডিটরের বিশ্লেষণ সক্রিয় রয়েছে...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "এডিটিং টেবিলে বক্তব্যের গভীর এজেন্ডা ও পাওয়ার ওয়ার্ড খোঁজা হচ্ছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun ErrorPanel(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error Info",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "অনুরোধটি সম্পন্ন করা যায়নি",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultsPanel(
    analysis: NewsAnalysis,
    onCopy: (String, String) -> Unit,
    onShare: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CHIEF EDITOR REMARKS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Assignment,
                        contentDescription = "Editor Remarks",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "সম্পাদকের মূল্যায়ন ও এজেন্ডা পর্যবেক্ষণ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Agenda
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Announcement, contentDescription = "Agenda", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "বক্তার মূল এজেন্ডা:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = analysis.editorAgenda,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }

                // Public Impact
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Groups, contentDescription = "Impact", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "জনসাধারণের ওপর প্রভাব:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = analysis.editorPublicImpact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }

                // Power words
                if (analysis.editorPowerWordsJoined.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Star, contentDescription = "Power Words", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "সনাক্তকৃত পাওয়ার ওয়ার্ডস (Power Words):",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            analysis.editorPowerWordsJoined.split(",").forEach { word ->
                                val trimmed = word.trim()
                                if (trimmed.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = trimmed,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 10 TITLES LISTS BY CATEGORY ---
        Text(
            text = "৫টি গুরুত্বপূর্ণ ক্যাটাগরির সংবাদ শিরোনাম:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // 1. Hard News (তথ্যভিত্তিক)
        CategoryHeadlineCard(
            categoryTitle = "১. Hard News Headline (তথ্যভিত্তিক)",
            categorySubtitle = "সম্পূর্ণ সত্য, বস্তুনিষ্ঠ ও সংবাদ গুরুত্ব সম্পন্ন তথ্যনির্ভর শিরোনাম।",
            icon = Icons.Filled.Newspaper,
            color = MaterialTheme.colorScheme.primary,
            title1 = analysis.hardNews1,
            tag1 = "hard_1",
            title2 = analysis.hardNews2,
            tag2 = "hard_2",
            onCopy = onCopy,
            onShare = onShare
        )

        // 2. Direct Quote (উদ্ধৃতিমূলক)
        CategoryHeadlineCard(
            categoryTitle = "২. Direct Quote Headline (উদ্ধৃতিমূলক)",
            categorySubtitle = "বক্তার হুবহু শক্তিশালী কথার নিখুঁত উদ্ধৃতিমূলক শিরোনাম।",
            icon = Icons.Filled.FormatQuote,
            color = MaterialTheme.colorScheme.secondary,
            title1 = analysis.directQuote1,
            tag1 = "quote_1",
            title2 = analysis.directQuote2,
            tag2 = "quote_2",
            onCopy = onCopy,
            onShare = onShare
        )

        // 3. Warning/Action (হুঁশিয়ারিমূলক)
        CategoryHeadlineCard(
            categoryTitle = "৩. Warning/Action Headline (হুঁশিয়ারিমূলক)",
            categorySubtitle = "কঠোর প্রশাসনিক পদক্ষেপ, হুঁশিয়ারি, বা শাস্তিমূলক বার্তা সমৃদ্ধ টাইটেল।",
            icon = Icons.Filled.Gavel,
            color = MaterialTheme.colorScheme.primary,
            title1 = analysis.warningAction1,
            tag1 = "warn_1",
            title2 = analysis.warningAction2,
            tag2 = "warn_2",
            onCopy = onCopy,
            onShare = onShare
        )

        // 4. Political/Conflict (রাজনৈতিক/আক্রমণাত্মক)
        CategoryHeadlineCard(
            categoryTitle = "৪. Political/Conflict Headline (রাজনৈতিক/আক্রমণাত্মক)",
            categorySubtitle = "রাজনৈতিক ডিলিং, প্রতিপক্ষ আক্রমণ বা তর্কমূলক শক্তিশালী শিরোনাম।",
            icon = Icons.Filled.Campaign,
            color = MaterialTheme.colorScheme.primary,
            title1 = analysis.politicalConflict1,
            tag1 = "pol_1",
            title2 = analysis.politicalConflict2,
            tag2 = "pol_2",
            onCopy = onCopy,
            onShare = onShare
        )

        // 5. Curiosity/Question (কৌতূহলোদ্দীপক)
        CategoryHeadlineCard(
            categoryTitle = "৫. Curiosity/Question Headline (কৌতূহলোদ্দীপক)",
            categorySubtitle = "টকশো, থাম্বনেইল বা সোশ্যাল ক্যাপশনে কৌতূহল উসকে দেওয়ার চরম টাইটেল।",
            icon = Icons.Filled.Help,
            color = MaterialTheme.colorScheme.secondary,
            title1 = analysis.curiosityQuestion1,
            tag1 = "cur_1",
            title2 = analysis.curiosityQuestion2,
            tag2 = "cur_2",
            onCopy = onCopy,
            onShare = onShare
        )
    }
}

@Composable
fun CategoryHeadlineCard(
    categoryTitle: String,
    categorySubtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title1: String,
    tag1: String,
    title2: String,
    tag2: String,
    onCopy: (String, String) -> Unit,
    onShare: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = categorySubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Headline 1
            HeadlineRowItem(
                indexText = "১",
                headlineText = title1,
                testTagPrefix = "copy_button_$tag1",
                onCopy = { onCopy(categoryTitle, title1) },
                onShare = { onShare(title1) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

            // Headline 2
            HeadlineRowItem(
                indexText = "২",
                headlineText = title2,
                testTagPrefix = "copy_button_$tag2",
                onCopy = { onCopy(categoryTitle, title2) },
                onShare = { onShare(title2) }
            )
        }
    }
}

@Composable
fun HeadlineRowItem(
    indexText: String,
    headlineText: String,
    testTagPrefix: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored badge index circles
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = indexText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Text(
            text = headlineText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            lineHeight = 22.sp
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // COPY Action Object with standard 48dp target via IconButton
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(40.dp)
                    .testTag(testTagPrefix)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Headline Text",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // SHARE Action Object with standard 48dp target via IconButton
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share Headline Text",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: NewsAnalysis,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(item.timestamp) {
        try {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(item.timestamp))
        } catch (e: Exception) {
            "Just now"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Newspaper,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.transcript,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "পর্যবেক্ষণ: ${item.editorAgenda.take(35)}...",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Restore Button
                IconButton(onClick = onLoad, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = "Restore Archive Text",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Archive Row",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InstallationStep(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = 11.sp
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
    }
}
