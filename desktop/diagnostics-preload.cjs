const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('dayflowDiagnostics',{metrics:()=>ipcRenderer.invoke('dayflow:developer-metrics')});
