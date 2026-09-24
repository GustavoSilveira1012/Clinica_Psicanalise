package com.psicogest.psicogest.security;

import com.psicogest.psicogest.security.config.ProductionDatabaseRoleValidator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionDatabaseRoleValidatorTest {
    @Test void deniesSuperuserBypassOrMissingDatabaseRole() {
        for (Boolean unsafe : new Boolean[]{true, null}) {
            var jdbc = mock(JdbcTemplate.class);
            when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(unsafe);
            assertThatThrownBy(() -> new ProductionDatabaseRoleValidator(jdbc).afterSingletonsInstantiated())
                    .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void permitsRestrictedRole() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        assertThatCode(() -> new ProductionDatabaseRoleValidator(jdbc).afterSingletonsInstantiated()).doesNotThrowAnyException();
    }
}
