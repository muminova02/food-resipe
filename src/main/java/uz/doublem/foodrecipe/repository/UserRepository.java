package uz.doublem.foodrecipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.doublem.foodrecipe.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
        Boolean existsByEmail(String email);
        Optional<User> findByEmail(String email);
}
