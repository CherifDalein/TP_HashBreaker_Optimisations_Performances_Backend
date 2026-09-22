"""Comparaison pédagogique des dispositions mémoire, sans dépendance externe."""

import argparse
import hashlib
import platform
import random
import statistics
import time


ALPHABET = b"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"


class Noeud:
    __slots__ = ("candidat", "suivant")

    def __init__(self, candidat):
        self.candidat = candidat
        self.suivant = None


def preparer(nombre, longueur, graine):
    """Préparer les mêmes candidats et leur permutation hors chronométrage."""
    bloc = bytearray(nombre * longueur)
    for numero in range(nombre):
        valeur = numero
        for position in range(longueur - 1, -1, -1):
            valeur, chiffre = divmod(valeur, len(ALPHABET))
            bloc[numero * longueur + position] = ALPHABET[chiffre]

    ordre = list(range(nombre))
    random.Random(graine).shuffle(ordre)

    # Chaque nœud possède ses propres octets. Leur placement dépend de Python.
    noeuds = [
        Noeud(bytes(bloc[i * longueur:(i + 1) * longueur]))
        for i in range(nombre)
    ]
    for position in range(nombre - 1):
        noeuds[ordre[position]].suivant = noeuds[ordre[position + 1]]
    return bloc, ordre, noeuds[ordre[0]]


def parcours_bloc(bloc, ordre, longueur):
    vue = memoryview(bloc)
    for indice in ordre:
        debut = indice * longueur
        # Une vue évite de recopier les octets, mais crée un objet Python.
        yield vue[debut:debut + longueur]


def parcours_noeuds(tete):
    noeud = tete
    while noeud is not None:
        yield noeud.candidat
        noeud = noeud.suivant


def lire(candidats):
    """Lire tous les octets et conserver une somme de contrôle."""
    controle = 0
    for candidat in candidats:
        controle += sum(candidat)
    return controle


def hacher(candidats):
    """La même logique SHA-256 s'applique aux trois parcours."""
    controle = 0
    for candidat in candidats:
        controle += int.from_bytes(hashlib.sha256(candidat).digest(), "big")
    return controle


def entier_positif(texte):
    valeur = int(texte)
    if valeur < 1:
        raise argparse.ArgumentTypeError("La valeur doit être positive.")
    return valeur


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--nombre", type=entier_positif, default=200_000)
    parser.add_argument("--longueur", type=entier_positif, default=4)
    parser.add_argument("--repetitions", type=entier_positif, default=3)
    parser.add_argument("--graine", type=int, default=42)
    args = parser.parse_args()
    if args.nombre > len(ALPHABET) ** args.longueur:
        parser.error("Le nombre dépasse le nombre de candidats uniques possibles.")

    print(f"Python {platform.python_version()} | {platform.platform()}")
    print(f"{args.nombre:,} candidats de {args.longueur} octets | "
          f"{args.repetitions} répétitions | graine {args.graine}")
    print(f"Bloc contigu : {args.nombre * args.longueur / 1024**2:.2f} Mio "
          "(hors indices et objets)", flush=True)
    bloc, ordre, tete = preparer(args.nombre, args.longueur, args.graine)
    parcours = {
        "Contigu séquentiel": lambda: parcours_bloc(
            bloc, range(args.nombre), args.longueur),
        "Contigu aléatoire": lambda: parcours_bloc(bloc, ordre, args.longueur),
        "Nœuds mélangés": lambda: parcours_noeuds(tete),
    }

    for nom_operation, operation in (("Lecture", lire), ("Lecture + SHA-256", hacher)):
        mesures = {nom: [] for nom in parcours}
        reference = None
        # Échauffement et vérification hors des mesures.
        for fabriquer in parcours.values():
            controle = operation(fabriquer())
            if reference is None:
                reference = controle
            elif controle != reference:
                raise RuntimeError("Les sommes de contrôle diffèrent entre parcours.")

        noms = list(parcours)
        for repetition in range(args.repetitions):
            # Alterner l'ordre pour limiter le biais dû à la position du test.
            decalage = repetition % len(noms)
            for nom in noms[decalage:] + noms[:decalage]:
                debut = time.perf_counter()
                controle = operation(parcours[nom]())
                duree = time.perf_counter() - debut
                if controle != reference:
                    raise RuntimeError("Somme de contrôle incorrecte.")
                mesures[nom].append(duree)

        print(f"\n{nom_operation} — médianes, sommes de contrôle identiques")
        base = statistics.median(mesures[noms[0]])
        for nom, durees in mesures.items():
            mediane = statistics.median(durees)
            print(f"{nom:22} {mediane:.6f} s | "
                  f"{args.nombre / mediane:,.0f} candidats/s | "
                  f"temps / séquentiel : {mediane / base:.2f}")

    print("\nAttention : ces mesures incluent le coût des objets et boucles Python.")
    print("Elles ne mesurent pas directement les cache misses ; "
          "aucun classement des parcours n'est garanti.")


if __name__ == "__main__":
    main()
