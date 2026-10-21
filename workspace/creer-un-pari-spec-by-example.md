# Spécification par l'exemple : Créer un pari

Issue de l'atelier BDD (2026-10-21) mené sur `workspace/creer-un-pari.md`, 7 questions sur 8.

## Personas impliqués
- [PO] : la création reste gratuite, sans limite de paris ; le coût de création est une piste pour plus tard.
- [BA] : bornes de la commission et des issues ; faits publiés à la création, invitations ; frontière
  avec `members` et avec le service de mail.
- [DEV] : « strictement futur » à la seconde près avec l'horloge (port), pari privé déduit de la liste
  et non d'un mode, refus qui n'enregistre et ne publie rien.
- [QA] : bornes 2/100 issues et 0 %/10 %, doublons sans casse ni espaces, créateur dans sa propre liste,
  invité en double, invité qui n'est pas joueur.
- [USER] : un pari qui clôt une seconde après sa création n'a aucun intérêt, d'où l'idée d'un coût de
  création plus tard.

## Valeurs de référence
- Horloge : **2026-10-21T14:00:00Z** (« maintenant »).
- Joueurs : **Alice** (créatrice), **Bob** et **Carole**. **Martin** et **Zoé** sont des membres sans
  Joueur dans `betting`.
- **Pari simple** : titre « Qui gagne la finale ? », issues « Lyon » et « Paris », clôture
  2026-10-22T14:00:00Z (J+1), commission **5 %**, sans description et sans invités.

