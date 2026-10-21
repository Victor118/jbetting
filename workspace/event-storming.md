# Event Storming Summary: jbetting (paris entre utilisateurs, monnaie virtuelle) — Big Picture V1

## Workshop Mode
Guided (mode guidé). Atelier mené en français, parti de zéro, sans aucun bounded context présupposé.

## Final Progression Table

| # | Activity              | Status | Artifacts |
|---|-----------------------|--------|-----------|
| 1 | Chaotic Exploration   | done   | 15 faits initiaux, 5 règles, 2 points ouverts |
| 2 | Timeline Ordering     | done   | 2 lignes (utilisateur, pari) + 1 branche (annulation), 5 règles d'ordre |
| 3 | Hot Spots             | done   | 5 hot spots (1 hors V1, 1 accepté, 3 à trancher) + 4 règles |
| 4 | Pivotal Events        | done   | 5 événements pivots, 5 phases |
| 5 | Commands & Actors     | done   | 21 lignes acteur → commande → fait |
| 6 | Policies              | done   | 10 politiques (dont 3 dérivées ; PO4, PO5, PO11 fusionnées dans le règlement le 2026-10-21), 1 politique devenue règle |
| 7 | Read Models           | done   | 9 read models, 1 règle de confidentialité |
| 8 | External Systems      | done   | 1 système externe (service de mail existant) |
| 9 | Aggregates            | done   | 3 agrégats (Utilisateur, Pari, Mise) |
| 10| Bounded Contexts      | done   | 2 contextes (members, betting) + 1 évolution envisagée |

## Décisions de cadrage
- Monnaie **virtuelle** (jetons entiers), pas d'argent réel, pas de contrainte réglementaire en V1.
- Paris **mutuels** créés par les utilisateurs eux-mêmes ; le créateur déclare le résultat (confiance, pas de contestation en V1).
- Objectif : périmètre simple pour la première version.

## Décisions complémentaires (2026-10-21)
- **Aucun gagnant** : si personne n'a misé sur l'issue gagnante, le créateur touche quand même sa commission ; le reste de la cagnotte est **détruit** (burn). *Remplace « le créateur prend tout ».*
- **Arrondis (H6)** : les restes du partage au prorata sont **détruits**.
- **Recharge (H5)** : une fois par mois, plancher fixe (par exemple 1000 jetons). Les soldes en dessous sont remis au plancher par **création** de jetons ; ceux au-dessus ne bougent pas. Risque accepté : un joueur peut tout miser juste avant une recharge. Pas de bonus d'activité.
- **Invalidation différée des mises** : conservée (mise acceptée temporairement, validée au règlement).
- **Règlement entre contextes** : nouveau fait **« Pari réglé »**, publié par betting après la déclaration du résultat. Il porte la liste des versements (bénéficiaire, montant, motif : gains, commission ou restitution) et le montant détruit. members le traite et crédite les soldes ; le traitement est **idempotent** (identifiant du pari) et **aucun événement ne doit être perdu**.
- **Hors V1** : taxe sur les jetons inactifs.

## Révision du 2026-10-21 : le solde passe dans betting

Décidée après l'atelier. Elle **prime** sur les sections qui suivent quand elles se contredisent ;
celles-ci restent telles que l'atelier les a produites.

**Constat** : miser, l'action la plus fréquente, traversait deux contextes (débit dans members, mise dans
betting), d'où une saga asynchrone (débit → mise placée ou refusée → recrédit) et une réponse différée
au parieur. Les jetons ne servent qu'au jeu : members n'en a pas besoin pour gérer un compte.

**Décision**
- **members** = l'identité : inscription, email confirmé, connexion. Agrégat Membre, **sans solde**.
- **betting** = le jeu. Nouvel agrégat **Joueur** (`Player`) : solde dépensable, historique des jetons,
  recharges. Tout membre confirmé a un Joueur ; créateur, invité et parieur sont des rôles du Joueur.
- **Seul fait entre contextes** : *Email confirmé* (members → betting). betting y réagit en créant le
  Joueur et en créditant les jetons de bienvenue (PO1, désormais dans betting). Le fait ne doit pas être
  perdu ; le consommateur est idempotent (un Joueur par membre).
