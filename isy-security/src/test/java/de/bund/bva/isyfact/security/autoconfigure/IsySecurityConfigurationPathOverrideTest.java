package de.bund.bva.isyfact.security.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.bund.bva.isyfact.security.config.IsySecurityConfigurationProperties;
import de.bund.bva.isyfact.security.xmlparser.RolePrivilegesMapper;

@SpringBootTest(
        classes = IsySecurityConfigurationPathOverrideTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("role-privileges-override")
class IsySecurityConfigurationPathOverrideTest {

    @Autowired
    private IsySecurityConfigurationProperties properties;

    @Autowired
    private RolePrivilegesMapper mapper;

    @Test
    void loadsRolePrivilegesMappingFileFromApplicationYaml() {
        assertThat(properties).isNotNull();
        assertThat(properties.getRolePrivilegesMappingFile()).isNotNull();
        assertThat(properties.getRolePrivilegesMappingFile().exists()).isTrue();
        assertThat(properties.getRolePrivilegesMappingFile().getDescription())
                .contains("secure-resources/sicherheit/rollenrechte.xml");

        assertThat(mapper).isNotNull();
        assertThat(mapper.getApplicationId()).isEqualTo("SECURE_TEST");

        assertThat(mapper.getAllPrivileges())
                .containsExactlyInAnyOrder("Recht_SECURE_TEST", "Recht_B", "Recht_C");

        assertThat(mapper.getPrivilegesByRoles(java.util.List.of("Rolle_SECURE_TEST")))
                .containsExactly("Recht_SECURE_TEST");
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            IsySecurityAutoConfiguration.class,
            SecurityAutoConfiguration.class
    })
    static class TestApplication {
    }
}