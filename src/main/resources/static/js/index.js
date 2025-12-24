let files;

const fileTypeToIcon = {
    'folder': 'wenjianjia.png',
    'video': 'shipin.png',
    'image': 'tupian.png',
    'audio': 'yinpin.png',
    'archive': 'yasuobao.png'
};

// 当前选中的文件ID
let selectedFiles = new Set();
let currentPreviewFileId = null;
let keyword = "";

let moveMode = false;           // 是否处于“移动模式”
let moveFiles = [];             // 被移动的文件对象数组
let moveSourcePath = "";        // 源路径（进入移动模式时的 path）

// 初始化文件列表
function renderFileList() {
    const fileListContainer = document.getElementById('fileList');
    fileListContainer.innerHTML = '';

    files.forEach(file => {
        const isSelected = selectedFiles.has(file.id);
        const fileRow = document.createElement('div');
        fileRow.className = `file-list-item p-1 ${isSelected ? 'selected' : ''}`;
        fileRow.dataset.id = file.id;

        // 根据文件类型确定图标
        const iconFile = fileTypeToIcon[file.type] || 'weizhi.png';
        const iconPath = `/ico/${iconFile}`;
        let nameHtml = ""
        if (file.type === "folder") {
            nameHtml = `<div class="file-name file-folder" onclick="changeFolder('${file.path}')">${file.name}</div>`
        } else {
            nameHtml = `<div class="file-name">${file.name}</div>`
        }

        let ic=`   <img src="${iconPath}" alt="${file.type}" class="file-icon-img">`;
        if(file.type==="image"){
            ic= `<img src="${window.location.origin}/file/preview${file.path}" alt="${file.type}" class="file-icon-img">`;
        }
        fileRow.innerHTML = `
                  <div class="row align-items-center">
                    <div class="col-2 col-md-1">
                        <div class="form-check">
                            <input class="form-check-input file-checkbox" type="checkbox" data-id="${file.id}" ${isSelected ? 'checked' : ''}>
                        </div>
                    </div>

                    <div class="col-7 col-md-4">
                        <div class="file-icon-container">
                        
                         ${ic}
                            <div class="file-name-container">
                                ${nameHtml}
                                ${file.type === 'folder' ? `<div class="file-info">${file.items} 个项目</div>` : ''}
                            </div>
                        </div>
                    </div>

                    <div class="col-1 d-none d-md-block file-date">${file.originType}</div>
                    <div class="col-3 d-none d-md-block file-date">${file.date}</div>
                    <div class="col-2 d-none d-md-block file-size">${file.size}</div>
                    <div class="col-3 col-md-1 text-end">
                        <div class="dropdown">
                            <button class="btn btn-sm btn-outline-secondary border-0" type="button" data-bs-toggle="dropdown">
                                <i class="bi bi-three-dots-vertical"></i>
                            </button>
                            <ul class="dropdown-menu">
                                <li><a class="dropdown-item" href="#" data-action="download" data-id="${file.id}"><i class="bi bi-download me-2"></i>下载</a></li>
                                <li><a class="dropdown-item" href="#" data-action="rename" data-id="${file.id}"><i class="bi bi-pencil me-2"></i>重命名</a></li>
                            </ul>
                        </div>
                    </div>
                </div>
                `;

        fileListContainer.appendChild(fileRow);
    });

    updateActionButtons();
}

// 更新操作按钮状态
function updateActionButtons() {
    const hasSelection = selectedFiles.size > 0;
    document.getElementById('moveBtn').disabled = moveMode ? false : !hasSelection;
    document.getElementById('downloadBtn').disabled = !hasSelection;
    document.getElementById('deleteBtn').disabled = !hasSelection;

    // 更新全选复选框
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    selectAllCheckbox.checked = selectedFiles.size === files.length && files.length > 0;
    selectAllCheckbox.indeterminate = selectedFiles.size > 0 && selectedFiles.size < files.length;
}

