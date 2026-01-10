package com.virtualclone.app.ml.onnx.tokenizer

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

private val TAG = Gpt2Tokenizer::class.qualifiedName

class Gpt2Tokenizer(
    private val vocab: Map<String, Int>,
    private val merges: Map<Pair<String, String>, Int>
) {
    companion object {
        fun fromAssets(context: Context): Gpt2Tokenizer {
            try {
                // Load vocabulary
                val vocabJson = loadAsset(context, "tokenizer/vocab.json")
                val vocabObject = Json.parseToJsonElement(vocabJson) as JsonObject
                val vocab = vocabObject.mapValues { it.value.jsonPrimitive.content.toInt() }

                // Load merges
                val mergesText = loadAsset(context, "tokenizer/merges.txt")
                val merges = mergesText.lines()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .mapIndexed { index, line ->
                        val parts = line.split(" ")
                        if (parts.size >= 2) {
                            parts[0] to parts[1] to index
                        } else {
                            "" to "" to index
                        }
                    }
                    .filter { it.first.first.isNotEmpty() && it.first.second.isNotEmpty() }
                    .associate { it.first to it.second }

                Log.d(TAG, "Tokenizer loaded with ${vocab.size} tokens and ${merges.size} merges")
                return Gpt2Tokenizer(vocab, merges)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tokenizer", e)
                throw e
            }
        }

        private fun loadAsset(context: Context, path: String): String {
            return context.assets.open(path).use { inputStream ->
                BufferedReader(
                    InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                    )
                ).use { reader ->
                    reader.readText()
                }
            }
        }
    }

    fun encode(text: String): IntArray {
        // Convert to bytes first (GPT-2 uses byte-level BPE)
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val byteStrings = bytes.map { byteToString(it) }

        // Apply BPE merges
        var tokens = byteStrings.toMutableList()
        var changed = true

        while (changed) {
            changed = false
            var bestPair: Pair<String, String>? = null
            var bestRank = Int.MAX_VALUE

            // Find the best pair to merge
            for (i in 0 until tokens.size - 1) {
                val pair = tokens[i] to tokens[i + 1]
                val rank = merges[pair] ?: continue
                if (rank < bestRank) {
                    bestRank = rank
                    bestPair = pair
                }
            }

            // Merge the best pair
            if (bestPair != null) {
                val newTokens = mutableListOf<String>()
                var i = 0
                while (i < tokens.size) {
                    if (i < tokens.size - 1 && tokens[i] == bestPair.first && tokens[i + 1] == bestPair.second) {
                        newTokens.add(bestPair.first + bestPair.second)
                        i += 2
                        changed = true
                    } else {
                        newTokens.add(tokens[i])
                        i++
                    }
                }
                tokens = newTokens
            }
        }

        // Convert to token IDs
        return tokens.mapNotNull { token ->
            vocab[token] ?: vocab["<unk>"]
        }.toIntArray()
    }

    fun decode(ids: IntArray): String {
        val tokenToWord = vocab.entries.associate { it.value to it.key }
        val tokens = ids.map { id -> tokenToWord[id] ?: "<unk>" }

        // Convert byte strings back to actual bytes
        val bytes = tokens.flatMap { token ->
            if (token.startsWith("</w>")) {
                // End of word marker
                emptyList()
            } else {
                stringToBytes(token)
            }
        }

        return String(bytes.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun byteToString(byte: Byte): String {
        return when {
            byte.toInt() in 33..126 -> byte.toInt().toChar().toString()
            byte.toInt() == 32 -> "</w>" // Space becomes end of word
            else -> "<0x${byte.toInt().and(0xFF).toString(16).padStart(2, '0')}>"
        }
    }

    private fun stringToBytes(token: String): List<Byte> {
        return when {
            token == "</w>" -> listOf(32) // End of word becomes space
            token.startsWith("<0x") && token.endsWith(">") -> {
                val hex = token.substring(3, token.length - 1)
                listOf(hex.toInt(16).toByte())
            }

            token.length == 1 -> listOf(token[0].code.toByte())
            else -> emptyList()
        }
    }
}