# Serveur Al Dokkan (Vercel + Neon)

Garde le choix de l'admin : pour chaque app, proposée aux clients sur mobile et/ou TV.

- `GET /api/catalog` : public, renvoie `{ mobile, tv, ids, enrolled, device }`.
- `POST /api/enroll` : `{ publicKey }` (base64, X.509, EC P-256). Accepté une seule fois : la clé de l'appareil admin.
- `PUT /api/catalog` : `{ mobile, tv }`, avec `x-ts` (ms) et `x-sig` = signature ECDSA-SHA256 (base64) de `"<ts>.<JSON compact des listes triées>"`.
  Refusé si la signature est fausse, l'heure à plus de 5 min, ou l'horodatage déjà utilisé.
- Variable : `DATABASE_URL` (Neon). Tables : `client_catalog`, `admin_device`.
- Changer d'appareil admin : vider la table `admin_device` (`DELETE FROM admin_device`), puis ouvrir l'app Admin sur le nouvel appareil.
