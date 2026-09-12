# Fragments — dossier de recette TestFlight / App Store

Préparation du 12 septembre 2026, fin du lot 09 et lot 10 avec Astra High,
à la demande du produit. Ce document ne vaut ni déploiement, ni soumission,
ni validation sur appareil. **Décision actuelle : NO GO pour soumettre.**

## Preuves locales obtenues

| Vérification du 12 septembre | Résultat |
| --- | --- |
| Backend, `bash scripts/test-release.sh` | 515 tests / 209 classes, 0 échec, 0 erreur, 0 ignoré ; terminé à 14:27:33 CEST, 2 min 49 |
| Mobile Jest | 77 suites, 289 tests verts |
| Configuration release mobile | 8 tests verts, dont le vrai plugin de permissions |
| Mobile types/lint/Redux/natif | TypeScript et contrôles verts ; lint 0 erreur, 20 avertissements préexistants |
| Studio | 31 suites, 146 tests verts ; build public sans tokens, vérification du bundle et contrat API verts |
| Backup réel / upgrade | 12:50:06 CEST : checksum valide, restauration locale réussie, 46 tables sources préservées, réexécution stable sur 69 tables ; copies nettoyées |

Rapports backend isolés : `target/release-verification.uWtZbL`. La JVM locale
est Java 23 Valhalla, avec compilation `release 21`. Ce résultat n'est pas une
preuve sur le runtime de production ni sur une archive iOS. La première
régression exhaustive avait trouvé 19 cas en échec/erreur sur 490 tests ; les
corrections et tests ajoutés sont détaillés dans
[l'audit](../audits/app-store-readiness-2026-09-11.md).

Commits backend : `f39ee74` (médias), `9bdf9fa` (Spring/profil), `726c254`
(contrôle de release) ; mobile `9eebdd9`, intégré localement par `32f4413`.

Le [préflight AWS du lot 10](aws-release-preflight-2026-09-12.md) a commencé :
inventaire réel en lecture seule, corrections infra locales et CI complète
préparées. Les [files/alertes/IAM sont désormais appliqués](aws-applied-2026-09-12.md)
avec accord : deux stacks `UPDATE_COMPLETE`, instances et disques préservés.
Un événement EIP legacy non annoncé par l'aperçu a été vérifié et documenté.
Confirmation email SNS vérifiée ; clé de chiffrement et trois paramètres Apple
créés dans SSM. Le backend `5437112` a été poussé, mais le premier pipeline
manuel a échoué avant Maven (prérequis CI ripgrep en cours de correction) :
[passage de configuration](staging-configuration-2026-09-12.md).
La restauration/upgrade est prouvée sur copie locale réelle et le déploiement
journalisé est préparé localement, mais ni SQL staging ni application déployés.
Aucun push mobile/Studio dans cette tranche. La recette fonctionnelle reste ouverte.
L'avertissement de terminaison Surefire reste observé sur le dernier run de
515 tests verts, non réexécuté pour cette tranche d'exploitation AWS.

## Conditions de passage

| Condition | Preuve à joindre avant GO |
| --- | --- |
| Version candidate figée | SHA backend, mobile, Studio ; migrations appliquées ; numéro de build iOS |
| Backend/Studio disponibles | URL réelle, santé worker, SQS/DLQ, SSE et commandes vérifiés sur l'environnement retenu |
| Build natif signé | Archive, profil Apple et entitlement `com.apple.developer.applesignin` ; connexion puis révocation Apple réelles |
| Configuration publique | API/OAuth/support/URL légales renseignés ; liens accessibles avant et après connexion |
| Sécurité médias | Upload, normalisation, autorisation, suppression et reprise vérifiés sur S3 déployé ; IAM restreint |
| Exploitation | Opérateur de modération désigné, procédure urgente, alertes, sauvegarde et restauration testée |
| Recette | Matrice ci-dessous renseignée avec appareil, OS, build, résultat et capture/vidéo |
| Dossier Apple | Accès de revue, ticket exploitable, captures réelles, App Privacy, âge et informations de contact |
| Accord | Validation produit et autorisation explicite avant upload/soumission |

Apple demande actuellement Xcode 26 ou ultérieur et un SDK iOS 26 ou ultérieur
pour les uploads depuis le 28 avril 2026. Ne pas confondre SDK de compilation et
version minimale iOS supportée. Le questionnaire d'âge et, pour l'UE, le statut
de professionnel sont également à vérifier dans App Store Connect.
[Exigences Apple](https://developer.apple.com/news/upcoming-requirements/).

## Préparation technique reproductible

Backend, avec Docker accessible :

```bash
cd /Users/nicolasmaldiney/fragmentsClean
bash scripts/test-release.sh
```

Le profil `release-verification` sélectionne explicitement unitaires, infra et
verticaux (`*Test`, `*Tests`, `*IT`). Le script crée un dossier de rapports neuf et
échoue en présence de tests ignorés. `mvn test` seul ne prouve pas les `*IT` ; une
somme globale de `target/surefire-reports` peut inclure des résultats anciens.
Le script ne remplace pas un test du binaire backend de production avec Java 21.

Mobile :

```bash
cd /Users/nicolasmaldiney/fragmentsCleanFront
npm run test:release:config
npm test -- --runInBand --watchman=false
./node_modules/.bin/tsc --noEmit
./node_modules/.bin/eslint app tests --no-cache
npm run redux:map:check
npm run native:release:check
```

Studio : exécuter `npm test`, `npm run build`, `npm run verify:dist` et
`npm run contract:check` depuis `fragments-admin/fragments-studio` avec le mode
OAuth, les gateways HTTP et une URL HTTPS. Les variables `VITE_*BEARER_TOKEN`
doivent être absentes/vides. Ne pas désactiver le garde-fou pour réutiliser la
configuration opérateur locale.

Les paramètres publics de build mobile sont :

- `EXPO_PUBLIC_API_BASE_URL` ; configuration Google iOS client/redirect déjà prévue ;
- `EXPO_PUBLIC_SUPPORT_EMAIL` ;
- `EXPO_PUBLIC_PRIVACY_POLICY_URL` et `EXPO_PUBLIC_TERMS_URL` : deux URL HTTPS
  sans credentials, obligatoires en production. Aucun domaine fictif de test
  ne doit être utilisé pour la candidate.

Ne jamais placer clé Apple, clé de chiffrement fournisseur ou token Studio dans
Expo. Les prérequis serveur Apple sont décrits dans
[identité et suppression](../architecture/identity-profile-account-deletion.md).

Le dépôt mobile contient un projet `ios/` versionné : vérifier le projet natif
effectivement construit, pas seulement `app.config.js`. La permission Photos et
l'entitlement Apple sont maintenant présents dans les sources ; cela n'active
pas automatiquement la capability sur le compte Apple ni le profil de signature.
À cette revue : Xcode non disponible dans le répertoire développeur actif,
pas de simulateur utilisable, pas de `Podfile.lock` ni de manifeste de
confidentialité applicatif versionné. L'installation des Pods, l'agrégation des
manifestes SDK et les raisons d'accès aux API doivent être contrôlées dans
l'archive. Ne pas inventer des raisons d'accès pour faire passer une validation.

Lancer un build EAS, gérer les credentials, déployer ou téléverser dans
App Store Connect demande un accord distinct. Aucune de ces actions n'a été
effectuée pendant cette préparation.

## Matrice de recette à remplir

Toutes les lignes sont **NON EXÉCUTÉES SUR APPAREIL** au moment de la préparation.
Consigner `build / appareil / iOS / date / résultat / preuve / anomalie` pour chaque
ligne ; au minimum petit et grand iPhone, iOS courant et version précédente retenue.

| Parcours | Résultat attendu |
| --- | --- |
| Premier lancement, Google puis Apple | Session utilisable ; email Apple masqué accepté ; annulation sans blocage |
| Home et onglets | Vraies données ; grand visuel et apparition du bandeau inchangés ; quatre destinations accessibles |
| Localisation refusée/approximative | Contenu utile et carte utilisable ; pas de demande de localisation permanente |
| Carte → fiche → retour | Sélection, zoom et position conservés ; clusters denses et nom long utilisables |
| Expérience sans ticket | Création/publication texte autorisée ; progression conforme aux règles backend |
| Expérience liée à un ticket | Visite cohérente ; niveaux exigeant une preuve non débloqués par du texte seul |
| Ticket valide/invalide/dupliqué | Confirmation ou rejet métier explicites ; pas de double contribution au Pass |
| Panne du moteur ticket | Échec technique distinct, reprise durable, relance Studio ; pas de faux rejet métier |
| Photo appareil/bibliothèque | Refus/accès limité/annulation gérés ; photo JPEG/PNG, aperçu et upload fonctionnels |
| Upload interrompu, 429/5xx, fermeture app | Fichier et commande conservés ; retry sans double publication |
| Socket absent, arrière-plan puis retour | Polling `/commands/{commandId}` réconcilie ; SSE ne transporte que l'invalidation |
| Signalement et blocage | Contenu masqué pour le demandeur ; file Studio ; masquer/restaurer ; déblocage |
| Profil et remplacement avatar | Nouvelle photo visible ; ancien objet nettoyé ; erreur technique retryable |
| Suppression Google puis Apple | Confirmation ; sessions révoquées ; cinq BC effacés ; objets privés supprimés ensuite |
| Hors ligne, redémarrage, A → B → A | Cache par compte ; aucun contenu/outbox privé de A affiché à B |
| Liens légaux et support | Accessibles avant/après connexion selon destination ; pas de lien mort |
| Accessibilité | VoiceOver, grand texte, zones tactiles, contraste, réduction mouvement/transparence, clavier |

Les flows Maestro existants couvrent seulement les lectures et un smoke offline,
pas toute cette matrice. Les exécuter sur une app installée/configurée avec de
vrais `CAFE_ID` et `ARTICLE_SLUG`. Vérifier la prise en charge de l'action avion
sur l'appareil retenu et rétablir le réseau même si le flow échoue. Les tests
Redux/MockMvc ne sont pas un test bout en bout du binaire iOS contre AWS.

## Notes App Review — brouillon à compléter, pas à soumettre tel quel

Fragments aide à découvrir des cafés, enregistrer ses adresses et partager une
expérience, avec ou sans justificatif. Le scan d'un ticket est facultatif pour
publier une expérience ; certains niveaux du Pass exigent une visite vérifiée.

Fournir à l'équipe de revue :

1. Mode d'accès réellement testé, comptes de test et instructions dans l'espace
   sécurisé App Store Connect — aucun mot de passe dans Git. L'OAuth actuel ne
   propose pas de connexion email/mot de passe Fragments : ne pas en promettre une.
2. Café de démonstration publié, ticket original exploitable et procédure de scan.
   Prévoir une remise en état via un workflow normal, sans SQL manuel ni bypass
   caché réservé à Apple. Ne pas proposer un QR si ce parcours n'est pas implémenté.
3. Instructions : ouvrir la fiche café → partager une expérience ; menu de contenu
   → signaler/bloquer ; profil → paramètres → supprimer le compte.
4. Coordonnées de contact surveillées, environnement maintenu disponible pendant
   la revue et description des traitements asynchrones des tickets/suppressions.

La revue exige un accès complet et un backend disponible. Pour l'UGC, vérifier
filtrage, signalement, traitement humain, blocage et contact ; une validation du
format d'image n'est pas un filtrage de son contenu visuel.
[App Review, sections 1.2 et préparation](https://developer.apple.com/app-store/review/guidelines/).

La suppression doit être initiable dans l'app ; la révocation Apple et la
suppression des contenus partagés doivent être vérifiées réellement.
[Suppression de compte](https://developer.apple.com/support/offering-account-deletion-in-your-app/).

## Inventaire App Privacy à valider par le responsable de publication

Ce tableau est un inventaire technique, pas une déclaration réglementaire déjà
validée. Finaliser durées de conservation, sous-traitants, régions d'hébergement
et traitement des sauvegardes ; ne pas déclarer « aucune collecte ».

| Données/usage observables | Vérification avant déclaration |
| --- | --- |
| Identité OAuth, email, nom public, identifiant utilisateur | Données liées au compte pour authentification/profil ; révocation et effacement |
| Avatar, photos, texte, signalements, blocages | Contenu utilisateur lié au compte ; visibilité publique distincte du stockage S3 privé |
| Texte OCR, ticket, café/date, historique de visites et Pass | Informations de visite/achat et contenu utilisateur ; contenu sensible incident possible sur un ticket |
| Coordonnées de découverte | Examiner requêtes et logs réellement conservés, SDK carte et navigation ; permission locale ≠ collecte déclarable à elle seule |
| Favoris, interactions, cache | Distinguer données locales, données serveur et éventuelle télémétrie |
| Journaux, IP, diagnostics et fournisseurs | Vérifier runtime déployé et SDK natifs ; ne pas conclure sur le tracking depuis package.json seulement |

Le moteur ticket actif observé reçoit le texte via un processus local ; cela ne
prouve pas les pratiques internes de ce binaire ou de tous les fournisseurs.
Les réponses App Privacy doivent inclure les partenaires concernés et rester
cohérentes avec la politique publiée.
[Déclarations App Privacy](https://developer.apple.com/app-store/app-privacy-details/).

## Déploiement, rollback et points encore ouverts

Suivre le [runbook d'exploitation](operations-runbook.md) et le
[candidat d'upgrade SQL](app-store-schema-upgrade.md). Le runtime et ses 46 tables
ont été inspectés ; le script global est testé sur le DDL réel avec des données
synthétiques, puis sur une restauration autorisée du vrai backup. Le
[journal et le déploiement contrôlé](journaled-staging-deployment.md) sont
préparés ; l'exécution réelle et un backup frais avant application restent nécessaires.
Ne pas exécuter tous les scripts à l'aveugle, ne pas remplacer une
migration par `schema.sql` sur une base existante. Aucun script de release n'a
été appliqué à distance dans ce lot.

Rollback applicatif vers une image identifiée, en conservant tables, commandes,
jobs et objets requis pour la reprise. Les effacements de comptes et révocations
ne sont pas annulables. Ne pas purger inbox/outbox/legacy pour faire disparaître
un incident. Le legacy sans propriétaire fiable reste inerte, non réattribué.

Restent à fournir/valider : URL légales/support, responsable de modération et
contrôle des images UGC, environnements et IAM, migration déployée, données de revue,
capabilities/credentials Apple, manifeste de confidentialité de l'archive,
build signé et recette complète. Tant que ces preuves manquent, le lot 10 est
**préparé localement mais non clos**.
