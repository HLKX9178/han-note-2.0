package com.hanserwei.auth.domain.mapper;

import com.hanserwei.auth.domain.dataobject.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserDOMapperTest {

    @Autowired
    private UserDOMapper userDOMapper;

    @Test
    void crud_smoke() {
        // insert
        UserDO user = UserDO.builder()
                .username("hanserwei-test")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        int inserted = userDOMapper.insert(user);
        assertEquals(1, inserted);
        assertNotNull(user.getId(), "IDENTITY 主键应回填");

        // select
        UserDO found = userDOMapper.selectById(user.getId());
        assertNotNull(found);
        assertEquals("hanserwei-test", found.getUsername());

        // delete (cleanup)
        int deleted = userDOMapper.deleteById(user.getId());
        assertTrue(deleted >= 1);
    }
}
