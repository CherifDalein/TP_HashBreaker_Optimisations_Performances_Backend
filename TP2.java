import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/** Séance 2. Usage : java TP2.java [nombre] [répétitions]. */
public class TP2 {
    private static final byte[] ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final int LONGUEUR = 4;
    private static final String[] NOMS = {
        "Contigu séquentiel", "Contigu aléatoire", "Nœuds mélangés"
    };
    // Rendre les résultats observables par la JVM.
    private static volatile long temoin;

    private static class Noeud {
        final byte[] candidat;
        Noeud suivant;

        Noeud(byte[] candidat) {
            this.candidat = candidat;
        }
    }

    private final byte[] bloc;
    private final int[] ordre;
    private final Noeud tete;
    private final MessageDigest sha256;

    private TP2(int nombre) throws NoSuchAlgorithmException {
        sha256 = MessageDigest.getInstance("SHA-256");
        bloc = new byte[nombre * LONGUEUR];
        ordre = new int[nombre];
        Noeud[] noeuds = new Noeud[nombre];
        for (int i = 0; i < nombre; i++) {
            int valeur = i;
            for (int position = LONGUEUR - 1; position >= 0; position--) {
                bloc[i * LONGUEUR + position] = ALPHABET[valeur % ALPHABET.length];
                valeur /= ALPHABET.length;
            }
            ordre[i] = i;
            noeuds[i] = new Noeud(Arrays.copyOfRange(
                    bloc, i * LONGUEUR, (i + 1) * LONGUEUR));
        }
        Random aleatoire = new Random(42);
        for (int i = nombre - 1; i > 0; i--) {
            int j = aleatoire.nextInt(i + 1);
            int temporaire = ordre[i];
            ordre[i] = ordre[j];
            ordre[j] = temporaire;
        }
        for (int i = 0; i < nombre - 1; i++) {
            noeuds[ordre[i]].suivant = noeuds[ordre[i + 1]];
        }
        tete = noeuds[ordre[0]];
    }

    // Même traitement pour les trois dispositions, sans copie du candidat.
    private long traiter(byte[] donnees, int debut, boolean hachage) {
        if (hachage) {
            sha256.update(donnees, debut, LONGUEUR);
            byte[] empreinte = sha256.digest(); // Réinitialise aussi le digest.
            long controle = 0;
            for (byte octet : empreinte) {
                controle = controle * 31 + (octet & 0xff);
            }
            return controle;
        }
        long somme = 0;
        for (int i = 0; i < LONGUEUR; i++) {
            somme += donnees[debut + i] & 0xff;
        }
        return somme;
    }

    private long parcourir(int mode, boolean hachage) {
        long controle = 0;
        if (mode == 2) {
            for (Noeud noeud = tete; noeud != null; noeud = noeud.suivant) {
                controle += traiter(noeud.candidat, 0, hachage);
            }
        } else if (mode == 1) {
            for (int indice : ordre) {
                controle += traiter(bloc, indice * LONGUEUR, hachage);
            }
        } else {
            for (int debut = 0; debut < bloc.length; debut += LONGUEUR) {
                controle += traiter(bloc, debut, hachage);
            }
        }
        return controle;
    }

    private void mesurer(boolean hachage, int repetitions) {
        long reference = parcourir(0, hachage);
        // Échauffement pratique ; ne garantit pas la stabilisation du JIT.
        for (int tour = 0; tour < 5; tour++) {
            for (int mode = 0; mode < 3; mode++) {
                verifier(parcourir(mode, hachage), reference);
            }
        }
        double[][] durees = new double[3][repetitions];
        for (int tour = 0; tour < repetitions; tour++) {
            for (int position = 0; position < 3; position++) {
                int mode = (position + tour) % 3;
                long debut = System.nanoTime();
                long controle = parcourir(mode, hachage);
                durees[mode][tour] = (System.nanoTime() - debut) / 1_000_000_000.0;
                verifier(controle, reference);
            }
        }
        System.out.println("\n" + (hachage ? "Lecture + SHA-256" : "Lecture")
                + " — médianes, sommes de contrôle identiques");
        double base = mediane(durees[0]);
        for (int mode = 0; mode < 3; mode++) {
            double temps = mediane(durees[mode]);
            System.out.printf("%-22s %.6f s | %,.0f candidats/s | "
                            + "temps / séquentiel : %.2f%n",
                    NOMS[mode], temps, ordre.length / temps, temps / base);
        }
    }

    private static void verifier(long controle, long reference) {
        temoin = controle;
        if (controle != reference) {
            throw new IllegalStateException("Sommes de contrôle différentes !");
        }
    }

    private static double mediane(double[] valeurs) {
        Arrays.sort(valeurs);
        int milieu = valeurs.length / 2;
        return valeurs.length % 2 == 1 ? valeurs[milieu]
                : (valeurs[milieu - 1] + valeurs[milieu]) / 2;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        int nombre = args.length > 0 ? Integer.parseInt(args[0]) : 200_000;
        int repetitions = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        if (args.length > 2 || nombre < 1 || nombre > 14_776_336 || repetitions < 1) {
            throw new IllegalArgumentException(
                    "Usage : java TP2.java [nombre 1..14776336] [répétitions >= 1]");
        }
        System.out.println("Java " + System.getProperty("java.version") + " | "
                + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.printf("%,d candidats de 4 octets | %d répétitions | graine 42%n",
                nombre, repetitions);
        System.out.printf("Bloc contigu : %.2f Mio (hors objets et indices)%n",
                nombre * LONGUEUR / (1024.0 * 1024));
        TP2 experience = new TP2(nombre); // Préparation hors chronométrage.
        experience.mesurer(false, repetitions);
        experience.mesurer(true, repetitions);
        System.out.println("\nMesures pédagogiques : JIT, allocations et GC influencent les temps.");
        System.out.println("Le placement des nœuds dépend de la JVM. Les cache misses ne sont pas mesurés.");
        System.out.println("Faire varier la taille du lot : un petit bloc peut tenir dans le cache.");
    }
}
