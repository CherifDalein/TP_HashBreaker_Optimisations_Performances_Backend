import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Séance 1 : compteur base 62 naïf et baseline, longueurs connues. */
public class TP1 {
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String chercher(byte[] hashCible, int longueur)
            throws NoSuchAlgorithmException {
        if (longueur < 1) {
            throw new IllegalArgumentException("La longueur doit être positive.");
        }
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        int[] indices = new int[longueur];
        while (true) {
            // Version volontairement naïve : reconstruire le candidat à chaque essai.
            StringBuilder construction = new StringBuilder(longueur);
            for (int indice : indices) {
                construction.append(ALPHABET.charAt(indice));
            }
            String candidat = construction.toString();
            byte[] hash = sha256.digest(candidat.getBytes(StandardCharsets.UTF_8));
            if (MessageDigest.isEqual(hash, hashCible)) {
                return candidat;
            }

            int position = longueur - 1;
            while (position >= 0) {
                indices[position]++;
                if (indices[position] < ALPHABET.length()) {
                    break;
                }
                indices[position] = 0;
                position--;
            }
            if (position < 0) {
                return null;
            }
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println("Java " + System.getProperty("java.version"));
        for (String mot : new String[]{"z3D", "Sh3n"}) {
            byte[] cible = MessageDigest.getInstance("SHA-256")
                    .digest(mot.getBytes(StandardCharsets.UTF_8));
            int longueur = mot.length();
            long debut = System.nanoTime();
            String resultat = chercher(cible, longueur);
            double secondes = (System.nanoTime() - debut) / 1_000_000_000.0;

            System.out.println("\nHash cible : " + HexFormat.of().formatHex(cible));
            System.out.println("Alphabet : 62 caractères | Longueur : " + longueur);
            System.out.println(resultat == null ? "Mot de passe non trouvé."
                    : "Mot de passe trouvé : " + resultat);
            System.out.printf("Temps de recherche : %.6f secondes%n", secondes);
        }
        System.out.println("Baseline simple sans échauffement dédié de la JVM.");
    }
}
