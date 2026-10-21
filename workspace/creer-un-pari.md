# Fonctionnalité : créer un pari

Source : `workspace/event-storming.md` (Big Picture V1). Cette fiche rassemble ce que l'atelier a
établi sur la création d'un pari et les questions qu'il a laissées ouvertes. Elle sert d'entrée à
l'atelier BDD, qui doit en tirer des règles précises et des exemples chiffrés.

## Contexte

- **Bounded context** : `betting` (en aval de `members`).
- **Acteur** : le créateur, un membre dont l'email est confirmé. Il n'existe qu'un seul type de membre,
  qui peut tour à tour être créateur, invité ou parieur. Il n'y a pas d'administrateur en V1.
- **Commande** : Créer un pari (issues, date de clôture, commission, liste d'accès éventuelle).
- **Fait produit** : **Pari créé** ★ (pivot qui ouvre la phase « Prise de mises »).
- **Agrégat** : Pari, qui porte ses invitations et fabrique les mises.

## Ce que l'atelier a établi

- Un pari est **mutuel** : les mises sur toutes les issues forment une cagnotte, que les gagnants se
  partagent au prorata après le prélèvement de la commission du créateur.
- Monnaie **virtuelle** : les jetons sont des entiers, sans argent réel.
- À la création, on fixe :
  - les **issues** ;
  - la **date de clôture** ;
  - la **commission** du créateur ;
  - éventuellement une **liste d'accès**.
- Après la création :
  - la commission et la date de clôture ne changent plus ;
  - seule exception, la clôture anticipée par le créateur, qui reste toujours possible.
- **Pari sans liste** : il est ouvert à tous. **Pari avec liste** : seuls les invités qui ont accepté
  peuvent miser.
- On n'invite que des membres dont l'email est confirmé. L'email confirmé est requis pour **toute**
  action, création comprise.
- Le créateur ne mise pas sur son propre pari (sujet hors de la création, mais lié au créateur).
- Le créateur déclarera lui-même le résultat, après la clôture. La V1 repose sur la confiance, sans
  contestation possible.
- Plus tard, au règlement, la commission sera **plafonnée** pour que chaque gagnant récupère au moins
  sa mise. Si personne n'a misé sur l'issue gagnante, le créateur touche quand même sa commission et le
  reste est détruit.

## Read models liés

- **Parieur, avant de miser** : le détail du pari (issues, date de clôture, commission, totaux misés
  par issue).
- **Membre** : la liste des paris ouverts à tous, plus ceux dont il a accepté l'invitation.
- **Créateur** : ses paris dont la date de clôture n'est pas passée (pour annuler ou clôturer par
  anticipation).

## Questions ouvertes à explorer

1. **Issues**
   - Combien au minimum ? Au maximum ?
   - Deux issues peuvent-elles porter le même libellé ?
   - Un libellé vide est-il accepté ?
2. **Date de clôture**
   - Doit-elle être dans le futur ? Avec quel délai minimal, et existe-t-il un délai maximal ?
   - À l'instant exact de la clôture, peut-on encore miser ?
3. **Commission**
   - Quelle forme : pourcentage de la cagnotte, ou montant fixe ?
   - Quelles bornes (0 % autorisé ? un maximum ?) ?
   - Doit-elle être un entier ?
4. **Liste d'accès**
   - Est-elle fixée à la création, ou le créateur invite-t-il ensuite au fil de l'eau ? (L'atelier dit
     les deux : « liste éventuelle fixée à la création », mais aussi une commande « Inviter » séparée.)
   - Un pari créé ouvert peut-il devenir restreint, ou l'inverse ?
5. **Contenu du pari** : un titre, une description ou l'événement réel concerné sont-ils nécessaires ?
6. **Créateur** : y a-t-il une limite au nombre de paris ouverts par créateur ? La création coûte-t-elle
   des jetons ?
7. **Email non confirmé** : un membre non confirmé peut-il seulement se connecter ? Si la connexion
   l'exige, tout créateur est confirmé et `betting` n'a rien à vérifier à la création. Sinon, que voit
   ce membre quand il tente de créer un pari ?

## Hors périmètre de cette fonctionnalité

- Invitations : envoi, réponses et retraits. C'est là que `betting` devra savoir quels membres sont
  confirmés, via sa copie alimentée par le fait *Email confirmé*.
- Mises et saga de débit.
- Clôture anticipée.
- Résultat, règlement et annulation.
- Hot spot H4 (erreur sur la date de clôture remarquée après la clôture) : il touche la date fixée à
  la création, mais sa résolution concerne la clôture et l'annulation.
