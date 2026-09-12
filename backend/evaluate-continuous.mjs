// Live integration check. Uses API credits to synthesize two spoken questions,
// then streams them into the running backend without manual audio commits.
import {WebSocket} from 'ws';
import assert from 'node:assert/strict';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=45000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(50);}throw Error('Timed out');}
async function spoken(text){
 const response=await fetch('https://api.openai.com/v1/audio/speech',{method:'POST',headers:{Authorization:'Bearer '+(process.env.OPENAI_API_KEY||process.env.OPENAI_KEY),'Content-Type':'application/json'},body:JSON.stringify({model:'tts-1',voice:'alloy',input:text,response_format:'pcm'}),signal:AbortSignal.timeout(30000)});
 if(!response.ok)throw Error('Test speech generation failed: HTTP '+response.status);
 return Buffer.from(await response.arrayBuffer());
}
const questions=[];
for(const text of ['Show me the drain connection diagram.','What size is the drain connection?'])questions.push(await spoken(text));
const ws=new WebSocket('ws://127.0.0.1:8787');const events=[];
ws.on('message',raw=>{const e=JSON.parse(raw);events.push(e);if(e.type==='document.show')ws.send(JSON.stringify({type:'document.ack',requestId:e.requestId,ok:true}));});
const count=t=>events.filter(e=>e.type===t).length;
const send=e=>ws.send(JSON.stringify(e));
async function stream(pcm){for(let i=0;i<pcm.length;i+=2400){send({type:'audio.append',audio:pcm.subarray(i,i+2400).toString('base64')});await sleep(50);}}
try{
 await until(()=>events.some(e=>e.voiceReady));send({type:'audio.start'});
 for(let i=0;i<questions.length;i++){
  const previousItems=new Set(events.filter(e=>e.type==='audio.delta').map(e=>e.itemId));
  const newAnswer=()=>events.some(e=>e.type==='audio.delta'&&!previousItems.has(e.itemId));
  await stream(questions[i]);
  const end=Date.now()+30000;
  while(!newAnswer()&&Date.now()<end){await stream(Buffer.alloc(2400));if(count('error'))throw Error(events.find(e=>e.type==='error').message);}
  assert.ok(newAnswer(),'Automatic answer after speech without a commit');
  console.log('Automatic spoken answer',i+1);
  // The next question deliberately interrupts the first generated answer.
 }
 await until(()=>count('transcript.user')>=2);
 assert.match(events.filter(e=>e.type==='transcript.user')[0].text,/drain connection diagram/i);
 assert.match(events.filter(e=>e.type==='transcript.user')[1].text,/size.*drain connection/i);
 send({type:'audio.stop'});await until(()=>events.some(e=>e.status==='Voice off'));
 const before=count('audio.delta');await stream(Buffer.alloc(48000));await sleep(300);
 assert.equal(count('audio.delta'),before,'No playback after voice stops');
 assert.equal(count('error'),0);
 console.log(JSON.stringify({passed:true,questions:events.filter(e=>e.type==='transcript.user').map(e=>e.text),pages:events.filter(e=>e.type==='document.show').map(e=>e.pdfPage),audioChunks:count('audio.delta')}));
}finally{ws.close();}
