import { neon } from '@neondatabase/serverless';
import { createHash, timingSafeEqual } from 'node:crypto';

// Liste des apps que la version Clients d'Al Dokkan affiche (une seule ligne en base).
const sql = neon(process.env.DATABASE_URL);

const sha = (s) => createHash('sha256').update(String(s)).digest();
const isAdmin = (req) => {
  const key = process.env.ADMIN_KEY;
  const given = req.headers['x-admin-key'];
  return Boolean(key) && typeof given === 'string' && timingSafeEqual(sha(key), sha(given));
};

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  try {
    if (req.method === 'GET') {
      const rows = await sql`SELECT ids, updated_at FROM client_catalog WHERE id = 1`;
      const row = rows[0];
      return res.status(200).json({ ids: row ? row.ids : [], updatedAt: row ? row.updated_at : null });
    }
    if (req.method === 'PUT') {
      if (!isAdmin(req)) return res.status(401).json({ error: 'cle refusee' });
      const ids = req.body && req.body.ids;
      const ok = Array.isArray(ids) && ids.length <= 100 && ids.every((i) => typeof i === 'string' && /^[a-z0-9_-]{1,40}$/.test(i));
      if (!ok) return res.status(400).json({ error: 'liste invalide' });
      const unique = [...new Set(ids)].sort();
      await sql`
        INSERT INTO client_catalog (id, ids, updated_at) VALUES (1, ${JSON.stringify(unique)}::jsonb, now())
        ON CONFLICT (id) DO UPDATE SET ids = EXCLUDED.ids, updated_at = now()`;
      return res.status(200).json({ ids: unique });
    }
    res.setHeader('Allow', 'GET, PUT');
    return res.status(405).json({ error: 'methode non permise' });
  } catch (e) {
    return res.status(500).json({ error: 'erreur serveur' });
  }
}