- **Miser** est synchrone : une **transaction locale** vérifie les règles du Pari (qui fabrique la Mise
  sans être modifié), débite le Joueur et enregistre la Mise. C'est une exception assumée à « un agrégat
  modifié par transaction » : réponse immédiate au parieur, seul le solde du Joueur est verrouillé.
- **Disparaissent** : « Jetons débités pour une mise », « Mise refusée » (un refus est désormais une
  erreur renvoyée au parieur, sans fait publié), « Jetons recrédités » après un refus, PO9 et PO10.
- **Deviennent internes à betting** : « Jetons de bienvenue crédités » et « Jetons rechargés » (PO2), le
  règlement (« Pari réglé » et PO14 ne traversent plus de frontière ; l'idempotence d'un versement par
  joueur, pari et motif reste une règle du Joueur), les restitutions à l'annulation (PO6), le read model
  R8 (solde et historique).
- **À trancher avec le règlement** : créditer les joueurs en une transaction, ou une par joueur via un
  fait interne.
- **Évolution envisagée** : si betting est un jour découpé, le Joueur ira avec la zone « mises ».

## Domain Events [ORANGE] — 21 faits

### Phase 1 : Entrée (de l'inscription à l'email confirmé)
- Utilisateur inscrit — un visiteur s'est inscrit avec son email (mot de passe géré par jbetting)
- **Email confirmé** ★ pivot — l'utilisateur a cliqué sur le lien reçu par mail ; requis pour toute action
- Jetons de bienvenue crédités — à la confirmation de l'email
- Jetons rechargés — chaque mois, les soldes sous le plancher (par exemple 1000) y sont remis par création de jetons ; les autres ne bougent pas

### Phase 2 : Prise de mises (de la création à la clôture)
- **Pari créé** ★ pivot — avec ses issues, sa date de clôture, sa commission et éventuellement une liste d'accès
- Utilisateur invité au pari — utilisateur à l'email confirmé ; invitation par mail et dans l'application
- Invitation acceptée
- Invitation refusée
- Utilisateur retiré de la liste d'accès — le créateur peut retirer n'importe qui
- Jetons débités pour une mise — le solde est débité avant que la mise soit placée
- Mise placée sur une issue — un parieur peut ajouter plusieurs mises, jamais en retirer
- Mise refusée — le pari n'accepte plus de mises (clos ou annulé)
- Jetons recrédités — après un refus ou une invalidation

