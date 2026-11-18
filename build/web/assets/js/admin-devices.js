(() => {
    'use strict';
    const rows = [...document.querySelectorAll('.device-row')];
    const search = document.querySelector('#device-search');
    const category = document.querySelector('#device-category');
    const brand = document.querySelector('#device-brand');
    const status = document.querySelector('#device-status');
    const pageCheckbox = document.querySelector('#select-device-page');
    const selected = new Set();
    const knownBrands = ['Sony','Canon','Fujifilm','Nikon','Panasonic','DJI','Sigma','Tamron','Godox'];
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').toLowerCase();
    let page = 1, filtered = [], visible = [];
    const pageSize = 8;
    const brandNames = new Set();
    rows.forEach(row => {
        row.dataset.brand = knownBrands.find(value => row.dataset.name.toLowerCase().includes(value.toLowerCase())) || 'Khác';
        row.querySelector('.device-brand-label').textContent = row.dataset.brand;
        brandNames.add(row.dataset.brand);
    });
    [...brandNames].sort().forEach(name => brand.add(new Option(name,name)));
    function updateSelection() {
        rows.forEach(row => {
            const checked = selected.has(row.dataset.id);
            row.querySelector('.device-checkbox').checked = checked;
            row.classList.toggle('selected', checked);
        });
        document.querySelector('#selection-count').textContent = `Đã chọn: ${selected.size} thiết bị`;
        document.querySelectorAll('[data-bulk]').forEach(button => { button.disabled = selected.size === 0; });
        const checked = visible.filter(row => selected.has(row.dataset.id)).length;
        pageCheckbox.checked = visible.length > 0 && checked === visible.length;
        pageCheckbox.indeterminate = checked > 0 && checked < visible.length;
        pageCheckbox.disabled = visible.length === 0;
    }
    function render() {
        const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
        page = Math.min(pages, Math.max(1,page));
        visible = filtered.slice((page-1)*pageSize,page*pageSize);
        rows.forEach(row => { row.hidden = !visible.includes(row); });
        document.querySelector('#devices-empty').hidden = filtered.length > 0;
        document.querySelector('#device-page-label').textContent = filtered.length ? `Hiển thị ${(page-1)*pageSize+1}–${Math.min(page*pageSize,filtered.length)} trong ${filtered.length} thiết bị` : '0 thiết bị';
        document.querySelector('#device-page-number').textContent = `${page} / ${pages}`;
        document.querySelector('#device-previous').disabled = page === 1;
        document.querySelector('#device-next').disabled = page === pages;
        updateSelection();
    }
    function filter() {
        const term = normalize(search.value.trim());
        filtered = rows.filter(row => (!category.value || row.dataset.category === category.value)
            && (!brand.value || row.dataset.brand === brand.value)
            && (!status.value || row.dataset.status === status.value)
            && normalize(`${row.dataset.name} ${row.dataset.serial} DEV-${row.dataset.id} DEV-${row.dataset.id.padStart(4,'0')}`).includes(term));
        page=1;render();
    }
    search.addEventListener('input',filter);
    [category,brand,status].forEach(input => input.addEventListener('change',filter));
    document.querySelectorAll('[data-device-quick]').forEach(button => button.addEventListener('click',()=>{status.value=button.dataset.deviceQuick;filter();}));
    document.querySelector('#device-reset').addEventListener('click',()=>{search.value='';category.value='';brand.value='';status.value='';filter();});
    document.querySelector('#device-previous').addEventListener('click',()=>{page--;render();});
    document.querySelector('#device-next').addEventListener('click',()=>{page++;render();});
    pageCheckbox.addEventListener('change',()=>{visible.forEach(row=>{if(pageCheckbox.checked)selected.add(row.dataset.id);else selected.delete(row.dataset.id);});updateSelection();});
    rows.forEach(row=>row.querySelector('.device-checkbox').addEventListener('change',event=>{if(event.target.checked)selected.add(row.dataset.id);else selected.delete(row.dataset.id);updateSelection();}));
    const editor = document.querySelector('#device-editor');
    const editorProduct = document.querySelector('#editor-product');
    function openEditor(row = null) {
        editor.querySelector('.inventory-form').reset();
        document.querySelector('#device-editor-title').textContent = row ? 'Chỉnh sửa thiết bị' : 'Thêm thiết bị mới';
        document.querySelector('#editor-action').value = row ? 'edit' : 'create';
        document.querySelector('#editor-id').value = row ? row.dataset.id : '';
        editorProduct.value = row ? row.dataset.product : '';
        document.querySelector('#editor-serial').value = row ? row.dataset.serial : '';
        document.querySelector('#editor-status').value = row ? row.dataset.physical : 'available';
        editor.showModal();
    }
    document.querySelector('#add-device').addEventListener('click',()=>openEditor());
    document.querySelectorAll('[data-edit]').forEach(button=>button.addEventListener('click',()=>openEditor(rows.find(row=>row.dataset.id===button.dataset.edit))));
    const bulkDialog = document.querySelector('#device-bulk-dialog');
    function openBulk(action, ids) {
        const labels = {price:'Cập nhật giá thuê',status:'Chuyển trạng thái thiết bị',delete:'Xóa thiết bị đã chọn'};
        document.querySelector('#bulk-dialog-title').textContent=labels[action];
        document.querySelector('#bulk-selection-description').textContent=`Áp dụng cho ${ids.length} thiết bị đã chọn.`;
        document.querySelector('#bulk-action').value=action;
        const container=document.querySelector('#bulk-device-ids');container.replaceChildren();
        ids.forEach(id=>{const input=document.createElement('input');input.type='hidden';input.name='ids';input.value=id;container.append(input);});
        document.querySelector('#bulk-price-field').hidden=action!=='price';
        document.querySelector('#bulk-status-field').hidden=action!=='status';
        const priceInput=document.querySelector('#bulk-price');priceInput.required=action==='price';priceInput.disabled=action!=='price';priceInput.value='';
        document.querySelector('#bulk-status').disabled=action!=='status';
        document.querySelector('#bulk-warning').textContent=action==='price'?'Giá nằm ở cấp mẫu máy: tất cả thiết bị cùng mẫu với lựa chọn sẽ được áp dụng giá mới. Giá của các đơn thuê hiện có không thay đổi.':action==='delete'?'Chỉ xóa được thiết bị chưa có lịch sử thuê. Thao tác xóa sẽ loại bỏ thiết bị khỏi kho.':'Thiết bị có đơn chờ duyệt, đã duyệt hoặc đang thuê cần xử lý đơn trước khi đổi trạng thái.';
        const confirm=document.querySelector('#bulk-confirm');confirm.textContent=action==='delete'?'Xác nhận xóa thiết bị':'Lưu thay đổi';confirm.classList.toggle('danger-action',action==='delete');
        bulkDialog.showModal();
    }
    document.querySelectorAll('[data-bulk]').forEach(button=>button.addEventListener('click',()=>openBulk(button.dataset.bulk,[...selected])));
    document.querySelectorAll('[data-delete]').forEach(button=>button.addEventListener('click',()=>openBulk('delete',[button.dataset.delete])));
    document.querySelectorAll('.inventory-form').forEach(form=>form.addEventListener('submit',event=>{
        if(!form.reportValidity()){event.preventDefault();return;}
        form.querySelector('[type="submit"]').disabled=true;
    }));
    window.addEventListener('pageshow',()=>document.querySelectorAll('.inventory-form [type="submit"]').forEach(button=>{button.disabled=false;}));
    document.querySelectorAll('dialog').forEach(dialog=>dialog.addEventListener('click',event=>{
        const b=dialog.getBoundingClientRect();if(event.target===dialog&&(event.clientX<b.left||event.clientX>b.right||event.clientY<b.top||event.clientY>b.bottom))dialog.close();
    }));
    document.querySelector('#export-devices').addEventListener('click',()=>{
        const safe=value=>{let text=String(value??'');if(/^[=+\-@\t\r\n]/.test(text))text=`'${text}`;return `"${text.replace(/"/g,'""')}"`;};
        const lines=[['Mã thiết bị','Tên thiết bị','Serial','Hãng','Trạng thái','Giá thuê/ngày'],...filtered.map(row=>[`DEV-${row.dataset.id}`,row.dataset.name,row.dataset.serial,row.dataset.brand,row.querySelector('.device-status-badge').textContent,row.dataset.price])];
        const url=URL.createObjectURL(new Blob(['\uFEFF'+lines.map(line=>line.map(safe).join(',')).join('\r\n')],{type:'text/csv;charset=utf-8'}));
        const link=document.createElement('a');link.href=url;link.download='lenscam-thiet-bi.csv';document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
    });
    filter();
})();
