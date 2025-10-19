import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class listaArtistaJson {
    public static List<Artista> cargarArtistasDesdeJSON(String rutaArchivo) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DatosArtistas wrapper = mapper.readValue(new File(rutaArchivo), DatosArtistas.class);
        return wrapper.getArtistas();
    }

    // Clase interna para reflejar la estructura raíz del JSON
    public static class DatosArtistas {
        @JsonProperty("artistas")
        private List<Artista> Artistas;

        public List<Artista> getArtistas() {
            return Artistas;
        }

        public void setArtistas(List<Artista> artistas) {
            this.Artistas = artistas;
        }
    }

    
}