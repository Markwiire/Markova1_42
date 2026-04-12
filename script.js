const SUPABASE_URL = 'https://rifzmuphaemtlmrijaqr.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJpZnptdXBoYWVtdGxtcmlqYXFyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIwNzc3MDAsImV4cCI6MjA3NzY1MzcwMH0.MA5qpZby_xlSAbwS70JfqbOGkRI04DZlb80MPRRP5Lc';

let currentUser = null;
let allPets = [];
let currentFilter = 'all';
let currentChatPet = null;
let currentChatId = null;
let isLoading = false;

function simpleEncrypt(password) {
    let encrypted = '';
    for (let i = 0; i < password.length; i++) encrypted += String.fromCharCode(password.charCodeAt(i) + 3);
    return encrypted;
}

function escapeHtml(str) { if (!str) return ''; return str.replace(/[&<>]/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[m])); }

function checkSession() {
    const saved = localStorage.getItem('paws_user');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            updateUIForUser();
        } catch(e) { localStorage.removeItem('paws_user'); }
    }
}

function updateUIForUser() {
    const authContainer = document.getElementById('authButtonContainer');
    const userPanelDiv = document.getElementById('userPanel');
    const adminChatsBtn = document.getElementById('adminChatsBtnHeader');
    const adminAddBtn = document.getElementById('adminAddPetBtn');
    
    if (currentUser) {
        authContainer.style.display = 'none';
        userPanelDiv.style.display = 'flex';
        document.getElementById('userName').textContent = currentUser.username;
        document.getElementById('userRole').textContent = currentUser.role === 'Manager' ? 'Менеджер' : 'Клиент';
        
        if (currentUser.role === 'Manager') {
            if (adminChatsBtn) adminChatsBtn.style.display = 'block';
            if (adminAddBtn) adminAddBtn.style.display = 'block';
        } else {
            if (adminChatsBtn) adminChatsBtn.style.display = 'none';
            if (adminAddBtn) adminAddBtn.style.display = 'none';
        }
    } else {
        authContainer.style.display = 'block';
        userPanelDiv.style.display = 'none';
    }
}

function saveSession(user) {
    currentUser = user;
    localStorage.setItem('paws_user', JSON.stringify(user));
    updateUIForUser();
}

function clearSession() {
    currentUser = null;
    localStorage.removeItem('paws_user');
    updateUIForUser();
    const modal = document.getElementById('authModal');
    if (modal) modal.classList.remove('active');
    closeChat();
    renderCatalog();
}

async function handleLogin(login, password) {
    const encrypted = simpleEncrypt(password);
    const response = await fetch(`${SUPABASE_URL}/rest/v1/users?username=eq.${encodeURIComponent(login)}&select=id,username,role,password`, {
        method: 'GET', headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
    });
    if (!response.ok) return { success: false, message: 'Ошибка соединения' };
    const data = await response.json();
    if (!data.length) return { success: false, message: 'Пользователь не найден' };
    const user = data[0];
    if (user.password === encrypted) {
        saveSession({ id: user.id, username: user.username, role: user.role });
        return { success: true };
    }
    return { success: false, message: 'Неверный пароль' };
}

async function handleRegister(login, password, repeat) {
    if (password !== repeat) return { success: false, message: 'Пароли не совпадают' };
    if (password.length < 8) return { success: false, message: 'Пароль должен быть не менее 8 символов' };
    const encrypted = simpleEncrypt(password);
    const checkResponse = await fetch(`${SUPABASE_URL}/rest/v1/users?username=eq.${encodeURIComponent(login)}&select=id`, {
        method: 'GET', headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
    });
    const existing = await checkResponse.json();
    if (existing.length) return { success: false, message: 'Логин уже занят' };
    const insertResponse = await fetch(`${SUPABASE_URL}/rest/v1/users`, {
        method: 'POST', headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json', 'Prefer': 'return=minimal' },
        body: JSON.stringify({ username: login, password: encrypted, role: 'Client' })
    });
    if (insertResponse.ok) return { success: true };
    return { success: false, message: 'Ошибка регистрации' };
}

