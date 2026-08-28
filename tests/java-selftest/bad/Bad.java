package x;

// Auto-test NEGATIF : chaque ligne marquee ci-dessous est une faute de COMPILATION que la grammaire
// seule ne voit pas. Si `scripts/selftest-parse-java.sh` ne les signale plus, le controle est devenu
// decoratif — et un controle decoratif a deja couté deux runs CI a ce depot.
public final class Bad {

    private int outer = 1;

    /** FAUTE 1 : accesseur deplace depuis la classe imbriquee ; `this.material` n'existe pas ici
     *  (une methode du meme nom existe, et c'est precisement le piege que la regle doit distinguer). */
    public int material() {
        return this.material;
    }

    /** FAUTE 2 : deux declarations du meme nom dans le meme bloc. */
    public int duplicated() {
        int price = 1;
        int price = 2;
        return price;
    }

    public static final class Inner {

        private int material = 3;

        public int material() {
            return this.material;
        }
    }
}
