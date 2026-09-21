import hashlib
import time

alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"


def chercher(hash_cible, longueur):
    if longueur < 1:
        raise ValueError("La longueur doit être supérieure ou égale à 1.")

    indices = [0] * longueur

    while True:
        guess = "".join(alphabet[indice] for indice in indices)
        hash_guess = hashlib.sha256(guess.encode("utf-8")).hexdigest()
        if hash_guess == hash_cible:
            return guess

        # Avancer à droite et propager la retenue vers la gauche.
        position = longueur - 1
        while position >= 0:
            indices[position] += 1
            if indices[position] < len(alphabet):
                break
            indices[position] = 0
            position -= 1

        # Une retenue au-delà de la première position termine la recherche.
        if position < 0:
            return None


if __name__ == "__main__":
    for mot in ("z3D", "Sh3n"):
        # Préparer la cible hors du chronométrage ; la longueur est connue.
        hash_cible = hashlib.sha256(mot.encode("utf-8")).hexdigest()
        longueur = len(mot)

        debut = time.perf_counter()
        result = chercher(hash_cible, longueur)
        duree = time.perf_counter() - debut

        print(f"\nCible SHA-256 : {hash_cible}")
        print(f"Alphabet : {len(alphabet)} caractères | Longueur : {longueur}")
        if result is None:
            print("Mot de passe non trouvé.")
        else:
            print(f"Mot de passe trouvé : {result}")
        print(f"Temps de recherche : {duree:.6f} secondes")
