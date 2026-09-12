package com.afternow.aura.manuals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.*
import android.os.Handler
import android.os.Looper
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Topic(val label:String,val page:Int)
data class Product(val id:String,val name:String,val kind:String,val maker:String,val file:String,val pageCount:Int,val offset:Int,val topics:List<Topic>)
data class Message(val who:String,val text:String,val id:String="")
class ManualViewModel(app:Application):AndroidViewModel(app) {
    var products by mutableStateOf<List<Product>>(emptyList());private set
    var selected by mutableStateOf<Product?>(null);private set
    var page by mutableIntStateOf(1);private set
    var zoom by mutableFloatStateOf(1f);private set
    var status by mutableStateOf("Connecting…");private set
    var connected by mutableStateOf(false);private set
    var voiceReady by mutableStateOf(false);private set
    var recording by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    var review by mutableStateOf<String?>(null);private set
    var renderRequest by mutableStateOf<String?>(null);private set
    val messages=mutableStateListOf<Message>()
    private val main=Handler(Looper.getMainLooper())
    private val http=OkHttpClient.Builder().readTimeout(0,TimeUnit.MILLISECONDS).pingInterval(20,TimeUnit.SECONDS).build()
    private var socket:WebSocket?=null
    private var connectionEpoch=0
    val audio=VoiceAudio(app,{send("audio.append", "audio" to it)},{message->main.post{error=message}})
    init {
        try{products=parseProducts(JSONArray(app.assets.open("manuals/catalog.json").bufferedReader().use{it.readText()}));selected=products.find{it.id=="db200h"}?:products.firstOrNull()}
        catch(_:Exception){error="Manual assets are missing. Run the preparation script."}
        connect()
    }
    private fun parseProducts(a:JSONArray)=List(a.length()){i->val p=a.getJSONObject(i);val t=p.getJSONArray("topics");Product(p.getString("id"),p.getString("name"),p.getString("kind"),p.getString("manufacturer"),p.getString("file"),p.getInt("pageCount"),p.getInt("offset"),List(t.length()){j->Topic(t.getJSONObject(j).getString("label"),t.getJSONObject(j).getInt("page"))})}
    fun connect(){
        stopRecording(false);audio.clear();val epoch=++connectionEpoch;socket?.close(1000,"Reconnect");connected=false;voiceReady=false;status="Connecting…";error=null
        socket=http.newWebSocket(Request.Builder().url("ws://127.0.0.1:8787").build(),object:WebSocketListener(){
            override fun onOpen(ws:WebSocket,response:Response){main.post{if(epoch==connectionEpoch){connected=true;status="Connected"}}}
            override fun onMessage(ws:WebSocket,text:String){main.post{if(epoch==connectionEpoch)try{handle(JSONObject(text))}catch(e:Exception){error="Unexpected server message"}}}
            override fun onFailure(ws:WebSocket,t:Throwable,response:Response?){main.post{if(epoch==connectionEpoch){connected=false;voiceReady=false;status="Offline · manuals available";error="Backend unavailable. Start the server and ADB USB connection.";stopRecording(false)}}}
            override fun onClosed(ws:WebSocket,code:Int,reason:String){main.post{if(epoch==connectionEpoch){connected=false;voiceReady=false;status="Disconnected";stopRecording(false)}}}
        })
    }
    private fun handle(e:JSONObject){when(e.getString("type")){
        "catalog"->{/* Bundled assets determine which documents can be rendered. */}
        "product.selected"->{selected=products.find{it.id==e.getString("productId")};page=1;zoom=1f;messages.clear();review=null;renderRequest=null;audio.clear();recording=false;audio.stop()}
        "status"->{status=e.getString("status");if(e.has("voiceReady"))voiceReady=e.getBoolean("voiceReady")}
        "error"->{error=e.getString("message");if(status.startsWith("Connecting"))voiceReady=false}
        "document.show"->{if(e.getString("productId")==selected?.id){page=e.getInt("pdfPage");zoom=1f;renderRequest=e.getString("requestId")}else send("document.ack","requestId" to e.getString("requestId"),"ok" to false)}
        "document.zoom"->{zoom=when(e.getString("action")){"zoom_in"->(zoom+0.5f).coerceAtMost(3f);"zoom_out"->(zoom-0.5f).coerceAtLeast(1f);else->1f}}
        "audio.delta"->audio.play(e.getString("audio"))
        "audio.clear"->audio.clear()
        "transcript.user"->messages.add(Message("You",e.getString("text")))
        "transcript.delta"->{val id=e.optString("itemId");val index=messages.indexOfLast{it.id==id&&it.who=="Agent"};if(index>=0)messages[index]=messages[index].copy(text=messages[index].text+e.getString("delta"))else messages.add(Message("Agent",e.getString("delta"),id))}
        "transcript.done"->{val id=e.optString("itemId");val index=messages.indexOfLast{it.id==id&&it.who=="Agent"};if(index>=0)messages[index]=messages[index].copy(text=e.getString("text"))}
        "tool"->{status=when(e.getString("name")){"search_manual"->"Searching the manual…";"show_page"->"Opening the source…";"get_page"->"Reading the page…";else->"Working…"}}
        "review"->{val step=e.optJSONObject("step");review=step?.let{"${it.getInt("index")}/${it.getInt("total")} · ${it.getString("title")}"}}
    }}
    fun select(p:Product){stopRecording(false);audio.clear();selected=p;page=1;zoom=1f;messages.clear();review=null;voiceReady=false;send("product.select","productId" to p.id)}
    fun show(n:Int){val p=selected?:return;page=n.coerceIn(1,p.pageCount);zoom=1f;renderRequest=null;send("document.visible","pdfPage" to page)}
    fun zoomBy(delta:Float){zoom=(zoom+delta).coerceIn(1f,3f)}
    fun rendered(request:String?,ok:Boolean){if(request!=null){send("document.ack","requestId" to request,"ok" to ok);if(renderRequest==request)renderRequest=null}}
    fun ask(text:String){if(text.isBlank())return;stopRecording(false);audio.clear();error=null;send("text.ask","text" to text)}
    fun toggleRecording(){if(recording)stopRecording(true)else if(voiceReady){error=null;audio.clear();send("audio.start");recording=audio.start()}}
    private fun stopRecording(commit:Boolean){if(recording){recording=false;audio.stop{if(commit)send("audio.commit")}}}
    fun interrupt(){stopRecording(false);audio.clear();send("interrupt")}
    fun clearError(){error=null}
    private fun send(type:String,vararg fields:Pair<String,Any>){val j=JSONObject().put("type",type);fields.forEach{j.put(it.first,it.second)};socket?.send(j.toString())}
    override fun onCleared(){connectionEpoch++;socket?.close(1000,"App closed");audio.release();http.dispatcher.executorService.shutdown()}
}