async function loadPets() {
    if (isLoading) return;
    isLoading = true;
    
    const appDiv = document.getElementById('appContent');
    if (appDiv) appDiv.innerHTML = '<div class="container"><div class="loading">Загрузка питомцев...</div></div>';
    try {
        const response = await fetch(`${SUPABASE_URL}/rest/v1/pets?select=*`, {
            method: 'GET', headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
        });
        if (!response.ok) throw new Error();
        allPets = await response.json();
        renderCatalog();
    } catch (error) {
        if (appDiv) appDiv.innerHTML = `<div class="container"><div class="empty-catalog">Не удалось загрузить питомцев</div></div>`;
    } finally {
        isLoading = false;
    }
}

function getChipClass(pet, type, value) {
    if (type === 'type') return value === 'dog' ? 'chip-type-dog' : 'chip-type-cat';
    if (type === 'gender') return value === 'male' ? 'chip-gender-male' : 'chip-gender-female';
    if (type === 'size') {
        if (value === 'Маленький') return 'chip-size-small';
        if (value === 'Средний') return 'chip-size-medium';
        if (value === 'Большой') return 'chip-size-large';
    }
    return 'chip-age';
}

function renderCatalog() {
    let filtered = [...allPets];
    if (currentFilter === 'dog') filtered = filtered.filter(p => p.type === 'dog');
    else if (currentFilter === 'cat') filtered = filtered.filter(p => p.type === 'cat');
    else if (currentFilter === 'small') filtered = filtered.filter(p => p.size === 'Маленький');
    else if (currentFilter === 'medium') filtered = filtered.filter(p => p.size === 'Средний');
    else if (currentFilter === 'large') filtered = filtered.filter(p => p.size === 'Большой');

    const appDiv = document.getElementById('appContent');
    if (!appDiv) return;

    if (filtered.length === 0) {
        appDiv.innerHTML = `
            <div class="container">
                <div class="filters-section"><div class="filters-title">Фильтры</div><div class="chip-group" id="filterChips">
                    <div class="chip ${currentFilter === 'all' ? 'active' : ''}" data-filter="all">Все</div>
                    <div class="chip ${currentFilter === 'dog' ? 'active' : ''}" data-filter="dog">Собаки</div>
                    <div class="chip ${currentFilter === 'cat' ? 'active' : ''}" data-filter="cat">Кошки</div>
                    <div class="chip ${currentFilter === 'small' ? 'active' : ''}" data-filter="small">Маленькие</div>
                    <div class="chip ${currentFilter === 'medium' ? 'active' : ''}" data-filter="medium">Средние</div>
                    <div class="chip ${currentFilter === 'large' ? 'active' : ''}" data-filter="large">Большие</div>
                </div></div><div class="empty-catalog">По таким критериям никого не найдено</div>
            </div>`;
        attachFilterEvents();
        return;
    }

    let cardsHtml = '';
    const isManager = currentUser && currentUser.role === 'Manager';
    
    for (let pet of filtered) {
        let imageUrl = pet.image_url;
        if (imageUrl && !imageUrl.startsWith('http')) imageUrl = `${SUPABASE_URL}/storage/v1/object/public/pets_images/${imageUrl}`;
        const typeLabel = pet.type === 'dog' ? 'Собака' : 'Кошка';
        const genderLabel = pet.gender === 'male' ? 'Мальчик' : 'Девочка';
        const typeClass = getChipClass(pet, 'type', pet.type);
        const genderClass = getChipClass(pet, 'gender', pet.gender);
        const sizeClass = pet.size ? getChipClass(pet, 'size', pet.size) : '';
        
        cardsHtml += `
            <div class="pet-card" data-pet-id="${pet.id}">
                <div class="image-container">
                    <img class="pet-image" src="${imageUrl || 'https://placehold.co/600x400?text=Нет+фото'}" alt="${pet.name}" onerror="this.src='https://placehold.co/600x400?text=Нет+фото'">
                </div>
                <div class="pet-info">
                    <div class="pet-name">${escapeHtml(pet.name)}</div>
                    <div class="pet-breed">${escapeHtml(pet.breed || 'Порода не указана')}</div>
                    <div class="pet-details">
                        <span class="pet-detail-chip ${typeClass}">${typeLabel}</span>
                        <span class="pet-detail-chip ${genderClass}">${genderLabel}</span>
                        <span class="pet-detail-chip chip-age">${pet.age || '?'} мес.</span>
                        ${pet.size ? `<span class="pet-detail-chip ${sizeClass}">${escapeHtml(pet.size)}</span>` : ''}
                    </div>
                    <div class="pet-price">${pet.price ? pet.price.toLocaleString() + ' ₽' : 'Цена по запросу'}</div>
                    <div class="card-buttons">
                        <button class="btn-detail" data-pet-id="${pet.id}">Узнать больше</button>
                        ${!isManager ? `<button class="btn-chat" data-pet-id="${pet.id}">Написать менеджеру</button>` : ''}
                    </div>
                    ${isManager ? `
                    <div class="admin-actions">
                        <button class="edit-pet-btn" data-pet-id="${pet.id}">Редактировать</button>
                        <button class="delete-pet-btn" data-pet-id="${pet.id}">Удалить</button>
                    </div>` : ''}
                </div>
            </div>`;
    }
    
    appDiv.innerHTML = `
        <div class="container">
            <div class="filters-section"><div class="filters-title">Фильтры</div><div class="chip-group" id="filterChips">
                <div class="chip ${currentFilter === 'all' ? 'active' : ''}" data-filter="all">Все</div>
                <div class="chip ${currentFilter === 'dog' ? 'active' : ''}" data-filter="dog">Собаки</div>
                <div class="chip ${currentFilter === 'cat' ? 'active' : ''}" data-filter="cat">Кошки</div>
                <div class="chip ${currentFilter === 'small' ? 'active' : ''}" data-filter="small">Маленькие</div>
                <div class="chip ${currentFilter === 'medium' ? 'active' : ''}" data-filter="medium">Средние</div>
                <div class="chip ${currentFilter === 'large' ? 'active' : ''}" data-filter="large">Большие</div>
            </div></div>
            <div class="pets-grid">${cardsHtml}</div>
        </div>`;
    
    appDiv.querySelectorAll('.btn-detail').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const petId = btn.dataset.petId;
            const pet = allPets.find(p => p.id === petId);
            if (pet) showPetDetailPage(pet);
        });
    });
    
    if (isManager) {
        appDiv.querySelectorAll('.edit-pet-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const petId = btn.dataset.petId;
                const pet = allPets.find(p => p.id === petId);
                if (pet) showPetForm(pet);
            });
        });
        
        appDiv.querySelectorAll('.delete-pet-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const petId = btn.dataset.petId;
                const pet = allPets.find(p => p.id === petId);
                if (pet && confirm(`Вы уверены, что хотите удалить питомца ${pet.name}?`)) {
                    await deletePet(petId);
                }
            });
        });
    }
    
    appDiv.querySelectorAll('.btn-chat').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const petId = btn.dataset.petId;
            const pet = allPets.find(p => p.id === petId);
            if (pet) openChatForPet(pet);
        });
    });
    
    attachFilterEvents();
}

