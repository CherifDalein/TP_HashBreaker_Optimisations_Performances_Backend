alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
mot = "z3D"

def chercher(mot):
    for i in range(len(alphabet)):
        for j in range(len(alphabet)):
            for k in range(len(alphabet)):
                guess = alphabet[i] + alphabet[j] + alphabet[k]
                if guess == mot:
                    print(f"Found the password: {guess}")
                    return guess
    return None


if __name__ == "__main__":
    result = chercher(mot)
    if result is None:
        print("Password not found.")

