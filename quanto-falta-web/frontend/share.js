// share.js

const API_BASE_URL = '/api'; // Aponta para o próprio Cloudflare Pages Functions

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const pathParts = window.location.pathname.split('/');
    const lastPart = pathParts[pathParts.length - 1];
    // Suporta tanto /share/abc123 quanto /share?s=abc123
    const slug = (lastPart !== 'share.html' && lastPart !== 'share' && lastPart !== '')
        ? lastPart
        : urlParams.get('s');

    if (!slug) {
        showError();
        return;
    }

    fetchEvent(slug);
});

async function fetchEvent(slug) {
    try {
        const response = await fetch(`${API_BASE_URL}/events?slug=${slug}`);
        if (!response.ok) throw new Error('Not found');
        
        const eventData = await response.json();
        renderEvent(eventData);
    } catch (err) {
        console.error("Erro ao buscar evento:", err);
        showError();
    }
}

function renderEvent(event) {
    document.getElementById('loading').classList.add('hidden');
    document.getElementById('event-view').classList.remove('hidden');

    // Mapeamento básico de ícones do Android para Material Icons
    const iconMap = {
        'airplane': 'flight', 'cake': 'cake', 'favorite': 'favorite', 'star': 'star', 
        'work': 'work', 'school': 'school', 'home': 'home', 'beachaccess': 'beach_access',
        'musicnote': 'music_note', 'alarm': 'alarm', 'celebration': 'celebration',
        'cardgiftcard': 'card_giftcard', 'localhospital': 'local_hospital', 
        'healthandsafety': 'health_and_safety', 'favoriteborder': 'favorite_border', 
        'healing': 'healing', 'default': 'event'
    };

    document.getElementById('event-title').textContent = event.title;
    
    // Configura data alvo
    const targetDate = new Date(event.date);
    targetDate.setHours(23, 59, 59, 999);
    
    const formatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' });
    document.getElementById('event-date').textContent = formatter.format(targetDate);

    // Ajusta a cor principal (gradiente de fundo)
    if (event.color) {
        const color = event.color.startsWith('#') ? event.color : `#${event.color}`;
        document.documentElement.style.setProperty('--primary-color', color);
        
        // Converte HEX para RGB
        const r = parseInt(color.slice(1, 3), 16) || 0;
        const g = parseInt(color.slice(3, 5), 16) || 0;
        const b = parseInt(color.slice(5, 7), 16) || 0;
        
        // Generate darker shade for the gradient bottom
        const darkR = Math.min(255, Math.floor(r * 0.75));
        const darkG = Math.min(255, Math.floor(g * 0.75));
        const darkB = Math.min(255, Math.floor(b * 1.25));
        const darkColor = `rgb(${darkR}, ${darkG}, ${darkB})`;
        document.documentElement.style.setProperty('--dark-color', darkColor);
        
        document.getElementById('dynamic-bg').style.background = `radial-gradient(circle at center, rgba(${r}, ${g}, ${b}, 0.15) 0%, transparent 50%)`;
    }

    const emoji = iconMap[event.icon?.toLowerCase()] || iconMap['default'];
    document.getElementById('floating-emoji').textContent = emoji;
    document.getElementById('watermark-emoji').textContent = emoji;

    // Inicia o relógio
    updateCountdown(targetDate);
    setInterval(() => updateCountdown(targetDate), 1000);
}

function updateCountdown(targetDate) {
    const now = new Date();
    const diff = targetDate.getTime() - now.getTime();

    if (diff <= 0) {
        document.getElementById('countdown-wrapper').classList.add('hidden');
        document.getElementById('celebration-msg').classList.remove('hidden');
        return;
    }

    // Lógica simplificada de "Tô Contando" da Web
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    let value, unit;
    
    if (days > 365) {
        value = Math.floor(days / 365);
        unit = value === 1 ? 'ano' : 'anos';
    } else if (days > 30) {
        value = Math.floor(days / 30);
        unit = value === 1 ? 'mês' : 'meses';
    } else if (days > 7) {
        value = Math.floor(days / 7);
        unit = value === 1 ? 'semana' : 'semanas';
    } else if (days > 0) {
        value = days;
        unit = days === 1 ? 'dia' : 'dias';
    } else {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        if (hours > 0) {
            value = hours;
            unit = hours === 1 ? 'hora' : 'horas';
        } else {
            const mins = Math.floor(diff / (1000 * 60));
            value = mins;
            unit = mins === 1 ? 'minuto' : 'minutos';
            if (mins === 0) {
                value = "< 1";
                unit = "minuto";
            }
        }
    }

    document.getElementById('time-left').textContent = value;
    document.getElementById('time-unit').textContent = unit;
}

function showError() {
    document.getElementById('loading').classList.add('hidden');
    document.getElementById('error').classList.remove('hidden');
}
