'use strict';
const {contextBridge,ipcRenderer}=require('electron');
// Expose only the three bounded website-dialog operations, never raw IPC or Node APIs.
contextBridge.exposeInMainWorld('cemBrowserDialog',(kind,message,initial)=>{
 if(!['alert','confirm','prompt'].includes(kind))return {accept:false,text:null};
 return ipcRenderer.sendSync('cem-browser-dialog',kind,String(message??'').slice(0,1000),String(initial??'').slice(0,500));
});
contextBridge.executeInMainWorld({func:()=>{
 const request=window.cemBrowserDialog;
 window.alert=message=>{request('alert',message,'');};
 window.confirm=message=>!!request('confirm',message,'')?.accept;
 window.prompt=(message,initial='')=>{const result=request('prompt',message,initial);return result?.accept?result.text:null;};
}});
