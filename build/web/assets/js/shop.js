(() => {
 'use strict';
 document.querySelectorAll('[data-copy]').forEach(button=>button.addEventListener('click',async()=>{
  const text=document.getElementById(button.dataset.copy).textContent.trim();
  try{await navigator.clipboard.writeText(text);button.textContent='Đã sao chép';setTimeout(()=>{button.textContent='Sao chép';},1800);}catch(_){button.textContent='Chọn và sao chép nội dung';}
 }));
 const qr=document.querySelector('.vietqr-image');if(qr){const fail=()=>{qr.hidden=true;document.querySelector('.qr-fallback').hidden=false;};qr.addEventListener('error',fail,{once:true});if(qr.dataset.payload){try{const code=qrcode(0,'M');code.addData(qr.dataset.payload);code.make();qr.src=code.createDataURL(6,24);}catch(_){fail();}}else{fail();}}
 const form=document.querySelector('#place-order-form');if(form){form.addEventListener('submit',()=>{form.querySelector('[type="submit"]').disabled=true;form.querySelector('[type="submit"]').textContent='Đang tạo đơn…';});window.addEventListener('pageshow',()=>{form.querySelector('[type="submit"]').disabled=false;form.querySelector('[type="submit"]').textContent='Tạo đơn & lấy mã VietQR';});}
 const receiptInput=document.querySelector('#receipt-file');
 if(receiptInput){
  let previewUrl=null;const button=document.querySelector('#receipt-submit');const error=document.querySelector('#receipt-file-error');const preview=document.querySelector('#receipt-preview');
  receiptInput.addEventListener('change',()=>{
   if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}preview.hidden=true;error.hidden=true;button.disabled=true;
   const file=receiptInput.files[0];if(!file)return;
   if(!['image/png','image/jpeg'].includes(file.type)||file.size>5*1024*1024){error.textContent='Chọn ảnh PNG hoặc JPG tối đa 5 MB.';error.hidden=false;receiptInput.value='';return;}
   previewUrl=URL.createObjectURL(file);document.querySelector('#receipt-preview-image').src=previewUrl;document.querySelector('#receipt-file-name').textContent=file.name;preview.hidden=false;button.disabled=false;
  });
  document.querySelector('#receipt-form').addEventListener('submit',()=>{button.disabled=true;button.textContent='Đang gửi ảnh…';});
  window.addEventListener('pageshow',()=>{button.disabled=!receiptInput.files.length;button.textContent='Gửi ảnh chuyển khoản';});
  window.addEventListener('pagehide',()=>{if(previewUrl)URL.revokeObjectURL(previewUrl);});
 }
})();
