package dirtymaster.freedomexchange.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    /**
     * Никнейм пользователя
     */
    @Id
    private String username;

    /**
     * Хеш пароля
     */
    private String password;

    /**
     * Признак активности пользователя
     */
    private boolean enabled;
}
