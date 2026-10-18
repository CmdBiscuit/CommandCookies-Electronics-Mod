'use strict';
const {app,BrowserWindow,session,components,safeStorage,ipcMain}=require('electron');
const fs=require('node:fs'),path=require('node:path'),net=require('node:net');
const {encodeUTF,Input}=require('./protocol');
const [profileArg,dataArg,portArg,token,widthArg,heightArg]=process.argv.slice(-6);
const profile=path.resolve(profileArg||''),port=Number(portArg);
let width=Number(widthArg),height=Number(heightArg),closing=false,selected=null,socket,latest=null,pumping=false,saveTimer=null,dialogTab=null,pendingDialog=null,pressed=0,lastClick=null;
let frameRate=process.env.CEM_BROWSER_FPS==='60'?60:25,visible=true,lastMedia=null;
let mediaTimer;
const tabs=[],events=[],HOME='https://www.google.com/',MAX_TABS=8,MAX_W=2560,MAX_H=1440;
if(!profileArg||!dataArg||!Number.isInteger(port)||port<1||port>65535||!/^[a-f0-9]{64}$/.test(token||'')||!validSize(width,height))throw new Error('Invalid browser launch');
const dataRoot=path.resolve(dataArg),tabsFile=path.join(profile,'tabs.json'),sessionCookiesFile=path.join(dataRoot,'session-cookies.bin');
fs.mkdirSync(dataRoot,{recursive:true});app.setPath('userData',dataRoot);app.disableHardwareAcceleration();app.enableSandbox();
// Identify the CEM browser with the real Chromium version; runtime branding can trigger app-only website layouts.
app.userAgentFallback=app.userAgentFallback.replace(/ Electron\/[0-9.+-]+/g,'').replace(/ cem-media-browser\/[0-9.]+/g,'')+' CEMBrowser/1.0';
function validSize(w,h){return Number.isInteger(w)&&Number.isInteger(h)&&w>0&&h>0&&w<=MAX_W&&h<=MAX_H;}
function allowed(value){try{const u=new URL(value);return typeof value==='string'&&value.length<=2048&&['http:','https:'].includes(u.protocol)&&!!u.hostname&&!u.username&&!u.password;}catch{return false;}}
function limited(value,count){return String(value??'').slice(0,count);}
function event(type,values={}){const value=JSON.stringify({type,...values});if(value.length>12000)return;if(events.length>=64)events.shift();events.push(value);pump();}
function answerDialog(accept,text=''){
  const pending=pendingDialog;pendingDialog=null;dialogTab=null;if(!pending)return;
  try{pending.event.returnValue={accept:!!accept,text:accept?limited(text,500):null};}catch{}
}
ipcMain.on('cem-browser-dialog',(request,kind,message,initial)=>{
  const tab=tabs.find(t=>t.win&&!t.win.isDestroyed()&&t.win.webContents===request.sender);
  if(closing||!visible||!tab||tab!==selected||request.senderFrame!==request.sender.mainFrame||pendingDialog||!['alert','confirm','prompt'].includes(kind)||typeof message!=='string'||typeof initial!=='string'){
    request.returnValue={accept:false,text:null};return;
  }
  pendingDialog={event:request,kind};dialogTab=tab;event('dialog',{kind,message:limited(message,1000),initial:limited(initial,500)});
});
function mediaState(){
  const playing=tabs.some(tab=>{const web=tab.win?.webContents;if(!web||web.isDestroyed())return false;const audible=web.isCurrentlyAudible();if(audible)tab.wasAudible=true;return audible||!!(tab.playing&&tab.wasAudible&&!web.isAudioMuted());});
  if(playing!==lastMedia){lastMedia=playing;event('media',{playing});}
}
function visibility(value){
  if(typeof value!=='boolean')return;visible=value;answerDialog(false);pressed=0;lastClick=null;latest=null;
  for(const tab of tabs)if(tab.win&&!tab.win.isDestroyed()){
    const web=tab.win.webContents;if(visible&&tab===selected){web.startPainting();web.invalidate();web.focus();}
    else{web.stopPainting();tab.win.blur();tab.frame=null;}
  }
  event('visibility',{visible});mediaState();
}
function state(){if(!selected||closing)return;const web=selected.win?.webContents;event('state',{url:selected.url,title:selected.title,loading:web&&!web.isDestroyed()?web.isLoading():false,back:web&&!web.isDestroyed()?web.navigationHistory.canGoBack():false,forward:web&&!web.isDestroyed()?web.navigationHistory.canGoForward():false,active:Math.max(0,tabs.indexOf(selected)),tabs:tabs.map(t=>({title:t.title}))});}
function save(){
  clearTimeout(saveTimer);saveTimer=null;if(!tabs.length)return;
  const saved={version:1,active:Math.max(0,tabs.indexOf(selected)),tabs:tabs.map(t=>({url:allowed(t.url)?t.url:HOME,title:limited(t.title,200)}))};
  const temp=tabsFile+'.tmp';try{fs.writeFileSync(temp,JSON.stringify(saved));fs.renameSync(temp,tabsFile);}catch{event('notice',{message:'Browser tabs could not be saved.'});}
}
function scheduleSave(){clearTimeout(saveTimer);saveTimer=setTimeout(save,100);}
let cookieSaveTimer=null,cookieSave=Promise.resolve();
function saveSessionCookies(store){
  clearTimeout(cookieSaveTimer);cookieSaveTimer=null;
  cookieSave=cookieSave.then(async()=>{
    if(!safeStorage.isEncryptionAvailable())return;
    const cookies=(await store.cookies.get({})).filter(cookie=>cookie.session);
    const text=JSON.stringify({version:1,cookies});if(text.length>4*1024*1024)throw new Error('Cookie storage limit');
    const temp=sessionCookiesFile+'.tmp';fs.writeFileSync(temp,safeStorage.encryptString(text));fs.renameSync(temp,sessionCookiesFile);
  }).catch(()=>{console.error('Browser session cookies could not be saved.');});return cookieSave;
}
async function restoreSessionCookies(store){
  try{
    if(!safeStorage.isEncryptionAvailable()||!fs.existsSync(sessionCookiesFile)||fs.statSync(sessionCookiesFile).size>5*1024*1024)return;
    const saved=JSON.parse(safeStorage.decryptString(fs.readFileSync(sessionCookiesFile)));if(saved.version!==1||!Array.isArray(saved.cookies)||saved.cookies.length>4096)return;
    for(const cookie of saved.cookies){
      const host=String(cookie.domain||'').replace(/^\./,'');if(!host||host.length>253||/[\s/\\@?#]/.test(host))continue;
      const address=(cookie.secure?'https://':'http://')+(host.includes(':')?'['+host+']':host)+(cookie.path||'/');
      const details={url:address,name:cookie.name,value:cookie.value,path:cookie.path||'/',secure:!!cookie.secure,httpOnly:!!cookie.httpOnly,sameSite:cookie.sameSite||'unspecified'};if(!cookie.hostOnly)details.domain=cookie.domain;
      try{await store.cookies.set(details);}catch{}
    }
  }catch{console.error('Browser session cookies could not be restored.');}
}
function restore(){
  let active=0;
  try{if(fs.statSync(tabsFile).size<=65536){const data=JSON.parse(fs.readFileSync(tabsFile,'utf8'));if(data.version===1&&Array.isArray(data.tabs)){for(const t of data.tabs.slice(0,MAX_TABS))if(allowed(t.url))tabs.push({url:t.url,title:limited(t.title||'Chromium',200),win:null,frame:null});active=Number.isInteger(data.active)?data.active:0;}}}catch{}
  if(!tabs.length)tabs.push({url:HOME,title:'Google',win:null,frame:null});selected=tabs[Math.max(0,Math.min(tabs.length-1,active))];
}
function pump(){
  if(!socket||socket.destroyed||pumping)return;pumping=true;
  const output=[];
  while(events.length){output.push(Buffer.from([1]),encodeUTF(events.shift()));}
  if(latest){const frame=latest;latest=null;const header=Buffer.alloc(13);header[0]=2;header.writeInt32BE(frame.w,1);header.writeInt32BE(frame.h,5);header.writeInt32BE(frame.pixels.length,9);output.push(header,frame.pixels);}
  if(!output.length){pumping=false;return;}
  socket.write(Buffer.concat(output),()=>{pumping=false;if(events.length||latest)setTimeout(pump,0);});
}
function setFrame(tab,rect,image){
  if(closing||!visible||tab!==selected)return;const size=image.getSize();if(!validSize(size.width,size.height))return;
  const pixels=image.toBitmap({scaleFactor:1});if(pixels.length!==size.width*size.height*4)return;
  if(size.width===width&&size.height===height)tab.frame={w:width,h:height,pixels};
  else if(size.width===rect.width&&size.height===rect.height&&rect.x>=0&&rect.y>=0&&rect.x+size.width<=width&&rect.y+size.height<=height){
    if(!tab.frame||tab.frame.w!==width||tab.frame.h!==height)tab.frame={w:width,h:height,pixels:Buffer.alloc(width*height*4,255)};
    const result=Buffer.from(tab.frame.pixels);for(let y=0;y<size.height;y++)pixels.copy(result,((y+rect.y)*width+rect.x)*4,y*size.width*4,(y+1)*size.width*4);tab.frame={w:width,h:height,pixels:result};
  }else return;
  latest=tab.frame;pump();
}
function configureTab(tab){
  const win=new BrowserWindow({width,height,useContentSize:true,show:false,skipTaskbar:true,webPreferences:{offscreen:true,preload:path.join(__dirname,'preload.js'),disableDialogs:true,sandbox:true,nodeIntegration:false,contextIsolation:true,backgroundThrottling:false,devTools:false,webviewTag:false}});
  tab.win=win;tab.playing=false;tab.wasAudible=false;const web=win.webContents;web.setFrameRate(frameRate);
  web.on('media-started-playing',()=>{tab.playing=true;mediaState();});web.on('media-paused',()=>{tab.playing=false;mediaState();});web.on('audio-state-changed',()=>mediaState());
  web.on('did-start-navigation',(_e,_url,inPlace,main)=>{if(main&&!inPlace){tab.playing=false;tab.wasAudible=false;mediaState();}});
  if(!visible)web.stopPainting();
  web.on('paint',(_event,rect,image)=>setFrame(tab,rect,image));
  web.on('will-navigate',(e,url)=>{if(!allowed(url))e.preventDefault();});web.on('will-redirect',(e,url)=>{if(!allowed(url))e.preventDefault();});
  web.setWindowOpenHandler(({url})=>{if(allowed(url))openTab(url);return {action:'deny'};});
  const address=(_event,url)=>{if(allowed(url)){tab.url=url;state();scheduleSave();}};web.on('did-navigate',address);web.on('did-navigate-in-page',address);
  web.on('page-title-updated',(e,title)=>{e.preventDefault();tab.title=limited(title,200);state();scheduleSave();});
  web.on('did-start-loading',state);web.on('did-stop-loading',state);
  web.on('did-fail-load',(_event,code,description,url,main)=>{if(main&&tab===selected&&code!==-3)event('error',{message:limited('Page could not load: '+description,300)});});
  web.on('render-process-gone',()=>{tab.playing=false;tab.wasAudible=false;mediaState();if(tab===selected)event('error',{message:'This page stopped. Reload it to continue.'});});
  web.on('will-prevent-unload',e=>e.preventDefault());
  try{web.debugger.attach('1.3');web.debugger.sendCommand('Page.enable').catch(()=>{});web.debugger.sendCommand('Page.setInterceptFileChooserDialog',{enabled:true}).catch(()=>{});web.debugger.on('message',(_event,method,data)=>{
    if(method==='Page.fileChooserOpened'){
      if(data.backendNodeId)web.debugger.sendCommand('DOM.setFileInputFiles',{files:[],backendNodeId:data.backendNodeId}).catch(()=>{});
      event('notice',{message:'Website file uploads are not enabled.'});return;
    }
  });}catch{event('notice',{message:'Page dialogs are unavailable in this browser session.'});}
  win.on('closed',()=>{tab.win=null;});web.loadURL(tab.url).catch(()=>{});
}
function activate(tab){
  if(closing)return;answerDialog(false);
  if(selected?.win){selected.win.webContents.stopPainting();selected.win.blur();}
  selected=tab;latest=null;pressed=0;lastClick=null;
  if(!tab.win)configureTab(tab);else{tab.win.setContentSize(width,height);if(visible){tab.win.webContents.startPainting();tab.win.webContents.invalidate();if(tab.frame?.w===width&&tab.frame?.h===height)latest=tab.frame;}else tab.win.webContents.stopPainting();}
  if(visible)tab.win.webContents.focus();else tab.win.webContents.stopPainting();state();save();pump();
}
function openTab(url=HOME){if(closing)return;if(tabs.length===MAX_TABS){event('notice',{message:'You can keep up to eight tabs open.'});return;}const tab={url:allowed(url)?url:HOME,title:'Chromium',win:null,frame:null};tabs.push(tab);activate(tab);}
function closeTab(index){
  if(!Number.isInteger(index)||index<0||index>=tabs.length)return;
  const tab=tabs[index];if(tabs.length===1){tab.url=HOME;tab.title='Google';tab.win.webContents.loadURL(HOME).catch(()=>{});save();return;}
  tabs.splice(index,1);if(tab===selected)activate(tabs[Math.min(index,tabs.length-1)]);else{state();save();}if(tab.win)tab.win.destroy();
}
function modifiers(flags){const values=[];if(flags&1)values.push('shift');if(flags&2)values.push('control');if(flags&4)values.push('alt');if(flags&8)values.push('meta');return values;}
function keyCode(key){if(key>=65&&key<=90||key>=48&&key<=57)return String.fromCharCode(key);if(key>=290&&key<=314)return 'F'+(key-289);return {32:'Space',39:"'",44:',',45:'-',46:'.',47:'/',59:';',61:'=',91:'[',92:'\\',93:']',96:'`',256:'Escape',257:'Enter',335:'Enter',258:'Tab',259:'Backspace',260:'Insert',261:'Delete',262:'Right',263:'Left',264:'Down',265:'Up',266:'PageUp',267:'PageDown',268:'Home',269:'End',340:'Shift',344:'Shift',341:'Control',345:'Control',342:'Alt',346:'Alt',343:'Super',347:'Super'}[key];}
function input(value){
  if(value.action==='close'){shutdown();return;}if(closing||!selected?.win)return;const web=selected.win.webContents;
  if(!visible&&!['visibility','frameRate','dialog','resize'].includes(value.action))return;
  try{switch(value.action){
    case 'visibility':visibility(value.visible);break;
    case 'frameRate':if(value.fps===25||value.fps===60){frameRate=value.fps;for(const tab of tabs)if(tab.win&&!tab.win.isDestroyed())tab.win.webContents.setFrameRate(frameRate);}break;
    case 'navigate':if(allowed(value.url))web.loadURL(value.url).catch(()=>{});else event('error',{message:'Use an HTTP or HTTPS address.'});break;
    case 'newTab':openTab();break;
    case 'switchTab':if(Number.isInteger(value.index)&&tabs[value.index])activate(tabs[value.index]);break;
    case 'closeTab':closeTab(value.index);break;
    case 'back':if(web.navigationHistory.canGoBack())web.navigationHistory.goBack();break;
    case 'forward':if(web.navigationHistory.canGoForward())web.navigationHistory.goForward();break;
    case 'reload':web.reload();break;
    case 'resize':if(validSize(value.width,value.height)){width=value.width;height=value.height;selected.frame=null;selected.win.setContentSize(width,height);web.invalidate();}break;
    case 'focus':if(value.focus)web.focus();else selected.win.blur();break;
    case 'mouse':{
      const {kind,x,y,button}=value;if(![0,1,2].includes(kind)||![0,1,2].includes(button)||!Number.isInteger(x)||!Number.isInteger(y)||x<0||y<0||x>=width||y>=height)break;
      const now=Date.now();if(kind===1){web.focus();pressed|=1<<button;const again=lastClick&&lastClick.button===button&&now-lastClick.at<500&&Math.abs(x-lastClick.x)<4&&Math.abs(y-lastClick.y)<4;lastClick={button,x,y,at:now,count:again?Math.min(3,lastClick.count+1):1};}
      const flags=modifiers(value.mods||0);for(const [bit,name] of [[1,'leftButtonDown'],[2,'rightButtonDown'],[4,'middleButtonDown']])if(pressed&bit)flags.push(name);
      web.sendInputEvent({type:['mouseMove','mouseDown','mouseUp'][kind],x,y,button:['left','right','middle'][button],clickCount:kind===0?0:(lastClick?.count||1),modifiers:flags});if(kind===2)pressed&=~(1<<button);break;
    }
    case 'wheel':{
      if(!Number.isFinite(value.amount)||Math.abs(value.amount)>100||!Number.isInteger(value.x)||!Number.isInteger(value.y)||value.x<0||value.y<0||value.x>=width||value.y>=height)break;
      web.focus();const sideways=!!(value.mods&1);web.sendInputEvent({type:'mouseWheel',x:value.x,y:value.y,deltaX:sideways?-value.amount*120:0,deltaY:sideways?0:-value.amount*120,canScroll:true,modifiers:modifiers(value.mods||0)});break;
    }
    case 'key':{const code=keyCode(value.key);if(code)web.sendInputEvent({type:value.release?'keyUp':'keyDown',keyCode:code,modifiers:modifiers(value.mods||0)});break;}
    case 'char':if(Number.isInteger(value.point)&&value.point>=32&&value.point<=0x10ffff&&!(value.point>=0xd800&&value.point<=0xdfff))web.sendInputEvent({type:'char',keyCode:String.fromCodePoint(value.point),modifiers:modifiers(value.mods||0)});break;
    case 'dialog':answerDialog(!!value.accept,limited(value.text,500));break;
  }}catch(error){console.error(error);event('error',{message:'Browser input could not be processed.'});}
}
async function shutdown(){
  if(closing)return;closing=true;clearInterval(mediaTimer);answerDialog(false);save();for(const tab of tabs)if(tab.win&&!tab.win.isDestroyed()){tab.win.webContents.setAudioMuted(true);tab.win.webContents.stopPainting();}
  try{await Promise.race([(async()=>{const store=session.defaultSession;await saveSessionCookies(store);await store.cookies.flushStore();store.flushStorageData();})(),new Promise(resolve=>setTimeout(resolve,4000))]);}catch{}
  for(const tab of tabs)if(tab.win)tab.win.destroy();if(socket&&!socket.destroyed)socket.end();app.quit();
}
app.on('window-all-closed',()=>{});app.on('before-quit',e=>{if(!closing){e.preventDefault();shutdown();}});
process.on('uncaughtException',error=>{console.error(error);shutdown();});
app.whenReady().then(async()=>{
  socket=net.createConnection({host:'127.0.0.1',port});socket.setNoDelay(true);
  await new Promise((resolve,reject)=>{socket.once('connect',resolve);socket.once('error',reject);});
  const magic=Buffer.alloc(4);magic.writeInt32BE(0x43454d42);socket.write(Buffer.concat([encodeUTF(token),magic]));
  socket.on('error',shutdown);socket.on('close',shutdown);const parser=new Input(input,()=>shutdown());socket.on('data',chunk=>parser.accept(chunk));
  event('notice',{message:'Preparing protected playback...'});
  await components.whenReady([components.WIDEVINE_CDM_ID]);
  if(closing)return;const store=session.defaultSession;
  await restoreSessionCookies(store);store.cookies.on('changed',()=>{if(!closing){clearTimeout(cookieSaveTimer);cookieSaveTimer=setTimeout(()=>saveSessionCookies(store),150);}});
  const permitted=new Set(['mediaKeySystem','persistent-storage','storage-access','top-level-storage-access','fullscreen']);
  store.setPermissionRequestHandler((_web,permission,callback)=>callback(permitted.has(permission)));
  store.setPermissionCheckHandler((_web,permission)=>permitted.has(permission));
  store.on('will-download',e=>{e.preventDefault();event('notice',{message:'Website file downloads are not enabled.'});});
  restore();activate(selected);mediaTimer=setInterval(mediaState,500);event('ready');state();mediaState();
}).catch(error=>{console.error(error);event('error',{message:'Protected playback could not start. Try again or use the regular browser.'});setTimeout(shutdown,1500);});
