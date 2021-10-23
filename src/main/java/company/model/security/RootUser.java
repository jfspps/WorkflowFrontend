package company.model.security;

import company.model.BaseEntity;
import lombok.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class RootUser extends BaseEntity {
    private String rootUserName;

    @OneToMany(mappedBy = "rootUser", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Set<User> users;
}
