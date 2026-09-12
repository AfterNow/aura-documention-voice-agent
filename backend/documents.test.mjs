import test from 'node:test';
import assert from 'node:assert/strict';
import {catalog,page,search,imageData,walkthrough} from './documents.mjs';
test('exactly the three demo products are available',()=>assert.deepEqual(catalog.map(p=>p.id).sort(),['db200h','mlg202dr','vsx']));
test('PDF and printed page numbers match verified figures',()=>{
 assert.equal(page('db200h',14).printedPage,'14');
 assert.equal(page('vsx',31).printedPage,'28');
 assert.equal(page('mlg202dr',19).printedPage,'12');
 assert.match(page('mlg202dr',19).text,/COMPONENT IDENTIFICATION/);
});
test('search is restricted to the active product',()=>{
 for(const p of catalog)assert.ok(search(p.id,'electrical maintenance').every(r=>r.productId===p.id));
 assert.equal(search('db200h','drain connection')[0].pdfPage,14);
 assert.equal(search('vsx','seal cross sections')[0].pdfPage,31);
 assert.equal(search('mlg202dr','component identification')[0].pdfPage,19);
});
test('unknown products, path traversal and invalid pages fail closed',()=>{
 for(const [id,n] of [['../vsx',1],['db200h',0],['db200h',20],['vsx',1.5]])assert.throws(()=>page(id,n));
 assert.deepEqual(search('db200h','xyzzynonexistent'),[]);
});
test('all curated sources and walkthrough steps resolve to existing pages and images',()=>{
 for(const p of catalog)for(const t of p.topics){assert.ok(page(p.id,t.page));assert.ok(imageData(p.id,t.page).startsWith('data:image/jpeg;base64,'));}
 for(const s of walkthrough.steps)assert.ok(page(walkthrough.productId,s.page));
});
