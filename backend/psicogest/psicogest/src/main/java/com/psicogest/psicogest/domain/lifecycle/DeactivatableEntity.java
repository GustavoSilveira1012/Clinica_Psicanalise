package com.psicogest.psicogest.domain.lifecycle;

import java.time.LocalDateTime;

public interface DeactivatableEntity {

        Boolean getActive();

        void setActive(Boolean active);

        LocalDateTime getDeactivatedAt();

        void setDeactivatedAt(
                        LocalDateTime deactivatedAt);

        String getDeactivationReason();

        void setDeactivationReason(
                        String deactivationReason);

        LocalDateTime getReactivatedAt();

        void setReactivatedAt(
                        LocalDateTime reactivatedAt);
}