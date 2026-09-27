package Dao;

import Interface.IPersona;
import Model.Persona;
import Model.Usuario;
import Util.ConexionSqlSingleton;
import java.util.List;
import java.sql.*;
import java.util.ArrayList;

public class PersonaDaoImpl implements IPersona {

    private Connection cn;

    @Override
    public List<Persona> lista() {
        List<Persona> lista = new ArrayList<>();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM persona ORDER BY id_persona";
            st = cn.prepareStatement(query);
            rs = st.executeQuery();
            while (rs.next()) {
                Persona p = new Persona();
                p.setId_persona(rs.getInt("id_persona"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));
                p.setDocumento(rs.getString("documento"));
                p.setNumeroDoc(rs.getString("numero_Doc"));
                p.setTelefono(rs.getString("telefono"));
                lista.add(p);
            }
        } catch (Exception e) {
            System.out.println("Error al listar esta persona: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return lista;
    }

    @Override
    public boolean insertSoloPersona(Persona p, Usuario u) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean resultado = false;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "INSERT INTO persona (nombre, apellido, documento, numero_Doc, telefono) "
                    + "VALUES(?,?,?,?,?)";
            st = cn.prepareStatement(query, new String[]{"id_persona"});
            st.setString(1, p.getNombre());
            st.setString(2, p.getApellido());
            st.setString(3, p.getDocumento());
            st.setString(4, p.getNumeroDoc());
            st.setString(5, p.getTelefono());

            int r = st.executeUpdate();
            resultado = r > 0;

            if (resultado) {
                rs = st.getGeneratedKeys();
                if (rs.next()) {
                    p.setId_persona(rs.getInt(1));
                }
                if (u != null) {
                    st.close();
                    String contraseñaSegura = u.getContraseña();
                    if (contraseñaSegura == null
                            || !contraseñaSegura.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) {
                        contraseñaSegura = org.mindrot.jbcrypt.BCrypt.hashpw(
                                contraseñaSegura, org.mindrot.jbcrypt.BCrypt.gensalt(12));
                    }
                    String queryUsuario = "INSERT INTO usuario (usuario, contrasena, rol, id_persona) "
                            + "VALUES (?, ?, ?, ?)";
                    st = cn.prepareStatement(queryUsuario);
                    st.setString(1, u.getUsuario());
                    st.setString(2, contraseñaSegura);
                    st.setString(3, u.getRol().name());
                    st.setInt(4, p.getId_persona());
                    st.executeUpdate();
                }
                System.out.println("Persona registrada correctamente con ID: " + p.getId_persona());
            }
        } catch (Exception e) {
            System.out.println("Error al insertar persona: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return resultado;
    }

    @Override
    public boolean update(Persona p) {
        PreparedStatement st = null;
        boolean resultado = false;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "UPDATE persona SET nombre=?, apellido=?, "
                    + "documento=?, numero_Doc=?, telefono=? "
                    + "WHERE id_persona=?";
            st = cn.prepareStatement(query);
            st.setString(1, p.getNombre());
            st.setString(2, p.getApellido());
            st.setString(3, p.getDocumento());
            st.setString(4, p.getNumeroDoc());
            st.setString(5, p.getTelefono());
            st.setInt(6, p.getId_persona());

            int r = st.executeUpdate();
            resultado = r > 0;
        } catch (Exception e) {
            System.out.println("Error al actualizar persona: " + e.getMessage());
        } finally {
            cerrarRecursos(null, st);
        }
        return resultado;
    }

    @Override
    public Persona SearchById(int id) {
        Persona persona = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM persona WHERE id_persona = ?";
            st = cn.prepareStatement(query);
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                persona = new Persona();
                persona.setId_persona(rs.getInt("id_persona"));
                persona.setNombre(rs.getString("nombre"));
                persona.setApellido(rs.getString("apellido"));
                persona.setDocumento(rs.getString("documento"));
                persona.setNumeroDoc(rs.getString("numero_Doc"));
                persona.setTelefono(rs.getString("telefono"));
            }
        } catch (Exception e) {
            System.out.println("Error al buscar la persona: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return persona;
    }

    @Override
    public boolean delete(int id) {
        PreparedStatement st = null;
        boolean resultado = false;
        try {
            cn = ConexionSqlSingleton.getConnection();
            
            // Eliminar usuario asociado primero
            try {
                String queryUsuario = "DELETE FROM usuario WHERE id_persona = ?";
                st = cn.prepareStatement(queryUsuario);
                st.setInt(1, id);
                st.executeUpdate();
            } catch (Exception e) {
                System.out.println("Error al eliminar usuario: " + e.getMessage());
            }
            
            // Luego eliminar persona
            st.close();
            String queryPersona = "DELETE FROM Persona WHERE id_persona = ?";
            st = cn.prepareStatement(queryPersona);
            st.setInt(1, id);
            int r = st.executeUpdate();
            resultado = r > 0;
        } catch (Exception e) {
            System.out.println("Error al elimianr esta persona: " + e.getMessage());
        } finally {
            cerrarRecursos(null, st);
        }
        return resultado;
    }
    
    @Override
    public Persona SearchByDocumento(String documento, String numeroDoc) {
        Persona persona = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM persona WHERE documento = ? AND numero_Doc = ?";
            st = cn.prepareStatement(query);
            st.setString(1, documento);
            st.setString(2, numeroDoc);
            rs = st.executeQuery();
            if (rs.next()) {
                persona = new Persona();
                persona.setId_persona(rs.getInt("id_persona"));
                persona.setNombre(rs.getString("nombre"));
                persona.setApellido(rs.getString("apellido"));
                persona.setDocumento(rs.getString("documento"));
                persona.setNumeroDoc(rs.getString("numero_Doc"));
                persona.setTelefono(rs.getString("telefono"));
                System.out.println("Persona encontrada por documento: " + documento + " - " + numeroDoc);
            } else {
                System.out.println("No se encontró persona con documento: " + documento + " - " + numeroDoc);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar persona por documento: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return persona;
    
    }

    @Override
    public boolean existeNumeroDoc(String numeroDoc) {
        return existeValor("SELECT 1 FROM persona WHERE numero_Doc = ?", numeroDoc);
    }

    @Override
    public boolean existeTelefono(String telefono) {
        return existeValor("SELECT 1 FROM persona WHERE telefono = ?", telefono);
    }

    private boolean existeValor(String query, String valor) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            st = cn.prepareStatement(query);
            st.setString(1, valor);
            rs = st.executeQuery();
            return rs.next();
        } catch (Exception e) {
            System.out.println("Error al verificar existencia: " + e.getMessage());
            return false;
        } finally {
            cerrarRecursos(rs, st);
        }
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement st) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
        } catch (Exception ex) {
            System.out.println("Error cerrando recursos: " + ex.getMessage());
        }
    }

    
}
