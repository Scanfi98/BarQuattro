
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *Main
 * @author Giovanni Scanferla
 */
public class BarQuattro {

    public static void main(String[] args) {
        new Thread(new Bancone(4)).start();
    }

}

/**
 * Classe del Bancone
 * @author Giovanni Scanferla
 */
class Bancone implements Runnable {

    private Contatore cont; //contatore alcolici

    private int dim; //dimensione tavolo

    private int time; //contatore dei secondi prima di pulire il banco

    private int totaleServiti; //contatore del totale dei clienti serviti

    private int fileOn; //contatore di tutte le file attive

    private Alcolico[] bancone; //array con gli alcolici

    private Thread[][] clienti; //matrice thread clienti

    private ReentrantLock[] lock; //array contentente i lock delle varie file di clienti

    private int[] fileAttive;

    /**
     * Costruttore del bancone
     *
     * @param dim dimensione del bancone
     */
    public Bancone(int dim) {
        this.dim = dim;
        bancone = new Alcolico[dim];
        cont = new Contatore();
        clienti = new Thread[4][dim];
        lock = new ReentrantLock[dim];
        time = 0;
        totaleServiti = 0;
        fileOn = 4;
        fileAttive = new int[dim];
        init();
    }

    @Override
    public void run() {
        while (totaleServiti < (clienti.length * clienti[0].length)) {
            if (time == 3) {
                clean();
            } else {
                boolean prova = add();
                if (!prova && !isFull()) {

                } else if (prova && isFull()) {

                } else if (!prova && isFull()) {
                    time++;
                }

            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {

            }
        }
        System.out.printf("SYSTEM: totale serviti %d\n", totaleServiti);

    }

    /**
     * Metodo che inizializza il Bancone, i Clienti e i Lock
     */
    public void init() {
        for (int i = 0; i < bancone.length; i++) {
            bancone[i] = null;
        }
        for (int i = 0; i < lock.length; i++) {
            lock[i] = new ReentrantLock();
        }

        for (int i = 0; i < clienti[0].length; i++) {
            for (int j = 0; j < clienti.length; j++) {
                clienti[i][j] = new Thread(new Cliente(lock[j], this, j), "CLIENTE" + (j + 1) + "X" + (i + 1));
                clienti[i][j].start();
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Bancone.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Aggiunge un Alcolico ad una fila ATTIVA, purchè non ne siano già presenti
     * tre dello stesso tipo
     *
     * @return true se lo inserisce, sennò false
     */
    public boolean add() {
        Alcolico a = new Alcolico();
        for (int i = 0; i < bancone.length; i++) {
            if (bancone[i] == null && fileAttive[i] < 4) {
                if (cont.increase(a.toString())) {
                    bancone[i] = a;
                    System.out.printf("BARISTA: aggiunto %s alla fila %d\n", a.toString(), i + 1);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Rimuove l'elemento alla posizione indicata
     *
     * @param pos il numero della fila
     * @return l'Alcolico rimosso
     */
    public Alcolico remove(int pos) {
        if (bancone[pos] != null) {
            cont.decrease(bancone[pos].toString());
            Alcolico prov = bancone[pos];
            bancone[pos] = null;
            return prov;
        }
        return null;
    }

    /**
     * Controlla se il bancone è pieno
     *
     * @return ritorna true se tutte le file ATTIVE sono piene, sennò false
     */
    public boolean isFull() {
        return cont.getTot() == fileOn;
    }

    /**
     * Prende l'Alcolico alla posizione indicata
     *
     * @param nFila posizione della fila
     * @return l'Alcolico
     */
    public Alcolico getAtPos(int pos) {
        return bancone[pos];
    }

    /**
     * Resetta il bancone
     */
    public void clean() {
        for (int i = 0; i < bancone.length; i++) {
            bancone[i] = null;
        }
        cont.reset();
        time = 0;
        System.out.println("BARISTA: bancone ripulito");
    }

    /**
     * Metodo che viene eseguito da un cliente una volta ottenuta l'ordinazione,
     * incrementa tutti i valori di gestione per le file attive
     *
     * @param fila il numero della fila
     */
    public void servitoCliente(int fila) {
        totaleServiti++;

        fileAttive[fila]++;
        if (fileAttive[fila] == 4) {
            fileOn--;
            System.out.printf("SYSTEM: fila %d terminata\n", fila + 1);
        }
    }

}

/**
 * Classe del cliente
 *
 * @author Giovanni Scanferla
 */
class Cliente implements Runnable {

    private ReentrantLock lock; //lock

    private Alcolico ordinazione; //ordinazione scelta

    private Bancone bancone; //bancone del bar

    private int fila; //fila di appartenenza


    /**
     * Costruttore del Cliente
     * @param lock lock di gestione fila
     * @param bancone bancone del bar
     * @param fila numero della fila
     */
    public Cliente(ReentrantLock lock, Bancone bancone, int fila) {
        this.lock = lock;
        ordinazione = new Alcolico();
        this.bancone = bancone;
        this.fila = fila;
    }

    @Override
    public void run() {
        lock.lock();
        try {
            System.out.printf("%s: vorrei %s\n", Thread.currentThread().getName(), ordinazione.toString());
            boolean bevuto = false;
            while (!bevuto) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Bancone.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (ordinazione.equals(bancone.getAtPos(fila))) {
                    bancone.remove(fila);
                    bevuto = true;
                    System.out.printf("%s: ordinazione bevuta\n", Thread.currentThread().getName());
                    bancone.servitoCliente(fila);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}

/**
 * Classe dell'Alcolico
 *
 * @author Giovanni Scanferla
 */
class Alcolico {

    public static String[] alcolici = {"Spritz", "Birra", "Cocktail", "Prosecco"};

    private String name;

    /**
     * Costruttore classe Alcolico
     */
    public Alcolico() {
        name = initNome();
    }

    /**
     * Genera un nome randomico per l'alcolico
     *
     * @return il nome generato
     */
    private String initNome() {
        Random r = new Random();
        return alcolici[r.nextInt(alcolici.length)];
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Alcolico other = (Alcolico) obj;
        if (other.toString().equals(name)) {
            return true;
        }
        return false;
    }

}

/**
 * Classe di gestione del Contatore degli Alcolici
 * @author Giovanni Scanferla
 */
class Contatore {

    private HashMap<String, Integer> contatore; //HashMap con i valori

    private int tot; //totale di Alcolici nell'HashMap

    /**
     * Costruttore del contatore
     */
    public Contatore() {
        contatore = new HashMap<>();
        tot = 0;
        init();
    }

    /**
     * Inizializza il Contatore
     */
    public void init() {
        for (int i = 0; i < Alcolico.alcolici.length; i++) {
            contatore.put(Alcolico.alcolici[i], 0);
        }
    }

    /**
     * Aumenta di uno la determinata Bevanda se possibile
     * @param name nome della bevanda
     * @return true se inserita
     */
    public boolean increase(String name) {
        if (contatore.get(name) <= 3) {
            contatore.put(name, (contatore.get(name) + 1));
            tot++;
            return true;
        }
        return false;
    }

    /**
     * Rimuove una bevanda dato il nome
     * @param name nome della bevanda
     * @return true se rimossa
     */
    public boolean decrease(String name) {
        if (contatore.get(name) > 0) {
            contatore.put(name, (contatore.get(name) - 1));
            tot--;
            return true;
        }
        return false;
    }

    /**
     * Resetta l'HashMap
     */
    public void reset() {
        tot = 0;
        init();
    }

    /**
     * Ritorna il totale delle bevande
     * @return il totale delle bevande
     */
    public int getTot() {
        return tot;
    }

}
