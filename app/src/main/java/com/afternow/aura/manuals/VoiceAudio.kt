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
class VoiceAudio(private val context: Context, private val onChunk: (String)->Unit, private val onError:(String)->Unit, private val onPlayed:(String,Int)->Unit) {
    private val recorderThread=Executors.newSingleThreadExecutor()
    private val playbackThread=Executors.newSingleThreadExecutor()
    private val epoch=AtomicInteger(0)
    private val recordEpoch=AtomicInteger(0)
    @Volatile private var recording=false
    private var recorder:AudioRecord?=null
    private var echo:AcousticEchoCanceler?=null
    private val playbackLock=Any()
    private var writtenFrames=0L
    private data class Segment(val item:String,val start:Long,var end:Long)
    private val segments=mutableListOf<Segment>()
    private val receivedFrames=mutableMapOf<String,Long>()
    private val track=AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setBufferSizeInBytes(maxOf(24000,AudioTrack.getMinBufferSize(24000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)))
        .setTransferMode(AudioTrack.MODE_STREAM).build()
    fun start():Boolean {
        if(recording)return true
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){onError("Microphone permission is required");return false}
        return try {
            val size=maxOf(4800,AudioRecord.getMinBufferSize(24000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT))
            val input=AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,24000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size*2)
            if(input.state!=AudioRecord.STATE_INITIALIZED){input.release();onError("Microphone could not initialize");return false}
            recorder=input
            if(AcousticEchoCanceler.isAvailable())echo=AcousticEchoCanceler.create(input.audioSessionId)?.apply{enabled=true}
            input.startRecording();recording=true
            val recordGeneration=recordEpoch.incrementAndGet()
            recorderThread.execute {
                val buffer=ByteArray(2400)
                try{while(recording&&recordEpoch.get()==recordGeneration){val n=input.read(buffer,0,buffer.size);if(n>0&&recording&&recordEpoch.get()==recordGeneration)onChunk(Base64.encodeToString(buffer,0,n,Base64.NO_WRAP))}}
                catch(e:Exception){if(recording)onError("Microphone error: ${e.message}")}
            }
            true
        }catch(e:Exception){onError("Microphone unavailable: ${e.message}");false}
    }
    fun stop(after:()->Unit={}) {
        recording=false;recordEpoch.incrementAndGet()
        val oldRecorder=recorder;val oldEcho=echo;recorder=null;echo=null
        try{oldRecorder?.stop()}catch(_:Exception){}
        recorderThread.execute{oldEcho?.release();oldRecorder?.release();after()}
    }
    fun play(base64:String,itemId:String) {
        val bytes=Base64.decode(base64,Base64.DEFAULT)
        val generation=synchronized(playbackLock){receivedFrames[itemId]=(receivedFrames[itemId]?:0L)+bytes.size/2;epoch.get()}
        playbackThread.execute {
            var offset=0
            try {
                while(offset<bytes.size&&epoch.get()==generation){
                    val n=synchronized(playbackLock){
                        if(epoch.get()!=generation)return@execute
                        if(track.playState!=AudioTrack.PLAYSTATE_PLAYING)track.play()
                        val count=track.write(bytes,offset,minOf(2400,bytes.size-offset),AudioTrack.WRITE_NON_BLOCKING)
                        if(count>0){
                            if(segments.lastOrNull()?.item!=itemId)segments.add(Segment(itemId,writtenFrames,writtenFrames))
                            writtenFrames+=count/2;segments.last().end=writtenFrames
                        }
                        count
                    }
                    if(n<0)break
                    if(n==0)Thread.sleep(5) else offset+=n
                }
            }catch(_:IllegalStateException){}
        }
    }
    fun clear(){
        val cut=synchronized(playbackLock){
            epoch.incrementAndGet()
            try{
                track.pause()
                val played=track.playbackHeadPosition.toLong() and 0xffffffffL
                val result=receivedFrames.mapNotNull{(item,total)->
                    val segment=segments.find{it.item==item}
                    val heard=if(segment==null)0L else (played-segment.start).coerceIn(0,total)
                    if(heard<total)item to (heard/24).toInt() else null
                }
                track.flush();writtenFrames=0;segments.clear();receivedFrames.clear();result
            }catch(_:IllegalStateException){emptyList()}
        }
        cut.forEach{(item,ms)->if(item.isNotEmpty())onPlayed(item,ms)}
    }
    fun release(){stop();clear();recorderThread.shutdown();playbackThread.execute{track.release()};playbackThread.shutdown()}
}
