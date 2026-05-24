package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String,
    val properties: Map<String, ResponseSchemaProperty>? = null,
    val required: List<String>? = null,
    val items: ResponseSchema? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchemaProperty(
    val type: String,
    val description: String? = null,
    val items: ResponseSchema? = null,
    val properties: Map<String, ResponseSchemaProperty>? = null,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// Inner structured classes returned by Gemini text
@JsonClass(generateAdapter = true)
data class GeminiEditorResponse(
    val hardNews: List<String>,
    val directQuote: List<String>,
    val warningAction: List<String>,
    val politicalConflict: List<String>,
    val curiosityQuestion: List<String>,
    val editorsReview: GeminiEditorsReview
)

@JsonClass(generateAdapter = true)
data class GeminiEditorsReview(
    val agenda: String,
    val publicImpact: String,
    val powerWords: List<String>
)
