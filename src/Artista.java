 
import java.util.List;

public class Artista {
    private String Nombre;
    private String Tipo;
    private String Pais;
    private String imagen;
    private List<Cancion> Canciones;

    public Artista() {}

    public Artista(String Nombre, String Tipo, String Pais, String imagen, List<Cancion> Canciones) {
        this.Nombre = Nombre;
        this.Tipo = Tipo;
        this.Pais = Pais;
        this.imagen = imagen;
        this.Canciones = Canciones;
    }
    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getNombre() {
        return Nombre;
    }
    public void setNombre(String Nombre) {
        this.Nombre = Nombre;
    }
    public String getTipo() {
        return Tipo;
    }
    public void setTipo(String Tipo) {
        this.Tipo = Tipo;
    }
    public String getPais() {
        return Pais;
    }
    public void setPais(String Pais) {
        this.Pais = Pais;
    }
    public List<Cancion> getCanciones() {
        return Canciones;
    }
    public void setCanciones(List<Cancion> Canciones) {
        this.Canciones = Canciones;
    }
    public String toString() {
        return Nombre + " (" + Tipo + ", " + Pais + ")";
    }

}
