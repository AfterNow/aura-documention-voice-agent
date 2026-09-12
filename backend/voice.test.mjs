import test from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {WebSocket,WebSocketServer} from 'ws';
import {createApp} from './server.mjs';
const pause=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn){for(let i=0;i<200;i++){const value=fn();if(value)return value;await pause(10);}throw Error('Timed out');}
test('continuous voice gates input, enables VAD, interrupts, truncates, and stops late responses',async()=>{
 const upstream=new WebSocketServer({port:0,host:'127.0.0.1'});await once(upstream,'listening');
 const received=[];let remote;
 upstream.on('connection',ws=>{remote=ws;ws.on('message',raw=>{const e=JSON.parse(raw);received.push(e);if(e.type==='session.update')ws.send(JSON.stringify({type:'session.updated'}));});});
 const app=createApp({apiKey:'test-only',port:0,realtimeUrl:`ws://127.0.0.1:${upstream.address().port}`});await once(app.server,'listening');
 const client=new WebSocket(`ws://127.0.0.1:${app.server.address().port}`);const events=[];client.on('message',raw=>events.push(JSON.parse(raw)));
 const send=e=>client.send(JSON.stringify(e));const emit=e=>remote.send(JSON.stringify(e));
 try{
  await until(()=>events.some(e=>e.voiceReady));
  send({type:'audio.append',audio:'AAAA'});await pause(30);assert.equal(received.filter(e=>e.type==='input_audio_buffer.append').length,0);
  send({type:'audio.start'});
  const setting=await until(()=>received.find(e=>e.session?.audio?.input?.turn_detection?.type==='semantic_vad'));
  assert.equal(setting.session.audio.input.turn_detection.create_response,true);assert.equal(setting.session.audio.input.turn_detection.interrupt_response,true);
  send({type:'audio.append',audio:'AAAA'});await until(()=>received.some(e=>e.type==='input_audio_buffer.append'));
  emit({type:'response.created',response:{id:'first'}});
  emit({type:'response.output_audio.delta',response_id:'first',item_id:'spoken',content_index:0,delta:Buffer.alloc(48000).toString('base64')});
  await until(()=>events.some(e=>e.type==='audio.delta'));
  const clears=events.filter(e=>e.type==='audio.clear').length;
  emit({type:'input_audio_buffer.speech_started'});await until(()=>events.filter(e=>e.type==='audio.clear').length>clears);
  send({type:'audio.played',itemId:'spoken',audioEndMs:200});
  const cut=await until(()=>received.find(e=>e.type==='conversation.item.truncate'));assert.equal(cut.audio_end_ms,200);
  emit({type:'response.output_audio.delta',response_id:'first',item_id:'spoken',delta:'AAAA'});await pause(30);assert.equal(events.filter(e=>e.type==='audio.delta').length,1);
  emit({type:'response.created',response:{id:'second'}});emit({type:'response.done',response:{id:'second',output:[],status:'completed'}});
  await until(()=>events.some(e=>e.type==='response.done'));
  send({type:'audio.stop'});await until(()=>received.some(e=>e.session?.audio?.input?.turn_detection===null)&&events.some(e=>e.status==='Voice off'));
  const count=received.filter(e=>e.type==='input_audio_buffer.append').length;
  send({type:'audio.append',audio:'AAAA'});emit({type:'response.created',response:{id:'late'}});
  await until(()=>received.some(e=>e.type==='response.cancel'));await pause(40);
  assert.equal(received.filter(e=>e.type==='input_audio_buffer.append').length,count);
  assert.equal(received.filter(e=>e.type==='input_audio_buffer.commit').length,0);
  send({type:'audio.start'});await until(()=>received.filter(e=>e.session?.audio?.input?.turn_detection?.type==='semantic_vad').length===2);
  emit({type:'response.created',response:{id:'restarted'}});emit({type:'response.output_audio.delta',response_id:'restarted',item_id:'new',content_index:0,delta:'AAAA'});
  await until(()=>events.filter(e=>e.type==='audio.delta').length===2);
 }finally{client.terminate();for(const ws of app.wss.clients)ws.terminate();remote?.terminate();await new Promise(r=>app.wss.close(r));await new Promise(r=>app.server.close(r));await new Promise(r=>upstream.close(r));}
});
