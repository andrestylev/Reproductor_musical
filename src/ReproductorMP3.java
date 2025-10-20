import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class ReproductorMP3 {

    private enum State {
        STOPPED, PLAYING, PAUSED
    }

    private Player player;
    private Thread playThread;

    private FileInputStream fis;
    private BufferedInputStream bis;

    private volatile State state = State.STOPPED;
    private String archivoActual = null;
    private long pausaByteOffset = 0L;

    // reproductor play
    public synchronized void play(String rutaArchivo) {
        stopInternalNoReset();

        File f = new File(rutaArchivo);
        if (!f.exists()) {
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
            cleanupStreams();
            state = State.STOPPED;
            return;
        }

        playThread = new Thread(() -> {
            try {              
                player.play();              
            } catch (Throwable t) {
                
                t.printStackTrace();
            } finally {
                synchronized (ReproductorMP3.this) {
                    if (state == State.PLAYING) {
                        state = State.STOPPED;
                        archivoActual = null;
                    }
                    player = null;
                    cleanupStreams();
                }               
            }
        }, "MP3-Player-Thread");

        playThread.start();
    }

    // pausar
    public synchronized void pause() {
        if (state != State.PLAYING || player == null) {
            return;
        }
        try {
            if (fis != null) {
                FileChannel ch = fis.getChannel();
                pausaByteOffset = ch.position();
            } else {
                pausaByteOffset = 0L;
            }
        } catch (Throwable t) {
            pausaByteOffset = 0L;
        }

        try {
            player.close();
        } catch (Exception ignored) {
        }
        player = null;

        state = State.PAUSED;
        cleanupStreams();
    }

    // reanudar
    public synchronized void resume() {
        if (state != State.PAUSED) {
            return;
        }
        if (archivoActual == null) {
            state = State.STOPPED;
            return;
        }

        final long startOffset = pausaByteOffset;
        try {
            fis = new FileInputStream(archivoActual);
            FileChannel ch = fis.getChannel();
            try {
                ch.position(startOffset);
            } catch (Throwable tt) {

            }
            bis = new BufferedInputStream(fis);
            player = new Player(bis);
        } catch (Exception e) {

            cleanupStreams();
            state = State.STOPPED;
            return;
        }

        state = State.PLAYING;

        playThread = new Thread(() -> {
            try {
                player.play();
            } catch (Throwable t) {

                t.printStackTrace();
            } finally {
                synchronized (ReproductorMP3.this) {
                    if (state == State.PLAYING) {
                        state = State.STOPPED;
                        archivoActual = null;
                    }
                    player = null;
                    cleanupStreams();
                }
            }
        }, "MP3-Resume-Thread");

        playThread.start();
    }

    // detener
    public synchronized void stop() {
        stopInternalNoReset();
        state = State.STOPPED;
        archivoActual = null;
        pausaByteOffset = 0L;
    }

    private synchronized void stopInternalNoReset() {
        if (player != null) {
            try {
                player.close();
            } catch (Exception ignored) {
            }
            player = null;
        }
        if (playThread != null && playThread.isAlive()) {
            try {
                playThread.interrupt();
            } catch (Exception ignored) {
            }
            playThread = null;
        }
        cleanupStreams();
    }
    // limpiar los streams
    private synchronized void cleanupStreams() {
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException ignored) {
            }
            bis = null;
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ignored) {
            }
            fis = null;
        }
    }

    // Estado publico  
    public synchronized boolean isPlaying() {
        return state == State.PLAYING;
    }

    public synchronized boolean isPaused() {
        return state == State.PAUSED;
    }
}
