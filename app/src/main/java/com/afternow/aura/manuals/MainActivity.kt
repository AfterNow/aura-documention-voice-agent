package com.afternow.aura.manuals

import android.Manifest
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.unit.DpVolumeSize

private val Ink=Color(0xFF0C1117)
private val Panel=Color(0xFF131C25)
private val Raised=Color(0xFF1D2A36)
private val Mint=Color(0xFF8AE5C0)
private val Muted=Color(0xFF9FB0BF)
private val Line=Color(0xFF2A3946)
private val Warm=Color(0xFFFFC68A)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{
  MaterialTheme(colorScheme=darkColorScheme(primary=Mint,background=Ink,surface=Panel,onPrimary=Ink,onSurface=Color(0xFFE7EFF5))){
   val vm:ManualViewModel=viewModel()
   DisposableEffect(vm){
    val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP)vm.interrupt()}
    lifecycle.addObserver(observer)
    onDispose{lifecycle.removeObserver(observer)}
   }
   if(LocalSpatialCapabilities.current.isSpatialUiEnabled){
    Subspace {
     SpatialRow {
      SpatialPanel(SubspaceModifier.width(480.dp).height(820.dp).movable(stickyPose=true).resizable(minimumSize=DpVolumeSize(420.dp,640.dp,0.dp),maximumSize=DpVolumeSize(900.dp,1200.dp,0.dp))) {
       Surface(color=Ink,contentColor=Color(0xFFE7EFF5)){
        Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
         Row(verticalAlignment=Alignment.CenterVertically){Text("Aura Voice Document",fontSize=24.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));TextButton(onClick={vm.connect()}){Text("Reconnect")}}
         Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){vm.products.forEach{p->
          FilterChip(selected=vm.selected?.id==p.id,onClick={vm.select(p)},label={Text(if(p.id=="vsx")"VSX" else p.name,fontSize=11.sp)})
         }}
         ConversationPanel(vm,Modifier.fillMaxWidth().weight(1f))
        }
       }
      }
      SpatialPanel(SubspaceModifier.padding(start=24.dp).width(760.dp).height(820.dp).movable(stickyPose=true).resizable(minimumSize=DpVolumeSize(480.dp,500.dp,0.dp),maximumSize=DpVolumeSize(1400.dp,1400.dp,0.dp))) {
       Surface(color=Ink,contentColor=Color(0xFFE7EFF5)){DocumentPanel(vm,Modifier.fillMaxSize())}
      }
     }
    }
   }else Surface(color=Ink,contentColor=Color(0xFFE7EFF5)){ManualsApp(vm)}
  }
 }}
}
@Composable fun ManualsApp(vm:ManualViewModel){
 Column(Modifier.fillMaxSize().background(Ink).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Mint),contentAlignment=Alignment.Center){Text("A",color=Ink,fontWeight=FontWeight.Black,fontSize=24.sp)}
   Spacer(Modifier.width(12.dp))
   Column{Text("Aura Voice Document",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("TECHNICAL COMPANION",fontSize=10.sp,color=Muted,letterSpacing=2.sp)}
   Spacer(Modifier.weight(1f))
   Box(Modifier.size(7.dp).background(if(vm.connected)Mint else Warm,RoundedCornerShape(7.dp)))
   Spacer(Modifier.width(8.dp));Text(if(vm.connected)"CONNECTED VIA USB" else "DOCUMENTS OFFLINE",fontSize=11.sp,color=Muted,letterSpacing=1.sp)
   Spacer(Modifier.width(14.dp));TextButton(onClick={vm.connect()}){Text("Reconnect",color=Mint)}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   vm.products.forEach { p->
    val active=vm.selected?.id==p.id
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if(active)Raised else Panel).border(1.dp,if(active)Mint else Line,RoundedCornerShape(16.dp)).clickable{vm.select(p)}.padding(16.dp)){
     Row(verticalAlignment=Alignment.CenterVertically){Text(p.kind.uppercase(),fontSize=10.sp,color=if(active)Mint else Muted,letterSpacing=1.sp);Spacer(Modifier.weight(1f));if(active)Text("●",color=Mint,fontSize=10.sp)}
     Spacer(Modifier.height(6.dp));Text(p.name,fontWeight=FontWeight.SemiBold,fontSize=20.sp)
     Spacer(Modifier.height(3.dp));Text(p.maker,fontSize=11.sp,color=Muted)
    }
   }
  }
  Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp)){
   ConversationPanel(vm,Modifier.weight(0.39f).fillMaxHeight())
   DocumentPanel(vm,Modifier.weight(0.61f).fillMaxHeight())
  }
 }
}
@Composable fun ConversationPanel(vm:ManualViewModel,modifier:Modifier=Modifier){
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)vm.toggleRecording()}
 val context=LocalContext.current
 var input by remember{mutableStateOf("")}
 Column(modifier.clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp,Line,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Text("Ask the manual",fontSize=20.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("VOICE",fontSize=10.sp,color=Mint,letterSpacing=2.sp)}
  Text(vm.status,fontSize=12.sp,color=if(vm.recording)Warm else Mint)
  vm.review?.let{Text(it,color=Warm,fontSize=12.sp)}
  if(vm.messages.isEmpty()){
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Spacer(Modifier.height(8.dp));Text("Expert answers.\nThe source, in view.",fontSize=27.sp,lineHeight=34.sp,fontWeight=FontWeight.Medium)
    Text("Ask a technical question or open a drawing. Every answer stays with your selected product.",fontSize=13.sp,lineHeight=20.sp,color=Muted)
    Spacer(Modifier.height(4.dp))
    vm.selected?.topics?.take(3)?.forEach{topic->
     Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Raised).clickable{if(vm.voiceReady)vm.ask("Show me and explain the ${topic.label}")else vm.show(topic.page)}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text(topic.label,Modifier.weight(1f),fontSize=12.sp);Text("↗",color=Mint,fontSize=18.sp)}
    }
   }
  }else{
   val list=androidx.compose.foundation.lazy.rememberLazyListState()
   LaunchedEffect(vm.messages.size,vm.messages.lastOrNull()?.text){if(vm.messages.isNotEmpty())list.animateScrollToItem(vm.messages.lastIndex)}
   LazyColumn(Modifier.weight(1f),state=list,verticalArrangement=Arrangement.spacedBy(15.dp)){
    items(vm.messages){m->Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(m.who=="You")Raised else Color.Transparent).padding(12.dp)){
     Text(m.who.uppercase(),fontSize=10.sp,color=if(m.who=="You")Muted else Mint,letterSpacing=1.sp)
     Spacer(Modifier.height(6.dp));Text(m.text,fontSize=14.sp,lineHeight=21.sp)
    }}
   }
  }
  vm.error?.let{message->Text(message,Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF352B23)).clickable{vm.clearError()}.padding(10.dp),color=Warm,fontSize=11.sp)}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
   OutlinedTextField(input,{input=it},Modifier.weight(1f),placeholder={Text("Or type a question",fontSize=12.sp)},singleLine=true,shape=RoundedCornerShape(12.dp))
   TextButton(onClick={vm.ask(input);input=""},enabled=vm.voiceReady&&input.isNotBlank()){Text("Ask")}
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Button(onClick={if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)vm.toggleRecording()else permission.launch(Manifest.permission.RECORD_AUDIO)},enabled=vm.voiceReady||vm.recording,modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(13.dp),colors=ButtonDefaults.buttonColors(containerColor=if(vm.recording)Warm else Mint)){
    Text(if(vm.recording)"■  End voice" else "●  Start voice",fontWeight=FontWeight.Bold)
   }
   OutlinedButton(onClick={vm.interrupt()},modifier=Modifier.height(52.dp),shape=RoundedCornerShape(13.dp)){Text("Stop",color=Muted)}
  }
  Text(if(vm.recording)"Microphone on · speak naturally, or tap End voice." else "Tap once to talk continuously. Tap again to stop.",fontSize=10.sp,color=Muted)
 }
}
@Composable fun DocumentPanel(vm:ManualViewModel,modifier:Modifier=Modifier){
 val p=vm.selected
 Column(modifier.clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp,Line,RoundedCornerShape(20.dp))){
  Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text("SOURCE DOCUMENT",fontSize=10.sp,color=Mint,letterSpacing=1.5.sp);Spacer(Modifier.height(3.dp));Text(p?.name?:"Select a product",fontSize=16.sp,fontWeight=FontWeight.Medium)}
   TextButton(onClick={vm.zoomBy(-0.5f)}){Text("−",fontSize=22.sp)}
   Text("${(vm.zoom*100).toInt()}%",fontSize=11.sp,color=Muted)
   TextButton(onClick={vm.zoomBy(0.5f)}){Text("+",fontSize=22.sp)}
  }
  HorizontalDivider(color=Line)
  Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF25303A)),contentAlignment=Alignment.TopCenter){
   if(p!=null)PdfPage(p,vm.page,vm.zoom,vm.renderRequest){request,ok->vm.rendered(request,ok)}
  }
  HorizontalDivider(color=Line)
  Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   TextButton(onClick={vm.show(vm.page-1)},enabled=vm.page>1){Text("← Previous")}
   Spacer(Modifier.weight(1f));val label=if(p!=null&&vm.page>p.offset)(vm.page-p.offset).toString()else "Cover"
   Text("Page $label  ·  PDF ${vm.page}/${p?.pageCount?:0}",fontSize=11.sp,color=Muted)
   Spacer(Modifier.weight(1f));TextButton(onClick={vm.show(vm.page+1)},enabled=p!=null&&vm.page<p.pageCount){Text("Next →")}
  }
 }
}
private data class RenderedPage(val productId:String,val number:Int,val bitmap:Bitmap)
@Composable fun PdfPage(product:Product,number:Int,zoom:Float,request:String?,ack:(String?,Boolean)->Unit){
 val context=LocalContext.current
 var failure by remember(product.id,number){mutableStateOf<String?>(null)}
 val rendered by produceState<RenderedPage?>(null,product.id,number){
  value=null
  try{value=RenderedPage(product.id,number,withContext(Dispatchers.IO){
   val f=File(context.cacheDir,product.file)
   if(!f.exists())context.assets.open("manuals/${product.file}").use{source->f.outputStream().use{source.copyTo(it)}}
   ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY).use{fd->PdfRenderer(fd).use{renderer->renderer.openPage(number-1).use{page->
    Bitmap.createBitmap(1700,(1700f*page.height/page.width).toInt(),Bitmap.Config.ARGB_8888).also{it.eraseColor(android.graphics.Color.WHITE);page.render(it,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)}
   }}}
  })}catch(e:Exception){failure="Page could not be rendered: ${e.message}"}
 }
 val bitmap=rendered?.takeIf{it.productId==product.id&&it.number==number}?.bitmap
 LaunchedEffect(bitmap,request,failure){if(bitmap!=null)ack(request,true)else if(failure!=null)ack(request,false)}
 BoxWithConstraints(Modifier.fillMaxSize()){
  val viewWidth=maxWidth
  val viewHeight=maxHeight
  val image=bitmap
  if(image!=null){
   Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()),contentAlignment=Alignment.TopCenter){
    Image(image.asImageBitmap(),"${product.name} PDF page $number",Modifier.width(minOf(viewWidth,viewHeight/(image.height.toFloat()/image.width))*zoom).height(minOf(viewWidth,viewHeight/(image.height.toFloat()/image.width))*zoom*(image.height.toFloat()/image.width)),contentScale=ContentScale.FillWidth)
   }
  }else if(failure!=null)Text(failure!!,Modifier.padding(24.dp),color=Warm)
  else CircularProgressIndicator(Modifier.align(Alignment.Center),color=Mint)
 }
}
