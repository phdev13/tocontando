const API_CONFIG = {
  // A URL oficial do backend
  BASE_URL: window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1" 
    ? "http://127.0.0.1:8787/api/v1/public" // ou URL local de desenvolvimento
    : "https://api.tocontando.com.br/api/v1/public",
  TIMEOUT: 5000,
};

const formatCountText = (count) => {
  if (count >= 1000000) {
    const milhoes = Math.floor(count / 1000000);
    return `Mais de ${milhoes} milh${milhoes > 1 ? 'ões' : 'ão'}`;
  }
  if (count >= 1000) {
    const milhares = Math.floor(count / 1000);
    // Se for entre 1.000 e 9.999, mostrar no formato 'X.000' (ex: 2.000)
    // Se for >= 10.000, mostrar 'X mil' (ex: 12 mil)
    if (milhares < 10) {
      return `Mais de ${milhares}.000`;
    }
    return `Mais de ${milhares} mil`;
  }
  // Se for muito baixo, exibir exatamente ou um texto neutro
  if (count > 0) {
    return `${count}`;
  }
  return "Milhares de"; // fallback
};

const fetchWithTimeout = async (url, options = {}) => {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), API_CONFIG.TIMEOUT);

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal
    });
    clearTimeout(id);
    return response;
  } catch (error) {
    clearTimeout(id);
    throw error;
  }
};

let cachedStats = null;
let isFetchingStats = false;

window.PublicApi = {
  getStats: async () => {
    // Deduplicação e cache em memória
    if (cachedStats) return cachedStats;
    if (isFetchingStats) return new Promise(resolve => setTimeout(() => resolve(cachedStats), 500));

    isFetchingStats = true;
    try {
      const response = await fetchWithTimeout(`${API_CONFIG.BASE_URL}/stats`);
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const json = await response.json();
      
      if (json && json.success && json.data) {
        cachedStats = json.data;
        return cachedStats;
      } else {
        throw new Error("Formato de resposta inválido");
      }
    } catch (error) {
      console.warn("Falha ao buscar estatísticas públicas da API. Usando fallback.", error);
      return null;
    } finally {
      isFetchingStats = false;
    }
  },

  formatStatsText: formatCountText
};

