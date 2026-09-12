"""Push a source checkpoint through the GitHub Git Data API when local Git is read-only."""
import argparse,base64,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REPO="AfterNow/aura-documention-voice-agent"
def api(endpoint,payload=None,method=None):
 cmd=["gh","api",f"repos/{REPO}/{endpoint}"]
 if payload is not None:cmd += ["--method",method or "POST","--input","-"]
 p=subprocess.run(cmd,input=json.dumps(payload) if payload is not None else None,text=True,capture_output=True,check=True)
 return json.loads(p.stdout)
def source_files():
 files=[ROOT/n for n in [".gitignore","README.md","settings.gradle.kts","build.gradle.kts","gradle.properties","gradlew","gradlew.bat","backend/package.json","backend/pnpm-lock.yaml","backend/.env.example","app/build.gradle.kts","app/src/main/AndroidManifest.xml"]]
 for directory,pattern in [("app/src/main/java","*.kt"),("backend","*.mjs"),("scripts","*.py"),("scripts","*.ps1"),("docs","*.md"),("gradle/wrapper","*")]:
  files.extend((ROOT/directory).rglob(pattern))
 return sorted(set(p for p in files if p.is_file()))
def main():
 parser=argparse.ArgumentParser();parser.add_argument("message");args=parser.parse_args()
 head=api("git/ref/heads/main")["object"]["sha"]
 tree=api(f"git/commits/{head}")["tree"]["sha"]
 old={x["path"]:x for x in api(f"git/trees/{tree}?recursive=1")["tree"]}
 import hashlib
 updates=[]
 for p in source_files():
  name=p.relative_to(ROOT).as_posix();data=p.read_bytes()
  sha=hashlib.sha1(b"blob "+str(len(data)).encode()+b"\0"+data).hexdigest()
  if old.get(name,{}).get("sha")==sha:continue
  blob=api("git/blobs",{"content":base64.b64encode(data).decode(),"encoding":"base64"})
  updates.append({"path":name,"mode":"100755" if name=="gradlew" else "100644","type":"blob","sha":blob["sha"]})
 if not updates:print("No changed source files");return
 newtree=api("git/trees",{"base_tree":tree,"tree":updates})
 commit=api("git/commits",{"message":args.message,"tree":newtree["sha"],"parents":[head]})
 api("git/refs/heads/main",{"sha":commit["sha"],"force":False},"PATCH")
 print("Pushed",commit["sha"],len(updates),"source files")
if __name__=="__main__":main()
