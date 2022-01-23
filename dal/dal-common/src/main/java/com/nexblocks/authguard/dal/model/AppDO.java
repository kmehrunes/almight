package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "apps")
@NamedQuery(
        name = "apps.getById",
        query = "SELECT app FROM AppDO app WHERE app.id = :id AND app.deleted = false"
)
@NamedQuery(
        name = "apps.getByExternalId",
        query = "SELECT app FROM AppDO app WHERE app.externalId = :externalId AND app.deleted = false"
)
@NamedQuery(
        name = "apps.getByAccountId",
        query = "SELECT app FROM AppDO app WHERE app.parentAccountId = :parentAccountId AND app.deleted = false"
)
public class AppDO extends AbstractDO {
    private String externalId;
    private String name;
    private String parentAccountId;
    private String domain;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<PermissionDO> permissions;

    private boolean active;
}
