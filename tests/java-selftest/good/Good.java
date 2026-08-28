package x;

// Auto-test POSITIF du controle : ce fichier est correct, il ne doit lever AUCUN signal.
// (Un controle qui signale du code correct finit ignore — donc nocif. Ce fixture est la pour que
//  `scripts/selftest-parse-java.sh` echoue si la regle devient bruyante.)
public final class Good {

    private int outer = 1;

    public Good() {
        this.outer = 2;
    }

    public int readField() {
        return this.outer;
    }

    public static final class Inner {

        private int material = 3;

        /** acces au champ de la CLASSE IMBRIQUEE : correct. */
        public int material() {
            return this.material;
        }

        /** variable de boucle reutilisee dans un bloc fils : correct, et c'est le cas que la
         *  premiere version de la regle 3 signalait a tort. */
        public int use() {
            int price = 1;
            for (int i = 0; i < 3; i++) {
                int step = 2;
                price = price + step + i;
            }
            return price;
        }

        public int save() {
            return this.use();
        }
    }
}