```gherkin
# language: fr
Fonctionnalité: Créer un pari
  Contexte borné : betting (écriture). Un joueur crée un pari mutuel avec un titre, des issues, une date
  de clôture, une commission et, éventuellement, une liste d'invités.

  Contexte:
    Étant donné qu'il est 2026-10-21T14:00:00Z
    Et qu'Alice est une joueuse

  Règle: Seul un joueur peut créer un pari

    Scénario: La création par un membre qui n'est pas joueur est refusée
      Étant donné que Martin n'est pas un joueur
      Quand Martin crée le pari simple
      Alors la création est refusée pour le motif « le créateur n'est pas un joueur »
      Et aucun pari n'est enregistré
      Et aucun fait n'est publié
      # Couvre l'email non confirmé comme « Email confirmé » pas encore traité :
      # dans betting, les deux situations sont indiscernables.

  Règle: Créer un pari publie « Pari créé » avec son contenu ; sans invités, le pari est ouvert à tous

    Scénario: Créer un pari publie « Pari créé » avec tout son contenu
      Quand Alice crée un pari :
        | titre      | Qui gagne la finale ? |
        | issues     | Lyon, Paris           |
        | clôture    | 2026-10-22T14:00:00Z  |
        | commission | 5 %                   |
      Alors le fait « Pari créé » est publié avec :
        | créateur    | Alice                 |
        | titre       | Qui gagne la finale ? |
        | description | (aucune)              |
        | issues      | Lyon, Paris           |
        | clôture     | 2026-10-22T14:00:00Z  |
        | commission  | 5 %                   |
        | invités     | (aucun)               |
      Et aucun fait « Joueur invité au pari » n'est publié

    Scénario: La création d'un pari ne coûte aucun jeton
      Étant donné qu'Alice a un solde de 0 jeton
      Quand Alice crée le pari simple
      Alors le fait « Pari créé » est publié
      Et le solde d'Alice est toujours de 0 jeton

  Règle: Un pari a un titre non vide

    Plan du scénario: Un pari sans titre est refusé
      Quand Alice crée le pari simple avec le titre "<titre>"
      Alors la création est refusée pour le motif « titre manquant »
      Et aucun pari n'est enregistré
      Et aucun fait n'est publié

      Exemples:
        | titre | illustre                     |
        |       | titre vide                   |
        | "   " | titre fait de trois espaces  |

  Règle: Un pari a de 2 à 100 issues, au libellé non vide et toutes distinctes

    Scénario: Un pari accepte jusqu'à 100 issues
      Quand Alice crée le pari simple avec les 100 issues « Issue 1 » à « Issue 100 »
      Alors le fait « Pari créé » est publié avec les 100 issues

    Plan du scénario: Des issues invalides font refuser la création
      Quand Alice crée le pari simple avec les issues <issues>
      Alors la création est refusée pour le motif « <motif> »
      Et aucun pari n'est enregistré
      Et aucun fait n'est publié

      Exemples:
        | issues                      | motif                    | illustre                                   |
        | « Lyon »                    | nombre d'issues invalide | une seule issue (minimum 2)                |
        | « Issue 1 » à « Issue 101 » | nombre d'issues invalide | 101 issues (maximum 100)                   |
        | « Lyon », «    »            | issue sans libellé       | libellé fait d'espaces                     |
        | « Lyon », « lyon »          | issues en double         | doublon qui ne diffère que par la casse    |
        | « Lyon », « Lyon  »         | issues en double         | doublon qui ne diffère que par des espaces |

  Règle: La date de clôture est strictement dans le futur, sans délai minimal ni maximal

    Plan du scénario: Une date de clôture strictement future est acceptée
      Quand Alice crée le pari simple avec la clôture <clôture>
      Alors le fait « Pari créé » est publié avec la clôture <clôture>

      Exemples:
        | clôture              | illustre                     |
        | 2026-10-21T14:00:01Z | une seconde après maintenant |
        | 2028-10-21T14:00:00Z | dans 2 ans : pas de maximum  |

    Plan du scénario: Une date de clôture qui n'est pas dans le futur est refusée
      Quand Alice crée le pari simple avec la clôture <clôture>
      Alors la création est refusée pour le motif « date de clôture non future »
      Et aucun pari n'est enregistré
      Et aucun fait n'est publié

      Exemples:
        | clôture              | illustre        |
        | 2026-10-21T14:00:00Z | l'instant même  |
        | 2026-10-21T13:59:59Z | une date passée |

  Règle: La commission est un pourcentage entier de 0 % à 10 % inclus

    Plan du scénario: Une commission dans les bornes est acceptée
      Quand Alice crée le pari simple avec une commission de <commission>
      Alors le fait « Pari créé » est publié avec la commission <commission>

      Exemples:
        | commission | illustre                        |
        | 0 %        | borne basse : pas de commission |
        | 10 %       | borne haute                     |

    Plan du scénario: Une commission hors bornes est refusée
      Quand Alice crée le pari simple avec une commission de <commission>
      Alors la création est refusée pour le motif « commission hors bornes »
      Et aucun pari n'est enregistré
      Et aucun fait n'est publié

      Exemples:
        | commission | illustre                   |
        | 11 %       | juste au-dessus du maximum |
        | -1 %       | négative                   |

  Règle: Un pari créé avec des invités est privé ; chaque invité distinct reçoit une invitation
    # Le caractère privé est fixé à la création et ne change plus, même si tous les invités sont
    # retirés plus tard (le retrait est hors périmètre : pas de scénario ici).

    Plan du scénario: Chaque invité distinct reçoit une seule invitation dès la création
      Étant donné que Bob et Carole sont des joueurs
      Quand Alice crée le pari simple avec les invités <invités>
      Alors le fait « Pari créé » est publié avec les invités <invités distincts>
      Et un fait « Joueur invité au pari » est publié pour chacun de <invités distincts>, et pour personne d'autre

      Exemples:
        | invités     | invités distincts | illustre                             |
        | Bob         | Bob               | un invité                            |
        | Bob, Carole | Bob, Carole       | une invitation par invité            |
        | Bob, Bob    | Bob               | un doublon est fusionné, sans erreur |

    Plan du scénario: Un invité inadmissible fait refuser toute la création
      Étant donné que Bob est un joueur
      Et que Zoé n'est pas une joueuse
      Quand Alice crée le pari simple avec les invités <invités>
      Alors la création est refusée pour le motif « <motif> »
      Et aucun pari n'est enregistré
      Et aucun fait n'est publié, pas même une invitation pour Bob

      Exemples:
        | invités  | motif                       | illustre                                                     |
        | Alice    | le créateur s'invite        | le créateur ne peut pas figurer dans sa propre liste         |
        | Bob, Zoé | invité qui n'est pas joueur | refus total : Bob, qui est valide, n'est pas invité non plus |
```

## Règles métier

1. **Seul un joueur crée un pari.** Le créateur a un Joueur dans `betting`, né du traitement
   d'*Email confirmé*. `members` n'est jamais appelé.
