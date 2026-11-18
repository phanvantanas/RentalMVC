(() => {
 'use strict';
 document.getElementById('refresh-my-rentals')?.addEventListener('click',()=>location.reload());
 const search=document.getElementById('my-order-search'),status=document.getElementById('my-order-status');
 if(!search||!status)return;
 const cards=[...document.querySelectorAll('.my-order-card')];
 const normalize=value=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').toLowerCase();
 function filter(){const term=normalize(search.value.trim());let count=0;for(const card of cards){card.hidden=Boolean(status.value&&card.dataset.orderStatus!==status.value)||!normalize(card.textContent).includes(term);if(!card.hidden)count++;}document.getElementById('my-order-count').textContent=`${count} đơn thuê`;document.getElementById('my-order-no-match').hidden=count>0;}
 search.addEventListener('input',filter);status.addEventListener('change',filter);
})();
