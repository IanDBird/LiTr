package com.linkedin.android.litr.demo

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TransformationPresenter
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.databinding.FragmentAudioRecordBinding
import com.linkedin.android.litr.io.AudioRecordMediaSource
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File

class RecordAudioFragment : BaseTransformationFragment() {
    private lateinit var binding: FragmentAudioRecordBinding

    private lateinit var mediaTransformer: MediaTransformer
    private var targetMedia: TargetMedia = TargetMedia()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaTransformer = MediaTransformer(context!!.applicationContext)

        // Check to make sure the user has granted permission to record audio.
        if (!hasAudioRecordPermission()) {
            ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_AUDIO_RECORD_PERMISSION
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaTransformer.release()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioRecordBinding.inflate(layoutInflater, container, false)

        binding.transformationState = TransformationState()
        binding.transformationPresenter = TransformationPresenter(context!!, mediaTransformer)
        binding.mediaSource = AudioRecordMediaSource()

        val targetFile = File(
                TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
                "transcoded_audio_recorder.mp4"
        )
        targetMedia.setTargetFile(targetFile)
        binding.targetMedia = targetMedia

        return binding.root
    }

    private fun hasAudioRecordPermission(): Boolean {
        val validContext = context ?: return false
        return ContextCompat.checkSelfPermission(
                validContext,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_AUDIO_RECORD_PERMISSION = 14
    }
}