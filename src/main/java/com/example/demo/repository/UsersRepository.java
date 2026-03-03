// UsersRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByPhoneNumber(String phoneNumber);
    Optional<Users> findByFirebaseUid(String firebaseUid);
    Optional<Users> findByRole(String role);

    boolean existsByPhoneNumber(String phoneNumber);

}