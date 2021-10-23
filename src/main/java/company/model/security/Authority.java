package company.model.security;

import company.model.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.util.Set;

//USER <--> ROLE <--> AUTHORITY

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Authority extends BaseEntity {

    private String permission;

    @ManyToMany(mappedBy = "authorities")
    private Set<Role> roles;
}