// 预览文件
function previewFile(fileId) {
    const file = files.find(f => f.id === fileId);
    if (!file) return;

    currentPreviewFileId = fileId;
    document.getElementById('defaultPreview').style.display = 'none';
    const previewContainer = document.getElementById('filePreview');
    previewContainer.style.display = 'block';

    // 根据文件类型确定图标
    const iconFile = fileTypeToIcon[file.type] || 'weizhi.png';
    const iconPath = `/ico/${iconFile}`;

    // 根据文件类型显示不同的预览
    let previewContent = '';

    if (file.type === 'folder') {
        previewContent = `
                    <div class="text-center">
                        <div class="mb-3" style="width: 80px; height: 80px; margin: 0 auto;">
                            <img src="${iconPath}" alt="文件夹" style="width: 100%; height: 100%; object-fit: contain;">
                        </div>
                        <h3>${file.name}</h3>
                        <p class="text-muted">文件夹</p>
                        <div class="mt-4">
                            <p><i class="bi bi-calendar me-2"></i>创建日期: ${file.date}</p>
                            <p><i class="bi bi-files me-2"></i>包含 ${file.items} 个项目</p>
                        </div>
                    </div>
                `;
    } else if (file.type === 'image') {
        previewContent = `
                    <div>
                        <h5>${file.name}</h5>
                        <div class="text-center">
                            <img src="${window.location.origin}/file/preview${file.path}" alt="图片文件">
                        </div>
                        <div class="row mt-4">
                            <div class="col-md-12">
                                <ul class="list-unstyled">
                                    <li><strong>大小:</strong> ${file.size}</li>
                                    <li><strong>修改日期:</strong> ${file.date}</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                `;
    } else if (file.type === 'video') {
        previewContent = `
                    <div>
                        <h5 >${file.name}</h5>
                        <div class="border rounded text-center ">
                            <video src="${window.location.origin}/file/preview${file.path}" controls style="width: 100%">
                            <p class="text-muted">大小: ${file.size}</p>
                        </div>
                             <div class="row mt-4">
                            <div class="col-md-12">
                                <ul class="list-unstyled">
                                    <li><strong>大小:</strong> ${file.size}</li>
                                    <li><strong>修改日期:</strong> ${file.date}</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                `;
    } else if (file.type === 'audio') {
        previewContent = `
                    <div>
                        <h5 class="mb-3">${file.name}</h5>
                        <div class="border rounded p-4 text-center mb-3">
                            <audio src="${window.location.origin}/file/preview${file.path}" controls style="width: 100%">
                            <p class="text-muted">大小: ${file.size}</p>
                        </div>
                             <div class="row mt-4">
                            <div class="col-md-12">
                                <ul class="list-unstyled">
                                    <li><strong>大小:</strong> ${file.size}</li>
                                    <li><strong>修改日期:</strong> ${file.date}</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                `;
    } else if (file.type === 'archive') {
        previewContent = `
                    <div>
                        <h4 class="mb-3">${file.name}</h4>
                        <div class="border rounded p-4 text-center mb-3">
                            <img src="${iconPath}" alt="压缩文件" style="width: 80px; height: 80px;">
                            <p class="mt-3">压缩文件预览</p>
                            <p class="text-muted">大小: ${file.size}</p>
                        </div>
                    </div>
                `;
    } else if (file.type === 'doc') {
        previewContent = `
                    <div>
                        <div class="row mt-4">
                            <div class="col-md-6">
                                <h6>文档信息</h6>
                                <ul class="list-unstyled">
                                    <li><strong>类型:</strong> Word 文档</li>
                                    <li><strong>大小:</strong> ${file.size}</li>
                                    <li><strong>修改日期:</strong> ${file.date}</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                `;
    } else {
        // 默认预览
        previewContent = `
                    <div class="text-center">
                        <div class="mb-3" style="width: 80px; height: 80px; margin: 0 auto;">
                            <img src="${iconPath}" alt="${file.type === 'folder' ? '文件夹' : '文件'}" style="width: 100%; height: 100%; object-fit: contain;">
                        </div>
                        <h3>${file.name}</h3>
                        <p class="text-muted">${file.type === 'folder' ? '文件夹' : '文件'}</p>
                        <div class="mt-4">
                            <p><i class="bi bi-hdd me-2"></i>大小: ${file.size}</p>
                            <p><i class="bi bi-calendar me-2"></i>修改日期: ${file.date}</p>
                        </div>
                    </div>
                `;
    }

    previewContainer.innerHTML = previewContent;

    // 高亮选中的文件
    document.querySelectorAll('.file-list-item').forEach(item => {
        item.classList.remove('selected');
        if (parseInt(item.dataset.id) === fileId) {
            item.classList.add('selected');
        }
    });
}

