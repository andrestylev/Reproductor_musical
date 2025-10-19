import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * ReproductorMP3 con estado sincronizado (STOPPED, PLAYING, PAUSED).
 * Maneja pause/resume de forma robusta evitando que los hilos sobrescriban el estado.
 */
public class ReproductorMP3 {

    private enum State { STOPPED, PLAYING, PAUSED }

    private Player player;
    private Thread playThread;

    private FileInputStream fis;
    private BufferedInputStream bis;

    private volatile State state = State.STOPPED;
    private String archivoActual = null;
    private long pausaByteOffset = 0L;

    // ---------- PLAY ----------
    public synchronized void play(String rutaArchivo) {
        System.out.println("DBG Reproductor: play() -> " + rutaArchivo);
        stopInternalNoReset(); // limpia reproducción anterior (no resetea archivoActual si queremos)

        File f = new File(rutaArchivo);
        if (!f.exists()) {
            System.err.println("DBG Reproductor: archivo no existe: " + f.getAbsolutePath());
            return;
        }

        archivoActual = rutaArchivo;
        pausaByteOffset = 0L;
        state = State.PLAYING;

        try {
            fis = new FileInputStream(archivoActual);
            bis = new BufferedInputStream(fis);
            player = new Player(bis);
        } catch (Exception e) {
            System.err.println("DBG Reproductor: error abriendo archivo: " + e);
            cleanupStreams();
            state = State.STOPPED;
            return;
        }

        playThread = new Thread(() -> {
            try {
                System.out.println("DBG Reproductor: player.play() starting");
                player.play(); // bloqueante dentro del hilo
                System.out.println("DBG Reproductor: player.play() finished normally");
            } catch (Throwable t) {
                System.err.println("DBG Reproductor: exception en play thread: " + t);
                t.printStackTrace();
            } finally {
                synchronized (ReproductorMP3.this) {
                    // Si al terminar el hilo el estado todavía era PLAYING significa que la pista
                    // terminó "naturalmente" -> pasar a STOPPED.
                    // Si el estado fue cambiado a PAUSED por pause(), o a STOPPED por stop(), no lo
                    // sobreescribimos: respetamos la intención externa.
                    if (state == State.PLAYING) {
                        state = State.STOPPED;
                        archivoActual = null; // opcional: limpiar el archivo si quieres
                    }
                    player = null;
                    cleanupStreams(); // asegurar recursos cerrados
                }
                System.out.println("DBG Reproductor: play-thread finalizado (state=" + state + ")");
            }
        }, "MP3-Player-Thread");

        playThread.start();
    }

    // ---------- PAUSE ----------
    public synchronized void pause() {
        System.out.println("DBG Reproductor: pause() called (state=" + state + ")");
        if (state != State.PLAYING || player == null) {
            System.out.println("DBG Reproductor: no se puede pausar si no está PLAYING");
            return;
        }

        // Guardar posición actual si podemos
        try {
            if (fis != null) {
                FileChannel ch = fis.getChannel();
                pausaByteOffset = ch.position();
            } else {
                pausaByteOffset = 0L;
            }
        } catch (Throwable t) {
            System.err.println("DBG Reproductor: warn al leer byte-offset: " + t);
            pausaByteOffset = 0L;
        }

        // Cerrar player para detener reproducción (esto hará que el hilo salga)
        try { player.close(); } catch (Exception ignored) {}
        player = null;

        // Cambiar estado a PAUSED y limpiar streams (los reabriremos en resume)
        state = State.PAUSED;
        cleanupStreams();

        System.out.println("DBG Reproductor: pause -> saved byteOffset=" + pausaByteOffset + " (state=" + state + ")");
    }

    // ---------- RESUME ----------
    public synchronized void resume() {
        System.out.println("DBG Reproductor: resume() called (state=" + state + ")");
        if (state != State.PAUSED) {
            System.out.println("DBG Reproductor: resume ignorado: estado no es PAUSED");
            return;
        }
        if (archivoActual == null) {
            System.out.println("DBG Reproductor: resume ignorado: archivoActual==null");
            state = State.STOPPED;
            return;
        }

        final long startOffset = pausaByteOffset;
        System.out.println("DBG Reproductor: resume -> offset=" + startOffset);

        // reabrir streams y posicionar
        try {
            fis = new FileInputStream(archivoActual);
            FileChannel ch = fis.getChannel();
            try { ch.position(startOffset); } catch (Throwable tt) {
                System.err.println("DBG Reproductor: warn position failed: " + tt);
            }
            bis = new BufferedInputStream(fis);
            player = new Player(bis);
        } catch (Exception e) {
            System.err.println("DBG Reproductor: error reabriendo archivo en resume: " + e);
            cleanupStreams();
            state = State.STOPPED;
            return;
        }

        // marcar como PLAYING antes de arrancar el hilo
        state = State.PLAYING;

        playThread = new Thread(() -> {
            try {
                System.out.println("DBG Reproductor: resume-thread starting");
                player.play();
                System.out.println("DBG Reproductor: resume-thread finished normally");
            } catch (Throwable t) {
                System.err.println("DBG Reproductor: exception en resume thread: " + t);
                t.printStackTrace();
            } finally {
                synchronized (ReproductorMP3.this) {
                    // Igual que en play(): si el estado sigue PLAYING -> terminó naturalmente -> STOPPED.
                    if (state == State.PLAYING) {
                        state = State.STOPPED;
                        archivoActual = null;
                    }
                    player = null;
                    cleanupStreams();
                }
                System.out.println("DBG Reproductor: resume-thread finalizado (state=" + state + ")");
            }
        }, "MP3-Resume-Thread");

        playThread.start();
    }

    // ---------- STOP ----------
    public synchronized void stop() {
        System.out.println("DBG Reproductor: stop() called");
        stopInternalNoReset();
        state = State.STOPPED;
        archivoActual = null;
        pausaByteOffset = 0L;
    }

    // Limpieza sin tocar state/archivoActual (uso interno por play)
    private synchronized void stopInternalNoReset() {
        if (player != null) {
            try { player.close(); } catch (Exception ignored) {}
            player = null;
        }
        if (playThread != null && playThread.isAlive()) {
            try { playThread.interrupt(); } catch (Exception ignored) {}
            playThread = null;
        }
        cleanupStreams();
    }

    private synchronized void cleanupStreams() {
        if (bis != null) {
            try { bis.close(); } catch (IOException ignored) {}
            bis = null;
        }
        if (fis != null) {
            try { fis.close(); } catch (IOException ignored) {}
            fis = null;
        }
    }

    // Estado público (útil para la UI)
    public synchronized boolean isPlaying() { return state == State.PLAYING; }
    public synchronized boolean isPaused()  { return state == State.PAUSED; }
}
