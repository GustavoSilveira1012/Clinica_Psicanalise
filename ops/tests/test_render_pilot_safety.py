import re
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
RENDER_MANIFEST = REPOSITORY_ROOT / "render.yaml"


class RenderSyntheticPilotSafetyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = RENDER_MANIFEST.read_text(encoding="utf-8")

    def assert_env_value(self, key: str, value: str) -> None:
        pattern = rf"(?m)^\s+- key: {re.escape(key)}\s*\r?\n\s+value: {re.escape(value)}\s*$"
        self.assertRegex(self.manifest, pattern, f"Render pilot setting {key} must remain {value}")

    def assert_secret_is_not_committed(self, key: str) -> None:
        pattern = rf"(?m)^\s+- key: {re.escape(key)}\s*\r?\n\s+sync: false\s*$"
        self.assertRegex(self.manifest, pattern, f"Render secret {key} must be configured outside the manifest")

    def test_only_synthetic_api_is_defined(self):
        self.assertIn("name: psicogest-synthetic-api", self.manifest)
        self.assertNotIn("name: psicogest-synthetic-web", self.manifest)

    def test_clinical_data_and_external_modules_remain_closed(self):
        for key in (
            "CLINICAL_DATA_ENABLED",
            "CLINICAL_DATA_RELEASE_APPROVED",
            "AUTH_ACTION_MAIL_ENABLED",
            "REQUIRE_REAL_PROVIDERS",
            "REQUIRE_PAYMENT_PROVIDERS",
            "REQUIRE_NOTIFICATION_PROVIDERS",
            "NOTIFICATION_OUTBOX_SCHEDULER_ENABLED",
            "REQUIRE_NATIONAL_NFSE",
            "CLINICAL_EXPORT_STORAGE_REQUIRED",
            "CLINICAL_EXPORT_RETENTION_ENABLED",
            "SCHEDULING_ENABLED",
        ):
            self.assert_env_value(key, '"false"')

        self.assert_env_value("CLINICAL_ONLY_PILOT", '"true"')
        self.assert_env_value("CLINICAL_EXPORT_STORAGE_TYPE", "disabled")
        self.assert_env_value("JWT_COOKIE_SECURE", '"true"')
        self.assert_env_value("JPA_SHOW_SQL", '"false"')
        self.assert_env_value("REDIS_SSL_ENABLED", '"false"')
        self.assert_env_value("PUBLIC_PLANS_ENABLED", '"false"')
        self.assert_env_value("PUBLIC_PRICING_APPROVED", '"false"')
        self.assert_env_value("PUBLIC_TRIAL_ENABLED", '"false"')
        self.assert_env_value("PUBLIC_CHECKOUT_ENABLED", '"false"')
        self.assert_env_value("JWT_PUBLIC_KEY_LOCATION", "file:/etc/secrets/jwt-public.pem")
        self.assert_env_value("JWT_PRIVATE_KEY_LOCATION", "file:/etc/secrets/jwt-private.pem")

    def test_credentials_are_external_render_secrets(self):
        for key in (
            "DATABASE_URL",
            "DATABASE_USERNAME",
            "DATABASE_PASSWORD",
            "SECURITY_ALLOWED_ORIGINS",
            "JWT_KEY_ID",
            "MFA_ENCRYPTION_KEY",
            "CLINICAL_KEK",
            "AUDIT_HMAC_KEY",
            "REDIS_URL",
        ):
            self.assert_secret_is_not_committed(key)

    def test_api_runtime_does_not_receive_migration_credentials(self):
        self.assertNotIn("MIGRATION_DATABASE_USERNAME", self.manifest)
        self.assertNotIn("MIGRATION_DATABASE_PASSWORD", self.manifest)
        production_config = (
            REPOSITORY_ROOT
            / "backend/psicogest/psicogest/src/main/resources/application-production.properties"
        ).read_text(encoding="utf-8")
        self.assertIn("spring.flyway.enabled=false", production_config)
        self.assertNotIn("${MIGRATION_DATABASE_USERNAME}", production_config)
        self.assertNotIn("${MIGRATION_DATABASE_PASSWORD}", production_config)

    def test_private_redis_url_is_required_by_production_profile(self):
        production_config = (
            REPOSITORY_ROOT
            / "backend/psicogest/psicogest/src/main/resources/application-production.properties"
        ).read_text(encoding="utf-8")
        self.assertIn("spring.data.redis.url=${REDIS_URL}", production_config)
        self.assertNotIn("spring.data.redis.password=${REDIS_PASSWORD}", production_config)

    def test_render_health_check_targets_the_readiness_probe(self):
        self.assertIn("healthCheckPath: /actuator/health/readiness", self.manifest)

    def test_synthetic_service_requires_manual_deploy(self):
        self.assertIn("autoDeployTrigger: 'off'", self.manifest)


if __name__ == "__main__":
    unittest.main()
