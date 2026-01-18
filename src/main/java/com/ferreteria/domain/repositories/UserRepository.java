package com.ferreteria.domain.repositories;

import com.ferreteria.domain.entities.User;
import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) para el repositorio de usuarios.
 * Define el contrato sin conocer la implementación.
 */
public interface UserRepository {

    Optional<User> findById(int id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    List<User> findAllActive();

    User save(User user);

    void delete(int id);

    boolean existsByUsername(String username);

    int count();
}
