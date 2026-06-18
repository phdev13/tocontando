// share.js

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const pathParts = window.location.pathname.split('/');
    const lastPart = pathParts[pathParts.length - 1];
    
    // Support both /share/abc123 and /share?s=abc123
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
        // Use the centralized api.js fetcher (window.PublicApi)
        const response = await window.PublicApi.fetchData(`/share/events/${slug}`);
        if (!response || !response.event) throw new Error('Not found');
        
        renderEvent(response.event);
    } catch (err) {
        console.error("Erro ao buscar evento:", err);
        showError();
    }
}

function renderEvent(event) {
    document.getElementById('loading').classList.add('hidden');
    document.getElementById('event-view').classList.remove('hidden');

    // Material Icons mapping
    const iconMap = {
        'airplane': 'flight', 'cake': 'cake', 'favorite': 'favorite', 'star': 'star', 
        'work': 'work', 'school': 'school', 'home': 'home', 'beachaccess': 'beach_access',
        'musicnote': 'music_note', 'alarm': 'alarm', 'celebration': 'celebration',
        'cardgiftcard': 'card_giftcard', 'localhospital': 'local_hospital', 
        'healthandsafety': 'health_and_safety', 'favoriteborder': 'favorite_border', 
        'healing': 'healing', 'default': 'event'
    };

    document.getElementById('event-title').textContent = event.title;
    
    // Configure target date
    const targetDate = new Date(event.date);
    targetDate.setHours(23, 59, 59, 999);
    
    const formatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' });
    document.getElementById('event-date').textContent = formatter.format(targetDate);

    // Cover Image integration
    const coverEl = document.getElementById('event-cover');
    if (event.cover_url) {
        coverEl.style.backgroundImage = `url('${event.cover_url}')`;
        // Make the overlay slightly darker so text is readable
        coverEl.querySelector('.cover-overlay').style.background = 'linear-gradient(to bottom, rgba(0,0,0,0.4), var(--surface))';
    } else {
        // Fallback pattern if no photo
        coverEl.style.background = 'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.0) 100%)';
    }

    // Colors and backgrounds
    if (event.color) {
        const color = event.color.startsWith('#') ? event.color : `#${event.color}`;
        
        // HEX to RGB
        const r = parseInt(color.slice(1, 3), 16) || 0;
        const g = parseInt(color.slice(3, 5), 16) || 0;
        const b = parseInt(color.slice(5, 7), 16) || 0;
        
        // Set brand color
        document.documentElement.style.setProperty('--brand', color);
        
        // Background blob gradient
        document.getElementById('dynamic-bg').style.background = `radial-gradient(circle at 50% 30%, rgba(${r}, ${g}, ${b}, 0.25) 0%, var(--bg) 80%)`;
        
        // Icon wrapper background
        document.getElementById('event-icon-wrapper').style.background = `rgba(${r}, ${g}, ${b}, 0.2)`;
        document.getElementById('event-icon-wrapper').style.border = `1px solid rgba(${r}, ${g}, ${b}, 0.4)`;
        document.getElementById('event-icon').style.color = color;
    }

    const emoji = iconMap[event.icon?.toLowerCase()] || iconMap['default'];
    document.getElementById('event-icon').textContent = emoji;

    // Start clock
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
