package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.NewsAnalysis
import com.example.data.NewsAnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GeneratorUiState {
    object Idle : GeneratorUiState
    object Loading : GeneratorUiState
    data class Success(val analysis: NewsAnalysis) : GeneratorUiState
    data class Error(val message: String) : GeneratorUiState
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NewsAnalysisRepository
    val historyState: StateFlow<List<NewsAnalysis>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NewsAnalysisRepository(database.newsAnalysisDao())
        historyState = repository.allAnalyses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Idle)
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _selectedFileMime = MutableStateFlow<String?>(null)
    val selectedFileMime: StateFlow<String?> = _selectedFileMime.asStateFlow()

    var selectedFileBytes: ByteArray? = null
        private set

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun setUploadedFile(name: String, mime: String, bytes: ByteArray) {
        _selectedFileName.value = name
        _selectedFileMime.value = mime
        selectedFileBytes = bytes
    }

    fun clearSelectedFile() {
        _selectedFileName.value = null
        _selectedFileMime.value = null
        selectedFileBytes = null
    }

    fun loadSample(index: Int) {
        clearSelectedFile()
        val sample = when (index) {
            1 -> "বাজারে নিত্যপ্রয়োজনীয় জিনিসপত্রের দাম যেভাবে বাড়ছে, তা নিয়ে আমরা চিন্তিত। কিছু অসাধু ব্যবসায়ী সিন্ডিকেট করে কৃত্রিম সংকট তৈরি করছে। আমি স্পষ্ট করে বলে দিতে চাই, যারা চাল-ডালের দাম বাড়াচ্ছে তাদের কোনোভাবেই ছাড় দেওয়া হবে না। আগামী ২৪ ঘণ্টার মধ্যে বাজার মনিটরিং শুরু হবে। আর বিরোধীরা তো শুধু বিবৃতি দেয়, তারা তো আর বাজার করতে যায় না।"
            2 -> "শহরে কিশোর গ্যাংয়ের দৌরাত্ম্য আমরা কোনোভাবেই মেনে নেব না। পাড়ায় পাড়ায় যারা চাঁদাবাজি করছে এবং গ্যাং কালচার তৈরি করছে, তাদের শেকড় উপড়ে ফেলা হবে। এদের পেছনে যত বড় প্রভাবশালী নেতাই থাকুক না কেন, পুলিশ কাউকে পরোয়া করবে না। আজ থেকেই আমাদের জিরো টলারেন্স নীতি কার্যকর হচ্ছে।"
            3 -> "আগামী ডিসেম্বরের মধ্যেই আমাদের নতুন মেগাপ্রকল্পের কাজ শেষ হবে এবং জানুয়ারির ১ তারিখ থেকে সাধারণ মানুষ এই সেতু দিয়ে যাতায়াত করতে পারবে। এতে করে দক্ষিণাঞ্চলের সাথে যাতায়াতের সময় ৩ ঘণ্টা কমে যাবে। কিছু মানুষ এই প্রকল্প নিয়ে মিথ্যা গুজব ছড়িয়েছিল, আজ তাদের মুখে চুনকালি পড়েছে। দেশের উন্নয়ন তাদের সহ্য হয় না।"
            4 -> "আগামী বছর থেকে আমরা পরীক্ষাপদ্ধতিতে বড় ধরনের পরিবর্তন আনতে যাচ্ছি। শিক্ষার্থীদের মুখস্থবিদ্যার ওপর নির্ভরশীলতা কমানো হবে। প্র্যাকটিক্যাল বা হাতে-কলমে শিক্ষার ওপর জোর দেওয়া হবে বেশি। প্রশ্নফাঁসের কোনো সুযোগ এবার থাকবে না, যারা এই চক্রের সাথে জড়িত তাদের যাবজ্জীবন কারাদণ্ড দেওয়া হবে।"
            else -> ""
        }
        _inputText.value = sample
    }

    fun generateHeadlines() {
        val text = _inputText.value.trim()
        val hasFile = selectedFileBytes != null && !_selectedFileMime.value.isNullOrEmpty()

        if (text.isEmpty() && !hasFile) {
            _uiState.value = GeneratorUiState.Error("দয়া করে বক্তব্যের ট্রান্সক্রিপ্ট যুক্ত করুন অথবা কোনো অডিও/ভিডিও ফাইল আপলোড করুন!")
            return
        }

        _uiState.value = GeneratorUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val editorResponse = GeminiClient.analyzeSpeech(
                    transcriptText = if (text.isNotEmpty()) text else null,
                    mediaBytes = selectedFileBytes,
                    mimeType = _selectedFileMime.value
                )
                if (editorResponse != null) {
                    val savedTranscript = if (text.isNotEmpty()) {
                        text
                    } else {
                        "আপলোড করা ফাইল: ${_selectedFileName.value ?: "অডিও/ভিডিও"}"
                    }

                    val analysis = NewsAnalysis(
                        transcript = savedTranscript,
                        hardNews1 = editorResponse.hardNews.getOrNull(0) ?: "সংবাদ শিরোনাম পাওয়া যায়নি",
                        hardNews2 = editorResponse.hardNews.getOrNull(1) ?: "সংবাদ শিরোনাম পাওয়া যায়নি",
                        directQuote1 = editorResponse.directQuote.getOrNull(0) ?: "উদ্ধৃতিমূলক শিরোনাম পাওয়া যায়নি",
                        directQuote2 = editorResponse.directQuote.getOrNull(1) ?: "উদ্ধৃতিমূলক শিরোনাম পাওয়া যায়নি",
                        warningAction1 = editorResponse.warningAction.getOrNull(0) ?: "হুঁশিয়ারিমূলক শিরোনাম পাওয়া যায়নি",
                        warningAction2 = editorResponse.warningAction.getOrNull(1) ?: "হুঁশিয়ারিমূলক শিরোনাম পাওয়া যায়নি",
                        politicalConflict1 = editorResponse.politicalConflict.getOrNull(0) ?: "রাজনৈতিক শিরোনাম পাওয়া যায়নি",
                        politicalConflict2 = editorResponse.politicalConflict.getOrNull(1) ?: "রাজনৈতিক শিরোনাম পাওয়া যায়নি",
                        curiosityQuestion1 = editorResponse.curiosityQuestion.getOrNull(0) ?: "কৌতুহলোদ্দীপক শিরোনাম পাওয়া যায়নি",
                        curiosityQuestion2 = editorResponse.curiosityQuestion.getOrNull(1) ?: "কৌতুহলোদ্দীপক শিরোনাম পাওয়া যায়নি",
                        editorAgenda = editorResponse.editorsReview.agenda,
                        editorPublicImpact = editorResponse.editorsReview.publicImpact,
                        editorPowerWordsJoined = editorResponse.editorsReview.powerWords.joinToString(", ")
                    )
                    
                    // Save to history list
                    repository.insert(analysis)
                    
                    _uiState.value = GeneratorUiState.Success(analysis)
                } else {
                    _uiState.value = GeneratorUiState.Error("দুঃখিত, কোনো ফলাফল পাওয়া যায়নি। আবার চেষ্টা করুন।")
                }
            } catch (e: Exception) {
                _uiState.value = GeneratorUiState.Error(e.localizedMessage ?: "একটি ত্রুটি ঘটেছে। অনুগ্রহ করে আবার চেষ্টা করুন!")
            }
        }
    }

    fun loadFromHistory(analysis: NewsAnalysis) {
        _inputText.value = analysis.transcript
        _uiState.value = GeneratorUiState.Success(analysis)
    }

    fun deleteFromHistory(analysis: NewsAnalysis) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(analysis)
            // If deleting the active item, return state to idle
            val current = _uiState.value
            if (current is GeneratorUiState.Success && current.analysis.id == analysis.id) {
                _uiState.value = GeneratorUiState.Idle
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            _uiState.value = GeneratorUiState.Idle
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "শিরোনামটি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    fun shareHeadline(context: Context, headline: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "আকর্ষণীয় শিরোনাম: \"$headline\"\n\n- সংগৃহীত 'নিউজ এডিটর' অ্যাপ থেকে।")
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(shareIntent, "শিরোনামটি শেয়ার করুন").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
