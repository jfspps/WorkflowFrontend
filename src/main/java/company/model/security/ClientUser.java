package company.model.security;

//Multi-tenancy: each ClientUser is part of a group of ClientUsers, all of whom access one User account (i.e. one User object,
//in which the credentials and account status are stored)

import company.model.BaseEntity;
import lombok.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public
class ClientUser extends BaseEntity {

    @Size(min = 1, max = 255)
    private String clientUserName;

    @OneToMany(mappedBy = "clientUser", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Set<User> users;
}
