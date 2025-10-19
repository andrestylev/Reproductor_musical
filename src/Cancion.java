public class Cancion {
    private String Titulo;
    private String Duracion;
    private String anio;
    private String Genero;
    private String nombreArchivo;

    public Cancion() {}

    public Cancion(String Titulo, String Duracion, String anio, String Genero, String nombreArchivo) {
        this.Titulo = Titulo;
        this.Duracion = Duracion;
        this.anio = anio;
        this.Genero = Genero;
        this.nombreArchivo = nombreArchivo;
    }
    public String getTitulo() {
        return Titulo;
    }

    public void setTitulo(String titulo) {
        Titulo = titulo;
    }

    public String getDuracion() {
        return Duracion;
    }

    public void setDuracion(String Duracion) {
        this.Duracion = Duracion;
    }

    public String getAnio() {
        return anio;
    }

    public void setAnio(String anio) {
        this.anio = anio;
    }

    public String getGenero() {
        return Genero;
    }

    public void setGenero(String Genero) {
        this.Genero = Genero;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    @Override
    public String toString() {
        return Titulo + " (" + Duracion + ", " + anio + ", " + Genero + ")";
    }
}

