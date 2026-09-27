package Interface;

import Model.GaleriaItem;
import java.util.List;

public interface IGaleriaItem {
    List<GaleriaItem> listaActivos();
    List<GaleriaItem> listaTodos();
    boolean insertar(GaleriaItem item);
    boolean eliminar(int idItem);
    GaleriaItem SearchById(int idItem);
    boolean actualizarOrden(int idItem, int nuevoOrden);
}
