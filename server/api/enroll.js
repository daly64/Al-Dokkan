import { neon } from '@neondatabase/serverless';
import { createPublicKey } from 'node:crypto';

// Enregistre la clé publique de l'appareil admin. Le dernier appareil qui ouvre l'Admin la remplace.
const sql = neon(process.env.DATABASE_URL);

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'methode non permise' });
  }
  try {
    const b64 = req.body && req.body.publicKey;
    if (typeof b64 !== 'string' || b64.length > 400) return res.status(400).json({ error: 'cle invalide' });
    const key = createPublicKey({ key: Buffer.from(b64, 'base64'), format: 'der', type: 'spki' });
    if (key.asymmetricKeyType !== 'ec' || key.asymmetricKeyDetails.namedCurve !== 'prime256v1') return res.status(400).json({ error: 'cle invalide' });
    await sql`INSERT INTO admin_device (id, pub, last_ts) VALUES (1, ${b64}, 0)
      ON CONFLICT (id) DO UPDATE SET pub = EXCLUDED.pub, last_ts = CASE WHEN admin_device.pub = EXCLUDED.pub THEN admin_device.last_ts ELSE 0 END`;
    return res.status(200).json({ ok: true });
  } catch (e) {
    return res.status(400).json({ error: 'cle invalide' });
  }
}
