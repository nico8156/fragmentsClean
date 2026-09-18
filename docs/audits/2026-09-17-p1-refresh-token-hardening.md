# P1 — Rotation atomique et stockage des refresh tokens

Date : 2026-09-17  
Branche backend : `fix/p1-atomic-refresh-token-rotation`  
Branche mobile : `fix/p1-auth-refresh-single-flight`  
Constats traités : `FR-006` et volet stockage des refresh de `FR-024`

## Résultat recherché

Une valeur de refresh obtenue depuis une copie de PostgreSQL ne doit pas être
réutilisable. Deux requêtes concurrentes présentant le même token ne doivent
pas produire deux sessions descendantes. La révocation de l'ancien token et la
création du successeur doivent former une seule transaction.

## Décisions

- Le client reçoit toujours un bearer opaque aléatoire.
- Le serveur persiste uniquement son SHA-256 hexadécimal sur 64 caractères.
- Le domaine `RefreshToken` refuse toute valeur qui n'a pas cette forme.
- La recherche hashée normale sert à la révocation ; la rotation utilise une
  lecture JPA `PESSIMISTIC_WRITE`.
- `RefreshTokenCommandHandler` englobe verrou, révocation et création du
  successeur dans une transaction. Une exception annule les deux écritures.
- Un token absent, expiré, révoqué ou déjà consommé reçoit HTTP 401 sans
  distinguer les causes.
- Le mobile coalesce les demandes de refresh d'une même génération de session.
  Une ancienne requête pendante n'empêche pas une nouvelle session de tourner.

## Migration et compatibilité

Le manifeste `refresh-token-hardening-2026-09.psql` dépend du manifeste
`projection-sync-audience-2026-09`. Il s'exécute backend arrêté dans le pipeline
staging existant.

Le passage du bearer brut à son empreinte n'utilise pas une période dual-read :
la migration supprime les lignes `refresh_tokens`, retire la colonne `token`,
ajoute `token_hash NOT NULL` et son index unique. Cela impose une reconnexion
unique aux utilisateurs déjà connectés lors du déploiement, mais évite de
conserver ou de migrer des secrets directement réutilisables.

Une réponse réseau perdue après commit n'est pas rejouée avec le même ancien
token : le mobile garde la session sur erreur transitoire, puis demandera une
reconnexion lorsque le serveur rejettera explicitement ce token déjà consommé.
Ce compromis privilégie la non-réémission d'un secret. Un protocole de replay
idempotent nécessiterait de conserver un successeur récupérable ou de le dériver
avec un secret serveur ; ce surcroît de complexité n'est pas retenu pour le MVP.

## Vérifications

- cycle rouge observé avant implémentation : colonne `token_hash` absente et
  trois appels mobiles concurrents pour une même session ;
- test PostgreSQL vertical login → rotation → ancien token refusé ;
- test PostgreSQL concurrent : exactement un HTTP 200 et un HTTP 401 ;
- test transactionnel avec panne injectée pendant l'émission du successeur :
  l'ancien token reste ensuite utilisable, preuve du rollback ;
- test de persistance : deux hashes de 64 caractères, aucune colonne `token`,
  aucune valeur brute retrouvée ;
- test domaine de la frontière exacte d'expiration ;
- test unitaire SHA-256 et refus d'un bearer brut dans le domaine ;
- test Jest mobile : trois intentions simultanées produisent une seule rotation ;
- `StagingReleaseUpgradeIT` : upgrade depuis le schéma observé, sessions
  existantes invalidées, colonne brute retirée, rejeu et concurrence du
  manifeste verts.

Résultats finaux du 2026-09-17 :

- `./scripts/test-release.sh` : **570 tests**, 0 échec, 0 erreur, 0 ignoré ;
  PostgreSQL/Testcontainers, LocalStack, architecture, migrations et parcours
  verticaux inclus ;
- `npx tsc --noEmit` : succès ;
- `npx jest --runInBand --no-watchman` : **93 suites / 354 tests** verts ;
- `npx eslint . --no-cache` : 0 erreur, 16 avertissements déjà présents dans
  des tests sans rapport avec ce lot ;
- `npm run redux:map:check` : projection Redux à jour ;
- `git diff --check` : succès dans les deux dépôts.

La première commande ciblée mobile a échoué car le chemin de test fourni était
erroné ; elle a été rejouée avec le chemin réel et a réussi. Deux exécutions
backend ciblées ont d'abord échoué avant l'exécution des tests parce que Docker
Desktop était arrêté, puis parce que le sandbox ne pouvait pas joindre son
socket. Après démarrage explicite de Docker et exécution autorisée hors sandbox,
la suite ciblée puis la gate release complète ont réussi. Ces échecs ne sont pas
présentés comme des échecs fonctionnels du code.

## Limites restantes

`FR-024` comporte aussi la certification de l'historique Git, des anciens
artefacts, des logs CloudWatch/SSM et des accès aux backups. Aucun `gitleaks`,
`trufflehog` ou `git-secrets` n'est installé localement au moment de ce lot.
Cette partie reste ouverte et ne doit pas être déclarée corrigée par le seul
hashage des refresh tokens.

`FR-007` reste le lot suivant : révocation de logout cohérente côté mobile,
Studio et backend.

## Déploiement

État au moment de ce document : implémentation locale, non mergée, non poussée,
non déployée. La migration doit impérativement précéder le démarrage du backend
candidat ; le mobile ancien reste compatible avec le contrat JSON mais devra se
reconnecter après la migration.