2. **La création publie « Pari créé ».** Le fait porte l'identifiant, le créateur, le titre, la
   description facultative, les issues, la clôture, la commission et la liste des invités (vide pour un
   pari ouvert à tous). Il n'y a pas de champ « mode ». La création ne coûte aucun jeton.
3. **Titre** non vide une fois les espaces retirés au début et à la fin.
4. **Issues** : de 2 à 100, chaque libellé non vide une fois les espaces retirés, tous distincts sans
   tenir compte de la casse ni des espaces au début et à la fin.
5. **Date de clôture** postérieure à maintenant, à la seconde près selon l'horloge. Ni délai minimal ni
   maximal.
6. **Commission** : pourcentage entier de 0 % à 10 % inclus.
7. **Invités** : une liste non vide à la création rend le pari privé. Les doublons sont fusionnés, le
   créateur ne peut pas figurer dans sa liste, chaque invité doit être un joueur, et un seul invité
   inadmissible fait refuser toute la création. **Le caractère privé est fixé à la création et ne
   change plus**, même si le créateur retire ensuite tous les invités. C'est un état du Pari fixé à la
   création, jamais recalculé à partir de la liste courante. Sans scénario ici : la fonctionnalité
   « Retirer de la liste » devra écrire « étant donné un pari privé dont Bob, seul invité, a été
   retiré, quand Carole, joueuse non invitée, mise, alors la mise est refusée ».

Un refus est une erreur renvoyée au créateur : aucun pari enregistré, aucun fait publié.

## Contextes concernés

| Contexte | Scénarios | Faits qu'il publie | Faits auxquels il réagit |
|---|---|---|---|
| `betting` (écriture) | les 12 scénarios | *Pari créé*, *Joueur invité au pari* | *Email confirmé* (crée le Joueur), présent ici seulement comme `Given` |
| `members` | aucun | *Email confirmé* | — |
| Service de mail (externe) | aucun | — | *Joueur invité au pari*, via PO8 (hors périmètre) |

## Découpages effectués

| Candidat d'origine | Découpé en | Fait à la frontière |
|---|---|---|
| « Martin confirme son email puis crée aussitôt un pari » | `members` publie *Email confirmé* · `betting` (PO1, hors périmètre) crée le Joueur · `betting` : règle 1 (pas encore joueur, donc refus) | *Email confirmé* |
| « À la création, Bob reçoit le mail d'invitation » | `betting` publie *Joueur invité au pari* (règle 7) · PO8 (hors périmètre) demande l'envoi au service de mail | *Joueur invité au pari* |
| « Le pari ouvert apparaît dans la liste des paris ouverts de Carole » | Pas une frontière de contexte mais un passage au côté lecture, qui part de « étant donné *Pari créé* » (hors périmètre) | *Pari créé* |

## Fusions effectuées

| Gardé | Absorbé | Ce qui ne variait pas |
|---|---|---|
| Membre non joueur refusé | email non confirmé, « Email confirmé » pas encore traité | même `Given` dans betting : pas de Joueur |
| « Pari créé » publié avec tout son contenu | 2 issues acceptées, pari ouvert, description facultative | Pari créé publié, aucune invitation |
| Pari sans titre refusé | titre vide, titre fait d'espaces | refus « titre manquant » |
| Issues invalides | 1 issue, 101 issues, libellé vide, doublon de casse, doublon d'espaces | refus ; le motif devient une colonne |
| Date future acceptée / non future refusée | +1 s, +2 ans / l'instant même, date passée | même résultat dans chaque paire |
| Commission dans / hors bornes | 0 %, 10 % / 11 %, -1 % | même résultat dans chaque paire |
| Une invitation par invité distinct | 1 invité, 2 invités, doublon | une invitation par invité distinct |
| Invité inadmissible | le créateur s'invite, invité non joueur | refus total ; le motif devient une colonne |

Les refus de règles différentes ne sont pas fusionnés entre eux : chacun a son propre axe, et un tableau
unique ferait varier cinq axes à la fois.

## Substitutions effectuées (données minimales)