### Phase 3 : Attente du résultat (de la clôture au résultat)
- ★ pivot : **la date de clôture est atteinte** (c'est un état, pas un fait émis), ou bien :
- Mises clôturées par anticipation — le créateur a clôturé plus tôt (par exemple si l'horaire de l'événement réel a changé)

### Phase 4 : Règlement
- **Résultat du pari déclaré** ★ pivot — par le créateur, uniquement après la clôture
- Mise invalidée — au règlement, une mise non valide est écartée (parieur absent de la liste, mise postérieure à la clôture)
- **Pari réglé** (betting) — liste des versements (bénéficiaire, montant, motif : gains, commission, restitution d'une mise invalidée) et montant détruit (cagnotte sans gagnant, restes d'arrondi)
- Commission versée au créateur (members, en traitant « Pari réglé »)
- Gains versés aux gagnants (members, en traitant « Pari réglé ») — au prorata ; si personne n'a misé sur l'issue gagnante, seule la commission est versée et le reste est détruit
- Invitations en attente retirées

### Phase 5 : Annulation (fin alternative, seulement avant la clôture)
- **Pari annulé** ★ pivot
- Mises restituées
- (Invitations en attente retirées)

## Commands & Actors [BLUE] / [YELLOW]

Un seul type d'utilisateur, qui joue les rôles Créateur, Invité et Parieur. Pas d'administrateur en V1.

| Actor | Command | Resulting Event |
|-------|---------|-----------------|
| Visiteur | S'inscrire avec son email | Utilisateur inscrit |
| Utilisateur | Confirmer son email (clic sur le lien) | Email confirmé |
| Système (PO1) | Créditer les jetons de bienvenue | Jetons de bienvenue crédités |
| Tâche périodique (le temps) | Remettre les soldes à niveau | Jetons rechargés |
| Créateur | Créer un pari (issues, date de clôture, commission, liste éventuelle) | Pari créé |
| Créateur | Inviter un utilisateur à l'email confirmé | Utilisateur invité au pari |
| Invité | Accepter l'invitation | Invitation acceptée |
| Invité | Refuser l'invitation | Invitation refusée |
| Créateur | Retirer un utilisateur de la liste | Utilisateur retiré de la liste d'accès |
| Parieur | Miser sur une issue | Jetons débités pour une mise |
| Système (PO9) | Placer la mise (le Pari fabrique la Mise) | Mise placée sur une issue / Mise refusée |
| Système (PO10) | Recréditer les jetons d'une mise refusée | Jetons recrédités |
| Créateur | Clôturer les mises par anticipation | Mises clôturées par anticipation |
| Créateur | Déclarer le résultat | Résultat du pari déclaré |
| Système (PO12) | Régler le pari (valider les mises, calculer les versements et le montant détruit) | Mise invalidée, Pari réglé |
| Système (PO14) | Créditer les soldes selon les versements | Commission versée au créateur, Gains versés aux gagnants, Jetons recrédités |
| Créateur | Annuler le pari | Pari annulé |
| Système (PO6) | Restituer les mises | Mises restituées |
| Système (PO7) | Retirer les invitations en attente | Invitations en attente retirées |
| Système (PO8, PO13) | Envoyer un mail (invitation, lien de confirmation) | — (service de mail externe) |

## Policies [LILAC]

| When (Event) | Then (Command) | Rule |
|--------------|----------------|------|
| Utilisateur inscrit | Envoyer le lien de confirmation par mail | PO13 (dérivée de l'activité 8) |
| Email confirmé | Créditer les jetons de bienvenue | PO1 |
| Échéance mensuelle atteinte | Remettre au plancher les soldes qui sont en dessous | PO2 (plancher fixe, par exemple 1000 ; création de jetons) |
| Utilisateur invité au pari | Envoyer le mail d'invitation | PO8 |
| Jetons débités pour une mise | Placer la mise sur le pari | PO9 (dérivée de l'activité 9) |
| Mise refusée | Recréditer les jetons | PO10 |
| Résultat du pari déclaré | Régler le pari : valider les mises, calculer les versements et le montant détruit | PO12 (betting ; remplace PO4, PO5 et PO11) |
| Pari réglé | Créditer les soldes, un crédit par versement | PO14 (members ; idempotent par versement : membre, pari, motif ; aucun événement perdu) |
| Pari annulé | Restituer les mises | PO6 |
| Résultat déclaré ou Pari annulé | Retirer les invitations en attente | PO7 |

PO3 (« date de clôture atteinte → clôturer ») est devenue une **règle** : miser est refusé une fois la date atteinte, après une clôture anticipée ou après une annulation.
Le crédit des gains et de la commission peut intervenir un peu après la déclaration du résultat (délai accepté).

## Read Models [GREEN]

| Actor | Needs to See | Before Command |
|-------|-------------|----------------|
| Parieur | Détail du pari : issues, date de clôture, commission, **totaux misés par issue**, ses mises, son solde | Miser |
| Utilisateur | Paris ouverts à tous + paris dont il a accepté l'invitation | Chercher un pari où miser |
| Invité | Invitations en attente : pari, créateur, issues, date de clôture, commission | Accepter / Refuser |
| Créateur | Recherche d'un utilisateur à l'email confirmé | Inviter |
| Créateur | Liste des invités avec leur réponse (et s'ils ont misé — voir hot spot) | Retirer de la liste |
| Créateur | Ses paris dont la date de clôture est passée, en attente de résultat | Déclarer le résultat |
| Créateur | Ses paris dont la date de clôture n'est pas passée | Annuler / Clôturer par anticipation |
| Utilisateur | Son solde et l'historique de ses jetons (un mouvement par ligne : membre, pari, motif, montant) | (en général) |
| Parieur | Pari terminé : résultat, ses mises, ses gains (un pari terminé reste consultable) | Consulter |

Règle de confidentialité : le créateur ne voit que les **totaux par issue**, jamais qui a misé sur quoi.

## External Systems [PINK]

- **Service de mail existant** (on ne le construit pas) : il envoie le lien de confirmation (qui mène à « Email confirmé ») et le mail d'invitation à un pari (PO8).
- Aucun autre : la connexion se fait par mot de passe géré par jbetting ; pas de source externe de résultats ; pas de contrainte réglementaire.

## Aggregates [LARGE YELLOW]

### Utilisateur (avec son solde) — members
- **Commands:** S'inscrire, Confirmer son email, Créditer les jetons de bienvenue, Remettre le solde à niveau, Débiter pour une mise, Recréditer, Créditer la commission, Créditer les gains, Restituer une mise
- **Events:** Utilisateur inscrit, Email confirmé, Jetons de bienvenue crédités, Jetons rechargés, Jetons débités pour une mise, Jetons recrédités, Commission versée au créateur, Gains versés aux gagnants, Mises restituées
- **Invariants:**
  - email unique ;
  - email confirmé requis pour toute action ;
  - jetons entiers ;
  - jamais de débit supérieur au solde ;
  - la recharge mensuelle remet au plancher un solde qui est en dessous (création de jetons), et ne touche pas un solde au-dessus ;
  - un versement n'est appliqué qu'une seule fois : sa clé est (pari, motif) pour ce membre ; un versement déjà appliqué est ignoré.
- **Traitement de « Pari réglé » (décidé le 2026-10-21)** : le fait est éclaté en un crédit par versement, chacun modifiant **un seul** Utilisateur dans sa propre transaction (pas de transaction englobant tous les parieurs). Chaque mouvement de jetons est une ligne de l'historique (read model R8), unique par (membre, pari, motif). Le message n'est acquitté qu'une fois tous les crédits appliqués ; en cas de relivraison, les crédits déjà appliqués sont ignorés (livraison au moins une fois, effet exactement une fois).

### Pari (avec ses invitations) — betting
- **Commands:** Créer, Inviter, Accepter / Refuser l'invitation, Retirer de la liste, Placer une mise (fabrique de Mise), Clôturer par anticipation, Déclarer le résultat, Vérifier la validité des mises, Annuler, Retirer les invitations en attente
- **Events:** Pari créé, Utilisateur invité au pari, Invitation acceptée, Invitation refusée, Utilisateur retiré de la liste d'accès, Mise refusée, Mises clôturées par anticipation, Résultat du pari déclaré, Pari annulé, Invitations en attente retirées
- **Invariants:**
  - issues, date de clôture, commission et liste éventuelle sont fixées à la création ; la commission et la date ne se modifient pas (seule exception : la clôture anticipée) ;
  - pas de nouvelle mise après la date de clôture, une clôture anticipée ou une annulation ;
  - le créateur ne mise pas sur son propre pari ;
  - pari avec liste : seuls les invités ayant accepté peuvent miser ; pari sans liste : ouvert à tous ;
  - on n'invite que des utilisateurs à l'email confirmé ; pas de réinvitation sur le même pari après un refus ;
  - le créateur peut retirer n'importe qui de la liste ;
  - on ne peut plus accepter une invitation à un pari terminé ;
  - annulation seulement avant la clôture ;
  - résultat seulement après la clôture, et jamais après une annulation ;
  - règlement en pari mutuel : prorata ; mises invalidées exclues (et restituées) ; commission limitée pour que chaque gagnant récupère au moins sa mise ; si personne n'a misé sur l'issue gagnante, le créateur touche sa commission et le reste est détruit ; les restes d'arrondi sont détruits ;
  - conservation : somme des versements + montant détruit = total des mises du pari.
- Le Pari n'est **pas modifié** quand on mise : il vérifie ses règles et fabrique la Mise.

### Mise — betting
- **Commands:** (créée par le Pari) ; invalidée au règlement
- **Events:** Mise placée sur une issue, Mise invalidée
- **Invariants:**
  - une mise ne se retire pas, on peut seulement en ajouter ;
  - montant entier ;
  - elle est **acceptée temporairement**, puis validée au règlement : elle est invalidée si le parieur n'est plus sur la liste, ou si elle est postérieure à la clôture.
- Les totaux par issue peuvent brièvement inclure une mise qui sera invalidée (accepté).

## Bounded Context Map

### members
- **Aggregates:** Utilisateur (avec son solde)
- **Ubiquitous Language:** membre / compte, inscription, email confirmé, solde, jetons, jetons de bienvenue, recharge (remise à niveau). Ici, « utilisateur » = un compte de l'application.
- **Relationships:**
  - publie *Email confirmé* et *Jetons débités pour une mise* vers betting ;
  - consomme *Mise refusée*, *Pari réglé* et *Pari annulé* depuis betting ;
  - utilise le service de mail.
  - Type de relation : **members est en amont, betting en aval** (betting dépend de members — décidé le 2026-10-21).

### betting
- **Aggregates:** Pari, Mise
- **Ubiquitous Language:** pari, issue, date de clôture, clôture anticipée, commission, liste d'accès, invitation, mise, mise invalidée, résultat, règlement, pari mutuel, cagnotte. Ici, « utilisateur » = créateur (fournisseur du résultat), invité ou parieur. « Clos » = résultat déclaré ou annulé ; « mises clôturées » = on n'accepte plus de mises.
- **Relationships:**
  - consomme *Email confirmé* et *Jetons débités pour une mise* depuis members ;
  - publie *Mise refusée*, *Pari réglé* et *Pari annulé* ;
  - utilise le service de mail (invitations).

Communication **uniquement par faits publiés**, avec un délai accepté. Pas d'appel synchrone entre contextes.
Garantie de livraison : aucun fait publié ne doit être perdu (« Pari réglé » en particulier) ; le consommateur est idempotent.

### Évolution envisagée (après la V1)
Séparer betting en deux :
- une zone « pari / événement » : issues, invitations, clôture, résultat ; « utilisateur » = créateur et fournisseur du résultat ;
- une zone « mises » : « utilisateur » = parieur.

Points à résoudre avant ce découpage :
- le Pari ne pourrait plus fabriquer directement la Mise ; il faudrait une copie locale du pari dans la zone « mises » ;
- trouver où placer les invitations et le règlement ;
- trouver un autre nom que « event », qui se confond avec les événements métier.

## Hot Spots / Open Questions [PINK]

- H1 — Le créateur ne déclare jamais de résultat : les mises restent bloquées (l'annulation est impossible après la clôture). **Hors V1.**
- H2 — Un créateur mise sur son propre pari via un autre compte. **Risque accepté.**
- H4 — Erreur sur la date de clôture remarquée **après** la clôture : on ne peut plus annuler. **À trancher.**
- H5 — Recharge : **tranché** (mensuelle, plancher fixe, soldes au-dessus inchangés). Risque accepté : tout miser juste avant une recharge.
- H6 — Restes du partage au prorata : **tranché** (détruits).
- Taxe sur les jetons inactifs : **hors V1.**
- Restitution des mises à l'**annulation** : passe-t-elle aussi par un fait porteur des versements (comme « Pari réglé »), ou reste-t-elle portée par « Pari annulé » ? **À trancher.**
- Notifications à l'annulation d'un pari et quand un invité répond : **ouvert** (pas de notification du résultat en V1).
- Read model « liste des invités » : montrer au créateur qui a misé contredit-il la règle « le créateur ne voit que les totaux » ? Depuis que le créateur peut retirer n'importe qui, cette information n'est plus nécessaire. **À trancher.**
- Règlement entre contextes : **tranché** (« Pari réglé »).
- Relation entre members et betting : **tranché** (members en amont, betting en aval).
- Connexion et mot de passe (oubli, réinitialisation) : non explorés.

## Coverage Matrix

Aucune spec ni user story n'était fournie : la matrice de couverture n'est pas applicable. Les 5 phases (entrée, prise de mises, attente du résultat, règlement, annulation) sont couvertes par des faits, des commandes et des politiques.
