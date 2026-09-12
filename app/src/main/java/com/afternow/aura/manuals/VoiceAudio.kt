package com.afternow.aura.manuals

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Raw mono PCM16 at 24 kHz. Microphone is active only after the user taps Talk. */
class VoiceAudio(private val context: Context, private val onChunk: (String)->Unit, private val onError:(String)->Unit) {
    private val recorderThread=Executors.newSingleThreadExecutor()
    private val playbackThread=Executors.newSingleThreadExecutor()
    private val epoch=AtomicInteger(0)
    @Volatile private var recording=false
    private var recorder:AudioRecord?=null
    private var echo:AcousticEchoCanceler?=null
    private val track=AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setBufferSizeInBytes(maxOf(24000,AudioTrack.getMinBufferSize(24000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)))
        .setTransferMode(AudioTrack.MODE_STREAM).build()
    fun start():Boolean {
        if(recording)return true
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){onError("Microphone permission is required");return false}
        return try {
            val size=maxOf(4800,AudioRecord.getMinBufferSize(24000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT))
            val input=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,24000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size*2)
            if(input.state!=AudioRecord.STATE_INITIALIZED){input.release();onError("Microphone could not initialize");return false}
            recorder=input
            if(AcousticEchoCanceler.isAvailable())echo=AcousticEchoCanceler.create(input.audioSessionId)?.apply{enabled=true}
            input.startRecording();recording=true
            recorderThread.execute {
                val buffer=ByteArray(2400)
                try{while(recording){val n=input.read(buffer,0,buffer.size);if(n>0&&recording)onChunk(Base64.encodeToString(buffer,0,n,Base64.NO_WRAP))}}
                catch(e:Exception){if(recording)onError("Microphone error: ${e.message}")}
            }
            true
        }catch(e:Exception){onError("Microphone unavailable: ${e.message}");false}
    }
    fun stop(after:()->Unit={}) {
        recording=false
        try{recorder?.stop()}catch(_:Exception){}
        recorderThread.execute{echo?.release();echo=null;recorder?.release();recorder=null;after()}
    }
    fun play(base64:String) {
        val generation=epoch.get()
        val bytes=Base64.decode(base64,Base64.DEFAULT)
        playbackThread.execute {
            if(epoch.get()!=generation)return@execute
            try{if(track.playState!=AudioTrack.PLAYSTATE_PLAYING)track.play();var offset=0
                while(offset<bytes.size&&epoch.get()==generation){val n=track.write(bytes,offset,minOf(2400,bytes.size-offset));if(n<=0)break;offset+=n}
            }catch(_:IllegalStateException){}
        }
    }
    fun clear(){epoch.incrementAndGet();try{track.pause();track.flush()}catch(_:IllegalStateException){}}
    fun release(){stop();clear();recorderThread.shutdown();playbackThread.execute{track.release()};playbackThread.shutdown()}
}
