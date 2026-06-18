// Cloudflare Pages Function: /api/og

export async function onRequestGet(context) {
  try {
    const url = new URL(context.request.url);
    const slug = url.searchParams.get("slug");

    if (!slug) {
      return new Response("Slug missing", { status: 400 });
    }

    // Busca o evento do banco de dados D1
    const { results } = await context.env.DB.prepare(
      "SELECT title, date, color FROM events WHERE slug = ? LIMIT 1"
    ).bind(slug).all();

    if (!results || results.length === 0) {
      return new Response("Not found", { status: 404 });
    }

    const event = results[0];
    const targetDate = new Date(event.date);
    const now = new Date();
    const diff = targetDate.getTime() - now.getTime();
    const daysLeft = Math.ceil(diff / (1000 * 60 * 60 * 24));
    
    let color = event.color;
    if (!color.startsWith('#')) color = `#${color}`;
    
    let message = `${daysLeft} dias restantes`;
    if (daysLeft < 0) {
      message = "Este evento já aconteceu 🎉";
    } else if (daysLeft === 0) {
      message = "É hoje! 🎉";
    } else if (daysLeft === 1) {
      message = "Falta 1 dia!";
    }

    // Gerando um SVG puro (Perfeito para Edge Computing, 0 dependências)
    const svg = `
      <svg width="1200" height="630" viewBox="0 0 1200 630" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <radialGradient id="bg" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="${color}" stop-opacity="0.2"/>
            <stop offset="100%" stop-color="#0A0A0A" stop-opacity="1"/>
          </radialGradient>
        </defs>
        <rect width="1200" height="630" fill="url(#bg)"/>
        
        <text x="600" y="280" font-family="sans-serif" font-size="70" font-weight="bold" fill="#ffffff" text-anchor="middle">
          ${event.title}
        </text>
        
        <text x="600" y="380" font-family="sans-serif" font-size="45" font-weight="bold" fill="${color}" text-anchor="middle">
          ${message}
        </text>

        <text x="600" y="580" font-family="sans-serif" font-size="28" fill="#666666" text-anchor="middle">
          tocontando.com.br
        </text>
      </svg>
    `;

    return new Response(svg, {
      headers: {
        "Content-Type": "image/svg+xml",
        "Cache-Control": "public, max-age=3600"
      }
    });

  } catch (error) {
    console.error(error);
    return new Response("Failed to generate image", { status: 500 });
  }
}
