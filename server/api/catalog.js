import { neon } from '@neondatabase/serverless';
import { createHash, timingSafeEqual } from 'node:crypto';

// Choix de l'admin : pour chaque app, proposée aux clients sur mobile et/ou sur TV (une seule ligne en base).
const sql = neon(process.env.DATABASE_URL);

const sha = (s) => createHash('sha256').update(String(s)).digest();
const isAdmin = (req) => {
  const key = process.env.ADMIN_KEY;
  const given = req.headers['x-admin-key'];
  return Boolean(key) && typeof given === 'string' && timingSafeEqual(sha(key), sha(given));
};
const valid = (l) => Array.isArray(l) && l.length <= 100 && l.every((i) => typeof i === 'string' && /^[a-z0-9_-]{1,40}$/.test(i));
const clean = (l) => [...new Set(l)].sort();

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  try {
    if (req.method === 'GET') {
      const rows = await sql`SELECT ids, updated_at FROM client_catalog WHERE id = 1`;
      const raw = rows[0] ? rows[0].ids : [];
      // Ancien format : une simple liste valable pour mobile et TV.
      const mobile = Array.isArray(raw) ? raw : raw.mobile || [];
      const tv = Array.isArray(raw) ? raw : raw.tv || [];
      return res.status(200).json({ mobile, tv, ids: clean([...mobile, ...tv]), updatedAt: rows[0] ? rows[0].updated_at : null });
    }
    if (req.method === 'PUT') {
      if (!isAdmin(req)) return res.status(401).json({ error: 'cle refusee' });
      const b = req.body || {};
      if (!valid(b.mobile) || !valid(b.tv)) return res.status(400).json({ error: 'liste invalide' });
      const value = { mobile: clean(b.mobile), tv: clean(b.tv) };
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
