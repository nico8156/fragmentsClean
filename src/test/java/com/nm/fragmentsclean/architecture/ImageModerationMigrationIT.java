package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

class ImageModerationMigrationIT {
  @Test void journaled_upgrade_preserves_existing_media_and_replays_once() throws Exception {
    try(var postgres=new PostgreSQLContainer<>("postgres:15-alpine")) {
      postgres.start();
      postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("src/main/resources/db/release")),"/release");
      postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("infra/aws/compose/platform/staging/fragments/render-release-migration.sh")),"/render.sh");
      var setup=postgres.execInContainer("psql","-U",postgres.getUsername(),"-d",postgres.getDatabaseName(),"-v","ON_ERROR_STOP=1","-c","""
        CREATE TABLE release_schema_history(version TEXT PRIMARY KEY,checksum TEXT,source_revision TEXT);
        INSERT INTO release_schema_history VALUES('avatar-media-lifecycle-2026-10',repeat('0',64),repeat('a',40));
        CREATE TABLE experience_media(status TEXT CONSTRAINT ck_experience_media_status CHECK(status IN('PENDING','AVAILABLE','DELETION_PENDING','DELETED')));
        CREATE TABLE experience_media_views(status TEXT CONSTRAINT ck_experience_media_view_status CHECK(status IN('PENDING','AVAILABLE','DELETION_PENDING','DELETED')));
        CREATE TABLE user_avatar_media(status TEXT CONSTRAINT ck_user_avatar_media_status CHECK(status IN('PENDING','AVAILABLE','RETIRED','DELETION_PENDING','DELETED')));
        CREATE TABLE media_catalog_entries(status TEXT CHECK(status IN('PENDING','AVAILABLE','DELETION_PENDING','DELETED')));
        INSERT INTO experience_media VALUES('AVAILABLE');
        INSERT INTO user_avatar_media VALUES('RETIRED');
        """);
      assertThat(setup.getExitCode()).as(setup.getStderr()).isZero();
      for(int i=0;i<2;i++) {
        var result=postgres.execInContainer("bash","-c","""
          bash /render.sh /release "$1" image-moderation-2026-10.psql >/migration.psql &&
          psql -U "$2" -d "$3" -v ON_ERROR_STOP=1 -f /migration.psql
          ""","render","a".repeat(40),postgres.getUsername(),postgres.getDatabaseName());
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
      }
      var result=postgres.execInContainer("psql","-U",postgres.getUsername(),"-d",postgres.getDatabaseName(),"-v","ON_ERROR_STOP=1","-At","-c","""
        INSERT INTO experience_media VALUES('REVIEW_REQUIRED'),('REJECTED');
        INSERT INTO experience_media_views VALUES('REVIEW_REQUIRED'),('REJECTED');
        INSERT INTO user_avatar_media VALUES('REVIEW_REQUIRED'),('REJECTED');
        INSERT INTO media_catalog_entries VALUES('REVIEW_REQUIRED'),('REJECTED');
        SELECT count(*) FROM experience_media WHERE status='AVAILABLE';
        SELECT count(*) FROM user_avatar_media WHERE status='RETIRED';
        SELECT count(*) FROM release_schema_history WHERE version='image-moderation-2026-10';
        """);
      assertThat(result.getExitCode()).as(result.getStderr()).isZero();
      assertThat(result.getStdout().trim()).endsWith("1\n1\n1");
    }
  }
}
