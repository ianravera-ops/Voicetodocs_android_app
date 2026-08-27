package com.voicetodocs.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voicetodocs.app.databinding.ActivityMainBinding
import com.voicetodocs.app.summary.TranscriptSummarizer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val summarizer = TranscriptSummarizer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.summarizeButton.setOnClickListener {
            val transcript = binding.transcriptInput.text?.toString().orEmpty()
            if (transcript.isBlank()) {
                binding.resultView.text = getString(R.string.empty_transcript_hint)
                return@setOnClickListener
            }
            val digest = summarizer.digest(transcript)
            binding.resultView.text = getString(
                R.string.digest_format,
                digest.summary,
                digest.actionItems.joinToString("\n") { "• $it" }
                    .ifEmpty { getString(R.string.no_action_items) },
                digest.draftReply,
            )
        }
    }
}
