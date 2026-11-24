package ultima_cena;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;



public class Cena {

    // Cuenta las veces que un tenedor lo usan dos filósofos a la vez (nuestro "fallo")
    static AtomicInteger conflictos = new AtomicInteger(0);

    // ---------- Tenedor: el recurso compartido ----------
    static class Tenedor {
        int id;
        boolean ocupado = false;
        int usuarios = 0;                      // cuántos lo usan ahora mismo

        Tenedor(int id) { this.id = id; }

        synchronized void coger() throws InterruptedException {
            while (ocupado) wait();            // espera a que esté libre
            ocupado = true;
        }

        synchronized void soltar() {
            ocupado = false;
            notifyAll();                       // avisa a los que esperan
        }

        // Los dos métodos siguientes NO son synchronized a propósito: así se ve el fallo
        void empezarAUsar() {
            usuarios++;
            if (usuarios > 1) conflictos.incrementAndGet();   // ¡lo usa otro a la vez!
        }

        void dejarDeUsar() {
            usuarios--;
        }
    }

    // ---------- Camarero: deja sentarse como mucho a n-1 ----------
    static class Camarero {
        int plazas;
        Camarero(int plazas) { this.plazas = plazas; }

        synchronized void entrar() throws InterruptedException {
            while (plazas == 0) wait();
            plazas--;
        }

        synchronized void salir() {
            plazas++;
            notifyAll();
        }
    }

    // ---------- Filósofo ----------
    static class Filosofo extends Thread {
        int id, tPensar, tComer, pausa;
        String modo, estado = "PENSANDO";
        Tenedor izq, der;
        Camarero camarero;
        int comidas = 0;

        Filosofo(int id, Tenedor izq, Tenedor der, String modo, Camarero camarero,
                 int tPensar, int tComer, int pausa) {
            this.id = id; this.izq = izq; this.der = der; this.modo = modo;
            this.camarero = camarero; this.tPensar = tPensar; this.tComer = tComer; this.pausa = pausa;
        }

        public void run() {
            try {
                while (true) {
                    estado = "PENSANDO";
                    Thread.sleep(tPensar);

                    estado = "HAMBRIENTO";
                    Tenedor primero = izq, segundo = der;
                    if (modo.equals("orden") && izq.id > der.id) {   // siempre el menor primero
                        primero = der;
                        segundo = izq;
                    }
                    if (modo.equals("camarero")) camarero.entrar();
                    if (!modo.equals("sin-control")) {
                        primero.coger();
                        Thread.sleep(pausa);       // con pausa, todos cogen el primero antes del segundo
                        segundo.coger();
                    }

                    estado = "COMIENDO";
                    izq.empezarAUsar();
                    der.empezarAUsar();
                    Thread.sleep(tComer);          // comiendo
                    izq.dejarDeUsar();
                    der.dejarDeUsar();
                    comidas++;

                    if (!modo.equals("sin-control")) {
                        primero.soltar();
                        segundo.soltar();
                    }
                    if (modo.equals("camarero")) camarero.salir();
                }
            } catch (InterruptedException e) {
                // fin de la simulación
            }
        }
    }

    // ---------- Programa principal ----------
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {                       // modo clásico: con argumentos
            simular(args[0],
                    args.length > 1 ? Integer.parseInt(args[1]) : 5,
                    args.length > 2 ? Integer.parseInt(args[2]) : 10,
                    args.length > 3 ? Integer.parseInt(args[3]) : 100,
                    args.length > 4 ? Integer.parseInt(args[4]) : 100);
            return;
        }
        String[] modos = {"sin-control", "correcto", "deadlock", "orden", "camarero"};
        Scanner sc = new Scanner(System.in);
        while (true) {                               // menú: se puede repetir las demos
            System.out.println("\n=========== MENÚ ===========");
            System.out.println("1 sin-control   (el fallo)");
            System.out.println("2 correcto      (arreglado)");
            System.out.println("3 deadlock      (bloqueo)");
            System.out.println("4 orden         (solución 1)");
            System.out.println("5 camarero      (solución 2)");
            System.out.println("0 salir");
            System.out.print("Elige: ");
            if (!sc.hasNextLine()) return;
            String op = sc.nextLine().trim();
            if (op.equals("0")) return;
            if (op.matches("[1-5]")) simular(modos[Integer.parseInt(op) - 1], 5, 10, 100, 100);
        }
    }

    static void simular(String modo, int n, int segundos, int tPensar, int tComer) throws Exception {
        conflictos.set(0);
        System.out.println("\n>>>>> MODO: " + modo + " <<<<<");
        int pausa = (modo.equals("correcto") || modo.equals("sin-control")) ? 0 : 100;

        Tenedor[] tenedores = new Tenedor[n];
        for (int i = 0; i < n; i++) tenedores[i] = new Tenedor(i);
        Camarero camarero = new Camarero(n - 1);

        Filosofo[] f = new Filosofo[n];
        for (int i = 0; i < n; i++) {
            f[i] = new Filosofo(i, tenedores[i], tenedores[(i + 1) % n], modo, camarero, tPensar, tComer, pausa);
            f[i].start();
        }

        // Cada segundo: qué hace cada filósofo y en qué estado está su hilo (Reto 1)
        int totalAntes = 0, segundosSinComer = 0;
        for (int s = 1; s <= segundos; s++) {
            Thread.sleep(1000);
            String linea = "t=" + s + "s ";
            int total = 0;
            boolean todosEsperando = true;
            for (Filosofo x : f) {
                linea += " F" + x.id + ":" + x.estado + "(" + x.getState() + ")";
                total += x.comidas;
                if (x.getState() != Thread.State.WAITING) todosEsperando = false;
            }
            System.out.println(linea + "  comidas=" + total);

            // Bloqueo = nadie come y todos los hilos en WAITING (no solo lento)
            segundosSinComer = (total == totalAntes && todosEsperando) ? segundosSinComer + 1 : 0;
            totalAntes = total;
            if (segundosSinComer >= 2) {
                System.out.println(">>> DEADLOCK: todos en WAITING y nadie ha comido en 2 s");
                break;
            }
        }

        for (Filosofo x : f) x.interrupt();

        System.out.println("\n--- RESULTADO ---");
        for (Filosofo x : f) System.out.println("Filósofo " + x.id + " comió " + x.comidas + " veces");
        System.out.println("Conflictos (tenedor usado por 2 a la vez): " + conflictos.get());
    }
}