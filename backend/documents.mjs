import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
export const dataDir=path.join(path.dirname(fileURLToPath(import.meta.url)),'data');
export const catalog=JSON.parse(fs.readFileSync(path.join(dataDir,'catalog.json'),'utf8'));
const pages=JSON.parse(fs.readFileSync(path.join(dataDir,'pages.json'),'utf8'));
const stop=new Set('a an the me show tell about what where how is are do does to of for in on this that please can you i it and with my explain'.split(' '));
const tokens=t=>(t.toLowerCase().match(/[a-z0-9]+/g)||[]).filter(t=>!stop.has(t));
export function product(id){const p=catalog.find(p=>p.id===id);if(!p)throw Error('Unknown product');return p;}
export function page(id,n){product(id);if(!Number.isInteger(n))throw Error('Page must be an integer');const p=pages.find(p=>p.productId===id&&p.pdfPage===n);if(!p)throw Error('Page outside this manual');return p;}
export function search(id,query,limit=4){
 product(id);const terms=tokens(query);if(!terms.length)return [];
 return pages.filter(p=>p.productId===id).map(p=>{
  const text=p.text.toLowerCase();const tags=p.tags.join(' ').toLowerCase();
  const score=terms.reduce((s,t)=>s+(text.includes(t)?1:0)+(tags.includes(t)?8:0),0);
  return {...p,score};
 }).filter(p=>p.score>0).sort((a,b)=>b.score-a.score).slice(0,limit).map(p=>({...p,text:p.text.slice(0,9500)}));
}
export function imageData(id,n){page(id,n);return 'data:image/jpeg;base64,'+fs.readFileSync(path.join(dataDir,id,`${n}.jpg`)).toString('base64');}
export function brief(p){return {productId:p.productId,pdfPage:p.pdfPage,printedPage:p.printedPage,tags:p.tags,text:p.text};}
export const walkthrough={id:'db200h-inspection',productId:'db200h',title:'Installation checklist review',steps:[
 {page:13,title:'Review the installation checklist',text:'Review the manufacturer’s final checklist on printed page 13. This is a documentation walkthrough; do not operate or service equipment during this demo.'},
 {page:9,title:'Review the installation location',text:'Read the location requirements on printed page 9, including ambient conditions, a firm level foundation, and service clearances. Confirm you have reviewed the page before continuing.'},
 {page:14,title:'Inspect the drain drawing',text:'Review Figure 4 and its two configurations. Compare it with the installation instructions on printed page 13. Do not assume the two drawings apply to the same configuration.'},
 {page:13,title:'Finish the checklist review',text:'Return to the manufacturer checklist. Electrical and plumbing verification must be performed by appropriately qualified personnel. The walkthrough records review, not certification of the installation.'}
]};
