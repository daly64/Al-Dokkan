# Serveur Al Dokkan (Vercel + Neon)

Garde la liste des apps que la version **Clients** affiche.

- `GET /api/catalog` : public, renvoie `{ "ids": [...] }`.
- `PUT /api/catalog` : en-tête `x-admin-key`, corps `{ "ids": [...] }` (utilisé par la version Admin).
- Variables : `DATABASE_URL` (Neon), `ADMIN_KEY`.
- Table : `client_catalog (id smallint primary key, ids jsonb, updated_at timestamptz)`.
