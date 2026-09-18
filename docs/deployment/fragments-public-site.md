# Présentation et pages légales Fragments — préparation du 12 septembre 2026

## Mise à jour du 13 septembre — publication bêta

Les trois pages et la feuille CSS sont publiées sur le staging. L'opérateur a
confirmé `studio@anchor-event.fr` et sa redirection vers son Gmail pour recevoir
les demandes. Les textes sont datés du 13 septembre, identifiés `beta` et restent
`noindex,nofollow`. Ils exposent les limites de conservation/restauration et ne
présentent pas les correctifs locaux comme déployés. Publication informative de
la bêta, pas certification de conformité ni autorisation App Store publique.

Les sections suivantes conservent l'historique de préparation du 12 septembre.
Leur état « brouillon/non publié/EAS inchangé » est dépassé pour le site ; les
questions de conformité non résolues restent ouvertes. Preuves actuelles,
variables EAS, version backend et retour arrière :
[dossier TestFlight du 13 septembre](testflight-2026-09-13.md).

Lot complémentaire TestFlight, Astra High (choix opérateur du lot 10).
Demande : une page rudimentaire de confidentialité, puis une présentation du
projet. L'opérateur a choisi le domaine staging existant, pas un nouveau DNS.

## État

**Pages locales préparées ; aucune publication du site, aucun changement EAS.**
Le 12 septembre, après accord explicite, seule la rétention S3 des sauvegardes
Fragments a été appliquée à 30 jours et relue. Voir le
[complément conservation](../audits/privacy-retention-2026-09-12.md).
Présentation du projet et textes légaux marqués comme brouillons. Ne pas utiliser
ces brouillons comme URL de politique validée pour un build de distribution.

Sources statiques : `infra/aws/compose/platform/staging/fragments/public-site/`.
Pas de JavaScript, cookies, fonts distantes, formulaire, analytics ou fausses
captures de l'app. CSS responsive et navigation clavier ; le contenu présente
le projet, pas une offre commerciale ou une disponibilité App Store inventée.

Routes prévues :

- `https://fragments-staging.anchor-event.fr/` : présentation ;
- `/legal/` : même présentation ;
- `/legal/confidentialite.html` : politique ;
- `/legal/conditions.html` : conditions/règles de communauté ;
- `/legal/site.css` : styles locaux.

## Frontières et raccordement

Site purement statique dans le proxy existant, sans bounded context, controller,
SQL, nouvelle image backend ou nouveau service. La proposition Caddy ajoute
uniquement l'accueil exact et `/legal/*`, depuis `/data/fragments-public`.
Elle conserve le reverse proxy API et les deux routes SSE, dont `flush_interval -1`.
La racine `/data` entière, notamment ses certificats, ne doit jamais être exposée.

Avant application réelle : relever et comparer le Caddyfile et les montages
réellement actifs ; le fichier local ne prouve pas l'absence de dérive. Préserver
tous les blocs existants, dont ceux d'Anchor s'il y en a. Publier uniquement les
quatre fichiers HTML/CSS dans le répertoire dédié, jamais `.env`, README, sources
de tests ou fichiers de certificats. Sauvegarder le Caddyfile actif, valider le
candidat dans le vrai conteneur puis effectuer un reload sans redémarrer la
plateforme. Vérifier les routes publiques, API et SSE et disposer d'un retour
au Caddyfile précédent. Aucun de ces changements n'est exécuté dans cette préparation.

## Validation de contenu nécessaire avant publication

1. **Confirmé explicitement par l'opérateur le 12 septembre 2026** : Nicolas
   Maldiney, éditeur à titre personnel et responsable de traitement. Identité
   intégrée aux trois pages ; les autres mentions éventuellement applicables
   restent à vérifier.
2. **Confirmé explicitement pour publication** : `nmaldiney@gmail.com` pour
   le support et la confidentialité. Liens de contact intégrés, y compris
   pour les demandes de suppression hors application et l'exercice des droits.
