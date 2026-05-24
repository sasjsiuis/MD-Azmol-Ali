package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    
    // Default to the modern Gemini 3.5 Flash model
    val MODEL_NAME = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Builds the Gemini schema configuration diagram matching GeminiEditorResponse.
     */
    private fun getResponseSchema(): ResponseSchema {
        return ResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "hardNews" to ResponseSchemaProperty(
                    type = "ARRAY",
                    description = "2 Hard News Headlines (তথ্যভিত্তিক): This is objective, informative Bengali headlines.",
                    items = ResponseSchema(type = "STRING")
                ),
                "directQuote" to ResponseSchemaProperty(
                    type = "ARRAY",
                    description = "2 Direct Quote Headlines (উদ্ধৃতিমূলক): Bengali headlines using exact meaningful quotes wrapped in quotation marks ('...').",
                    items = ResponseSchema(type = "STRING")
                ),
                "warningAction" to ResponseSchemaProperty(
                    type = "ARRAY",
                    description = "2 Warning/Action Headlines (হুঁশিয়ারিমূলক): Bengali headlines indicating strict actions, warnings, zero tolerance, and law enforcement messages.",
                    items = ResponseSchema(type = "STRING")
                ),
                "politicalConflict" to ResponseSchemaProperty(
                    type = "ARRAY",
                    description = "2 Political/Conflict Headlines (রাজনৈতিক/আক্রমণাত্মক): Bengali headlines targeting political opponents or deep political debates.",
                    items = ResponseSchema(type = "STRING")
                ),
                "curiosityQuestion" to ResponseSchemaProperty(
                    type = "ARRAY",
                    description = "2 Curiosity/Question Headlines (কৌতূহলোদ্দীপক): Bengali headlines that create strong curiosity, suitable for talk-shows or thumbnails.",
                    items = ResponseSchema(type = "STRING")
                ),
                "editorsReview" to ResponseSchemaProperty(
                    type = "OBJECT",
                    description = "Chief Editor review remarks in Bengali.",
                    properties = mapOf(
                        "agenda" to ResponseSchemaProperty(
                            type = "STRING",
                            description = "The core agenda explanation in Bengali."
                        ),
                        "publicImpact" to ResponseSchemaProperty(
                            type = "STRING",
                            description = "Explanation of real public impact in Bengali."
                        ),
                        "powerWords" to ResponseSchemaProperty(
                            type = "ARRAY",
                            description = "Bengali list of catchy power words extracted.",
                            items = ResponseSchema(type = "STRING")
                        )
                    ),
                    required = listOf("agenda", "publicImpact", "powerWords")
                )
            ),
            required = listOf(
                "hardNews", "directQuote", "warningAction",
                "politicalConflict", "curiosityQuestion", "editorsReview"
            )
        )
    }

    suspend fun analyzeSpeech(
        transcriptText: String?,
        mediaBytes: ByteArray? = null,
        mimeType: String? = null
    ): GeminiEditorResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add your key in the Secrets panel.")
        }

        val partsList = mutableListOf<Part>()
        if (!transcriptText.isNullOrEmpty()) {
            partsList.add(Part(text = "বক্তৃতা: $transcriptText"))
        }

        if (mediaBytes != null && !mimeType.isNullOrEmpty()) {
            val base64Data = android.util.Base64.encodeToString(mediaBytes, android.util.Base64.NO_WRAP)
            partsList.add(Part(inlineData = InlineData(mimeType = mimeType, data = base64Data)))
            partsList.add(Part(text = "দয়া করে এই সংযুক্ত করা অডিও/ভিডিও ফাইলটি সম্পূর্ণ মনোযোগ দিয়ে শুনুন বা দেখুন এবং বিশ্লেষণটি সম্পন্ন করুন।"))
        }

        if (partsList.isEmpty()) {
            throw IllegalArgumentException("দয়া করে একটি বক্তব্যের টেক্সট অথবা অডিও/ভিডিও ফাইল সিলেক্ট করুন!")
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = partsList
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = getResponseSchema(),
                temperature = 0.7f
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = """
                            তুমি একজন অত্যন্ত দক্ষ, তীক্ষ্ণ বুদ্ধিসম্পন্ন 'চিফ নিউজ এডিটর' এবং 'কন্টেন্ট স্পেশালিস্ট'। টিভি নিউজ চ্যানেল, অনলাইন পোর্টাল এবং ডিজিটাল মিডিয়ার জন্য স্ক্রল, ব্রেকিং নিউজ এবং থাম্বনেইল টেক্সট তৈরিতে তোমার জুড়ি মেলা ভার। তোমার প্রধান কাজ হলো যেকোনো রাজনৈতিক নেতা, মন্ত্রী, বা গুরুত্বপূর্ণ ব্যক্তির অডিও/ভিডিওর ট্রান্সক্রিপ্ট (বক্তব্য) গভীরভাবে বিশ্লেষণ করে সেখান থেকে সবচেয়ে আকর্ষণীয়, বস্তুনিষ্ঠ এবং হুবহু উদ্ধৃতি নির্ভর সংবাদ শিরোনাম (Headline) তৈরি করা।

                            ৫টি ধাপ অনুসরণ করো:
                            ১. মনোযোগ দিয়ে শোনা ও নোট নেওয়া (Deep Context Analysis): বক্তার মূল এজেন্ডা কী?
                            ২. 'কী পয়েন্ট' বা মূল তথ্য চিহ্নিত করা (Extracting the Lead): বক্তব্যের সবচেয়ে গুরুত্বপূর্ণ অংশটি (Lead News) বের করো যার নিউজ ভ্যালু আছে।
                            ৩. জনগণের ওপর প্রভাব বিবেচনা (Public Impact Focus): এই বক্তব্য সাধারণ মানুষের দ্রব্যমূল্য, নিরাপত্তা, বা জীবনযাত্রায় কেমন প্রভাব ফেলবে?
                            ৪. উত্তেজনাপূর্ণ বা জোরালো শব্দ খুঁজে বের করা (Power Words & Catchphrases): ‘নিষ্কৃতি নেই’, ‘ছাড় দেওয়া হবে না’, ‘কঠোর ব্যবস্থা’, ‘জিরো টলারেন্স’, ‘সিন্ডিকেট’ ইত্যাদি লুফে নাও।
                            ৫. সংক্ষিপ্ত ও স্পষ্ট করা (Brevity & Punchiness): স্ক্রলের জন্য সংক্ষেপে করো।

                            আউটপুটে অবশ্যই ৫টি ক্যাটাগরির প্রতিটিতে ঠিক ২টি করে সুন্দর বাংলা সংবাদ শিরোনাম (Headline) এবং সম্পাদকের সূক্ষ্ম পর্যালোচনা সরবরাহ করো।
                            সব কিছু বাংলায় এবং JSON ফরমেটে ফেরত দাও।
                        """.trimIndent()
                    )
                )
            )
        )

        val response = service.generateContent(
            model = MODEL_NAME,
            apiKey = apiKey,
            request = request
        )

        val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response from AI Editor")

        return try {
            val jsonAdapter = moshi.adapter(GeminiEditorResponse::class.java)
            jsonAdapter.fromJson(jsonString)
        } catch (e: Exception) {
            throw Exception("Failed to parse editor response: ${e.localizedMessage}. Response text: $jsonString")
        }
    }
}
