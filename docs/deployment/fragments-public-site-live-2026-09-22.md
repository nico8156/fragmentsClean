# Publication de la vitrine Fragments — 22 septembre 2026

## Périmètre et source

- Source : `main` au commit `8d2160f22a0f10dc4247a863e746347812f8feb5`,
  working tree propre avant l'intervention.
- Site : `https://fragments.anchor-event.fr/`.
- Pages : accueil, Tech, confidentialité, conditions, mentions légales et CSS.
- Aucun backend, Studio, build mobile, variable EAS ou schéma SQL modifié.
- Les pages `https://fragments-staging.anchor-event.fr/legal/*` restent sur
  leur ancien répertoire, utilisé par TestFlight.
- Le site reste marqué `beta` et `noindex,nofollow` dans le HTML : il est
  directement accessible mais n'est pas destiné à l'indexation pour l'instant.

## DNS, fichiers et Caddy

Le propriétaire a créé dans OVH l'entrée A
`fragments.anchor-event.fr → 13.39.97.191`. La réponse a été vérifiée auprès
de `dns109.ovh.net` et de `1.1.1.1`.

Les six fichiers publics sont dans le répertoire dédié
`/srv/platform/data/fragments-site/releases/8d2160f`. Le lien relatif
`/srv/platform/data/fragments-site/current → releases/8d2160f` est visible
dans le conteneur Caddy sous `/data/fragments-site/current`. L'archive de
transfert avait l'empreinte SHA-256
`22523cda6e056d62103d25ead1df1af67112c1feea4710c4e2819511b60641b3`.
Son contenu a été vérifié avant extraction. Les six fichiers AppleDouble
`._*` ajoutés par le tar macOS ont été retirés **avant** exposition ; la route
`/legal/._index.html` retourne 404.

Le Caddyfile actif préexistant avait l'empreinte
`48abc04cd5f0ebdd5357856b906c1bb8fcfa0a63d787387738fb1bc83e4f0ea3`.
Il contenait deux imports Dogs Out, qui ont été préservés à l'identique. Le
nouveau candidat porte l'empreinte
`335e8f3e5005818400b8b9432a47d0cda543b69350b75340bdd9f4ad497543e0`.
Il ajoute exclusivement le vhost `fragments.anchor-event.fr` : uniquement
les fichiers de présentation explicitement listés, CSP restrictive et 404
pour l'API et les autres chemins. Le candidat a passé
`caddy validate --config /config/Caddyfile.fragments-public-candidate` dans
le conteneur actif.

La sauvegarde du Caddyfile précédent est
`/srv/platform/Caddyfile.before-fragments-public-20260922`. Le Caddyfile
candidat est installé à `/srv/platform/Caddyfile` et le conteneur actif a été
rechargé sans recréation. Attention : le fichier est monté individuellement
en lecture seule. Après un remplacement atomique côté hôte, le chemin
`/etc/caddy/Caddyfile` **dans le conteneur courant** pointait encore vers
l'ancien inode ; le premier reload a loggé `config is unchanged`.
La configuration a donc été chargée depuis
`/config/Caddyfile.fragments-public-candidate`. Au prochain démarrage du
conteneur, le bind mount utilisera le nouveau Caddyfile installé sur l'hôte.
Ne pas supprimer le candidat dans `/srv/platform/config` tant que ce
conteneur tourne.

Le Caddyfile versionné dans ce dépôt porte le vhost Fragments public, mais pas
les imports Dogs Out, détenus par le déploiement Dogs Out. Ne jamais écraser
aveuglément `/srv/platform/Caddyfile` avec ce seul fichier source : préserver
les imports des deux produits et valider le candidat combiné avant reload.

## Vérifications réalisées

- `node --test scripts/test-fragments-public-site.mjs` : 7/7 verts.
- `node --test scripts/test-fragments-public-site-infra.mjs` : 1/1 vert avec
  de vrais conteneurs Caddy ; le premier essai dans le sandbox avait échoué
  faute d'accès au socket Docker, puis le test autorisé a réussi.
- Une première commande SSM de copie a échoué par substitution shell
  incompatible, avant toute écriture distante ; la seconde a réussi.
- Accueil, Tech, confidentialité, conditions, mentions et CSS : HTTP 200,
  certificat TLS vérifié (`curl ssl_verify_result=0`).
- Route inconnue, API sur le domaine public et fichier AppleDouble : HTTP 404.
- Fragments staging, sa liveness et Dogs Out : HTTP 200 après reload.
- Réponse de l'accueil : CSP `default-src 'none'` avec CSS local,
  `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`,
  sans cookie.
- Captures desktop et mobile inspectées. Avec un vrai viewport mobile de
  390 px, `documentElement.scrollWidth=390` pour l'accueil, Tech et la
  confidentialité : pas de débordement horizontal.

Commandes SSM principales : inspection `c12de562-064a-4246-a24b-e4e548ca214c`,
copie réussie `0756f899-5d36-4799-90bd-488946a4872e`, nettoyage et lien
`79496bdd-6b74-4ea3-886e-702294128eca`, validation Caddy
`16405f4f-8384-4e3f-821c-413a9c643959`, chargement à chaud effectif
`ca7e7075-9e89-46ec-ace5-ecb4eeaa9b02`.

## Retour arrière

Ne toucher ni au backend Fragments ni aux volumes Dogs Out. Vérifier d'abord
que le Caddyfile hôte porte encore l'empreinte candidate ci-dessus ; si elle
a changé, arrêter et réexaminer l'état actif. Restaurer alors la sauvegarde
vers `/srv/platform/Caddyfile` avec un remplacement atomique, puis recharger
le conteneur courant depuis `/etc/caddy/Caddyfile` (son bind mount pointe
encore vers l'ancien contenu) ou recréer Caddy avec **tous** ses overlays
Dogs Out après validation. Vérifier de nouveau Fragments staging, Dogs Out
et l'API. Le DNS A peut rester temporairement en place, mais le nouveau
domaine ne servira plus le site tant que le vhost est retiré.

Le répertoire de release statique est conservé pour permettre une remise en
ligne sans retransfert. Ne pas le supprimer lors d'un retour arrière proxy.
