package kcs.funding.fundingboost.init;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberRoleSchemaMigrationRunner implements ApplicationRunner {

    private static final String TABLE_NAME = "member";
    private static final String COLUMN_NAME = "member_role";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }

        Integer columnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                TABLE_NAME,
                COLUMN_NAME
        );

        if (columnCount == null || columnCount == 0) {
            log.info("member_role 컬럼이 아직 생성되지 않아 스키마 보정을 건너뜁니다.");
            return;
        }

        String dataType = jdbcTemplate.queryForObject(
                """
                SELECT DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                String.class,
                TABLE_NAME,
                COLUMN_NAME
        );

        String columnType = jdbcTemplate.queryForObject(
                """
                SELECT COLUMN_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                String.class,
                TABLE_NAME,
                COLUMN_NAME
        );

        if (!"varchar".equalsIgnoreCase(dataType) || columnType == null || !columnType.toLowerCase().startsWith("varchar(20")) {
            log.info("member_role 컬럼 타입 보정 시작: dataType={}, columnType={}", dataType, columnType);
            jdbcTemplate.execute(
                    "ALTER TABLE `member` MODIFY COLUMN `member_role` VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER'"
            );
        }

        int normalizedUserCount = jdbcTemplate.update(
                """
                UPDATE `member`
                SET `member_role` = 'ROLE_USER'
                WHERE `member_role` IS NULL
                   OR TRIM(`member_role`) = ''
                   OR `member_role` = 'USER'
                """
        );

        int normalizedAdminCount = jdbcTemplate.update(
                """
                UPDATE `member`
                SET `member_role` = 'ROLE_ADMIN'
                WHERE `member_role` = 'ADMIN'
                """
        );

        if (normalizedUserCount > 0 || normalizedAdminCount > 0) {
            log.info("member_role 값 정규화 완료: ROLE_USER={}, ROLE_ADMIN={}", normalizedUserCount, normalizedAdminCount);
        }
    }

    private boolean isMySql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null && databaseProductName.toLowerCase().contains("mysql");
        } catch (SQLException e) {
            log.warn("DB 타입 확인에 실패하여 member_role 스키마 보정을 건너뜁니다.", e);
            return false;
        }
    }
}
