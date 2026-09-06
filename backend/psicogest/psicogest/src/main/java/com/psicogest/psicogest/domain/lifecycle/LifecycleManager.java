package com.psicogest.psicogest.domain.lifecycle;

import com.psicogest.psicogest.exception.EntityLifecycleException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LifecycleManager {

        public void deactivate(
                        DeactivatableEntity entity,
                        String reason) {

                if (Boolean.FALSE.equals(
                                entity.getActive())) {

                        throw new EntityLifecycleException(
                                        "O recurso já está desativado");
                }

                if (reason == null
                                || reason.isBlank()) {

                        throw new EntityLifecycleException(
                                        "Motivo da desativação é obrigatório");
                }

                entity.setActive(false);

                entity.setDeactivatedAt(
                                LocalDateTime.now());

                entity.setDeactivationReason(
                                reason.trim());

                entity.setReactivatedAt(null);
        }

        public void reactivate(
                        DeactivatableEntity entity) {

                if (Boolean.TRUE.equals(
                                entity.getActive())) {

                        throw new EntityLifecycleException(
                                        "O recurso já está ativo");
                }

                entity.setActive(true);

                /*
                 * Mantemos os dados da última
                 * desativação até AuditLog assumir
                 * o histórico completo.
                 */
                entity.setReactivatedAt(
                                LocalDateTime.now());
        }
}