// 初始化事件监听
function initEventListeners() {
    // 文件选择
    document.getElementById('fileList').addEventListener('click', function (e) {
        const fileItem = e.target.closest('.file-list-item');
        if (!fileItem) return;

        const fileId = fileItem.dataset.id;

        // 如果点击的是复选框，只处理选择/取消选择
        if (e.target.classList.contains('file-checkbox')) {
            if (e.target.checked) {
                selectedFiles.add(fileId);
            } else {
                selectedFiles.delete(fileId);
            }
            renderFileList();
            return;
        }

        // 如果点击的是其他部分，预览文件
        previewFile(fileId);
    });

    // 全选复选框
    document.getElementById('selectAllCheckbox').addEventListener('change', function (e) {
        if (e.target.checked) {
            files.forEach(file => selectedFiles.add(file.id));
        } else {
            selectedFiles.clear();
        }
        renderFileList();
    });


    // 操作按钮
    document.getElementById('newFolderBtn').addEventListener('click', function () {
        const modal = new bootstrap.Modal(document.getElementById('newFolderModal'));
        modal.show();
    });

    document.getElementById('createFolderBtn').addEventListener('click', function () {
        const folderName = document.getElementById('folderName').value;
        if (folderName.trim()) {
            fetch(`/file/createFolder?path=${currentPath}&name=${folderName}`)
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        loadFiles();
                    } else {
                        alert(data.message);
                    }
                })
            bootstrap.Modal.getInstance(document.getElementById('newFolderModal')).hide();
            document.getElementById('folderName').value = '';
        }
    });

    document.getElementById('moveBtn').addEventListener('click', function () {
        if (moveMode) {
            // 点击移动到此处
            if (currentPath === moveSourcePath) {
                alert('不能移动到原位置');
                return;
            }
            // 弹出确认模态
            const moveFileList = document.getElementById('moveFileList');
            moveFileList.innerHTML = '';
            moveFiles.forEach(file => {
                const item = document.createElement('div');
                item.className = 'list-group-item';
                item.textContent = `${file.name} - ${file.size}`;
                moveFileList.appendChild(item);
            });
            const modal = new bootstrap.Modal(document.getElementById('moveConfirmModal'));
            modal.show();
        } else {
            // 进入移动模式
            if (selectedFiles.size === 0) return;
            moveFiles = files.filter(f => selectedFiles.has(f.id));
            moveSourcePath = currentPath;
            moveMode = true;
            selectedFiles.clear();
            renderFileList();
            // 改变按钮
            const moveBtn = document.getElementById('moveBtn');
            moveBtn.innerHTML = '<i class="bi bi-arrow-right-circle me-1"></i>移动到此处';
            // 添加取消按钮
            const cancelBtn = document.createElement('button');
            cancelBtn.type = 'button';
            cancelBtn.className = 'btn btn-secondary btn-file-action ms-2';
            cancelBtn.id = 'cancelMoveBtn';
            cancelBtn.innerHTML = '<i class="bi bi-x-circle me-1"></i>取消移动';
            moveBtn.after(cancelBtn);
            // 取消监听
            cancelBtn.addEventListener('click', exitMoveMode);
        }
    });

    document.getElementById('startMoveBtn').addEventListener('click', async function () {
        const modal = bootstrap.Modal.getInstance(document.getElementById('moveConfirmModal'));
        modal.hide();
        const overwrite = document.getElementById('overwriteCheck').checked;
        const request = {
            sourcePath: moveSourcePath,
            targetPath: currentPath,
            names: moveFiles.map(f => f.name),
            overwrite: overwrite
        };
        try {
            const response = await fetch('/file/move', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(request)
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const r = await response.json();
            if (!r.success) {
                alert(r.message || '移动失败');
                return;
            }
            handleMoveResult(r.data);
        } catch (e) {
            alert('移动失败');
        }
    });

    function handleMoveResult(initialResult) {
        if (initialResult.conflicted && initialResult.conflicted.length > 0) {
            // 显示冲突模态
            const conflictList = document.getElementById('conflictList');
            conflictList.innerHTML = '';
            initialResult.conflicted.forEach(name => {
                const item = document.createElement('div');
                item.className = 'list-group-item d-flex justify-content-between align-items-center';
                item.innerHTML = `
                        <span>${name}</span>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" data-name="${name}" checked>
                            <label class="form-check-label">覆盖</label>
                        </div>
                    `;
                conflictList.appendChild(item);
            });
            const modal = new bootstrap.Modal(document.getElementById('conflictModal'));
            modal.show();
            // 监听确认
            const confirmBtn = document.getElementById('confirmConflictBtn');
            const listener = async function () {
                const toOverwrite = [];
                conflictList.querySelectorAll('input:checked').forEach(input => {
                    toOverwrite.push(input.dataset.name);
                });
                modal.hide();
                confirmBtn.removeEventListener('click', listener);
                if (toOverwrite.length > 0) {
                    const overwriteRequest = {
                        sourcePath: moveSourcePath,
                        targetPath: currentPath,
                        names: toOverwrite,
                        overwrite: true
                    };
                    try {
                        const overwriteResponse = await fetch('/file/move', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify(overwriteRequest)
                        });
                        if (!overwriteResponse.ok) {
                            throw new Error(`HTTP error! status: ${overwriteResponse.status}`);
                        }
                        const overwriteR = await overwriteResponse.json();
                        if (!overwriteR.success) {
                            alert(overwriteR.message || '覆盖失败');
                            return;
                        }
                        const overwriteResult = overwriteR.data;
                        // 合并结果
                        const finalResult = {
                            success: [...initialResult.success, ...overwriteResult.success],
                            skipped: [...initialResult.skipped, ...overwriteResult.skipped, ...(initialResult.conflicted.filter(n => !toOverwrite.includes(n)))],
                            conflicted: []
                        };
                        showMoveResult(finalResult);
                    } catch (e) {
                        alert('覆盖失败');
                    }
                } else {
                    // all skipped
                    const finalResult = {
                        success: initialResult.success,
                        skipped: [...initialResult.skipped, ...initialResult.conflicted],
                        conflicted: []
                    };
                    showMoveResult(finalResult);
                }
            };
            confirmBtn.addEventListener('click', listener);
        } else {
            showMoveResult(initialResult);
        }
    }

    function showMoveResult(result) {
        const moveResultList = document.getElementById('moveResultList');
        moveResultList.innerHTML = '';
        result.success.forEach(name => {
            const item = document.createElement('div');
            item.className = 'list-group-item list-group-item-success';
            item.textContent = `${name} - 移动成功`;
            moveResultList.appendChild(item);
        });
        result.skipped.forEach(name => {
            const item = document.createElement('div');
            item.className = 'list-group-item list-group-item-warning';
            item.textContent = `${name} - 已跳过`;
            moveResultList.appendChild(item);
        });
        const modal = new bootstrap.Modal(document.getElementById('moveResultModal'));
        modal.show();
        // 退出移动模式
        exitMoveMode();
        selectedFiles.clear();
        loadFiles();
    }

    function exitMoveMode() {
        moveMode = false;
        moveFiles = [];
        moveSourcePath = '';
        const moveBtn = document.getElementById('moveBtn');
        moveBtn.innerHTML = '<i class="bi bi-arrow-right-circle me-1"></i>移动';
        const cancelBtn = document.getElementById('cancelMoveBtn');
        if (cancelBtn) cancelBtn.remove();
    }

    document.getElementById('downloadBtn').addEventListener('click', function () {
        if (selectedFiles.size > 0) {
            const fs = Array.from(selectedFiles).map(id => {
                return files.find(f => f.id === id);
            });
            let paths = []
            for (let i = 0; i < fs.length; i++) {
                paths.push(fs[i].path)
            }

            const query = paths.map(p => 'path=' + encodeURIComponent(p)).join('&');

            window.location.href = '/file/download?' + query;

        }
    });

    document.getElementById('deleteBtn').addEventListener('click', async function () {
        if (selectedFiles.size === 0) return;

        if (!confirm(`确定要删除 ${selectedFiles.size} 个选中的文件吗？`)) {
            return;
        }

        // 收集 path（后端只需要 path）
        const paths = Array.from(selectedFiles).map(id => {
            const f = files.find(file => file.id === id);
            return f.path;
        });

        try {
            const res = await fetch('/file/recycle', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(paths)
            });

            const r = await res.json();
            if (!r.success) {
                alert(r.message || '删除失败');
                return;
            }

            alert(`成功回收 ${r.data.success.length} 个文件`);
            selectedFiles.clear();
            loadFiles();

            document.getElementById('defaultPreview').style.display = 'block';
            document.getElementById('filePreview').style.display = 'none';

        } catch (e) {
            alert('删除失败');
        }
    });


    // 全选/取消全选按钮
    document.getElementById('selectAllBtn').addEventListener('click', function () {
        files.forEach(file => selectedFiles.add(file.id));
        renderFileList();
    });
    document.getElementById('clearUploadBtn').addEventListener('click', () => {
        if (uploadTasks.length === 0) return;

        if (confirm('确定要清空所有待上传的文件吗？')) {
            // 取消所有正在上传的任务
            uploadTasks.forEach(task => {
                task.cancelled = true;
                if (task.controller) task.controller.abort();
            });

            // 清空任务数组和 DOM
            uploadTasks = [];
            document.getElementById('selectedFiles').innerHTML = '';
        }
    });
    document.getElementById('deselectAllBtn').addEventListener('click', function () {
        selectedFiles.clear();
        renderFileList();
    });





    /* ===================== 基础配置 ===================== */
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const selectedFilesContainer = document.getElementById('selectedFiles');
    const startUploadBtn = document.getElementById('startUploadBtn');
    const CHUNK_SIZE = isMobile() ? 2 * 1024 * 1024 : 5 * 1024 * 1024;

    function isMobile() {
        return /Android|iPhone|iPad/i.test(navigator.userAgent);
    }

    let uploadTasks = [];

    /* ===================== 工具 ===================== */
    function generateFileId() {
        if (crypto && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        // 兼容旧手机 / 微信
        return 'f_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
    }

    /* ===================== 拖拽 / 选择 ===================== */
    dropZone.onclick = (e) => {
        if (e.target === fileInput) return;
        fileInput.value = '';  // 先清空，确保能重复选同一文件
        fileInput.click();
    };

    dropZone.ondragover = e => {
        e.preventDefault();
        dropZone.classList.add('border-primary', 'bg-light');
    };

    dropZone.ondragleave = () => {
        dropZone.classList.remove('border-primary', 'bg-light');
    };

    dropZone.ondrop = e => {
        e.preventDefault();
        dropZone.classList.remove('border-primary', 'bg-light');
        handleFiles(e.dataTransfer.files);
    };

    fileInput.onchange = e => {
        if (e.target.files.length > 0) {
            handleFiles(e.target.files);
        }
    };

    /* ===================== 处理文件 ===================== */
    function handleFiles(fileList) {
        if (uploadTasks.length === 0) {
            selectedFilesContainer.innerHTML = '<h6>已选择的文件:</h6>';
        }

        Array.from(fileList).forEach(file => {
            const id = generateFileId();

            const task = {
                id,
                file,
                controller: null,
                cancelled: false
            };
            uploadTasks.push(task);

            const item = document.createElement('div');
            item.className = 'd-flex flex-column border-bottom py-2';
            item.dataset.id = id;

            item.innerHTML = `
          <div class="d-flex justify-content-between align-items-center">
            <div>
              <i class="bi bi-file-earmark me-2"></i>${file.name}
            </div>
            <div>
              <span class="text-muted me-3">${formatFileSize(file.size)}</span>
              <button class="btn btn-sm btn-outline-danger remove-btn">&times;</button>
            </div>
          </div>
          <div class="progress mt-2" style="height:8px; display:none;">
            <div class="progress-bar"></div>
          </div>
          <small class="upload-status text-muted"></small>
        `;

            selectedFilesContainer.appendChild(item);

            /* ===== 点击 × 取消 ===== */
            item.querySelector('.remove-btn').onclick = async () => {
                task.cancelled = true;
                if (task.controller) task.controller.abort();

                const md5 = await calculateMD5(file);
                await cancelUpload(md5, file.name);

                item.remove();
            };
        });
    }

    /* ===================== 开始上传 ===================== */
    startUploadBtn.onclick = async () => {
        for (const task of uploadTasks) {
            if (task.cancelled) continue;

            const file = task.file;
            const item = selectedFilesContainer.querySelector(`[data-id="${task.id}"]`);
            if (!item) continue;

            const progressBox = item.querySelector('.progress');
            const progressBar = item.querySelector('.progress-bar');
            const status = item.querySelector('.upload-status');

            progressBox.style.display = 'block';
            status.textContent = '准备上传...';

            const controller = new AbortController();
            task.controller = controller;

            try {
                const md5 = await calculateMD5(file);

                // 秒传
                const check = await checkFileExists(md5, file.name);
                if (check.exists) {
                    progressBar.style.width = '100%';
                    status.textContent = '文件已存在';
                    continue;
                }

                const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
                const uploadedChunks = await getUploadedChunks(md5, file.name);

                let uploadedSize = 0;

                for (let i = 0; i < totalChunks; i++) {
                    if (task.cancelled) throw new Error('cancelled');

                    if (uploadedChunks.includes(i)) {
                        uploadedSize += Math.min(CHUNK_SIZE, file.size - i * CHUNK_SIZE);
                        progressBar.style.width = `${uploadedSize / file.size * 100}%`;
                        continue;
                    }

                    const start = i * CHUNK_SIZE;
                    const end = Math.min(start + CHUNK_SIZE, file.size);
                    const chunk = file.slice(start, end);

                    status.textContent = `上传分片 ${i + 1}/${totalChunks}`;

                    await uploadChunk(
                        chunk, i, totalChunks, md5, file.name, controller
                    );

                    uploadedSize += (end - start);
                    progressBar.style.width = `${uploadedSize / file.size * 100}%`;
                }

                await mergeChunks(md5, file.name, totalChunks);
                status.textContent = '上传完成';
                loadFiles()

            } catch (e) {
                if (e.name === 'AbortError' || task.cancelled) {
                    status.textContent = '已取消';
                } else {
                    status.textContent = '上传失败';
                }
            }
        }
    };

    /* ===================== MD5 ===================== */
    function calculateMD5(file) {
        return new Promise((resolve, reject) => {
            const spark = new SparkMD5.ArrayBuffer();
            const reader = new FileReader();
            const chunks = Math.ceil(file.size / CHUNK_SIZE);
            let current = 0;

            reader.onload = e => {
                spark.append(e.target.result);
                current++;
                current < chunks ? load() : resolve(spark.end());
            };
            reader.onerror = reject;

            function load() {
                reader.readAsArrayBuffer(
                    file.slice(current * CHUNK_SIZE, Math.min((current + 1) * CHUNK_SIZE, file.size))
                );
            }

            load();
        });
    }

    /* ===================== 接口 ===================== */
    const checkFileExists = (md5, fileName) =>
        fetch(`/file/check?md5=${md5}&fileName=${encodeURIComponent(fileName)}&path=${currentPath}`).then(r => r.json());

    const getUploadedChunks = (md5, fileName) =>
        fetch(`/file/uploaded-chunks?md5=${md5}&fileName=${encodeURIComponent(fileName)}`)
            .then(r => r.json()).then(r => r.uploadedChunks || []);

    function uploadChunk(chunk, chunkIndex, totalChunks, md5, fileName, controller) {
        const fd = new FormData();
        fd.append('chunk', chunk);
        fd.append('chunkIndex', chunkIndex);
        fd.append('totalChunks', totalChunks);
        fd.append('md5', md5);
        fd.append('fileName', fileName);

        return fetch('/file/upload-chunk', {
            method: 'POST',
            body: fd,
            signal: controller.signal
        }).then(r => {
            if (!r.ok) throw new Error();
        });
    }

    const mergeChunks = (md5, fileName, totalChunks) =>
        fetch('/file/merge-chunks', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({md5, fileName, totalChunks, currentPath})
        });

    const cancelUpload = (md5, fileName) =>
        fetch('/file/cancel-upload', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({md5, fileName})
        });


    // 处理文件操作下拉菜单
    document.addEventListener('click', function (e) {
        if (e.target.closest('[data-action]')) {
            e.preventDefault();
            const action = e.target.closest('[data-action]').dataset.action;
            const fileId = e.target.closest('[data-action]').dataset.id;

            switch (action) {
                case 'download':
                    window.location.href = `/file/download?path=${files.find(f => f.id === fileId).path}`;
                    break;
                case 'rename':
                    let oldName = files.find(f => f.id === fileId).name;
                    const newName = prompt('请输入新文件名:',);
                    if (newName && newName.trim()) {
                        fetch(`/file/rename?path=${currentPath}&name=${oldName}&newName=${newName}`)
                            .then(r => r.json())
                            .then(r => {
                                if (r.success) {
                                    alert(r.msg)
                                    loadFiles()
                                } else {
                                    alert(r.msg)
                                }
                            })
                    }
                    break;
                case 'move':
                    alert(`移动文件 ID: ${fileId}`);
                    break;
                case 'delete':
                    if (confirm('确定要删除这个文件吗?')) {
                        const index = files.findIndex(f => f.id === fileId);
                        if (index > -1) {
                            files.splice(index, 1);
                            selectedFiles.delete(fileId);
                            renderFileList();
                            document.getElementById('defaultPreview').style.display = 'block';
                            document.getElementById('filePreview').style.display = 'none';
                        }
                    }
                    break;
            }
        }
    });
}

