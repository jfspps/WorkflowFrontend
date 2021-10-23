package company.model.security;

import company.model.BaseEntity;
import company.model.TestRecord;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.transaction.Transactional;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Set;
import java.util.stream.Collectors;

//USER <--> ROLE <--> AUTHORITY

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Entity
public class User extends BaseEntity implements UserDetails, CredentialsContainer {

    @Size(min = 1, max = 255)
    private String username;

    @Size(min = 8, max = 255)
    private String password;

    // Singular (Lombok) builds a singular Set with one Authority in authorities, called "authority"
    @Singular
//    Adding CascadeType.PERSIST is problematic for User
    @ManyToMany(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = {@JoinColumn(name = "USER_ID", referencedColumnName = "ID")},
            inverseJoinColumns = {@JoinColumn(name = "ROLE_ID", referencedColumnName = "ID")})
    private Set<Role> roles;

    // recall from the DB as needed (when User is injected into the context, converting Authorities
    // from Roles is handled here instead of through JPAUserDetails (ensure roles is loaded eagerly,
    // not lazily, above)
    @Transactional
    public Set<GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(Role::getAuthorities)
                .flatMap(Set::stream)
                .map(authority -> new SimpleGrantedAuthority(authority.getPermission()))
                .collect(Collectors.toSet());
    }

    // adding other Spring's UserDetails interface properties
    // override the null value with true
    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean accountNonExpired = true;

    @Builder.Default
    private Boolean accountNonLocked = true;

    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    // Privileges Root > Admin > Client;
    // preparing for multi-tenancy (multiple User models with access to one account)

    // RootUser is granted all DB credentials
    @ManyToOne(fetch = FetchType.EAGER)
    private RootUser rootUser;

    // AdminUser < RootUser
    @ManyToOne(fetch = FetchType.EAGER)
    private AdminUser adminUser;

    // ClientUser < AdminUser
    @ManyToOne(fetch = FetchType.EAGER)
    private ClientUser clientUser;

    // testRecord mappings, one user to many testRecords
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<TestRecord> testRecords;

    // these are standard JPA annotations and can be applied to any entity which requires creation
    // and modification dates
    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdDate;

    @UpdateTimestamp
    private Timestamp lastModifiedDate;
}
