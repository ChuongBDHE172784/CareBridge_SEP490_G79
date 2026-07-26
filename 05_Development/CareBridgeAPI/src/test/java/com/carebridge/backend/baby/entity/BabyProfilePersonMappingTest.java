package com.carebridge.backend.baby.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.profile.entity.Person;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class BabyProfilePersonMappingTest {

    @Test
    void babyPersistsNotNullPersonForeignKeyToSyntheticUsersRow() throws Exception {
        var field = BabyProfile.class.getDeclaredField("person");
        JoinColumn join = field.getAnnotation(JoinColumn.class);

        assertThat(join).isNotNull();
        assertThat(join.name()).isEqualTo("person_id");
        assertThat(join.nullable()).isFalse();
        assertThat(Person.class.getAnnotation(Table.class).name()).isEqualTo("users");
    }
}
