const { spawnSync } = require('child_process');

function putSecret(name, value) {
  const child = spawnSync('npx.cmd', ['wrangler', 'secret', 'put', name], {
    input: value,
    encoding: 'utf-8'
  });
  console.log(`Put ${name}:`, child.stdout, child.stderr);
}

putSecret('CF_ACCESS_TEAM_DOMAIN', 'https://philippeboechat1.cloudflareaccess.com');
putSecret('CF_ACCESS_AUDIENCE', '65d43babaa531c1c316b65357c703feb05e70b7351ef6cbe3bba0d27e7ce12db');
