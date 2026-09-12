"""Prepare local PDF assets and per-page text/image retrieval. Never uploads manuals."""
import argparse, hashlib, json, shutil
from pathlib import Path
from pypdf import PdfReader
import pypdfium2 as pdfium
ROOT=Path(__file__).resolve().parents[1]
CATALOG=[
 {"id":"mlg202dr","name":"MLG-202DR","kind":"Industrial dryer","manufacturer":"American Dryer Corporation","file":"mlg202dr.pdf","offset":7,"revision":"113289-6 (ManualsLib wrapper: +3 PDF pages)","aliases":["dryer","MLG 202 DR"],"topics":[{"label":"202DR specifications","page":15},{"label":"Component identification","page":19},{"label":"Electrical information","page":32},{"label":"Routine maintenance","page":52}]},
 {"id":"db200h","name":"DB-200H","kind":"Ice dispenser","manufacturer":"Hoshizaki","file":"db200h.pdf","offset":0,"revision":"2001-06-05","aliases":["ice dispenser","DB 200 H"],"topics":[{"label":"Dimensions and connections","page":5},{"label":"Electrical connection","page":12},{"label":"Drain connection drawing","page":14},{"label":"Cleaning and maintenance","page":16}]},
 {"id":"vsx","name":"VSX / P500219","kind":"Centrifugal pump","manufacturer":"Bell & Gossett","file":"vsx.pdf","offset":3,"revision":"P5002169","aliases":["pump","P500219","P5002169","VSX"],"topics":[{"label":"Troubleshooting","page":24},{"label":"Seal cross-sections","page":31},{"label":"Exploded stuffing box","page":35},{"label":"Coupling alignment","page":11}]}
]
def main():
 parser=argparse.ArgumentParser();parser.add_argument("source",type=Path);args=parser.parse_args()
 data=ROOT/"backend/data";assets=ROOT/"app/src/main/assets/manuals"
 data.mkdir(parents=True,exist_ok=True);assets.mkdir(parents=True,exist_ok=True)
 pages=[];catalog=[]
 for product in CATALOG:
  path=args.source/product["file"]
  if not path.exists():
   print("MISSING",path);continue
  reader=PdfReader(path);doc=pdfium.PdfDocument(path)
  product={**product,"pageCount":len(reader.pages),"sha256":hashlib.sha256(path.read_bytes()).hexdigest()}
  catalog.append(product);shutil.copyfile(path,assets/product["file"])
  images=data/product["id"];images.mkdir(exist_ok=True)
  for i,page in enumerate(reader.pages):
   n=i+1;label=str(n-product["offset"]) if n>product["offset"] else ("Cover" if n==1 else "Front matter")
   text=page.extract_text() or ""
   tags=[t["label"] for t in product["topics"] if t["page"]==n]
   pages.append({"productId":product["id"],"pdfPage":n,"printedPage":label,"text":text,"tags":tags})
   doc[i].render(scale=1.7).to_pil().convert("RGB").save(images/f"{n}.jpg",quality=85)
  print(product["id"],len(reader.pages),"pages")
 (data/"catalog.json").write_text(json.dumps(catalog,indent=2),encoding="utf-8")
 (data/"pages.json").write_text(json.dumps(pages,ensure_ascii=False),encoding="utf-8")
 (assets/"catalog.json").write_text(json.dumps(catalog,indent=2),encoding="utf-8")
if __name__=="__main__":main()
