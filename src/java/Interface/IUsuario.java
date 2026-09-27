
package Interface;

import Model.Usuario;
import java.util.List;

public interface IUsuario {
    
    public List<Usuario> lista();
    public boolean insertar(Usuario usuario);
    public boolean update(Usuario usuario);
    public Usuario SearchById(int id);
    public boolean delete(int id);
    public Usuario validate(String user, String pass);
    public Usuario SearchByUsername(String username);
    public List<Usuario> SearchByRol(String rol);
    public Usuario SearchByPersonaId(int idPersona);
}
