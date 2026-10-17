'use strict';
if(new URLSearchParams(location.search).get('panel')==='1')document.documentElement.classList.add('quick-panel');
(()=>{const waiting=new Map();let count=0;
window.CasaReply=(id,ok,value)=>{const item=waiting.get(id);if(!item)return;clearTimeout(item.timeout);waiting.delete(id);item.resolve({ok,status:ok?200:400,json:async()=>value});};
window.fetch=async(url,options={})=>{if(typeof url!=='string'||!/^\/api\/[a-z-]+$/.test(url))throw Error('Endereço não permitido.');const id='r'+(++count)+'_'+Date.now();return new Promise((resolve,reject)=>{const timeout=setTimeout(()=>{waiting.delete(id);reject(Error('O Android não respondeu. Tente novamente.'));},url==='/api/choose-photo'||url==='/api/add-tile'?180000:90000);waiting.set(id,{resolve,reject,timeout});try{CasaNative.call(id,url.slice(5),options.body||'{}');}catch(e){clearTimeout(timeout);waiting.delete(id);reject(e);}});};
})();
