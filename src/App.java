import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class App {

    private List<Artista> artistas;
    private ReproductorMP3 player;
    private Cancion cancionSeleccionada; // guarda la canción seleccionada en el árbol
    private JFrame frame;
    private JTree tree;
    private JButton btnPlay, btnTogglePause, btnStop;
    private boolean isPaused = false;
    // label para la imagen del artista
    private JLabel lblImagen;

    public App() {
        try {
            artistas = listaArtistaJson.cargarArtistasDesdeJSON("src/datos/Artistas.json");
            if (artistas == null)
                artistas = java.util.Collections.emptyList();
        } catch (IOException e) {
            artistas = java.util.Collections.emptyList();
            JOptionPane.showMessageDialog(null, "Error cargando artistas: " + e.getMessage());
        }

        player = new ReproductorMP3();
        SwingUtilities.invokeLater(this::appUI);
    }

    private void appUI() {
        frame = new JFrame("Reproductor de Música");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 600);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        frame.setContentPane(main);

        // --- LEFT: árbol ---
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Artistas");
        for (Artista a : artistas) {
            DefaultMutableTreeNode nodoArtista = new DefaultMutableTreeNode(a);
            root.add(nodoArtista);
            if (a.getCanciones() != null) {
                for (Cancion c : a.getCanciones()) {
                    DefaultMutableTreeNode nodoCancion = new DefaultMutableTreeNode(c);
                    nodoArtista.add(nodoCancion);
                }
            }

        }
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(24);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object obj = node.getUserObject();
                if (obj instanceof Artista) {
                    setText(((Artista) obj).getNombre());
                } else if (obj instanceof Cancion) {
                    setText(((Cancion) obj).getTitulo());
                } else {
                    setText(obj == null ? "" : obj.toString());
                }
                return this;
            }
        });

        // --- RIGHT: detalles y controles ---
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // -- arriba: imagen y detalles --
        JPanel topDetail = new JPanel(new BorderLayout(8, 8));
        lblImagen = new JLabel();
        lblImagen.setPreferredSize(new Dimension(400, 500));
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagen.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        topDetail.add(lblImagen, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel lblInfoTitulo = new JLabel("Título: -");
        JLabel lblInfoDuracion = new JLabel("Duración: -");
        JLabel lblInfoTiempo = new JLabel("Año: -");
        JLabel lblInfoArtista = new JLabel("Artista: -");
        details.add(lblInfoTitulo);
        details.add(Box.createVerticalStrut(6));
        details.add(lblInfoDuracion);
        details.add(Box.createVerticalStrut(6));
        details.add(lblInfoTiempo);
        details.add(Box.createVerticalStrut(6));
        details.add(lblInfoArtista);
        topDetail.add(details, BorderLayout.CENTER);

        right.add(topDetail, BorderLayout.CENTER);

        // Panel de controles (botones)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        btnPlay = crearBoton("Reproducir", e -> {
            if (cancionSeleccionada == null) {
                JOptionPane.showMessageDialog(frame, "Selecciona primero una canción.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String ruta = cancionSeleccionada.getNombreArchivo();
            if (ruta == null || ruta.trim().isEmpty()) {
                int resp = JOptionPane.showConfirmDialog(frame, "No hay archivo asociado. ¿Deseas buscarlo?",
                        "Archivo falta", JOptionPane.YES_NO_OPTION);
                if (resp == JOptionPane.YES_OPTION) {
                    buscarYAsignarArchivo(cancionSeleccionada);
                }
                return;
            }
            player.stop();
            player.play(ruta);
        });

        btnPlay = crearBoton("Reproducir", e -> onPlay());
        btnTogglePause = crearBoton("Pausar", e -> togglePause());
        btnTogglePause.setEnabled(false);
        btnStop = crearBoton("Detener", e -> onStop());

        // Botón extra: buscar archivo manualmente
        JButton btnBuscarArchivo = crearBoton("Buscar archivo...", e -> {
            if (cancionSeleccionada == null) {
                JOptionPane.showMessageDialog(frame, "Selecciona una canción primero.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            buscarYAsignarArchivo(cancionSeleccionada);
        });

        controls.add(btnPlay);
        controls.add(btnTogglePause);
        controls.add(btnStop);
        controls.add(btnBuscarArchivo);

        right.add(controls, BorderLayout.SOUTH);

        // Listener del árbol (modo manual)
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode sel = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (sel == null)
                return;
            Object user = sel.getUserObject();
            if (user instanceof Cancion) {
                Cancion c = (Cancion) user;
                cancionSeleccionada = c;
                lblInfoTitulo.setText("Título: " + safe(c.getTitulo()));
                lblInfoDuracion.setText("Duración: " + safe(c.getDuracion()));
                lblInfoTiempo.setText("Año: " + safe(c.getAnio()));
                TreeNode parent = sel.getParent();
                if (parent instanceof DefaultMutableTreeNode) {
                    Object p = ((DefaultMutableTreeNode) parent).getUserObject();
                    if (p instanceof Artista) {
                        Artista art = (Artista) p;
                        lblInfoArtista.setText("Artista: " + art.getNombre());
                        // mostrar imagen del artista (si existe)
                        ImageIcon icon = loadAndScale(art.getImagen(), 400, 400);
                        lblImagen.setIcon(icon);
                    } else {
                        lblInfoArtista.setText("Artista: -");
                        lblImagen.setIcon(null);
                    }
                } else {
                    lblInfoArtista.setText("Artista: -");
                    lblImagen.setIcon(null);
                }
            } else if (user instanceof Artista) {
                Artista a = (Artista) user;
                cancionSeleccionada = null;
                lblInfoTitulo.setText("Título: -");
                lblInfoDuracion.setText("Duración: -");
                lblInfoTiempo.setText("Año: -");
                lblInfoArtista.setText("Artista: " + a.getNombre());
                ImageIcon icon = loadAndScale(a.getImagen(), 400, 400);
                lblImagen.setIcon(icon);
            } else {
                cancionSeleccionada = null;
                lblInfoTitulo.setText("Título: -");
                lblInfoDuracion.setText("Duración: -");
                lblInfoTiempo.setText("Año: -");
                lblInfoArtista.setText("Artista: -");
                lblImagen.setIcon(null);
            }
        });

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(320, 0));
        main.add(treeScroll, BorderLayout.WEST);
        main.add(right, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private Cancion obtenerCancionSeleccionada() {
        TreePath selPath = tree.getSelectionPath();
        if (selPath == null)
            return null;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
        if (node == null)
            return null;

        Object user = node.getUserObject();
        if (user instanceof Cancion) {
            return (Cancion) user;
        }
        return null;
    }

    private void onPlay() {
        Cancion c = obtenerCancionSeleccionada();
        if (c == null) {
            JOptionPane.showMessageDialog(frame, "Selecciona una canción.");
            return;
        }
        String ruta = c.getNombreArchivo();
        if (ruta == null || ruta.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No hay archivo asociado.");
            return;
        }
        File f = new File(ruta);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(frame, "No se encontró el archivo: " + f.getAbsolutePath());
            return;
        }

        player.stop();
        player.play(ruta);
        isPaused = false;
        btnPlay.setEnabled(false);
        btnTogglePause.setEnabled(true);
        btnTogglePause.setText("Pausar");
        btnStop.setEnabled(true);
    }

    private void togglePause() {
        if (!isPaused) {
            // pedir pausar
            player.pause();
            isPaused = true;
            btnTogglePause.setText("Reanudar");
            // ajuste botones
            btnPlay.setEnabled(true); // opcional: permite volver a play desde inicio
            btnStop.setEnabled(true);
        } else {
            // pedir reanudar
            player.resume();
            // sólo si resume efectivamente cambió el estado, asumimos que resume() lo hará
            isPaused = false;
            btnTogglePause.setText("Pausar");
            btnPlay.setEnabled(false);
            btnStop.setEnabled(true);
        }
    }

    private void onStop() {
        if (player != null)
            player.stop();
        isPaused = false;
        btnPlay.setEnabled(true);
        btnTogglePause.setEnabled(false);
        btnTogglePause.setText("Pausar");
        btnStop.setEnabled(false);
    }

    // -------------------------
    // MÉTODO AUXILIAR REUTILIZABLE
    // -------------------------
    private JButton crearBoton(String texto, ActionListener action) {
        JButton boton = new JButton(texto);
        boton.setFocusable(false);
        boton.setPreferredSize(new Dimension(120, 30));
        boton.addActionListener(action);
        boton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return boton;
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }

    // -------------------------
    // Cargar y escalar imagen (soporta ruta en disco y recurso en classpath)
    // -------------------------
    private ImageIcon loadAndScale(String path, int maxW, int maxH) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        try {
            BufferedImage img = null;
            File f = new File(path);
            if (f.exists()) {
                img = ImageIO.read(f);
            } else {
                // intentar resource dentro del JAR (path relativo)
                InputStream in = getClass().getResourceAsStream("/" + path.replaceFirst("^/+", ""));
                if (in != null) {
                    img = ImageIO.read(in);
                    in.close();
                }
            }
            if (img == null)
                return null;
            int w = img.getWidth();
            int h = img.getHeight();
            double scale = Math.min((double) maxW / w, (double) maxH / h);
            int nw = Math.max(1, (int) (w * scale));
            int nh = Math.max(1, (int) (h * scale));
            Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // -------------------------
    // Abrir JFileChooser para asignar archivo mp3 a la cancion
    // -------------------------
    private void buscarYAsignarArchivo(Cancion c) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecciona archivo MP3 para: " + c.getTitulo());
        // Opcional: establecer filtro
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
        int resp = fc.showOpenDialog(frame);
        if (resp == JFileChooser.APPROVE_OPTION) {
            File sel = fc.getSelectedFile();
            c.setNombreArchivo(sel.getAbsolutePath());
            JOptionPane.showMessageDialog(frame, "Archivo asignado:\n" + sel.getAbsolutePath(), "Archivo asignado",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // MAIN
    public static void main(String[] args) {
        System.out.println(">> main started");
        try {
            new App();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
