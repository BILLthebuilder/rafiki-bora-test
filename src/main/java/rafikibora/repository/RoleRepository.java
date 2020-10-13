package rafikibora.repository;

import rafikibora.model.account.Account;
import rafikibora.model.users.Roles;
import rafikibora.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Roles, Long>{
    Optional<Roles> findByName(String name);
    Optional<Roles> findByRoleName(String name);
}
