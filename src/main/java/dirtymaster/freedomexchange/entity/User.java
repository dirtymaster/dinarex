package dirtymaster.freedomexchange.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

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
