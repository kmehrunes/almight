package org.auther.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AccountDTO.class)
@JsonDeserialize(as = AccountDTO.class)
public interface Account {
    @Nullable String getId();
    String getUsername();
    @Nullable String getPlainPassword();
    List<String> getPermissions();
    String getRole();
    boolean isActive();
    boolean isDeleted();
}
