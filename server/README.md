# Serveur Al Dokkan (Vercel + Neon)

Garde le choix de l'admin : pour chaque app, proposée aux clients sur mobile et/ou TV.

- `GET /api/catalog` : public, renvoie `{ "mobile": [...], "tv": [...], "ids": [...] }`.
- `PUT /api/catalog` : en-tête `x-admin-key`, corps `{ "mobile": [...], "tv": [...] }` (utilisé par l'app Admin).
- Variables : `DATABASE_URL` (Neon), `ADMIN_KEY`.
- Table : `client_catalog (id smallint primary key, ids jsonb, updated_at timestamptz)`.
