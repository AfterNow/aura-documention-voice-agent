import test from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {networkInterfaces} from 'node:os';
import {WebSocket} from 'ws';
import {createApp} from './server.mjs';

test('LAN listener accepts HTTP and document WebSockets over loopback and local IPv4',async()=>{
 const app=createApp({apiKey:'',host:'0.0.0.0',port:0});await once(app.server,'listening');
 try{
  const hosts=['127.0.0.1',...Object.values(networkInterfaces()).flat().filter(a=>a.family==='IPv4'&&!a.internal).map(a=>a.address)];
  for(const host of new Set(hosts)){
   const address=`${host}:${app.server.address().port}`;
   const health=await fetch(`http://${address}/health`,{signal:AbortSignal.timeout(3000)}).then(r=>r.json());
   assert.equal(health.ok,true);assert.equal(health.voiceConfigured,false);assert.equal(health.products,3);
   const ws=new WebSocket(`ws://${address}`);
   try{const [data]=await once(ws,'message',{signal:AbortSignal.timeout(3000)});const event=JSON.parse(data);assert.equal(event.type,'catalog');assert.equal(event.products.length,3);}finally{ws.terminate();}
  }
 }finally{for(const ws of app.wss.clients)ws.terminate();await new Promise(r=>app.wss.close(r));await new Promise(r=>app.server.close(r));}
});