function changeFolder(path) {
    currentPath = path;
    loadFiles();
}

function changePath(i) {
    if (i === 0) {
        currentPath = "/";
        loadFiles();
        return;
    }
    let parts = currentPath.split("/").filter(p => p !== "");
    let newParts = parts.slice(0, i);
    currentPath = "/" + newParts.join("/") + "/";
    loadFiles();
}

function loadPath() {
    let $p = $("#path-menu");
    let paths = currentPath.split("/").filter(p => p !== "");
    let h = ``;
    if (paths.length === 0) {
        h += `<li class="breadcrumb-item"><i class="bi bi-house-door"></i> 根目录</li>`;
    } else {
        h += `<li class="breadcrumb-item"><a href="#" onclick="changePath(0)"><i class="bi bi-house-door"></i> 根目录</a></li>`;
    }
    for (let i = 0; i < paths.length; i++) {
        if (i !== paths.length - 1) {
            h += `<li class="breadcrumb-item"><a href="#"  onclick="changePath(${i + 1})">${paths[i]}</a></li>`
        } else {
            h += `<li class="breadcrumb-item active" aria-current="page">${paths[i]}</li>`
        }
    }
    $p.empty();
    $p.append(h);
}


let currentPath = "/"
let sortType = "date";


function goSearch(){

    keyword =$("#keyword").val();
    loadFiles();
    keyword="";

}

function loadFiles(st) {
    if (st != undefined && st != null) {
        sortType = st;
    }
    fetch(`/file/list?path=${currentPath}&sort=${sortType}&keyword=${keyword}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(r => r.json())
        .then(r => {
            files = r.fileItemVOList;
            currentPath = r.currentPath;
            renderFileList()
            loadPath()
        });
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
    loadFiles();
    initEventListeners();
});



