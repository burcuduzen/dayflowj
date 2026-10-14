const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('dayflowDiagnostics',{metrics:()=>ipcRenderer.invoke('dayflow:developer-metrics'),open:()=>ipcRenderer.invoke('dayflow:open-system-status')});
