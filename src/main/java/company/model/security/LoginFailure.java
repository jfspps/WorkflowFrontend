package company.model.security;

import company.model.BaseEntity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class LoginFailure extends BaseEntity {

    @ManyToOne
    private User user;

    private String sourceIP;

    private String usernameEntered;

    //these are standard JPA annotations and can be applied to any entity which requires creation and modification dates
    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdDate;

    @UpdateTimestamp
    private Timestamp lastModifiedDate;
}