3. Bases légales, destinataires/garanties de transfert, public visé et organisation
   de modération : propositions signalées, pas affirmations juridiques inventées.
4. Durées/critères de conservation et mise en œuvre réelle, notamment logs,
   événements techniques, sauvegardes et effacement après restauration.
   Sauvegardes : règle d'expiration à 30 jours désormais appliquée. Copies
   personnelles dans l'outbox, logs et restauration : correctif distinct encore
   nécessaire. Ne pas assimiler l'expiration S3 à un effacement complet du compte.

Les textes s'appuient sur les gateways mobile, le flux OAuth, les erasers par
contexte, le stockage média et la sauvegarde réellement inspectés. Le calcul
de proximité observé est local ; cela ne permet pas d'affirmer une absence
universelle de traitement par la carte ou les fournisseurs natifs. La région
AWS Paris n'est pas une garantie d'absence de transfert hors UE pour les partenaires.
La suppression applicative ne prouve pas la purge immédiate des sauvegardes.

La validation de ces points doit précéder le remplacement des marqueurs
`fragments-publication-status=draft` et des encadrés de travail. Il ne suffit
pas d'enlever la mention brouillon pour rendre le texte conforme.

Références de cadrage, consultées le 12 septembre :

- [CNIL — information des personnes](https://www.cnil.fr/fr/conformite-rgpd-information-des-personnes-et-transparence)
- [CNIL — droits des personnes](https://www.cnil.fr/fr/passer-laction/les-droits-des-personnes-sur-leurs-donnees)
- [CNIL — applications mobiles](https://www.cnil.fr/fr/recommandations-applications-mobiles)
- [Apple — App Review Guidelines, 5.1.1](https://developer.apple.com/app-store/review/guidelines/)

Ce travail est une préparation technique/rédactionnelle, pas un avis juridique.

## Tests

```sh
node --test scripts/test-fragments-public-site.mjs
node --test scripts/test-fragments-public-site-infra.mjs
```

5 tests statiques verts après un premier passage rouge (dont la présence de
l'identité et du contact confirmés) ; 1 test d'intégration
vert avec deux vrais Caddy locaux, backend remplacé par une réponse technique
de test. Vérifie HTML/CSS, liens, langue/viewport, absence de scripts/ressources
distantes, état brouillon, règles Experience/Pass, headers, erreurs 404 et
routage API/SSE. Ce n'est pas une preuve de streaming SSE métier ni une recette
du backend : aucune logique Java/Redux n'est modifiée, leurs suites ne sont
pas relancées pour cette page. Conteneurs/réseau de test supprimés après exécution.

Après confirmation de l'éditeur : les 5 tests statiques sont relancés et verts.
Le test Caddy d'intégration n'est pas relancé pour cette modification de texte
et de liens `mailto:` ; le routage n'a pas changé. Aucune politique de purge
ni modification AWS/EAS n'est appliquée par cette confirmation.

Le premier Chrome headless a produit une capture desktop mais atteint son délai
de terminaison ; l'image a pu être ouverte et contrôlée. Ne pas déclarer le
processus navigateur entièrement vert sur cette seule capture.

Le contrôle mobile distinct via Chrome DevTools Protocol est terminé avec
succès : viewport de 390 px, largeur du document de 390 px, capture complète
de 3 737 px de hauteur inspectée visuellement. Aucun débordement horizontal ;
navigation et cartes correctement empilées. Profil navigateur isolé et
conteneur de prévisualisation supprimés après contrôle.

Captures locales temporaires (non versionnées) :
`/private/tmp/fragments-public-desktop.png` et
`/private/tmp/fragments-public-mobile.png`.

La durée active de cette tranche n'est pas isolée. La mise en ligne reste
conditionnée aux informations/choix de l'éditeur ; elle n'est pas assimilée au
temps du futur build TestFlight.
