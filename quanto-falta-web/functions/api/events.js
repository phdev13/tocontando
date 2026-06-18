// Cloudflare Pages Function: /api/events

function generateSlug(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-API-Key",
};

export async function onRequestOptions() {
  return new Response(null, { headers: corsHeaders });
}

export async function onRequestPost(context) {
  try {
    const apiKey = context.request.headers.get("X-API-Key");
    
    // In production, this should ideally be in context.env.API_SECRET
    // but for simplicity and immediate security we can hardcode a strong token here
    if (apiKey !== "QF_SECURE_API_KEY_998877") {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });
    }

    const body = await context.request.json();
    const { title, date, color, icon } = body;

    if (!title || !date || !color || !icon) {
      return new Response(JSON.stringify({ error: "Campos obrigatórios faltando" }), {
        status: 400,
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });
    }

    const slug = generateSlug(8);
    
    // context.env.DB é a binding do D1 Database
    await context.env.DB.prepare(
      "INSERT INTO events (slug, title, date, color, icon) VALUES (?, ?, ?, ?, ?)"
    ).bind(slug, title, date, color, icon).run();

    const url = `https://share.tocontando.com.br/s/${slug}`;
    
    return new Response(JSON.stringify({ slug, url }), {
      status: 201,
      headers: { "Content-Type": "application/json", ...corsHeaders }
    });

  } catch (error) {
    console.error("API Error:", error);
    return new Response(JSON.stringify({ error: "Erro interno no servidor" }), {
      status: 500,
      headers: { "Content-Type": "application/json", ...corsHeaders }
    });
  }
}

export async function onRequestGet(context) {
  try {
    const url = new URL(context.request.url);
    const slug = url.searchParams.get("slug");

    if (!slug) {
      return new Response(JSON.stringify({ error: "Slug obrigatório" }), {
        status: 400,
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });
    }

    const { results } = await context.env.DB.prepare(
      "SELECT * FROM events WHERE slug = ? LIMIT 1"
    ).bind(slug).all();

    if (!results || results.length === 0) {
      return new Response(JSON.stringify({ error: "Evento não encontrado" }), {
        status: 404,
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });
    }

    return new Response(JSON.stringify(results[0]), {
      status: 200,
      headers: { "Content-Type": "application/json", ...corsHeaders }
    });

  } catch (error) {
    console.error("API Error:", error);
    return new Response(JSON.stringify({ error: "Erro interno no servidor" }), {
      status: 500,
      headers: { "Content-Type": "application/json", ...corsHeaders }
    });
  }
}
