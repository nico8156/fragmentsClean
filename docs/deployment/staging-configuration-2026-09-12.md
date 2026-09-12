# Configuration staging et passage au déploiement — 12 septembre 2026

Lot 10D, Astra High. L'opérateur autorise désormais la configuration SSM, la
préparation/publication du candidat backend et le déploiement avec migration.
Il prend en charge la recette produit. L'autorisation ne supprime pas les
prérequis : **aucun déploiement ni SQL exécuté dans cette tranche**, faute de
configuration Apple complète.

## Réalisé

- Confirmation SNS vérifiée dans AWS : l'abonnement email possède maintenant un
  ARN confirmé, suffixe `f63a5b6c-1ad6-42b9-91ac-0381f7533bc2`, et n'est plus
  `PendingConfirmation`. Aucun test de notification manuelle envoyé.
- Paramètre `/fragments/staging/AUTH_PROVIDER_CREDENTIAL_ENCRYPTION_KEY` créé :
  `SecureString`, Standard, version **1**, 32 octets aléatoires encodés en Base64,
  clé KMS existante `6e2d9298-8432-48e2-a566-bc10cbc78f2a` (`alias/aws/ssm`).
- Création avec `Overwrite=false`, puis relecture déchiffrée vérifiée en mémoire
  contre la valeur générée, avec contrôle du type et de la longueur. Aucun secret
  affiché ni passé en argument de processus, aucun secret existant remplacé.
- Première tentative d'entrée CLI par stdin refusée localement (`Invalid JSON`),
  absence du paramètre revérifiée avant retry. Deuxième tentative via fichier
  temporaire 0600, supprimé automatiquement après l'appel. Suppression normale,
  pas une garantie d'effacement forensique. Aucune copie du secret conservée dans
  le projet ou Git.

Ne jamais régénérer/écraser cette clé après stockage de credentials fournisseur
sans migration de rechiffrement : les anciens credentials deviendraient illisibles.

## Apple : entrée opérateur encore nécessaire

L'inventaire SSM confirme l'absence de :

```text
/fragments/staging/APPLE_TEAM_ID
/fragments/staging/APPLE_KEY_ID
/fragments/staging/APPLE_PRIVATE_KEY
```

Aucun fichier `.p8` trouvé dans la recherche des fichiers visibles backend/mobile.
Cela ne prouve pas l'absence d'une clé ailleurs sur la machine ou dans le compte
Apple. Le mobile déclare le bundle `com.nico8156.fragments` et Sign in with Apple,
mais cela ne fournit ni les identifiants de signature ni la clé privée.

Demande adressée à l'opérateur : fournir le **chemin local** d'une clé Sign in
with Apple `.p8`, son Key ID et son Team ID, sans coller le contenu de la clé dans
la conversation. Si la clé n'existe pas, une action dans le compte Apple Developer
est nécessaire. Aucun accès Apple Developer authentifié n'a été utilisé ici.
Ne pas remplacer une clé Sign in with Apple par une clé App Store Connect ou un
certificat Wallet d'Anchor, ni désactiver Apple pour contourner ce prérequis.

Le bootstrap lit les trois paramètres obligatoirement avant d'écrire `.env`.
Le workflow de déploiement ne doit pas être déclenché avant leur provisionnement
et vérification. L'accord de maintenance est acquis, mais l'arrêt du backend
attend cette condition et les contrôles de release.

## Préparation Git et vérifications

Fetch réalisé, `origin/main` est ancêtre de `main`, sans divergence. Le seul
workflow backend candidat est manuel avec `approve_staging_release=false` par
défaut ; publier les sources ne déclenche pas le déploiement. Le workflow fera
exécuter la suite complète sur Java 21 avant packaging/build ARM64 et déploiement.

Le scanner conservateur ressort non vert à cause de trois fichiers : parser PEM,
test qui génère une paire EC à l'exécution, et test Compose avec texte explicitement
synthétique. Les occurrences ont été revues ; aucun secret réel identifié.
Le même contrôle de motifs sur les **66 révisions à publier avant cette trace**
ne trouve que ces trois chemins. Aucun fichier `.p8`, `.pem`, `.key` ou dump
suivi par Git. Ce contrôle n'est pas un détecteur universel de secrets ; aucune
exclusion n'a été ajoutée pour transformer artificiellement le scanner en vert.

Dernière preuve backend : 515 tests verts, rapports
`target/release-verification.uWtZbL`, non relancés pour cette tranche sans code
produit modifié. Aucun commit mobile/Studio ni workflow de déploiement déclenché.
La preuve du push effectif doit être contrôlée sur la référence distante ; la
publication Git n'est pas une preuve de build ou déploiement.

## Reprise

1. Recevoir les trois éléments Apple, vérifier leur cohérence pour Fragments,
   les placer dans SSM sans exposition et sans écrasement non contrôlé.
2. Vérifier la référence candidate distante et déclencher le workflow manuel
   autorisé à cette révision ; tests Java 21, image ARM64, prérequis runtime.
3. Backup frais après arrêt du writer Fragments, migration journalisée et
   contrôles de santé selon le chemin déjà testé. Aucun arrêt d'Anchor.
4. Fournir les versions effectivement déployées et la matrice de recette à
   l'opérateur. Une panne technique ou un bootstrap incomplet ne vaut pas livraison.
