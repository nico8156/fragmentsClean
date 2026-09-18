# P1 FR-023 / FR-024 — certification App Store et secrets

Date : 2026-09-18  
Backend : branche `fix/p1-release-certification`, base `75d0690`  
Mobile : branche `fix/p1-release-certification`, base `938faf5`  
Studio : branche `fix/studio-secret-history-gate`, base `1a11982`

## Résultat

FR-024 dispose désormais d'une preuve historique et d'un gate reproductible.
FR-023 est techniquement préparé, mais ne peut être déclaré fermé avant la
nouvelle candidate EAS/TestFlight et sa recette humaine : une suite JavaScript
ou l'inspection d'une ancienne IPA ne remplace pas une preuve sur appareil.

## FR-024 — historique des secrets

Scanner utilisé : `zricethezav/gitleaks:v8.28.0`, digest :
`sha256:cdbb7c955abce02001a9f6c9f602fb195b7fadc1e812065883f695d1eeaba854`.

Commandes, exécutées sur chaque dépôt :

```text
docker run --rm --network none --read-only -v <repository>:/repo:ro \
  zricethezav/gitleaks:v8.28.0@sha256:cdbb7c955abce02001a9f6c9f602fb195b7fadc1e812065883f695d1eeaba854 \
  git --log-opts=--all --redact=100 \
  --no-banner --no-color --exit-code=1 /repo
```

Premier passage, valeurs intégralement masquées :

| Dépôt | Commits scannés | Candidats |
| --- | ---: | ---: |
| Backend | 405 | 3 |
| Mobile | 376 | 4 |
| Studio | 93 | 1 |

Classification, sans reproduire les valeurs :

- backend : deux clés privées synthétiques de tests ; un ancien secret OAuth
  Google. Une comparaison en mémoire, sans affichage, prouve que ce dernier
  diffère du SecureString `/fragments/staging/GOOGLE_STUDIO_CLIENT_SECRET` ;
- mobile : deux JWT synthétiques de tests et deux checksums CocoaPods ;
- Studio : ancien bearer d'un `.env.local` versionné. Le namespace SSM staging
  ne contient plus `ADMIN_SECURITY_TOKEN` et les builds publics interdisent les
  variables `VITE_*BEARER_TOKEN`.

Le filtre backend statique correspondant était devenu du code mort : la chaîne
active est OAuth/JWT avec allowlist `admin_user_access`. Le filtre, sa propriété,
la variable Compose et le contrat SSM ont été retirés. Les tests de contrôleurs
standalone sont explicitement séparés de `AdminAccessSecurityIT`, qui exerce la
chaîne de sécurité réelle avec PostgreSQL/Testcontainers.

Les fingerprints exacts sont bornés dans `.gitleaksignore`. Ils ne constituent
pas une exclusion par chemin/règle : une nouvelle valeur au même endroit reste
bloquée. Après classification, les trois scans sont verts. Les workflows font
un checkout `fetch-depth: 0` et utilisent l'image épinglée par digest avec
redaction totale, montage du dépôt en lecture seule et réseau coupé.

Cette preuve ne lit pas les anciens artefacts EAS, logs CloudWatch/SSM ou
backups. Leur inventaire/rétention reste une action d'exploitation ; aucune
absence de secret n'est affirmée au-delà des historiques Git scannés.

## FR-023 — archive et crash reporting

L'IPA EAS de référence consultée est le build
`7300b4d2-fa83-436b-9d4e-77624b692fac`, version `1.0.0 (7)`, commit mobile
`f72396ec68075986fc36bb05907e091d5a475263`. Elle précède ce lot et ne constitue
donc pas la candidate finale.

L'inspection par `npm run native:ipa:inspect` a établi :

- SHA-256 `ed32abf377841ea8f8e48dd463eb44c295b3affa3b481c9f47efc9aa97cc01c4` ;
- bundle `com.nico8156.fragments`, équipe `GZU3V23673` ;
- Apple Sign-In `Default`, `get-task-allow=false`, beta reports active ;
- bundle JavaScript embarqué ;
- 12 manifests de confidentialité, dont le manifeste agrégé de l'app ;
- structure/signature non altérée. La chaîne locale du certificat de
  distribution n'était pas approuvée (`CSSMERR_TP_NOT_TRUSTED`), état distingué
  d'une signature invalide ; l'artefact avait été accepté par App Store Connect.

Le mobile ajoute maintenant :

- `@sentry/react-native` avec Metro, scripts Xcode de source maps/dSYM et Gradle ;
- DSN public, organisation/projet de build et `SENTRY_AUTH_TOKEN` secret requis
  pour toute configuration production ;
- aucun token Sentry dans Expo `extra` ou Git ;
- PII, tracing et session replay désactivés ; user, requête, breadcrumbs et
  contexte extra supprimés par `beforeSend` ;
- `eas.json` exige un commit propre et l'environnement EAS `production` ;
- inspecteur IPA et modèle de preuve de recette versionnés.

L'installation CocoaPods locale n'a pas pu être exécutée : la machine ne
dispose pas de Xcode complet (`xcode-select` pointe vers CommandLineTools) et la
locale Ruby/CocoaPods n'est pas UTF-8. EAS doit compiler cette intégration dans
une VM propre. Cet échec est conservé et n'est pas présenté comme un test natif
vert.

## Conditions restantes avant fermeture FR-023

1. créer le projet Sentry et renseigner DSN, organisation, projet et auth token
   dans l'environnement EAS production ;
2. mettre à jour la politique publiée et App Privacy pour le diagnostic Sentry ;
3. construire une IPA EAS depuis le commit mobile final et conserver son id ;
4. vérifier les logs d'upload source maps/dSYM et exécuter l'inspecteur IPA ;
5. provoquer un diagnostic contrôlé sur une candidate TestFlight puis vérifier
   sa symbolication jusqu'à la ligne TypeScript ;
6. remplir la matrice appareil, notamment Apple/Google, suppression, UGC,
   médias, offline, VoiceOver et tailles de texte.

## Vérifications locales du lot

- backend : 27 tests ciblés contrôleurs/workflow verts ; 2 tests d'intégration
  OAuth/JWT admin verts avec PostgreSQL/Testcontainers ;
- mobile : 94 suites / 357 tests, 13 tests de configuration release, contrôle
  natif et carte Redux verts ; export iOS production synthétique réussi ;
- Studio : 35 fichiers / 164 tests, build OAuth sans bearer et trois contrôles
  de workflow verts ; le build avec les anciens bearers locaux a été refusé
  comme prévu ;
- Gitleaks final après commits : backend 406 commits, mobile 377, Studio 94 ;
  aucun secret non classifié après application des fingerprints exacts.

La tentative CocoaPods locale reste non concluante faute de Xcode complet. Les
21 alertes npm mobiles restantes sont `MODERATE` dans deux chaînes Expo ; aucun
`HIGH` ou `CRITICAL` n'est accepté par le gate.

## FlowAtlas

FlowAtlas n'a pas été utilisé : historique Git, IPA, profil de signature,
manifeste Apple et symbolication ne sont pas des graphes métier Redux/Java. Les
outils probants sont Gitleaks, EAS, les outils Apple, l'inspecteur d'archive et
la recette appareil. Son usage aurait été symbolique pour ces deux constats.
