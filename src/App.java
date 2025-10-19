import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
 
public class App {

    private List<Artista> artistas;
    private ReproductorMP3 player;
    private Cancion cancionSeleccionada;
    private JFrame frame;
    private JTree tree;
    private JLabel lblImagen;
    private JButton btnPlayPause;
    private JButton btnStop;
    private JButton lyrics;

    // json cargador
    public App() {
        try {
            artistas = listaArtistaJson.cargarArtistasDesdeJSON("src/datos/Artistas.json");
            if (artistas == null)
                artistas = java.util.Collections.emptyList();
        } catch (Exception e) {
            artistas = java.util.Collections.emptyList();
            JOptionPane.showMessageDialog(null, "error cargando artistas: " + e.getMessage());
        }
        player = new ReproductorMP3();
        SwingUtilities.invokeLater(this::appUI);
    }

    // UI
    private void appUI() {
        frame = new JFrame("Reproductor De Música");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 650);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(6, 11));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.setBorder(BorderFactory.createLineBorder(Color.black, 2));
        main.setBackground(Color.black);
        frame.setContentPane(main);

        // arrbol izquierda
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Artistas");
        for (Artista a : artistas) {
            DefaultMutableTreeNode nodoArtista = new DefaultMutableTreeNode(a);
            root.add(nodoArtista);
            if (a.getCanciones() != null) {
                for (Cancion c : a.getCanciones()) {
                    nodoArtista.add(new DefaultMutableTreeNode(c));
                }
            }
        }
        DefaultTreeModel model = new DefaultTreeModel(root);

        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(28);
        tree.setOpaque(true);
        tree.setBackground(Color.darkGray);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object obj = node.getUserObject();

                if (obj instanceof Artista)
                    setText(((Artista) obj).getNombre());
                else if (obj instanceof Cancion) {
                    setText(((Cancion) obj).getTitulo());
                } else
                    setText(obj == null ? "" : obj.toString());
                // colores y fuentes de los nodos
                setOpaque(true);

                if (sel) {
                    setBackground(new Color(19, 168, 2));
                    setForeground(Color.BLACK); 
                } else {
                    setBackground(Color.DARK_GRAY);
                    setForeground(Color.WHITE); 
                }
                
                setFont(new Font("SansSerif", Font.BOLD, 13));

                return this;
            }
        });

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(300, 0));
        treeScroll.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        treeScroll.setBackground(Color.darkGray);
        treeScroll.setForeground(Color.white);
        treeScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(19, 168, 2), 3, true),
                "Biblioteca", JLabel.CENTER, JLabel.CENTER, new Font("SansSerif", Font.BOLD, 14), Color.white));
        main.add(treeScroll, BorderLayout.WEST);

        // detalles y controles y imagen derecha
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        right.setBackground(Color.darkGray);

        lblImagen = new JLabel();
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagen.setPreferredSize(new Dimension(350, 350));
        lblImagen.setBorder(BorderFactory.createLineBorder(Color.black, 3, true));
        right.add(lblImagen, BorderLayout.CENTER);

        // Informacion
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(new Color(19, 168, 2));
        info.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        info.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        JLabel lblTitulo = new JLabel("Título: -");
        JLabel lblDuracion = new JLabel("Duración: -");
        JLabel lblAnio = new JLabel("Año: -");
        JLabel lblArtista = new JLabel("Artista: -");
        info.add(lblTitulo);
        info.add(Box.createVerticalStrut(6));
        info.add(lblDuracion);
        info.add(Box.createVerticalStrut(6));
        info.add(lblAnio);
        info.add(Box.createVerticalStrut(6));
        info.add(lblArtista);

        //lyrics = crearBoton("Lyrics", e -> verLetras());
        //lyrics.setLayout(new FlowLayout(FlowLayout.RIGHT, 18, 7));
        //info.add(lyrics);

        right.add(info, BorderLayout.NORTH);

        // Controles boton toggle y stop
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 7));
        controls.setBackground(new Color(19, 168, 2));
        controls.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 6));

        btnPlayPause = crearBoton("Play", e -> togglePlayPause());
        btnPlayPause.setBackground(Color.green);
        btnPlayPause.setForeground(Color.BLACK);
        btnPlayPause.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        btnPlayPause.setFont(new Font("SansSerif", Font.BOLD, 14));

        btnStop = crearBoton("Stop", e -> {
            btnStop.setForeground(Color.BLACK);
            player.stop();
            btnPlayPause.setText("Play");
            btnPlayPause.setEnabled(true);
            btnPlayPause.setBackground(Color.green);
            btnPlayPause.setForeground(Color.BLACK);
            btnStop.setEnabled(false);

        });

        btnStop.setEnabled(false);
        btnStop.setBackground(Color.red);
        btnStop.setForeground(Color.BLACK);
        btnStop.setFont(new Font("SansSerif", Font.BOLD, 14));

        controls.add(btnPlayPause);
        controls.add(btnStop);
        controls.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        right.add(controls, BorderLayout.SOUTH);

        main.add(right, BorderLayout.CENTER);

        // Listeners de arbol
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode sel = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (sel == null)
                return;
            Object user = sel.getUserObject();

            if (user instanceof Cancion) {
                Cancion c = (Cancion) user;
                cancionSeleccionada = c;
                lblTitulo.setText("Título: " + safe(c.getTitulo()));
                lblDuracion.setText("Duración: " + safe(c.getDuracion()));
                lblAnio.setText("Año: " + safe(c.getAnio()));
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) sel.getParent();

                if (parent != null && parent.getUserObject() instanceof Artista) {
                    Artista art = (Artista) parent.getUserObject();
                    lblArtista.setText("Artista: " + art.getNombre());
                    ImageIcon icon = loadAndScale(art.getImagen(), 350, 350);
                    lblImagen.setIcon(icon);
                } else {
                    lblArtista.setText("Artista: -");
                    lblImagen.setIcon(null);
                }
            } else if (user instanceof Artista) {
                Artista a = (Artista) user;
                cancionSeleccionada = null;
                lblTitulo.setText("Título: -");
                lblDuracion.setText("Duración: -");
                lblAnio.setText("Año: -");
                lblArtista.setText("Artista: " + a.getNombre());
                lblImagen.setIcon(loadAndScale(a.getImagen(), 350, 350));

            } else {
                cancionSeleccionada = null;
                lblTitulo.setText("Título: -");
                lblDuracion.setText("Duración: -");
                lblAnio.setText("Año: -");
                lblArtista.setText("Artista: -");
                lblImagen.setIcon(null);
            }
        });

        frame.setVisible(true);
    }

    // Toggle Play/Pause/Resume logica y validaciones
    private void togglePlayPause() {
        if (cancionSeleccionada == null) {
            JOptionPane.showMessageDialog(frame, "Selecciona primero una canción.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String ruta = cancionSeleccionada.getNombreArchivo();
        if (ruta == null || ruta.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "La canción no tiene archivo asignado.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        File f = new File(ruta);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(frame, "No se encontró el archivo: " + f.getAbsolutePath(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Estado: STOPPED -> iniciar; PLAYING -> pausar; PAUSED -> reanudar
        if (!player.isPlaying() && !player.isPaused()) {
            // start
            player.stop();
            player.play(ruta);
            btnPlayPause.setText("Pause");
            btnPlayPause.setBackground(Color.LIGHT_GRAY);
            btnStop.setEnabled(true);

        } else if (player.isPlaying()) {
            // pause
            player.pause();
            btnPlayPause.setText("Resume");
            btnPlayPause.setBackground((new Color(200, 245, 100)));
            // mantener stop habilitado
            btnStop.setEnabled(true);

        } else if (player.isPaused()) {
            // resume
            player.resume();
            btnPlayPause.setText("Pause");
            btnPlayPause.setBackground(Color.LIGHT_GRAY);
            btnStop.setEnabled(true);
        }
    }

    // metodo que crea botones
    private JButton crearBoton(String texto, ActionListener action) {
        JButton boton = new JButton(texto);
        boton.setFocusable(false);
        boton.setPreferredSize(new Dimension(110, 30));
        boton.addActionListener(action);
        boton.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        return boton;
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }

    // cargar y escalar imagen
    private ImageIcon loadAndScale(String path, int maxW, int maxH) {
        if (path == null || path.trim().isEmpty())
            return null;
        try {
            File f = new File(path);
            if (f.exists()) {
                ImageIcon icon = new ImageIcon(path);
                Image img = icon.getImage().getScaledInstance(maxW, maxH, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            } else {
                java.io.InputStream in = getClass().getResourceAsStream("/" + path.replaceFirst("^/+", ""));
                if (in != null) {
                    Image img = javax.imageio.ImageIO.read(in);
                    Image scaled = img.getScaledInstance(maxW, maxH, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // MAIN
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }
}
