-- Owner-local selective audit reads, preserving timestamp/UUID keyset order.
CREATE INDEX IF NOT EXISTS ix_admin_audit_actor_cursor
    ON admin_audit_log (actor_user_id, occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS ix_admin_audit_command_cursor
    ON admin_audit_log (command_id, occurred_at DESC, id DESC)
    WHERE command_id IS NOT NULL;
