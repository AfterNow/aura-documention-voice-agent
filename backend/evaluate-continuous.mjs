// Live integration check. Uses API credits to synthesize two spoken questions,
// then streams them into the running backend without manual audio commits.
import {WebSocket} from 'ws';
import {once} from 'node:events';
import assert from 'node:assert/strict';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=45000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(50);}throw Error('Timed out');}
async function spoken(text){
 const ws=new WebSocket('wss://api.openai.com/v1/realtime?model=gpt-realtime',{headers:{Authorization:'Bearer '+(process.env.OPENAI_API_KEY||process.env.OPENAI_KEY)}});
 const chunks=[];let done=false,error;
 ws.on('message',raw=>{const e=JSON.parse(raw);if(e.type==='response.output_audio.delta')chunks.push(Buffer.from(e.delta,'base64'));if(e.type==='response.done')done=true;if(e.type==='error')error=e.error.message;});
 await once(ws,'open');
 ws.send(JSON.stringify({type:'session.update',session:{type:'realtime',audio:{input:{turn_detection:null},output:{format:{type:'audio/pcm',rate:24000},voice:'marin'}}}}));
 ws.send(JSON.stringify({type:'response.create',response:{instructions:'Read only this sentence aloud verbatim, without answering it: '+text}}));
 try{await until(()=>done||error);if(error)throw Error(error);return Buffer.concat(chunks);}finally{ws.close();}
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
  const before=count('audio.delta');await stream(questions[i]);
  const end=Date.now()+30000;
  while(count('audio.delta')===before&&Date.now()<end){await stream(Buffer.alloc(2400));if(count('error'))throw Error(events.find(e=>e.type==='error').message);}
  assert.ok(count('audio.delta')>before,'Automatic answer after speech without a commit');
  console.log('Automatic spoken answer',i+1);
  // The next question deliberately interrupts the first generated answer.
 }
 await until(()=>count('transcript.user')>=2);
 send({type:'audio.stop'});await until(()=>events.some(e=>e.status==='Voice off'));
 const before=count('audio.delta');await stream(Buffer.alloc(48000));await sleep(300);
 assert.equal(count('audio.delta'),before,'No playback after voice stops');
 assert.equal(count('error'),0);
 console.log(JSON.stringify({passed:true,questions:events.filter(e=>e.type==='transcript.user').map(e=>e.text),pages:events.filter(e=>e.type==='document.show').map(e=>e.pdfPage),audioChunks:count('audio.delta')}));
}finally{ws.close();}
