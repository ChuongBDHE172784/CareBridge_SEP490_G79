package com.carebridge.backend.migration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** Produces deterministic hashes for the production PostgreSQL catalog surface. */
final class PostgresCatalogManifest {

    private static final Map<String, String> SECTION_QUERIES = sectionQueries();

    private PostgresCatalogManifest() {}

    static Map<String, Section> capture(Connection connection) throws Exception {
        Map<String, Section> sections = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : SECTION_QUERIES.entrySet()) {
            sections.put(entry.getKey(), captureSection(connection, entry.getValue()));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sections));
    }

    static String properties(Map<String, Section> sections) {
        StringBuilder output = new StringBuilder();
        sections.forEach((name, section) -> output
                .append(name).append(".count=").append(section.count()).append('\n')
                .append(name).append(".sha256=").append(section.sha256()).append('\n'));
        return output.toString();
    }

    private static Section captureSection(Connection connection, String query) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long count = 0;
        try (var statement = connection.createStatement(); ResultSet rows = statement.executeQuery(query)) {
            int columns = rows.getMetaData().getColumnCount();
            while (rows.next()) {
                count++;
                for (int column = 1; column <= columns; column++) {
                    if (column > 1) {
                        digest.update((byte) '\t');
                    }
                    digest.update(normalize(rows.getString(column)).getBytes(StandardCharsets.UTF_8));
                }
                digest.update((byte) '\n');
            }
        }
        return new Section(count, HexFormat.of().formatHex(digest.digest()));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "<null>";
        }
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Map<String, String> sectionQueries() {
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("tables_columns_defaults", """
                SELECT relation.relname,
                       owner_role.rolname,
                       COALESCE((SELECT string_agg(acl::text, ',' ORDER BY acl::text)
                                   FROM unnest(relation.relacl) acl), ''),
                       attribute.attnum::text,
                       attribute.attname,
                       pg_catalog.format_type(attribute.atttypid, attribute.atttypmod),
                       attribute.attnotnull::text,
                       COALESCE(pg_catalog.pg_get_expr(default_entry.adbin, default_entry.adrelid), '')
                  FROM pg_catalog.pg_class relation
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = relation.relnamespace
                  JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = relation.relowner
                  JOIN pg_catalog.pg_attribute attribute ON attribute.attrelid = relation.oid
             LEFT JOIN pg_catalog.pg_attrdef default_entry
                    ON default_entry.adrelid = relation.oid
                   AND default_entry.adnum = attribute.attnum
                 WHERE namespace_entry.nspname = 'public'
                   AND relation.relkind IN ('r', 'p')
                   AND relation.relname <> 'flyway_schema_history'
                   AND attribute.attnum > 0
                   AND NOT attribute.attisdropped
              ORDER BY relation.relname, attribute.attnum
                """);
        queries.put("constraints", """
                SELECT relation.relname,
                       constraint_entry.conname,
                       constraint_entry.contype::text,
                       pg_catalog.pg_get_constraintdef(constraint_entry.oid, true),
                       constraint_entry.convalidated::text,
                       constraint_entry.condeferrable::text,
                       constraint_entry.condeferred::text
                  FROM pg_catalog.pg_constraint constraint_entry
                  JOIN pg_catalog.pg_class relation ON relation.oid = constraint_entry.conrelid
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = relation.relnamespace
                 WHERE namespace_entry.nspname = 'public'
                   AND relation.relname <> 'flyway_schema_history'
              ORDER BY relation.relname, constraint_entry.conname
                """);
        queries.put("nonconstraint_indexes", """
                SELECT table_entry.relname,
                       index_entry.relname,
                       pg_catalog.pg_get_indexdef(index_entry.oid),
                       index_state.indisunique::text,
                       index_state.indisvalid::text,
                       COALESCE(pg_catalog.pg_get_expr(index_state.indpred, index_state.indrelid), '')
                  FROM pg_catalog.pg_index index_state
                  JOIN pg_catalog.pg_class table_entry ON table_entry.oid = index_state.indrelid
                  JOIN pg_catalog.pg_class index_entry ON index_entry.oid = index_state.indexrelid
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = table_entry.relnamespace
             LEFT JOIN pg_catalog.pg_constraint constraint_entry
                    ON constraint_entry.conindid = index_state.indexrelid
                 WHERE namespace_entry.nspname = 'public'
                   AND table_entry.relname <> 'flyway_schema_history'
                   AND constraint_entry.oid IS NULL
              ORDER BY table_entry.relname, index_entry.relname
                """);
        queries.put("views", """
                SELECT relation.relname,
                       pg_catalog.pg_get_viewdef(relation.oid, true),
                       owner_role.rolname,
                       COALESCE((SELECT string_agg(acl::text, ',' ORDER BY acl::text)
                                   FROM unnest(relation.relacl) acl), '')
                  FROM pg_catalog.pg_class relation
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = relation.relnamespace
                  JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = relation.relowner
                 WHERE namespace_entry.nspname = 'public'
                   AND relation.relkind IN ('v', 'm')
              ORDER BY relation.relname
                """);
        queries.put("functions", """
                SELECT routine.proname,
                       pg_catalog.pg_get_function_identity_arguments(routine.oid),
                       pg_catalog.pg_get_function_result(routine.oid),
                       language_entry.lanname,
                       routine.provolatile::text,
                       routine.prosecdef::text,
                       COALESCE(array_to_string(routine.proconfig, ','), ''),
                       pg_catalog.pg_get_functiondef(routine.oid),
                       owner_role.rolname,
                       COALESCE((SELECT string_agg(acl::text, ',' ORDER BY acl::text)
                                   FROM unnest(routine.proacl) acl), '')
                  FROM pg_catalog.pg_proc routine
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = routine.pronamespace
                  JOIN pg_catalog.pg_language language_entry
                    ON language_entry.oid = routine.prolang
                  JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
                 WHERE namespace_entry.nspname = 'public'
              ORDER BY routine.proname,
                       pg_catalog.pg_get_function_identity_arguments(routine.oid)
                """);
        queries.put("triggers", """
                SELECT relation.relname,
                       trigger_entry.tgname,
                       pg_catalog.pg_get_triggerdef(trigger_entry.oid, true),
                       trigger_entry.tgenabled::text,
                       trigger_entry.tgtype::text
                  FROM pg_catalog.pg_trigger trigger_entry
                  JOIN pg_catalog.pg_class relation ON relation.oid = trigger_entry.tgrelid
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = relation.relnamespace
                 WHERE namespace_entry.nspname = 'public'
                   AND NOT trigger_entry.tgisinternal
              ORDER BY relation.relname, trigger_entry.tgname
                """);
        queries.put("rls_policies", """
                SELECT table_entry.relname,
                       table_entry.relrowsecurity::text,
                       table_entry.relforcerowsecurity::text,
                       COALESCE(policy_entry.polname, ''),
                       COALESCE(policy_entry.polpermissive::text, ''),
                       COALESCE(policy_entry.polcmd::text, ''),
                       COALESCE(pg_catalog.pg_get_expr(policy_entry.polqual, policy_entry.polrelid), ''),
                       COALESCE(pg_catalog.pg_get_expr(policy_entry.polwithcheck, policy_entry.polrelid), '')
                  FROM pg_catalog.pg_class table_entry
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = table_entry.relnamespace
             LEFT JOIN pg_catalog.pg_policy policy_entry
                    ON policy_entry.polrelid = table_entry.oid
                 WHERE namespace_entry.nspname = 'public'
                   AND table_entry.relkind IN ('r', 'p')
                   AND table_entry.relname <> 'flyway_schema_history'
              ORDER BY table_entry.relname, policy_entry.polname
                """);
        queries.put("comments", """
                SELECT object_kind, object_name, sub_name, description
                  FROM (
                    SELECT 'relation' AS object_kind,
                           relation.relname AS object_name,
                           '' AS sub_name,
                           description_entry.description
                      FROM pg_catalog.pg_description description_entry
                      JOIN pg_catalog.pg_class relation ON relation.oid = description_entry.objoid
                      JOIN pg_catalog.pg_namespace namespace_entry
                        ON namespace_entry.oid = relation.relnamespace
                     WHERE namespace_entry.nspname = 'public'
                       AND description_entry.classoid = 'pg_class'::regclass
                       AND description_entry.objsubid = 0
                    UNION ALL
                    SELECT 'column', relation.relname, attribute.attname,
                           description_entry.description
                      FROM pg_catalog.pg_description description_entry
                      JOIN pg_catalog.pg_class relation ON relation.oid = description_entry.objoid
                      JOIN pg_catalog.pg_namespace namespace_entry
                        ON namespace_entry.oid = relation.relnamespace
                      JOIN pg_catalog.pg_attribute attribute
                        ON attribute.attrelid = relation.oid
                       AND attribute.attnum = description_entry.objsubid
                     WHERE namespace_entry.nspname = 'public'
                       AND description_entry.classoid = 'pg_class'::regclass
                       AND description_entry.objsubid > 0
                    UNION ALL
                    SELECT 'function', routine.proname,
                           pg_catalog.pg_get_function_identity_arguments(routine.oid),
                           description_entry.description
                      FROM pg_catalog.pg_description description_entry
                      JOIN pg_catalog.pg_proc routine ON routine.oid = description_entry.objoid
                      JOIN pg_catalog.pg_namespace namespace_entry
                        ON namespace_entry.oid = routine.pronamespace
                     WHERE namespace_entry.nspname = 'public'
                       AND description_entry.classoid = 'pg_proc'::regclass
                  ) described
              ORDER BY object_kind, object_name, sub_name
                """);
        return Collections.unmodifiableMap(new LinkedHashMap<>(queries));
    }

    record Section(long count, String sha256) {}
}
