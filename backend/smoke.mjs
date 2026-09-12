import {WebSocket} from 'ws';
const ws=new WebSocket('ws://127.0.0.1:8787');let asked=false,audio=0;
const timeout=setTimeout(()=>{console.error('TIMEOUT');ws.close();process.exitCode=1},45000);
ws.on('message',raw=>{const e=JSON.parse(raw);if(e.type==='audio.delta'){audio+=Buffer.from(e.audio,'base64').length;return;}
if(e.type==='transcript.delta')return;
console.log(e.type,JSON.stringify(e));
if(e.type==='status'&&e.voiceReady&&!asked){asked=true;ws.send(JSON.stringify({type:'text.ask',text:'Show me the drain connections diagram and briefly describe what is on it.'}));}
if(e.type==='document.show')ws.send(JSON.stringify({type:'document.ack',requestId:e.requestId,ok:true}));
if(e.type==='response.done'){console.log('AUDIO_BYTES',audio);clearTimeout(timeout);ws.close();}
if(e.type==='error'){clearTimeout(timeout);ws.close();process.exitCode=1;}
});
