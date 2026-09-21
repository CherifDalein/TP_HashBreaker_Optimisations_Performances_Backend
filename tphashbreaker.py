import hashlib

alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
mot = "z3D"
hash_cible = hashlib.sha256(mot.encode()).hexdigest()


def chercher(hash_cible):
    for i in range(len(alphabet)):
        for j in range(len(alphabet)):
            for k in range(len(alphabet)):
                guess = alphabet[i] + alphabet[j] + alphabet[k]
                hash_guess = hashlib.sha256(guess.encode()).hexdigest()
                if hash_guess == hash_cible:
                    print(f"Mot de passe trouvé: {guess}")
                    return guess
    return None


if __name__ == "__main__":
    result = chercher(hash_cible)
    if result is None:
        print("Mot de passe non trouvé.")

