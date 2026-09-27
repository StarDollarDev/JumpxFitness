package Interface;

import Model.BannerEvento;
import java.util.List;

public interface IBannerEvento {
    BannerEvento buscarActivo();
    List<BannerEvento> listaTodos();
    /** Inserta o actualiza (según tenga id) y, si queda activo=true, desactiva a los demás. */
    boolean guardar(BannerEvento banner);
    boolean eliminar(int idBanner);
    BannerEvento SearchById(int idBanner);
}
