const SUPABASE_URL = 'https://rifzmuphaemtlmrijaqr.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJpZnptdXBoYWVtdGxtcmlqYXFyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIwNzc3MDAsImV4cCI6MjA3NzY1MzcwMH0.MA5qpZby_xlSAbwS70JfqbOGkRI04DZlb80MPRRP5Lc';

let currentUser = null;
let allPets = [];
let currentFilter = 'all';
let currentChatPet = null;
let currentChatId = null;

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
    if (currentUser) {
        authContainer.style.display = 'none';
        userPanelDiv.style.display = 'flex';
        document.getElementById('userName').textContent = currentUser.username;
        document.getElementById('userRole').textContent = currentUser.role === 'Manager' ? 'Менеджер' : 'Клиент';
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
    document.getElementById('authModal').classList.remove('active');
    closeChat();
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
    document.getElementById('appContent').innerHTML = '<div class="container"><div class="loading">Загрузка питомцев...</div></div>';
    try {
        const response = await fetch(`${SUPABASE_URL}/rest/v1/pets?select=*`, {
            method: 'GET', headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
        });
        if (!response.ok) throw new Error();
        allPets = await response.json();
        renderCatalog();
    } catch (error) {
        document.getElementById('appContent').innerHTML = `<div class="container"><div class="empty-catalog">Не удалось загрузить питомцев</div></div>`;
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

    if (filtered.length === 0) {
        document.getElementById('appContent').innerHTML = `
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
    for (let pet of filtered) {
        let imageUrl = pet.image_url;
        if (imageUrl && !imageUrl.startsWith('http')) imageUrl = `${SUPABASE_URL}/storage/v1/object/public/pets_images/${imageUrl}`;
        const typeLabel = pet.type === 'dog' ? 'Собака' : 'Кошка';
        const genderLabel = pet.gender === 'male' ? 'Мальчик' : 'Девочка';
        const typeClass = getChipClass(pet, 'type', pet.type);
        const genderClass = getChipClass(pet, 'gender', pet.gender);
        const sizeClass = pet.size ? getChipClass(pet, 'size', pet.size) : '';
        cardsHtml += `
            <div class="pet-card" data-pet='${JSON.stringify(pet).replace(/'/g, "&#39;")}'>
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
                        <button class="btn-detail">Узнать больше</button>
                        <button class="btn-chat">Написать менеджеру</button>
                    </div>
                </div>
            </div>`;
    }
    document.getElementById('appContent').innerHTML = `
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
    
    document.querySelectorAll('.pet-card').forEach(card => {
        const pet = JSON.parse(card.dataset.pet);
        const detailBtn = card.querySelector('.btn-detail');
        const chatBtn = card.querySelector('.btn-chat');
        detailBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            showPetDetailPage(pet);
        });
        chatBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            openChatForPet(pet);
        });
    });
    attachFilterEvents();
}

function attachFilterEvents() {
    document.querySelectorAll('.chip').forEach(chip => {
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
    document.getElementById('appContent').innerHTML = detailHtml;
    document.getElementById('backToCatalogBtn').addEventListener('click', () => renderCatalog());
}

async function openChatForPet(pet) {
    if (!currentUser) {
        alert('Войдите в аккаунт, чтобы написать менеджеру');
        document.getElementById('openLoginBtn').click();
        return;
    }
    currentChatPet = pet;
    document.getElementById('chatTitle').innerHTML = `Чат о ${pet.name}`;
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
        document.getElementById('chatWindow').classList.add('active');
    } catch(e) { console.error(e); alert('Ошибка открытия чата'); }
}

async function loadMessages() {
    if (!currentChatId) return;
    const response = await fetch(`${SUPABASE_URL}/rest/v1/messages?chat_id=eq.${currentChatId}&order=created_at.asc`, {
        headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` }
    });
    const messages = await response.json();
    const chatBody = document.getElementById('chatBody');
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
    input.value = '';
    await loadMessages();
}

function closeChat() {
    document.getElementById('chatWindow').classList.remove('active');
    currentChatId = null;
    currentChatPet = null;
}

document.getElementById('openLoginBtn').addEventListener('click', () => {
    document.getElementById('authFormContainer').style.display = 'block';
    document.getElementById('registerFormContainer').style.display = 'none';
    document.getElementById('authModal').classList.add('active');
});

document.getElementById('logoutBtn').addEventListener('click', () => { clearSession(); renderCatalog(); });

document.getElementById('switchToRegister').addEventListener('click', () => {
    document.getElementById('authFormContainer').style.display = 'none';
    document.getElementById('registerFormContainer').style.display = 'block';
});

document.getElementById('switchToLogin').addEventListener('click', () => {
    document.getElementById('registerFormContainer').style.display = 'none';
    document.getElementById('authFormContainer').style.display = 'block';
});

document.getElementById('submitAuthBtn').addEventListener('click', async () => {
    const login = document.getElementById('authLogin').value.trim();
    const pwd = document.getElementById('authPassword').value;
    const errorDiv = document.getElementById('authModalError');
    if (!login || !pwd) { errorDiv.textContent = 'Заполните все поля'; errorDiv.style.display = 'block'; return; }
    const res = await handleLogin(login, pwd);
    if (res.success) {
        document.getElementById('authModal').classList.remove('active');
        renderCatalog();
    } else {
        errorDiv.textContent = res.message;
        errorDiv.style.display = 'block';
    }
});

document.getElementById('submitRegisterBtn').addEventListener('click', async () => {
    const login = document.getElementById('regLogin').value.trim();
    const pwd = document.getElementById('regPassword').value;
    const repeat = document.getElementById('regPasswordRepeat').value;
    const errorDiv = document.getElementById('regModalError');
    if (!login || !pwd || !repeat) { errorDiv.textContent = 'Заполните все поля'; errorDiv.style.display = 'block'; return; }
    const res = await handleRegister(login, pwd, repeat);
    if (res.success) {
        const loginRes = await handleLogin(login, pwd);
        if (loginRes.success) {
            document.getElementById('authModal').classList.remove('active');
            renderCatalog();
        } else {
            errorDiv.textContent = 'Регистрация прошла, войдите вручную';
            errorDiv.style.display = 'block';
            document.getElementById('switchToLogin').click();
        }
    } else {
        errorDiv.textContent = res.message;
        errorDiv.style.display = 'block';
    }
});

window.addEventListener('click', (e) => { if (e.target === document.getElementById('authModal')) document.getElementById('authModal').classList.remove('active'); });

document.getElementById('chatToggleBtn').addEventListener('click', () => {
    if (currentChatId) {
        document.getElementById('chatWindow').classList.toggle('active');
    } else if (currentUser) {
        alert('Выберите питомца, чтобы начать чат');
    } else {
        alert('Войдите в аккаунт, чтобы начать чат');
    }
});

document.getElementById('chatCloseBtn').addEventListener('click', closeChat);
document.getElementById('chatSendBtn').addEventListener('click', sendMessage);
document.getElementById('chatInput').addEventListener('keypress', (e) => { if (e.key === 'Enter') sendMessage(); });

checkSession();
loadPets();
