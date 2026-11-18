import { neon } from '@neondatabase/serverless';
import { createHash, verify } from 'node:crypto';

// Choix de l'admin : pour chaque app, proposée aux clients sur mobile et/ou sur TV (une seule ligne en base).
// Seul l'appareil admin enregistré (clé dans son Keystore) peut modifier : chaque PUT est signé et horodaté.
const sql = neon(process.env.DATABASE_URL);

const valid = (l) => Array.isArray(l) && l.length <= 100 && l.every((i) => typeof i === 'string' && /^[a-z0-9_-]{1,40}$/.test(i));
const clean = (l) => [...new Set(l)].sort();
const deviceId = (pub) => createHash('sha256').update(Buffer.from(pub, 'base64')).digest('hex').slice(0, 8);

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  try {
    if (req.method === 'GET') {
      const rows = await sql`SELECT ids, updated_at FROM client_catalog WHERE id = 1`;
      const dev = await sql`SELECT pub FROM admin_device WHERE id = 1`;
      const raw = rows[0] ? rows[0].ids : [];
      // Ancien format : une simple liste valable pour mobile et TV.
      const mobile = Array.isArray(raw) ? raw : raw.mobile || [];
      const tv = Array.isArray(raw) ? raw : raw.tv || [];
      return res.status(200).json({
        mobile, tv, ids: clean([...mobile, ...tv]), updatedAt: rows[0] ? rows[0].updated_at : null,
        enrolled: dev.length > 0, device: dev[0] ? deviceId(dev[0].pub) : null,
      });
    }
    if (req.method === 'PUT') {
      const b = req.body || {};
      if (!valid(b.mobile) || !valid(b.tv)) return res.status(400).json({ error: 'liste invalide' });
      const value = { mobile: clean(b.mobile), tv: clean(b.tv) };
      const ts = Number(req.headers['x-ts']);
      const sig = req.headers['x-sig'];
      const dev = await sql`SELECT pub, last_ts FROM admin_device WHERE id = 1`;
      if (!dev[0] || typeof sig !== 'string' || !Number.isFinite(ts)) return res.status(401).json({ error: 'non autorise' });
      if (Math.abs(Date.now() - ts) > 5 * 60 * 1000) return res.status(401).json({ error: 'heure incorrecte' });
      const ok = verify('sha256', Buffer.from(`${ts}.${JSON.stringify(value)}`),
        { key: Buffer.from(dev[0].pub, 'base64'), format: 'der', type: 'spki' }, Buffer.from(sig, 'base64'));
      if (!ok) return res.status(401).json({ error: 'signature refusee' });
      // Chaque horodatage ne sert qu'une fois (pas de rejeu).
      const fresh = await sql`UPDATE admin_device SET last_ts = ${ts} WHERE id = 1 AND last_ts < ${ts} RETURNING id`;
      if (!fresh.length) return res.status(401).json({ error: 'requete deja utilisee' });
      await sql`
        INSERT INTO client_catalog (id, ids, updated_at) VALUES (1, ${JSON.stringify(value)}::jsonb, now())
        ON CONFLICT (id) DO UPDATE SET ids = EXCLUDED.ids, updated_at = now()`;
      return res.status(200).json(value);
    }
    res.setHeader('Allow', 'GET, PUT');
    return res.status(405).json({ error: 'methode non permise' });
  } catch (e) {
    return res.status(500).json({ error: 'erreur serveur' });
  }
}
