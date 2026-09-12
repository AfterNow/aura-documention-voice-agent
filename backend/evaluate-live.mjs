// Live API integration test. Simulates renderer ACKs; does not assert headset rendering.
import {WebSocket} from 'ws';
import fs from 'node:fs';
const cases=[
 ['db200h','Show me the drain connection drawing and explain it briefly.'],
 ['db200h','Where is the safety switch shown? Open that page.'],
 ['vsx','Show the cartridge seal cross-section.'],
 ['vsx','What does the manual say about pump bearing lubrication?'],
 ['mlg202dr','Show the dryer front component identification drawing.'],
 ['mlg202dr','What is the maximum dry weight capacity of the 202DR?']
];
const results=[];
for(const [productId,question] of cases){
 const result=await new Promise((resolve,reject)=>{
  const ws=new WebSocket('ws://127.0.0.1:8787');let selected=false,asked=false;
  const r={productId,question,tools:[],pages:[],answer:'',audioBytes:0};
  const timer=setTimeout(()=>{ws.close();reject(Error('Timeout: '+question))},60000);
  ws.on('error',reject);
  ws.on('message',raw=>{const e=JSON.parse(raw);
   if(e.type==='product.selected'&&e.productId===productId)selected=true;
   if(e.type==='status'&&e.voiceReady&&!asked){
    if(!selected){ws.send(JSON.stringify({type:'product.select',productId}));return;}
    asked=true;ws.send(JSON.stringify({type:'text.ask',text:question}));
   }
   if(e.type==='tool')r.tools.push(e.name);
   if(e.type==='document.show'){r.pages.push(e.pdfPage);ws.send(JSON.stringify({type:'document.ack',requestId:e.requestId,ok:true}));}
   if(e.type==='audio.delta')r.audioBytes+=Buffer.from(e.audio,'base64').length;
   if(e.type==='transcript.done')r.answer+=e.text;
   if(e.type==='error'){clearTimeout(timer);ws.close();reject(Error(e.message));}
   if(e.type==='response.done'&&asked){clearTimeout(timer);ws.close();resolve(r);}
  });
 });
 results.push(result);console.log(JSON.stringify(result));
}
fs.mkdirSync('../work',{recursive:true});fs.writeFileSync('../work/live-evaluation.json',JSON.stringify(results,null,2));
if(results.some(r=>!r.audioBytes||!r.answer||!r.tools.some(t=>['show_page','get_page','search_manual'].includes(t))))process.exitCode=1;
