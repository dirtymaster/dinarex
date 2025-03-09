package dirtymaster.freedomexchange.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "user_datas")
public class UserData {
    @Id
    @GeneratedValue
    private UUID id;

    private String username;

    /**
     * Email пользователя
     */
    private String email;

    /**
     * Telegram
     */
    private String telegram;
}