| Scénarios | Valeur remplacée | Remplacée par | Règle qu'elle ne présuppose plus |
|---|---|---|---|
| Tous sauf règle 6 | commission 0 % ou 10 % | 5 % | bornes de la commission |
| Tous sauf règle 5 | clôture +1 s | J+1 | « strictement futur » |
| Tous sauf règle 4 | 3 issues ou plus | « Lyon », « Paris » | maximum et unicité des issues |
| Tous sauf règle 7 | liste d'invités | aucune | admissibilité, fusion, invitations |
| Invité inadmissible, ligne créateur | « Bob, Alice » | « Alice » | rien : le tout ou rien est porté par « Bob, Zoé » |
| Création gratuite | solde de 1000 | solde de 0 | jetons de bienvenue et recharge ; toute exigence de solde apparaîtrait |
| Tous | avec description | sans description | rien, la description n'a pas de règle |

## Décisions clés

- **Pas de mode d'accès** : un pari est privé si et seulement si sa liste d'invités est non vide à la
  création, et il le reste ensuite. « Ouvert avec invités » et « privé sans invités » ne peuvent pas
  s'exprimer.
- **Doublon d'invité fusionné, créateur qui s'invite refusé** : le doublon est une erreur de saisie sans
  danger ; s'inviter contredit « le créateur ne mise pas sur son propre pari ».
- **Un invité non joueur fait refuser toute la création**, pas seulement son invitation.
- **Commission de 0 % à 10 %.** Le plafond du règlement (chaque gagnant récupère sa mise) reste une règle
  à part.
- **De 2 à 100 issues.**
- **Date de clôture strictement future**, à la seconde près, selon l'horloge (port).
- **Création gratuite, nombre de paris illimité** en V1.
- **Hors périmètre, simple mention** : la clôture anticipée est généralisée. Le créateur peut seulement
  **avancer** la date de clôture, vers un instant entre maintenant et la clôture actuelle, jamais la
  reculer. Si l'événement réel est reporté, il laisse les mises scellées et attend pour déclarer le
  résultat, ou il annule le pari.
- Dans la langue de `betting`, « Utilisateur invité au pari » devient **« Joueur invité au pari »**.

## Questions ouvertes

1. **Coût de création**, pour décourager les paris inutiles (clôture à +1 s). Le jour où il arrive, le
   scénario « La création d'un pari ne coûte aucun jeton » change d'issue.
2. **Normalisation des libellés** : stocke-t-on et publie-t-on le titre et les issues sans les espaces
   au début et à la fin ? La comparaison se fait sur le libellé nettoyé ; le stockage n'est pas fixé.
3. **Longueurs maximales** du titre, des issues et de la description : non bornées pour l'instant.
4. **Plusieurs motifs à la fois** (commission 11 % et une seule issue) : premier motif ou tous ? Un
   scénario le fixera quand l'interface en aura besoin.
5. **Ordre des issues** conservé tel que saisi dans *Pari créé* : hypothèse retenue, oui.
6. **Commission non entière** (5,5 %) : rejetée comme erreur de format au niveau HTTP, sans règle
   métier. À confirmer.

## Contradictions résolues

- **Q7 (pari privé sans invité refusé) contre « pas de mode »** : sans mode, une liste vide veut dire
  « ouvert à tous » ; le cas « privé sans invité » ne peut plus s'exprimer, d'où aucun scénario.
- **« Liste fixée à la création » contre « commande Inviter séparée »** : la liste initiale est donnée à
  la création, d'autres invitations peuvent suivre avec « Inviter » (hors périmètre).
- **« Le créateur ne mise pas sur son propre pari »** devient, à la création, « le créateur ne peut pas
  s'inviter ».

## Matrice de couverture

| Règle | Nominal | Cas limites | Erreurs |
|---|---|---|---|
| 1. Créateur joueur | (par la règle 2) | — | ✅ |
| 2. Publication, ouvert, gratuit | ✅ | ✅ gratuité | — |
| 3. Titre | (par la règle 2) | ⚠️ longueur maximale ouverte | ✅ |
| 4. Issues | (par la règle 2) | ✅ 100 | ✅ 1, 101, vide, doublons |
| 5. Date de clôture | (par la règle 2) | ✅ +1 s, +2 ans | ✅ maintenant, passé |
| 6. Commission | (par la règle 2) | ✅ 0 %, 10 % | ✅ 11 %, -1 % |
| 7. Invités | ✅ | ✅ doublon fusionné | ✅ créateur, non joueur |