async function deletePet(petId) {
    try {
        const response = await fetch(`${SUPABASE_URL}/rest/v1/pets?id=eq.${petId}`, {
            method: 'DELETE',
            headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
        });
        if (response.ok) {
            alert('Питомец удалён');
            await loadPets();
        } else {
            alert('Ошибка при удалении');
        }
    } catch(e) {
        console.error(e);
        alert('Ошибка соединения');
    }
}

async function savePet(petData, isEdit = false, petId = null) {
    try {
        let response;
        if (isEdit && petId) {
            response = await fetch(`${SUPABASE_URL}/rest/v1/pets?id=eq.${petId}`, {
                method: 'PATCH',
                headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json' },
                body: JSON.stringify(petData)
            });
        } else {
            response = await fetch(`${SUPABASE_URL}/rest/v1/pets`, {
                method: 'POST',
                headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json' },
                body: JSON.stringify(petData)
            });
        }
        if (response.ok) {
            alert(isEdit ? 'Питомец обновлён' : 'Питомец добавлен');
            await loadPets();
            return true;
        } else {
            alert('Ошибка при сохранении');
            return false;
        }
    } catch(e) {
        console.error(e);
        alert('Ошибка соединения');
        return false;
    }
}

function showPetForm(pet = null) {
    const isEdit = !!pet;
    
    const modalHtml = `
        <div class="admin-pet-modal" id="adminPetModal">
            <div class="admin-pet-modal-content">
                <h3>${isEdit ? 'Редактировать питомца' : 'Добавить питомца'}</h3>
                <input type="text" id="petName" placeholder="Имя" value="${pet ? escapeHtml(pet.name) : ''}">
                <select id="petType">
                    <option value="dog" ${pet && pet.type === 'dog' ? 'selected' : ''}>Собака</option>
                    <option value="cat" ${pet && pet.type === 'cat' ? 'selected' : ''}>Кошка</option>
                </select>
                <input type="text" id="petBreed" placeholder="Порода" value="${pet ? escapeHtml(pet.breed || '') : ''}">
                <input type="number" id="petAge" placeholder="Возраст (месяцев)" value="${pet ? pet.age : ''}">
                <select id="petGender">
                    <option value="male" ${pet && pet.gender === 'male' ? 'selected' : ''}>♂ Мальчик</option>
                    <option value="female" ${pet && pet.gender === 'female' ? 'selected' : ''}>♀ Девочка</option>
                </select>
                <select id="petSize">
                    <option value="">Размер не указан</option>
                    <option value="Маленький" ${pet && pet.size === 'Маленький' ? 'selected' : ''}>Маленький</option>
                    <option value="Средний" ${pet && pet.size === 'Средний' ? 'selected' : ''}>Средний</option>
                    <option value="Большой" ${pet && pet.size === 'Большой' ? 'selected' : ''}>Большой</option>
                </select>
                <input type="text" id="petColor" placeholder="Окрас" value="${pet ? escapeHtml(pet.color || '') : ''}">
                <input type="text" id="petHairLength" placeholder="Длина шерсти" value="${pet ? escapeHtml(pet.hair_length || '') : ''}">
                <input type="text" id="petImageUrl" placeholder="URL изображения" value="${pet ? escapeHtml(pet.image_url || '') : ''}">
                <textarea id="petDescription" placeholder="Описание">${pet ? escapeHtml(pet.description || '') : ''}</textarea>
                <input type="number" id="petPrice" placeholder="Цена (руб.)" value="${pet ? pet.price : ''}">
                <input type="text" id="petAddress" placeholder="Адрес" value="${pet ? escapeHtml(pet.address || '') : ''}">
                <input type="text" id="petPhone" placeholder="Телефон" value="${pet ? escapeHtml(pet.phone || '') : ''}">
                <div class="admin-pet-modal-buttons">
                    <button class="btn-save" id="savePetBtn">Сохранить</button>
                    <button class="btn-cancel" id="cancelPetBtn">Отмена</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    document.getElementById('savePetBtn').addEventListener('click', async () => {
        const petData = {
            name: document.getElementById('petName').value.trim(),
            type: document.getElementById('petType').value,
            breed: document.getElementById('petBreed').value.trim(),
            age: parseInt(document.getElementById('petAge').value) || 0,
            gender: document.getElementById('petGender').value,
            size: document.getElementById('petSize').value,
            color: document.getElementById('petColor').value.trim(),
            hair_length: document.getElementById('petHairLength').value.trim(),
            image_url: document.getElementById('petImageUrl').value.trim(),
            description: document.getElementById('petDescription').value.trim(),
            price: parseFloat(document.getElementById('petPrice').value) || 0,
            address: document.getElementById('petAddress').value.trim(),
            phone: document.getElementById('petPhone').value.trim(),
            created_date: pet ? pet.created_date : new Date().toISOString().split('T')[0]
        };
        
        if (!petData.name) {
            alert('Введите имя питомца');
            return;
        }
        
        const success = await savePet(petData, isEdit, pet ? pet.id : null);
        if (success) {
            const modal = document.getElementById('adminPetModal');
            if (modal) modal.remove();
        }
    });
    
    document.getElementById('cancelPetBtn').addEventListener('click', () => {
        const modal = document.getElementById('adminPetModal');
        if (modal) modal.remove();
    });
}

function attachFilterEvents() {
    document.querySelectorAll('.chip').forEach(chip => {
        chip.removeEventListener('click', () => {});
        chip.addEventListener('click', () => {
            currentFilter = chip.dataset.filter;
            renderCatalog();
        });
    });
}

function showPetDetailPage(pet) {
    let imageUrl = pet.image_url;
    if (imageUrl && !imageUrl.startsWith('http')) imageUrl = `${SUPABASE_URL}/storage/v1/object/public/pets_images/${imageUrl}`;
    const typeLabel = pet.type === 'dog' ? 'Собака' : 'Кошка';
    const genderLabel = pet.gender === 'male' ? 'Мальчик' : 'Девочка';
    const detailHtml = `
        <div class="container">
            <button class="back-btn" id="backToCatalogBtn">← Назад к каталогу</button>
            <div class="detail-container">
                <div class="detail-image-container">
                    <img class="detail-image" src="${imageUrl || 'https://placehold.co/800x500?text=Нет+фото'}" alt="${pet.name}" onerror="this.src='https://placehold.co/800x500?text=Нет+фото'">
                </div>
                <div class="detail-info">
                    <div class="detail-name">${escapeHtml(pet.name)}</div>
                    <div class="detail-breed">${escapeHtml(pet.breed || 'Порода не указана')}</div>
                    <div class="detail-chips">
                        <span class="detail-chip">${typeLabel}</span>
                        <span class="detail-chip">${genderLabel}</span>
                        <span class="detail-chip">${pet.age || '?'} мес.</span>
                        ${pet.size ? `<span class="detail-chip">${escapeHtml(pet.size)}</span>` : ''}
                        ${pet.color ? `<span class="detail-chip">${escapeHtml(pet.color)}</span>` : ''}
                        ${pet.hair_length ? `<span class="detail-chip">${escapeHtml(pet.hair_length)}</span>` : ''}
                    </div>
                    <div class="detail-price">${pet.price ? pet.price.toLocaleString() + ' ₽' : 'Цена по запросу'}</div>
                    <div class="detail-description">${escapeHtml(pet.description || 'Описание отсутствует')}</div>
                    ${pet.address ? `<div class="detail-description"><strong>Адрес:</strong> ${escapeHtml(pet.address)}</div>` : ''}
                    ${pet.phone ? `<div class="detail-description"><strong>Телефон:</strong> ${escapeHtml(pet.phone)}</div>` : ''}
                </div>
            </div>
        </div>
    `;
    const appDiv = document.getElementById('appContent');
    if (appDiv) appDiv.innerHTML = detailHtml;
    const backBtn = document.getElementById('backToCatalogBtn');
    if (backBtn) backBtn.addEventListener('click', () => renderCatalog());
}

async function openChatForPet(pet) {
    if (!currentUser) {
        alert('Войдите в аккаунт, чтобы написать менеджеру');
        document.getElementById('openLoginBtn').click();
        return;
    }
    
    if (currentUser.role === 'Manager') {
        alert('Вы вошли как менеджер. Для общения с клиентами используйте кнопку "Чаты" в шапке сайта.');
        return;
    }
    
    currentChatPet = pet;
    const chatTitle = document.getElementById('chatTitle');
    if (chatTitle) chatTitle.innerHTML = `Чат о ${pet.name}`;
    const managerId = (pet.type === 'dog') ? 'bab5ce91-d235-4672-a851-597c1fce679b' : '46d51d9c-b480-4716-84c8-e4025026963b';
    
    try {
        const existingResponse = await fetch(`${SUPABASE_URL}/rest/v1/chats?pet_id=eq.${pet.id}&client_id=eq.${currentUser.id}&select=id`, {
            headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
        });
        const existing = await existingResponse.json();
        if (existing && existing.length > 0) {
            currentChatId = existing[0].id;
        } else {
            const newChat = { pet_id: pet.id, client_id: currentUser.id, manager_id: managerId, last_message: 'Чат начат', last_message_time: new Date().toISOString() };
            const createResponse = await fetch(`${SUPABASE_URL}/rest/v1/chats`, {
                method: 'POST',
                headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json', 'Prefer': 'return=representation' },
                body: JSON.stringify(newChat)
            });
            const created = await createResponse.json();
            currentChatId = created[0]?.id;
        }
        await loadMessages();
        const chatWindow = document.getElementById('chatWindow');
        if (chatWindow) chatWindow.classList.add('active');
    } catch(e) { console.error(e); alert('Ошибка открытия чата'); }
}

async function loadMessages() {
    if (!currentChatId) return;
    const response = await fetch(`${SUPABASE_URL}/rest/v1/messages?chat_id=eq.${currentChatId}&order=created_at.asc`, {
        headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
    });
    const messages = await response.json();
    const chatBody = document.getElementById('chatBody');
    if (!chatBody) return;
    chatBody.innerHTML = '';
    if (messages) {
        messages.forEach(msg => {
            const div = document.createElement('div');
            div.className = `message ${msg.sender_id === currentUser.id ? 'user' : 'manager'}`;
            div.textContent = msg.message;
            chatBody.appendChild(div);
        });
        chatBody.scrollTop = chatBody.scrollHeight;
    }
}

async function sendMessage() {
    const input = document.getElementById('chatInput');
    const text = input.value.trim();
    if (!text || !currentChatId || !currentUser) return;
    const newMsg = { chat_id: currentChatId, sender_id: currentUser.id, message: text, created_at: new Date().toISOString() };
    await fetch(`${SUPABASE_URL}/rest/v1/messages`, {
        method: 'POST',
        headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json', 'Prefer': 'return=minimal' },
        body: JSON.stringify(newMsg)
    });
    await fetch(`${SUPABASE_URL}/rest/v1/chats?id=eq.${currentChatId}`, {
        method: 'PATCH',
        headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ last_message: text, last_message_time: new Date().toISOString() })
    });
    if (input) input.value = '';
    await loadMessages();
}

function closeChat() {
    const chatWindow = document.getElementById('chatWindow');
    if (chatWindow) chatWindow.classList.remove('active');
    currentChatId = null;
    currentChatPet = null;
}

async function showAdminChats() {
    if (!currentUser || currentUser.role !== 'Manager') return;
    
    const appDiv = document.getElementById('appContent');
    if (appDiv) appDiv.innerHTML = '<div class="container"><div class="loading">Загрузка чатов...</div></div>';
    
    try {
        const response = await fetch(`${SUPABASE_URL}/rest/v1/chats?manager_id=eq.${currentUser.id}&select=*,pets(name),users!chats_client_id_fkey(username)`, {
            headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
        });
        const chats = await response.json();
        
        if (!chats.length) {
            if (appDiv) appDiv.innerHTML = `
                <div class="container">
                    <button class="back-btn" id="backToCatalogBtn">← Назад к каталогу</button>
                    <div class="empty-catalog">У вас пока нет чатов с клиентами</div>
                </div>`;
            const backBtn = document.getElementById('backToCatalogBtn');
            if (backBtn) backBtn.addEventListener('click', () => renderCatalog());
            return;
        }
        
        let chatsHtml = `
            <div class="container">
                <button class="back-btn" id="backToCatalogBtn">← Назад к каталогу</button>
                <div class="filters-section"><div class="filters-title">Чаты с клиентами</div></div>
                <div class="pets-grid">`;
        
        for (let chat of chats) {
            const petName = chat.pets?.name || 'Питомец';
            const clientName = chat.users?.username || 'Клиент';
            chatsHtml += `
                <div class="pet-card" data-chat-id="${chat.id}" data-pet-name="${petName}" data-client-name="${clientName}">
                    <div class="pet-info">
                        <div class="pet-name">${escapeHtml(petName)}</div>
                        <div class="pet-breed">Клиент: ${escapeHtml(clientName)}</div>
                        <div class="pet-details">
                            <span class="pet-detail-chip">Последнее: ${chat.last_message ? chat.last_message.substring(0, 30) : 'Нет сообщений'}</span>
                        </div>
                        <div class="card-buttons">
                            <button class="btn-chat" style="background: #6a1b9a;">Открыть чат</button>
                        </div>
                    </div>
                </div>`;
        }
        
        chatsHtml += `</div></div>`;
        if (appDiv) appDiv.innerHTML = chatsHtml;
        
        const backBtn = document.getElementById('backToCatalogBtn');
        if (backBtn) backBtn.addEventListener('click', () => renderCatalog());
        
        document.querySelectorAll('.pet-card').forEach(card => {
            const chatId = card.dataset.chatId;
            const petName = card.dataset.petName;
            const clientName = card.dataset.clientName;
            const chatBtn = card.querySelector('.btn-chat');
            if (chatBtn) {
                chatBtn.addEventListener('click', () => {
                    openAdminChat(chatId, petName, clientName);
                });
            }
        });
    } catch(e) {
        console.error(e);
        if (appDiv) appDiv.innerHTML = `<div class="container"><div class="empty-catalog">Ошибка загрузки чатов</div></div>`;
    }
}

async function openAdminChat(chatId, petName, clientName) {
    currentChatId = chatId;
    const chatTitle = document.getElementById('chatTitle');
    if (chatTitle) chatTitle.innerHTML = `Чат: ${petName} (${clientName})`;
    await loadMessages();
    const chatWindow = document.getElementById('chatWindow');
    if (chatWindow) chatWindow.classList.add('active');
}

document.addEventListener('DOMContentLoaded', () => {
    const openLoginBtn = document.getElementById('openLoginBtn');
    if (openLoginBtn) {
        openLoginBtn.addEventListener('click', () => {
            const authForm = document.getElementById('authFormContainer');
            const registerForm = document.getElementById('registerFormContainer');
            const authModal = document.getElementById('authModal');
            if (authForm) authForm.style.display = 'block';
            if (registerForm) registerForm.style.display = 'none';
            if (authModal) authModal.classList.add('active');
        });
    }
    
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => { clearSession(); renderCatalog(); });
    }
    
    const switchToRegister = document.getElementById('switchToRegister');
    if (switchToRegister) {
        switchToRegister.addEventListener('click', () => {
            const authForm = document.getElementById('authFormContainer');
            const registerForm = document.getElementById('registerFormContainer');
            if (authForm) authForm.style.display = 'none';
            if (registerForm) registerForm.style.display = 'block';
        });
    }
    
    const switchToLogin = document.getElementById('switchToLogin');
    if (switchToLogin) {
        switchToLogin.addEventListener('click', () => {
            const registerForm = document.getElementById('registerFormContainer');
            const authForm = document.getElementById('authFormContainer');
            if (registerForm) registerForm.style.display = 'none';
            if (authForm) authForm.style.display = 'block';
        });
    }
    
    const submitAuthBtn = document.getElementById('submitAuthBtn');
    if (submitAuthBtn) {
        submitAuthBtn.addEventListener('click', async () => {
            const login = document.getElementById('authLogin')?.value.trim() || '';
            const pwd = document.getElementById('authPassword')?.value || '';
            const errorDiv = document.getElementById('authModalError');
            if (!login || !pwd) {
                if (errorDiv) {
                    errorDiv.textContent = 'Заполните все поля';
                    errorDiv.style.display = 'block';
                }
                return;
            }
            const res = await handleLogin(login, pwd);
            if (res.success) {
                const authModal = document.getElementById('authModal');
                if (authModal) authModal.classList.remove('active');
                renderCatalog();
            } else {
                if (errorDiv) {
                    errorDiv.textContent = res.message;
                    errorDiv.style.display = 'block';
                }
            }
        });
    }
    
    const submitRegisterBtn = document.getElementById('submitRegisterBtn');
    if (submitRegisterBtn) {
        submitRegisterBtn.addEventListener('click', async () => {
            const login = document.getElementById('regLogin')?.value.trim() || '';
            const pwd = document.getElementById('regPassword')?.value || '';
            const repeat = document.getElementById('regPasswordRepeat')?.value || '';
            const errorDiv = document.getElementById('regModalError');
            if (!login || !pwd || !repeat) {
                if (errorDiv) {
                    errorDiv.textContent = 'Заполните все поля';
                    errorDiv.style.display = 'block';
                }
                return;
            }
            const res = await handleRegister(login, pwd, repeat);
            if (res.success) {
                const loginRes = await handleLogin(login, pwd);
                if (loginRes.success) {
                    const authModal = document.getElementById('authModal');
                    if (authModal) authModal.classList.remove('active');
                    renderCatalog();
                } else {
                    if (errorDiv) {
                        errorDiv.textContent = 'Регистрация прошла, войдите вручную';
                        errorDiv.style.display = 'block';
                    }
                    const switchBtn = document.getElementById('switchToLogin');
                    if (switchBtn) switchBtn.click();
                }
            } else {
                if (errorDiv) {
                    errorDiv.textContent = res.message;
                    errorDiv.style.display = 'block';
                }
            }
        });
    }
    
    window.addEventListener('click', (e) => {
        const authModal = document.getElementById('authModal');
        if (e.target === authModal && authModal) authModal.classList.remove('active');
    });
    
    const chatToggleBtn = document.getElementById('chatToggleBtn');
    if (chatToggleBtn) {
        chatToggleBtn.addEventListener('click', () => {
            if (currentChatId) {
                const chatWindow = document.getElementById('chatWindow');
                if (chatWindow) chatWindow.classList.toggle('active');
            } else if (currentUser && currentUser.role === 'Manager') {
                alert('Нажмите кнопку "Чаты" в шапке сайта, чтобы открыть список чатов');
            } else if (currentUser) {
                alert('Выберите питомца и нажмите "Написать менеджеру", чтобы начать чат');
            } else {
                alert('Войдите в аккаунт, чтобы начать чат');
            }
        });
    }
    
    const chatCloseBtn = document.getElementById('chatCloseBtn');
    if (chatCloseBtn) chatCloseBtn.addEventListener('click', closeChat);
    
    const chatSendBtn = document.getElementById('chatSendBtn');
    if (chatSendBtn) chatSendBtn.addEventListener('click', sendMessage);
    
    const chatInput = document.getElementById('chatInput');
    if (chatInput) chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') sendMessage(); });
    
    const adminChatsBtn = document.getElementById('adminChatsBtnHeader');
    if (adminChatsBtn) adminChatsBtn.addEventListener('click', showAdminChats);
    
    const adminAddBtn = document.getElementById('adminAddPetBtn');
    if (adminAddBtn) adminAddBtn.addEventListener('click', () => showPetForm());
    
    checkSession();
    loadPets();
});
